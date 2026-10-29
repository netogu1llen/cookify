package com.app.cookify.server.datos.externo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El parser es la pieza que se va a romper cuando la cadena cambie su sitio, y
 * la que tiene que fallar de forma visible cuando eso pase.
 *
 * El fragmento de abajo reproduce la forma real del payload: el catalogo viene
 * dentro de una etiqueta script con las comillas escapadas, los objetos de
 * producto estan anidados dentro de objetos mas grandes, y entre ellos hay
 * arreglos y campos que no interesan. Se usa un fragmento y no una pagina
 * completa porque una pagina real pesa dos megas.
 */
class ParserCatalogoTest {

    private val fragmento = """
        <script>self.__next_f.push([1,"{\"props\":{\"products\":[
        {\"productId\":\"5804\",\"slug\":\"papas-jumbo-malla-2-kg\",\"categories\":[\"20\",\"786\"],
        \"items\":[{\"skuId\":\"5872\",\"cartLimit\":6,\"measurementUnit\":\"un\",
        \"measurementUnitUn\":\"kg\",\"unitMultiplierUn\":2,\"price\":3450,\"listPrice\":3450,
        \"ppumPrice\":1725,\"ppumListPrice\":1725,\"ppumMeasurementUnit\":\"kg\",
        \"images\":[\"https://ejemplo/papa.jpg?v=1\"],\"name\":\"Papa Malla 2 kg\",\"stock\":true,
        \"promotions\":[]}]},
        {\"productId\":\"5805\",\"items\":[{\"skuId\":\"3573\",\"measurementUnitUn\":\"un\",
        \"unitMultiplierUn\":1,\"price\":850,\"listPrice\":850,\"ppumListPrice\":850,
        \"ppumMeasurementUnit\":\"un\",\"name\":\"Pepino Ensalada 1 un.\",\"stock\":false}]},
        {\"productId\":\"5806\",\"items\":[{\"skuId\":\"9001\",\"listPrice\":1150,
        \"ppumListPrice\":230,\"ppumMeasurementUnit\":\"g\",
        \"name\":\"Orégano Entero Gourmet 50 g\",\"stock\":true}]}
        ]}}"])</script>
    """.trimIndent()

    @Test
    fun `saca los tres productos del payload embebido`() {
        val productos = ParserCatalogo.productos(fragmento)
        assertEquals(3, productos.size)
        assertEquals(
            listOf("Papa Malla 2 kg", "Pepino Ensalada 1 un.", "Orégano Entero Gourmet 50 g"),
            productos.map { it.nombre },
        )
    }

    @Test
    fun `lee el precio por unidad de medida con su unidad`() {
        val papa = ParserCatalogo.productos(fragmento).first()
        assertEquals("5872", papa.sku)
        assertEquals(3450.0, papa.precioLista)
        assertEquals(1725.0, papa.ppum)
        assertEquals("kg", papa.medida)
        assertTrue(papa.enStock)
    }

    @Test
    fun `respeta el stock que declara la cadena`() {
        val pepino = ParserCatalogo.productos(fragmento)[1]
        assertEquals(false, pepino.enStock)
    }

    @Test
    fun `prefiere el precio de lista sobre el precio con promocion`() {
        // Se lee ppumListPrice y no ppumPrice a proposito: una promocion
        // horneada en el catalogo hace que el presupuesto de la semana se vea
        // optimista y que cambie solo entre dos consultas.
        val conOferta = """{\"skuId\":\"1\",\"ppumPrice\":500,\"ppumListPrice\":900,
            \"ppumMeasurementUnit\":\"kg\",\"name\":\"Arroz 1 kg\",\"stock\":true}"""
        val producto = ParserCatalogo.productos(conOferta).single()
        assertEquals(900.0, producto.ppum)
    }

    @Test
    fun `cae al precio con promocion solo si no hay precio de lista`() {
        val sinLista = """{\"skuId\":\"1\",\"ppumPrice\":500,
            \"ppumMeasurementUnit\":\"kg\",\"name\":\"Arroz 1 kg\",\"stock\":true}"""
        assertEquals(500.0, ParserCatalogo.productos(sinLista).single().ppum)
    }

    @Test
    fun `no repite un producto que aparece dos veces en la pagina`() {
        // El payload trae el mismo producto en el listado y en el carrusel de
        // destacados. Contarlo dos veces sesgaria el percentil.
        val repetido = fragmento + fragmento
        assertEquals(3, ParserCatalogo.productos(repetido).size)
    }

    @Test
    fun `una pagina sin catalogo devuelve lista vacia y no revienta`() {
        // Es lo que pasa cuando la cadena cambia su sitio: el recolector tiene
        // que terminar sin precios, no caerse. La recoleccion queda FALLIDA y
        // el detalle lo dice.
        assertTrue(ParserCatalogo.productos("<html><body>Mantencion</body></html>").isEmpty())
        assertTrue(ParserCatalogo.productos("").isEmpty())
    }

    @Test
    fun `un objeto cortado a la mitad no arrastra a los demas`() {
        val cortado = """{\"skuId\":\"1\",\"name\":\"Incompleto"""
        assertTrue(ParserCatalogo.productos(cortado).isEmpty())
    }

    @Test
    fun `una llave dentro del nombre no confunde el conteo de llaves`() {
        // Un nombre con llaves escapadas haria que una expresion regular
        // cerrara el objeto en el lugar equivocado.
        val conLlaves = """{\"skuId\":\"7\",\"name\":\"Pack {2 un.} Arroz\",
            \"ppumListPrice\":1000,\"ppumMeasurementUnit\":\"kg\",\"stock\":true}"""
        val producto = ParserCatalogo.productos(conLlaves).single()
        assertEquals("Pack {2 un.} Arroz", producto.nombre)
        assertEquals(1000.0, producto.ppum)
    }
}

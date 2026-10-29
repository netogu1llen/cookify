package com.app.cookify.server.negocio

import com.app.cookify.server.datos.externo.ProductoCrudo
import com.app.cookify.server.modelo.Fuente
import com.app.cookify.server.modelo.Unidad
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El normalizador es el centro del servicio y es Kotlin puro, asi que se prueba
 * sin levantar nada: ni base, ni servidor, ni red.
 *
 * Los numeros de estas pruebas no son inventados. Salen de productos reales
 * leidos de jumbo.cl el 29 de octubre de 2026 y verificados contra el precio y
 * el tamano del envase con tools/verificar_fuentes.py.
 */
class NormalizadorTest {

    private fun fuente(
        medida: Unidad = Unidad.GRAMO,
        factor: Double = 1.0,
        terminos: List<String> = emptyList(),
        excluir: List<String> = emptyList(),
    ) = Fuente(
        id = 1,
        supermercado = "JUMBO",
        ingredienteId = "prueba",
        categoriaRuta = "/categoria",
        terminos = terminos,
        excluir = excluir,
        medidaEsperada = medida,
        unidadesBasePorMedida = factor,
        activa = true,
    )

    private fun producto(
        nombre: String,
        ppum: Double?,
        medida: String?,
        enStock: Boolean = true,
    ) = ProductoCrudo(
        sku = nombre,
        nombre = nombre,
        precioLista = null,
        ppum = ppum,
        medida = medida,
        enStock = enStock,
    )

    // --- canonizar ---------------------------------------------------------

    @Test
    fun `un precio por kilo queda en pesos por gramo`() {
        // Papa Malla 2 kg: $3.450, la cadena publica 1725 por kg.
        val canonico = Normalizador.canonizar("kg", 1725.0)
        assertEquals(Unidad.GRAMO, canonico?.unidad)
        assertEquals(1.725, canonico?.precio)
    }

    @Test
    fun `un precio en gramos viene por cada diez gramos, no por gramo`() {
        // Este es el hallazgo que mas costo y el que mas importa: el oregano de
        // 50 g cuesta $1.150, o sea $23 por gramo, y la cadena publica 230.
        // Leer ese 230 como pesos por gramo multiplicaba por diez el costo de
        // todos los condimentos de una receta, sin que nada fallara.
        val canonico = Normalizador.canonizar("g", 230.0)
        assertEquals(Unidad.GRAMO, canonico?.unidad)
        assertEquals(23.0, canonico?.precio)
    }

    @Test
    fun `un precio por litro queda en pesos por mililitro`() {
        val canonico = Normalizador.canonizar("lt", 1410.0)
        assertEquals(Unidad.MILILITRO, canonico?.unidad)
        assertEquals(1.41, canonico?.precio)
    }

    @Test
    fun `la unidad queda tal cual`() {
        val canonico = Normalizador.canonizar("un", 850.0)
        assertEquals(Unidad.UNIDAD, canonico?.unidad)
        assertEquals(850.0, canonico?.precio)
    }

    @Test
    fun `una medida que no se conoce se descarta en lugar de suponerse`() {
        assertNull(Normalizador.canonizar("docena", 1000.0))
        assertNull(Normalizador.canonizar(null, 1000.0))
    }

    // --- factor de conversion ---------------------------------------------

    @Test
    fun `el ajo se cobra por diente aunque la cadena venda la cabeza`() {
        // El catalogo mide el ajo en dientes; la cadena vende cabezas a $471.
        // Una cabeza trae unos diez dientes.
        val candidatos = Normalizador.candidatos(
            listOf(producto("Ajo 3 un.", ppum = 471.0, medida = "un")),
            fuente(medida = Unidad.UNIDAD, factor = 10.0, terminos = listOf("ajo")),
        )
        assertEquals(1, candidatos.size)
        assertEquals(47.1, candidatos.first().precioBase)
    }

    @Test
    fun `la palta se cobra por unidad aunque la cadena la venda por kilo`() {
        // Palta Hass Malla 1 kg a $3.690. Una palta pesa unos 200 g, asi que un
        // gramo es 1/200 de palta: el factor es 0,005 y la division lo invierte.
        val candidatos = Normalizador.candidatos(
            listOf(producto("Palta Hass Malla 1 kg", ppum = 3690.0, medida = "kg")),
            fuente(medida = Unidad.GRAMO, factor = 0.005, terminos = listOf("palta")),
        )
        assertEquals(738.0, candidatos.first().precioBase)
    }

    // --- filtros ----------------------------------------------------------

    @Test
    fun `los terminos eligen el ingrediente dentro de la categoria`() {
        val productos = listOf(
            producto("Cebolla Granel", 1975.0, "kg"),
            producto("Zanahoria Bolsa 1 kg", 1390.0, "kg"),
            producto("Papa Malla 5 kg", 1742.0, "kg"),
        )
        val candidatos = Normalizador.candidatos(productos, fuente(terminos = listOf("cebolla")))
        assertEquals(listOf("Cebolla Granel"), candidatos.map { it.nombre })
    }

    @Test
    fun `las exclusiones sacan la variedad que no corresponde`() {
        // "cebolla" tambien encuentra el cebollin y la cebolla frita, que no
        // son el ingrediente y cuestan otra cosa.
        val productos = listOf(
            producto("Cebolla Granel", 1975.0, "kg"),
            producto("Cebollín Feria 3 un.", 2817.0, "kg"),
            producto("Cebolla Frita Crispy 100 g", 12000.0, "kg"),
        )
        val candidatos = Normalizador.candidatos(
            productos,
            fuente(terminos = listOf("cebolla"), excluir = listOf("cebollin", "frita")),
        )
        assertEquals(listOf("Cebolla Granel"), candidatos.map { it.nombre })
    }

    @Test
    fun `las tildes no cambian el resultado en ninguna de las dos direcciones`() {
        // El mapeo escribe sin tildes y la cadena publica con ellas.
        val candidatos = Normalizador.candidatos(
            listOf(producto("Champiñón Bandeja 200 g", 7950.0, "kg")),
            fuente(terminos = listOf("champinon")),
        )
        assertEquals(1, candidatos.size)
    }

    @Test
    fun `un producto en otra medida que la esperada se descarta`() {
        // El pimiento se vende por unidad; una fuente que espera gramos no
        // puede convertirlo sin inventar cuanto pesa.
        val candidatos = Normalizador.candidatos(
            listOf(producto("Pimiento Rojo Extra", 1890.0, "un")),
            fuente(medida = Unidad.GRAMO, terminos = listOf("pimiento")),
        )
        assertTrue(candidatos.isEmpty())
    }

    @Test
    fun `un producto sin stock o sin precio por unidad se descarta`() {
        val productos = listOf(
            producto("Cebolla Agotada", 1975.0, "kg", enStock = false),
            producto("Cebolla Sin Precio", null, "kg"),
            producto("Cebolla Precio Cero", 0.0, "kg"),
        )
        assertTrue(Normalizador.candidatos(productos, fuente(terminos = listOf("cebolla"))).isEmpty())
    }

    @Test
    fun `sin terminos pasa toda la categoria`() {
        val productos = listOf(
            producto("Lentejas 1 kg", 3800.0, "kg"),
            producto("Lentejas 500 g", 4200.0, "kg"),
        )
        assertEquals(2, Normalizador.candidatos(productos, fuente()).size)
    }

    // --- percentil 25 -----------------------------------------------------

    @Test
    fun `el percentil veinticinco interpola entre los dos valores vecinos`() {
        // Cinco valores: la posicion 0,25 x 4 = 1 cae justo en el segundo.
        assertEquals(2.0, Normalizador.percentil25(listOf(1.0, 2.0, 3.0, 4.0, 5.0)))
        // Cuatro valores: 0,25 x 3 = 0,75, entre el primero y el segundo.
        assertEquals(1.75, Normalizador.percentil25(listOf(1.0, 2.0, 3.0, 4.0)))
    }

    @Test
    fun `con un solo candidato el percentil es ese candidato`() {
        assertEquals(42.0, Normalizador.percentil25(listOf(42.0)))
    }

    @Test
    fun `con dos candidatos tira hacia el barato, que es la intencion`() {
        // La app es para quien cocina con presupuesto contado: se asume que
        // compra la marca barata, no la del medio.
        assertEquals(1.25, Normalizador.percentil25(listOf(1.0, 2.0)))
    }

    @Test
    fun `sin candidatos no hay precio, y eso no es cero`() {
        // Devolver 0 seria peor que no devolver nada: una receta saldria gratis.
        assertNull(Normalizador.percentil25(emptyList()))
    }

    @Test
    fun `el orden en que llegan los candidatos no cambia el percentil`() {
        val valores = listOf(3.0, 1.0, 5.0, 2.0, 4.0)
        assertEquals(Normalizador.percentil25(valores.sorted()), Normalizador.percentil25(valores))
    }
}

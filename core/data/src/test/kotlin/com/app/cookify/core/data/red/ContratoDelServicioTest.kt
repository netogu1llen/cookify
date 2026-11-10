package com.app.cookify.core.data.red

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Que el cliente entienda lo que el servidor responde de verdad.
 *
 * `precios-jumbo.json` no es un ejemplo inventado: es la respuesta literal de
 * https://cookifies.duckdns.org, recortada a diez precios para que se pueda leer
 * de un vistazo. Si el servicio cambia la forma de su respuesta, esta prueba
 * falla al compilar la app y no en el teléfono de alguien.
 *
 * Es la contraparte de las 48 verificaciones del contrato que corren contra el
 * servicio: aquellas comprueban que el servidor responde lo que prometió; esta,
 * que el cliente lo interpreta bien. Las dos miran el mismo contrato desde lados
 * opuestos.
 *
 * No usa red: lee el archivo del classpath. Una prueba que depende de que un
 * servidor esté arriba no es una prueba, es una alarma.
 */
class ContratoDelServicioTest {

    /** El mismo Json que configura el cliente en RedModule. */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val respuesta: PreciosVigentesDto = json.decodeFromString(
        checkNotNull(javaClass.classLoader?.getResourceAsStream("precios-jumbo.json")) {
            "precios-jumbo.json no esta en el classpath del test"
        }.bufferedReader().use { it.readText() }
    )

    @Test
    fun `se interpreta la respuesta real del servicio`() {
        assertThat(respuesta.supermercado).isEqualTo("JUMBO")
        assertThat(respuesta.datos).hasSize(respuesta.cantidad)
        assertThat(respuesta.datos).isNotEmpty()
    }

    @Test
    fun `cada precio trae lo que el repositorio necesita`() {
        val primero = respuesta.datos.first()
        assertThat(primero.ingredienteId).isNotEmpty()
        assertThat(primero.precioClpPorUnidad).isGreaterThan(0.0)
        assertThat(primero.unidad).isIn(listOf("g", "ml", "un"))
        assertThat(primero.origen).isIn(listOf("MEDIDO", "ESTIMADO"))
    }

    @Test
    fun `la fecha del lote viene y tiene forma de ISO 8601`() {
        // La app la usa para decidir si su cache vencio y para mostrar cuando se
        // midio. Un formato distinto la dejaria sin poder hacer ninguna de las dos.
        val fecha = respuesta.actualizadoEn
        assertThat(fecha).isNotNull()
        assertThat(fecha).matches("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.*""")
    }

    @Test
    fun `los campos que el cliente no conoce no lo tumban`() {
        // La respuesta real trae id y recoleccionId, que al cliente no le
        // sirven de nada. Que los ignore es lo que permite al servicio agregar
        // campos sin subir la version de la interfaz, tal como dice el contrato.
        val conCampoNuevo = """
            {"supermercado":"JUMBO","cantidad":1,"inventadoManana":true,
             "datos":[{"ingredienteId":"cebolla","precioClpPorUnidad":2.0,
                       "unidad":"g","origen":"MEDIDO","loQueVenga":42}]}
        """.trimIndent()

        val leida = json.decodeFromString<PreciosVigentesDto>(conCampoNuevo)

        assertThat(leida.datos.single().ingredienteId).isEqualTo("cebolla")
    }

    @Test
    fun `una respuesta sin datos se interpreta como vacia y no como error`() {
        val vacia = json.decodeFromString<PreciosVigentesDto>(
            """{"supermercado":"TOTTUS","cantidad":0,"datos":[]}"""
        )

        assertThat(vacia.datos).isEmpty()
        assertThat(vacia.actualizadoEn).isNull()
    }
}

package com.app.cookify.server.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * El servicio tiene que negarse a arrancar sin configuracion.
 *
 * Arrancar con una clave vacia dejaria la API abierta a escritura y nadie se
 * daria cuenta hasta que alguien escriba. Fallar en el arranque es ruidoso y
 * barato; fallar en produccion, ninguna de las dos cosas.
 */
class ConfiguracionTest {

    private val completo = mapOf(
        "COOKIFY_DB_URL" to "jdbc:postgresql://localhost:5434/cookify",
        "COOKIFY_DB_USUARIO" to "cookify",
        "COOKIFY_DB_CLAVE" to "clave_de_prueba",
        "COOKIFY_API_KEY" to "clave_de_api_de_prueba",
    )

    private fun entorno(valores: Map<String, String>): (String) -> String? = valores::get

    @Test
    fun `lee el entorno completo y aplica los valores por omision`() {
        val configuracion = Configuracion.desdeEntorno(entorno(completo))
        assertEquals(8080, configuracion.puerto)
        assertEquals(4, configuracion.recolector.paginasMaximas)
        assertEquals(400L, configuracion.recolector.pausaEntrePeticionesMs)
        assertEquals("cookify", configuracion.baseDeDatos.usuario)
    }

    @Test
    fun `sin clave de api no arranca`() {
        val error = assertFailsWith<IllegalStateException> {
            Configuracion.desdeEntorno(entorno(completo - "COOKIFY_API_KEY"))
        }
        assertTrue(error.message!!.contains("COOKIFY_API_KEY"))
        assertTrue(error.message!!.contains(".env.example"))
    }

    @Test
    fun `sin datos de la base no arranca`() {
        assertFailsWith<IllegalStateException> {
            Configuracion.desdeEntorno(entorno(completo - "COOKIFY_DB_URL"))
        }
    }

    @Test
    fun `una variable en blanco cuenta como ausente`() {
        // Un .env con la linea presente y el valor vacio es el error mas comun
        // al copiar el ejemplo, y es el que mas cuesta de encontrar despues.
        assertFailsWith<IllegalStateException> {
            Configuracion.desdeEntorno(entorno(completo + ("COOKIFY_DB_CLAVE" to "   ")))
        }
    }

    @Test
    fun `una clave de api demasiado corta no se acepta`() {
        val error = assertFailsWith<IllegalArgumentException> {
            Configuracion.desdeEntorno(entorno(completo + ("COOKIFY_API_KEY" to "corta")))
        }
        // El mensaje dice el largo y nunca el valor: un mensaje de arranque
        // termina en un registro, y un registro se comparte.
        assertTrue(error.message!!.contains("5 caracteres"))
        assertTrue(!error.message!!.contains("corta"))
    }

    @Test
    fun `un numero que no es numero cae al valor por omision en lugar de reventar`() {
        // El puerto y la pausa son ajustes, no identidad: si vienen mal escritos
        // conviene arrancar con el valor sano antes que no arrancar.
        val configuracion = Configuracion.desdeEntorno(
            entorno(completo + ("COOKIFY_PUERTO" to "ocho mil"))
        )
        assertEquals(8080, configuracion.puerto)
    }
}

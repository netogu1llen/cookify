package com.app.cookify.core.common

import com.google.common.truth.Truth.assertThat
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * La fecha que la pantalla muestra junto al precio.
 *
 * La zona horaria se fija en la prueba: sin eso, el resultado dependería de la
 * máquina que la corre y la prueba pasaría en un equipo y fallaría en otro.
 */
class FormatoFechaHoraTest {

    private val original: TimeZone = TimeZone.getDefault()

    @Before
    fun fijarZona() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"))
    }

    @After
    fun restaurarZona() {
        TimeZone.setDefault(original)
    }

    @Test
    fun `una fecha del servicio se muestra en hora local`() {
        // El servicio responde en UTC. Decirle a alguien en Chile que su precio
        // se midio a las 22:50 cuando eran las 19:50 lo hace dudar del dato.
        assertThat("2026-11-09T22:50:09.857044Z".aFechaHoraCorta()).isEqualTo("09/11/26 19:50")
    }

    @Test
    fun `el dia y el mes van con cero adelante`() {
        // "9/1/26" se lee peor que "09/01/26" en una linea de texto chica, y
        // ademas desalinea cuando hay varias fechas una debajo de otra.
        assertThat("2026-01-09T12:00:00Z".aFechaHoraCorta()).isEqualTo("09/01/26 09:00")
    }

    @Test
    fun `una fecha sin milisegundos tambien se entiende`() {
        assertThat("2026-11-09T22:50:09Z".aFechaHoraCorta()).isEqualTo("09/11/26 19:50")
    }

    @Test
    fun `lo que no es una fecha devuelve nulo en vez de inventar una`() {
        // Quien la use decide que mostrar. Una fecha inventada al lado de un
        // precio es peor que no mostrar fecha.
        assertThat("ayer".aFechaHoraCorta()).isNull()
        assertThat("".aFechaHoraCorta()).isNull()
        assertThat(null.aFechaHoraCorta()).isNull()
    }
}

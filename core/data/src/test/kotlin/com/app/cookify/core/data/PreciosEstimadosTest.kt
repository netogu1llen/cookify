package com.app.cookify.core.data

import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.OrigenPrecio
import com.app.cookify.core.model.Pasillo
import com.app.cookify.core.model.PreciosDeCadena
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.model.Unidad
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * La estimacion de precios por cadena.
 *
 * Hasta el Hito 2 esta regla vivia dentro de CalculadoraCostos, que multiplicaba
 * el precio del catalogo por el factor de la cadena. Con precios medidos eso
 * contaria dos veces, asi que el multiplicador paso a ser asunto de la fuente que
 * estima. Estas pruebas fijan ese contrato.
 */
class PreciosEstimadosTest {

    private val base = mapOf(
        "cebolla" to Ingrediente("cebolla", "Cebolla", Pasillo.VERDULERIA, Unidad.GRAMO, 1.3),
        "huevo" to Ingrediente("huevo", "Huevo", Pasillo.LACTEOS, Unidad.UNIDAD, 250.0),
    )

    @Test
    fun `la cadena cara sale mas cara que la barata`() {
        val jumbo = PreciosDeCadena.estimados(Supermercado.JUMBO, base)
        val acuenta = PreciosDeCadena.estimados(Supermercado.ACUENTA, base)

        val enJumbo = jumbo.ingredientes.getValue("cebolla").precioClpPorUnidad
        val enAcuenta = acuenta.ingredientes.getValue("cebolla").precioClpPorUnidad
        assertThat(enJumbo).isGreaterThan(enAcuenta)
    }

    @Test
    fun `el multiplicador se aplica al precio base de Lider`() {
        val jumbo = PreciosDeCadena.estimados(Supermercado.JUMBO, base)
        // 1,3 por gramo en Lider, por el 1,15 de Jumbo.
        assertThat(jumbo.ingredientes.getValue("cebolla").precioClpPorUnidad)
            .isWithin(TOLERANCIA).of(1.3 * Supermercado.JUMBO.multiplicadorPrecio)
    }

    @Test
    fun `Lider no se multiplica, porque es la base`() {
        val lider = PreciosDeCadena.estimados(Supermercado.LIDER, base)
        assertThat(lider.ingredientes.getValue("huevo").precioClpPorUnidad)
            .isWithin(TOLERANCIA).of(250.0)
    }

    @Test
    fun `todo lo estimado queda marcado como estimado`() {
        val estimados = PreciosDeCadena.estimados(Supermercado.SANTA_ISABEL, base)

        assertThat(estimados.origen.values).containsExactly(
            OrigenPrecio.ESTIMADO, OrigenPrecio.ESTIMADO,
        )
        assertThat(estimados.medidos).isEqualTo(0)
        assertThat(estimados.mayoriaMedida).isFalse()
    }

    @Test
    fun `sin medicion no hay fecha de medicion`() {
        // La fecha del catalogo es cuando se escribieron los valores a mano, que
        // no es lo mismo que haberlos medido. Decir que si enganaria a la pantalla.
        assertThat(PreciosDeCadena.estimados(Supermercado.TOTTUS, base).medidoEn).isNull()
    }

    @Test
    fun `no se pierde ni se inventa ningun ingrediente`() {
        val estimados = PreciosDeCadena.estimados(Supermercado.UNIMARC, base)
        assertThat(estimados.ingredientes.keys).isEqualTo(base.keys)
    }

    @Test
    fun `la unidad y el pasillo no los toca la estimacion`() {
        val estimados = PreciosDeCadena.estimados(Supermercado.JUMBO, base)
        val huevo = estimados.ingredientes.getValue("huevo")
        assertThat(huevo.unidad).isEqualTo(Unidad.UNIDAD)
        assertThat(huevo.pasillo).isEqualTo(Pasillo.LACTEOS)
    }

    private companion object {
        const val TOLERANCIA = 0.0001
    }
}

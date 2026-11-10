package com.app.cookify.core.data.red

import com.app.cookify.core.database.PrecioCacheDao
import com.app.cookify.core.database.PrecioCacheEntity
import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.OrigenPrecio
import com.app.cookify.core.model.Pasillo
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.model.Unidad
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * La cascada que decide de dónde sale cada precio.
 *
 * ```
 * caché fresca  →  servicio  →  caché vencida  →  catálogo empaquetado
 * ```
 *
 * Es la lógica con más riesgo de toda la integración, porque sus caminos
 * dependen de cosas que en una prueba manual no se pueden provocar a voluntad:
 * que no haya red, que la caché esté vencida, que el servicio responda a medias.
 * Aquí se provocan todos.
 *
 * No hay red ni base de datos: el cliente y el DAO son dobles.
 */
class CascadaDePreciosTest {

    private val catalogo = mapOf(
        "cebolla" to Ingrediente("cebolla", "Cebolla", Pasillo.VERDULERIA, Unidad.GRAMO, 1.3),
        "huevo" to Ingrediente("huevo", "Huevo", Pasillo.LACTEOS, Unidad.UNIDAD, 250.0),
        "tofu" to Ingrediente("tofu", "Tofu", Pasillo.ABARROTES, Unidad.GRAMO, 8.0),
    )

    private fun dto(vararg precios: Pair<String, Double>, medidoEn: String? = "2026-11-09T22:50:09Z") =
        PreciosVigentesDto(
            supermercado = "JUMBO",
            actualizadoEn = medidoEn,
            cantidad = precios.size,
            datos = precios.map { (id, valor) ->
                PrecioDto(
                    ingredienteId = id,
                    precioClpPorUnidad = valor,
                    unidad = catalogo.getValue(id).unidad.simbolo,
                    origen = "MEDIDO",
                    candidatos = 3,
                    medidoEn = medidoEn,
                )
            },
        )

    private fun entidad(id: String, valor: Double, guardadoEn: Long) = PrecioCacheEntity(
        supermercado = "JUMBO",
        ingredienteId = id,
        precioClpPorUnidad = valor,
        unidad = catalogo.getValue(id).unidad.simbolo,
        origen = "MEDIDO",
        medidoEn = "2026-11-01T10:00:00Z",
        guardadoEn = guardadoEn,
    )

    private fun hace(dias: Int): Long = System.currentTimeMillis() - dias.days.inWholeMilliseconds

    // --- caché fresca ------------------------------------------------------

    @Test
    fun `con cache fresca no se llama al servicio`() = runTest {
        val remoto = RemotoFalso(PreciosRemotos.Resultado.Hay(dto("cebolla" to 9.99)))
        val cache = CacheFalso(listOf(entidad("cebolla", 2.015, hace(1))))

        val precios = repositorio(remoto, cache).precios(Supermercado.JUMBO)

        assertThat(remoto.llamadas).isEqualTo(0)
        assertThat(precios.ingredientes.getValue("cebolla").precioClpPorUnidad).isEqualTo(2.015)
    }

    @Test
    fun `una cache de seis dias todavia sirve`() = runTest {
        val cache = CacheFalso(listOf(entidad("cebolla", 2.015, hace(6))))
        val remoto = RemotoFalso(PreciosRemotos.Resultado.NoSePudo("sin red"))

        repositorio(remoto, cache).precios(Supermercado.JUMBO)

        assertThat(remoto.llamadas).isEqualTo(0)
    }

    @Test
    fun `a los ocho dias la cache vencio y se vuelve a pedir`() = runTest {
        val cache = CacheFalso(listOf(entidad("cebolla", 2.015, hace(8))))
        val remoto = RemotoFalso(PreciosRemotos.Resultado.Hay(dto("cebolla" to 3.0)))

        val precios = repositorio(remoto, cache).precios(Supermercado.JUMBO)

        assertThat(remoto.llamadas).isEqualTo(1)
        assertThat(precios.ingredientes.getValue("cebolla").precioClpPorUnidad).isEqualTo(3.0)
    }

    // --- servicio ----------------------------------------------------------

    @Test
    fun `lo que responde el servicio queda guardado en la cache`() = runTest {
        val cache = CacheFalso(emptyList())
        val remoto = RemotoFalso(PreciosRemotos.Resultado.Hay(dto("cebolla" to 2.5)))

        repositorio(remoto, cache).precios(Supermercado.JUMBO)

        assertThat(cache.guardados.map { it.ingredienteId }).containsExactly("cebolla")
        assertThat(cache.guardados.single().precioClpPorUnidad).isEqualTo(2.5)
    }

    @Test
    fun `el precio del servicio viene marcado como medido`() = runTest {
        val remoto = RemotoFalso(PreciosRemotos.Resultado.Hay(dto("cebolla" to 2.5)))

        val precios = repositorio(remoto, CacheFalso(emptyList())).precios(Supermercado.JUMBO)

        assertThat(precios.origenDe("cebolla")).isEqualTo(OrigenPrecio.MEDIDO)
        assertThat(precios.medidoEn).isEqualTo("2026-11-09T22:50:09Z")
    }

    @Test
    fun `un ingrediente que el servicio no conoce se queda estimado, no desaparece`() = runTest {
        // Es el caso real de tofu y soya texturizada: no se venden en linea, el
        // servicio no tiene precio medido, y las recetas que los usan tienen que
        // poder costearse igual.
        val remoto = RemotoFalso(PreciosRemotos.Resultado.Hay(dto("cebolla" to 2.5)))

        val precios = repositorio(remoto, CacheFalso(emptyList())).precios(Supermercado.JUMBO)

        assertThat(precios.ingredientes.keys).isEqualTo(catalogo.keys)
        assertThat(precios.origenDe("tofu")).isEqualTo(OrigenPrecio.ESTIMADO)
        assertThat(precios.ingredientes.getValue("tofu").precioClpPorUnidad)
            .isWithin(TOLERANCIA).of(8.0 * Supermercado.JUMBO.multiplicadorPrecio)
    }

    // --- sin red -----------------------------------------------------------

    @Test
    fun `sin red se usa la cache vencida antes que estimar`() = runTest {
        val cache = CacheFalso(listOf(entidad("cebolla", 2.015, hace(30))))
        val remoto = RemotoFalso(PreciosRemotos.Resultado.NoSePudo("sin red"))

        val precios = repositorio(remoto, cache).precios(Supermercado.JUMBO)

        // Un precio medido hace un mes es mejor que uno estimado hoy.
        assertThat(precios.ingredientes.getValue("cebolla").precioClpPorUnidad).isEqualTo(2.015)
        assertThat(precios.origenDe("cebolla")).isEqualTo(OrigenPrecio.MEDIDO)
    }

    @Test
    fun `sin red y sin cache se arma la semana igual, con precios estimados`() = runTest {
        // El escalon que no es opcional: sin el no se podria armar una semana
        // sin señal, y la app dejaria de servir en el unico momento en que mas
        // se usa, que es en el supermercado.
        val remoto = RemotoFalso(PreciosRemotos.Resultado.NoSePudo("sin red"))

        val precios = repositorio(remoto, CacheFalso(emptyList())).precios(Supermercado.JUMBO)

        assertThat(precios.ingredientes).hasSize(catalogo.size)
        assertThat(precios.medidos).isEqualTo(0)
        assertThat(precios.medidoEn).isNull()
        assertThat(precios.ingredientes.getValue("cebolla").precioClpPorUnidad)
            .isWithin(TOLERANCIA).of(1.3 * Supermercado.JUMBO.multiplicadorPrecio)
    }

    @Test
    fun `una respuesta vacia del servicio no borra lo que habia`() = runTest {
        // Un servicio recien desplegado, sin recolecciones todavia, responde una
        // lista vacia. Tomarla al pie de la letra dejaria la app sin precios.
        val cache = CacheFalso(listOf(entidad("cebolla", 2.015, hace(30))))
        val remoto = RemotoFalso(PreciosRemotos.Resultado.Hay(dto(medidoEn = null)))

        val precios = repositorio(remoto, cache).precios(Supermercado.JUMBO)

        assertThat(cache.guardados).isEmpty()
        assertThat(precios.ingredientes.getValue("cebolla").precioClpPorUnidad).isEqualTo(2.015)
    }

    @Test
    fun `un precio en cero o negativo se ignora y se estima ese ingrediente`() = runTest {
        val remoto = RemotoFalso(PreciosRemotos.Resultado.Hay(dto("cebolla" to 0.0)))

        val precios = repositorio(remoto, CacheFalso(emptyList())).precios(Supermercado.JUMBO)

        assertThat(precios.ingredientes.getValue("cebolla").precioClpPorUnidad)
            .isWithin(TOLERANCIA).of(1.3 * Supermercado.JUMBO.multiplicadorPrecio)
    }

    // --- dobles ------------------------------------------------------------

    private fun repositorio(remoto: PreciosRemotos, cache: CacheFalso) =
        PrecioRepositoryRemoto(remoto, cache, RespaldoFalso(catalogo))

    private class RemotoFalso(private val respuesta: PreciosRemotos.Resultado) : PreciosRemotos {
        var llamadas = 0
            private set

        override suspend fun deCadena(supermercado: Supermercado): PreciosRemotos.Resultado {
            llamadas++
            return respuesta
        }
    }

    /** El ultimo escalon: lo que la app sabe sin red. */
    private class RespaldoFalso(
        private val catalogo: Map<String, Ingrediente>,
    ) : com.app.cookify.core.domain.PrecioRepository {
        override suspend fun precios(supermercado: Supermercado) =
            com.app.cookify.core.model.PreciosDeCadena.estimados(supermercado, catalogo)
    }

    /**
     * La cache, en memoria.
     *
     * reemplazarLote no se redefine: viene implementado en la interfaz del DAO y
     * llama a borrarDe y a insertar, asi que probarlo aqui prueba tambien que esa
     * implementacion hace lo que dice.
     */
    private class CacheFalso(inicial: List<PrecioCacheEntity>) : PrecioCacheDao {
        private val filas = inicial.toMutableList()

        /** Lo que se escribio durante la prueba, para poder afirmar sobre ello. */
        val guardados: List<PrecioCacheEntity> get() = escrituras

        private val escrituras = mutableListOf<PrecioCacheEntity>()

        override suspend fun deSupermercado(supermercado: String) =
            filas.filter { it.supermercado == supermercado }

        override suspend fun guardadoEn(supermercado: String): Long? =
            filas.filter { it.supermercado == supermercado }.minOfOrNull { it.guardadoEn }

        override suspend fun borrarDe(supermercado: String) {
            filas.removeAll { it.supermercado == supermercado }
        }

        override suspend fun insertar(precios: List<PrecioCacheEntity>) {
            filas += precios
            escrituras += precios
        }

        override suspend fun resumen() = filas.groupBy { it.supermercado }.map { (cadena, suyas) ->
            com.app.cookify.core.database.ResumenCache(
                supermercado = cadena,
                total = suyas.size,
                medidos = suyas.count { it.origen == "MEDIDO" },
                medidoEn = suyas.mapNotNull { it.medidoEn }.maxOrNull(),
                guardadoEn = suyas.minOfOrNull { it.guardadoEn },
            )
        }

        override suspend fun borrarTodo() {
            filas.clear()
        }
    }

    private companion object {
        const val TOLERANCIA = 0.0001
    }
}

package com.app.cookify.feature.planning

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.cookify.core.domain.CatalogoRepository
import com.app.cookify.core.domain.GenerarPlanSemanalUseCase
import com.app.cookify.core.domain.PrecioRepository
import com.app.cookify.core.model.Artefacto
import com.app.cookify.core.model.BasePrincipal
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.IngredienteReceta
import com.app.cookify.core.model.Pasillo
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.Restriccion
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.model.Unidad
import com.app.cookify.core.ui.componentes.EstadoCheck
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArmadoViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun instalarMain() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun soltarMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun `los textos nombran el super, el presupuesto y los dias que pidio el usuario`() = runTest {
        val vm = viewModel(recetas = catalogoDe(cantidad = 5))

        vm.armar(solicitud())
        advanceUntilIdle()

        val textos = vm.estado.value.etapas.map { it.texto }
        assertThat(textos[0]).contains("Jumbo")
        assertThat(textos[1]).contains("45.000")
        assertThat(textos[2]).contains("de lunes a miércoles")
    }

    @Test
    fun `cuando la semana cabe, cierra las cuatro etapas y avisa`() = runTest {
        val vm = viewModel(recetas = catalogoDe(cantidad = 5))

        vm.eventos.test {
            vm.armar(solicitud())
            val listo = awaitItem() as EventoArmado.Listo

            assertThat(listo.solicitud).isEqualTo(solicitud())
            assertThat(vm.estado.value.etapas.map { it.estado }).containsExactly(
                EstadoCheck.LISTO,
                EstadoCheck.LISTO,
                EstadoCheck.LISTO,
                EstadoCheck.LISTO,
            )
            assertThat(vm.estado.value.fallo).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `si no alcanza el presupuesto se detiene en la etapa del presupuesto`() = runTest {
        val vm = viewModel(recetas = catalogoDe(cantidad = 5))

        vm.eventos.test {
            vm.armar(solicitud(presupuestoClp = PRESUPUESTO_IMPOSIBLE))
            advanceUntilIdle()

            val fallo = vm.estado.value.fallo
            assertThat(fallo).isInstanceOf(FalloArmado.Presupuesto::class.java)
            assertThat(fallo?.etapa).isEqualTo(ETAPA_PRESUPUESTO)

            // La etapa del catalogo alcanzo a cerrarse; la del presupuesto quedo en curso.
            assertThat(vm.estado.value.etapas[ETAPA_CATALOGO].estado).isEqualTo(EstadoCheck.LISTO)
            assertThat(vm.estado.value.etapas[ETAPA_PRESUPUESTO].estado)
                .isEqualTo(EstadoCheck.EN_CURSO)
            expectNoEvents()
        }
    }

    @Test
    fun `continuar con el minimo avisa con el presupuesto ya subido`() = runTest {
        val vm = viewModel(recetas = catalogoDe(cantidad = 5))

        vm.eventos.test {
            vm.armar(solicitud(presupuestoClp = PRESUPUESTO_IMPOSIBLE))
            advanceUntilIdle()
            vm.continuarConMinimo()

            val fallo = vm.estado.value.fallo as FalloArmado.Presupuesto
            val listo = awaitItem() as EventoArmado.Listo

            assertThat(listo.solicitud.presupuestoClp).isEqualTo(fallo.minimoClp)
            // Misma semilla: el usuario ve la semana que se le prometio en la hoja.
            assertThat(listo.semilla).isEqualTo(fallo.semilla)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `si el catalogo no da para tantos dias se detiene en la primera etapa`() = runTest {
        val vm = viewModel(recetas = catalogoDe(cantidad = 1))

        vm.eventos.test {
            vm.armar(solicitud())
            advanceUntilIdle()

            assertThat(vm.estado.value.fallo).isInstanceOf(FalloArmado.SinRecetas::class.java)
            assertThat(vm.estado.value.fallo?.etapa).isEqualTo(ETAPA_CATALOGO)
            expectNoEvents()
        }
    }

    @Test
    fun `armar dos veces no rearma la semana`() = runTest {
        val vm = viewModel(recetas = catalogoDe(cantidad = 5))

        vm.eventos.test {
            // La pantalla llama a armar desde un LaunchedEffect: una recomposicion no
            // puede disparar un segundo armado ni una segunda navegacion.
            vm.armar(solicitud())
            vm.armar(solicitud())

            awaitItem()
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    // --- andamiaje ----------------------------------------------------------

    private fun viewModel(recetas: List<Receta>) = ArmadoViewModel(
        generarPlan = GenerarPlanSemanalUseCase(
            catalogo = CatalogoFalso(recetas),
            precios = PreciosFalsos(INGREDIENTES),
        ),
        savedStateHandle = SavedStateHandle(),
    )

    private fun solicitud(presupuestoClp: Int = 45_000) = SolicitudPlan(
        supermercado = Supermercado.JUMBO,
        personas = PERSONAS,
        dias = listOf(DiaSemana.LUNES, DiaSemana.MARTES, DiaSemana.MIERCOLES),
        presupuestoClp = presupuestoClp,
        antojos = emptySet(),
        restriccion = Restriccion.NINGUNA,
        artefactos = setOf(Artefacto.ESTUFA),
    )

    /** Recetas distintas entre si y de bases distintas, para no chocar con el tope por base. */
    private fun catalogoDe(cantidad: Int): List<Receta> = List(cantidad) { indice ->
        Receta(
            id = "r$indice",
            nombre = "Receta $indice",
            descripcion = "Prueba",
            minutosTotal = MINUTOS,
            porcionesBase = 2,
            basePrincipal = BasePrincipal.entries[indice % BasePrincipal.entries.size],
            ingredientes = listOf(IngredienteReceta("arroz", GRAMOS_POR_PORCION)),
            pasos = listOf("Cocinar"),
            kcalPorPorcion = KCAL,
            proteinaGPorPorcion = PROTEINA,
        )
    }

    private class CatalogoFalso(private val lista: List<Receta>) : CatalogoRepository {
        override suspend fun recetas() = lista
        override suspend fun receta(id: String) = lista.firstOrNull { it.id == id }
    }

    private class PreciosFalsos(private val mapa: Map<String, Ingrediente>) : PrecioRepository {
        override suspend fun ingredientes() = mapa
        override suspend fun actualizadoEn() = "2026-01-01"
    }

    private companion object {
        /** Ni el plato mas barato del catalogo falso cabe en cien pesos. */
        const val PRESUPUESTO_IMPOSIBLE = 100

        const val PERSONAS = 4
        const val MINUTOS = 30
        const val KCAL = 600
        const val PROTEINA = 20
        const val GRAMOS_POR_PORCION = 100.0

        val INGREDIENTES = mapOf(
            "arroz" to Ingrediente(
                id = "arroz",
                nombre = "Arroz",
                pasillo = Pasillo.ABARROTES,
                unidad = Unidad.GRAMO,
                precioClpPorUnidad = 1.5,
            ),
        )
    }
}

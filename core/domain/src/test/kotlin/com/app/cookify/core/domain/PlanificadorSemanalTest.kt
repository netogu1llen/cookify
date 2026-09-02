package com.app.cookify.core.domain

import com.app.cookify.core.model.Antojo
import com.app.cookify.core.model.Artefacto
import com.app.cookify.core.model.BasePrincipal
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.IngredienteReceta
import com.app.cookify.core.model.Pasillo
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.ResultadoPlan
import com.app.cookify.core.model.Restriccion
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.model.Unidad
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests del motor con un catalogo sintetico. No usa el JSON real a proposito: aca se
 * verifica el algoritmo, no los datos (de eso se encarga CatalogoValidacionTest).
 */
class PlanificadorSemanalTest {

    // Un ingrediente que cuesta 1 peso por gramo hace que el costo de una receta sea
    // igual a la suma de sus gramos, y los numeros de los tests se leen solos.
    private val ingredientes = mapOf(
        "barato" to ingrediente("barato", 1.0),
        "caro" to ingrediente("caro", 10.0),
    )

    private val catalogo = buildList {
        // Diez recetas baratas repartidas en cinco bases distintas.
        BasePrincipal.entries.take(5).forEachIndexed { indice, base ->
            repeat(2) { copia ->
                add(
                    receta(
                        id = "barata_${base.name}_$copia",
                        base = base,
                        gramosBaratos = 100 + indice * 10,
                    ),
                )
            }
        }
        // Cinco recetas caras y bien puntuadas para los antojos.
        repeat(5) { indice ->
            add(
                receta(
                    id = "cara_$indice",
                    base = BasePrincipal.entries[indice],
                    gramosCaros = 100,
                    antojos = setOf(Antojo.RAPIDO, Antojo.ALTO_PROTEINA),
                    minutos = 20,
                    proteina = 40,
                ),
            )
        }
    }

    @Test
    fun `respeta la restriccion alimentaria`() {
        val soloVeganas = catalogo.map { it.copy(aptaPara = setOf(Restriccion.VEGANO)) }
            .take(8)
            .plus(catalogo.drop(8).map { it.copy(aptaPara = emptySet()) })

        val plan = exito(
            solicitud = solicitud(restriccion = Restriccion.VEGANO, dias = 5),
            recetas = soloVeganas,
        )

        assertThat(plan.almuerzos.map { it.receta.aptaPara })
            .containsNoneOf(emptySet<Restriccion>(), setOf(Restriccion.VEGETARIANO))
    }

    @Test
    fun `solo propone recetas que se pueden cocinar con los artefactos disponibles`() {
        val mitadHorno = catalogo.mapIndexed { indice, receta ->
            if (indice % 2 == 0) {
                receta.copy(requisitosArtefactos = listOf(setOf(Artefacto.HORNO)))
            } else {
                receta.copy(requisitosArtefactos = listOf(setOf(Artefacto.ESTUFA)))
            }
        }

        val plan = exito(
            solicitud = solicitud(artefactos = setOf(Artefacto.ESTUFA), dias = 5),
            recetas = mitadHorno,
        )

        plan.almuerzos.forEach {
            assertThat(it.receta.puedeCocinarseCon(setOf(Artefacto.ESTUFA))).isTrue()
        }
    }

    @Test
    fun `no repite recetas en la semana`() {
        val plan = exito(solicitud(dias = 7))
        val ids = plan.almuerzos.map { it.receta.id }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `asigna los dias en orden de lunes a domingo`() {
        val plan = exito(
            solicitud(dias = listOf(DiaSemana.VIERNES, DiaSemana.LUNES, DiaSemana.MIERCOLES)),
        )
        assertThat(plan.almuerzos.map { it.dia })
            .containsExactly(DiaSemana.LUNES, DiaSemana.MIERCOLES, DiaSemana.VIERNES)
            .inOrder()
    }

    @Test
    fun `el plan cabe en el presupuesto cuando es alcanzable`() {
        val plan = exito(solicitud(dias = 5, presupuesto = 30_000))
        assertThat(plan.costoTotalClp).isAtMost(30_000)
        assertThat(plan.dentroDePresupuesto).isTrue()
    }

    @Test
    fun `con presupuesto imposible informa cuanto haria falta`() {
        val resultado = PlanificadorSemanal.planificar(
            solicitud = solicitud(dias = 5, presupuesto = 1_000),
            recetas = catalogo,
            ingredientes = ingredientes,
            semilla = SEMILLA,
        )

        assertThat(resultado).isInstanceOf(ResultadoPlan.PresupuestoInsuficiente::class.java)
        val insuficiente = resultado as ResultadoPlan.PresupuestoInsuficiente
        assertThat(insuficiente.minimoNecesarioClp).isGreaterThan(1_000)
        assertThat(insuficiente.planMasBarato.almuerzos).hasSize(5)
    }

    @Test
    fun `sin recetas suficientes lo dice en vez de devolver una semana incompleta`() {
        val resultado = PlanificadorSemanal.planificar(
            solicitud = solicitud(dias = 7),
            recetas = catalogo.take(3),
            ingredientes = ingredientes,
            semilla = SEMILLA,
        )

        assertThat(resultado).isInstanceOf(ResultadoPlan.SinRecetasSuficientes::class.java)
        val sinRecetas = resultado as ResultadoPlan.SinRecetasSuficientes
        assertThat(sinRecetas.recetasDisponibles).isEqualTo(3)
        assertThat(sinRecetas.diasPedidos).isEqualTo(7)
    }

    @Test
    fun `la misma semilla produce exactamente el mismo plan`() {
        val a = exito(solicitud(dias = 5), semilla = 42L)
        val b = exito(solicitud(dias = 5), semilla = 42L)
        assertThat(a.almuerzos.map { it.receta.id }).isEqualTo(b.almuerzos.map { it.receta.id })
    }

    @Test
    fun `escala el costo con la cantidad de personas`() {
        val paraDos = exito(solicitud(dias = 3, personas = 2, presupuesto = 500_000), semilla = 7L)
        val paraCuatro = exito(solicitud(dias = 3, personas = 4, presupuesto = 500_000), semilla = 7L)

        assertThat(paraCuatro.almuerzos.map { it.receta.id })
            .isEqualTo(paraDos.almuerzos.map { it.receta.id })
        assertThat(paraCuatro.costoTotalClp).isEqualTo(paraDos.costoTotalClp * 2)
    }

    @Test
    fun `el supermercado mas caro sube el total`() {
        val lider = exito(solicitud(dias = 3, supermercado = Supermercado.LIDER), semilla = 7L)
        val jumbo = exito(solicitud(dias = 3, supermercado = Supermercado.JUMBO), semilla = 7L)
        assertThat(jumbo.costoTotalClp).isGreaterThan(lider.costoTotalClp)
    }

    @Test
    fun `prioriza las recetas que satisfacen los antojos cuando el presupuesto alcanza`() {
        val plan = exito(
            solicitud(dias = 3, presupuesto = 500_000, antojos = setOf(Antojo.ALTO_PROTEINA)),
        )
        // Las "caras" son las unicas con proteina alta; con presupuesto de sobra deben ganar.
        assertThat(plan.almuerzos.count { it.receta.id.startsWith("cara_") }).isAtLeast(2)
    }

    @Test
    fun `no repite la misma base principal mas de dos veces`() {
        val plan = exito(solicitud(dias = 7, presupuesto = 500_000))
        val porBase = plan.almuerzos.groupingBy { it.receta.basePrincipal }.eachCount()
        assertThat(porBase.values.max()).isAtMost(2)
    }

    /**
     * El presupuesto es un techo, no una meta: sin antojos que justifiquen gastar mas,
     * el motor no tiene por que encarecer la semana. Lo que si debe pasar es que dos
     * semillas distintas den semanas distintas, o "regenerar" no serviria de nada.
     */
    @Test
    fun `con presupuesto amplio varia la semana en vez de repetir siempre lo mismo`() {
        val holgado = solicitud(dias = 5, presupuesto = 500_000)

        val planes = (1L..8L)
            .map { semilla -> exito(holgado, semilla = semilla).almuerzos.map { it.receta.id } }
            .toSet()

        assertThat(planes.size).isGreaterThan(1)
        planes.forEach { assertThat(it).hasSize(5) }
    }

    @Test
    fun `nunca propone una semana que se pase del presupuesto`() {
        (1L..20L).forEach { semilla ->
            val plan = exito(solicitud(dias = 5, presupuesto = 30_000), semilla = semilla)
            assertThat(plan.costoTotalClp).isAtMost(30_000)
        }
    }

    // --- helpers ------------------------------------------------------------

    private fun exito(
        solicitud: SolicitudPlan,
        recetas: List<Receta> = catalogo,
        semilla: Long = SEMILLA,
    ) = (
        PlanificadorSemanal.planificar(solicitud, recetas, ingredientes, semilla)
            as ResultadoPlan.Exito
        ).plan

    private fun solicitud(
        dias: Int = 5,
        personas: Int = 4,
        presupuesto: Int = 100_000,
        antojos: Set<Antojo> = emptySet(),
        restriccion: Restriccion = Restriccion.NINGUNA,
        artefactos: Set<Artefacto> = Artefacto.entries.toSet(),
        supermercado: Supermercado = Supermercado.LIDER,
    ) = solicitud(
        dias = DiaSemana.entries.take(dias),
        personas = personas,
        presupuesto = presupuesto,
        antojos = antojos,
        restriccion = restriccion,
        artefactos = artefactos,
        supermercado = supermercado,
    )

    private fun solicitud(
        dias: List<DiaSemana>,
        personas: Int = 4,
        presupuesto: Int = 100_000,
        antojos: Set<Antojo> = emptySet(),
        restriccion: Restriccion = Restriccion.NINGUNA,
        artefactos: Set<Artefacto> = Artefacto.entries.toSet(),
        supermercado: Supermercado = Supermercado.LIDER,
    ) = SolicitudPlan(
        supermercado = supermercado,
        personas = personas,
        dias = dias,
        presupuestoClp = presupuesto,
        antojos = antojos,
        restriccion = restriccion,
        artefactos = artefactos,
    )

    private fun ingrediente(id: String, precio: Double) = Ingrediente(
        id = id,
        nombre = id,
        pasillo = Pasillo.ABARROTES,
        unidad = Unidad.GRAMO,
        precioClpPorUnidad = precio,
    )

    private fun receta(
        id: String,
        base: BasePrincipal,
        gramosBaratos: Int = 0,
        gramosCaros: Int = 0,
        antojos: Set<Antojo> = emptySet(),
        minutos: Int = 40,
        proteina: Int = 15,
    ) = Receta(
        id = id,
        nombre = id,
        descripcion = id,
        minutosTotal = minutos,
        porcionesBase = 4,
        basePrincipal = base,
        antojos = antojos,
        aptaPara = emptySet(),
        requisitosArtefactos = listOf(setOf(Artefacto.ESTUFA)),
        ingredientes = buildList {
            if (gramosBaratos > 0) add(IngredienteReceta("barato", gramosBaratos.toDouble()))
            if (gramosCaros > 0) add(IngredienteReceta("caro", gramosCaros.toDouble()))
        },
        pasos = listOf("paso uno"),
        kcalPorPorcion = 500,
        proteinaGPorPorcion = proteina,
    )

    private companion object {
        const val SEMILLA = 1234L
    }
}

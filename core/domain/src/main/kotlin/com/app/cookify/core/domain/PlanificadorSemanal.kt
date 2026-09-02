package com.app.cookify.core.domain

import com.app.cookify.core.model.AlmuerzoDelDia
import com.app.cookify.core.model.Antojo
import com.app.cookify.core.model.BasePrincipal
import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.PlanSemanal
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.ResultadoPlan
import com.app.cookify.core.model.SolicitudPlan
import kotlin.random.Random

/**
 * El motor: dado lo que el usuario pidio y el catalogo local, arma la semana de
 * almuerzos.
 *
 * Es Kotlin puro y determinista: la misma solicitud con la misma semilla produce
 * siempre el mismo plan. Eso lo hace testeable sin emulador y permite que
 * "regenerar semana" entregue algo distinto pero reproducible.
 */
object PlanificadorSemanal {

    /** Un antojo que la receta declara vale mas que una coincidencia numerica. */
    private const val PUNTOS_POR_ANTOJO = 3

    /** Bonus cuando ademas del tag la receta cumple la metrica dura (minutos, kcal, proteina). */
    private const val PUNTOS_POR_METRICA = 2

    private const val MINUTOS_RAPIDO = 25
    private const val KCAL_BAJO = 500
    private const val PROTEINA_ALTA = 30

    /**
     * Cuantos almuerzos pueden compartir la misma base principal. Sin este tope el
     * motor arma semanas de siete pollos: baratas, bien puntuadas y horribles.
     */
    private const val MAX_POR_BASE = 2

    /** Bajo este porcentaje del presupuesto se intenta subir de categoria algunos platos. */
    private const val UMBRAL_HOLGURA = 0.70

    fun planificar(
        solicitud: SolicitudPlan,
        recetas: List<Receta>,
        ingredientes: Map<String, Ingrediente>,
        semilla: Long,
    ): ResultadoPlan {
        val dias = solicitud.diasOrdenados
        val candidatas = recetas
            .filter { it.respeta(solicitud.restriccion) && it.puedeCocinarseCon(solicitud.artefactos) }
            .map { receta ->
                Candidata(
                    receta = receta,
                    costoClp = CalculadoraCostos.costoReceta(
                        receta = receta,
                        ingredientes = ingredientes,
                        personas = solicitud.personas,
                        supermercado = solicitud.supermercado,
                    ),
                    puntaje = puntuar(receta, solicitud.antojos),
                )
            }

        if (candidatas.size < dias.size) {
            return ResultadoPlan.SinRecetasSuficientes(
                motivo = motivoDeEscasez(solicitud),
                recetasDisponibles = candidatas.size,
                diasPedidos = dias.size,
            )
        }

        val random = Random(semilla)

        // 1. Mejor semana por gusto, ignorando el presupuesto.
        val porGusto = seleccionar(candidatas, dias.size) { a, b ->
            compareValuesBy(b, a) { it.puntaje }
                .takeIf { it != 0 }
                ?: compareValuesBy(a, b) { it.costoClp }
                    .takeIf { it != 0 }
                ?: desempate(a, b, random)
        }

        // 2. Si no cabe, se abarata hasta que quepa.
        val ajustada = ajustarAPresupuesto(porGusto, candidatas, solicitud.presupuestoClp)
        val costoAjustado = ajustada.sumOf { it.costoClp }

        if (costoAjustado > solicitud.presupuestoClp) {
            // Ni la combinacion mas barata cabe: devolvemos cuanto haria falta.
            val masBarata = seleccionar(candidatas, dias.size) { a, b ->
                compareValuesBy(a, b) { it.costoClp }
                    .takeIf { it != 0 }
                    ?: compareValuesBy(b, a) { it.puntaje }
                        .takeIf { it != 0 }
                    ?: desempate(a, b, random)
            }
            val planMasBarato = armarPlan(masBarata, dias, solicitud, semilla)
            return ResultadoPlan.PresupuestoInsuficiente(
                minimoNecesarioClp = planMasBarato.costoTotalClp,
                planMasBarato = planMasBarato,
            )
        }

        // 3. Si sobra mucho, se sube de categoria en vez de dejar plata sin usar.
        val final = if (costoAjustado < solicitud.presupuestoClp * UMBRAL_HOLGURA) {
            aprovecharHolgura(ajustada, candidatas, solicitud.presupuestoClp)
        } else {
            ajustada
        }

        return ResultadoPlan.Exito(armarPlan(final, dias, solicitud, semilla))
    }

    // --- puntuacion ---------------------------------------------------------

    private fun puntuar(receta: Receta, antojos: Set<Antojo>): Int = antojos.sumOf { antojo ->
        val porTag = if (antojo in receta.antojos) PUNTOS_POR_ANTOJO else 0
        val porMetrica = when (antojo) {
            Antojo.RAPIDO -> if (receta.minutosTotal <= MINUTOS_RAPIDO) PUNTOS_POR_METRICA else 0
            Antojo.BAJO_CALORIAS -> if (receta.kcalPorPorcion <= KCAL_BAJO) PUNTOS_POR_METRICA else 0
            Antojo.ALTO_PROTEINA ->
                if (receta.proteinaGPorPorcion >= PROTEINA_ALTA) PUNTOS_POR_METRICA else 0
            else -> 0
        }
        porTag + porMetrica
    }

    /**
     * Desempate estable pero variable: dos recetas igual de buenas e igual de caras se
     * ordenan segun la semilla, para que "regenerar" cambie la semana.
     */
    private fun desempate(a: Candidata, b: Candidata, random: Random): Int =
        compareValues(mezclar(a.receta.id, random), mezclar(b.receta.id, random))

    private fun mezclar(id: String, random: Random): Int = id.hashCode() xor random.nextInt()

    // --- seleccion ----------------------------------------------------------

    /**
     * Elige [cantidad] recetas distintas segun el [comparador], respetando el tope de
     * platos por base principal. Si el tope impide llenar la semana (por ejemplo vegano
     * con pocas bases disponibles) se relaja progresivamente en vez de fallar.
     */
    private fun seleccionar(
        candidatas: List<Candidata>,
        cantidad: Int,
        comparador: Comparator<Candidata>,
    ): List<Candidata> {
        val ordenadas = candidatas.sortedWith(comparador)
        var tope = MAX_POR_BASE
        while (true) {
            val elegidas = tomarConTope(ordenadas, cantidad, tope)
            if (elegidas.size == cantidad || tope >= cantidad) return elegidas
            tope++
        }
    }

    private fun tomarConTope(
        ordenadas: List<Candidata>,
        cantidad: Int,
        topePorBase: Int,
    ): List<Candidata> {
        val elegidas = mutableListOf<Candidata>()
        val usoPorBase = mutableMapOf<BasePrincipal, Int>()
        for (candidata in ordenadas) {
            if (elegidas.size == cantidad) break
            val base = candidata.receta.basePrincipal
            if (usoPorBase.getOrDefault(base, 0) >= topePorBase) continue
            elegidas += candidata
            usoPorBase[base] = usoPorBase.getOrDefault(base, 0) + 1
        }
        return elegidas
    }

    private fun respetaTope(seleccion: List<Candidata>, entrante: Candidata, saliente: Candidata): Boolean {
        if (entrante.receta.basePrincipal == saliente.receta.basePrincipal) return true
        val usos = seleccion.count { it.receta.basePrincipal == entrante.receta.basePrincipal }
        return usos < MAX_POR_BASE
    }

    // --- ajuste al presupuesto ---------------------------------------------

    /**
     * Reemplaza platos caros por alternativas mas baratas, sacrificando lo menos posible
     * de puntaje, hasta que la semana quepa en el presupuesto o no queden reemplazos.
     */
    private fun ajustarAPresupuesto(
        seleccion: List<Candidata>,
        candidatas: List<Candidata>,
        presupuesto: Int,
    ): List<Candidata> {
        var actual = seleccion
        while (actual.sumOf { it.costoClp } > presupuesto) {
            val exceso = actual.sumOf { it.costoClp } - presupuesto
            val cambio = mejorAbaratamiento(actual, candidatas, exceso) ?: break
            actual = actual.map { if (it.receta.id == cambio.first.receta.id) cambio.second else it }
        }
        return actual
    }

    /**
     * Busca el cambio que mas ahorra por unidad de puntaje perdido. Prefiere los que ya
     * cierran la brecha de una vez, para no encadenar reemplazos innecesarios.
     */
    private fun mejorAbaratamiento(
        seleccion: List<Candidata>,
        candidatas: List<Candidata>,
        excesoClp: Int,
    ): Pair<Candidata, Candidata>? {
        val enUso = seleccion.mapTo(mutableSetOf()) { it.receta.id }
        var mejor: Pair<Candidata, Candidata>? = null
        var mejorValor = Double.NEGATIVE_INFINITY

        for (saliente in seleccion) {
            for (entrante in candidatas) {
                if (entrante.receta.id in enUso) continue
                val ahorro = saliente.costoClp - entrante.costoClp
                if (ahorro <= 0) continue
                if (!respetaTope(seleccion, entrante, saliente)) continue

                val perdida = (saliente.puntaje - entrante.puntaje).coerceAtLeast(0)
                // Ahorro por punto sacrificado; el +1 evita dividir por cero y premia
                // levemente los cambios que no cuestan puntaje.
                val eficiencia = ahorro.toDouble() / (perdida + 1)
                // Cerrar la brecha de un viaje vale mas que ahorrar de a poco.
                val valor = if (ahorro >= excesoClp) eficiencia * 2 else eficiencia

                if (valor > mejorValor) {
                    mejorValor = valor
                    mejor = saliente to entrante
                }
            }
        }
        return mejor
    }

    /**
     * Con presupuesto de sobra, sube de categoria los platos peor puntuados mientras la
     * plata alcance. Dejar 40% del presupuesto sin usar no le sirve a nadie.
     */
    private fun aprovecharHolgura(
        seleccion: List<Candidata>,
        candidatas: List<Candidata>,
        presupuesto: Int,
    ): List<Candidata> {
        var actual = seleccion
        while (true) {
            val disponible = presupuesto - actual.sumOf { it.costoClp }
            val enUso = actual.mapTo(mutableSetOf()) { it.receta.id }

            val mejora = actual
                .sortedBy { it.puntaje }
                .firstNotNullOfOrNull { saliente ->
                    candidatas
                        .filter { it.receta.id !in enUso }
                        .filter { it.puntaje > saliente.puntaje }
                        .filter { it.costoClp - saliente.costoClp <= disponible }
                        .filter { respetaTope(actual, it, saliente) }
                        .maxByOrNull { it.puntaje }
                        ?.let { saliente to it }
                } ?: break

            actual = actual.map { if (it.receta.id == mejora.first.receta.id) mejora.second else it }
        }
        return actual
    }

    // --- armado -------------------------------------------------------------

    private fun armarPlan(
        seleccion: List<Candidata>,
        dias: List<com.app.cookify.core.model.DiaSemana>,
        solicitud: SolicitudPlan,
        semilla: Long,
    ): PlanSemanal = PlanSemanal(
        almuerzos = dias.zip(seleccion) { dia, candidata ->
            AlmuerzoDelDia(dia = dia, receta = candidata.receta, costoClp = candidata.costoClp)
        },
        solicitud = solicitud,
        semilla = semilla,
    )

    private fun motivoDeEscasez(solicitud: SolicitudPlan): String = when {
        solicitud.artefactos.size == 1 ->
            "Con solo ${solicitud.artefactos.first().etiqueta.lowercase()} quedan muy pocas recetas."
        solicitud.restriccion != com.app.cookify.core.model.Restriccion.NINGUNA ->
            "No hay suficientes recetas ${solicitud.restriccion.etiqueta.lowercase()} " +
                "para los artefactos que tienes."
        else -> "No hay suficientes recetas para armar todos los dias que pediste."
    }

    private data class Candidata(
        val receta: Receta,
        val costoClp: Int,
        val puntaje: Int,
    )
}

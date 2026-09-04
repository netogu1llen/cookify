package com.app.cookify.feature.planning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.cookify.core.common.aPesos
import com.app.cookify.core.domain.GenerarPlanSemanalUseCase
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.ResultadoPlan
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.ui.componentes.EstadoCheck
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Arma la semana y escenifica la espera.
 *
 * El motor termina en milisegundos, así que las cuatro etapas son deliberadamente
 * temporizadas. No es adorno: es el único momento en que la app puede mostrar que
 * entendió lo que el usuario pidió, y llegar al resultado de golpe hace que la
 * semana parezca sacada de un sombrero en vez de calculada.
 */
@HiltViewModel
class ArmadoViewModel @Inject constructor(
    private val generarPlan: GenerarPlanSemanalUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _estado = MutableStateFlow(ArmadoUiState())
    val estado: StateFlow<ArmadoUiState> = _estado.asStateFlow()

    private val _eventos = Channel<EventoArmado>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    private var trabajo: Job? = null

    /**
     * Idempotente: la pantalla la llama desde un LaunchedEffect y una recomposición o
     * un cambio de configuración no deben rearmar la semana.
     */
    fun armar(solicitud: SolicitudPlan) {
        if (trabajo != null) return
        trabajo = viewModelScope.launch { correr(solicitud) }
    }

    /** Sigue con el presupuesto que de verdad alcanza, sin rehacer el cuestionario. */
    fun continuarConMinimo() {
        val fallo = _estado.value.fallo as? FalloArmado.Presupuesto ?: return
        viewModelScope.launch {
            _eventos.send(EventoArmado.Listo(fallo.solicitudMinima, fallo.semilla))
        }
    }

    private suspend fun correr(solicitud: SolicitudPlan) {
        _estado.value = ArmadoUiState(
            etapas = textosDe(solicitud).map { EtapaArmado(it, EstadoCheck.PENDIENTE) },
        )

        // La primera etapa arranca antes de tocar el catálogo: la lectura de assets
        // en frío puede tardar y la pantalla no puede quedarse muda mientras tanto.
        marcar(ETAPA_CATALOGO, EstadoCheck.EN_CURSO)

        // La semilla se guarda apenas se elige: si el proceso muere en medio del
        // armado, al volver se reconstruye la misma semana y no otra.
        val semilla = savedStateHandle.get<Long>(CLAVE_SEMILLA)
            ?: Random.nextLong().also { savedStateHandle[CLAVE_SEMILLA] = it }

        val resultado = generarPlan(solicitud, semilla)
        val fallo = falloDe(resultado, solicitud, semilla)

        _estado.value.etapas.indices.forEach { indice ->
            marcar(indice, EstadoCheck.EN_CURSO)
            delay(DURACION_ETAPA_MS)

            if (fallo != null && fallo.etapa == indice) {
                _estado.update { it.copy(fallo = fallo) }
                return
            }
            marcar(indice, EstadoCheck.LISTO)
        }

        // Un respiro para que el último check alcance a rebotar antes de irse.
        delay(PAUSA_FINAL_MS)
        _eventos.send(EventoArmado.Listo(solicitud, semilla))
    }

    private fun marcar(indice: Int, estado: EstadoCheck) = _estado.update { actual ->
        actual.copy(
            etapas = actual.etapas.mapIndexed { i, etapa ->
                if (i == indice) etapa.copy(estado = estado) else etapa
            },
        )
    }

    private fun falloDe(
        resultado: ResultadoPlan,
        solicitud: SolicitudPlan,
        semilla: Long,
    ): FalloArmado? = when (resultado) {
        is ResultadoPlan.Exito -> null

        is ResultadoPlan.PresupuestoInsuficiente -> FalloArmado.Presupuesto(
            presupuestoClp = solicitud.presupuestoClp,
            minimoClp = resultado.minimoNecesarioClp,
            solicitudMinima = solicitud.copy(presupuestoClp = resultado.minimoNecesarioClp),
            semilla = semilla,
        )

        is ResultadoPlan.SinRecetasSuficientes -> FalloArmado.SinRecetas(
            motivo = resultado.motivo,
            disponibles = resultado.recetasDisponibles,
            diasPedidos = resultado.diasPedidos,
        )
    }

    private fun textosDe(solicitud: SolicitudPlan): List<String> = listOf(
        "Revisando el catálogo de ${solicitud.supermercado.etiqueta}",
        "Calzando platos con tus ${solicitud.presupuestoClp.aPesos()}",
        "Ordenando los almuerzos ${DiaSemana.frase(solicitud.diasOrdenados)}",
        "Armando tu lista de compras",
    )

    private companion object {
        const val CLAVE_SEMILLA = "armado_semilla"

        /**
         * Suficiente para leer la línea, corto para no aburrir. Bajo ~600 ms el texto
         * pasa antes de que alcance a leerse y la escena deja de comunicar.
         */
        const val DURACION_ETAPA_MS = 750L
        const val PAUSA_FINAL_MS = 400L
    }
}

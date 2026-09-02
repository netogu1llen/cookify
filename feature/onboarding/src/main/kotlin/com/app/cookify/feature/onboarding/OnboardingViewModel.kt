package com.app.cookify.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.cookify.core.model.Antojo
import com.app.cookify.core.model.Artefacto
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.Restriccion
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.model.Supermercado
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _estado = MutableStateFlow(leerEstadoGuardado())
    val estado: StateFlow<OnboardingUiState> = _estado.asStateFlow()

    /**
     * Eventos de una sola vez. Channel y no StateFlow: terminar el cuestionario debe
     * navegar exactamente una vez, no volver a dispararse tras una rotacion.
     */
    private val _eventos = Channel<EventoOnboarding>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    fun elegirSupermercado(valor: Supermercado) = actualizar { it.copy(supermercado = valor) }

    fun cambiarPersonas(delta: Int) = actualizar {
        val nuevo = (it.personas + delta).coerceIn(SolicitudPlan.RANGO_PERSONAS)
        it.copy(personas = nuevo)
    }

    fun alternarDia(dia: DiaSemana) = actualizar {
        it.copy(dias = if (dia in it.dias) it.dias - dia else it.dias + dia)
    }

    fun cambiarPresupuesto(valor: Int?) = actualizar { it.copy(presupuestoClp = valor) }

    /**
     * Deseleccionar siempre se permite; seleccionar solo mientras no se llegue al
     * tope. Asi el usuario nunca queda trancado sin poder cambiar de opinion.
     */
    fun alternarAntojo(antojo: Antojo) = actualizar {
        when {
            antojo in it.antojos -> it.copy(antojos = it.antojos - antojo)
            it.antojos.size < Antojo.MAXIMO_SELECCION -> it.copy(antojos = it.antojos + antojo)
            else -> it
        }
    }

    fun elegirRestriccion(valor: Restriccion) = actualizar { it.copy(restriccion = valor) }

    fun alternarArtefacto(artefacto: Artefacto) = actualizar {
        val nuevos = if (artefacto in it.artefactos) {
            it.artefactos - artefacto
        } else {
            it.artefactos + artefacto
        }
        it.copy(artefactos = nuevos)
    }

    fun avanzar() {
        val actual = _estado.value
        if (!actual.puedeAvanzar) return

        if (actual.esUltimoPaso) {
            viewModelScope.launch { _eventos.send(EventoOnboarding.Terminado(actual.aSolicitud())) }
        } else {
            actualizar { it.copy(paso = PasoOnboarding.entries[it.paso.ordinal + 1]) }
        }
    }

    /** @return true si retrocedio dentro del wizard; false si hay que salir de la pantalla. */
    fun retroceder(): Boolean {
        val actual = _estado.value
        if (actual.paso.ordinal == 0) return false
        actualizar { it.copy(paso = PasoOnboarding.entries[it.paso.ordinal - 1]) }
        return true
    }

    private fun actualizar(bloque: (OnboardingUiState) -> OnboardingUiState) {
        val nuevo = bloque(_estado.value)
        _estado.value = nuevo
        savedStateHandle[CLAVE_ESTADO] = json.encodeToString(nuevo)
    }

    private fun leerEstadoGuardado(): OnboardingUiState =
        savedStateHandle.get<String>(CLAVE_ESTADO)
            ?.let { runCatching { json.decodeFromString<OnboardingUiState>(it) }.getOrNull() }
            ?: OnboardingUiState()

    private companion object {
        const val CLAVE_ESTADO = "onboarding_estado"
        val json = Json { ignoreUnknownKeys = true }
    }
}

sealed interface EventoOnboarding {
    data class Terminado(val solicitud: SolicitudPlan) : EventoOnboarding
}

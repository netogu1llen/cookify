package com.app.cookify.feature.week

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.cookify.core.common.nombrePorDefectoSemana
import com.app.cookify.core.domain.GenerarPlanSemanalUseCase
import com.app.cookify.core.domain.SemanaRepository
import com.app.cookify.core.model.ResultadoPlan
import com.app.cookify.core.model.SolicitudPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reconstruye la semana a partir de la solicitud y la semilla que trae la ruta, y la
 * guarda cuando el usuario lo pide.
 *
 * "Regenerar" es cambiar la semilla y volver a pedirle el plan al mismo motor: por eso
 * entrega una semana distinta pero reproducible, y por eso la semilla vigente se
 * guarda en el estado y no en la ruta, que quedó fija al navegar.
 */
@HiltViewModel
class SemanaViewModel @Inject constructor(
    private val generarPlan: GenerarPlanSemanalUseCase,
    private val semanas: SemanaRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _estado = MutableStateFlow(SemanaUiState())
    val estado: StateFlow<SemanaUiState> = _estado.asStateFlow()

    private val _eventos = Channel<EventoSemana>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    private var solicitud: SolicitudPlan? = null
    private var idGuardada: Long? = null

    /** La semilla que se está mostrando. Sobrevive rotación y muerte de proceso. */
    private var semilla: Long
        get() = savedStateHandle.get<Long>(CLAVE_SEMILLA) ?: 0L
        set(valor) {
            savedStateHandle[CLAVE_SEMILLA] = valor
        }

    /** Idempotente: la pantalla la llama desde un LaunchedEffect. */
    fun cargar(solicitud: SolicitudPlan, semillaInicial: Long) {
        if (this.solicitud != null) return
        this.solicitud = solicitud
        // Si veníamos de una regeneración previa, se respeta esa y no la de la ruta.
        if (savedStateHandle.get<Long>(CLAVE_SEMILLA) == null) semilla = semillaInicial
        armar()
    }

    /** Reabre una semana guardada tal como quedo, sin pasarla por el motor. */
    fun cargarGuardada(id: Long) {
        if (idGuardada != null) return
        idGuardada = id

        viewModelScope.launch {
            val guardada = semanas.porId(id)
            _estado.value = SemanaUiState(
                cargando = false,
                plan = guardada?.plan,
                error = if (guardada == null) "Esta semana ya no existe." else null,
                soloLectura = true,
            )
        }
    }

    fun borrar() {
        val id = idGuardada ?: return
        viewModelScope.launch {
            semanas.borrar(id)
            _eventos.send(EventoSemana.Borrada)
        }
    }

    fun regenerar() {
        semilla = Random.nextLong()
        armar()
    }

    fun pedirNombre() = _estado.update {
        it.copy(
            pidiendoNombre = true,
            nombrePropuesto = nombrePorDefectoSemana(System.currentTimeMillis()),
        )
    }

    fun cambiarNombre(valor: String) = _estado.update { it.copy(nombrePropuesto = valor) }

    fun cancelarGuardado() = _estado.update { it.copy(pidiendoNombre = false) }

    fun guardar() {
        val plan = _estado.value.plan ?: return
        val nombre = _estado.value.nombrePropuesto.trim().ifEmpty {
            nombrePorDefectoSemana(System.currentTimeMillis())
        }

        viewModelScope.launch {
            _estado.update { it.copy(pidiendoNombre = false, guardando = true) }
            semanas.guardar(nombre, plan)
            _estado.update { it.copy(guardando = false, guardada = true) }

            // El check de éxito alcanza a verse antes de salir. Guardar es el momento
            // en que la app cumple lo que prometió; irse de inmediato lo desperdicia.
            delay(DURACION_CELEBRACION_MS)
            _eventos.send(EventoSemana.Guardada)
        }
    }

    private fun armar() {
        val solicitud = solicitud ?: return
        _estado.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch {
            val resultado = generarPlan(solicitud, semilla)
            _estado.update { actual ->
                when (resultado) {
                    is ResultadoPlan.Exito ->
                        actual.copy(cargando = false, plan = resultado.plan, error = null)

                    // Se llega acá solo cuando el usuario aceptó seguir sabiendo que no
                    // alcanzaba: se muestra la semana más barata con la barra en rojo.
                    is ResultadoPlan.PresupuestoInsuficiente -> actual.copy(
                        cargando = false,
                        plan = resultado.planMasBarato,
                        error = null,
                    )

                    is ResultadoPlan.SinRecetasSuficientes ->
                        actual.copy(cargando = false, plan = null, error = resultado.motivo)
                }
            }
        }
    }

    private companion object {
        const val CLAVE_SEMILLA = "semana_semilla"
        const val DURACION_CELEBRACION_MS = 1_200L
    }
}

/** Evento de una sola vez: la semana quedó guardada y hay que volver a la home. */
sealed interface EventoSemana {
    data object Guardada : EventoSemana
    data object Borrada : EventoSemana
}

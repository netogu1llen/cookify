package com.app.cookify.feature.ajustes

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.cookify.core.domain.ServicioRepository
import com.app.cookify.core.model.PreciosGuardados
import com.app.cookify.core.model.PruebaDeConexion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AjustesUiState(
    val direccion: String = "",
    val probando: Boolean = false,
    /** Nulo mientras nadie haya probado: no se sale a la red sin que lo pidan. */
    val prueba: PruebaDeConexion? = null,
    val guardados: List<PreciosGuardados> = emptyList(),
    val olvidando: Boolean = false,
    /** Mensaje de una sola vez, para confirmar que algo pasó. */
    val aviso: String? = null,
)

/**
 * La pantalla de diagnóstico del servicio.
 *
 * No prueba la conexión al abrirse. Una pantalla que sale a la red sola haría
 * imposible distinguir "el servicio responde" de "el servicio respondía hace un
 * rato": la prueba vale justamente porque alguien la pidió en ese instante.
 */
@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val servicio: ServicioRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(AjustesUiState(direccion = servicio.direccion))
    val estado: StateFlow<AjustesUiState> = _estado.asStateFlow()

    init {
        refrescarGuardados()
    }

    fun probarConexion() {
        if (_estado.value.probando) return
        viewModelScope.launch {
            _estado.update { it.copy(probando = true, prueba = null, aviso = null) }
            val resultado = servicio.probarConexion()
            _estado.update { it.copy(probando = false, prueba = resultado) }
        }
    }

    /**
     * Borra los precios guardados para que la próxima semana vuelva a pedirlos.
     *
     * Es la única forma honesta de comprobar el camino completo: con la caché
     * llena, la app arma la semana sin tocar la red y el resultado no prueba
     * nada sobre la conexión.
     */
    fun olvidarPrecios() {
        if (_estado.value.olvidando) return
        viewModelScope.launch {
            _estado.update { it.copy(olvidando = true, aviso = null) }
            servicio.olvidarPreciosGuardados()
            _estado.update {
                it.copy(
                    olvidando = false,
                    guardados = emptyList(),
                    aviso = "Listo. La próxima semana que armes va a pedirle los precios al servicio.",
                )
            }
        }
    }

    fun descartarAviso() = _estado.update { it.copy(aviso = null) }

    private fun refrescarGuardados() {
        viewModelScope.launch {
            val guardados = servicio.preciosGuardados()
            _estado.update { it.copy(guardados = guardados) }
        }
    }
}

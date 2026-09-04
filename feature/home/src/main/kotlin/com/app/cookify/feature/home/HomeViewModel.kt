package com.app.cookify.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.cookify.core.domain.SemanaRepository
import com.app.cookify.core.model.SemanaGuardada
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Estado de la home.
 *
 * [cargando] arranca en true y no en "lista vacía": mostrar el estado vacío por un
 * frame mientras Room contesta haría parpadear "Todavía no tienes semanas" cada vez
 * que el usuario abre la app teniéndolas.
 */
@Immutable
data class HomeUiState(
    val cargando: Boolean = true,
    val semanas: List<SemanaGuardada> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repositorio: SemanaRepository,
) : ViewModel() {

    val estado: StateFlow<HomeUiState> = repositorio.observar()
        .map { HomeUiState(cargando = false, semanas = it) }
        .stateIn(
            scope = viewModelScope,
            // Se mantiene viva un rato tras perder la suscripcion: al volver de ver una
            // semana la lista ya esta ahi y no hay un parpadeo de carga.
            started = SharingStarted.WhileSubscribed(TIEMPO_VIVO_MS),
            initialValue = HomeUiState(),
        )

    private companion object {
        const val TIEMPO_VIVO_MS = 5_000L
    }
}

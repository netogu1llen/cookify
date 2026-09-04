package com.app.cookify.feature.week

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.cookify.core.domain.CalculadoraCostos
import com.app.cookify.core.domain.CatalogoRepository
import com.app.cookify.core.domain.PrecioRepository
import com.app.cookify.core.model.IngredienteCalculado
import com.app.cookify.core.model.Pasillo
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.Supermercado
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * El detalle de un plato, con las cantidades ya escaladas a los comensales.
 *
 * Escalar es el punto entero de la pantalla: la receta del catálogo está por porción,
 * y quien va al súper necesita el kilo y medio que tiene que comprar, no los 375 g de
 * una porción multiplicados mentalmente por cuatro.
 */
@Immutable
data class DetalleUiState(
    val cargando: Boolean = true,
    val receta: Receta? = null,
    val personas: Int = 1,
    val costoClp: Int = 0,
    val ingredientesPorPasillo: List<GrupoPasillo> = emptyList(),
)

@Immutable
data class GrupoPasillo(
    val pasillo: Pasillo,
    val ingredientes: List<IngredienteCalculado>,
)

@HiltViewModel
class DetalleViewModel @Inject constructor(
    private val catalogo: CatalogoRepository,
    private val precios: PrecioRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(DetalleUiState())
    val estado: StateFlow<DetalleUiState> = _estado.asStateFlow()

    private var cargada = false

    /** Idempotente: la pantalla la llama desde un LaunchedEffect. */
    fun cargar(recetaId: String, personas: Int, supermercado: Supermercado) {
        if (cargada) return
        cargada = true

        viewModelScope.launch {
            val receta = catalogo.receta(recetaId)
            if (receta == null) {
                _estado.value = DetalleUiState(cargando = false)
                return@launch
            }

            val ingredientes = precios.ingredientes()
            val calculados = CalculadoraCostos.ingredientesCalculados(
                receta = receta,
                ingredientes = ingredientes,
                personas = personas,
                supermercado = supermercado,
            )

            _estado.value = DetalleUiState(
                cargando = false,
                receta = receta,
                personas = personas,
                costoClp = CalculadoraCostos.costoReceta(
                    receta = receta,
                    ingredientes = ingredientes,
                    personas = personas,
                    supermercado = supermercado,
                ),
                // Agrupados por pasillo y en el orden del enum, que es el orden en que
                // se recorre el supermercado: la lista sirve para comprar, no solo para
                // leer.
                ingredientesPorPasillo = calculados
                    .groupBy { it.ingrediente.pasillo }
                    .toSortedMap(compareBy { it.ordinal })
                    .map { (pasillo, lista) -> GrupoPasillo(pasillo, lista) },
            )
        }
    }
}

package com.app.cookify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.ui.theme.CookifyTheme
import com.app.cookify.feature.home.PantallaHome
import com.app.cookify.feature.onboarding.PantallaOnboarding
import com.app.cookify.feature.planning.PantallaArmado
import com.app.cookify.feature.week.PantallaDetalle
import com.app.cookify.feature.week.PantallaSemana
import com.app.cookify.feature.week.PantallaSemanaGuardada
import com.app.cookify.navegacion.Ruta
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CookifyTheme {
                CookifyApp()
            }
        }
    }
}

/**
 * Shell de navegacion. Navigation3 trabaja sobre una lista mutable de claves: navegar
 * es agregar al final y volver es sacar el ultimo, sin grafo declarado aparte.
 */
@Composable
private fun CookifyApp() {
    val backStack = rememberNavBackStack(Ruta.Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider<NavKey> {
            entry<Ruta.Home> {
                PantallaHome(
                    onNuevaSemana = { backStack.add(Ruta.Onboarding) },
                    onAbrirSemana = { id -> backStack.add(Ruta.SemanaGuardada(id)) },
                )
            }

            entry<Ruta.Onboarding> {
                PantallaOnboarding(
                    // El armado se apila encima del cuestionario en vez de
                    // reemplazarlo: si falla, "cambiar mis respuestas" es un pop y el
                    // usuario encuentra sus siete respuestas donde las dejo.
                    onTerminado = { solicitud -> backStack.add(Ruta.Armado(solicitud)) },
                    onSalir = { backStack.removeLastOrNull() },
                )
            }

            entry<Ruta.Armado> { ruta ->
                PantallaArmado(
                    solicitud = ruta.solicitud,
                    onListo = { solicitud, semilla ->
                        backStack.irASemana(solicitud, semilla)
                    },
                    onCambiarRespuestas = { backStack.removeLastOrNull() },
                )
            }

            entry<Ruta.Semana> { ruta ->
                PantallaSemana(
                    solicitud = ruta.solicitud,
                    semilla = ruta.semilla,
                    onAbrirReceta = { recetaId, personas, supermercado ->
                        backStack.add(Ruta.Detalle(recetaId, personas, supermercado))
                    },
                    // Al guardar se limpia todo el camino y se vuelve a la home,
                    // que ahora tiene la semana recien guardada arriba.
                    onGuardada = { backStack.removeAll { it != Ruta.Home } },
                )
            }

            entry<Ruta.SemanaGuardada> { ruta ->
                PantallaSemanaGuardada(
                    id = ruta.id,
                    onAbrirReceta = { recetaId, personas, supermercado ->
                        backStack.add(Ruta.Detalle(recetaId, personas, supermercado))
                    },
                    onBorrada = { backStack.removeLastOrNull() },
                )
            }

            entry<Ruta.Detalle> { ruta ->
                PantallaDetalle(
                    recetaId = ruta.recetaId,
                    personas = ruta.personas,
                    supermercado = ruta.supermercado,
                    onVolver = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

/**
 * Deja el back stack en [Home, Semana].
 *
 * Se descartan el armado y el cuestionario: desde la semana lista, volver atras tiene
 * que llevar a la home, no a rearmar la misma semana ni a responder de nuevo.
 */
private fun NavBackStack<NavKey>.irASemana(solicitud: SolicitudPlan, semilla: Long) {
    val semana = Ruta.Semana(solicitud, semilla)
    removeAll { it is Ruta.Armado || it is Ruta.Onboarding }
    add(semana)
}

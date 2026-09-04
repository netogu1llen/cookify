package com.app.cookify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.ui.componentes.EstadoVacio
import com.app.cookify.core.ui.theme.CookifyTheme
import com.app.cookify.feature.onboarding.PantallaOnboarding
import com.app.cookify.feature.planning.PantallaArmado
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
                PantallaHomeVacia(onNuevaSemana = { backStack.add(Ruta.Onboarding) })
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
                PantallaSemanaProvisoria(ruta)
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

/** Home provisoria: se reemplaza por :feature:home cuando existan semanas guardadas. */
@Composable
private fun PantallaHomeVacia(onNuevaSemana: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { relleno ->
        EstadoVacio(
            titulo = "Todavía no tienes semanas",
            mensaje = "Arma tu primera semana de almuerzos y te decimos cuánto te va a costar en el súper.",
            textoAccion = "Armar mi primera semana",
            onAccion = onNuevaSemana,
            modifier = Modifier.padding(relleno),
        )
    }
}

/** Provisoria hasta la fase 8, que trae :feature:week con la semana y el detalle. */
@Composable
private fun PantallaSemanaProvisoria(ruta: Ruta.Semana) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { relleno ->
        Text(
            text = "Semana lista para ${ruta.solicitud.personas} personas " +
                "(semilla ${ruta.semilla})",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(relleno).padding(24.dp),
        )
    }
}

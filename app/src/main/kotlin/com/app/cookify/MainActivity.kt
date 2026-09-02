package com.app.cookify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.app.cookify.core.ui.componentes.EstadoVacio
import com.app.cookify.core.ui.theme.CookifyTheme
import com.app.cookify.feature.onboarding.PantallaOnboarding
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
                    onTerminado = { solicitud ->
                        // TODO(fase 7): navegar a la pantalla de armado con esta solicitud.
                        backStack.removeLastOrNull()
                    },
                    onSalir = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

/** Home provisoria: se reemplaza por :feature:home cuando existan semanas guardadas. */
@Composable
private fun PantallaHomeVacia(onNuevaSemana: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { relleno ->
        EstadoVacio(
            titulo = "Todavia no tienes semanas",
            mensaje = "Arma tu primera semana de almuerzos y te decimos cuanto te va a costar en el super.",
            textoAccion = "Armar mi primera semana",
            onAccion = onNuevaSemana,
            modifier = Modifier.padding(relleno),
        )
    }
}

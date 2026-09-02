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
import com.app.cookify.core.ui.componentes.EstadoVacio
import com.app.cookify.core.ui.theme.CookifyTheme
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
 * Shell de la app.
 *
 * Por ahora muestra el estado vacio de la home. Cuando existan los modulos de
 * feature, aca vive el NavDisplay de Navigation3 con su back stack, y la clave
 * inicial se decide segun si hay semanas guardadas.
 */
@Composable
private fun CookifyApp() {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        EstadoVacio(
            titulo = "Todavia no tienes semanas",
            mensaje = "Arma tu primera semana de almuerzos y te decimos cuanto te va a costar en el super.",
            textoAccion = "Armar mi primera semana",
            onAccion = { },
            modifier = Modifier.padding(relleno),
        )
    }
}

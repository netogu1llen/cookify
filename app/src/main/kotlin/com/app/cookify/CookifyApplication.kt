package com.app.cookify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada del grafo de Hilt. Registrada en el manifest como android:name. */
@HiltAndroidApp
class CookifyApplication : Application()

package com.app.cookify.core.common

import javax.inject.Qualifier

/**
 * Dispatchers inyectados en vez de referenciados directo, para poder cambiarlos por
 * un dispatcher de test y no depender de Dispatchers.IO real en los unit tests.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: CookifyDispatcher)

enum class CookifyDispatcher {
    /** Lectura de assets y base de datos. */
    IO,

    /** Trabajo de CPU: el motor de planificacion. */
    DEFAULT,
}

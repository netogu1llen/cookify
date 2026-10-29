package com.app.cookify.server.config

/**
 * Toda la configuracion del servicio, leida del entorno al arrancar.
 *
 * Nada de esto esta escrito en el codigo. La razon no es elegancia: una clave
 * en el repositorio hay que rotarla, no borrarla, porque el historial de git la
 * conserva. El archivo .env.example documenta cada variable con un valor de
 * ejemplo y es lo unico que se versiona.
 *
 * Si falta una variable obligatoria el servicio **no arranca**. Arrancar con
 * una clave vacia significaria que la API queda abierta a escritura y nadie se
 * daria cuenta hasta que alguien escriba.
 */
data class Configuracion(
    val puerto: Int,
    val baseDeDatos: BaseDeDatos,
    val claveApi: String,
    val recolector: Recolector,
) {
    data class BaseDeDatos(
        val url: String,
        val usuario: String,
        val clave: String,
    )

    data class Recolector(
        val paginasMaximas: Int,
        val pausaEntrePeticionesMs: Long,
    )

    companion object {
        fun desdeEntorno(entorno: (String) -> String? = System::getenv): Configuracion {
            fun obligatoria(nombre: String): String =
                entorno(nombre)?.takeIf { it.isNotBlank() }
                    ?: error(
                        "Falta la variable de entorno $nombre. " +
                            "Copiar .env.example como .env y completar los valores."
                    )

            fun entera(nombre: String, porOmision: Int): Int =
                entorno(nombre)?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: porOmision

            val clave = obligatoria("COOKIFY_API_KEY")
            // Se informa el largo y nunca el valor: un mensaje de arranque
            // termina en un registro, y un registro se comparte.
            require(clave.length >= 16) {
                "COOKIFY_API_KEY tiene ${clave.length} caracteres y necesita al menos 16. " +
                    "Generar una con: openssl rand -hex 24"
            }

            return Configuracion(
                puerto = entera("COOKIFY_PUERTO", 8080),
                baseDeDatos = BaseDeDatos(
                    url = obligatoria("COOKIFY_DB_URL"),
                    usuario = obligatoria("COOKIFY_DB_USUARIO"),
                    clave = obligatoria("COOKIFY_DB_CLAVE"),
                ),
                claveApi = clave,
                recolector = Recolector(
                    paginasMaximas = entera("COOKIFY_RECOLECTOR_PAGINAS", 4),
                    pausaEntrePeticionesMs = entera("COOKIFY_RECOLECTOR_PAUSA_MS", 400).toLong(),
                ),
            )
        }
    }
}

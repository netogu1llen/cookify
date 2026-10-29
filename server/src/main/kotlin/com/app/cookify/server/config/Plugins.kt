package com.app.cookify.server.config

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

/**
 * Los plugins del servidor: serializacion, registro, errores y la clave de la API.
 *
 * Todo lo transversal vive aqui y no repartido por las rutas. La diferencia es
 * concreta: con la clave verificada en un interceptor, una ruta de escritura
 * nueva queda protegida por omision; con la clave verificada dentro de cada
 * manejador, queda protegida solo si alguien se acuerda.
 */
fun Application.instalarPlugins(configuracion: Configuracion) {
    install(ContentNegotiation) {
        json(
            Json {
                // La respuesta no incluye los campos nulos: la app no gana nada
                // recibiendo "detalle": null noventa y seis veces.
                explicitNulls = false
                encodeDefaults = true
                // El servicio no se cae porque el cliente mande un campo que
                // todavia no conoce. Es lo que permite agregar campos sin subir
                // la version de la API.
                ignoreUnknownKeys = true
                prettyPrint = false
            }
        )
    }

    install(DefaultHeaders) {
        header("X-Servicio", "cookify-precios")
    }

    install(CallLogging) {
        level = Level.INFO
        // Nunca se registra el cuerpo ni las cabeceras: ahi viaja la clave de
        // la API, y un registro se comparte y se respalda.
        format { llamada ->
            "${llamada.request.httpMethod.value} ${llamada.request.path()} " +
                "-> ${llamada.response.status()?.value}"
        }
    }

    // La app Android no necesita CORS, pero el cliente de pruebas en el
    // navegador si, y la demostracion se hace desde ahi.
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(CABECERA_CLAVE)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }

    instalarManejoDeErrores()
    instalarClaveDeApi(configuracion.claveApi)
}

/**
 * Traduce cada excepcion a su codigo de estado, en un solo lugar.
 *
 * Esta es la pieza que hace cumplir la regla de que ningun error viaja con
 * codigo 200. Una excepcion que se escape sin mapear sale como 500, no como
 * exito.
 */
private fun Application.instalarManejoDeErrores() {
    install(StatusPages) {
        exception<ErrorDelServicio> { llamada, error ->
            llamada.respond(
                error.estado,
                CuerpoError(
                    CuerpoError.Contenido(
                        codigo = error.codigo,
                        mensaje = error.message,
                        detalles = error.detalles,
                    )
                ),
            )
        }

        // Cuerpo que no es JSON, o JSON con un campo del tipo equivocado.
        exception<SerializationException> { llamada, error ->
            llamada.respond(
                HttpStatusCode.BadRequest,
                CuerpoError(
                    CuerpoError.Contenido(
                        codigo = "VALIDACION",
                        mensaje = "El cuerpo no es JSON valido para esta ruta",
                        detalles = listOf(
                            DetalleError("cuerpo", error.message ?: "no se pudo interpretar")
                        ),
                    )
                ),
            )
        }

        exception<BadRequestException> { llamada, error ->
            llamada.respond(
                HttpStatusCode.BadRequest,
                CuerpoError(
                    CuerpoError.Contenido(
                        codigo = "VALIDACION",
                        mensaje = error.message ?: "La peticion no es valida",
                    )
                ),
            )
        }

        exception<Throwable> { llamada, error ->
            // Al cliente se le da un identificador, no la traza: la traza dice
            // cosas del servidor que no son suyas. Con ese identificador se
            // encuentra el detalle completo en el registro.
            val identificador = UUID.randomUUID().toString().take(8)
            llamada.application.log.error("fallo no previsto [$identificador]", error)
            llamada.respond(
                HttpStatusCode.InternalServerError,
                CuerpoError(
                    CuerpoError.Contenido(
                        codigo = "ERROR_INTERNO",
                        mensaje = "Falla no prevista del servicio. Referencia: $identificador",
                    )
                ),
            )
        }

        status(HttpStatusCode.NotFound) { llamada, estado ->
            llamada.respond(
                estado,
                CuerpoError(
                    CuerpoError.Contenido(
                        codigo = "NO_ENCONTRADO",
                        mensaje = "La ruta ${llamada.request.path()} no existe en esta version " +
                            "de la API. El contrato esta en docs/api-cookify.md",
                    )
                ),
            )
        }
    }
}

const val CABECERA_CLAVE = "X-API-Key"

/**
 * Exige la clave en toda escritura bajo /api, y deja la lectura abierta.
 *
 * Es un plugin que mira cada llamada, y no una verificacion dentro de cada
 * manejador, para que no se pueda olvidar: una ruta de escritura nueva queda
 * protegida por omision.
 *
 * Cuando la clave no corresponde se lanza la excepcion del servicio en lugar de
 * responder aqui. Asi la respuesta la arma StatusPages, que es el unico lugar
 * que traduce errores, y el 401 sale con el mismo cuerpo que todos los demas.
 * Esto funciona porque el plugin se instala **despues** de StatusPages y por lo
 * tanto corre dentro de su try.
 *
 * La comparacion es de tiempo constante: `==` sale en el primer caracter
 * distinto, y esa diferencia de tiempo se puede medir desde afuera para
 * adivinar la clave carácter por carácter.
 */
private fun Application.instalarClaveDeApi(claveEsperada: String) {
    val esperada = claveEsperada.toByteArray()

    install(
        createApplicationPlugin("ClaveDeApi") {
            onCall { llamada ->
                val escribe = llamada.request.httpMethod in METODOS_DE_ESCRITURA
                val bajoApi = llamada.request.path().startsWith("/api/")
                if (!escribe || !bajoApi) return@onCall

                val recibida = llamada.request.headers[CABECERA_CLAVE]?.toByteArray()
                // Se responde 401 tanto si falta como si no corresponde:
                // distinguir los dos casos le confirma a quien prueba claves
                // que el formato era el correcto.
                if (recibida == null || !MessageDigest.isEqual(esperada, recibida)) {
                    throw ErrorDelServicio.NoAutorizado()
                }
            }
        }
    )
}

private val METODOS_DE_ESCRITURA = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete)

/** El identificador numerico de una ruta, o un 400 que dice cual era el problema. */
internal fun RoutingCall.identificadorLargo(nombre: String): Long =
    parameters[nombre]?.toLongOrNull()
        ?: throw ErrorDelServicio.Validacion(
            "El parametro '$nombre' de la ruta tiene que ser un numero entero"
        )

internal fun RoutingCall.enteroOpcional(nombre: String): Int? =
    request.queryParameters[nombre]?.let { crudo ->
        crudo.toIntOrNull()
            ?: throw ErrorDelServicio.Validacion("El parametro '$nombre' tiene que ser un entero")
    }

internal fun RoutingCall.booleanoOpcional(nombre: String): Boolean? =
    request.queryParameters[nombre]?.let { crudo ->
        when (crudo.lowercase()) {
            "true", "1", "si" -> true
            "false", "0", "no" -> false
            else -> throw ErrorDelServicio.Validacion(
                "El parametro '$nombre' tiene que ser true o false"
            )
        }
    }

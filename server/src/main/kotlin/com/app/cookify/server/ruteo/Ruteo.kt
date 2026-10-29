package com.app.cookify.server.ruteo

import com.app.cookify.server.config.booleanoOpcional
import com.app.cookify.server.config.enteroOpcional
import com.app.cookify.server.config.identificadorLargo
import com.app.cookify.server.dto.FuentePeticion
import com.app.cookify.server.dto.IngredientePeticion
import com.app.cookify.server.dto.PrecioPeticion
import com.app.cookify.server.dto.PreciosVigentesDto
import com.app.cookify.server.dto.RecoleccionPeticion
import com.app.cookify.server.dto.SaludDto
import com.app.cookify.server.dto.aDto
import com.app.cookify.server.negocio.FuenteServicio
import com.app.cookify.server.negocio.IngredienteServicio
import com.app.cookify.server.negocio.PrecioServicio
import com.app.cookify.server.negocio.RecolectorServicio
import com.app.cookify.server.negocio.SaludServicio
import com.app.cookify.server.negocio.SupermercadoServicio
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * La capa de presentacion del servicio: traduce HTTP a llamadas de negocio.
 *
 * Lo que esta capa hace: leer parametros, deserializar el cuerpo, elegir el
 * codigo de exito y poner la cabecera Location. Lo que **no** hace, y se puede
 * verificar leyendo el archivo: no valida reglas de negocio, no arma consultas
 * y no toca la base. Cada manejador cabe en cuatro lineas porque no le
 * corresponde hacer mas.
 *
 * Los errores no se manejan aqui: las excepciones del servicio suben hasta
 * StatusPages, que las traduce en un solo lugar.
 */
fun Application.instalarRutas(
    salud: SaludServicio,
    supermercados: SupermercadoServicio,
    ingredientes: IngredienteServicio,
    fuentes: FuenteServicio,
    precios: PrecioServicio,
    recolector: RecolectorServicio,
    version: String,
) {
    routing {

        // Fuera de /api/v1 a proposito: no es parte del contrato de datos, es
        // para el despliegue. Por eso tampoco cambia cuando cambia la version.
        get("/salud") {
            val responde = salud.baseResponde()
            call.respond(
                if (responde) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                SaludDto(
                    estado = if (responde) "arriba" else "sin base de datos",
                    version = version,
                    baseDeDatos = if (responde) "responde" else "no responde",
                ),
            )
        }

        route("/api/v1") {

            // --- supermercados: catalogo cerrado, solo lectura -------------
            route("/supermercados") {
                get {
                    call.respond(supermercados.todos().map { it.aDto() })
                }
                get("/{codigo}") {
                    val codigo = call.parameters["codigo"].orEmpty()
                    call.respond(supermercados.porCodigo(codigo).aDto())
                }
            }

            // --- ingredientes ---------------------------------------------
            route("/ingredientes") {
                get {
                    val pagina = ingredientes.listar(
                        pasillo = call.request.queryParameters["pasillo"],
                        busqueda = call.request.queryParameters["q"],
                        pagina = call.enteroOpcional("pagina"),
                        tamano = call.enteroOpcional("tamano"),
                    )
                    call.respond(pagina.aDto { it.aDto() })
                }

                get("/{id}") {
                    call.respond(ingredientes.porId(call.parameters["id"].orEmpty()).aDto())
                }

                post {
                    val creado = ingredientes.crear(call.receive<IngredientePeticion>())
                    // 201 con Location: quien creo el recurso recibe donde
                    // quedo, sin tener que construir la ruta por su cuenta.
                    call.response.header("Location", "/api/v1/ingredientes/${creado.id}")
                    call.respond(HttpStatusCode.Created, creado.aDto())
                }

                put("/{id}") {
                    val id = call.parameters["id"].orEmpty()
                    call.respond(ingredientes.actualizar(id, call.receive()).aDto())
                }

                delete("/{id}") {
                    ingredientes.borrar(call.parameters["id"].orEmpty())
                    // 204 y no 200 con un cuerpo que diga "borrado": el estado
                    // ya lo dice y el cuerpo no agrega nada.
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            // --- fuentes --------------------------------------------------
            route("/fuentes") {
                get {
                    val pagina = fuentes.listar(
                        supermercado = call.request.queryParameters["supermercado"],
                        ingredienteId = call.request.queryParameters["ingredienteId"],
                        activa = call.booleanoOpcional("activa"),
                        pagina = call.enteroOpcional("pagina"),
                        tamano = call.enteroOpcional("tamano"),
                    )
                    call.respond(pagina.aDto { it.aDto() })
                }

                get("/{id}") {
                    call.respond(fuentes.porId(call.identificadorLargo("id")).aDto())
                }

                post {
                    val creada = fuentes.crear(call.receive<FuentePeticion>())
                    call.response.header("Location", "/api/v1/fuentes/${creada.id}")
                    call.respond(HttpStatusCode.Created, creada.aDto())
                }

                put("/{id}") {
                    val id = call.identificadorLargo("id")
                    call.respond(fuentes.actualizar(id, call.receive()).aDto())
                }

                delete("/{id}") {
                    fuentes.borrar(call.identificadorLargo("id"))
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            // --- precios --------------------------------------------------
            route("/precios") {
                get {
                    val parametros = call.request.queryParameters
                    // Dos lecturas distintas en una ruta: el precio vigente,
                    // que es lo que la app pide, y la historia completa, que es
                    // para auditar. La forma de la respuesta no es la misma, y
                    // el parametro lo dice.
                    if (call.booleanoOpcional("historia") == true) {
                        val pagina = precios.historia(
                            supermercado = parametros["supermercado"],
                            ingredienteId = parametros["ingredienteId"],
                            origen = parametros["origen"],
                            pagina = call.enteroOpcional("pagina"),
                            tamano = call.enteroOpcional("tamano"),
                        )
                        call.respond(pagina.aDto { it.aDto() })
                    } else {
                        val vigentes = precios.vigentes(
                            supermercado = parametros["supermercado"],
                            origen = parametros["origen"],
                            ingredienteId = parametros["ingredienteId"],
                        )
                        call.respond(
                            PreciosVigentesDto(
                                supermercado = parametros["supermercado"].orEmpty().uppercase(),
                                // La fecha del lote es la de la medicion mas
                                // nueva: es con esa que la app decide si su
                                // cache vencio.
                                actualizadoEn = vigentes.maxOfOrNull { it.medidoEn }?.aDto(),
                                cantidad = vigentes.size,
                                datos = vigentes.map { it.aDto() },
                            )
                        )
                    }
                }

                get("/{id}") {
                    call.respond(precios.porId(call.identificadorLargo("id")).aDto())
                }

                post {
                    val creado = precios.crear(call.receive<PrecioPeticion>())
                    call.response.header("Location", "/api/v1/precios/${creado.id}")
                    call.respond(HttpStatusCode.Created, creado.aDto())
                }

                put("/{id}") {
                    val id = call.identificadorLargo("id")
                    call.respond(precios.actualizar(id, call.receive()).aDto())
                }

                delete("/{id}") {
                    precios.borrar(call.identificadorLargo("id"))
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            // --- recolecciones --------------------------------------------
            route("/recolecciones") {
                get {
                    val pagina = recolector.listar(
                        supermercado = call.request.queryParameters["supermercado"],
                        pagina = call.enteroOpcional("pagina"),
                        tamano = call.enteroOpcional("tamano"),
                    )
                    call.respond(pagina.aDto { it.aDto() })
                }

                get("/{id}") {
                    call.respond(recolector.porId(call.identificadorLargo("id")).aDto())
                }

                post {
                    val abierta = recolector.iniciar(call.receive<RecoleccionPeticion>())
                    call.response.header("Location", "/api/v1/recolecciones/${abierta.id}")
                    // 202 y no 201: la corrida arranco, no termino. El Location
                    // apunta a donde se sigue su avance.
                    call.respond(HttpStatusCode.Accepted, abierta.aDto())
                }

                delete("/{id}") {
                    recolector.borrar(call.identificadorLargo("id"))
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

/** Fecha de un precio en el formato del contrato, para el campo del lote. */
private fun java.time.OffsetDateTime.aDto(): String =
    format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)

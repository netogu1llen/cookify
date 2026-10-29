package com.app.cookify.server

import com.app.cookify.server.config.Configuracion
import com.app.cookify.server.config.instalarPlugins
import com.app.cookify.server.datos.FuenteDao
import com.app.cookify.server.datos.FuenteDeDatos
import com.app.cookify.server.datos.IngredienteDao
import com.app.cookify.server.datos.PrecioDao
import com.app.cookify.server.datos.RecoleccionDao
import com.app.cookify.server.datos.SupermercadoDao
import com.app.cookify.server.datos.externo.ClienteCadena
import com.app.cookify.server.negocio.FuenteServicio
import com.app.cookify.server.negocio.IngredienteServicio
import com.app.cookify.server.negocio.PrecioServicio
import com.app.cookify.server.negocio.RecolectorServicio
import com.app.cookify.server.negocio.SaludServicio
import com.app.cookify.server.negocio.SupermercadoServicio
import com.app.cookify.server.ruteo.instalarRutas
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Arranque y cableado del servicio.
 *
 * Las dependencias se construyen aqui y se pasan por constructor. Sin
 * inyeccion automatica: con cinco servicios y cinco DAO, un contenedor de
 * dependencias esconderia el grafo en lugar de aclararlo, y este archivo es
 * justamente donde conviene poder leerlo completo.
 *
 * El esquema **no** se crea al arrancar. Vive en db/001_esquema.sql y lo aplica
 * el contenedor de PostgreSQL. Generarlo desde el codigo crearia dos verdades
 * sobre la forma de la base.
 */
private const val VERSION = "1.0.0"

fun main() {
    val registro = LoggerFactory.getLogger("cookify")
    val configuracion = Configuracion.desdeEntorno()

    val fuenteDeDatos = FuenteDeDatos(configuracion.baseDeDatos)
    val cliente = ClienteCadena(
        paginasMaximas = configuracion.recolector.paginasMaximas,
        pausaEntrePeticionesMs = configuracion.recolector.pausaEntrePeticionesMs,
    )

    // Que los recursos se cierren aunque el proceso termine por una senal:
    // docker compose down manda SIGTERM, no mata el proceso.
    Runtime.getRuntime().addShutdownHook(
        Thread {
            registro.info("cerrando el pozo de conexiones y el cliente HTTP")
            cliente.close()
            fuenteDeDatos.close()
        }
    )

    val supermercadoDao = SupermercadoDao()
    val ingredienteDao = IngredienteDao()
    val fuenteDao = FuenteDao()
    val precioDao = PrecioDao()
    val recoleccionDao = RecoleccionDao()

    val recolector = RecolectorServicio(
        fuenteDeDatos = fuenteDeDatos,
        recolecciones = recoleccionDao,
        fuentes = fuenteDao,
        precios = precioDao,
        supermercados = supermercadoDao,
        ingredientes = ingredienteDao,
        catalogo = cliente,
        // Se reemplaza por el alcance de la aplicacion apenas esta existe: la
        // peticion que inicia una corrida termina enseguida con un 202, y la
        // corrida tiene que seguir despues de eso.
        alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    val servidor = embeddedServer(Netty, port = configuracion.puerto) {
        instalarPlugins(configuracion)

        // Lo primero, antes de atender nada: una corrida que quedo EN_CURSO
        // porque el proceso anterior se cayo bloquearia para siempre las
        // recolecciones de esa cadena.
        launch { recolector.cerrarCorridasHuerfanas() }

        instalarRutas(
            salud = SaludServicio(fuenteDeDatos),
            supermercados = SupermercadoServicio(fuenteDeDatos, supermercadoDao),
            ingredientes = IngredienteServicio(fuenteDeDatos, ingredienteDao),
            fuentes = FuenteServicio(fuenteDeDatos, fuenteDao, supermercadoDao, ingredienteDao),
            precios = PrecioServicio(fuenteDeDatos, precioDao, supermercadoDao, ingredienteDao),
            recolector = recolector,
            version = VERSION,
        )

        registro.info(
            "cookify-precios {} escuchando en :{} contra {}",
            VERSION, configuracion.puerto, configuracion.baseDeDatos.url,
        )
    }
    servidor.start(wait = true)
}

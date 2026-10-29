"""Genera la coleccion de Postman desde una tabla de peticiones.

Se genera y no se exporta a mano desde Postman por dos razones: el archivo
exportado trae identificadores y metadatos que cambian en cada exportacion y
ensucian el historial, y una tabla se lee de corrido para revisar que las
peticiones de la demostracion esten completas y en orden.

Cada peticion lleva una prueba que verifica su codigo de estado, asi que la
coleccion se puede correr entera, tambien sin Postman:

  newman run postman/cookify-api.postman_collection.json \
      --env-var baseUrl=http://localhost:8090 --env-var apiKey=$COOKIFY_API_KEY

Uso:
  python tools/generar_coleccion.py
"""
import io
import json
import os

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

IDENTIFICADOR = "cookify-precios-v1"

IN_IGREDIENTE = {
    "id": "cebolla_morada",
    "nombre": "Cebolla morada",
    "pasillo": "VERDULERIA",
    "unidad": "g",
    "precioBaseClp": 1.9,
}

# (nombre, metodo, ruta, cuerpo, estado esperado, nota)
DEMOSTRACION = [
    ("0 · El servicio esta arriba", "GET", "/salud", None, 200,
     "Primero que todo: el servicio responde y la base responde. Si esto falla, "
     "no sigue nada."),
    ("1 · Crear un ingrediente", "POST", "/api/v1/ingredientes", IN_IGREDIENTE, 201,
     "Crear. Responde 201 y la cabecera Location con la ruta del recurso nuevo. "
     "Guarda el id en una variable para los pasos siguientes."),
    ("2 · Leer el que se acaba de crear", "GET", "/api/v1/ingredientes/cebolla_morada",
     None, 200,
     "Leer. Es la prueba de que el dato quedo guardado y no solo aceptado: la "
     "respuesta trae creadoEn, que lo puso la base."),
    ("3 · Actualizar su precio", "PUT", "/api/v1/ingredientes/cebolla_morada",
     {"nombre": "Cebolla morada grande", "pasillo": "VERDULERIA", "unidad": "g",
      "precioBaseClp": 2.4}, 200,
     "Actualizar. Responde el recurso completo ya modificado, con actualizadoEn "
     "distinto de creadoEn."),
    ("4 · Borrarlo", "DELETE", "/api/v1/ingredientes/cebolla_morada", None, 204,
     "Borrar. 204 sin cuerpo: el estado ya lo dice todo."),
    ("5 · Confirmar que ya no esta", "GET", "/api/v1/ingredientes/cebolla_morada",
     None, 404,
     "El cierre de las cuatro operaciones: lo que se borro, se borro."),
    ("6 · Disparar una recoleccion de Jumbo", "POST", "/api/v1/recolecciones",
     {"supermercado": "JUMBO", "ingredientes": ["cebolla", "papa", "lentejas"]}, 202,
     "202 y no 201: la corrida arranca en segundo plano y la respuesta llega antes "
     "de que termine. El Location apunta a donde se sigue su avance."),
    ("7 · Seguir la recoleccion", "GET", "/api/v1/recolecciones?supermercado=JUMBO",
     None, 200,
     "El estado de la corrida, con categorias leidas, productos leidos y precios "
     "escritos. Se repite hasta que deja de decir EN_CURSO."),
    ("8 · Los precios medidos que quedaron", "GET",
     "/api/v1/precios?supermercado=JUMBO&origen=MEDIDO", None, 200,
     "El resultado: precio por gramo, por mililitro o por unidad, con cuantos "
     "productos se consideraron para cada uno. Esto es lo que consume la app."),
    ("9 · Los estimados, para comparar", "GET",
     "/api/v1/precios?supermercado=TOTTUS&origen=ESTIMADO", None, 200,
     "La otra mitad del argumento: en las cuatro cadenas sin recolector el precio "
     "sigue siendo ESTIMADO, y el servicio lo dice en lugar de disimularlo."),
]

ERRORES = [
    ("401 · Escribir sin la clave", "POST", "/api/v1/ingredientes", IN_IGREDIENTE, 401,
     "Sin la cabecera X-API-Key no se escribe. La lectura es abierta.", False),
    ("401 · Escribir con la clave equivocada", "POST", "/api/v1/ingredientes",
     IN_IGREDIENTE, 401,
     "Mismo codigo que cuando falta: distinguirlos le confirma a quien prueba "
     "claves que el formato era el correcto.", "equivocada"),
    ("400 · Cuerpo con tres campos malos", "POST", "/api/v1/ingredientes",
     {"id": "MAL ID", "nombre": "", "pasillo": "FERRETERIA", "unidad": "kg",
      "precioBaseClp": -5}, 400,
     "El error nombra los campos uno por uno en 'detalles'. Quien manda tres "
     "campos malos se entera de los tres, no del primero.", True),
    ("400 · Cuerpo que no es JSON", "POST", "/api/v1/ingredientes", "esto no es json", 400,
     "El cuerpo no se puede interpretar y el servicio lo dice con 400, no con 500.",
     True),
    ("409 · Crear dos veces el mismo ingrediente", "POST", "/api/v1/ingredientes",
     {"id": "cebolla", "nombre": "Cebolla", "pasillo": "VERDULERIA", "unidad": "g",
      "precioBaseClp": 1.3}, 409,
     "No reemplaza en silencio: quien manda el segundo POST cree que esta creando "
     "algo.", True),
    ("409 · Dos fuentes para el mismo par", "POST", "/api/v1/fuentes",
     {"supermercado": "JUMBO", "ingredienteId": "cebolla",
      "categoriaRuta": "/frutas-y-verduras/verduras/verdura", "terminos": ["cebolla"],
      "medidaEsperada": "g", "unidadesBasePorMedida": 1}, 409,
     "Con dos fuentes para la misma cadena y el mismo ingrediente, el precio "
     "dependeria de cual se leyo primero.", True),
    ("409 · Recolectar en una cadena sin recolector", "POST", "/api/v1/recolecciones",
     {"supermercado": "TOTTUS"}, 409,
     "Tottus no entrega el catalogo renderizado en el servidor. El servicio lo "
     "dice en lugar de intentarlo y devolver una lista vacia.", True),
    ("404 · Un ingrediente que no existe", "GET", "/api/v1/ingredientes/unicornio",
     None, 404, "", False),
    ("404 · Una fuente con un ingrediente que no existe", "POST", "/api/v1/fuentes",
     {"supermercado": "JUMBO", "ingredienteId": "unicornio", "categoriaRuta": "/x",
      "medidaEsperada": "g", "unidadesBasePorMedida": 1}, 404,
     "404 y no 400: el cuerpo esta bien formado, lo que no existe es a lo que "
     "apunta.", True),
    ("400 · Pedir los vigentes sin decir la cadena", "GET", "/api/v1/precios", None, 400,
     "El precio vigente es siempre de una cadena. Sin ella la pregunta no tiene "
     "respuesta, y el servicio no elige una por su cuenta.", False),
    ("400 · Una pagina que no existe", "GET", "/api/v1/ingredientes?pagina=0", None, 400,
     "", False),
    ("400 · Una pagina mas grande que el tope", "GET", "/api/v1/ingredientes?tamano=500",
     None, 400,
     "El tope es 200: sin el, un solo GET puede arrastrar la tabla completa.", False),
]

CATALOGO = [
    ("Supermercados", "GET", "/api/v1/supermercados", None, 200),
    ("Un supermercado", "GET", "/api/v1/supermercados/SANTA_ISABEL", None, 200),
    ("Ingredientes paginados", "GET", "/api/v1/ingredientes?pagina=1&tamano=10", None, 200),
    ("Ingredientes de un pasillo", "GET", "/api/v1/ingredientes?pasillo=ABARROTES", None, 200),
    ("Ingredientes por nombre, sin tildes", "GET", "/api/v1/ingredientes?q=brocoli", None, 200),
    ("Un ingrediente", "GET", "/api/v1/ingredientes/lentejas", None, 200),
    ("Fuentes de una cadena", "GET", "/api/v1/fuentes?supermercado=JUMBO&tamano=200", None, 200),
    ("Fuentes inactivas", "GET", "/api/v1/fuentes?activa=false", None, 200),
    ("Fuente de un ingrediente", "GET", "/api/v1/fuentes?ingredienteId=ajo", None, 200),
    ("Precios vigentes de Jumbo", "GET", "/api/v1/precios?supermercado=JUMBO", None, 200),
    ("Precio de un solo ingrediente", "GET",
     "/api/v1/precios?supermercado=JUMBO&ingredienteId=cebolla", None, 200),
    ("Historia de un precio", "GET",
     "/api/v1/precios?historia=true&ingredienteId=cebolla&tamano=20", None, 200),
    ("Recolecciones", "GET", "/api/v1/recolecciones", None, 200),
]


def peticion(nombre, metodo, ruta, cuerpo, estado, nota="", con_clave=None):
    """Una peticion de la coleccion, con su prueba de codigo de estado."""
    if con_clave is None:
        con_clave = metodo in ("POST", "PUT", "DELETE")

    cabeceras = []
    if cuerpo is not None:
        cabeceras.append({"key": "Content-Type", "value": "application/json"})
    if con_clave is True:
        cabeceras.append({"key": "X-API-Key", "value": "{{apiKey}}"})
    elif con_clave == "equivocada":
        cabeceras.append({"key": "X-API-Key", "value": "esta-clave-no-corresponde"})

    item = {
        "name": nombre,
        "event": [{
            "listen": "test",
            "script": {
                "type": "text/javascript",
                "exec": [
                    "pm.test('responde %s', function () {" % estado,
                    "    pm.response.to.have.status(%d);" % estado,
                    "});",
                ],
            },
        }],
        "request": {
            "method": metodo,
            "header": cabeceras,
            "url": {"raw": "{{baseUrl}}" + ruta},
        },
    }
    if nota:
        item["request"]["description"] = nota
    if cuerpo is not None:
        crudo = cuerpo if isinstance(cuerpo, str) else json.dumps(
            cuerpo, ensure_ascii=False, indent=2)
        item["request"]["body"] = {"mode": "raw", "raw": crudo,
                                  "options": {"raw": {"language": "json"}}}
    return item


def main():
    coleccion = {
        "info": {
            "_postman_id": IDENTIFICADOR,
            "name": "Cookify · API de precios v1",
            "description": (
                "Servicio de precios de Cookify, Hito 2 de INGT1003.\n\n"
                "Antes de correr nada hay que definir dos variables: baseUrl, que por "
                "omision es http://localhost:8090, y apiKey, que es el valor de "
                "COOKIFY_API_KEY del archivo .env.\n\n"
                "La clave NO viene en este archivo y no debe escribirse aqui: este "
                "archivo esta en el repositorio y el repositorio es publico. Va en un "
                "entorno de Postman, que no se versiona.\n\n"
                "La carpeta «Demostracion» esta en el orden en que se ejecuta en la "
                "presentacion, de principio a fin. «Errores» es la otra mitad del "
                "trabajo de un servicio: responder bien cuando algo falla. "
                "«Catalogo completo» tiene el resto de las rutas del contrato.\n\n"
                "El contrato completo esta en docs/api-cookify.md."
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "variable": [
            {"key": "baseUrl", "value": "http://localhost:8090",
             "description": "Raiz del servicio, sin barra al final."},
            {"key": "apiKey", "value": "",
             "description": "COOKIFY_API_KEY del .env. Dejar vacio aqui y definirlo "
                            "en un entorno de Postman."},
        ],
        "item": [
            {
                "name": "1 · Demostracion en vivo",
                "description": "En este orden, de principio a fin.",
                "item": [peticion(*p) for p in DEMOSTRACION],
            },
            {
                "name": "2 · Errores",
                "description": "Un error nunca viaja con codigo 200.",
                "item": [peticion(*p) for p in ERRORES],
            },
            {
                "name": "3 · Catalogo completo de rutas",
                "description": "El resto del contrato, solo lectura.",
                "item": [peticion(*p) for p in CATALOGO],
            },
        ],
    }

    carpeta = os.path.join(RAIZ, "postman")
    os.makedirs(carpeta, exist_ok=True)
    destino = os.path.join(carpeta, "cookify-api.postman_collection.json")
    io.open(destino, "w", encoding="utf-8", newline="\n").write(
        json.dumps(coleccion, ensure_ascii=False, indent=2) + "\n")

    total = len(DEMOSTRACION) + len(ERRORES) + len(CATALOGO)
    print("escrito %s" % destino)
    print("  %d peticiones: %d de demostracion, %d de error, %d de catalogo"
          % (total, len(DEMOSTRACION), len(ERRORES), len(CATALOGO)))


if __name__ == "__main__":
    main()

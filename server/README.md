# Servicio de precios de Cookify

API REST que lee los precios reales de los supermercados, los normaliza a la
unidad del recetario y los sirve a la app. Es el back end del Hito 2.

Kotlin · Ktor 3.2 · PostgreSQL 18 · JDBC con HikariCP · Gradle 9.5

---

## Levantarlo en otro equipo

Hacen falta **Docker** y un **JDK 21 o superior**. Nada más: Gradle lo baja el
wrapper del repositorio.

```bash
git clone https://github.com/netogu1llen/cookify.git
cd cookify

cp .env.example .env            # y editar los valores (ver abajo)
docker compose up -d db         # PostgreSQL, con el esquema y la semilla aplicados

./gradlew -p server test        # 33 pruebas, sin base ni red
./gradlew -p server run         # el servicio, en el puerto que diga .env
```

Verificación en una línea:

```bash
curl -s http://localhost:8090/salud
# {"estado":"arriba","version":"1.0.0","baseDeDatos":"responde"}
```

### Qué hay que editar en `.env`

Tres valores y ninguno más:

| Variable | Qué poner |
|---|---|
| `POSTGRES_PASSWORD` | cualquier cosa; la base solo escucha en localhost |
| `COOKIFY_DB_CLAVE` | **el mismo valor** que `POSTGRES_PASSWORD` |
| `COOKIFY_API_KEY` | al menos 16 caracteres · `openssl rand -hex 24` |

El servicio **no arranca** si falta alguna: arrancar con la clave vacía dejaría
la API abierta a escritura y nadie se daría cuenta hasta que alguien escriba.

`.env` está en `.gitignore` y nunca se versiona. El que se versiona es
`.env.example`, que solo trae nombres y valores de ejemplo.

### Todo en contenedores, con un comando

```bash
docker compose up -d            # la base y el servicio ya empaquetado
```

La primera construcción de la imagen baja Gradle y las dependencias y tarda
varios minutos; después queda en caché. Para desarrollar conviene el camino de
arriba, con `docker compose up -d db` y Gradle en el equipo: el ciclo de cambio
y prueba es de segundos y no de minutos.

### Partir la base de cero

```bash
docker compose down -v && docker compose up -d db
```

Los scripts de `db/` solo se ejecutan cuando el volumen se crea. Mientras el
volumen exista, editar `db/001_esquema.sql` no cambia nada.

---

## La API

El contrato completo, con cada ruta, cada cuerpo y cada código de estado, está
en [`docs/api-cookify.md`](../docs/api-cookify.md). Resumen:

| Recurso | Qué es | Operaciones |
|---|---|---|
| `/api/v1/supermercados` | las seis cadenas que la app ofrece | leer |
| `/api/v1/ingredientes` | los 98 del recetario | crear · leer · actualizar · borrar |
| `/api/v1/fuentes` | de dónde se lee el precio de cada ingrediente | crear · leer · actualizar · borrar |
| `/api/v1/precios` | la medición, con su historia | crear · leer · actualizar · borrar |
| `/api/v1/recolecciones` | una corrida del recolector | crear · leer · borrar |
| `/salud` | para el despliegue, fuera del contrato | leer |

La lectura es abierta; toda escritura exige la cabecera `X-API-Key`.

### Probarla

```bash
set -a && . ./.env && set +a
bash tools/probar_api.sh        # 48 verificaciones, incluidos los errores
```

O con la colección de Postman, que está en
[`postman/`](../postman/cookify-api.postman_collection.json) con 35 peticiones
en tres carpetas: la demostración en orden, los errores y el resto del
contrato. Las variables `baseUrl` y `apiKey` se definen en un entorno de
Postman —no en el archivo, que está en un repositorio público—.

---

## Cómo está armado

```
server/src/main/kotlin/com/app/cookify/server/
├── Aplicacion.kt            arranque y cableado: el grafo completo se lee aquí
├── config/
│   ├── Configuracion.kt     el entorno, validado al arrancar
│   ├── Errores.kt           los errores que el servicio sabe nombrar
│   └── Plugins.kt           serialización, registro, errores, clave de la API
├── ruteo/
│   └── Ruteo.kt             HTTP: parámetros, cuerpos, códigos y Location
├── negocio/
│   ├── Servicios.kt         las reglas y las validaciones
│   ├── RecolectorServicio.kt la corrida contra el sitio de la cadena
│   └── Normalizador.kt      de lo que publica la cadena al precio del recetario
├── datos/
│   ├── FuenteDeDatos.kt     pozo de conexiones y transacciones
│   ├── Daos.kt              el único lugar con SQL
│   └── externo/
│       ├── ClienteCadena.kt  el único lugar que habla con jumbo.cl
│       └── ParserCatalogo.kt el catálogo embebido en la página
├── dto/Dtos.kt              el contrato HTTP, separado del dominio
└── modelo/Modelos.kt        el dominio: Kotlin puro, sin anotaciones
```

La regla que ordena todo, y que se puede verificar abriendo cualquier archivo:

- En **ruteo** no hay validaciones de negocio ni consultas. Cada manejador cabe
  en cuatro líneas porque no le corresponde hacer más.
- En **negocio** no hay una sola sentencia SQL ni una referencia a un código de
  estado HTTP.
- En **datos** está todo el SQL y nada más que SQL.

El recorrido completo de una petición, capa por capa, está en el documento
técnico del hito.

### El esquema no lo crea el código

Vive en [`db/001_esquema.sql`](../db/001_esquema.sql) y lo aplica el contenedor
de PostgreSQL al arrancar. El servicio no genera tablas: si las generara habría
dos verdades sobre la forma de la base, y se separarían en la primera edición.

La semilla (`db/002_semilla.sql`) se **genera** desde el catálogo de la app, por
la misma razón:

```bash
python tools/generar_semilla.py
```

### Sin ORM, a propósito

El esquema está en SQL versionado, la consulta que se ejecuta es la que está
escrita, y cualquiera la lee sin aprender un lenguaje intermedio. El costo que
se asume: mapear cada fila a mano es código repetitivo que un ORM daría gratis.
Se acepta porque el servicio tiene cuatro tablas, no cuarenta.

---

## El recolector

```bash
curl -X POST http://localhost:8090/api/v1/recolecciones \
  -H "Content-Type: application/json" -H "X-API-Key: $COOKIFY_API_KEY" \
  -d '{"supermercado":"JUMBO"}'
```

Responde **202** con la recolección abierta y sigue en segundo plano. El avance
se consulta en `GET /api/v1/recolecciones/{id}`.

Qué hace, en orden:

1. Agrupa las fuentes activas **por categoría**, no por ingrediente: 96
   ingredientes caben en 52 categorías, y pedir una página por ingrediente
   sería pedir la misma página tres veces para la cebolla, el tomate y la papa.
2. Por cada categoría pide sus páginas y extrae los productos del catálogo
   embebido.
3. Por cada fuente filtra los productos por términos y exclusiones, lleva el
   precio publicado a su unidad canónica y lo divide por el factor de la fuente.
4. Toma el **percentil 25** de los candidatos, no la mediana: quien cocina con
   presupuesto contado compra la marca barata, no la del medio.
5. Escribe todo en **una** transacción. Media cadena medida y media vieja sería
   imposible de distinguir después.

Una corrida completa de Jumbo son 52 categorías, unos 150 pedidos al sitio y
entre cinco y diez minutos. Por eso esto vive en un servidor y no en el
teléfono.

### El mapeo de fuentes

Es el trabajo de verdad del servicio, y está verificado ingrediente por
ingrediente contra el sitio real:

```bash
python tools/verificar_fuentes.py --cadena JUMBO
python tools/verificar_fuentes.py --cadena JUMBO --diagnostico cebolla,ajo
python tools/verificar_fuentes.py --cadena JUMBO --listar /despensa/legumbres
```

La fuente de verdad es [`db/fuentes_precios.json`](../db/fuentes_precios.json).
Un mapeo malo **no falla**: cobra mal y nadie se entera. Por eso existe la
herramienta.

---

## Pruebas

```bash
./gradlew -p server test
```

33 pruebas sobre la capa de negocio y el parser, sin base de datos y sin red,
porque esas dos capas son Kotlin puro. Lo que cubren:

- **`NormalizadorTest`** · las conversiones de unidad, el percentil, los
  filtros de términos y exclusiones, y los dos casos donde el catálogo y la
  cadena no miden lo mismo (el ajo por diente, la palta por unidad).
- **`ParserCatalogoTest`** · el catálogo embebido, el precio de lista sobre el
  de promoción, y que una página sin catálogo devuelva vacío en lugar de
  reventar: es lo que va a pasar cuando la cadena cambie su sitio.
- **`ConfiguracionTest`** · que el servicio se niegue a arrancar sin
  configuración, y que el mensaje de error nunca imprima la clave.

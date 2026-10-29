# API de precios de Cookify · v1

Contrato del servicio. Es el acuerdo entre quien construye el servidor y quien
construye la app: lo que está aquí es lo que el cliente puede suponer, y nada más.

- **Base** `http://localhost:8090/api/v1`
- **Formato** JSON, `application/json; charset=utf-8`
- **Moneda** pesos chilenos, sin decimales en el total y con hasta cuatro
  decimales en el precio por unidad base
- **Zona horaria** las fechas se devuelven en ISO 8601 con desplazamiento
  explícito (`2026-10-29T14:03:11-03:00`)

---

## Criterio de versionado

La versión va en la ruta (`/api/v1`), no en una cabecera, porque así una
petición es legible y reproducible desde la barra del navegador y desde `curl`,
que es donde se demuestra.

| Cambio | Qué pasa con la versión |
|---|---|
| Agregar un recurso | no sube |
| Agregar un campo opcional a una respuesta | no sube |
| Agregar un parámetro opcional de consulta | no sube |
| Quitar o renombrar un campo | `/api/v2` |
| Cambiar el tipo o el significado de un campo | `/api/v2` |
| Cambiar un código de estado ya documentado | `/api/v2` |

Cuando aparezca `v2`, `v1` se mantiene un ciclo completo de la app en las
tiendas antes de retirarse. El cliente que ya está instalado no se puede
actualizar por decreto.

---

## Autenticación

La lectura es abierta. **Toda escritura** (`POST`, `PUT`, `DELETE`) exige la
cabecera `X-API-Key`.

```
X-API-Key: <COOKIFY_API_KEY>
```

La clave se configura por variable de entorno y **no está en el repositorio**:
`.env.example` muestra el nombre de cada variable con un valor de ejemplo.

| Situación | Código |
|---|---|
| Falta la cabecera | `401` |
| La clave no corresponde | `401` |

Se responde `401` en los dos casos a propósito: distinguir «falta» de «es
incorrecta» le dice a quien prueba claves que el formato era el correcto.

---

## Forma de los errores

Un error nunca viaja con código 200. El cuerpo es siempre el mismo objeto:

```json
{
  "error": {
    "codigo": "VALIDACION",
    "mensaje": "El cuerpo de la petición no es válido",
    "detalles": [
      { "campo": "precioBaseClp", "problema": "debe ser mayor que 0" },
      { "campo": "unidad", "problema": "debe ser uno de: g, ml, un" }
    ]
  }
}
```

| `codigo` | Estado | Cuándo |
|---|---|---|
| `VALIDACION` | 400 | el cuerpo o un parámetro no cumplen el contrato |
| `NO_AUTORIZADO` | 401 | falta la clave o no corresponde |
| `NO_ENCONTRADO` | 404 | el recurso pedido no existe |
| `CONFLICTO` | 409 | ya existe un recurso con esa identidad |
| `FUENTE_NO_DISPONIBLE` | 503 | el sitio de la cadena no respondió |
| `ERROR_INTERNO` | 500 | falla no prevista; queda en el registro con su identificador |

---

## Paginación

Las colecciones se paginan. `pagina` empieza en 1 y `tamano` tiene techo de 200
para que una petición no pueda arrastrar la tabla completa.

```
GET /api/v1/ingredientes?pagina=2&tamano=50
```

```json
{
  "datos": [ "..." ],
  "pagina": { "numero": 2, "tamano": 50, "total": 98, "paginas": 2 }
}
```

---

## Recursos

### 1 · `supermercados` — solo lectura

Las seis cadenas que la app ofrece. Es un catálogo cerrado: se lee, no se
escribe, porque agregar una cadena implica construirle un recolector.

| Verbo | Ruta | Éxito | Error |
|---|---|---|---|
| GET | `/supermercados` | 200 | — |
| GET | `/supermercados/{codigo}` | 200 | 404 |

```json
{
  "codigo": "JUMBO",
  "etiqueta": "Jumbo",
  "multiplicadorEstimado": 1.15,
  "soportaMedicion": true,
  "sitio": "https://www.jumbo.cl"
}
```

`soportaMedicion` es lo que permite a la app decir «precio medido» o «precio
estimado» sin prometer una precisión que no tiene.

---

### 2 · `ingredientes` — CRUD completo

Los 98 ingredientes del recetario, con su precio de referencia.

| Verbo | Ruta | Cuerpo | Éxito | Error |
|---|---|---|---|---|
| GET | `/ingredientes` | — | 200 | 400 |
| GET | `/ingredientes/{id}` | — | 200 | 404 |
| POST | `/ingredientes` | sí | 201 + `Location` | 400 · 401 · 409 |
| PUT | `/ingredientes/{id}` | sí | 200 | 400 · 401 · 404 |
| DELETE | `/ingredientes/{id}` | — | 204 | 401 · 404 |

**Parámetros de `GET /ingredientes`**

| Parámetro | Tipo | Por omisión | Qué hace |
|---|---|---|---|
| `pasillo` | enum | — | filtra por pasillo |
| `q` | texto | — | busca en el nombre, sin distinguir tildes ni mayúsculas |
| `pagina` | entero ≥ 1 | 1 | |
| `tamano` | entero 1–200 | 50 | |

**Cuerpo de `POST` y `PUT`**

```json
{
  "id": "cebolla_morada",
  "nombre": "Cebolla morada",
  "pasillo": "VERDULERIA",
  "unidad": "g",
  "precioBaseClp": 1.9
}
```

| Campo | Regla |
|---|---|
| `id` | minúsculas, números y guión bajo, 2 a 48 caracteres. En `PUT` se ignora: la identidad la fija la ruta |
| `nombre` | no vacío |
| `pasillo` | `VERDULERIA` `CARNICERIA` `PESCADERIA` `LACTEOS` `ABARROTES` `PANADERIA` `CONGELADOS` |
| `unidad` | `g` `ml` `un` |
| `precioBaseClp` | mayor que 0 |

**Respuesta**

```json
{
  "id": "cebolla",
  "nombre": "Cebolla",
  "pasillo": "VERDULERIA",
  "unidad": "g",
  "precioBaseClp": 1.3,
  "creadoEn": "2026-10-29T10:12:04-03:00",
  "actualizadoEn": "2026-10-29T10:12:04-03:00"
}
```

`POST` con un `id` que ya existe responde `409`, no `200` ni un reemplazo
silencioso: quien manda el segundo `POST` cree que está creando algo.

---

### 3 · `fuentes` — CRUD completo

De dónde se lee el precio de un ingrediente en una cadena. Es el mapeo que
sostiene todo el servicio.

| Verbo | Ruta | Cuerpo | Éxito | Error |
|---|---|---|---|---|
| GET | `/fuentes` | — | 200 | 400 |
| GET | `/fuentes/{id}` | — | 200 | 404 |
| POST | `/fuentes` | sí | 201 + `Location` | 400 · 401 · 404 · 409 |
| PUT | `/fuentes/{id}` | sí | 200 | 400 · 401 · 404 |
| DELETE | `/fuentes/{id}` | — | 204 | 401 · 404 |

**Parámetros de `GET /fuentes`**: `supermercado`, `ingredienteId`, `activa`,
`pagina`, `tamano`.

```json
{
  "id": 1,
  "supermercado": "JUMBO",
  "ingredienteId": "cebolla",
  "categoriaRuta": "/frutas-y-verduras/verduras/verdura",
  "terminos": ["cebolla"],
  "excluir": ["cebollin", "morada", "polvo", "frita"],
  "medidaEsperada": "g",
  "unidadesBasePorMedida": 1,
  "activa": true
}
```

| Campo | Regla |
|---|---|
| `supermercado` | código existente, si no `404` |
| `ingredienteId` | ingrediente existente, si no `404` |
| `categoriaRuta` | empieza con `/`, no vacía |
| `medidaEsperada` | `g` `ml` `un` — la unidad canónica en que la cadena publica su precio por unidad de medida |
| `unidadesBasePorMedida` | mayor que 0 — cuántas unidades base del catálogo entrega una unidad canónica del producto |

El par (`supermercado`, `ingredienteId`) es único: dos fuentes para el mismo par
harían que el precio dependiera de cuál se leyó primero. El segundo `POST`
recibe `409`.

---

### 4 · `precios` — CRUD completo

El recurso que consume la app. Cada medición se **inserta**; ninguna se
sobrescribe, así que la tabla es la historia del precio y `GET /precios`
devuelve, por omisión, solo el valor vigente de cada ingrediente.

| Verbo | Ruta | Cuerpo | Éxito | Error |
|---|---|---|---|---|
| GET | `/precios` | — | 200 | 400 |
| GET | `/precios/{id}` | — | 200 | 404 |
| POST | `/precios` | sí | 201 + `Location` | 400 · 401 · 404 · 409 |
| PUT | `/precios/{id}` | sí | 200 | 400 · 401 · 404 |
| DELETE | `/precios/{id}` | — | 204 | 401 · 404 |

**Parámetros de `GET /precios`**

| Parámetro | Tipo | Por omisión | Qué hace |
|---|---|---|---|
| `supermercado` | código | — | **obligatorio** si `historia=false` |
| `origen` | `MEDIDO` `ESTIMADO` | — | filtra por procedencia |
| `ingredienteId` | id | — | un solo ingrediente |
| `historia` | booleano | `false` | `true` devuelve todas las mediciones en lugar de la vigente |
| `pagina`, `tamano` | | 1, 200 | solo cuando `historia=true` |

**Respuesta de la consulta vigente** — esta es la forma que la app guarda en su
caché local:

```json
{
  "supermercado": "JUMBO",
  "actualizadoEn": "2026-10-29T11:40:22-03:00",
  "cantidad": 98,
  "datos": [
    {
      "id": 1042,
      "ingredienteId": "cebolla",
      "precioClpPorUnidad": 1.975,
      "unidad": "g",
      "origen": "MEDIDO",
      "candidatos": 2,
      "recoleccionId": 7,
      "medidoEn": "2026-10-29T11:40:22-03:00"
    }
  ]
}
```

**Cuerpo de `POST` y `PUT`**

```json
{
  "supermercado": "JUMBO",
  "ingredienteId": "cebolla",
  "precioClpPorUnidad": 1.975,
  "unidad": "g",
  "origen": "MEDIDO",
  "candidatos": 2,
  "medidoEn": "2026-10-29T11:40:22-03:00"
}
```

`medidoEn` es opcional y por omisión es el instante de la petición. La terna
(`supermercado`, `ingredienteId`, `medidoEn`) es única: la misma medición
cargada dos veces responde `409`.

---

### 5 · `recolecciones` — crear y leer

Una corrida del recolector contra el sitio de la cadena. Se modela como recurso
y no como una acción (`/recolectar`) porque una corrida **tiene estado y dura**:
hay que poder preguntarle cómo va y qué dejó.

| Verbo | Ruta | Cuerpo | Éxito | Error |
|---|---|---|---|---|
| GET | `/recolecciones` | — | 200 | 400 |
| GET | `/recolecciones/{id}` | — | 200 | 404 |
| POST | `/recolecciones` | sí | **202** + `Location` | 400 · 401 · 409 · 503 |
| DELETE | `/recolecciones/{id}` | — | 204 | 401 · 404 · 409 |

`POST` responde **202 Accepted**, no 201: la corrida arranca en segundo plano y
la respuesta llega antes de que termine. El `Location` apunta al recurso donde
se sigue su avance.

```json
{ "supermercado": "JUMBO", "ingredientes": ["cebolla", "papa"] }
```

`ingredientes` es opcional; sin él se recorren todas las fuentes activas de la
cadena.

```json
{
  "id": 7,
  "supermercado": "JUMBO",
  "estado": "COMPLETADA",
  "iniciadaEn": "2026-10-29T11:38:02-03:00",
  "terminadaEn": "2026-10-29T11:40:22-03:00",
  "categoriasPedidas": 29,
  "categoriasLeidas": 29,
  "productosLeidos": 1520,
  "preciosEscritos": 96,
  "detalle": null
}
```

| `estado` | Qué significa |
|---|---|
| `EN_CURSO` | está corriendo; `terminadaEn` es `null` |
| `COMPLETADA` | terminó y escribió precios |
| `FALLIDA` | terminó sin poder leer la cadena; `detalle` dice por qué |

- Una segunda recolección de la misma cadena mientras hay una `EN_CURSO`
  responde `409`: dos corridas simultáneas escribirían la misma fila.
- `DELETE` de una recolección `EN_CURSO` responde `409`.

---

## Salud del servicio

| Verbo | Ruta | Para qué |
|---|---|---|
| GET | `/salud` | responde 200 con la versión y el estado de la conexión a la base, o 503 si la base no responde |

Vive fuera de `/api/v1` porque no es parte del contrato de datos: es para el
despliegue.

---

## Tabla completa de rutas

| # | Verbo | Ruta | Éxito | Errores |
|---|---|---|---|---|
| 1 | GET | `/api/v1/supermercados` | 200 | — |
| 2 | GET | `/api/v1/supermercados/{codigo}` | 200 | 404 |
| 3 | GET | `/api/v1/ingredientes` | 200 | 400 |
| 4 | GET | `/api/v1/ingredientes/{id}` | 200 | 404 |
| 5 | POST | `/api/v1/ingredientes` | 201 | 400 · 401 · 409 |
| 6 | PUT | `/api/v1/ingredientes/{id}` | 200 | 400 · 401 · 404 |
| 7 | DELETE | `/api/v1/ingredientes/{id}` | 204 | 401 · 404 |
| 8 | GET | `/api/v1/fuentes` | 200 | 400 |
| 9 | GET | `/api/v1/fuentes/{id}` | 200 | 404 |
| 10 | POST | `/api/v1/fuentes` | 201 | 400 · 401 · 404 · 409 |
| 11 | PUT | `/api/v1/fuentes/{id}` | 200 | 400 · 401 · 404 |
| 12 | DELETE | `/api/v1/fuentes/{id}` | 204 | 401 · 404 |
| 13 | GET | `/api/v1/precios` | 200 | 400 |
| 14 | GET | `/api/v1/precios/{id}` | 200 | 404 |
| 15 | POST | `/api/v1/precios` | 201 | 400 · 401 · 404 · 409 |
| 16 | PUT | `/api/v1/precios/{id}` | 200 | 400 · 401 · 404 |
| 17 | DELETE | `/api/v1/precios/{id}` | 204 | 401 · 404 |
| 18 | GET | `/api/v1/recolecciones` | 200 | 400 |
| 19 | GET | `/api/v1/recolecciones/{id}` | 200 | 404 |
| 20 | POST | `/api/v1/recolecciones` | 202 | 400 · 401 · 409 · 503 |
| 21 | DELETE | `/api/v1/recolecciones/{id}` | 204 | 401 · 404 · 409 |
| 22 | GET | `/salud` | 200 | 503 |

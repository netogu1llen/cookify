# Manual de uso e instalación · Cookify

Cookify arma una semana de almuerzos que cabe en un presupuesto, con el precio del
supermercado donde se compra. Son dos piezas: una **app Android** y un **servicio de
precios** que ya está publicado y funcionando.

Para usar la app **no hace falta levantar nada**: viene apuntando al servicio publicado.
El servicio solo se instala si se quiere correr todo en un equipo propio.

Este manual está escrito para alguien que no trabajó en el proyecto. Cada paso dice qué
se debe ver cuando sale bien, para poder detectar dónde se rompió cuando sale mal.

| | |
|---|---|
| Repositorio | <https://github.com/netogu1llen/cookify> |
| Servicio publicado | <https://cookifies.duckdns.org> |
| Prototipo navegable | <https://netogu1llen.github.io/cookify/fuentes/prototipo.html> |

---

## Requisitos

**Para usar la app**

| | |
|---|---|
| Teléfono | Android 7.0 o superior (API 24), o un emulador |
| Espacio | 25 MB |
| Red | Wi‑Fi o datos. Sin red la app **igual funciona**, con precios estimados |

**Para compilar la app desde el código**

| | |
|---|---|
| JDK | 21 o superior |
| SDK de Android | Plataforma 37 instalada |
| Gradle | No hace falta instalarlo: lo baja el wrapper del repositorio |

**Para levantar el servicio de precios**

| | |
|---|---|
| Docker | Con Compose v2 (`docker compose`, sin guion) |
| JDK | 21 o superior |
| Puertos libres | 5434 para PostgreSQL, 8090 para el servicio |

---

## Parte 1 · Instalar la app

### Opción A · Instalar el APK ya compilado

Es el camino corto y el que se recomienda para evaluar el producto.

1. En el teléfono, abrir <https://github.com/netogu1llen/cookify/releases> y descargar
   el archivo `cookify.apk` de la versión más reciente.
2. Abrir el archivo descargado. Android pregunta si se permite instalar aplicaciones de
   esta fuente: hay que aceptar. Es lo normal para un APK que no viene de Play.
3. Instalar y abrir.

El APK es de depuración y está firmado con la clave de depuración, así que convive sin
problema con cualquier otra app: el identificador es `com.app.cookify.debug`.

### Opción B · Compilar desde el código

```bash
git clone https://github.com/netogu1llen/cookify.git
cd cookify

./gradlew :app:assembleDebug
# el APK queda en app/build/outputs/apk/debug/app-debug.apk
```

Para instalarlo en un teléfono o emulador conectado por ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Y para comprobar que el proyecto está sano antes de confiar en él:

```bash
./gradlew test detekt     # pruebas de la app y análisis estático
```

### Apuntar la app a otro servicio

La dirección del servicio **se fija al compilar**, no se lee en ejecución: así no hay
forma de desviar la app a otro servidor sin recompilarla. Por defecto es
`https://cookifies.duckdns.org`. Para compilarla contra un servicio local:

```bash
./gradlew :app:assembleDebug -PcookifyApiUrl=http://10.0.2.2:8090
```

`10.0.2.2` y no `localhost`: dentro del emulador, `localhost` es el emulador mismo.
Desde un teléfono físico hay que poner la IP del equipo en la red local.

La dirección con la que se compiló se puede leer siempre en la app, en **Estado del
servicio** (el engranaje de la portada).

---

## Parte 2 · Usar la app

### Datos de prueba

Estos valores sirven para recorrer el producto completo y dan un resultado holgado, que
no obliga a pasar por la pantalla de presupuesto insuficiente.

| Pregunta | Qué responder |
|---|---|
| ¿En qué súper compras? | **Jumbo** (es una de las dos cadenas con precio medido) |
| ¿Para cuántas personas cocinas? | **4** |
| ¿Qué días vas a cocinar? | **Lunes, miércoles y viernes** |
| ¿Cuánto quieres gastar? | **45.000** |
| ¿Qué se te antoja? | **Rápido y sin esfuerzo** y **Alto en proteína** |
| ¿Alguna restricción? | **Ninguna** |
| ¿Con qué cocinas? | **Estufa** y **Horno** |

Para probar el camino de error, repetir con **$6.000** de presupuesto: no alcanza y la
app lo dice en vez de fallar en silencio.

Para probar el filtro duro, repetir con restricción **Vegano**: el recetario se filtra
entero y la semana sale igual.

### El recorrido

1. **Portada.** La primera vez está vacía y ofrece *Armar mi primera semana*.
2. **Siete preguntas.** Una por pantalla, con la barra de pasos arriba. *Continuar* se
   habilita solo cuando la respuesta es válida: el presupuesto, por ejemplo, exige al
   menos $5.000, porque por debajo de eso no hay semana posible y es mejor decirlo
   antes de armarla.
3. **Armado.** Se ven las verificaciones que va pasando el motor. Dura unos segundos.
4. **La semana.** Un plato por día elegido, con su tiempo y su costo, y arriba la barra
   de presupuesto. Debajo del total aparece el sello de precios:
   - **«Precios medidos en el súper · 09/11/26 22:50»** → el precio vino del servicio y
     esa es la fecha en que se midió en la cadena.
   - **«Sin conexión · Precios estimados»** → no se pudo hablar con el servicio y la
     semana se armó con el catálogo que viaja dentro del APK.
5. **Detalle del plato.** Al tocar una tarjeta: ingredientes ya escalados a las personas
   que se indicaron —no la receta por porción, sino lo que hay que comprar—,
   instrucciones y el mismo sello de procedencia del precio.
6. **Guardar.** Botón *Guardar semana*; se puede cambiar el nombre, que viene propuesto
   como «Semana del 22 de noviembre».
7. **La semana queda en la portada** y se puede reabrir.

### Comprobar que el dato sobrevive

Cerrar la app por completo (deslizarla fuera de las aplicaciones recientes) y volver a
abrirla. La semana sigue en la portada, con el mismo nombre, el mismo total y los mismos
platos. Se guarda en la base de datos del teléfono —Room sobre SQLite—, no en memoria.

### Comprobar que la app habla de verdad con el servicio

Esta es la prueba que no se puede fingir:

1. Tocar el **engranaje** arriba a la derecha de la portada → **Estado del servicio**.
2. Se ve con qué dirección se compiló esta versión de la app.
3. **Probar conexión** → debe responder *Conectado*, el tiempo que tardó el viaje, la
   versión del servicio y si la base de datos responde.
4. Abajo, *Precios guardados en este teléfono* muestra, por cadena, cuántos precios hay,
   cuándo se midieron en el súper y cuándo se trajeron al teléfono.
5. **Olvidar precios guardados** borra la caché —no las semanas— y obliga a la app a
   volver a pedírselos al servicio la próxima vez. Armar otra semana después de esto
   prueba la ida y la vuelta completas.

Para ver el comportamiento sin red: activar modo avión, usar *Olvidar precios guardados*
y armar una semana. El sello cambia a *Sin conexión · Precios estimados* y el total
sube, porque el valor estimado del catálogo es más conservador que el medido.

---

## Parte 3 · El servicio de precios

### Usarlo publicado

No hay que instalar nada. Está arriba con certificado de Let's Encrypt:

```bash
curl -s https://cookifies.duckdns.org/salud
# {"estado":"arriba","version":"1.0.0","baseDeDatos":"responde"}

curl -s "https://cookifies.duckdns.org/api/v1/precios?supermercado=JUMBO"
```

La lectura es abierta. La escritura exige la cabecera `X-API-Key`.

### Levantarlo en un equipo propio

```bash
git clone https://github.com/netogu1llen/cookify.git
cd cookify

cp .env.example .env        # y editar tres valores, ver abajo
docker compose up -d db     # PostgreSQL con el esquema y la semilla ya aplicados

./gradlew -p server test    # 33 pruebas, sin base ni red
./gradlew -p server run     # el servicio, en el puerto 8090
```

Verificación en una línea:

```bash
curl -s http://localhost:8090/salud
```

Los tres valores de `.env` que hay que cambiar:

| Variable | Qué poner |
|---|---|
| `POSTGRES_PASSWORD` | cualquier cosa; la base solo escucha en localhost |
| `COOKIFY_DB_CLAVE` | **el mismo valor** que `POSTGRES_PASSWORD` |
| `COOKIFY_API_KEY` | al menos 16 caracteres · `openssl rand -hex 24` |

El servicio **no arranca** si falta alguna. Arrancar con la clave vacía dejaría la API
abierta a escritura sin que nadie se diera cuenta hasta que alguien escribiera.

Todo en contenedores, con un comando:

```bash
docker compose up -d        # la base y el servicio ya empaquetado
```

### Probar la API entera

```bash
bash tools/probar_api.sh                    # contra localhost:8090
COOKIFY_BASE_URL=https://cookifies.duckdns.org bash tools/probar_api.sh
# ok: 48   fallos: 0
```

También está la colección de Postman en `postman/cookify-api.postman_collection.json`:
35 peticiones listas para importar. La clave de la API va en la variable `apiKey` de la
colección, que se versiona vacía a propósito.

Ejemplo de escritura, que es lo que pide la cabecera:

```bash
curl -s -X POST http://localhost:8090/api/v1/ingredientes \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $COOKIFY_API_KEY" \
  -d '{"id":"quinoa","nombre":"Quinoa","pasillo":"ABARROTES","unidad":"g","precioBaseClp":4.5}'
# 201 Created
```

Los cinco campos son obligatorios. Si falta uno, la respuesta es **400** y dice cuál y
por qué, campo por campo, en vez de un error genérico:

```json
{"error":{"codigo":"VALIDACION","mensaje":"El cuerpo de la peticion no es valido",
 "detalles":[{"campo":"precioBaseClp","problema":"es obligatorio"}]}}
```

Sin la cabecera devuelve **401**, también con cuerpo y no con una página en blanco:

```bash
curl -s -X POST https://cookifies.duckdns.org/api/v1/ingredientes \
  -H "Content-Type: application/json" -d '{"id":"prueba"}'
# {"error":{"codigo":"NO_AUTORIZADO","mensaje":"Falta la cabecera X-API-Key o no corresponde","detalles":[]}}
```

El contrato completo de las 22 rutas está en [`docs/api-cookify.md`](api-cookify.md).

---

## Fallas frecuentes

### 1 · La app dice «Sin conexión · Precios estimados»

**Qué pasó.** La app no pudo hablar con el servicio y siguió funcionando con el catálogo
que trae dentro. Es el comportamiento diseñado, no una caída: la semana se arma igual.

**Cómo confirmarlo.** Engranaje → *Estado del servicio* → *Probar conexión*.

**Solución.** Si dice *No se pudo conectar*: revisar la red del teléfono y comprobar
desde un navegador que <https://cookifies.duckdns.org/salud> responde. Si el servicio
está arriba y el teléfono tiene red, es que la app se compiló apuntando a otra
dirección: la que usa está escrita en esa misma pantalla.

### 2 · `docker compose up -d` levanta la base, pero no hay tablas ni precios

**Qué pasó.** Los scripts de `db/` se ejecutan **solo cuando el volumen se crea**.
Si el volumen ya existía de un intento anterior, editar el esquema o la semilla no
cambia nada y la base queda vacía o a medias.

**Solución.**

```bash
docker compose down -v && docker compose up -d db
```

El `-v` es lo que importa: borra el volumen para que la base se cree de cero.

### 3 · En Linux o macOS los scripts fallan con `$'\r': command not found`

**Qué pasó.** El repositorio se clonó en Windows con finales de línea CRLF y los scripts
`.sh` llegaron con el retorno de carro pegado a cada línea.

**Solución.** El repositorio ya trae `.gitattributes` que fuerza LF en los scripts, así
que basta con volver a clonar. Sobre una copia ya rota:

```bash
sed -i 's/\r$//' tools/probar_api.sh
```

### 4 · El servicio no arranca y el log dice que falta configuración

**Qué pasó.** Falta `.env`, o falta alguno de los tres valores obligatorios, o
`COOKIFY_API_KEY` tiene menos de 16 caracteres. El servicio se niega a arrancar a
propósito en vez de quedar abierto a escritura.

**Solución.** `cp .env.example .env` y completar los tres valores de la tabla de arriba.
`COOKIFY_DB_CLAVE` tiene que ser idéntica a `POSTGRES_PASSWORD`.

### 5 · El puerto 8090 o el 5434 están ocupados

**Qué pasó.** Otro proceso los tiene tomados. Pasa seguido con el 8080 —por eso el
servicio no lo usa— y con el 5432 si ya hay un PostgreSQL instalado.

**Solución.** Cambiar `COOKIFY_PUERTO` o `POSTGRES_PORT` en `.env` y volver a levantar.
Si se cambia el puerto de la base hay que actualizar también `COOKIFY_DB_URL`.

### 6 · `./gradlew :app:assembleDebug` falla antes de compilar

**Qué pasó.** Casi siempre es el JDK o la plataforma del SDK. El proyecto necesita
**JDK 21 o superior** y la **plataforma 37** de Android instalada.

**Solución.** `java -version` para confirmar la versión, y en Android Studio
*SDK Manager → Android 37* para instalar la plataforma. El error de Gradle nombra cuál
de las dos falta.

### 7 · La recolección de precios devuelve 502 de Santa Isabel

**Qué pasó.** Se recolectó sin pausa entre peticiones. El sitio empieza a responder 502
cuando las recibe seguidas.

**Solución.** `COOKIFY_RECOLECTOR_PAUSA_MS=400` en `.env`, que es el valor por defecto.
No es un ajuste de rendimiento sino de cortesía con el sitio del que se lee.

---

## Límites conocidos

- El precio está **medido** solo en Jumbo y Santa Isabel, que son las dos cadenas que
  publican su catálogo en páginas de categoría. En las otras cuatro —Líder, aCuenta,
  Tottus y Unimarc— el precio es **estimado**, con un multiplicador por cadena, y la app
  lo dice en pantalla en vez de aparentar una precisión que no tiene.
- La app pide precios al servicio **una vez cada siete días** por cadena. No hay
  actualización en vivo: lo que se ve es lo que se midió, con su fecha a la vista.
- Las semanas guardadas viven **en el teléfono**. No hay cuenta de usuario ni sincronía
  entre dispositivos: desinstalar la app borra las semanas.
- El recetario —69 recetas, 98 ingredientes— viaja dentro del APK y se actualiza
  recompilando, no desde el servicio.

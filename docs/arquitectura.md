# Arquitectura de Cookify

![Diagrama de arquitectura](arquitectura.png)

Cookify es una aplicación **cliente–servidor en capas**. El cliente es una app Android
nativa que funciona completa y sin red; el servidor es un servicio de precios que ya está
construido y corriendo, con su propio documento en
[server/README.md](../server/README.md) y su contrato en
[docs/api-cookify.md](api-cookify.md).

La regla que ordena el diseño: **cada capa solo conoce la que tiene debajo, y el dominio
no conoce a ninguna**. `:core:domain` no tiene una sola importación de Android, así que el
motor de planificación se prueba con JUnit en la JVM, sin emulador y en milisegundos.

---

## Las capas del cliente y de qué responde cada una

| Capa | Módulos Gradle | De qué responde | De qué **no** responde |
|---|---|---|---|
| Presentación | `:app`, `:feature:*`, `:core:ui` | Dibujar y recibir gestos | No calcula precios ni elige recetas |
| Dominio | `:core:domain`, `:core:model` | Qué se come y cuánto cuesta | No sabe si el dato viene de disco o de red |
| Datos | `:core:data` | Resolver de dónde sale cada dato | No decide reglas de negocio |
| Persistencia | `:core:database`, `assets/` | Guardar y leer | No transforma nada |

El dominio define `SemanaRepository` y `PrecioRepository` como **interfaces**, y `:core:data`
las implementa. Esa inversión es la que permitió que el Hito 2 construyera el servidor **sin
tocar el motor de planificación**: cambia quién responde el precio, no quién lo usa. Y es la
que hizo que conectar la app al servicio, en el Hito 3, fuera escribir una implementación más
—`PrecioRepositoryRemoto`, que decora a la local— y cambiar qué se registra en Hilt. El motor
de planificación no se tocó.

---

## Caso de uso recorrido: «armar mi semana»

Es el recorrido completo del producto. Sigue el camino de un toque hasta la pantalla final.

**1 · Presentación** — El usuario contesta siete pasos. `OnboardingViewModel` guarda cada
respuesta en un `SavedStateHandle` (sobrevive a que Android mate el proceso) y valida en
cada paso si `puedeAvanzar`. Al terminar emite por un `Channel` el evento
`EventoOnboarding.Listo(SolicitudPlan)`.

> `SolicitudPlan` es el contrato entre capas: supermercado, personas, días, presupuesto,
> antojos, restricción y artefactos. Nada más cruza hacia abajo.

**2 · Presentación** — `MainActivity` navega a `Ruta.Armado(solicitud)`. La ruta lleva la
solicitud y una semilla, **nunca el plan ya construido**: así la navegación se serializa
barata y el plan se reconstruye igual en cualquier momento.

**3 · Dominio** — `ArmadoViewModel` llama a `GenerarPlanSemanalUseCase(solicitud, semilla)`,
que ejecuta cuatro pasos sobre las 69 recetas:
filtra por restricción y por artefactos disponibles → puntúa cada receta según los antojos
pedidos → selecciona sin repetir plato ni abusar de una misma proteína → ajusta a
presupuesto. Es **determinista**: misma semilla, mismo plan.

**4 · Datos** — El motor pide el catálogo a `CatalogoLocalDataSource`, que lee
`recetas.json` e `ingredientes.json` una sola vez al abrir la app y los deja en memoria.
Son de solo lectura, así que no pasan por Room.

**5 · Datos → Red** — `PrecioRepositoryRemoto` resuelve el precio de la cadena en cascada,
y se queda con el primer escalón que responda:

```
caché fresca (< 7 días)  →  servicio  →  caché vencida  →  valor de ingredientes.json
```

Con la caché fresca **ni se intenta la red**. Si venció, `ClientePrecios` pide
`GET /api/v1/precios?supermercado=JUMBO` y, si llega, guarda el resultado en la tabla
`precios_cache`. Si el servicio está caído o responde una lista vacía —que para la app es
lo mismo— se usa la caché vencida: un precio medido hace un mes vale más que uno estimado
hoy. El respaldo al catálogo local no es opcional: **sin él no se puede armar una semana
sin señal**.

El catálogo empaquetado manda en qué ingredientes existen y en qué unidad se miden; el
servicio solo aporta el precio. Un precio que no venga, o que venga en cero, deja al
ingrediente con su estimación en vez de sacarlo del mapa: el motor fallaría al costear una
receta que lo use. Las dos reglas salieron de pruebas que fallaron, no de suponerlas.

Cada `PreciosDeCadena` llega marcada con su procedencia —`SERVICIO`, `CACHE` o `CATALOGO`—,
la fecha en que se midió y si hubo conexión. Eso es lo que la pantalla muestra como
«Precios medidos en el súper · 09/11/26 22:50» o «Sin conexión · Precios estimados»: la app
no afirma una precisión que no tiene.

**6 · Dominio** — `CalculadoraCostos` escala cada ingrediente por el número de personas y
**redondea hacia arriba lo que se vende por unidad**: media palta no existe en el
supermercado, y el presupuesto tiene que cobrar la palta entera que el usuario va a pagar.

**7 · Presentación** — El motor termina en milisegundos, así que `ArmadoViewModel`
escenifica cuatro verificaciones de ~750 ms que nombran los datos reales del usuario. Si el
presupuesto no alcanza, la secuencia se detiene en la etapa que falló y ofrece el mínimo
viable calculado. Al terminar emite `EventoArmado.Listo(solicitud, semilla)`.

**8 · Persistencia** — Al tocar «Guardar semana», `SemanaRepositoryLocal` escribe en Room a
través de `SemanaDao`, en dos tablas con borrado en cascada. Se guarda **el plato de cada
día, no la semilla**: si mañana cambia el catálogo, la semana guardada no muta.

**9 · Presentación** — `HomeViewModel` observa `repositorio.observar(): Flow<List<SemanaGuardada>>`.
La portada se actualiza sola, sin que nadie le avise.

---

## Por qué hay un servidor

La decisión no salió de un requisito académico sino de una medición. En el Hito 1 se
comparó el precio de los **17 productos idénticos** presentes el mismo día en Jumbo y en
Santa Isabel: la diferencia iba de **−9,1% a +57,4% según el producto, no según la cadena**.

El Hito 2 amplió esa medición a **2.608 productos** de las dos cadenas, con el mismo
resultado y más fuerte: el desvío contra los precios de referencia tiene una mediana de
+23% y un rango de −65% a +313%. No existe un multiplicador por supermercado, así que el
precio hay que **medirlo**, no estimarlo.

Medido el costo de hacerlo desde el teléfono:

| | Peso comprimido | Tiempo |
|---|---|---|
| Una categoría de Jumbo | 302 KB | 2,8 s |
| Una búsqueda de Santa Isabel | 41 KB | 1,1 s |
| **Un refresco completo** | **6–9 MB** | **10–20 s** |

Tres razones para que eso viva en un servidor y no en cada teléfono:

1. **Costo por cliente.** 6–9 MB por usuario contra una sola descarga que sirve a todos.
2. **Fragilidad.** Cuando la cadena cambia su sitio, se cae un servicio que se arregla en
   una tarde; no una app instalada que necesita pasar por Play.
3. **Cortesía de red.** Un servicio consulta una vez cada varias horas; mil teléfonos
   consultando es otra cosa.

## Decisión de renderizado

El cliente es **nativo, no web**. El equivalente móvil de la discusión entre página única y
multipágina es **una sola Activity con navegación declarativa** (`NavDisplay` de
Navigation3) frente a varias Activities. Se eligió la primera: el estado del onboarding
vive en un solo `ViewModel` y las transiciones entre los siete pasos no pierden lo ya
respondido. El costo asumido es que toda la navegación depende de una librería en versión
1.1.4, todavía joven.

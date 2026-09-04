# Cookify — Planificador de recetas semanales

## Contexto

`C:\Users\ernes\Desktop\cookify` es hoy un template stock de Android Studio ("Empty Compose Activity"): 3 archivos Kotlin de boilerplate, sin git, sin Room, Hilt, Navigation ni red. Todo el producto está por construirse.

El objetivo es una app Android que arme una semana de **almuerzos** (1 platillo por día, L–D) a partir de: supermercado, número de personas, cantidad de días, presupuesto en CLP, antojos (máx. 3), restricción alimentaria y artefactos de cocina disponibles. La app calcula qué recetas caben en el presupuesto, muestra la semana, permite ver el detalle de cada plato y guardar la semana para verla en la home al reabrir.

**Decisiones tomadas:**
- Motor: **catálogo local curado** (JSON empaquetado en `assets/`), algoritmo local, 100% offline. Sin API externa.
- Precios: **tabla local por ingrediente + multiplicador por supermercado**.
- Idioma: **español (Chile)**.
- Alcance: **MVP completo del flujo** (onboarding → carga → semana → detalle → guardar → home). Sin cuentas, sin backend.
- Stack: **el moderno del skill** — AGP 9.3.1, Kotlin 2.3.21, compileSdk/targetSdk 37, Navigation3, Room 3, Hilt.
- Estructura: **multi-módulo con `build-logic` y convention plugins**.
- Estilo visual: **oscuro premium**.

---

## Fase 0 ✅ — Toolchain (bloqueante, parte manual del usuario)

**Estado actual detectado:** Android Studio 2024.3 (Meerkat, build 243), JBR 21, SDK con plataformas 33/34/35, build-tools hasta 36.0.0, Gradle wrapper 8.11.1, sin git. `sdkmanager` CLI disponible en `Sdk/cmdline-tools/latest/bin/`.

El stack elegido (AGP 9.3.1) **no abre en Studio 2024.3**. Pasos:

1. **Usuario:** instalar Android Studio 2025.x (Narwhal/Otter o superior).
2. **Claude (CLI, no necesita Studio):** bajar la plataforma con
   `sdkmanager "platforms;android-37" "build-tools;37.0.0"` y aceptar licencias.
3. **Claude:** subir el wrapper a la versión de Gradle que exige AGP 9.3.1 (`gradle/wrapper/gradle-wrapper.properties`).
4. **Claude:** `git init` + commit inicial del template antes de tocar nada, para tener punto de retorno.

> Se puede escribir y compilar todo desde CLI usando la JBR 21 ya instalada (`org.gradle.java.home`), así que la **Fase 1 arranca en paralelo mientras se descarga el Studio nuevo**. Studio sólo hace falta para el IDE, el emulador y los previews.

**Verificación:** `./gradlew --version` reporta la versión esperada y `./gradlew help` corre limpio.

---

## Fase 1 ✅ — Bootstrap multi-módulo + `build-logic`

Fuente de verdad: `C:\Users\ernes\.claude\skills\claude-android-ninja\assets\`. Se copia, no se reinventa.

1. Reemplazar `gradle/libs.versions.toml` con `assets/libs.versions.toml.template` completo (incluye el bloque `[plugins]` de convention plugins y los bundles `compose`, `navigation3`, `adaptive`, `unit-test`, `android-test`).
2. Crear `build-logic/convention/src/main/kotlin/` copiando **todos** los `.kt` de `assets/convention/` (incluido `config/`), más `build-logic/convention/build.gradle.kts` desde `assets/convention/build.gradle.kts`.
3. Crear `build-logic/settings.gradle.kts` con el `versionCatalogs { from(files("../gradle/libs.versions.toml")) }` según `assets/convention/QUICK_REFERENCE.md`.
4. Reescribir `settings.gradle.kts` desde `assets/settings.gradle.kts.template`: `includeBuild("build-logic")`, `rootProject.name = "cookify"` e includes de módulos.
5. `config/detekt.yml` desde `assets/detekt.yml.template`; `app/proguard-rules.pro` desde `assets/proguard-rules.pro.template`.
6. `gradle.properties`: activar `org.gradle.parallel`, `org.gradle.caching`, `org.gradle.configuration-cache`, subir `jvmargs` a `-Xmx4096m`.

**Módulos a incluir** (el `AndroidFeatureConventionPlugin` inyecta automáticamente `:core:ui`, `:core:domain` y `:core:data` en cada feature, así que esos tres son obligatorios):

| Módulo | Plugin(s) de convención | Contenido |
|---|---|---|
| `:app` | `app.android.application` + `.compose` + `app.hilt` + `app.detekt` + `app.spotless` | `MainActivity`, `CookifyApplication` (`@HiltAndroidApp`), `NavDisplay` + backstack Nav3, entry providers |
| `:core:model` | `app.jvm.library` + `app.kotlin.serialization` | Kotlin puro: modelos y enums del dominio |
| `:core:domain` | `app.jvm.library` | Kotlin puro: interfaces de repositorio + **el motor de planificación** |
| `:core:data` | `app.android.library` + `app.hilt` + `app.kotlin.serialization` | Carga del catálogo desde `assets/`, calculadora de precios, impl. de repositorios, módulos Hilt |
| `:core:database` | `app.android.library` + `app.hilt` + `app.android.room` | Room 3: entidades, DAOs, `CookifyDatabase` |
| `:core:ui` | `app.android.library` + `.compose` | Tema oscuro premium + design system |
| `:core:common` | `app.jvm.library` | Dispatchers, `Result`, utilidades |
| `:feature:onboarding` | `app.android.feature` | Wizard de 7 pasos |
| `:feature:planning` | `app.android.feature` | Pantalla de carga con checks |
| `:feature:week` | `app.android.feature` | Semana L–D + detalle de receta |
| `:feature:home` | `app.android.feature` | Semanas guardadas |

**Ajuste al template:** la función `validateProjectStructure()` de `assets/settings.gradle.kts.template` chequea el prefijo `":feature-"`; hay que cambiarlo a `":feature:"` porque usamos módulos anidados.

**Verificación:** `./gradlew help` y luego `./gradlew :app:assembleDebug` compilan (paso obligatorio del skill tras cambios de módulos/DI/toolchain).

---

## Fase 2 ✅ — Design system oscuro premium (`:core:ui`)

Aplicar `references/android-theming-quick.md`: **set completo de roles M3** en `Color.kt` (incluidos `surfaceContainer*`, `*Dim`/`*Bright`, `*Fixed`/`*FixedDim`), nunca `Color(0xFF…)` crudo dentro de composables.

**Paleta propuesta** (a afinar al verla corriendo):
- `background` `#0B0B0E`, `surface` `#111116`, `surfaceContainer` `#191920`, `surfaceContainerHigh` `#22222B`
- `primary` ámbar-naranjo `#FF7A3D` (CTAs, precio, día activo) con `onPrimary` `#22090B`
- `secondary` verde lima `#B9F227` para tags de nutrición/salud
- `tertiary` para tags de tiempo/rapidez; `error` reservado para "fuera de presupuesto"
- Profundidad por **tono de contenedor**, no por sombra; `outlineVariant` para divisores, `outline` para bordes interactivos.

Componentes compartidos: `CookifyScaffold`, `StepProgressBar`, `OptionCard` (seleccionable, con icono), `ChipToggle` (para antojos con límite de 3), `BudgetInput` (formateo CLP), `RecipeCard`, `DayRail`, `PrimaryButton` (thumb zone, 56dp, ancho completo), `CheckRow` (spinner → check), `EmptyState`.

**Restricción del skill:** prohibido `androidx.compose.material.icons.Icons.*` — los iconos van como drawables de **Material Symbols** vía `painterResource`. Hay que bajar el set de iconos que usemos a `core/ui/src/main/res/drawable/`.

Dynamic color (Material You) queda **desactivado por defecto**: la identidad de marca oscura es una decisión de producto explícita; el set completo de roles queda igual declarado.

**Verificación:** `@Preview` de cada componente en tema oscuro; `./gradlew :core:ui:assembleDebug` y `detekt`.

---

## Fase 3 ✅ — Modelo de dominio y catálogo de datos

### `:core:model`
```
enum Supermercado(nombre, multiplicador)   // Líder 1.00 (base), Tottus 0.98, Santa Isabel 1.08,
                                            // Jumbo 1.15, Unimarc 1.05, aCuenta 0.92
enum Antojo   // RAPIDO, BAJO_CALORIAS, PARA_TODOS, SANO, TAKEAWAY, GUT_FRIENDLY, ALTO_PROTEINA
enum Restriccion // NINGUNA, VEGETARIANO, VEGANO, SOLO_PESCADO
enum Artefacto   // ESTUFA, HORNO, AIRFRYER, HORNO_ELECTRICO
enum Pasillo     // VERDULERIA, CARNICERIA, PESCADERIA, LACTEOS, ABARROTES, CONGELADOS, PANADERIA

data class SolicitudPlan(supermercado, personas, dias 1..7, presupuestoClp, antojos<=3, restriccion, artefactos)
data class Ingrediente(id, nombre, pasillo, unidad, precioBaseClpPorUnidad)
data class IngredienteReceta(ingredienteId, cantidadPorPorcion, unidad, opcional)
data class Receta(id, nombre, descripcion, minutosTotal, antojos:Set, aptaPara:Set<Restriccion>,
                  artefactosRequeridos:Set, artefactosAlternativos:List<Set>, porcionesBase,
                  proteinaPrincipal, ingredientes, pasos:List<String>, kcalPorPorcion, proteinaGPorPorcion)
data class DiaPlanificado(indice 0..6, diaSemana, receta, costoClp)
data class PlanSemanal(dias:List<DiaPlanificado>, costoTotalClp, solicitud, semilla)
```

### Catálogo (`core/data/src/main/assets/`)
- `ingredientes.json` — ~90 ingredientes con precio referencial CLP base Líder y pasillo.
- `recetas.json` — **~70 recetas chilenas/caseras**: ~40 omnívoras, ~14 vegetarianas, ~10 veganas, ~8 sólo pescado. Cada una con tags de antojo, artefactos requeridos, ingredientes por porción, pasos y macros.
  - El volumen importa: con menos de ~8 recetas viables por combinación de restricción el algoritmo no puede variar ni ajustar presupuesto.
- Parseo con **kotlinx-serialization** una sola vez, cacheado en memoria (`@Singleton CatalogoLocalDataSource`). No va a Room: es de sólo lectura.

**Cálculo de costo:**
`costo(receta) = Σ (precioBase(ing) × cantidadPorPorcion × personas) × multiplicador(supermercado)`, redondeado a la centena.

**Verificación:** test unitario que valida que cada `ingredienteId` de `recetas.json` existe en `ingredientes.json` y que no hay recetas sin pasos ni sin artefactos.

---

## Fase 4 ✅ — Motor de planificación (`:core:domain`)

`GenerarPlanSemanalUseCase` — Kotlin puro, sin Android, determinista dada una semilla. Es el corazón de la app y lo que más tests lleva.

1. **Filtrar** el catálogo: `restriccion ∈ receta.aptaPara` **y** los artefactos requeridos están cubiertos por los del usuario (considerando `artefactosAlternativos`: p. ej. "horno O airfryer").
2. **Puntuar** cada receta: +3 por cada antojo coincidente, bonus por macro relevante (proteína alta si pidió `ALTO_PROTEINA`, ≤500 kcal si pidió `BAJO_CALORIAS`, ≤25 min si pidió `RAPIDO`), penalización por repetir `proteinaPrincipal` ya usada en la semana.
3. **Selección greedy** de N días (N = días pedidos), sin recetas repetidas y máximo 2 platos con la misma proteína principal.
4. **Ajuste a presupuesto:**
   - Si el total excede el presupuesto → reemplazar iterativamente el plato más caro por la mejor alternativa más barata hasta calzar.
   - Si el total queda bajo el ~70% del presupuesto → subir de categoría 1–2 platos hacia los de mejor puntaje.
5. **Resultados posibles:** `Exito(plan)`, `PresupuestoInsuficiente(minimoNecesario)` (la UI ofrece subir el presupuesto o generar igual con el mínimo), `SinRecetasCompatibles(motivo)` (p. ej. vegano + sólo airfryer).
6. La `semilla` se guarda en el plan → "regenerar semana" produce una combinación distinta y reproducible.

**Verificación:** tests JUnit + Truth cubriendo: respeta restricción, respeta artefactos, no repite recetas, cae dentro del presupuesto, presupuesto imposible devuelve el error correcto, misma semilla ⇒ mismo plan.

---

## Fase 5 ✅ — Persistencia (`:core:database` + `:core:data`)

Room 3 (plugin `app.android.room`) con `BundledSQLiteDriver()` en `Room.databaseBuilder`, `@ColumnTypeConverter` (no `@TypeConverter`), DAOs `suspend`/`Flow`, y `room3 { schemaDirectory(...) }` con los schemas commiteados.

- `semanas_guardadas`: `id`, `nombre`, `creadaEn`, `supermercado`, `personas`, `dias`, `presupuestoClp`, `costoTotalClp`, `antojos`, `restriccion`, `artefactos`, `semilla`
- `dias_semana_guardada`: `id`, `semanaId` (FK con `onDelete = CASCADE`), `indiceDia`, `recetaId`, `costoClp`

Sólo se guardan **IDs de receta**; el detalle se resuelve contra el catálogo al leer. `SemanaRepository` expone `Flow<List<SemanaGuardada>>` para que la home se actualice sola.

**Verificación:** test instrumentado del DAO (insertar semana con 7 días, leer, borrar en cascada).

---

## Fase 6 ✅ — Onboarding: el wizard de 7 pasos (`:feature:onboarding`)

Un solo `OnboardingViewModel` con `StateFlow<OnboardingUiState>` sobre `SavedStateHandle` (sobrevive muerte de proceso) y un `Channel` para eventos one-shot (navegar a planificación). Nav3 maneja las claves; **cada paso es un pane dentro de una misma ruta** con transición horizontal, para no perder estado entre pasos.

Pasos, en orden:
1. **Supermercado** — grid 2×N de `OptionCard` con el logo/color de cada cadena.
2. **Personas** — stepper grande `−  4  +` (rango 1–12), número en tipografía monoespaciada.
3. **Días** — lista con los **nombres completos** de los días (Lunes, Martes, Miércoles, Jueves, Viernes, Sábado, Domingo), cada uno como fila seleccionable con check. Se eligen días concretos, no una cantidad; el orden L–D se respeta en el resultado.
4. **Presupuesto** — campo numérico con formateo CLP en vivo (`$45.000`) + chips de sugerencia rápida.
5. **Antojos** — 7 chips con icono, **máximo 3**: al llegar a 3 los demás se atenúan y el contador dice "3 de 3".
6. **Restricción** — 4 `OptionCard` de selección única.
7. **Artefactos** — selección múltiple; estufa preseleccionada; no se puede avanzar con cero.

Transversal: `StepProgressBar` arriba (7 segmentos), botón primario en la thumb zone deshabilitado hasta que el paso sea válido, back que retrocede paso a paso, y copy en español chileno.

**Verificación:** test de Compose UI que recorre los 7 pasos y valida que el botón se habilita/deshabilita correctamente; test del ViewModel con Turbine.

---

## Fase 7 ✅ — Pantalla de carga con checks (`:feature:planning`)

El motor real corre en <100 ms, así que la secuencia es deliberadamente escenificada — es el momento en que la app "se gana" la confianza del usuario.

Cuatro pasos, cada uno con spinner → check animado (~700–900 ms), texto que nombra los datos reales del usuario:
1. "Revisando el catálogo de **Jumbo**"
2. "Calzando platos con tus **$45.000**"
3. "Ordenando los almuerzos de **lunes a viernes**"
4. "Armando tu lista de compras"

Estado emitido desde el ViewModel como `Flow<EstadoPlanificacion>`; el trabajo real se lanza en paralelo y se espera a que ambos terminen. `progress` como live region para TalkBack. Al terminar, transición al resultado (`NavDisplay` reemplaza la clave, sin dejar la carga en el backstack).

Manejo de fallos: si el motor devuelve `PresupuestoInsuficiente` o `SinRecetasCompatibles`, la animación se detiene en el check correspondiente y aparece una hoja con el motivo y acciones ("Subir presupuesto a $X", "Cambiar restricción", "Generar igual").

---

## Fase 8 ✅ — Semana y detalle de receta (`:feature:week`)

**Pantalla de semana:**
- Header con resumen: total gastado vs. presupuesto (barra), supermercado, personas.
- Lista vertical L→D, una `RecipeCard` por día: nombre completo del día ("Miércoles"), nombre del plato, minutos, tags de clasificación, costo del día. Un solo almuerzo por día.
- Acciones: **Guardar semana** (primaria, thumb zone) y **Regenerar** (secundaria, nueva semilla).

**Detalle de receta** (al tocar un día) — pane de detalle Nav3:
- Nombre + descripción corta.
- Fila de metadatos: ⏱ tiempo total · 🏷 clasificación (ej. "Gut friendly", "Alto en proteína") · 👥 "Para 4 personas" · 💰 costo.
- **Ingredientes**: lista con cantidades **ya escaladas al número de personas**, agrupadas por pasillo.
- **Instrucciones**: pasos numerados.
- Macros por porción (kcal / proteína).

---

## Fase 9 ✅ — Guardar semana y Home (`:feature:home`)

- Al guardar: nombre por defecto editable ("Semana del 2 de septiembre"), confirmación con animación de éxito (**peak moment**: check que crece + copy que celebra) y vuelta a la home.
- **Home = pantalla de arranque de la app.**
  - Con semanas guardadas: lista de tarjetas (nombre, rango de días, supermercado, costo total, cantidad de platos) + FAB/botón "Nueva semana". Tocar una tarjeta reabre la vista de semana en modo lectura.
  - Sin semanas: `EmptyState` con ilustración, copy que explica el valor en una línea y CTA "Armar mi primera semana".
  - Swipe/long-press para borrar, con confirmación.
- `MainActivity` decide la clave inicial de Nav3 según si hay semanas guardadas.

---

## Fase 10 ⏳ — Pulido

- Micro-animaciones de estado (selección de opciones, checks, guardado) — `references/compose-patterns-quick.md`.
- Estados vacío / error / carga en cada pantalla.
- Accesibilidad: `contentDescription` en todos los iconos, targets ≥48dp, live regions en la pantalla de carga, contraste verificado en la paleta oscura (`references/android-accessibility-quick.md`).
- Splash screen (`androidx.core:core-splashscreen`) e ícono de app.
- `./gradlew detekt spotlessApply` limpio; R8 activado en release con las reglas del template.

---

---

## Estado al 4 de septiembre de 2026

Fases 0 a 9 terminadas y verificadas en emulador API 36: se recorre el flujo completo
-siete preguntas, armado con checks, semana, detalle, guardar, home- y la semana
sobrevive a matar el proceso.

Queda la **fase 10 (pulido)**, y dentro de ella lo que ya esta detectado:

- **Iconos repetidos.** Los cuatro artefactos comparten el icono de horno, y "bajo en
  calorias" y "alto en proteina" comparten el de balanza. Hay que dibujar los que
  faltan en `core/ui/src/main/res/drawable` con el prefijo `core_ui_`.
- **Imagenes de los platos.** La semana y el detalle son solo texto. Es lo que mas
  cambiaria la percepcion de la app, y lo que hay que decidir es de donde salen: 69
  fotos con licencia pesan y hay que conseguirlas.
- **Icono de la app y splash** propios.
- **Accesibilidad**: repasar contraste y `contentDescription` con TalkBack encendido.
- **R8** en release con las reglas del template.


## Verificación end-to-end

Tras cada fase con cambios de módulo/DI/schema (regla dura del skill):
```
./gradlew help
./gradlew :app:assembleDebug
```

Suite completa:
```
./gradlew test            # motor de planificación, ViewModels, validación del catálogo
./gradlew detekt          # calidad + reglas de Compose
./gradlew connectedCheck  # DAOs Room + tests de Compose UI (emulador)
```

Recorrido manual en emulador (Pixel API 37), que es la prueba real del MVP:
1. Abrir la app limpia → ver el empty state de la home.
2. "Armar mi primera semana" → completar los 7 pasos.
3. Ver los 4 checks de carga completarse.
4. Ver la semana L–V con 5 platos y el total bajo el presupuesto.
5. Tocar el miércoles → verificar tiempo, clasificación, "para 4 personas", ingredientes escalados ×4 e instrucciones.
6. Guardar semana → volver a la home y verla listada.
7. Cerrar y reabrir la app → la semana sigue ahí.
8. Repetir con **vegano + sólo airfryer + presupuesto $10.000** para ver el camino de error.

---

## Puntos abiertos a resolver sobre la marcha

- **Precios del catálogo:** se siembran con valores CLP de referencia (pollo ~$7.500/kg, arroz ~$1.500/kg, etc.). Al ver la app corriendo probablemente haya que corregirlos con precios reales — el archivo es un JSON plano, editarlo es trivial.
- **Supermercados:** parte con Líder, Jumbo, Santa Isabel, Tottus, Unimarc y aCuenta. Agregar otro es una línea en un enum.
- **Comida:** confirmado que la app se limita a **almuerzos** (un plato principal por día).

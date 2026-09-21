<div align="center">

<img src="docs/logo-cookify.png" width="104" alt="Cookify">

# Cookify

**Para quien cocina en casa con un presupuesto contado, Cookify arma una semana de
almuerzos que cabe en lo que puede gastar, con el precio del supermercado donde compra.**

Android · Kotlin · Jetpack Compose · Navigation3 · Room 3 · Hilt

</div>

---

## Qué hace

Se contestan siete preguntas —súper, personas, días, presupuesto, antojos, restricción y
con qué se cocina— y la app arma una semana de almuerzos que cabe en esa plata. Cada día
trae su plato con el tiempo, el costo, los ingredientes ya escalados a la cantidad de
personas y las instrucciones. La semana se guarda y queda en la portada.

Si el presupuesto no alcanza, la app no falla en silencio: dice cuánto falta y ofrece
armarla igual por el mínimo posible.

## Estado

| | |
|---|---|
| Recorrido completo | ✅ funcionando de punta a punta |
| Catálogo | 69 recetas chilenas · 98 ingredientes con precio |
| Persistencia | Room 3, las semanas sobreviven al cierre |
| Motor de planificación | determinista, probado con JUnit |
| Servicio de precios | 🔜 comprometido para el Hito 2 |

## Documentos

| Documento | Qué contiene |
|---|---|
| [Propuesta (PDF)](docs/propuesta-cookify.pdf) | El documento del Hito 1 |
| [Láminas (PDF)](docs/laminas-hito1.pdf) | Las de la presentación |
| [Mapa del producto](docs/mapa-del-producto.png) | Arquitectura de información |
| [Diagrama de arquitectura](docs/arquitectura.png) | Cliente, servidor y capas |
| [Arquitectura en detalle](docs/arquitectura.md) | El caso de uso recorrido capa por capa |
| [Evidencia del problema](docs/evidencia.md) | Cifras con fuente verificada |
| [Precios reales](docs/PRECIOS_REALES.md) | Por qué el precio necesita un servidor |
| [Plan del proyecto](PLAN.md) | Las fases, desde el andamiaje hasta hoy |

## Prototipo navegable

**Recorrido en el navegador:**
[netogu1llen.github.io/cookify/fuentes/prototipo.html](https://netogu1llen.github.io/cookify/fuentes/prototipo.html)

Catorce pantallas encadenadas de la app funcionando, con las zonas clickeables sobre los
botones reales. No son maquetas: son capturas de una sola corrida en el emulador.

**La app de verdad:** el APK de depuración está en
[Releases](https://github.com/netogu1llen/cookify/releases). Se instala en cualquier
Android 7 o superior.

Para compilarla desde el código:

```bash
git clone https://github.com/netogu1llen/cookify.git
cd cookify
./gradlew :app:assembleDebug        # el APK queda en app/build/outputs/apk/debug/
./gradlew test detekt               # pruebas y análisis estático
```

Requiere JDK 21 y el SDK de Android con la plataforma 37.

## Cómo está armado

Multi-módulo con Gradle, en capas. El dominio es Kotlin puro y no importa nada de
Android, por eso el motor de planificación se prueba en la JVM sin emulador.

```
:app                     navegación y arranque
:feature:onboarding      las siete preguntas
:feature:planning        el armado y sus verificaciones
:feature:week            la semana y el detalle del plato
:feature:home            las semanas guardadas
:core:model              modelos del dominio
:core:domain             motor de planificación y cálculo de costos
:core:data               catálogo, repositorios y precios
:core:database           Room 3
:core:ui                 tema oscuro y componentes
:core:common             utilidades compartidas
```

El detalle está en [docs/arquitectura.md](docs/arquitectura.md).

## Quién

| | GitHub |
|---|---|
| **Ernesto Guillén Guerrero** · 300050336 | [@netogu1llen](https://github.com/netogu1llen) |

INGT1003 · Arquitectura de Desarrollo (Móvil y Web) · Escuela de Ingeniería Civil ·
Universidad Mayor · Segundo semestre 2026 · Prof. Carlos Muñoz S.

## Uso de inteligencia artificial

Se declara conforme a la regla 7 del proyecto. Se usó Claude (Anthropic) como asistente
de programación y de redacción durante todo el desarrollo, con este alcance:

- **Código.** Escritura y revisión de los módulos de la app. Cada decisión de
  arquitectura —el corte en capas, la inversión de dependencias del dominio, la
  navegación con una sola Activity— está documentada en el código y en `docs/`, y es
  explicable línea por línea.
- **Verificación.** Todo lo que se afirma se comprobó: la app se recorrió en el emulador
  captura por captura, las pruebas y `detekt` corren en verde, y los bugs encontrados
  están en el historial de commits con su causa raíz.
- **Datos.** Las cifras del problema se verificaron abriendo la fuente original (INE,
  SERNAC, BCN); las notas de prensa sirvieron para encontrar los estudios, no para
  citarlos. La medición de dispersión de precios entre Jumbo y Santa Isabel es propia.
- **Documentos.** Redacción y diagramación de la propuesta, las láminas y los diagramas,
  a partir del contenido y las decisiones de este proyecto.

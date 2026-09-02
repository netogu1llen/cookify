# Catálogo de Cookify

Estos dos archivos son **la única fuente de datos de la app**. Se leen una vez al arrancar
y se cachean en memoria. Editarlos no requiere tocar código Kotlin.

| Archivo | Qué contiene | Con qué frecuencia cambia |
|---|---|---|
| `ingredientes.json` | Nombre, pasillo, unidad y **precio** de cada ingrediente | Seguido: los precios envejecen |
| `recetas.json` | Las recetas, sus ingredientes por porción y sus pasos | Rara vez |

---

## Cómo editar los precios

Es el cambio más común, así que va primero.

`precioClpPorUnidad` es **el precio en pesos de UNA unidad base**, en Líder, sin IVA aparte
(el precio de góndola tal cual). La unidad base la define el campo `unidad`:

| `unidad` | Significa | Cómo convertir desde el precio de góndola |
|---|---|---|
| `"g"` | 1 gramo | precio por kilo **÷ 1000** |
| `"ml"` | 1 mililitro | precio por litro **÷ 1000** |
| `"un"` | 1 unidad | el precio tal cual |

**Ejemplos reales:**

```
Pollo a $8.900 el kilo      → 8900 / 1000 = 8.9
Aceite a $2.500 el litro    → 2500 / 1000 = 2.5
Huevo a $250 cada uno       → 250
Palta a $900 cada una       → 900
```

El error clásico es escribir `8900` en un ingrediente en gramos. Eso hace que un almuerzo
cueste tres millones de pesos. El test de validación lo detecta (ver abajo).

### Los otros supermercados

**No hay que editar precios por supermercado.** El catálogo es siempre la base de Líder y
cada cadena aplica un multiplicador definido en
`core/model/src/main/kotlin/com/app/cookify/core/model/Supermercado.kt`:

```kotlin
LIDER(1.00)  ACUENTA(0.92)  TOTTUS(0.98)
UNIMARC(1.05)  SANTA_ISABEL(1.08)  JUMBO(1.15)
```

Si querés afinar qué tan caro es Jumbo respecto de Líder, se cambia ahí, en un solo lugar.

### Después de editar

Subí `version` y `actualizadoEn` en la cabecera del JSON. La app muestra esa fecha, así el
usuario sabe qué tan viejos son los números.

---

## Cómo agregar un ingrediente

```json
{ "id": "palta", "nombre": "Palta", "pasillo": "VERDULERIA", "unidad": "un", "precioClpPorUnidad": 900 }
```

- `id`: minúsculas con guión bajo, único. Es lo que referencian las recetas.
- `pasillo`: uno de `VERDULERIA`, `CARNICERIA`, `PESCADERIA`, `LACTEOS`, `ABARROTES`,
  `PANADERIA`, `CONGELADOS`. Define cómo se agrupa la lista de compras.

---

## Cómo agregar una receta

```json
{
  "id": "pollo_al_curry",
  "nombre": "Pollo al curry",
  "descripcion": "Cremoso y suave, se hace en una sola olla.",
  "minutosTotal": 35,
  "porcionesBase": 4,
  "basePrincipal": "POLLO",
  "antojos": ["PARA_TODOS", "TAKEAWAY"],
  "aptaPara": [],
  "requisitosArtefactos": [["ESTUFA"]],
  "ingredientes": [
    { "ingredienteId": "pechuga_pollo", "cantidadPorPorcion": 150 },
    { "ingredienteId": "cilantro", "cantidadPorPorcion": 0.25, "opcional": true }
  ],
  "pasos": ["Corta el pollo en cubos.", "..."],
  "kcalPorPorcion": 520,
  "proteinaGPorPorcion": 38
}
```

Puntos donde es fácil equivocarse:

- **`cantidadPorPorcion` es POR PERSONA**, en la unidad base del ingrediente. 150 g de pollo
  por persona; para 4 comensales la app multiplica sola. No pongas la cantidad total.
- **`aptaPara`** lista las restricciones que la receta respeta: `VEGETARIANO`, `VEGANO`,
  `SOLO_PESCADO`. Una receta con carne lleva `[]`. `NINGUNA` no hace falta ponerla nunca:
  sin restricción entran todas. Dos reglas que hay que respetar a mano:
  - **Vegano implica vegetariano**: una receta vegana lista los dos.
  - **Quien elige "sólo pescado" también come vegetariano y vegano.** Por eso toda receta
    vegetariana o vegana lleva además `SOLO_PESCADO`. Si no, esa persona tendría apenas
    ocho platos de pescado para armar siete días y el motor no podría variar nada.

  En la práctica: pescado → `["SOLO_PESCADO"]`, vegetariana →
  `["VEGETARIANO", "SOLO_PESCADO"]`, vegana → `["VEGETARIANO", "VEGANO", "SOLO_PESCADO"]`.
- **`requisitosArtefactos`** es una lista de grupos "alguno de estos", todos obligatorios:
  - `[["ESTUFA"]]` → necesita estufa.
  - `[["HORNO", "AIRFRYER"]]` → necesita horno **o** airfryer, cualquiera sirve.
  - `[["ESTUFA"], ["HORNO", "AIRFRYER"]]` → necesita estufa **y además** horno o airfryer.
  - `[]` → no necesita nada (una ensalada).
- **`antojos`**: `RAPIDO`, `BAJO_CALORIAS`, `PARA_TODOS`, `SANO`, `TAKEAWAY`,
  `GUT_FRIENDLY`, `ALTO_PROTEINA`. Poné solo los que la receta cumple de verdad; el motor
  premia además las métricas duras (≤25 min, ≤500 kcal, ≥30 g de proteína), así que mentir
  en los tags no ayuda y empeora las recomendaciones.
- **`opcional: true`** deja el ingrediente fuera del cálculo de presupuesto pero lo muestra
  en la receta. Úsalo para el cilantro de encima, no para la proteína.

---

## Validar los cambios

Hay un test que revisa el catálogo entero y falla con un mensaje claro:

```bash
./gradlew :core:data:test
```

Chequea que no haya ids duplicados, que toda receta referencie ingredientes que existen,
que los precios y cantidades sean positivos, que las recetas veganas también estén marcadas
como vegetarianas, y que ningún precio sea absurdo (el caso "escribí el precio por kilo en
un ingrediente que va en gramos").

Córrelo siempre después de editar. Un JSON mal formado hace que la app no arranque.

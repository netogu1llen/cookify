# Fase 11 — Precios reales desde el supermercado

Estado: **diseño validado con datos en vivo, implementación pendiente.**

Hoy los precios salen de `ingredientes.json` (98 valores escritos a mano, base Líder)
multiplicados por una constante por cadena (`Supermercado.multiplicador`: Jumbo 1,15,
Santa Isabel 1,08, …). El objetivo es reemplazar esa constante por el precio que el
supermercado cobra de verdad.

---

## Por qué el multiplicador no sirve

Comparando los 17 productos idénticos que aparecen en Jumbo y en Santa Isabel el mismo
día, la diferencia entre cadenas va de **−9% a +58%** según el producto:

| Producto | Jumbo | Santa Isabel | Δ |
|---|---|---|---|
| Arroz Taj Mahal basmati 1 kg | $5.890 | $5.890 | 0,0% |
| Arroz Miraflores largo y ancho 1 kg | $2.650 | $2.690 | +1,5% |
| Arroz Banquete grado 2 1 kg | $2.300 | $2.090 | −9,1% |
| Arroz Miraflores basmati 400 g | $2.430 | $2.990 | +23,0% |
| Arroz Tucapel gran selección 1 kg | $1.790 | $2.540 | +41,9% |
| Arroz Tucapel Blue 1 kg | $1.150 | $1.810 | +57,4% |

No hay un factor por tienda: la diferencia depende del producto. Un número fijo por
cadena inventa precios.

> Varios de los saltos grandes son **ofertas**, no precios de lista. Por eso se lee
> `ListPrice` y no `Price`: una promo horneada en el catálogo hace que el presupuesto se
> vea optimista y que cambie solo entre versiones.

---

## De dónde salen los datos

No hay API pública. Se probaron y descartaron: `catalog_system` de VTEX (404 contra el
Next.js nuevo de Jumbo), `intelligent-search` (404), y los endpoints de las otras
cadenas. Lo que sí funciona es la página renderizada en el servidor.

| Cadena | Vía | Estado |
|---|---|---|
| **Jumbo** | páginas de categoría; productos en `data-gtm-impression` | ✅ 58–116 productos/página, 302 KB, 2,8 s |
| **Santa Isabel** | páginas de categoría; JSON de VTEX embebido (`productName` + `ListPrice`) | ✅ 40 productos/página, 41 KB, 1,1 s |
| aCuenta | HTML llega, productos se renderizan en el cliente | ⚠️ fuera de alcance |
| Unimarc / Lider / Tottus | 403 (WAF) / 302 / 526 | ❌ fuera de alcance |

Las cuatro cadenas restantes siguen con el multiplicador actual, y hay que marcarlas en
la UI como precio **estimado** para no dar una precisión que no existe.

Detalle por cadena: la búsqueda de Jumbo **no** renderiza productos en el servidor (sólo
el menú), y las categorías sí. En Santa Isabel funcionan las dos. Se usa categoría en
ambas por la razón de abajo.

---

## Categorías, no búsqueda

Esta es la decisión que sostiene todo lo demás.

Buscar el nombre del ingrediente y promediar da precios absurdos, porque la búsqueda
devuelve cualquier producto que mencione la palabra. Medido:

- `leche` → mediana **$8,74/ml** contra $1,06 real (entró leche condensada y en polvo).
- `cebolla` → **$17,6/g** contra $1,69 real.
- `lentejas` → devolvió **porotos**.

Filtrar por palabras tampoco alcanza. Con lista negra, "cebolla" seguía trayendo
mermelada de cebolla, pretzels, crutones, chips, condimento McCormick y cebolla crispy.
La lista de exclusiones necesaria es abierta, y 98 ingredientes × una lista abierta es
una fuente de errores silenciosos: una receta con 500 ml de leche pasaría de $650 a
$4.370 sin que nada falle visiblemente.

Desde la categoría el problema desaparece por construcción — `frutas-y-verduras/verduras`
sólo contiene verduras:

```
                        Santa Isabel    Jumbo
Cebolla Malla 1 kg         $1.790       $1.870
Cebolla Morada 1 kg        $1.990          —
Cebolla Granel             $1.690          —
```

El árbol de categorías es específico y mapea casi uno a uno contra los ingredientes:
`despensa/legumbres/lentejas`, `despensa/arroz-quinoa-cuscus/arroz`,
`despensa/aceites-sal-y-condimentos/aceite`. Jumbo publica 157 categorías de comida; se
estiman 35–45 necesarias para cubrir los 98 ingredientes.

---

## Normalización a la unidad base

El catálogo guarda `precioClpPorUnidad` por gramo, ml o unidad. El supermercado vende
envases. El tamaño se saca del nombre del producto, y eso **sí funciona**: 26/26, 40/40
y 34/40 nombres legibles en las muestras.

- `1 kg`, `1.5 kg` → ×1000 g
- `900 ml`, `155 cc` → ml
- `30 un.`, `docena` → unidades (para huevos, paltas, limones)
- **Granel sin peso** (`Cebolla Granel (1 a 2 un. Aprox)`) → se descarta, no hay divisor

Dentro de la categoría ya filtrada se toma el **percentil 25** de los candidatos, no la
mediana: el planificador asume que el usuario compra la marca barata, no la del medio.
Con pocos candidatos cae al mínimo, que es el comportamiento deseado.

Calidad con este método sobre precios escritos a mano (Santa Isabel): leche +4%,
aceite −18%, lentejas +19%, arroz +34%, huevo +36%.

---

## Dónde corre

**En la app**, decisión del usuario. Pero medido: un refresco completo son ~35–45
peticiones, 6–9 MB y 10–20 s. No cabe dentro de la pantalla de armado, que dura 3,5 s.

Entonces: sincronización en segundo plano con caché y vencimiento, no consulta por
semana.

```
al abrir la app / caché vencida (7 días)
   └─> sincroniza en segundo plano  ──> Room: precios_cache
                                          (supermercado, ingredienteId,
                                           precioClpPorUnidad, actualizadoEn)
armar semana
   └─> precio = caché fresca ?: caché vencida ?: ingredientes.json
```

El respaldo a `ingredientes.json` no es negociable: sin él no se puede armar una semana
sin señal, y los 98 valores a mano ya están ahí. Además siguen siendo editables por el
desarrollador, que era el requisito original.

---

## Qué falta construir

1. `:core:data` — cliente Ktor (`ktor-client-android` 3.0.3, ya está en el catálogo de
   versiones).
2. Dos parsers: `data-gtm-impression` (Jumbo) y JSON de VTEX embebido (Santa Isabel).
3. `TamanoEnvase` — el parser de formato del nombre, con sus tests.
4. `fuentes_precios.json` — el mapeo de los 98 ingredientes a categoría + términos de
   nombre. **Es el trabajo real y hay que verificarlo ingrediente por ingrediente**;
   un mapeo malo no falla, sólo cobra mal.
5. `:core:database` — tabla `precios_cache` y su DAO.
6. `PrecioRepository` con la cadena de respaldo de arriba.
7. Sincronización en segundo plano y estado "precios actualizados hace X" en la UI.
8. Marcar las 4 cadenas sin datos reales como precio estimado.

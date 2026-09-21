# Evidencia del problema

Base de datos verificados que sostiene la propuesta. Cada cifra está citada en norma APA
séptima edición al final, y todas se verificaron en la fuente primaria el 20 de
septiembre de 2026.

La regla que se siguió: ninguna cifra entra si no se pudo abrir la fuente original. Las
notas de prensa sirvieron para encontrar los estudios, no para citarlos.

---

## 1. La comida es el mayor gasto del hogar chileno, y está subiendo

La IX Encuesta de Presupuestos Familiares del INE, levantada entre octubre de 2021 y
septiembre de 2022 sobre **15.134 hogares de 79 comunas en las 16 regiones**, midió:

| | Monto mensual |
|---|---|
| Gasto promedio del hogar | **$1.451.782** |
| Gasto mediano del hogar | $1.108.065 |
| **Alimentos y bebidas no alcohólicas** | **$307.947 · 21,2% del gasto total** |

Alimentos es **la primera categoría de gasto** del hogar chileno y creció **2,5 puntos
porcentuales** respecto de la EPF anterior.

> Sobre un gasto de $307.947 al mes en comida, equivocarse en un 20% son **$61.600
> mensuales**, o $739.000 al año.

## 2. La misma canasta cuesta hasta 238% más según dónde se compre

El SERNAC publicó el **7 de septiembre de 2026** su Cotizador de Fiestas Patrias,
construido sobre **250.000 precios de 1.300 sucursales de 47 locales** (40 supermercados
y 7 carnicerías) a lo largo del país:

| Canasta de 6 productos idénticos | Precio |
|---|---|
| Más barata | **$23.139** |
| Promedio | $45.290 |
| Más cara | **$78.197** |
| **Diferencia** | **$55.058 · 237,8%** |

El costillar de cerdo de 1,2 kg por sí solo varía **$14.004**: de $6.588 a $20.592.

Es el dato más fuerte que tenemos y es de este mes. Confirma que el supermercado elegido
no es un detalle: es la diferencia entre que el presupuesto alcance o no.

## 3. Lo que más se bota es comida que alguien cocinó

Chile genera **1,62 millones de toneladas** de residuos de alimentos al año (BCN, 2021).
El estudio *Cuánto alimento desechan los chilenos* de la Universidad de Talca determinó
en 2017 que **el 95% de las personas considera normal botar la comida acumulada en el
refrigerador**.

La categoría que más pesa en el desperdicio doméstico no son las sobras del plato sino
**la comida que se compró y nunca se cocinó, o se cocinó de más**. Es el desperdicio que
ataca planificar: comprar exactamente para los días y las personas que se van a cocinar.

## 4. Medición propia: el multiplicador por cadena no existe

*Dato levantado por el equipo el 20 de septiembre de 2026.*

Se compararon los **17 productos idénticos** presentes el mismo día en el catálogo en
línea de Jumbo y de Santa Isabel:

| Producto | Jumbo | Santa Isabel | Δ |
|---|---|---|---|
| Arroz Taj Mahal basmati 1 kg | $5.890 | $5.890 | 0,0% |
| Arroz Miraflores largo y ancho 1 kg | $2.650 | $2.690 | +1,5% |
| Arroz Banquete grado 2 1 kg | $2.300 | $2.090 | **−9,1%** |
| Arroz Miraflores basmati 400 g | $2.430 | $2.990 | +23,0% |
| Arroz Tucapel gran selección 1 kg | $1.790 | $2.540 | +41,9% |
| Arroz Tucapel Blue 1 kg | $1.150 | $1.810 | **+57,4%** |

La diferencia entre cadenas va de **−9,1% a +57,4% y depende del producto, no de la
tienda**. No existe un factor por supermercado.

Esta medición es la que justifica la decisión de arquitectura del Hito 2: si el precio
no se puede derivar de una constante, hay que ir a buscarlo, y eso necesita un servicio
que lo haga una vez para todos los clientes.

---

## Qué concluye la evidencia

1. El problema tiene tamaño: **$307.947 al mes** por hogar, la primera categoría de gasto.
2. El problema tiene dispersión: **hasta 238%** de diferencia por la misma canasta.
3. El problema tiene desperdicio asociado, y es el que la planificación ataca.
4. La solución no puede estimar precios con una constante: hay que medirlos.

---

## Referencias

Biblioteca del Congreso Nacional de Chile. (2021). *Proyectos de ley que regulen el
desperdicio alimentario en Chile: antecedentes y actualidad* (Minuta N.º 32-21).
Departamento de Estudios, Extensión y Publicaciones.
https://obtienearchivo.bcn.cl/obtienearchivo?id=repositorio/10221/32197/1/Minuta_32_21_Desperdicio_Alimentos_Chile.pdf

Instituto Nacional de Estadísticas. (2023). *IX Encuesta de Presupuestos Familiares:
síntesis de resultados (octubre 2021 – septiembre 2022)*. INE. https://www.ine.gob.cl/epf

Servicio Nacional del Consumidor. (2026, 7 de septiembre). *Cotizador de Fiestas
Patrias: casi $55 mil de diferencia en la "canasta dieciochera" detectó el SERNAC*.
SERNAC. https://www.sernac.cl/portal/604/w3-article-89167.html

# Cookify · Guion de 5 minutos

Ocho láminas, 593 palabras habladas. Lo que está en cursiva es lo que se dice; el resto
son indicaciones. Los tiempos salen de contar las palabras a 135 por minuto, más dos
segundos de transición entre láminas.

**La marca que importa:** si a los 1:30 todavía no se ve el teléfono en pantalla, hay
que apurar. Si a los 3:30 no se llegó a la lámina 7, saltar directo a ella.

| # | Lámina | Entra | Dura | Palabras |
|---|---|---|---|---|
| 1 | Apertura | 0:00 | 20 s | 40 |
| 2 | Evidencia | 0:20 | 40 s | 83 |
| 3 | La promesa | 1:00 | 30 s | 62 |
| 4 | El recorrido | 1:30 | 40 s | 86 |
| 5 | Mapa del producto | 2:10 | 35 s | 79 |
| 6 | Arquitectura | 2:45 | 45 s | 96 |
| 7 | La decisión | 3:30 | 50 s | 106 |
| 8 | Cierre | 4:20 | 20 s | 41 |
| | **Termina** | **4:40** | | **593** |

Margen real según el ritmo: **4:56 hablando pausado** y 3:57 apurado. Aun en el peor caso
se cierra antes del corte, y en el mejor sobra tiempo para recorrer el prototipo en vivo.

---

## 1 · Apertura — 20 s · entra 0:00

> *Todos los domingos alguien abre el refrigerador y hace la misma cuenta. Qué cocino
> esta semana. Y si me alcanza.*
>
> *Esa cuenta se hace de memoria, se hace mal, y la diferencia se paga después, en la
> caja del supermercado.*

No decir todavía qué es Cookify. Menos aún con qué está hecho.

## 2 · Evidencia — 40 s · entra 0:20

> *El problema tiene tamaño. El hogar chileno gasta trescientos siete mil pesos al mes
> en comida. Es su primer gasto, por delante de vivienda y de transporte.*
>
> *Y tiene dispersión. Hace dos semanas el SERNAC comparó la misma canasta entre
> cuarenta supermercados: la más barata costaba veintitrés mil pesos; la más cara,
> setenta y ocho mil. Doscientos treinta y siete por ciento de diferencia por
> exactamente los mismos productos.*
>
> *Dónde compras no es un detalle. Es la diferencia entre que alcance o no.*

Decir los números despacio. Son el ancla de todo lo que viene.

## 3 · La promesa — 30 s · entra 1:00

> *Cookify hace esto: para quien cocina en casa con el presupuesto contado, arma una
> semana de almuerzos que cabe en lo que puede gastar, con el precio del supermercado
> donde compra.*
>
> *Y es verificable. Si dice que tiene cincuenta mil, recibe una semana de cincuenta mil
> o menos. O la app le dice exactamente cuánto le falta. No hay un tercer resultado.*

**Esta es la lámina que decide quince puntos.** Al terminarla, el evaluador tiene que
poder escribir en una línea qué se vende, a quién y qué problema resuelve. Si algo se
sacrifica por tiempo, no es esto.

## 4 · El recorrido — 40 s · entra 1:30

> *Siete preguntas, una por pantalla. En qué súper compras, y es la primera justamente
> por lo que acabo de mostrar. Para cuántas personas. Qué días. Cuánto quieres gastar.
> Qué se te antoja. Alguna restricción. Y con qué cocinas, para no proponer recetas que
> no se pueden hacer.*
>
> *Con eso arma la semana: un plato por día, con su costo, y el total contra el
> presupuesto.*
>
> *Y al tocar un día, la receta completa. Los ingredientes ya escalados a cuatro
> personas y ordenados por pasillo del supermercado.*

Si la sala lo permite, recorrer el prototipo en vivo en lugar de las capturas.
**Tenerlo abierto en una pestaña y descargado en el computador**: la conexión falla
justo cuando importa.

## 5 · Mapa del producto — 35 s · entra 2:10

> *Esta es la arquitectura de información. Desde la portada salen dos caminos: armar una
> semana nueva, o abrir una guardada. Todo lo demás son pasos dentro de esos dos.*
>
> *Fíjense en la flecha de vuelta: guardar devuelve a la portada. Es el único ciclo, y es
> el que hace que la próxima vez lo primero que vea sea lo que armó.*
>
> *Y cada caja lleva el nombre que el usuario lee en el teléfono, no el de la clase.*

**Recorrer el mapa con el dedo mientras se habla.** La rúbrica pide usarlo para explicar
el recorrido, no solo mostrarlo: son 8 puntos.

## 6 · Arquitectura — 45 s · entra 2:45

> *Cliente y servidor, en capas.*
>
> *El cliente tiene cuatro. Presentación dibuja. Dominio decide qué se come y cuánto
> cuesta. Datos resuelve de dónde sale cada dato. Persistencia guarda.*
>
> *Dos decisiones importan. La primera: el dominio no importa nada de Android, así que el
> motor se prueba en la máquina virtual de Java, sin emulador.*
>
> *La segunda: el dominio define los repositorios como interfaz. Por eso el Hito 2 puede
> cambiar de dónde viene el precio sin tocar el motor.*
>
> *En ámbar está lo que ya existe. En azul punteado, el servidor, que se entrega en
> noviembre.*

Decir lo del azul punteado sin rodeos. Reconocer lo que falta es lo que hace creíble lo
que sí está.

## 7 · La decisión — 50 s · entra 3:30

> *Y por qué hay un servidor.*
>
> *El modelo empezó estimando: un precio base por una constante por cadena.*
>
> *Se comprobó con los diecisiete productos idénticos que estaban el mismo día en las dos
> cadenas. La diferencia va de menos nueve por ciento a más cincuenta y siete. Depende
> del producto, no de la tienda. La constante no existe.*
>
> *Entonces hay que medirlo. Y hacerlo desde cada teléfono son hasta nueve megas y veinte
> segundos. Por eso va en un servidor: lo baja una vez y lo sirve a todos.*
>
> *La arquitectura no se eligió para que el diagrama se viera bien. Se eligió después de
> medir.*

La lámina más fuerte. Es evidencia propia y ningún otro equipo la va a tener.

## 8 · Cierre — 20 s · entra 4:20

> *Lo que está construido, lo que viene en el Hito 2 y lo que viene en el Hito 3.*
>
> *El compromiso es simple: lo que prometo acá es lo que entrego en diciembre. El
> repositorio y el prototipo quedan en pantalla.*

Dejar la lámina 8 proyectada durante las preguntas.

---

## Si vas atrasado

Cortar en este orden, nunca antes:

1. **Lámina 6**, la segunda decisión (la de los repositorios como interfaz). Se entiende
   igual con la primera.
2. **Lámina 5**, la frase de los nombres de usuario. El mapa se explica solo.
3. **Lámina 2**, el tercer dato (el 95 %). Los dos primeros bastan.

**Nunca cortar la lámina 3.** Si el evaluador no puede escribir la promesa en una línea,
el bloque completo de presentación queda en cero.

## Si vas adelantado

Sobra tiempo en la lámina 4: recorrer una pantalla más del prototipo, o abrir el detalle
de un plato y mostrar los ingredientes agrupados por pasillo.

---

## La pregunta del sorteo

Vale 10 puntos y define el factor individual.

**«¿Por qué eligieron esta arquitectura?»**
No responder con nombres de tecnologías. El dominio está aislado de Android para poder
probarlo sin emulador, y los repositorios son interfaces para poder cambiar el origen del
precio sin tocar el motor. Rematar con la medición: es lo que obliga a tener servidor.

**«¿Por qué no algo multiplataforma?»**
Porque el producto tiene que funcionar sin red y guardar datos localmente, y ahí Room es
de primera clase. La desventaja está asumida por escrito en el documento: **no va a haber
iOS ni web sin reescribir el cliente.**

**«¿Qué es lo que todavía no funciona?»**
De frente: el servidor de precios no existe, y la pantalla de armado anuncia una lista de
compras que todavía no se entrega. Las dos están declaradas en el capítulo 6. Reconocer
una deuda declarada es más fuerte que esquivarla.

**«¿Usaste inteligencia artificial?»**
Sí, está declarado en el README y en el anexo D. Se usó como asistente de programación.
Las decisiones de arquitectura y la verificación de cada cifra son propias, y están
documentadas en el repositorio commit por commit.

---

## Antes de entrar

- [ ] Láminas en PDF, abiertas y en pantalla completa
- [ ] Prototipo en una pestaña **y** descargado en el computador
- [ ] Repositorio abierto en otra pestaña
- [ ] Tres ensayos completos con cronómetro
- [ ] Avisarle al profesor por escrito que el equipo quedó en una persona

**Enlaces**
Repositorio · github.com/netogu1llen/cookify
Prototipo · netogu1llen.github.io/cookify/fuentes/prototipo.html

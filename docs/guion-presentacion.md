# Guion de la presentación · Hito 1

Cinco minutos, ocho láminas. Este archivo no se proyecta: es para ensayar.

**La regla de las láminas:** lo que está escrito no se dice, y lo que se dice no está
escrito. Leer la lámina es lo que castiga el criterio A4.

**Lo que tiene que pasar antes del minuto 1:30** — que el evaluador pueda escribir en una
línea qué se vende, a quién y qué problema resuelve. Eso son las láminas 1 a 3, y valen 15
de los 50 puntos de la presentación. Si algo se sacrifica por tiempo, no es esto.

---

## 1 · Apertura · ~35 s

> «Todos los domingos alguien hace la misma cuenta mal.»

Abrir por la persona, no por la tecnología. Contar la escena: alguien decide qué va a
cocinar la semana y calcula de memoria si le alcanza. Se equivoca, y la diferencia la paga
en la caja del supermercado.

**No decir todavía** qué es Cookify ni con qué está hecho.

## 2 · Evidencia · ~40 s

Tres cifras, una por una. No leerlas: decir qué significan.

- **$307.947** — la comida es el primer gasto del hogar chileno, por delante de vivienda y
  transporte. Y subió 2,5 puntos respecto de la medición anterior.
- **237,8 %** — el dato más fuerte, y es de hace dos semanas. La misma canasta de seis
  productos cuesta $23.139 o $78.197 según dónde se compre.
- **95 %** — la gente considera normal botar comida del refrigerador.

**La frase que amarra:** «el supermercado donde compras no es un detalle, es la diferencia
entre que alcance o no».

## 3 · La promesa · ~30 s

Decirla completa y despacio. Es la lámina que el evaluador tiene que poder copiar.

Rematar con lo verificable: **si dice $50.000, recibe una semana de $50.000 o menos, o le
decimos exactamente cuánto le falta. No hay un tercer resultado.**

## 4 · El producto · ~50 s

> «Esto no es una maqueta.»

Es el momento de mayor ventaja: casi ningún equipo llega al Hito 1 con algo que funciona.
Recorrer el prototipo navegable **en vivo** si la sala lo permite; si no, las capturas de
la lámina.

Mostrar el camino: las siete preguntas → el armado → la semana con su costo → el detalle de
un plato con los ingredientes escalados.

**Respaldo:** tener el prototipo abierto en una pestaña y descargado en el computador. La
conexión falla justo cuando importa.

## 5 · Mapa del producto · ~35 s

Aquí se juegan los 8 puntos del criterio A3, y la rúbrica pide **usar el mapa para explicar
el recorrido**, no solo mostrarlo. Así que recorrerlo con el dedo:

«Desde la portada salen dos caminos. Este de acá arma una semana nueva; este otro abre una
guardada. Todo lo demás son pasos dentro de esos dos. Y fíjense en esta flecha de vuelta:
guardar devuelve a la portada, y es el único ciclo del producto.»

Mencionar la regla de nombres: cada caja dice lo que el usuario lee en el teléfono, no el
nombre de la clase.

## 6 · Arquitectura · ~50 s

Recorrer el diagrama de izquierda a derecha.

- Cliente en cuatro capas. **El dominio no importa nada de Android** — por eso el motor se
  prueba en la JVM, sin emulador.
- El dominio define los repositorios como **interfaz**. Esa inversión es la que permite que
  el Hito 2 cambie de dónde sale el precio **sin tocar el motor**.
- En ámbar lo construido, en azul punteado lo comprometido. **Decirlo explícitamente:** el
  servidor todavía no existe, y se entrega en noviembre.

## 7 · La decisión · ~45 s

La lámina más fuerte técnicamente. Contar la historia en orden:

1. El modelo empezó estimando: un precio base por un multiplicador por cadena.
2. Fuimos a comprobarlo con los 17 productos idénticos que estaban el mismo día en Jumbo y
   Santa Isabel.
3. La diferencia va de −9,1 % a +57,4 % **según el producto, no según la cadena**. El
   multiplicador no existe.
4. Entonces el precio hay que medirlo. Y medirlo desde cada teléfono son 6 a 9 MB y hasta
   20 segundos, así que va en un servidor.

**El remate:** «la arquitectura no se eligió para que el diagrama se viera bien. Se eligió
después de medir.»

## 8 · Cierre · ~25 s

Las tres columnas de un vistazo: lo construido, el Hito 2 y el Hito 3.

Cerrar con el compromiso: **lo prometido acá es lo que se entrega en diciembre**, y dejar
en pantalla el enlace del repositorio mientras vienen las preguntas.

---

## Para la pregunta sorteada

Vale 10 puntos y define el factor individual. Las tres más probables:

**«¿Por qué eligieron esta arquitectura?»**
No responder con nombres de tecnologías. Responder: el dominio está aislado de Android para
poder probarlo sin emulador, y los repositorios son interfaces para poder cambiar el origen
del precio sin tocar el motor. Rematar con la medición de precios, que es lo que obliga a
tener servidor.

**«¿Por qué no usaron algo multiplataforma?»**
Porque el producto tiene que funcionar sin red y guardar datos localmente, y ahí Room es de
primera clase. La desventaja se asume por escrito: **no va a haber iOS ni web sin reescribir
el cliente.**

**«¿Qué es lo que todavía no funciona?»**
Contestar de frente: el servidor de precios no existe todavía, y la pantalla de armado
anuncia una lista de compras que aún no se entrega. Las dos cosas están declaradas en el
documento, en el capítulo 6. Reconocer una deuda declarada es más fuerte que esquivarla.

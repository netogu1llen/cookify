# -*- coding: utf-8 -*-
"""Arma el prototipo navegable embebiendo las capturas reales de la app.

Las imagenes van como data URI y no como archivos sueltos: el entregable es un
enlace que el evaluador abre una vez, y una pagina que depende de catorce archivos
externos se rompe en cuanto uno se mueve de lugar.
"""
import base64
import io
import json
import os
import sys

from PIL import Image

CAPTURAS = "C:/Users/ernes/Desktop/cookify/docs/capturas"
SALIDA = "C:/Users/ernes/Desktop/cookify/docs/fuentes/prototipo.html"
ANCHO = 430  # ancho de render del telefono en la pagina, a 2x

# archivo, titulo, que esta pasando, zona clickeable (x%, y%, ancho%, alto%),
# y que dato queda sabido despues de este paso
PASOS = [
    ("01-portada-vacia", "Tus semanas", "Sin semanas guardadas la portada es una sola invitacion. No hay menus ni pestanas: la app hace una cosa.", (5, 60, 90, 6.5), None),
    ("02-supermercado", "\u00bfEn qu\u00e9 s\u00faper compras?", "Los precios cambian mucho entre cadenas, as\u00ed que es la primera pregunta y no un ajuste escondido.", (5, 89, 90, 7), ("S\u00faper", "Jumbo")),
    ("03-personas", "\u00bfPara cu\u00e1ntas personas?", "De esto dependen las cantidades de cada ingrediente y el costo total de la semana.", (5, 89, 90, 7), ("Personas", "4")),
    ("04-dias", "\u00bfQu\u00e9 d\u00edas vas a cocinar?", "Se eligen d\u00edas concretos, no una cantidad: el resultado sale ordenado de lunes a domingo.", (5, 89, 90, 7), ("D\u00edas", "De lunes a viernes")),
    ("05-presupuesto", "\u00bfCu\u00e1nto quieres gastar?", "El total de la semana para todas las personas. Las sugerencias evitan escribir.", (5, 89, 90, 7), ("Presupuesto", "$50.000")),
    ("06-antojos", "\u00bfQu\u00e9 se te antoja?", "Hasta tres. Al llegar al tercero los dem\u00e1s se aten\u00faan: se previene el error en vez de avisarlo.", (5, 89, 90, 7), ("Antojos", "Hasta 3")),
    ("07-restriccion", "\u00bfAlguna restricci\u00f3n?", "Filtra el recetario completo antes de que el motor empiece a elegir.", (5, 89, 90, 7), ("Restricci\u00f3n", "Ninguna")),
    ("08-artefactos", "\u00bfCon qu\u00e9 cocinas?", "Solo se proponen recetas que se puedan hacer de verdad con lo que hay en la cocina.", (5, 89, 90, 7), ("Cocina", "Estufa, horno y horno el\u00e9ctrico")),
    ("09-armando", "Armando tu semana", "Cuatro verificaciones que nombran los datos reales de quien la est\u00e1 usando. Si el presupuesto no alcanza, la secuencia se detiene en la etapa que fall\u00f3 y ofrece una salida.", (5, 20, 90, 70), None),
    ("10-semana", "Tu semana", "$40.800 de $50.000. Un almuerzo por d\u00eda con su costo, su tiempo y su clasificaci\u00f3n. Toca un d\u00eda para ver el plato.", (5, 37, 90, 13), ("Resultado", "$40.800 de $50.000")),
    ("11-detalle", "Detalle del plato", "Ingredientes ya escalados a cuatro personas y agrupados por pasillo del s\u00faper, en el orden en que se recorre la tienda.", (2, 4, 16, 5), None),
    ("12-guardar", "Guardar semana", "Viene con un nombre por defecto ya escrito: guardar tiene que poder ser dos toques.", (15, 52, 70, 7), None),
    ("13-guardada", "Semana guardada", "El cierre del recorrido completo. Es el \u00fanico lugar de la app donde vale la pena gastar un segundo en celebrar.", (5, 20, 90, 60), None),
    ("14-portada", "Tus semanas", "La semana queda en la portada y sigue ah\u00ed al cerrar y volver a abrir la app.", (5, 89, 90, 7), None),
]


def img_data_uri(ruta):
    im = Image.open(ruta).convert("RGB")
    im = im.resize((ANCHO * 2, int(im.height * ANCHO * 2 / im.width)), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "WEBP", quality=78, method=6)
    return "data:image/webp;base64," + base64.b64encode(buf.getvalue()).decode()


def main():
    pasos = []
    total = 0
    for archivo, titulo, nota, zona, dato in PASOS:
        uri = img_data_uri(os.path.join(CAPTURAS, archivo + ".png"))
        total += len(uri)
        pasos.append({
            "img": uri, "titulo": titulo, "nota": nota,
            "zona": zona, "dato": dato,
        })
    print("pasos: %d   peso de las imagenes: %.1f MB" % (len(pasos), total / 1e6))

    logo = Image.open("C:/Users/ernes/Desktop/cookify/docs/logo-cookify.png").convert("RGBA")
    logo = logo.resize((160, 160), Image.LANCZOS)
    b = io.BytesIO(); logo.save(b, "PNG", optimize=True)
    logo_uri = "data:image/png;base64," + base64.b64encode(b.getvalue()).decode()

    plantilla = io.open(sys.argv[1], encoding="utf-8").read()
    html = (plantilla.replace("/*LOGO*/", logo_uri)
                     .replace("/*DATOS*/", json.dumps(pasos, ensure_ascii=False)))
    io.open(SALIDA, "w", encoding="utf-8", newline="\n").write(html)
    print("escrito:", SALIDA, "%.1f MB" % (len(html.encode("utf-8")) / 1e6))


if __name__ == "__main__":
    main()

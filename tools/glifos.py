# -*- coding: utf-8 -*-
"""Los doce glifos de categoria de plato, en un solo lugar.

Se generan con codigo y no a mano porque varias formas son elipses rotadas y bandas
diagonales: escribir esos numeros a ojo en el pathData es donde se cuelan los errores
que despues aparecen como un dibujo torcido en el telefono.

La misma fuente produce la vista previa en HTML y los VectorDrawable, asi que lo que
se revisa en el navegador es exactamente lo que termina en la app.
"""
import math

VIEWPORT = 24.0


def circulo(cx, cy, r):
    return "M%g,%g a%g,%g 0 1,0 0,%g a%g,%g 0 1,0 0,%g Z" % (cx, cy - r, r, r, 2 * r, r, r, -2 * r)


def elipse(cx, cy, rx, ry, rot=0.0):
    """Elipse con rotacion. Los extremos se toman sobre el eje menor ya rotado."""
    t = math.radians(rot)
    dx, dy = -ry * math.sin(t), ry * math.cos(t)
    x1, y1 = cx + dx, cy + dy
    return "M%.2f,%.2f a%g,%g %g 1,0 %.2f,%.2f a%g,%g %g 1,0 %.2f,%.2f Z" % (
        x1, y1, rx, ry, rot, -2 * dx, -2 * dy, rx, ry, rot, 2 * dx, 2 * dy)


def banda(x1, y1, x2, y2, grosor):
    """Barra recta entre dos puntos, como poligono cerrado."""
    dx, dy = x2 - x1, y2 - y1
    largo = math.hypot(dx, dy)
    nx, ny = -dy / largo * grosor / 2, dx / largo * grosor / 2
    return "M%.2f,%.2f L%.2f,%.2f L%.2f,%.2f L%.2f,%.2f Z" % (
        x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny)


def onda(y, grosor):
    """Cinta ondulada de lado a lado: una hebra de fideo."""
    g = grosor
    return ("M1,%g C5,%g 8,%g 12,%g C16,%g 19,%g 23,%g L23,%g "
            "C19,%g 16,%g 12,%g C8,%g 5,%g 1,%g Z") % (
        y, y - 3, y + 3, y, y - 3, y + 3, y, y + g,
        y + 3 + g, y - 3 + g, y + g, y + 3 + g, y - 3 + g, y + g)


# Cada entrada: (pathData, usa_evenOdd)
GLIFOS = {
    # Posta: la masa redonda y el hueso en diagonal con sus dos perillas.
    "pollo": (
        circulo(9, 9.5, 5.6)
        + banda(11.5, 12, 18.5, 18.5, 2.8)
        + circulo(19.4, 17.2, 1.9)
        + circulo(17.2, 19.4, 1.9),
        False,
    ),
    # Corte de vacuno: silueta irregular con vetas de grasa.
    # Antes el calado era un circulo y el resultado era indistinguible del huevo.
    "vacuno": (
        "M5.5,5.5 C9,2.8 16.5,3.2 19.3,7 C22,10.6 20.4,17.6 15.3,19.6 "
        "C10.3,21.5 4.3,18.6 3.4,13.3 C2.9,10.2 3.2,7.3 5.5,5.5 Z"
        + banda(6.8, 9.6, 13.2, 7.4, 1.15)
        + banda(6.2, 13.6, 15.4, 10.4, 1.15)
        + banda(8.4, 17.2, 16.6, 14.0, 1.15),
        True,
    ),
    # Hocico de chancho: mas reconocible a este tamano que una chuleta.
    "cerdo": (
        elipse(12, 12, 9, 6.2)
        + elipse(9, 12, 1.4, 2.3)
        + elipse(15, 12, 1.4, 2.3),
        True,
    ),
    # Concha de ostion: abanico con nervaduras caladas.
    # El camaron en C se leia como el icono de recargar, que es justo lo que no
    # puede pasar en una tarjeta donde tambien hay un boton de regenerar.
    "marisco": (
        "M2.6,17.4 C2.6,9.2 6.8,3.4 12,3.4 C17.2,3.4 21.4,9.2 21.4,17.4 "
        "C21.4,18.9 20.4,19.9 19,19.9 L5,19.9 C3.6,19.9 2.6,18.9 2.6,17.4 Z"
        + banda(12, 19.9, 5.2, 10.4, 1.15)
        + banda(12, 19.9, 9.2, 5.6, 1.15)
        + banda(12, 19.9, 14.8, 5.6, 1.15)
        + banda(12, 19.9, 18.8, 10.4, 1.15),
        True,
    ),
    # Huevo frito: clara despareja y yema descentrada.
    # Lo que lo separaba de una dona no era el anillo sino la simetria: con la yema
    # al centro de una clara redonda el dibujo es un blanco de tiro.
    "huevo": (
        "M9.6,2.6 C13.4,2.2 16.4,3.8 18.4,6.2 C20.4,8.6 19.6,11.4 20.6,13.6 "
        "C21.8,16.2 20.4,19.4 17.4,20.6 C14.2,21.9 11.2,20.4 8.4,20.8 "
        "C5.2,21.2 2.4,19.2 2.4,16 C2.4,13.4 4,11.8 3.8,9.4 "
        "C3.5,6 6,3 9.6,2.6 Z"
        + circulo(9.8, 10.2, 4.0)
        + circulo(9.8, 10.2, 2.9),
        True,
    ),
    # Legumbres: tres porotos inclinados en distinta direccion.
    "legumbre": (
        elipse(8, 8.2, 4.3, 2.7, -25)
        + elipse(15.6, 12.4, 4.3, 2.7, 20)
        + elipse(8.6, 16.6, 4.3, 2.7, -10),
        False,
    ),
    # Brocoli: copa de circulos encimados y tronco.
    "verdura": (
        circulo(8.2, 8.4, 3.6)
        + circulo(12, 6.6, 4.0)
        + circulo(15.8, 8.6, 3.5)
        + circulo(12, 10.4, 4.0)
        + "M10.4,12.2 L13.6,12.2 L14.4,21 L9.6,21 Z",
        False,
    ),
    # Fideos: tres hebras onduladas.
    "pasta": (onda(6.5, 2.0) + onda(12, 2.0) + onda(17.5, 2.0), False),
    # Arroz: el tazon con los granos encima.
    "arroz": (
        "M2.6,12 L21.4,12 C21.4,17.6 17.2,21.4 12,21.4 C6.8,21.4 2.6,17.6 2.6,12 Z"
        + elipse(7.8, 9.2, 2.1, 1.3, -20)
        + elipse(12, 7.2, 2.1, 1.3, 10)
        + elipse(16.2, 9.2, 2.1, 1.3, 25),
        False,
    ),
    # Cuna de queso con sus ojos.
    "queso": (
        "M2.5,19.2 L21.5,5.2 L21.5,19.2 Z"
        + circulo(14.2, 15.1, 1.55)
        + circulo(18.2, 11.4, 1.2)
        + circulo(10.2, 17.6, 1.05),
        True,
    ),
    # Tofu: cubo isometrico. Las tres aristas internas son calados, no trazos.
    "tofu": (
        "M12,2.6 L21,7.6 L21,16.4 L12,21.4 L3,16.4 L3,7.6 Z"
        + banda(12, 12, 12, 21.4, 1.1)
        + banda(12, 12, 3, 7.6, 1.1)
        + banda(12, 12, 21, 7.6, 1.1),
        True,
    ),
}

# El pescado ya existe como icono de la restriccion "Solo pescado"; se reusa ese
# mismo dibujo en vez de tener dos pescados distintos en la misma app.
PESCADO = (
    "M2.5,12C5.5,7.5 10.5,6.5 14,9C16,10.3 17.5,12 17.5,12C17.5,12 16,13.7 14,15"
    "C10.5,17.5 5.5,16.5 2.5,12ZM17.5,12L21.5,8.5L21.5,15.5ZM7.2,10.6"
    "a1,1 0 1,0 0,2a1,1 0 1,0 0,-2Z",
    True,
)

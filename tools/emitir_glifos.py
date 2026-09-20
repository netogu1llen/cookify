# -*- coding: utf-8 -*-
"""Escribe los VectorDrawable de las categorias de plato."""
import io, os
from glifos import GLIFOS

DEST = "C:/Users/ernes/Desktop/cookify/core/ui/src/main/res/drawable"

CABECERA = """<!--
    Generado: ver tools/glifos.py. Los numeros salen de ahi, no se editan a mano.
    Glifo de la categoria {nombre} para las tarjetas de plato.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"{tipo}
        android:pathData="{d}" />
</vector>
"""

for nombre, (d, eo) in GLIFOS.items():
    tipo = '\n        android:fillType="evenOdd"' if eo else ''
    ruta = os.path.join(DEST, "core_ui_ic_plato_%s.xml" % nombre)
    io.open(ruta, "w", encoding="utf-8", newline="\n").write(
        CABECERA.format(nombre=nombre, tipo=tipo, d=" ".join(d.split())))
    print("  core_ui_ic_plato_%s.xml" % nombre)

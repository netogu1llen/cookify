"""Genera db/002_semilla.sql desde las fuentes de verdad del proyecto.

Lee el catalogo de la app (core/data/src/main/assets/ingredientes.json) y el
mapeo de fuentes (db/fuentes_precios.json), y escribe un script idempotente con
las seis cadenas, los 98 ingredientes, las fuentes de las dos cadenas con
recolector y un precio ESTIMADO para cada par cadena-ingrediente.

Por que se genera y no se escribe a mano: el catalogo de la app ya es la fuente
de verdad de los ingredientes. Copiarlos a mano al SQL crea dos verdades que se
separan en la primera edicion.

Los precios sembrados son ESTIMADO, derivados del precio base por el
multiplicador de la cadena, y se fechan el dia del catalogo (2026-09-02) a
proposito: cualquier medicion posterior del recolector los reemplaza en la vista
precio_vigente sin tener que borrar nada.

Uso:
  python tools/generar_semilla.py
"""
import io
import json
import os

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Las seis cadenas que la app ofrece. El multiplicador es el del enum
# Supermercado de :core:model, y se mantiene porque sigue siendo lo unico que
# hay para las cuatro cadenas sin recolector.
CADENAS = [
    ("LIDER",        "Líder",        1.00, False, "https://www.lider.cl"),
    ("ACUENTA",      "aCuenta",      0.92, False, "https://www.acuenta.cl"),
    ("TOTTUS",       "Tottus",       0.98, False, "https://www.tottus.cl"),
    ("UNIMARC",      "Unimarc",      1.05, False, "https://www.unimarc.cl"),
    ("SANTA_ISABEL", "Santa Isabel", 1.08, True,  "https://www.santaisabel.cl"),
    ("JUMBO",        "Jumbo",        1.15, True,  "https://www.jumbo.cl"),
]

CON_RECOLECTOR = ("JUMBO", "SANTA_ISABEL")
FECHA_CATALOGO = "2026-09-02T00:00:00-04:00"


def txt(s):
    """Literal de texto de PostgreSQL."""
    return "'" + str(s).replace("'", "''") + "'"


def arreglo(xs):
    """Literal de arreglo de texto de PostgreSQL."""
    if not xs:
        return "'{}'"
    return "array[" + ", ".join(txt(x) for x in xs) + "]::text[]"


def main():
    catalogo = json.load(io.open(
        os.path.join(RAIZ, "core", "data", "src", "main", "assets", "ingredientes.json"),
        encoding="utf-8"))
    mapeo = json.load(io.open(os.path.join(RAIZ, "db", "fuentes_precios.json"), encoding="utf-8"))
    ingredientes = catalogo["ingredientes"]
    ajustes = mapeo.get("ajustesPorCadena", {})

    o = []
    w = o.append

    w("-- ============================================================================")
    w("-- Cookify · semilla del servicio de precios")
    w("--")
    w("-- GENERADO. No editar a mano: lo produce tools/generar_semilla.py desde")
    w("-- core/data/src/main/assets/ingredientes.json y db/fuentes_precios.json.")
    w("-- Para rehacerlo:  python tools/generar_semilla.py")
    w("--")
    w("-- Es idempotente: se puede aplicar sobre una base ya sembrada sin duplicar")
    w("-- nada. docker-compose lo monta en /docker-entrypoint-initdb.d, despues del")
    w("-- esquema.")
    w("-- ============================================================================")
    w("")
    w("begin;")
    w("")
    w("-- --- cadenas -----------------------------------------------------------")
    w("insert into supermercado (codigo, etiqueta, multiplicador_estimado, soporta_medicion, sitio) values")
    filas = [
        "  (%s, %s, %.2f, %s, %s)" % (txt(c), txt(e), m, str(s).lower(), txt(w_))
        for c, e, m, s, w_ in CADENAS
    ]
    w(",\n".join(filas))
    w("on conflict (codigo) do update set")
    w("    etiqueta               = excluded.etiqueta,")
    w("    multiplicador_estimado = excluded.multiplicador_estimado,")
    w("    soporta_medicion       = excluded.soporta_medicion,")
    w("    sitio                  = excluded.sitio;")
    w("")

    w("-- --- los %d ingredientes del recetario ----------------------------------" % len(ingredientes))
    w("insert into ingrediente (id, nombre, pasillo, unidad, precio_base_clp) values")
    filas = [
        "  (%s, %s, %s, %s, %s)" % (
            txt(i["id"]), txt(i["nombre"]), txt(i["pasillo"]),
            txt(i["unidad"]), repr(float(i["precioClpPorUnidad"])))
        for i in ingredientes
    ]
    w(",\n".join(filas))
    w("on conflict (id) do update set")
    w("    nombre          = excluded.nombre,")
    w("    pasillo         = excluded.pasillo,")
    w("    unidad          = excluded.unidad,")
    w("    precio_base_clp = excluded.precio_base_clp,")
    w("    actualizado_en  = now();")
    w("")

    total_fuentes = 0
    for cadena in CON_RECOLECTOR:
        ajuste = ajustes.get(cadena, {})
        filas = []
        inactivas = 0
        for f in mapeo["fuentes"]:
            fuente = dict(f)
            fuente.update(ajuste.get(f["ingredienteId"], {}))
            activa = bool(fuente.get("activa", True))
            if not activa:
                inactivas += 1
            filas.append("  (%s, %s, %s, %s, %s, %s, %s, %s)" % (
                txt(cadena), txt(fuente["ingredienteId"]), txt(fuente["categoria"]),
                arreglo(fuente["terminos"]), arreglo(fuente["excluir"]),
                txt(fuente["medida"]), repr(float(fuente["factor"])), str(activa).lower()))
        total_fuentes += len(filas)
        w("-- --- fuentes de %s: %d, de las cuales %d inactivas -------------------"
          % (cadena, len(filas), inactivas))
        if inactivas:
            w("-- Una fuente inactiva es un ingrediente que la cadena no vende en linea.")
            w("-- Su precio queda ESTIMADO, que es preferible a apuntar a una categoria")
            w("-- que no corresponde y cobrar mal sin que nada falle.")
        w("insert into fuente_precio (supermercado_codigo, ingrediente_id, categoria_ruta,")
        w("                           terminos, excluir, medida_esperada,")
        w("                           unidades_base_por_medida, activa) values")
        w(",\n".join(filas))
        w("on conflict (supermercado_codigo, ingrediente_id) do update set")
        w("    categoria_ruta           = excluded.categoria_ruta,")
        w("    terminos                 = excluded.terminos,")
        w("    excluir                  = excluded.excluir,")
        w("    medida_esperada          = excluded.medida_esperada,")
        w("    unidades_base_por_medida = excluded.unidades_base_por_medida,")
        w("    activa                   = excluded.activa,")
        w("    actualizado_en           = now();")
        w("")

    w("-- --- precio estimado de partida para las %d cadenas ---------------------" % len(CADENAS))
    w("-- Derivado del precio base por el multiplicador de la cadena, fechado el dia")
    w("-- del catalogo. Existe para que la API tenga respuesta para cualquier par")
    w("-- cadena-ingrediente desde el primer arranque; cualquier medicion posterior")
    w("-- del recolector lo reemplaza en precio_vigente, porque es mas nueva.")
    filas = []
    for codigo, _, mult, _, _ in CADENAS:
        for i in ingredientes:
            filas.append("  (%s, %s, %s, %s, 'ESTIMADO', 0, %s)" % (
                txt(codigo), txt(i["id"]),
                repr(round(float(i["precioClpPorUnidad"]) * mult, 4)),
                txt(i["unidad"]), txt(FECHA_CATALOGO)))
    w("insert into precio (supermercado_codigo, ingrediente_id, precio_clp_por_unidad,")
    w("                    unidad, origen, candidatos, medido_en) values")
    w(",\n".join(filas))
    w("on conflict (supermercado_codigo, ingrediente_id, medido_en) do nothing;")
    w("")
    w("commit;")
    w("")

    destino = os.path.join(RAIZ, "db", "002_semilla.sql")
    io.open(destino, "w", encoding="utf-8", newline="\n").write("\n".join(o))
    print("escrito %s" % destino)
    print("  %d cadenas, %d ingredientes, %d fuentes, %d precios estimados"
          % (len(CADENAS), len(ingredientes), total_fuentes, len(filas)))


if __name__ == "__main__":
    main()

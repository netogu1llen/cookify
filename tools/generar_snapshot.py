"""Vuelca los precios MEDIDO de la base a db/003_snapshot_precios.sql.

El snapshot existe para que la demostracion en vivo no dependa de que el sitio
de la cadena responda en la sala. Con el, una base recien creada ya trae
precios medidos de verdad; sin el, habria que esperar seis minutos de
recoleccion delante del evaluador o mostrar solo precios estimados.

No reemplaza al recolector: son los datos que el recolector produjo, con la
fecha en que los produjo y el detalle de la corrida que los dejo.

Uso, con el servicio corriendo y .env cargado:
  python tools/generar_snapshot.py
"""
import io
import os
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

CONSULTA = """
copy (
  select p.supermercado_codigo, p.ingrediente_id, p.precio_clp_por_unidad,
         p.unidad, p.candidatos, to_char(p.medido_en, 'YYYY-MM-DD"T"HH24:MI:SSOF')
    from precio_vigente p
   where p.origen = 'MEDIDO'
   order by p.supermercado_codigo, p.ingrediente_id
) to stdout with (format csv)
"""


def main():
    crudo = subprocess.run(
        ["docker", "compose", "exec", "-T", "db", "psql", "-U", "cookify", "-d", "cookify",
         "-v", "ON_ERROR_STOP=1", "-c", CONSULTA.strip()],
        cwd=RAIZ, capture_output=True, text=True, encoding="utf-8",
    )
    if crudo.returncode != 0:
        print(crudo.stderr, file=sys.stderr)
        raise SystemExit("no se pudo leer la base: esta levantada con docker compose up -d db?")

    filas = [l for l in crudo.stdout.splitlines() if l.strip()]
    if not filas:
        raise SystemExit("no hay precios MEDIDO en la base: correr una recoleccion primero")

    valores = []
    cadenas = {}
    for linea in filas:
        cadena, ingrediente, precio, unidad, candidatos, medido = linea.split(",")
        cadenas[cadena] = cadenas.get(cadena, 0) + 1
        valores.append("  ('%s', '%s', %s, '%s', 'MEDIDO', %s, '%s')"
                       % (cadena, ingrediente, precio, unidad, candidatos, medido))

    detalle = " · ".join("%s: %d" % (c, n) for c, n in sorted(cadenas.items()))

    o = []
    w = o.append
    w("-- ============================================================================")
    w("-- Cookify · snapshot de precios medidos")
    w("--")
    w("-- GENERADO. Lo produce tools/generar_snapshot.py desde la base, despues de")
    w("-- una recoleccion real contra el sitio de las cadenas.")
    w("--")
    w("-- Esta aqui para que una base recien creada traiga precios medidos de verdad")
    w("-- y la demostracion no dependa de que jumbo.cl responda en la sala. No")
    w("-- reemplaza al recolector: son exactamente los datos que el recolector")
    w("-- produjo, con la fecha en que los produjo.")
    w("--")
    w("-- %d precios · %s" % (len(valores), detalle))
    w("-- ============================================================================")
    w("")
    w("begin;")
    w("")
    w("insert into precio (supermercado_codigo, ingrediente_id, precio_clp_por_unidad,")
    w("                    unidad, origen, candidatos, medido_en) values")
    w(",\n".join(valores))
    w("on conflict (supermercado_codigo, ingrediente_id, medido_en) do nothing;")
    w("")
    w("commit;")
    w("")

    destino = os.path.join(RAIZ, "db", "003_snapshot_precios.sql")
    io.open(destino, "w", encoding="utf-8", newline="\n").write("\n".join(o))
    print("escrito %s" % destino)
    print("  %d precios medidos · %s" % (len(valores), detalle))


if __name__ == "__main__":
    main()

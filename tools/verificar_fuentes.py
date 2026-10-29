"""Verifica el mapeo de db/fuentes_precios.json contra el sitio de la cadena.

Para cada ingrediente baja su categoria, aplica los terminos y las exclusiones,
normaliza a la unidad base del catalogo y compara con el precio escrito a mano
en ingredientes.json. Imprime una fila por ingrediente y marca:

  SIN CANDIDATOS   la categoria o los terminos estan mal: no hay que adivinar,
                   hay que corregir el mapeo.
  MEDIDA           la cadena publica ese producto en otra unidad de medida que
                   la declarada en la fuente.
  DESVIO           el precio sale, pero se aleja mas de lo tolerable del valor
                   de referencia: hay que mirarlo a ojo.

Es una herramienta de desarrollo, no parte del servicio: el servicio hace lo
mismo en Kotlin (RecolectorServicio + Normalizador). Vive aqui para que el
mapeo sea auditable sin levantar nada.

Uso:
  python tools/verificar_fuentes.py --cadena JUMBO
  python tools/verificar_fuentes.py --cadena SANTA_ISABEL --solo cebolla,ajo
  python tools/verificar_fuentes.py --cadena JUMBO --volcar snapshot.json
"""
import argparse
import json
import os
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(RAIZ, "build", "cache-fuentes")

SITIOS = {
    "JUMBO": "https://www.jumbo.cl",
    "SANTA_ISABEL": "https://www.santaisabel.cl",
}

AGENTE = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
)

PAGINAS_MAX = 4
TOLERANCIA = 0.60  # 60 % de desvio contra el precio de referencia


def sin_tildes(s):
    return "".join(
        c for c in unicodedata.normalize("NFD", s.lower())
        if unicodedata.category(c) != "Mn"
    )


def bajar(url, ttl_horas=12):
    """Baja la pagina con cache en disco, para poder iterar el mapeo sin
    golpear el sitio de la cadena una vez por intento."""
    os.makedirs(CACHE, exist_ok=True)
    clave = re.sub(r"[^a-z0-9]+", "_", url.lower())[-120:]
    destino = os.path.join(CACHE, clave + ".html")
    if os.path.exists(destino):
        edad = (time.time() - os.path.getmtime(destino)) / 3600
        if edad < ttl_horas:
            with open(destino, encoding="utf-8", errors="replace") as f:
                return f.read()
    pedido = urllib.request.Request(url, headers={
        "User-Agent": AGENTE,
        "Accept-Language": "es-CL,es;q=0.9",
    })
    try:
        with urllib.request.urlopen(pedido, timeout=45) as r:
            cuerpo = r.read().decode("utf-8", errors="replace")
    except Exception as e:
        return "ERROR:" + str(e)
    with open(destino, "w", encoding="utf-8") as f:
        f.write(cuerpo)
    return cuerpo


def objetos_con(texto, clave):
    """Todo objeto JSON de primer nivel que tenga `clave` como primera llave.

    Cuenta llaves en lugar de usar una expresion regular porque los objetos
    vienen anidados y una regex no cierra bien el parentesis.
    """
    salida = []
    aguja = '{"' + clave + '":'
    i = texto.find(aguja)
    while i >= 0:
        prof, en_cadena, escapa = 0, False, False
        for j in range(i, min(len(texto), i + 20000)):
            c = texto[j]
            if en_cadena:
                if escapa:
                    escapa = False
                elif c == "\\":
                    escapa = True
                elif c == '"':
                    en_cadena = False
                continue
            if c == '"':
                en_cadena = True
            elif c == "{":
                prof += 1
            elif c == "}":
                prof -= 1
                if prof == 0:
                    try:
                        salida.append(json.loads(texto[i:j + 1]))
                    except Exception:
                        pass
                    break
        i = texto.find(aguja, i + 1)
    return salida


def productos(sitio, ruta):
    """Todos los productos de una categoria, recorriendo sus paginas.

    Una categoria que se acaba responde 404 a la pagina siguiente, asi que un
    error despues de la primera pagina es el fin del recorrido y no una falla:
    solo el 404 de la primera pagina significa que la ruta no existe.
    """
    vistos, items = set(), []
    for pagina in range(1, PAGINAS_MAX + 1):
        url = sitio + ruta + ("" if pagina == 1 else "?page=%d" % pagina)
        html = bajar(url)
        if html.startswith("ERROR:"):
            return (items, None) if pagina > 1 else (items, html)
        crudos = objetos_con(html.replace('\\"', '"'), "skuId")
        nuevos = [i for i in crudos if i.get("skuId") not in vistos]
        for i in nuevos:
            vistos.add(i.get("skuId"))
        items += nuevos
        if len(crudos) < 40:  # ultima pagina
            break
    return items, None


def percentil25(valores):
    """Percentil 25 por interpolacion lineal.

    No la mediana: el planificador asume que quien cocina con presupuesto
    contado compra la marca barata, no la del medio. Con pocos candidatos cae
    naturalmente hacia el minimo, que es el comportamiento deseado.
    """
    if not valores:
        return None
    v = sorted(valores)
    if len(v) == 1:
        return v[0]
    pos = 0.25 * (len(v) - 1)
    bajo = int(pos)
    resto = pos - bajo
    if bajo + 1 >= len(v):
        return v[bajo]
    return v[bajo] + resto * (v[bajo + 1] - v[bajo])


# De la unidad en que la cadena publica el precio por unidad de medida a la
# unidad canonica, con su divisor. La cadena mezcla escalas dentro de una misma
# categoria, asi que esto no es opcional.
#
# El divisor de 'g' es 10 y no 1. Verificado contra precio y tamano en los 176
# productos de la categoria de condimentos: el oregano de 50 g cuesta $1.150,
# o sea $23 por gramo, y la cadena publica ppum = 230. La paprika de 100 g
# cuesta $1.950, o sea $19,5 por gramo, y publica 195. Cuando la unidad es 'g'
# el valor viene por cada 10 gramos; tomarlo como precio por gramo multiplicaba
# por diez el costo de todos los condimentos de una receta.
CANONICA = {
    "kg": ("g", 1000.0), "g": ("g", 10.0), "gr": ("g", 10.0),
    "lt": ("ml", 1000.0), "l": ("ml", 1000.0), "ml": ("ml", 10.0), "cc": ("ml", 10.0),
    "un": ("un", 1.0), "unidad": ("un", 1.0),
}


def canonizar(unidad, ppum):
    """Lleva el precio publicado a $/g, $/ml o $/un."""
    par = CANONICA.get(str(unidad).lower())
    if not par:
        return None, None
    base, divisor = par
    return base, ppum / divisor


def candidatos(items, fuente):
    """Los productos de la categoria que son el ingrediente, ya normalizados."""
    terminos = [sin_tildes(t) for t in fuente["terminos"]]
    excluir = [sin_tildes(t) for t in fuente["excluir"]]
    medida = fuente["medida"]
    factor = float(fuente["factor"])

    elegidos, otra_medida = [], 0
    for it in items:
        nombre = sin_tildes(str(it.get("name", "")))
        if terminos and not any(t in nombre for t in terminos):
            continue
        if any(x in nombre for x in excluir):
            continue
        if not it.get("stock", True):
            continue
        precio = it.get("ppumListPrice") or it.get("ppumPrice")
        if not precio or precio <= 0:
            continue
        base, canonico = canonizar(it.get("ppumMeasurementUnit"), float(precio))
        if base != medida:
            otra_medida += 1
            continue
        elegidos.append((canonico / factor, it.get("name"), float(precio)))
    return elegidos, otra_medida


def ubicar(sitio, texto):
    """En que categoria vive un producto, segun el buscador de la cadena.

    Sirve para colocar los ingredientes que no se encuentran a mano en el
    arbol. Se lee de data-gtm-impression porque ese atributo trae la categoria
    escrita en palabras, que es justamente lo que falta saber.
    """
    import html as htmlmod
    url = sitio + "/busqueda?q=" + urllib.parse.quote(texto)
    cuerpo = bajar(url, ttl_horas=6)
    if cuerpo.startswith("ERROR:"):
        print("  ", cuerpo[:90])
        return
    vistos = set()
    for crudo in re.findall(r'data-gtm-impression="([^"]*)"', cuerpo):
        try:
            d = json.loads(htmlmod.unescape(crudo))
        except Exception:
            continue
        clave = (d.get("name"), d.get("category"))
        if clave in vistos:
            continue
        vistos.add(clave)
        print("   %-46.46s $%-8s %s" % (d.get("name"), d.get("price"), d.get("category")))
    if not vistos:
        print("   sin resultados renderizados en el servidor")


def diagnostico(sitio, mapeo, ajustes, iid):
    """Todo lo que la categoria de un ingrediente contiene, sin filtrar por
    medida, para poder decidir que medida y que factor declarar."""
    fuente = next(dict(f) for f in mapeo["fuentes"] if f["ingredienteId"] == iid)
    fuente.update(ajustes.get(iid, {}))
    items, error = productos(sitio, fuente["categoria"])
    print("%s  ->  %s   (%d productos, medida declarada %s, factor %s)" % (
        iid, fuente["categoria"], len(items), fuente["medida"], fuente["factor"]))
    if error:
        print("  ", error[:90])
        return
    terminos = [sin_tildes(t) for t in fuente["terminos"]]
    excluir = [sin_tildes(t) for t in fuente["excluir"]]
    for it in items:
        nombre = sin_tildes(str(it.get("name", "")))
        if terminos and not any(t in nombre for t in terminos):
            continue
        marca = "excluido" if any(x in nombre for x in excluir) else ""
        print("   %-48.48s ppum=%-8s %-3s venta=%s x%-5s %s" % (
            it.get("name"), it.get("ppumListPrice"), it.get("ppumMeasurementUnit"),
            it.get("measurementUnitUn"), it.get("unitMultiplierUn"), marca))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cadena", default="JUMBO", choices=sorted(SITIOS))
    ap.add_argument("--solo", default="", help="ids separados por coma")
    ap.add_argument("--volcar", default="", help="archivo json con el resultado")
    ap.add_argument("--diagnostico", default="", help="ids a mirar en detalle")
    ap.add_argument("--ubicar", default="", help="textos a buscar en el sitio")
    ap.add_argument("--listar", default="", help="ruta de categoria a listar completa")
    args = ap.parse_args()
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    sitio = SITIOS[args.cadena]
    mapeo = json.load(open(os.path.join(RAIZ, "db", "fuentes_precios.json"), encoding="utf-8"))
    ajustes = mapeo.get("ajustesPorCadena", {}).get(args.cadena, {})
    catalogo = {
        i["id"]: i for i in json.load(open(
            os.path.join(RAIZ, "core", "data", "src", "main", "assets", "ingredientes.json"),
            encoding="utf-8"))["ingredientes"]
    }

    if args.ubicar:
        for texto in args.ubicar.split(","):
            print("== buscar:", texto.strip())
            ubicar(sitio, texto.strip())
        return
    if args.listar:
        for ruta in args.listar.split(","):
            items, error = productos(sitio, ruta.strip())
            print("==", ruta.strip(), len(items), error or "")
            for it in sorted(items, key=lambda x: str(x.get("name"))):
                print("   %-54.54s ppum=%-8s %-3s x%s" % (
                    it.get("name"), it.get("ppumListPrice"),
                    it.get("ppumMeasurementUnit"), it.get("unitMultiplierUn")))
        return
    if args.diagnostico:
        for iid in args.diagnostico.split(","):
            diagnostico(sitio, mapeo, ajustes, iid.strip())
        return

    filtro = {s.strip() for s in args.solo.split(",") if s.strip()}
    fuentes = [f for f in mapeo["fuentes"] if not filtro or f["ingredienteId"] in filtro]

    cache_categoria = {}
    resultado, problemas = [], 0

    print("%-26s %-9s %10s %10s %7s %6s  %s" % (
        "ingrediente", "estado", "medido", "referencia", "desvio", "cand.", "producto p25"))
    print("-" * 118)

    for f in fuentes:
        iid = f["ingredienteId"]
        fuente = dict(f)
        fuente.update(ajustes.get(iid, {}))
        ruta = fuente["categoria"]

        if not fuente.get("activa", True):
            # Declarada inactiva a proposito: el ingrediente no se vende en el
            # catalogo en linea y su precio queda ESTIMADO.
            print("%-26s %-9s %10s %10.2f" % (iid, "inactiva", "-",
                                              catalogo[iid]["precioClpPorUnidad"]))
            resultado.append({"ingredienteId": iid, "estado": "inactiva",
                              "categoria": ruta, "activa": False})
            continue

        if ruta not in cache_categoria:
            cache_categoria[ruta] = productos(sitio, ruta)
        items, error = cache_categoria[ruta]

        base = catalogo[iid]["precioClpPorUnidad"]
        fila = {
            "ingredienteId": iid, "categoria": ruta, "referencia": base,
            "medida": fuente["medida"], "factor": fuente["factor"],
        }

        if error:
            print("%-26s %-9s  %s" % (iid, "RED", error[:70]))
            fila["estado"] = "RED"
            problemas += 1
            resultado.append(fila)
            continue

        elegidos, otra_medida = candidatos(items, fuente)
        if not elegidos:
            estado = "MEDIDA" if otra_medida else "SIN CAND."
            print("%-26s %-9s %10s %10.2f %7s %6d  (categoria con %d productos)" % (
                iid, estado, "-", base, "-", 0, len(items)))
            fila["estado"] = estado
            fila["productosEnCategoria"] = len(items)
            fila["otraMedida"] = otra_medida
            problemas += 1
            resultado.append(fila)
            continue

        p25 = percentil25([e[0] for e in elegidos])
        cerca = min(elegidos, key=lambda e: abs(e[0] - p25))
        desvio = (p25 - base) / base
        estado = "ok" if abs(desvio) <= TOLERANCIA else "DESVIO"
        if estado != "ok":
            problemas += 1
        print("%-26s %-9s %10.3f %10.2f %+6.0f%% %6d  %s" % (
            iid, estado, p25, base, desvio * 100, len(elegidos), str(cerca[1])[:40]))

        fila.update({
            "estado": estado, "medido": round(p25, 4), "desvio": round(desvio, 4),
            "candidatos": len(elegidos), "ejemplo": cerca[1], "ppum": cerca[2],
        })
        resultado.append(fila)

    print("-" * 118)
    print("%d fuentes, %d con problema" % (len(fuentes), problemas))
    if args.volcar:
        json.dump({"cadena": args.cadena, "fuentes": resultado},
                  open(args.volcar, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        print("volcado en", args.volcar)


if __name__ == "__main__":
    main()

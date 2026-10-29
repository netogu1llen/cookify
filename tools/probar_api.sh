#!/usr/bin/env bash
# Recorre el contrato de la API y verifica el codigo de estado de cada caso,
# incluidos los de error. Es la red de seguridad antes de demostrar en vivo:
# si algo se rompio, aparece aqui y no delante del evaluador.
#
# Uso:  set -a && . ./.env && set +a && bash tools/probar_api.sh
set -u

BASE="http://localhost:${COOKIFY_PUERTO:-8090}"
API="$BASE/api/v1"
CLAVE="${COOKIFY_API_KEY:?falta COOKIFY_API_KEY: cargar el .env antes de correr esto}"

ok=0
fallos=0

# Cada corrida usa su propio instante de medicion. Con una fecha fija, el POST
# de la segunda corrida chocaria con el de la primera y daria 409: una bateria
# que solo pasa la primera vez no sirve de red.
MEDIDO_EN=$(date +%Y-%m-%dT%H:%M:%S-03:00)

# verificar <esperado> <descripcion> <curl...>
verificar() {
  local esperado="$1" descripcion="$2"; shift 2
  local cuerpo codigo
  cuerpo=$(curl -s -o /tmp/cookify_respuesta -w "%{http_code}" "$@")
  codigo="$cuerpo"
  if [ "$codigo" = "$esperado" ]; then
    ok=$((ok + 1))
    printf '  \033[32mok\033[0m   %-3s  %s\n' "$codigo" "$descripcion"
  else
    fallos=$((fallos + 1))
    printf '  \033[31mMAL\033[0m  %-3s  %s  (se esperaba %s)\n' "$codigo" "$descripcion" "$esperado"
    head -c 300 /tmp/cookify_respuesta; echo
  fi
}

echo "== salud y catalogo cerrado =="
verificar 200 "GET /salud"                        "$BASE/salud"
verificar 200 "GET /supermercados"                "$API/supermercados"
verificar 200 "GET /supermercados/JUMBO"          "$API/supermercados/JUMBO"
verificar 404 "GET /supermercados/NO_EXISTE"      "$API/supermercados/NO_EXISTE"

echo "== ingredientes: las cuatro operaciones =="
verificar 200 "GET /ingredientes"                 "$API/ingredientes"
verificar 200 "GET /ingredientes?pasillo=LACTEOS" "$API/ingredientes?pasillo=LACTEOS"
verificar 200 "GET /ingredientes?q=brocoli"       "$API/ingredientes?q=brocoli"
verificar 400 "GET con pasillo inexistente"       "$API/ingredientes?pasillo=FERRETERIA"
verificar 400 "GET con pagina 0"                  "$API/ingredientes?pagina=0"
verificar 400 "GET con tamano 500"                "$API/ingredientes?tamano=500"
verificar 200 "GET /ingredientes/cebolla"         "$API/ingredientes/cebolla"
verificar 404 "GET de uno que no existe"          "$API/ingredientes/unicornio"

CUERPO='{"id":"cebolla_morada","nombre":"Cebolla morada","pasillo":"VERDULERIA","unidad":"g","precioBaseClp":1.9}'

verificar 401 "POST sin clave" \
  -X POST -H "Content-Type: application/json" -d "$CUERPO" "$API/ingredientes"
verificar 401 "POST con clave equivocada" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: no-es-la-clave" \
  -d "$CUERPO" "$API/ingredientes"
verificar 201 "POST crea el ingrediente" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d "$CUERPO" "$API/ingredientes"
verificar 409 "POST repetido no reemplaza en silencio" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d "$CUERPO" "$API/ingredientes"
verificar 400 "POST con precio negativo y unidad invalida" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"id":"malo","nombre":"","pasillo":"NADA","unidad":"kg","precioBaseClp":-5}' \
  "$API/ingredientes"
verificar 400 "POST con cuerpo que no es JSON" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d 'esto no es json' "$API/ingredientes"
verificar 200 "PUT actualiza" \
  -X PUT -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"nombre":"Cebolla morada grande","pasillo":"VERDULERIA","unidad":"g","precioBaseClp":2.1}' \
  "$API/ingredientes/cebolla_morada"
verificar 404 "PUT de uno que no existe" \
  -X PUT -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"nombre":"X","pasillo":"VERDULERIA","unidad":"g","precioBaseClp":1}' \
  "$API/ingredientes/no_existe"
verificar 204 "DELETE borra" \
  -X DELETE -H "X-API-Key: $CLAVE" "$API/ingredientes/cebolla_morada"
verificar 404 "GET despues del DELETE"            "$API/ingredientes/cebolla_morada"
verificar 404 "DELETE repetido"                   -X DELETE -H "X-API-Key: $CLAVE" \
  "$API/ingredientes/cebolla_morada"

echo "== fuentes =="
verificar 200 "GET /fuentes?supermercado=JUMBO"   "$API/fuentes?supermercado=JUMBO"
verificar 200 "GET /fuentes?activa=false"         "$API/fuentes?activa=false"
verificar 400 "GET con activa que no es booleano" "$API/fuentes?activa=quizas"
verificar 400 "GET /fuentes/abc"                  "$API/fuentes/abc"
verificar 404 "POST de fuente con ingrediente inexistente" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"supermercado":"JUMBO","ingredienteId":"unicornio","categoriaRuta":"/x","medidaEsperada":"g","unidadesBasePorMedida":1}' \
  "$API/fuentes"
verificar 409 "POST de fuente repetida para el mismo par" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"supermercado":"JUMBO","ingredienteId":"cebolla","categoriaRuta":"/x","medidaEsperada":"g","unidadesBasePorMedida":1}' \
  "$API/fuentes"
verificar 400 "POST de fuente con ruta sin barra" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"supermercado":"JUMBO","ingredienteId":"cebolla","categoriaRuta":"sin-barra","medidaEsperada":"g","unidadesBasePorMedida":0}' \
  "$API/fuentes"

echo "== precios =="
verificar 200 "GET /precios?supermercado=JUMBO"   "$API/precios?supermercado=JUMBO"
verificar 200 "GET solo los medidos"              "$API/precios?supermercado=JUMBO&origen=MEDIDO"
verificar 200 "GET la historia"                   "$API/precios?historia=true&tamano=5"
verificar 400 "GET vigentes sin supermercado"     "$API/precios"
verificar 400 "GET con origen inexistente"        "$API/precios?supermercado=JUMBO&origen=ADIVINADO"
verificar 404 "GET vigentes de cadena inexistente" "$API/precios?supermercado=WALMART"
MEDICION="{\"supermercado\":\"LIDER\",\"ingredienteId\":\"cebolla\",\"precioClpPorUnidad\":1.45,\"unidad\":\"g\",\"origen\":\"MEDIDO\",\"candidatos\":3,\"medidoEn\":\"$MEDIDO_EN\"}"

verificar 201 "POST carga una medicion a mano" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d "$MEDICION" "$API/precios"
# Con grep y no con python: en Git Bash para Windows, python no entiende la
# ruta /tmp/... y devolveria vacio sin decir nada.
PRECIO_ID=$(grep -o '"id":[0-9]*' /tmp/cookify_respuesta | head -1 | cut -d: -f2)
verificar 409 "POST de la misma medicion dos veces" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d "$MEDICION" "$API/precios"
verificar 400 "POST con fecha que no es ISO 8601" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"supermercado":"LIDER","ingredienteId":"cebolla","precioClpPorUnidad":1,"unidad":"g","origen":"MEDIDO","medidoEn":"ayer"}' \
  "$API/precios"
verificar 200 "PUT corrige esa medicion" \
  -X PUT -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"supermercado":"LIDER","ingredienteId":"cebolla","precioClpPorUnidad":1.52,"unidad":"g","origen":"MEDIDO","candidatos":4}' \
  "$API/precios/$PRECIO_ID"
verificar 204 "DELETE la borra, y la bateria queda repetible" \
  -X DELETE -H "X-API-Key: $CLAVE" "$API/precios/$PRECIO_ID"

echo "== recolecciones =="
verificar 200 "GET /recolecciones"                "$API/recolecciones"
verificar 404 "GET de una que no existe"          "$API/recolecciones/999999"
verificar 409 "POST a una cadena sin recolector"  \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"supermercado":"TOTTUS"}' "$API/recolecciones"
verificar 404 "POST a una cadena inexistente" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{"supermercado":"WALMART"}' "$API/recolecciones"
verificar 400 "POST sin supermercado" \
  -X POST -H "Content-Type: application/json" -H "X-API-Key: $CLAVE" \
  -d '{}' "$API/recolecciones"

echo "== rutas que no existen =="
verificar 404 "GET /api/v1/inventado"             "$API/inventado"
verificar 404 "GET /api/v2/ingredientes"          "$BASE/api/v2/ingredientes"

echo
printf 'ok: %d   fallos: %d\n' "$ok" "$fallos"
[ "$fallos" -eq 0 ]

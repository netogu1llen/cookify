-- ============================================================================
-- Cookify · esquema del servicio de precios
-- PostgreSQL 18
--
-- Crea la base desde cero. Es la unica fuente de verdad del esquema: el
-- servicio no genera tablas al arrancar, asi que lo que esta aqui es lo que
-- hay corriendo. docker-compose lo monta en /docker-entrypoint-initdb.d y se
-- aplica solo la primera vez que el contenedor levanta.
--
-- Para aplicarlo a mano:
--   psql -h localhost -p 5434 -U cookify -d cookify -f db/001_esquema.sql
-- ============================================================================

begin;

-- ---------------------------------------------------------------------------
-- supermercado · catalogo cerrado de cadenas
--
-- No es configuracion del usuario: son las seis cadenas que la app ofrece.
-- soporta_medicion separa las dos donde el recolector funciona de las cuatro
-- que siguen con precio estimado, y es lo que deja a la app decir "medido" o
-- "estimado" sin inventar una precision que no tiene.
-- ---------------------------------------------------------------------------
create table supermercado (
    codigo                  text         primary key,
    etiqueta                text         not null,
    multiplicador_estimado  numeric(4,2) not null check (multiplicador_estimado > 0),
    soporta_medicion        boolean      not null default false,
    sitio                   text
);

-- ---------------------------------------------------------------------------
-- ingrediente · los 98 del catalogo de recetas
--
-- precio_base_clp es el precio de UNA unidad base (1 g, 1 ml o 1 unidad) en
-- Lider. numeric(10,4) y no entero a proposito: en gramos los precios son
-- fracciones de peso, y redondear aqui acumula un error grande al escalar a
-- cuatro porciones por siete dias.
-- ---------------------------------------------------------------------------
create table ingrediente (
    id               text          primary key,
    nombre           text          not null,
    pasillo          text          not null,
    unidad           text          not null,
    precio_base_clp  numeric(10,4) not null,
    creado_en        timestamptz   not null default now(),
    actualizado_en   timestamptz   not null default now(),

    constraint ingrediente_id_slug      check (id ~ '^[a-z0-9_]{2,48}$'),
    constraint ingrediente_nombre_hay   check (length(trim(nombre)) > 0),
    constraint ingrediente_pasillo_vale check (pasillo in (
        'VERDULERIA', 'CARNICERIA', 'PESCADERIA', 'LACTEOS',
        'ABARROTES', 'PANADERIA', 'CONGELADOS'
    )),
    constraint ingrediente_unidad_vale  check (unidad in ('g', 'ml', 'un')),
    constraint ingrediente_precio_pos   check (precio_base_clp > 0)
);

create index ingrediente_pasillo_idx on ingrediente (pasillo);

-- ---------------------------------------------------------------------------
-- recoleccion · una corrida del recolector
--
-- Existe para que una recoleccion sea consultable mientras corre, y para que
-- quede el rastro de por que un precio vale lo que vale. Va antes de precio
-- porque precio la referencia.
-- ---------------------------------------------------------------------------
create table recoleccion (
    id                   bigserial   primary key,
    supermercado_codigo  text        not null references supermercado (codigo),
    estado               text        not null,
    iniciada_en          timestamptz not null default now(),
    terminada_en         timestamptz,
    categorias_pedidas   integer     not null default 0,
    categorias_leidas    integer     not null default 0,
    productos_leidos     integer     not null default 0,
    precios_escritos     integer     not null default 0,
    detalle              text,

    constraint recoleccion_estado_vale check (estado in ('EN_CURSO', 'COMPLETADA', 'FALLIDA')),
    -- Una recoleccion en curso no tiene fecha de termino, y una terminada si.
    constraint recoleccion_cierre check (
        (estado =  'EN_CURSO' and terminada_en is null) or
        (estado <> 'EN_CURSO' and terminada_en is not null)
    )
);

create index recoleccion_super_idx on recoleccion (supermercado_codigo, iniciada_en desc);

-- ---------------------------------------------------------------------------
-- fuente_precio · de donde se lee el precio de un ingrediente en una cadena
--
-- Es el mapeo que sostiene todo: ingrediente -> categoria del sitio de la
-- cadena. Buscar por el nombre del ingrediente da precios absurdos (medido:
-- "leche" devuelve mediana de $8,74/ml contra $1,06 real, porque entra leche
-- condensada y en polvo). Dentro de la categoria el problema desaparece por
-- construccion. terminos afina cuando la categoria trae variedades y excluir
-- saca lo que no corresponde.
--
-- Un par (cadena, ingrediente) tiene a lo mas una fuente: con dos, el precio
-- dependeria de cual se leyo primero.
--
-- medida_esperada y unidades_base_por_medida resuelven el desencuentro entre
-- como mide el catalogo y como vende la cadena.
--
-- La cadena publica el precio por unidad de medida (ppum), pero no siempre en
-- la misma: $1.725 por kg la papa, $228 por g la chia, $850 por un el pepino.
-- El recolector lo lleva primero a la unidad canonica (kg -> g, lt -> ml) y
-- despues divide por este factor, que es cuantas unidades base del catalogo
-- entrega UNA unidad canonica del producto:
--
--   cebolla   producto en $/g    1 g     = 1 g de catalogo        ->     1
--   ajo       producto en $/un   1 cabeza= 10 dientes de catalogo ->    10
--   palta     producto en $/g    1 g     = 1/200 de una palta     -> 0.005
--
-- Un producto cuya medida canonica no sea la esperada se descarta en lugar de
-- convertirse a la fuerza: esa conversion seria una invencion.
-- ---------------------------------------------------------------------------
create table fuente_precio (
    id                        bigserial     primary key,
    supermercado_codigo       text          not null references supermercado (codigo) on delete cascade,
    ingrediente_id            text          not null references ingrediente (id)      on delete cascade,
    categoria_ruta            text          not null,
    terminos                  text[]        not null default '{}',
    excluir                   text[]        not null default '{}',
    medida_esperada           text          not null,
    unidades_base_por_medida  numeric(10,4) not null,
    activa                    boolean       not null default true,
    creado_en                 timestamptz   not null default now(),
    actualizado_en            timestamptz   not null default now(),

    constraint fuente_una_por_par    unique (supermercado_codigo, ingrediente_id),
    constraint fuente_ruta_no_vacia  check (length(trim(categoria_ruta)) > 0),
    constraint fuente_medida_vale    check (medida_esperada in ('g', 'ml', 'un')),
    constraint fuente_factor_positivo check (unidades_base_por_medida > 0)
);

create index fuente_super_activa_idx on fuente_precio (supermercado_codigo) where activa;

-- ---------------------------------------------------------------------------
-- precio · historia, no estado
--
-- Cada medicion se inserta; ninguna se sobrescribe. Asi se puede responder
-- cuanto costaba la cebolla hace tres semanas, y se puede auditar un precio
-- raro contra la recoleccion que lo produjo. El precio que la app usa sale de
-- la vista precio_vigente.
--
-- origen distingue lo medido en el sitio de la cadena de lo derivado del
-- catalogo base por multiplicador. Sin esa columna la app estaria obligada a
-- mentir sobre su precision en las cuatro cadenas sin recolector.
-- ---------------------------------------------------------------------------
create table precio (
    id                     bigserial     primary key,
    supermercado_codigo    text          not null references supermercado (codigo) on delete cascade,
    ingrediente_id         text          not null references ingrediente (id)      on delete cascade,
    precio_clp_por_unidad  numeric(10,4) not null,
    unidad                 text          not null,
    origen                 text          not null,
    candidatos             integer       not null default 0,
    recoleccion_id         bigint        references recoleccion (id) on delete set null,
    medido_en              timestamptz   not null default now(),

    constraint precio_positivo      check (precio_clp_por_unidad > 0),
    constraint precio_unidad_vale   check (unidad in ('g', 'ml', 'un')),
    constraint precio_origen_vale   check (origen in ('MEDIDO', 'ESTIMADO')),
    constraint precio_candidatos    check (candidatos >= 0),
    -- Dos mediciones del mismo par en el mismo instante son la misma medicion
    -- cargada dos veces.
    constraint precio_sin_duplicado unique (supermercado_codigo, ingrediente_id, medido_en)
);

create index precio_vigente_idx on precio (supermercado_codigo, ingrediente_id, medido_en desc);

-- ---------------------------------------------------------------------------
-- precio_vigente · la ultima medicion de cada par
--
-- La app pide los precios de Jumbo, no la historia de Jumbo. La vista resuelve
-- eso en la base con DISTINCT ON, en lugar de traer la historia completa por
-- la red y filtrarla en el servicio.
-- ---------------------------------------------------------------------------
create view precio_vigente as
select distinct on (p.supermercado_codigo, p.ingrediente_id)
       p.id,
       p.supermercado_codigo,
       p.ingrediente_id,
       p.precio_clp_por_unidad,
       p.unidad,
       p.origen,
       p.candidatos,
       p.recoleccion_id,
       p.medido_en
  from precio p
 order by p.supermercado_codigo, p.ingrediente_id, p.medido_en desc;

commit;

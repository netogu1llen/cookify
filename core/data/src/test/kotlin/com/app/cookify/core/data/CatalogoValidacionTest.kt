package com.app.cookify.core.data

import com.app.cookify.core.model.Antojo
import com.app.cookify.core.model.Restriccion
import com.app.cookify.core.model.Unidad
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Valida el catalogo que vive en assets.
 *
 * Lee los JSON del classpath en vez de por Context.getAssets(), para que corra como
 * unit test normal sin Robolectric ni emulador. Es la red de seguridad de cualquiera
 * que edite los precios a mano: ver assets/README.md.
 */
class CatalogoValidacionTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val ingredientesDto: IngredientesDto = leer("ingredientes.json")

    private val recetasDto: RecetasDto = leer("recetas.json")

    /**
     * Del classpath y no del filesystem: leerlo con File() dejaba el archivo fuera de
     * las entradas que Gradle rastrea, y la tarea se saltaba en UP-TO-DATE aunque el
     * catalogo hubiera cambiado. Ver el comentario en build.gradle.kts.
     */
    private inline fun <reified T> leer(nombre: String): T {
        val texto = checkNotNull(javaClass.classLoader?.getResourceAsStream(nombre)) {
            "$nombre no esta en el classpath del test"
        }.bufferedReader().use { it.readText() }
        return json.decodeFromString(texto)
    }

    private val ingredientes = ingredientesDto.ingredientes
    private val recetas = recetasDto.recetas

    @Test
    fun `los ids de ingrediente son unicos`() {
        val repetidos = ingredientes.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertThat(repetidos).isEmpty()
    }

    @Test
    fun `los ids de receta son unicos`() {
        val repetidos = recetas.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertThat(repetidos).isEmpty()
    }

    @Test
    fun `toda receta referencia ingredientes que existen`() {
        val conocidos = ingredientes.mapTo(mutableSetOf()) { it.id }
        val rotas = recetas.flatMap { receta ->
            receta.ingredientes
                .map { it.ingredienteId }
                .filterNot { it in conocidos }
                .map { "${receta.id} -> $it" }
        }
        assertThat(rotas).isEmpty()
    }

    @Test
    fun `todos los precios son positivos`() {
        val invalidos = ingredientes
            .filter { it.precioClpPorUnidad <= 0 }
            .map { it.id }
        assertThat(invalidos).isEmpty()
    }

    /**
     * El error clasico al editar precios: escribir el precio por kilo en un ingrediente
     * medido en gramos. Nada que se venda al peso cuesta mas de $100 el gramo
     * (eso serian $100.000 el kilo), asi que ese umbral separa bien el error del dato.
     */
    @Test
    fun `ningun precio por peso o volumen parece estar en la escala equivocada`() {
        val sospechosos = ingredientes
            .filter { it.unidad != Unidad.UNIDAD }
            .filter { it.precioClpPorUnidad > 100 }
            .map { "${it.id} = ${it.precioClpPorUnidad} por ${it.unidad.simbolo}" }
        assertThat(sospechosos).isEmpty()
    }

    @Test
    fun `todas las cantidades por porcion son positivas`() {
        val invalidas = recetas.flatMap { receta ->
            receta.ingredientes
                .filter { it.cantidadPorPorcion <= 0 }
                .map { "${receta.id} -> ${it.ingredienteId}" }
        }
        assertThat(invalidas).isEmpty()
    }

    @Test
    fun `toda receta tiene pasos, tiempo y macros`() {
        val incompletas = recetas
            .filter {
                it.pasos.isEmpty() ||
                    it.minutosTotal <= 0 ||
                    it.porcionesBase <= 0 ||
                    it.kcalPorPorcion <= 0 ||
                    it.proteinaGPorPorcion <= 0
            }
            .map { it.id }
        assertThat(incompletas).isEmpty()
    }

    @Test
    fun `toda receta vegana esta marcada tambien como vegetariana`() {
        val inconsistentes = recetas
            .filter { Restriccion.VEGANO in it.aptaPara && Restriccion.VEGETARIANO !in it.aptaPara }
            .map { it.id }
        assertThat(inconsistentes).isEmpty()
    }

    /**
     * Quien elige "solo pescado" tambien come vegetariano y vegano. Si una receta sin
     * carne se olvida de marcarlo, esa persona se queda sin variedad. Ver README.
     */
    @Test
    fun `toda receta vegetariana esta marcada tambien como apta para solo pescado`() {
        val inconsistentes = recetas
            .filter {
                Restriccion.VEGETARIANO in it.aptaPara && Restriccion.SOLO_PESCADO !in it.aptaPara
            }
            .map { it.id }
        assertThat(inconsistentes).isEmpty()
    }

    @Test
    fun `nadie declara mas antojos de los que el usuario puede elegir`() {
        // No es un limite del formato, pero una receta que dice cumplir los siete
        // antojos no esta describiendo nada y ensucia la puntuacion.
        val exageradas = recetas
            .filter { it.antojos.size > Antojo.entries.size - 2 }
            .map { it.id }
        assertThat(exageradas).isEmpty()
    }

    /**
     * Con menos recetas que dias no hay semana posible. Se exige holgura sobre siete
     * para que el motor tenga de donde elegir y pueda ajustar al presupuesto.
     */
    @Test
    fun `cada restriccion tiene recetas suficientes para una semana completa`() {
        val minimo = 12
        Restriccion.entries.forEach { restriccion ->
            val disponibles = recetas.count { it.respeta(restriccion) }
            assertThat(disponibles).isAtLeast(minimo)
        }
    }

    /**
     * Alguien puede tener solo estufa, o solo airfryer. Cada artefacto por separado
     * tiene que dar para armar una semana.
     */
    @Test
    fun `cada artefacto por si solo permite armar una semana`() {
        com.app.cookify.core.model.Artefacto.entries.forEach { artefacto ->
            val disponibles = recetas.count { it.puedeCocinarseCon(setOf(artefacto)) }
            assertThat(disponibles).isAtLeast(7)
        }
    }
}

// El servicio es un build de Gradle independiente del de la app Android, no un
// modulo de ese build. Dos razones: no tiene por que pelear con AGP ni con KSP
// para compilar Kotlin de JVM puro, y su ciclo de vida es propio — el servicio
// se despliega cuando cambia el servicio, no cuando cambia una pantalla.
//
// Se compila desde la raiz del repositorio con:  ./gradlew -p server <tarea>

rootProject.name = "cookify-server"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

# ============================================================================
# Cookify · imagen del servicio de precios
#
# Dos etapas. La primera compila con un JDK completo; la segunda se queda solo
# con el JRE y el artefacto. La imagen final no lleva Gradle, ni el codigo
# fuente, ni el cache de dependencias: lo que no esta en la imagen no hay que
# parchearlo despues.
#
# El contexto de construccion es la raiz del repositorio y no ./server, porque
# el wrapper de Gradle vive en la raiz y es lo que fija la version de Gradle
# para todos. Lo que entra al contexto lo limita .dockerignore.
#
#   docker compose build servicio
# ============================================================================

FROM eclipse-temurin:21-jdk AS construccion

WORKDIR /obra

# Primero el wrapper y los descriptores: mientras no cambien, Docker reusa la
# capa con las dependencias ya descargadas y la compilacion no vuelve a bajar
# internet entera.
COPY gradlew gradlew
COPY gradle gradle
COPY server/settings.gradle.kts server/build.gradle.kts server/
COPY server/gradle server/gradle
RUN chmod +x gradlew && ./gradlew -p server dependencies --no-daemon --quiet || true

COPY server/src server/src
RUN ./gradlew -p server installDist --no-daemon

# ---------------------------------------------------------------------------

# Alpine y no la imagen completa: medido, 323 MB contra 494 MB. Lo que no esta
# en la imagen no hay que parchearlo despues. Trae el wget de busybox, que es
# lo que usa la verificacion de salud de docker-compose.
FROM eclipse-temurin:21-jre-alpine

# No corre como root: si alguien llega a ejecutar codigo dentro del
# contenedor, que no llegue con todos los permisos.
RUN addgroup -S cookify && adduser -S -G cookify -s /sbin/nologin cookify
USER cookify

WORKDIR /opt/cookify
COPY --from=construccion --chown=cookify:cookify /obra/server/build/install/cookify-server ./

# El puerto de dentro del contenedor es siempre 8080; cual se publica afuera lo
# decide docker-compose con COOKIFY_PUERTO.
EXPOSE 8080

# Que la JVM mire los limites del contenedor y no los del equipo: sin esto
# toma como memoria disponible la del host y el contenedor muere por OOM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

ENTRYPOINT ["./bin/cookify-server"]

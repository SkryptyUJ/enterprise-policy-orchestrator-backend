FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY gradle gradle
COPY src src

RUN chmod +x gradlew
RUN ./gradlew --no-daemon clean bootJar -x test
RUN JAR_FILE=$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n 1) && cp "$JAR_FILE" build/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

ENV TZ=Europe/Warsaw

RUN useradd --system --uid 1001 --create-home --home-dir /home/appuser appuser

COPY --from=build --chown=appuser:appuser /workspace/build/app.jar app.jar

EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]


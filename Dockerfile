FROM eclipse-temurin:17-jre

LABEL org.opencontainers.image.vendor="Ioza"
ENV TZ=America/Mexico_City
WORKDIR /app
COPY --chown=10001:10001 app.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

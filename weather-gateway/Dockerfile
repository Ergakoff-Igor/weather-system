FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Копируем JAR файл
COPY target/weather-gateway-1.0.0.jar app.jar

# Создаем не-root пользователя для безопасности
RUN groupadd -r weather && useradd -r -g weather weather
USER weather

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
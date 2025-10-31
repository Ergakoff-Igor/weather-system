# Weather Collection and Forecast System

Система сбора и прогноза погоды, состоящая из микросервисов на Spring Boot.

## Архитектура

- **Weather Gateway Service** (порт 8080) - API шлюз для клиентов и погодных станций
- **Weather Processing Service** (порт 8081) - обработка данных и формирование прогнозов
- **PostgreSQL** - база данных для хранения погодных данных
- **RabbitMQ** - брокер сообщений для асинхронного взаимодействия

## Технологический стек

- Java 21
- Spring Boot 3.x
- Spring Data JPA
- Spring AMQP (RabbitMQ)
- PostgreSQL 15
- Docker & Docker Compose
- Maven
- Lombok
- Spring Boot Actuator

## Быстрый запуск

### Требования
- Docker & Docker Compose
- Java 21 (для локальной разработки)
- Maven 3.6+ (для локальной разработки)

### Запуск в Docker
1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd weather-system
```
2. Соберите и запустите все сервисы:
```bash
# Сборка и запуск
docker-compose up --build
```
```bash
# Или для запуска в фоновом режиме:
docker-compose up -d --build
```
3. Проверьте статус сервисов:
```bash
docker-compose ps
```
4. Остановка системы:
```bash
docker-compose down
```
## API Endpoints
### Weather Gateway Service (8080)
* Отправка данных о погоде
```bash
curl -X POST http://localhost:8080/api/v1/weather/data \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "station-1",
    "timestamp": "2025-10-07T10:30:00Z",
    "temperature": 25.5,
    "humidity": 65.0,
    "pressure": 1013.25,
    "precipitation": 0.0
  }'
```
* Получение прогноза погоды
```bash
curl "http://localhost:8080/api/v1/weather/forecast?stationId=station-1&hours=3"
```
### Прямые вызовы к Weather Processing Service (8081)
```bash
curl "http://localhost:8081/api/v1/weather/forecast?stationId=station-1&hours=3"
```
## Health Checks
* Gateway: http://localhost:8080/actuator/health
* Processing: http://localhost:8081/actuator/health
* RabbitMQ Management: http://localhost:15672 (guest/guest)

## Мониторинг
* Prometheus метрики: http://localhost:8080/actuator/prometheus
* Swagger UI: http://localhost:8080/swagger-ui.html

## Локальная разработка
### Запуск баз данных
```bash
docker-compose up postgres rabbitmq -d
```

### Запуск сервисов
* Weather Gateway
```bash
cd weather-gateway
mvn spring-boot:run
```
* Weather Processing (в отдельном терминале)
```bash
cd weather-processing
mvn spring-boot:run
```
## Тестирование
### Unit тесты
```bash
mvn test
Integration тесты
```
```bash
mvn verify
```
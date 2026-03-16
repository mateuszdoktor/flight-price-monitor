# Flight Price Monitor

A RESTful flight price anomaly detection system built with Spring Boot. The application tracks route prices via the Amadeus API, flags unusually cheap fares, and exposes a clean API for route management, historical data, statistics, and deal discovery.

## Features

- **Route Management** - Create, list, inspect, and deactivate monitored routes
- **Scheduled Price Collection** - Background price fetching for all active routes (every 6 hours by default)
- **Anomaly Detection** - Combined z-score and percentage-threshold detection
- **Deals Endpoint** - Aggregated list of currently detected low-price opportunities
- **Statistics Endpoint** - Mean, median, standard deviation, min/max, sample count, and z-score
- **Resilience for External API** - Retry, circuit breaker, timeout, and fallback for Amadeus calls
- **Integration Tests** - PostgreSQL Testcontainers-backed flow tests for API + persistence
- **Developer Experience** - Dockerfile, docker-compose, and Swagger/OpenAPI out of the box

## Tech Stack

### Backend

- **Java 25** with **Spring Boot 4.0**
- **Spring Data JPA** with Hibernate & PostgreSQL
- **Spring WebMVC** for REST API + **WebClient** for Amadeus integration
- **Flyway** for database migrations

### Key Features & Libraries

- **Bean Validation** for request validation
- **SpringDoc OpenAPI** for interactive API documentation
- **Resilience4j** (retry, circuit breaker, time limiter)
- **Lombok** for boilerplate reduction
- **Testcontainers** for integration testing against real PostgreSQL

### Architecture & Patterns

- Layered architecture (Controller-Service-Repository)
- DTO pattern with separate request/response models
- Domain-level statistics/anomaly utility logic
- Centralized exception handling with consistent error response model
- Soft-delete route semantics via deactivation
- Batch-oriented query path for deals to reduce N+1 issues

### Testing

- **JUnit 5** with **Mockito** for unit tests
- **MockWebServer** for Amadeus client integration-style tests
- **WebMvc tests** for controller and exception-handler contracts
- **Full-flow integration tests** with Testcontainers PostgreSQL

## Getting Started

### Prerequisites

- Java 25+
- Docker & Docker Compose

### Running with Docker Compose

1. Clone the repository

```bash
git clone https://github.com/yourusername/flight-price-monitor.git
cd flight-price-monitor
```

2. Start all services

```bash
docker compose up --build
```

3. Access the app and docs

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

4. Stop services

```bash
docker compose down
```

### Running Locally with Maven

1. Start PostgreSQL (local or via compose)
2. Export required environment variables

```bash
export DB_URL=jdbc:postgresql://localhost:5432/flight_price_monitor
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export AMADEUS_API_KEY=your_api_key
export AMADEUS_API_SECRET=your_api_secret
```

3. Run the application

```bash
./mvnw spring-boot:run
```

## Environment Variables

| Variable             | Description                                | Example                                                  |
| -------------------- | ------------------------------------------ | -------------------------------------------------------- |
| `DB_URL`             | PostgreSQL JDBC URL                        | `jdbc:postgresql://localhost:5432/flight_price_monitor` |
| `DB_USERNAME`        | PostgreSQL username                        | `postgres`                                               |
| `DB_PASSWORD`        | PostgreSQL password                        | `postgres`                                               |
| `AMADEUS_API_KEY`    | Amadeus API client ID                      | `your_api_key`                                           |
| `AMADEUS_API_SECRET` | Amadeus API client secret                  | `your_api_secret`                                        |

## Runtime Configuration

Core application properties:

| Property                                              | Description                                              | Default              |
| ----------------------------------------------------- | -------------------------------------------------------- | -------------------- |
| `scheduler.price-fetch.interval-ms`                   | Price fetch interval                                     | `21600000` (6h)      |
| `anomaly.min-samples`                                 | Minimum samples required before anomaly evaluation       | `5`                  |
| `anomaly.z-score-threshold`                           | Z-score threshold for anomaly                            | `2.0`                |
| `anomaly.percentage-threshold`                        | Percentage threshold for anomaly                         | `0.7`                |
| `amadeus.resilience.retry-max-attempts`               | Retry attempts for Amadeus calls                         | `3`                  |
| `amadeus.resilience.retry-wait-duration-ms`           | Delay between retries                                    | `200`                |
| `amadeus.resilience.circuit-breaker-sliding-window-size` | Circuit breaker sliding window                        | `20`                 |
| `amadeus.resilience.circuit-breaker-minimum-number-of-calls` | Minimum calls before breaker can open             | `10`                 |
| `amadeus.resilience.circuit-breaker-failure-rate-threshold` | Failure rate threshold (%)                          | `50`                 |
| `amadeus.resilience.circuit-breaker-wait-duration-ms` | Open-state wait duration                                 | `10000`              |
| `amadeus.resilience.timeout-ms`                       | Request timeout budget                                   | `3000`               |
| `amadeus.resilience.fallback-max-age-minutes`         | Max age of cached fallback price                         | `30`                 |

## API Documentation

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

## API Endpoints

### Routes

| Method | Endpoint                  | Description                               |
| ------ | ------------------------- | ----------------------------------------- |
| POST   | `/routes`                 | Create a new monitored route              |
| GET    | `/routes`                 | List all routes                           |
| GET    | `/routes/{id}`            | Get route details                         |
| DELETE | `/routes/{id}`            | Soft-delete route (deactivate monitoring) |
| GET    | `/routes/{id}/prices`     | Get route price history                   |
| GET    | `/routes/{id}/statistics` | Get route price statistics                |

### Deals

| Method | Endpoint | Description                    |
| ------ | -------- | ------------------------------ |
| GET    | `/deals` | Get current detected flight deals |

## Anomaly Detection Overview

The system uses two complementary checks:

1. **Z-Score Analysis** - Current price is compared to historical distribution
2. **Percentage Threshold** - Current price must be below a configurable percentage of historical mean

An anomaly is flagged when either condition is met and enough historical samples are available.

## Integration Test Scope

Current integration tests validate key API and persistence flows against real PostgreSQL (Testcontainers), including:

- Route creation and lifecycle (including deactivation)
- Deals and statistics behavior on persisted snapshots
- Duplicate route conflict handling

## License

This project is for educational and portfolio purposes.

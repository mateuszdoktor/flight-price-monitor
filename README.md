# Flight Price Monitor

A RESTful flight price anomaly detection system built with Spring Boot. The application monitors flight prices via the Amadeus API, detects anomalously cheap fares using statistical analysis (z-score), and exposes a REST API for route management, price history, statistics, and deal discovery.

## Features

- **Route Management** — Create, list, and deactivate monitored flight routes
- **Automated Price Tracking** — Scheduled fetching of lowest flight prices from Amadeus API (every 6 hours)
- **Anomaly Detection** — Identifies unusually cheap prices using z-score analysis and percentage-based thresholds
- **Price Statistics** — Mean, median, standard deviation, min/max, and z-score for each route
- **Deal Discovery** — Aggregated view of current price anomalies across all monitored routes
- **OAuth2 Token Management** — Automatic token caching and refresh for Amadeus API

## Tech Stack

### Backend

- **Java 25** with **Spring Boot 4.0**
- **Spring Data JPA** with Hibernate & PostgreSQL
- **Spring WebFlux** (`WebClient`) for non-blocking Amadeus API integration
- **Flyway** for database migrations

### Key Features & Libraries

- **Bean Validation** for input validation
- **Lombok** for boilerplate reduction
- **JPA Auditing** for automatic timestamp management

### Architecture & Patterns

- Layered architecture (Controller → Service → Repository)
- Domain model with pure Java records (no framework dependencies)
- DTO pattern with separate request/response models
- Utility-class based statistical analysis (`AnomalyDetector`)
- Centralized exception handling with custom error responses
- Scheduled background price fetching with `@Scheduled`
- OAuth2 client credentials flow with token caching

### Testing

- **JUnit 5** with **Mockito**
- **MockWebServer** for Amadeus API integration tests
- `@WebMvcTest` controller tests with validation
- Unit tests for domain logic and application services

## Getting Started

### Prerequisites

- Java 25+
- PostgreSQL

### Running the Application

1. Clone the repository

```bash
git clone https://github.com/yourusername/flight-price-monitor.git
cd flight-price-monitor
```

2. Start a PostgreSQL instance and create a database

3. Set environment variables and run the application

```bash
export DB_URL=jdbc:postgresql://localhost:5432/flight_price_monitor
export AMADEUS_API_KEY=your_api_key
export AMADEUS_API_SECRET=your_api_secret

./mvnw spring-boot:run
```

### Environment Variables

| Variable             | Description                    | Default |
| -------------------- | ------------------------------ | ------- |
| `DB_URL`             | PostgreSQL JDBC connection URL | —       |
| `AMADEUS_API_KEY`    | Amadeus API client ID          | —       |
| `AMADEUS_API_SECRET` | Amadeus API client secret      | —       |

### Configuration Properties

| Property                            | Description                                        | Default              |
| ----------------------------------- | -------------------------------------------------- | -------------------- |
| `scheduler.price-fetch.interval-ms` | Price fetch interval in milliseconds               | `21600000` (6 hours) |
| `anomaly.min-samples`               | Minimum snapshots required for anomaly analysis    | `5`                  |
| `anomaly.z-score-threshold`         | Z-score threshold (anomaly if z < -threshold)      | `2.0`                |
| `anomaly.percentage-threshold`      | Percentage threshold (anomaly if price ≤ mean × t) | `0.7`                |

## API Endpoints

### Routes

| Method | Endpoint                  | Description                      |
| ------ | ------------------------- | -------------------------------- |
| POST   | `/routes`                 | Create a new monitored route     |
| GET    | `/routes`                 | List all routes                  |
| GET    | `/routes/{id}`            | Get route details                |
| DELETE | `/routes/{id}`            | Deactivate a route               |
| GET    | `/routes/{id}/prices`     | Get price history for a route    |
| GET    | `/routes/{id}/statistics` | Get price statistics for a route |

### Deals

| Method | Endpoint | Description                                  |
| ------ | -------- | -------------------------------------------- |
| GET    | `/deals` | Get current deals (anomalously cheap prices) |

## Anomaly Detection

The system uses two complementary methods to detect price anomalies:

1. **Z-Score Analysis** — Flags prices where the z-score falls below `-threshold` (default: -2.0), indicating the price is more than 2 standard deviations below the mean
2. **Percentage Drop** — Flags prices that are at or below a percentage of the historical mean (default: 70%)

Both methods require a minimum number of price snapshots (default: 5) before analysis begins.

## Data Flow

```
User                          Scheduler (@Scheduled)
 │                                   │
 ▼                                   ▼
RouteController              PriceFetchScheduler
 │                                   │
 ▼                                   ▼
RouteService              PriceMonitoringService
 │                            │              │
 ▼                            ▼              ▼
RouteRepository         AmadeusClient   PriceSnapshotRepository
                              │              │
DealController ──► AnomalyDetectionService ──► GET /deals
```

## Planned Features

- Full-flow integration test with Testcontainers (PostgreSQL) and MockWebServer
- `Dockerfile` (multi-stage) and `docker-compose.yml` (app + PostgreSQL)
- Redis caching for OAuth tokens and statistics results
- Resilience4j retry and circuit breaker on Amadeus API calls
- Per-route anomaly threshold configuration
- Email alerts on anomaly detection
- Moving average (7-day window) as alternative to simple mean
- SpringDoc OpenAPI / Swagger UI documentation

## License

This project is for educational and portfolio purposes.

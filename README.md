<div align="center">

# Flight Price Monitor

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=flat&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)

*A resilient REST API for automated flight price tracking, anomaly detection, and deal discovery.*

</div>

## About The Project

This repository contains a RESTful backend service built to continuously monitor flight prices via the **Amadeus API**. By archiving historical pricing data on scheduled intervals, the system calculates statistical anomalies to immediately identify and flag unusually cheap flights.

The project demonstrates a robust, enterprise-grade architecture using **Spring Boot 4.0**. It heavily emphasizes domain-driven design principles, external API resilience (circuit breakers, fallbacks), and comprehensive automated testing.

### Key Highlights
* **Hexagonal Architecture:** Strict decoupling of the REST API (adapters), external Amadeus integration (infrastructure), and core detection logic (domain).
* **Intelligent Polling:** Automated Spring schedulers that fetch fresh market snapshots without manual intervention.
* **Resilience Patterns:** OAuth token caching, exponential retries, and circuit breakers for external API calls.
* **Reliable Persistence:** PostgreSQL database managed by Flyway migrations to ensure schema consistency alongside code.

---

## API Reference

The service exposes a clean RESTful interface for managing routes and discovering deals. Full API documentation is available via **Swagger/OpenAPI** after application startup.

### Core Endpoints

| Method | Endpoint | Description |
|:---|:---|:---|
| `GET` | `/api/v1/routes` | View all actively monitored flight routes. |
| `POST` | `/api/v1/routes` | Add a new origin/destination pair to the monitoring schedule. |
| `DELETE` | `/api/v1/routes/{id}` | Deactivate monitoring for a specific route. |
| `GET` | `/api/v1/deals` | Retrieve a list of currently detected price anomalies (low prices). |
| `GET` | `/api/v1/statistics/{routeId}` | Get mean, median, standard deviation, and historical price data. |

<details>
<summary><b>Click to see Swagger UI information</b></summary>
<br>

Once the application is running, the interactive API documentation is automatically generated.
You can explore all endpoints, request/response schemas, and test them directly in your browser at: 
`http://localhost:8080/swagger-ui.html`

</details>

---

## Tech Stack

* **Core:** Java 25, Spring Boot 4.0
* **Data Layer:** PostgreSQL 16, Flyway, Spring Data JPA
* **Integration:** Spring WebClient (Amadeus API)
* **Testing:** JUnit 5, Mockito, Testcontainers
* **DevOps:** Docker, Docker Compose, Maven Wrapper

---

## Project Structure

```text
flight-price-monitor/
├── src/main/java/com/flight_price_monitor/
│   ├── api/             # REST Controllers, DTOs, Exception Handlers
│   ├── application/     # Core Use Cases, Command/Query handlers
│   ├── domain/          # Business entities, Anomaly detection algorithms
│   ├── infrastructure/  # Amadeus WebClients, Circuit Breakers, External Adapters
│   ├── persistence/     # JPA Entities, Repositories, Database Mappers
│   └── scheduler/       # Cron jobs for background data polling
├── src/main/resources/
│   └── db/migration/    # Flyway SQL migrations (V1, V2...)
├── docker-compose.yml   # Infrastructure provisioning (PostgreSQL)
└── pom.xml              # Project dependencies and build config
```

---

## Getting Started

### Prerequisites
* **Java 25** installed on your local machine.
* **Docker & Docker Compose** for running the database locally.
* **Amadeus API Credentials** (Client ID and Secret).

### Installation & Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/flight-price-monitor.git
   cd flight-price-monitor
   ```

2. Start the local PostgreSQL database:
   ```bash
   docker compose up -d postgres
   ```

3. Configure your API keys in `src/main/resources/application.properties` or via environment variables:
   ```properties
   amadeus.api.key=YOUR_API_KEY
   amadeus.api.secret=YOUR_API_SECRET
   ```

### Usage

**Run the REST API:**
Use the provided Maven wrapper to launch the application:
```bash
./mvnw spring-boot:run
```
*Flyway will automatically apply database migrations on startup. Schedulers will begin polling Amadeus based on active routes.*

**Run Integration Tests:**
Execute the test suite (Testcontainers will automatically spin up a temporary database to verify API & persistence integrity):
```bash
./mvnw clean test
```

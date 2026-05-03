# oligarch-rating

A production-grade Spring Boot 3 (Java 21) microservice that rates a person against the world oligarch threshold by orchestrating two upstream services and persisting confirmed oligarchs.

## Architecture

```
client ─▶ POST /api/v1/oligarch-ratings
            │
            ▼
   OligarchRatingController ─▶ OligarchRatingService
                                  │
              ┌───────────────────┼─────────────────────────┐
              ▼                   ▼                         ▼
   AssetsValuationClient   OligarchHelperClient        OligarchRepository
     (cash/evaluate,         (oligarch-threshold)      (Postgres / H2)
      bitcoin/value)
```

* **Layering** – controller / service / client / repository, with DTOs at the edges.
* **Resilience** – Resilience4j retry on every upstream call.
* **Observability** – Spring Boot Actuator with liveness/readiness probes and Prometheus metrics.
* **Validation** – Bean Validation on the request payload, structured error responses.
* **Documentation** – OpenAPI 3 spec at `/v3/api-docs`, Swagger UI at `/swagger-ui.html`.
* **Persistence** – JPA. H2 in-memory by default, Postgres ready (used in `docker-compose`).

## API

### `POST /api/v1/oligarch-ratings`

Rates a person and, if their assets value exceeds the oligarch threshold, persists them.

Request:
```json
{
  "id": 123456789,
  "personInformation": { "firstName": "Bill", "lastName": "Gates" },
  "financialAssets": {
    "cashAmount": 16000000000,
    "currency": "ILS",
    "bitcoinAmount": 50
  }
}
```

Response (200):
```json
{
  "id": 123456789,
  "firstName": "Bill",
  "lastName": "Gates",
  "assetsValue": 4003000000.00,
  "oligarch": true
}
```

Status codes:
* `200 OK` – rating computed (oligarch flag indicates outcome).
* `400 Bad Request` – validation / payload error (returns structured `ApiError`).
* `502 Bad Gateway` – upstream service responded with an error.
* `504 Gateway Timeout` – upstream service was unreachable.

`assetsValue = cashInUsd + bitcoinAmount * bitcoinValueInUsd`, computed via the upstream `assets-valuation` service. The threshold is fetched from `oligarch-helper`.

## Running locally

### With Docker Compose (recommended)

Spins up Postgres, two WireMock-backed upstream stubs, and the service:

```bash
docker compose up --build
```

The service is on `http://localhost:8080`; Swagger UI on `http://localhost:8080/swagger-ui.html`.

Try it:
```bash
curl -X POST http://localhost:8080/api/v1/oligarch-ratings \
  -H 'Content-Type: application/json' \
  -d '{
    "id": 123456789,
    "personInformation": { "firstName": "Bill", "lastName": "Gates" },
    "financialAssets": { "cashAmount": 16000000000, "currency": "ILS", "bitcoinAmount": 50 }
  }'
```

### Without Docker

```bash
mvn spring-boot:run
```

By default the service runs against H2 and points at `http://assets-valuation:8080` and `http://oligarch-helper:8080`. Override with env vars:

```bash
ASSETS_VALUATION_URL=http://localhost:9081 \
OLIGARCH_HELPER_URL=http://localhost:9082 \
mvn spring-boot:run
```

## Testing

```bash
mvn test
```

Three layers of tests are in place:

| Layer | Location | Tooling |
| --- | --- | --- |
| **Unit** | `src/test/java/.../service/*Test.java` | JUnit 5 + Mockito (pure, no Spring context) |
| **Component / slice** | `src/test/java/.../api/OligarchRatingControllerTest.java`, `src/test/java/.../client/*ClientTest.java`, `src/test/java/.../repository/OligarchRepositoryTest.java` | `@WebMvcTest`, `@DataJpaTest`, WireMock-backed clients |
| **Integration** | `src/test/java/.../OligarchRatingIntegrationTest.java` | `@SpringBootTest` with `RANDOM_PORT`, WireMock upstreams, H2 persistence |

## Configuration

| Property | Env var | Default |
| --- | --- | --- |
| `server.port` | `SERVER_PORT` | `8080` |
| `spring.datasource.url` | `DATASOURCE_URL` | embedded H2 |
| `oligarch-rating.external.assets-valuation.base-url` | `ASSETS_VALUATION_URL` | `http://assets-valuation:8080` |
| `oligarch-rating.external.oligarch-helper.base-url` | `OLIGARCH_HELPER_URL` | `http://oligarch-helper:8080` |

Resilience4j retry settings live under `resilience4j.retry.instances.{assets-valuation,oligarch-helper}`.

## Operational endpoints

* `GET /actuator/health` – aggregated health
* `GET /actuator/health/liveness` – liveness probe
* `GET /actuator/health/readiness` – readiness probe
* `GET /actuator/prometheus` – Prometheus metrics
* `GET /v3/api-docs` – OpenAPI spec
* `GET /swagger-ui.html` – Swagger UI

# OrderBridge

A B2B order management REST API built with **Spring Boot 4.1** and **Java 25**. Every endpoint speaks
**both JSON and XML**: partners place orders, suppliers push catalog feeds, and admins manage the
catalog and the order lifecycle.

Many B2B integrations still exchange XML, while newer clients use JSON. This service handles both
from one set of endpoints. It also uses RFC 9457 problem details for errors, live currency
conversion through a declarative HTTP client, and atomic stock reservation backed by Postgres.

## Highlights

| Area | What's implemented | Where |
|---|---|---|
| **REST design** | Resource-oriented URLs; correct verbs and status codes (`201` + `Location`, `204`, `409`, `415`, `422`, `503`); paging; `PATCH` for state transitions | `catalog/ProductController`, `orders/OrderController` |
| **JSON + XML** | Content negotiation through the `Accept` header or `?format=xml`. Request bodies can be JSON or XML. XML uses attributes (`<product sku="...">`), wrapped lists (`<lines><line>`) and unwrapped lists | `*Response`, `ProductPage`, `OrderRequest` |
| **XML ingestion** | Bulk supplier feed (`<catalogFeed supplier="..">`) that only accepts `application/xml`, upserted in a single transaction | `catalog/CatalogFeed`, `ProductService#importFeed` |
| **Error contract** | RFC 9457 `ProblemDetail`, returned as `application/problem+json` or `application/problem+xml` to match what the client accepts. Includes per-field validation errors and business-rule violations | `web/ApiExceptionHandler` |
| **Consuming REST** | Declarative `@HttpExchange` client that reads a live JSON exchange-rate API, with timeouts and upstream failures mapped to `503` | `fx/ExchangeRateClient`, `fx/FxConfiguration` |
| **Modern Java** | Records for entities and DTOs, a sealed interface with exhaustive pattern-matching `switch` (`StockCheck`), switch expressions, text blocks | `orders/StockCheck`, `orders/OrderService` |
| **Persistence** | Spring Data JDBC aggregates (order → order lines, one-to-many), derived queries, optimistic locking with `@Version`, Flyway migrations, PostgreSQL | `orders/PurchaseOrder`, `db/migration` |
| **Consistency** | An order reserves stock for all of its lines or none. A rejected order rolls back. Cancelling returns stock | `OrderService#place`, `#changeStatus` |
| **Events** | `OrderPlaced` application event, handled after commit by a `@TransactionalEventListener` (low-stock monitor) | `orders/LowStockMonitor` |
| **Security** | Stateless HTTP Basic with bcrypt hashing through a delegating `PasswordEncoder`; role-based rules (public reads, `PARTNER` orders, `ADMIN` writes) | `security/SecurityConfiguration` |
| **Production** | Actuator health/info/metrics (actuator restricted to admins), OpenAPI/Swagger UI, multi-stage layered Docker image, Docker Compose | `Dockerfile`, `compose.yaml` |
| **Testing** | 28 tests: unit, `@WebMvcTest` slices (negotiation, validation, security), `MockRestServiceServer` for the HTTP client, and Testcontainers integration tests against real Postgres | `src/test` |

## Running it

Prerequisites: JDK 25 and Docker.

```bash
./mvnw spring-boot:run
```

Spring Boot's Docker Compose support starts Postgres from `compose.yaml` automatically, and Flyway
creates the schema and seed data. The API runs on http://localhost:8080.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

To run the whole stack in containers instead:

```bash
docker compose --profile app up --build
```

Demo users (override with `ORDERBRIDGE_ADMIN_PASSWORD` / `ORDERBRIDGE_PARTNER_PASSWORD`):

| User | Password | Role |
|---|---|---|
| `admin` | `admin-secret` | manage catalog, ship/cancel orders, actuator |
| `partner` | `partner-secret` | place and view orders |

## API tour

`requests.http` has ready-to-run requests for every endpoint. A few examples:

**Same resource, two representations**

```bash
curl localhost:8080/api/products/KB-100
# {"sku":"KB-100","name":"Mechanical Keyboard","category":"peripherals","price":89.90,"currency":"EUR","stock":40,...}

curl -H 'Accept: application/xml' localhost:8080/api/products/KB-100
# <product sku="KB-100"><name>Mechanical Keyboard</name>...<price>89.90</price><currency>EUR</currency><stock>40</stock></product>
```

**Place an order in XML**

```bash
curl -u partner:partner-secret -H 'Content-Type: application/xml' -H 'Accept: application/xml' \
  -d '<order><customerReference>ACME-42</customerReference>
        <lines><line><sku>KB-100</sku><quantity>2</quantity></line></lines></order>' \
  localhost:8080/api/orders
# HTTP 201, Location: /api/orders/1
# <order id="1"><customerReference>ACME-42</customerReference><status>PLACED</status><total>179.80</total>...
```

**Errors follow the client's format**

```bash
curl -H 'Accept: application/xml' localhost:8080/api/products/NOPE
# HTTP 404, Content-Type: application/problem+xml
# <problem xmlns="urn:ietf:rfc:7807"><detail>Product 'NOPE' was not found</detail>...<status>404</status>...</problem>
```

```bash
curl -u partner:partner-secret -H 'Content-Type: application/json' \
  -d '{"customerReference":"ACME-42","lines":[{"sku":"MN-270","quantity":999},{"sku":"NOPE","quantity":1}]}' \
  localhost:8080/api/orders
# HTTP 422
# {"title":"Order rejected","status":422,
#  "violations":["Insufficient stock for MN-270: requested 999, available 15","Unknown product NOPE"],...}
```

### Endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/products?category=&page=&size=` | public | paged, sorted by SKU |
| `GET` | `/api/products/{sku}` | public | |
| `GET` | `/api/products/{sku}/price?currency=USD` | public | live FX conversion |
| `POST` | `/api/products` | admin | JSON or XML body |
| `PUT` | `/api/products/{sku}` | admin | full replace |
| `DELETE` | `/api/products/{sku}` | admin | |
| `POST` | `/api/products/import` | admin | XML supplier feed only |
| `POST` | `/api/orders` | partner/admin | JSON or XML body; reserves stock |
| `GET` | `/api/orders?customerReference=` | partner/admin | |
| `GET` | `/api/orders/{id}` | partner/admin | |
| `PATCH` | `/api/orders/{id}/status` | admin | `PLACED → SHIPPED → DELIVERED`, or `PLACED → CANCELLED` |

## Tests

```bash
./mvnw verify   # needs Docker for the Testcontainers integration tests
```

## Design notes

- **Separate DTOs from entities.** API records (`ProductResponse`, `OrderRequest`, …) control the
  JSON and XML shape on their own, so the persistence model can change without breaking clients.
- **XML with records.** jackson-dataformat-xml can't bind a renamed, wrapped list
  (`<lines><line>`) through a record's constructor. `OrderRequest` declares an explicit
  `@JsonCreator` so the XML shape and the JSON shape (`"lines": [...]`) both work.
- **Error media type.** Spring only picks `application/problem+xml` when a client asks for it
  explicitly. Clients that send `Accept: application/xml` also expect XML errors, so
  `ApiExceptionHandler` chooses the problem media type from the client's preference.
- **Stock consistency.** Each requested line is checked into a sealed `StockCheck`
  (`Available | Insufficient | UnknownSku`). Any failure throws, which rolls back the
  transaction, and `@Version` on `Product` turns concurrent oversells into a `409` instead of
  silently losing updates.

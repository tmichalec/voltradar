# VoltRadar

VoltRadar is an electric vehicle (EV) charging discovery and cost optimization platform. It normalizes fragmented, complex charging tariffs into the user's **effective charging price** based on their active subscriptions, prepaid kWh packages, vehicle charging curve, and current charging needs.

---

## Key Features

- **Effective Price Calculation**: Transforms complex pricing structures (AC/DC tiers, per-minute idle/overstay fees, monthly prepaid quotas, roaming rates) into an accurate, personalized cost estimate.
- **Dual Optimization Modes**:
  - ⚡ **Fast Mode**: Prioritizes minimum transit delay (high charging power + live socket availability + cost transparency).
  - 💰 **Cheap Mode**: Prioritizes maximum financial savings per delivered kWh (lowest effective price + baseline availability).
- **Live Socket Availability**: Real-time connector status caching to avoid wasted trips to occupied or out-of-order chargers.
- **Provider-Agnostic Core**: Starting with **ZSE Drive** (Slovakia) as the initial ecosystem, designed with clean abstractions to support additional CPOs (GreenWay, Ionity, e-hub roaming) seamlessly.
- **Multi-Client Support**:
  - **Web Application** (Vite + React + Tailwind CSS) for instant testing and browser access.
  - **Android & Android Auto** (native Java/Kotlin via `androidx.car.app`) for in-car heads-up navigation and one-tap selection (e.g., in Skoda Enyaq and compatible vehicles).
- **Self-Hosted / Umbrel Ready**: Optimized for deployment on home servers (UmbrelOS 2.0 / Docker Compose) with remote access via Tailscale VPN or Cloudflare Tunnels.

---

## Architecture Overview

```
+-------------------------------------------------------------------------------+
|                                  VoltRadar                                    |
+-------------------------------------------------------------------------------+
| Clients:                                                                      |
|   - Web App (React / Vite / Tailwind)                                         |
|   - Mobile & Android Auto App (androidx.car.app / In-Car Display)             |
+-------------------------------------------------------------------------------+
                                      |
                                      | REST / JSON APIs
                                      v
+-------------------------------------------------------------------------------+
| Backend (Java 25 + Spring Boot 4 + Virtual Threads):                          |
|   - Optimization & Recommendation Engine (Fast vs. Cheap)                     |
|   - Tariff & Effective Price Calculation Engine                               |
|   - Ingestion Scheduler & CPO Adapters (ZSE Drive)                            |
+-------------------------------------------------------------------------------+
                 |                                              |
                 v                                              v
+--------------------------------+             +--------------------------------+
| PostgreSQL + PostGIS           |             | Redis 7 (In-Memory RAM)        |
| - Spatial queries (ST_DWithin) |             | - Live connector status cache  |
| - Stations, connectors, rules  |             | - Rapid TTL state eviction     |
| - User profiles & packages     |             | - Fast API response caching    |
+--------------------------------+             +--------------------------------+
```

---

## Tech Stack

- **Backend**: Java 25 (Records, Pattern Matching, Virtual Threads), Spring Boot 4.1.x, Spring Data JPA, Hibernate Spatial
- **Database**: PostgreSQL 16 + PostGIS 3.4
- **Cache**: Redis 7 Alpine
- **Build Tool**: Apache Maven
- **Frontend (Web)**: Vite, React, TypeScript, Tailwind CSS, MapLibre GL
- **Mobile / In-Car**: Android App with Android Auto (`androidx.car.app`) support
- **Infrastructure**: Docker Compose (UmbrelOS / Local Linux / macOS)

---

## Getting Started

### Prerequisites

- **Java 25** (JDK 25)
- **Maven 3.9+**
- **Docker & Docker Compose**

### 1. Start Infrastructure (PostGIS + Redis)

Use the simple Docker Compose script in `.docker/` (supports any `docker compose` arguments and custom project name):

```bash
# Start infrastructure in background (default project: voltradar)
./.docker/docker-compose.sh

# Stop infrastructure
./.docker/docker-compose.sh down

# Check status or follow logs
./.docker/docker-compose.sh ps
./.docker/docker-compose.sh logs -f

# Custom project name (if running multiple instances)
COMPOSE_PROJECT_NAME=my-voltradar ./.docker/docker-compose.sh
```

This starts:
- **PostgreSQL / PostGIS** on port `5432` (database: `voltradar`, user: `voltradar`, password: `voltradar_secret`)
- **Redis** on port `6379`

### 2. Build and Run Backend

**In IntelliJ IDEA:**
- Select and run the preconfigured **`VoltRadarApplication`** Run Configuration (or click the Run icon next to `VoltRadarApplication.java`).

**Via Terminal / Maven:**
```bash
cd backend
mvn clean spring-boot:run
```

The backend starts at `http://localhost:8080`.

### 3. API Documentation & Interactive Swagger UI

When the backend starts, OpenAPI and Swagger UI URLs are automatically logged to the console:
- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI v3 JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## Project Structure

The ZSE Drive client, observed JSON formats, nearby queries and synchronization constraints are documented in
[ZSE Drive API investigation](docs/zse-drive-api.md).

```text
voltradar/
├── .docker/                 # Docker Compose infrastructure & helper scripts
│   ├── docker-compose.yml   # PostGIS & Redis service definitions
│   └── docker-compose.sh    # Simple management script with project name support
├── .github/                 # CI/CD workflows
├── backend/                 # Java 25 + Spring Boot backend
│   ├── src/main/java/sk/brutech/voltradar/
│   │   ├── domain/          # Domain models (Station, Connector, Tariff, Mode)
│   │   ├── calculation/     # Effective price calculation engine
│   │   ├── ingestion/       # CPO data ingestion adapters (ZSE Drive)
│   │   ├── repository/      # Spring Data JPA / Spatial repositories
│   │   └── api/             # REST controllers & DTOs
│   ├── src/main/resources/  # application.yml configuration
│   └── pom.xml              # Maven dependencies and build plugins
├── agent.md                 # Agent guidelines, domain definitions, and conventions
└── README.md                # Main documentation
```

---

## Deployment on Umbrel

1. Clone or copy this repository to your Umbrel server.
2. Ensure Docker and Docker Compose are active on UmbrelOS.
3. Start the containers using `docker compose up -d`.
4. Access securely on mobile / Android Auto using **Tailscale** or configure a **Cloudflare Tunnel**.

# VoltRadar — Agent & Developer Guidelines

This document provides system instructions, domain architecture definitions, coding conventions, and workflow rules for AI agents and developers working on the **VoltRadar** codebase.

---

## 1. Project Vision & Core Problem

VoltRadar is an EV charging optimization engine designed to answer:
> *"Given my tariffs, active subscriptions, and car charging profile, what are my best charging options right now?"*

### Problem Space
CPOs (Charge Point Operators) present fragmented, opaque pricing structures. The nominal price per kWh rarely equals the user's actual out-of-pocket cost due to:
- Monthly subscription tiers with bundled / prepaid kWh.
- Variable rates by charging power (AC, DC <= 50kW, Ultra-fast DC > 150kW).
- Per-minute overstay / idle parking penalties after grace periods.
- Roaming surcharges and third-party network markups.

VoltRadar normalizes all variables to calculate the user's **effective price per kWh** and total session cost.

---

## 2. Core Domain Concepts & Entities

Agents must adhere to the following domain modeling rules:

### A. Optimization Modes (`OptimizationMode`)
- `FAST`: Prioritizes minimum total transit and charging delay. Stations ranked by highest available charging power (kW) and live socket availability, with secondary weighting on price.
- `CHEAP`: Prioritizes financial savings per kWh. Stations ranked by lowest net effective session cost, with power as secondary.

### B. Core Entities
- **`ChargingStation`**: Physical station location (`Point` in PostGIS / WGS84 coordinates), operator identifier, address, and list of EVSEs.
- **`Connector` / `EVSE`**: Individual charging plug characterized by `ConnectorType` (`AC_TYPE_2`, `DC_CCS`, `DC_CHADEMO`), maximum power output (`maxPowerKw`), and real-time status.
- **`ConnectorStatus`**: Live availability state (`AVAILABLE`, `OCCUPIED`, `OUT_OF_ORDER`, `UNKNOWN`). Kept in Redis for ultra-fast updates and low database load.
- **`TariffPlan`**: Pricing rules including base rate per kWh (by AC/DC tier), included prepaid kWh package balance, time limits before per-minute billing begins, and penalty rates.
- **`UserContract`**: The user's active billing profile, subscription tier, remaining prepaid kWh, and billing cycle renewal date.
- **`VehicleProfile`**: Battery capacity (kWh), maximum AC/DC acceptance rate (kW), and charging curve profile (e.g., Skoda Enyaq 80 / 85).
- **`CalculationResult`**: Normalized effective cost per kWh, estimated total cost (€), estimated charging duration (minutes), and breakdown of energy vs. time fees.

---

## 3. Technology Stack & Architectural Principles

### Backend
- **Language**: Java 25.
  - Utilize modern Java features: `records`, `sealed classes/interfaces`, enhanced `switch` expressions with pattern matching, and **Virtual Threads** (`Project Loom`) for high-throughput I/O and scraping.
- **Framework**: Spring Boot 4.1.x with Spring Data JPA and Hibernate Spatial.
- **Database**: PostgreSQL 16 with PostGIS extension for spatial queries (e.g., `ST_DWithin`, spatial indexes `GiST`).
- **Cache**: Redis 7 for high-frequency live connector statuses and TTL-managed transient states.
- **Build Tool**: Apache Maven (`pom.xml`).

### Clients & Integrations
- **Web**: React + Vite + TypeScript + Tailwind CSS.
- **Mobile & In-Car**: Android native module utilizing `androidx.car.app` for Android Auto.
- **Server Deployment**: Docker Compose on Umbrel / Linux server; remote access via Tailscale or Cloudflare Tunnels.

---

## 4. Coding & Project Standards

### Package & Versioning Conventions
- **Base Package**: `sk.brutech.voltradar`
- **Semantic Versioning (SemVer 2.0.0)**: Releases and modules adhere strictly to `MAJOR.MINOR.PATCH`:
  - `MAJOR`: Incompatible API changes, breaking architectural shifts, or major redesigns.
  - `MINOR`: New functionality added in a backwards-compatible manner.
  - `PATCH`: Backwards-compatible bug fixes and small maintenance patches.
  - Suffix `-SNAPSHOT` indicates active development before a release. Current base version: `0.1.0-SNAPSHOT`.

### Commit Message Conventions (Conventional Commits 1.0.0)
All commits must follow the **Conventional Commits** specification:
```text
<type>(<optional scope>): <description>

[optional body]

[optional footer(s)]
```
- **Allowed Types**:
  - `feat`: A new feature or domain capability (triggers SemVer MINOR).
  - `fix`: A bug fix (triggers SemVer PATCH).
  - `docs`: Documentation only changes (e.g., README.md, agent.md).
  - `style`: Changes that do not affect the meaning of code (formatting, whitespace, imports).
  - `refactor`: Code change that neither fixes a bug nor adds a feature.
  - `perf`: Performance improvements.
  - `test`: Adding or correcting tests.
  - `build`: Changes affecting build system, packaging, or external dependencies (Maven, Docker).
  - `ci`: Changes to CI/CD workflows and automation scripts.
  - `chore`: Repository tooling, auxiliary configs, or maintenance tasks.
- **Breaking Changes**: Denoted with `!` before the colon (e.g., `feat(tariff)!: rework pricing contract`) or a `BREAKING CHANGE:` footer (triggers SemVer MAJOR).
- **Rules**: Descriptions must be in **English**, concise, in the imperative mood, and conform to the 120-character line length limit.

### Language Rules
- **Codebase**: 100% in **English** (class names, variable names, method names, comments, commit messages, API specs, and technical documentation).
- **User Communication**: In **Slovak** (`sk`) as requested by the user, while keeping technical terms and code snippets cleanly formatted in English.

### Code Style & Quality
- **Line Length**: Maximum line length is **120 characters** (minor overflow is permitted only exceptionally when wrapping impairs readability).
- Favor immutable domain models and `record` types where appropriate.
- Keep domain calculation logic deterministic, pure, and thoroughly covered by unit tests.
- Avoid premature optimizations, but ensure spatial queries leverage PostGIS indexes rather than in-memory distance loops.
- Separate physical infrastructure data ingestion from the pricing calculation engine.

---

## 5. Development Workflow for Agents

1. **Investigate Before Editing**: Understand the existing domain models in `sk.brutech.voltradar.domain` before adding new entities.
2. **Deterministic Calculations**: Unit test all tariff calculations against known edge cases (depleted prepaid packages, overstay penalties, AC vs. DC rate changes).
3. **Keep CPO Ingestion Modular**: Keep provider-specific scrapers/clients (e.g., `ZseDriveClient`) isolated behind a generic provider interface (`CpoIngestionService`).
4. **Commitment & Version Control Workflow**: Commit changes after each logically complete functionality or feature. Remind the user when a cohesive unit of work is completed to keep commits clean, granular, and traceable.

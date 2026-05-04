<a id="readme-top"></a>

<div align="center">
  <h1>Syncturtle</h1>
  <h3>Open-source, self-hostable personal management platform</h3>

  <p>
    A production style full stack project focused on backend architecture,
    microservices, authentication, event-driven systems, observability,
    testing, and self-hosted deployment patterns.
  </p>

  <p>
    <a href="https://github.com/TwiceBoogie/syncturtle"><strong>Explore the project »</strong></a>
    ·
    <a href="https://github.com/TwiceBoogie/syncturtle/issues">Report Bug</a>
    ·
    <a href="https://github.com/TwiceBoogie/syncturtle/issues">Request Feature</a>
  </p>
</div>

> **Project status:** Syncturtle is a work in progress.
> The project is not finished or production ready and is actively being refactored (tend to change my mind a lot). Some modules are completed, some are experimental (no testing yet), and some are planned. The current recommended local development workflow is **hybrid local development**: run infrastructure with Docker Compose, then run backend services locally through Maven or an IDE using `.env` (vscode with recommended extensions).

---

## Table of Contents

- [About](#about)
- [Why I Built This](#why-built-this)
- [Current Status](#current-status)
- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Repository Structure]($repository-structure)
- [Local Development Model](#local-development-model)
- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [Configuration Server Requirements](#configuration-server-requirements)
- [Local Startup Order](#local-startup-order)
- [Running Infrastructure Locally](#running-infrastructure-locally)
- [Running Backend Services Locally](#running-backend-services-locally)
- [Running Frontend Apps Locally](#running-frontend-apps-locally)
- [Testing](#testing)
- [Observability](#observability)
- [Roadmap](#roadmap)
- [Notes for Recruiters](#notes-for-recruiters)
- [Known Limitations](#known-limitations)
- [Contributing](#contributing)
- [Contact](#contact)

---

## About

**Syncturtle** is an open-source, self-hostable personal management platform.

The long term goal is to build a modular "Life OS" style application where users can manage personal systems such as:

- tasks
- workspaces
- goals
- calendars
- passwords
- subscriptions
- notes
- user/admin settings
- email and authentication configurations

Syncturtle is built as a **polyglot monorepo** with:

- a **Next.js + TypeScript frontend**
- a **Java 21 + Spring Boot backend**
- shared frontend packages
- shared backend libraries
- infrastructure/platform services
- domain microservices
- local observability and system-level testing support

The product itself is still pretty early, but the engineering foundation is design to mimic/explore production style patterns.

---

## Why I Built This

I needed to show off what I know but at the same time practive and learn without being stuck in tutorial hell. I looked at multiple repos to see what I can get inspired and found this user (Volmarg) and thought that looks like a fun project. I also wanted to apply what I learn from a udemy course into my own project without relying too much on videos.

This project is designed to help me learn and demonstrate:

- backend architecture
- service boundaries
- API Gateway security
- distributed authentication
- event-driven communication
- database-per-service design
- Redis backed session state
- caching and cache invalidation
- database migrations
- testing layers
- observability
- deployment and self-hosting patterns

Teh goal is not only to build an app that works, but to understand how a larger system in a professional settings are organized, secured, tested, monitored, and evolved over time.

---

## Current Status

Syncturtle is currently under active development.

### Working / partially working areas

- backend multi-module Maven structure
- platform/service separation
- API Gateway foundation
- config server foundation
- discovery server foundation
- instance-service setup profile
- instance registration/configuration bootstrap
- user-service authentication foundation
- access token and refresh token design
- Redis backed refresh sessions
- JWT/JWKS validation through the gateway
- Kafka/Redpanda local messaging foundation
- response caching foundation
- OpenTelemetry local observability
- frontend admin app structure
- custom frontend store pattern
- frontend API service wrapper
- unit/integration/system test structure

### Planned / incomplete areas

- full product feature set
- complete OAuth providers
- complete password-service
- stronger production deployment setyp
- fully containerized servicse workflow
- AWS deployment proof of concept
- more system tests
- dashboards and alerts
- transaction outbox pattern on services
- expanded documentation and diagrams

---

## Architecture Overview

At a high level, Syncturtle follows this structure:

```text
Frontend Apps
    ↓
API Gateway
    ↓
Backend Domain Services
    ↓
PostgreSQL / Redis / Kafka / Email Infrastructure
```

The backend is organized around a production style microservice architecture.

```text
apps/backend
├── libs/               # Shared backend libraries
├── platform/           # Infrastructure/platform applications
├── services/           # Domain services
└── tests/system-tests  # Cross-service system tests
```

### Platform application

Platform applications support the system but do not own product domains.

```text
platform/
├── api-gateway
├── config-server
└── discovery-server
```

### Domain services

Domain services own business capabilities and data.

```text
services/
├── user-service
├── instance-service
├── workspace-service
├── email-service
└── password-service       # planned / in progress
```

---

### Backend configuration model

Most backend applications load their spring configurations from the **Config Server**.

The main exceptions are the foundational services that must start before the rest:

- `discovery-server`
- `config-server`

Because other services rely on `config-server`, it must be running before starting normal services or profile specific flows such as `instance-service` with the `setup` profile.

---

## Tech Stack

### Backend

- Java 21
- Spring Boot
- Spring Cloud Gateway
- Spring Cloud Config
- Spring Security
- Spring Data JPA
- Spring Data Redis
- OpenFeign
- Liquibase
- PostgreSQL
- Redis
- Kafka / Redpanda
- OpenTelemetry
- Maven
- JUnit 5
- Mockito
- Testcontainers

### Frontend

- TypeScript
- Next.js App Router
- React
- Tailwind CSS
- SWR
- custom class-based stores
- `useSyncExternalStore`
- shared workspace packages
- Turborepo/Yarn workspace-style monorepo tooling

### Infrastructure

- Docker
- Docker Compose
- PostgreSQL
- Redis
- Redpanda
- OpenTelemetry Collector
- Grafana LGTM
- GitHub Actions

---

## Repository Structure

> Exact structure may change while the project is being actively refactored.

```text
syncturtle
├── apps
│   ├── admin
│   ├── web
│   └── backend
│       ├── libs
│       ├── platform
│       ├── services
│       └── tests
├── packages
│   ├── constants
│   ├── types
│   ├── utils
│   ├── ui
│   └── typescript-config
└── deployments
```

Backend structure:

```text
apps/backend
├── libs
│   ├── common-core
│   ├── common-contracts
│   ├── common-data-jpa
│   ├── common-spring
│   ├── common-web
│   └── test-support
├── platform
│   ├── api-gateway
│   ├── config-server
│   └── discovery-server
├── services
│   ├── user-service
│   ├── instance-service
│   ├── workspace-service
│   └── email-service
└── tests
    └── system-tests
```

---

## Local Development Model

The current recommended local development model is:

```text
Docker Compose  = infrastructure only
Maven / IDE     = backend services
Yarn/Turbo      = frontend apps
.env            = local service configuration
```

Run PostgreSQL, Redis, Redpanda, OpenTelemetry, and Grafana through Docker Compose, then run Spring Boot services locally from your host machine.

---

## Prerequsites

Before running syncturtle locally, install the following tools.

### Required

| Tool     | Version        | Notes                                              |
| -------- | -------------- | -------------------------------------------------- |
| Node.js  | `>=20.9.0 <25` | More data                                          |
| Corepack |                | Used to activate the repo managed yarn version     |
| Yarn     | `4.13.0`       | managed through `packageManager` in `package.json` |
| Java     | `Java 21`      |                                                    |
| Docker   |                | Used for local infrastructure                      |

### Node and Yarn setup

This repository uses Yarn 4 through Corepack.

Enable Corepack:

```sh
corepack enable
```

Then from the repository root install frontend dependecies

```sh
yarn install
```

## Environment Setup

The backend uses a local `.env` file during development.

A safe starter template is provided at:

```text
apps/backend/.env.example
```

Copy it to `.env` before running the backend services:

```sh
cp apps/backend/.env.example apps/backend/.env
```

Then update any values in `apps/backend/.env` as needed for your local machine.

### Export `.env` values before running services

Before running backend services directly from your terminal, export the `.env` valus into your shell sessions.

From `apps/backend`:

```sh
cd apps/backend

set -a
source .env
set +a
```

Some spring configuration properties also provide safe local defaults. For example:

```yaml
${OTLP_TRACES_ENDPOINT:http://localhost:14317}
```

These defaults help the app start in local development, but I recoomend exporting `.env` so you can see all values that are being used

---

## Configuration Server Requirement

Most backend application read their `application-*.yaml` configuration through the `config-server`.

That means `config-server` must be running before starting services that depend on it, including setup profiles.

For example, `instance-service` with the `setup` profile loads setup specifc configuration through the `config-server`.
Therefore this will not work correctly unless the `config-server` is already running:

```sh
./mvnw -pl services/instance-service spring-boot:run \
  -Dspring-boot-run.profiles=setup
```

Start the foundational services first:

```text
1. discover-server
2. config-server
```

Then run setup profiles and normal services.

---

## Local Startup Order

Recommended startup order for local development:

```text
1. Copy apps/backend/.env.example to apps/backend/.env
2. Export apps/backend/.env into your terminal session
3. Start Docker Compose infrastructure
4. Start Discovery Server
5. Start Config Server
6. Run Instance Service once with the setup profile
7. Start API Gateway
8. Start domain services
9. Start frontend apps
```

The most important rule is:

> Run `config-server` before `instance-service` setup or any normal backend service that loads remote Spring configuration.

## Running Infrastructure Locally

From the repository root:

```sh
docker compose up -d
```

---

## Running Backend Services Locally

All commands in this section assumes you are in inside:

```sh
cd apps/backend
```

Export the local environment first:

```sh
set -a
source .env
set +a
```

### 1: Build backend modules

```sh
./mvnw clean verify
```

### 2. Start Discovery Server

Open a new terminal, export `.env`, then run:

```sh
./mvnw -pl platform/discover-server spring-boot:run
```

### 3. Start Config Server

Open a new terminal, export `.env`, then run:

```sh
./mvnw -pl platform/config-server spring-boot:run \
  -Dspring-boot.run.profiles=native
```

Wait until the Config Server is fully started before running setup or other services.

### 4. Run instance setup once

Before `instance-service` is used as a normal running service, it must first be run once with the `setup` profile.

```sh
./mvnw -pl services/instance-service spring-boot:run \
  -Dspring-boot.run.profiles=setup
```

The setup profile performs instance bootstrap work:

1. Registers or updates the local instance
2. Resolves or generates a machine signature
3. Seeds mandatory instance configuration keys
4. Creates derived flags for providers like Google, GitHub, GitLab, and Intercom
5. Exists after setup completes

The setup runner reads the machine signature from one of the following:

- `--machine-signature=...`
- `MACHINE_SIGNATURE` environment variable
- a generated persisted signature for first time setup

If you want to pass the signature manually:

```sh
./mvnw -pl services/instance-service spring-boot:run -am \
  -Dspring-boot.run.profiles=setup \
  -Dspring-boot.run.arguments="--machine-signature=local-dev"
```

### 5. Start API Gateway

Open a new terminal, expot `.env`, then run:

```sh
./mvnw -pl platform/api-gateway spring-boot:run -am
```

### 6. Start domain services

Open a new terminal per service, export `.env`, then run the services you need.

Instance Service:

```sh
./mvnw -pl services/instance-service spring-boot:run -am
```

User Service:

```bash
./mvnw -pl services/user-service spring-boot:run -am
```

Workspace Service:

```bash
./mvnw -pl services/workspace-service spring-boot:run -am
```

Email Service:

```bash
./mvnw -pl services/email-service spring-boot:run -am
```

---

## Running Frontend Apps Locally

From the repository root:

```sh
yarn install
```

Run the admin app:

```sh
yarn dev --filter=admin
```

Run the web app:

```sh
yarn dev --filter=web
```

Common local frontend URLs:

| App              | URL                     |
| ---------------- | ----------------------- |
| Web app          | `http://localhost:3000` |
| Admin app        | `http://localhost:3001` |
| Observability UI | `http://localhost:3002` |

---

## Testing

### Unit tests

```sh
cd apps/backend
./mvnw test
```

### Integration tests

```sh
cd apps/backend
./mvnw verify -Pit
```

### System tests

```sh
cd apps/backend
./mvnw verify -Psystem
```

### Test naming conventions

| Test Type              | Naming       |
| ---------------------- | ------------ |
| Unit test              | `*Test.java` |
| Integration test       | `*IT.java`   |
| End-to-end/system test | `*E2E.java`  |

---

## Observability

Local observability uses an OpenTelemetry Collector and Grafana LGTM.

The collector receives telemetry through OTLP and exports to the local observability backend.

Telemetry includes:

- traces
- metrics
- logs
- request correlation IDs
- request IDs
- trace IDs
- span IDs
- access logs

The collector also removes sensitive authorization header attributes before exporting telemetry.

---

## Roadmap

### Core platform

- [x] Multi-module backend structure
- [x] API Gateway foundation
- [x] Config Server foundation
- [x] Discovery Server foundation
- [x] Instance setup profile
- [x] Instance configuration bootstrap
- [x] JWT/JWKS access token validation
- [x] Redis backed refresh sessions
- [x] Response caching foundation
- [x] OpenTelemetry local observability
- [x] Unit/integration test structure
- [ ] Full transactional outbox pattern
- [ ] Expanded system test coverage
- [ ] Production deployment guide
- [ ] Refactored Docker service images
- [ ] AWS deployment proof of concept

## Authentication

- [x] Email/password foundation
- [x] Access tokens
- [x] Refresh token rotation
- [x] Admin session versioning
- [ ] Google login
- [ ] GitHub login
- [ ] GitLab login
- [ ] OIDC/SAML exploration

### Product features

- [x] Instance setup/admin foundation
- [x] Workspace foundation
- [x] Email configuration foundation
- [ ] Task management
- [ ] Calendar/events
- [ ] Password manager
- [ ] Subscriptions
- [ ] Goals
- [ ] Notes
- [ ] File assets

### Engineering improvements

- [ ] More diagrams
- [ ] Better local setup scripts
- [ ] More frontend tests
- [ ] More backend integration tests
- [ ] Alert examples
- [ ] Kubernetes deployment examples
- [ ] Security hardening checklist
- [ ] Fully documented Docker workflow

---

## Notes for Recruiters

Syncturtle is my primary portfolio project and is designed to demonstrate backend engineering in depth, beyond just a basic CURD app.

The project highlights experience with:

- Java 21
- Spring Boot
- Spring Cloud Gateway
- microservice architecture
- API Gateway security
- JWT/JWKS authentication
- refresh token/session design
- Redis backed session and cache state
- Kafka style event driven data replication design
- dead letter topic handling
- PostgreSQL schema ownership
- Liquibase migrations
- Maven multi-module organization
- OpenTelemetry observability
- Docker based local infrastructure
- frontend API/service architecture
- Next.js and Typescript
- unit, integration, and system testing strategy

> Imporant Note: Syncturtle is a work in progress portfolio project, not a real production system.

---

## Known Limitations

- The project is not finished
- Not all planned product modules are complete
- The docker service workflow is not finialized
- AWS deployment is planned but not implemented
- Transanctional outbox support is planned for stronger Kafka publishing guarantees
- More system tests and dashboards are planned
- OAuth providers are planned but not complete yet (at least on the server side)
- Documentation is still being improved

---

## Contributing

Contributions are welcome once the project stabilizes further. I would really appreciate if you include a comprehensive explanation of your changes.

For now, the best way to help are:

1. Open an issue for bugs or suggestions
2. Propose documentation improvements
3. Review architecture decisions
4. Suggest test cases
5. Create a pull request for small fixes

Basic contribution flow:

```sh
git checkout -b feature/my-change
git commit -m "Add my change"
git push origin feature/my-change
```

Then open a pull request.

---

**Salvador Sebastian**

- Email: `salsebastian13@gmail.com`
- GitHub: [TwiceBoogie](https://github.com/TwiceBoogie)
- LinkedIn: [salvador-sebastian](www.linkedin.com/in/salvador-sebastian-b0a58a169)

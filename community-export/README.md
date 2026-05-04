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
- [Backend Architecture](#backend-architecture)
- [Frontend Architecture](#frontend-architecture)
- [Key Engineering Concepts](#key-engineering-concepts)
- [Tech Stack](#tech-stack)
- [Repository Structure]($repository-structure)
- [Local Development Model](#local-development-model)
- [Environment Setup](#environment-setup)
- [Instance Setup Flow](#instance-setup-flow)
- [Running Infrastructure Locally](#running-infrastructure-locally)
- [Running Backend Services Locally](#running-backend-services-locally)
- [Running Frontend Apps Locally](#running-frontend-apps-locally)
- [Docker Status](#docker-status)
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

The long term gaol is to build a modular "Life OS" style application where users can manage personal systems such as:

- tasks
- workspaces
- goals
- calendars
- passwords
- subscriptions
- notes
- user/admin settings
- email and authentication configurations

Syncturtle is built as a \*polyglot monorepo\*\* with:

- a **Next.js + TypeScript frontend**
- a **Java 21 + Spring Boot backend**
- shared frontend packages
- shared backend libraries
- infrastructure/platform services
- domain microservices
- system-level testing support

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

The backend is organized around a production-style microservice architecture.

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

## Backend Architecture

The backend is a **Java 21 Spring Boot multi-module maven project**.

The root backend `pom.xml` manges:

- Java version
- Spring Boot parent
- Spring Cloud dependency management
- Testcontainers dependency managemnt
- internal module version
- Maven Surefire for unit tests
- Maven Failsafe for integration tests
- Maven Enforcer for Java/Maven version consistency

### Shared backend libraries

```text
libs/
├── common-core
├── common-contracts
├── common-data-jpa
├── common-spring
├── common-web
└── test-support
```

#### `common-core`

Shared low-level constants and utilities.

Examples:

- cookie names
- endpoint paths
- gateway/header names
- service client names
- base exception types
- text utilities

#### `common-contracts`

Shared service-to-service contracts.

Examples:

- API error response contracts
- authentication DTOs
- authorization request/response contracts
- refresh session records
- instance configuration keys
- Kafka topic names
- domain event contracts

This module is intentionally used for **communication contracts**, not service implementation details.

#### `common-data-jpa`

Reusable JPA support.

Examples:

- audited entity base classes
- soft delete support
- time auditing support
- UUID/time providers
- timezone validation

#### `common-spring`

Reusable Spring infrastructure.

Examples:

- global exception handling
- response cache annotations/aspects
- OpenTelemetry/logging auto-configuration
- CSRF support
- password hashing support
- public URL building
- model mapping helpers
- gateway context auto-configuration

#### `common-web`

Reusable web-layer utilities.

Examples:

- request user context
- request client context
- gateway header propagation
- CSRF token utilities
- pagination response helpers

#### `test-support`

Reusable testing infrastructure.

Examples:

- integration test annotations
- Testcontainers helpers
- Postgres container support
- Redis container support
- Kafka container support

---

## Frontend Architecture

The frontend uses **Next.js**, **TypeScript**, shared workspace packages, and Turborepo/Yarn workspace style organization.

The admin app is organized around:

```text
apps/admin
├── app/       # Next.js App Router routes/layouts
├── core/      # Core components, hooks, services, stores
├── ce/        # Community edition overrides/extensions
├── ee/        # Enterprise edition placeholder/extensions
├── helpers/
├── public/
└── styles/
```

### Frontend state management

The admin frontend uses a custom state-management layer inspired by MobX style root stores, but implemented with:

- TypeScript classes
- `React.Context`
- `useSyncExternalStore`
- domain-specific stores
- service classes for API calls

Example stores:

```text
core/store/
├── root.store.ts
├── instance.store.ts
├── user.store.ts
├── workspace.store.ts
└── theme.store.ts
```

The goal was to understand state management internals before relying on heavier dependencies.

### API service layer

Frontend service classes wrap fetch behavior behind reusable domain APIs.

Examples:

```text
core/services/
├── api.service.ts
├── auth.service.ts
├── instance.service.ts
├── user.service.ts
└── workspace.service.ts
```

The shared `APIService` handles:

- URL construction
- credentials
- JSON serialization
- response parsing
- typed HTTP errors
- CSRF token attachment for unsafe methods
- silent session refresh on `401`
- one-time retry after refresh

---

## Key Engineering Concepts

### API Gateway security

The API Gateway is the entry point for external traffic.

It is responsible for:

- route authorization
- JWT validation
- JWKS-based token verification
- issuer/audience/token-use validation
- cookie-or-bearer token support
- CORS handling
- CSRF middleware
- request correlation headers
- client metadata headers
- stripping spoofable inbound auth headers
- injecting trusted downstream identity headers

The gateway validates access tokens before requests reach downstream services.

### Authentication

The user-service owns authentication and token issuance.

The authentication flow uses:

- short-lived JWT access tokens
- longer-lived refresh tokens
- Redis-backed refresh sessions
- refresh token rotation
- hashed refresh token storage
- session revocation
- user auth versions
- admin session versions
- JWKS for gateway verification

The API Gateway validates access tokens, while Redis backed session checks make revocation possible before JWT expiration.

### Event-driven communication

Syncturtle uses Kafka style eventing for asynchronous cross service communication.

Current eventing patterns include:

- domain events
- service-owned topics
- dead-letter topics
- retry/backoff handling
- event-driven read models
- cache invalidation events
- after-commit event publishing

Kafka/Redpanda is used in local development.

### Database-per-service

Each service owns its own schema and migrations. When a service needs data owned by another service, it can rely on its own light weight local read model instead of querying other services.

### Response caching

Some backend GET endpoints use a Redis-backed response cache.

The response cache supports:

- annotation-based caching
- cache groups
- TTLs
- per-user scope
- per-workspace scope placeholder
- canonical request hashing
- group version keys
- O(1) invalidation by incrementing version keys

### Observability

Local observability uses:

- OpenTelemetry Collector
- Grafana LGTM stack
- traces
- metrics
- logs
- correlation IDs
- request IDs
- trace/span IDs in logs
- authorization-header redaction

The collector separates:

- system telemetry
- product telemetry

> Note: for product telemetry I had it sent to the locally to test if it works. it would be pointing to my cloud (my own app instance running on my own server).

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
Docker Compose = infrastructure only
Maven / IDE     = backend services
Yarn/Turbo      = frontend apps
.env            = local service configuration
```

Run PostgreSQL, Redis, Redpanda, OpenTelemetry, and Grafana through Docker Compose, then run Spring Boot services locally from your host machine.

This makes local debugging easier because services can be run directly from an IDE, while infrastructure still behaves like external services.

---

## Environment Setup

The backend uses a local `.env` file during development.

Recommended location:

```text
apps/backend/.env
```

This `.env` file is used locally to make running each microservice easier during development.

A safe starter template is provided at:

```text
apps/backend/.env.example
```

Copy it to `.env` before running the backend services:

```sh
cp apps/backend/.env.example apps/backend/.env
```

Then update any values in `apps/backend/.env` as needed for your local machine.

---

## Instance Setup Flow

Before `instance-service` is used as a normal running service, it must first be run with the **`setup` profile**.

The setup profile performs instance bootstrap work:

1. Registers or updates the local instance.
2. Resolves or generates a machine signature.
3. Seeds mandatory instance configuration keys.
4. Creates derived flags for providers like Google, GitHub, GitLab, and Intercom.
5. Exits after setup completes.

The setup runner reads the machine signature from either:

- `--machine-signature=...`
- `MACHINE_SIGNATURE` environment variable
- a generated persisted signature for first-time setup

### Option A: run setup with Maven

```bash
cd apps/backend

./mvnw -pl services/instance-service spring-boot:run \
  -Dspring-boot.run.profiles=setup \
  -Dspring-boot.run.arguments="--machine-signature=local-dev"
```

### Option B: run setup using `.env`

```bash
cd apps/backend

./mvnw -pl services/instance-service spring-boot:run \
  -Dspring-boot.run.profiles=setup
```

Make sure `apps/backend/.env` contains:

```bash
MACHINE_SIGNATURE=local-dev
```

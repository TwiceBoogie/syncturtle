# Database Migrations (liquibase)

This service uses **Liquibase formatted SQL** with a small YAML "table of contents".

## Why formatted SQL?
- We keep DDL as real SQL (easy to read/review).
- Liquibase still tracks every change via `DATABASECHANGELOG` + `DATABASECHANGELOGLOCK`.
- We get rollbacks per change set and clean release tagging.

---

## Folder layout (do not freestyle)

    .
    ├── ...
    ├── db/changelog
    │   ├── db.changelog-master.yaml
    │   ├── releases
    │   │   ├── 0001_init
    │   │   │   ├── db.changelog-0001.yaml
    │   │   │   ├── 001_tables.sql
    │   │   │   ├── 002_constraints.sql
    │   │   │   └── 003_indexes.sql
    │   │   ├── 0002_<short_desc>
    │   │   │   ├── db.changelog-0002.yaml
    │   │   │   ├── 001_tables.sql
    │   │   │   ├── 002_constraints.sql
    │   │   │   └── 003_indexes.sql
    │   │   └── ...
    │   ├── env
    │   │   ├── dev
    │   │   │   ├── db.changelog-dev.yaml
    │   │   │   └── 9000_seed_dev.sql
    │   │   ├── prod
    │   │   │   ├── db.changelog-prod.yaml
    │   │   │   └── 9000_seed_prod.sql
    │   │   └── ...
    │   ├── README.md
    │   └── ...
    └── ...

- **`db.changelog-master.yaml`**: only wires release bundles + env specific bundles.
- **`releases/*`**: immutable release bundles, applied in order.
- **`env/dev/*`**: dev only seed data guarded by `contextFilter: dev`.

---

## Rules (follow religiously)

### 1) Never edit an applied changeset
If a changeset has run in any shared environment (dev DB used by others, CI, staging, prod), it is **immutable**.

✅ Add a new release folder and new changesets.
❌ Do not modify old SQL files "just a little" - it breaks checksums and causes drift.

### 2) Baseline schema has NO contexts
The baseline schema must run everywhere. Do **not** put `context:dev` on schema changes.

Contexts/labels are allowed only for:
- dev only seed data
- diagnostic/testing helpers

### 3) One logical change per changeset
- `001_tables.sql`: tables/columns/defaults (shape)
- `002_constraints.sql`: FK / CHECK / UNIQUE constraints (rules)
- `003_indexes.sql`: indexes (performance)

Keep changesets small and reviewable.

### 4) Every changeset must have a rollback
Liquibase cannot reliably auto-rollback DDL. Always add `--rollback ...`.

### 5) Constraints must be named
Always use explicit constraint names (e.g. `fk_*`, `ck_*`, `uq_*`). This makes later migrations predictable.

### 6) Tag every release
Each `db.changelog-XXXX.yaml` ends with a `tagDatabase` changeset. This allows "rollback to vX.Y.Z".

### 7) Ordering is explicit
We prefer explicit `include:` entries over `includeAll` to avoid ordering surprises.

---

## How to create a new DB change (example)

1) Create a new release folder:
    - `releases/0002_add_instance_flags/`

2) Add:
    - `db.changelog-0002.yaml`
    - `001_tables.sql` / `002_constraints.sql` / `003_indexes.sql`

3) Add a new include to `db.changelog-master.yaml` after 0001.

4) Run locally:
    - `SPRING_PROFILES_ACTIVE=local ./mvnw -pl services/instance-service spring-boot:run`

5) Verify:
    - `GET /acutuator/liquibase` (if exposed in local/dev)

---

## Rollback workflow (examples)
- Roll back last N changsets:
    - `liquibase rollbackCount 1`
- Roll back to tag:
    - `liquibase rollback v0.0.1`

(When running via Spring Boot, rollbacks are typically done via the Liquibase CLI against the same DB.)
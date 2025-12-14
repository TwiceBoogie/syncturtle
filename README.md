Monorepo:

- backend/ (Maven multi-module: libs/, platform/, services/, tests/)
- frontend/ (Turborepo + Next.js apps)
- infra/ (compose/k8s/helm)
- docs/ (ADRs + self-host docs)

```sh
# unit tests only (fast)
./mvnw test

# unit + ITs (*IT.java) across modules
./mvnw -Pit verify

# unit + ITs + system-tests E2E (*E2E.java in system-tests module)
./mvnw -Pit -Psystem verify
```

# Architecture Decision Records (ADR)

## ADR-001: Modular Monolith over Microservices

**Status:** Accepted

**Context:** Single-developer team, free-tier deployment, strong transactional consistency requirements between Payment and Ledger modules.

**Decision:** Modular monolith with enforced package boundaries. Each module owns its entities and repositories. Cross-module access only via service interfaces.

**Consequences:** Simpler deployment, easier debugging, full ACID transactions. Can be decomposed into services later by converting internal event calls to Kafka messages.

---

## ADR-002: Append-Only Ledger

**Status:** Accepted

**Context:** Financial records must be auditable and immutable. Overwriting a balance introduces reconciliation risk.

**Decision:** `ledger_entries` table is append-only. No UPDATE or DELETE ever. Account balances are always derived by querying the ledger (SUM of debits minus credits), never stored as a field.

**Consequences:** Slightly more complex balance queries. Eliminates an entire class of consistency bugs. Makes auditing trivial.

---

## ADR-003: Idempotency Keys on Payment Endpoints

**Status:** Accepted

**Context:** Network retries can cause duplicate payment recording if the endpoint is not idempotent.

**Decision:** Every payment submission requires an `idempotency_key` (UUID generated client-side). Backend stores processed keys and returns the original response for duplicates.

**Consequences:** Safe retries. Consistent with patterns used by Stripe, Razorpay, and all serious payment APIs.

---

## ADR-004: JWT with Refresh Token Rotation

**Status:** Accepted

**Context:** Stateless auth needed for scalability. Short-lived access tokens reduce exposure risk.

**Decision:** Access tokens expire in 24h. Refresh tokens expire in 7 days. On each refresh, both tokens are rotated (old refresh token is revoked, new pair issued).

**Consequences:** Stolen refresh tokens detected on next use. Logout revokes all tokens immediately.

---

## ADR-005: BigDecimal for All Money

**Status:** Accepted — Non-negotiable

**Context:** Floating-point types (double, float) cannot represent most decimal fractions exactly, causing rounding errors that compound in financial calculations.

**Decision:** All monetary values use `BigDecimal` with `HALF_UP` rounding and scale 2 for display, scale 8 for intermediate calculations. Enforced via `MoneyUtils`.

**Consequences:** Correct financial arithmetic. Verbosity is acceptable cost.

---

## ADR-006: Flyway for Schema Migrations

**Status:** Accepted

**Context:** Database schema must evolve in a controlled, versioned, reproducible way across environments.

**Decision:** All schema changes go through Flyway migration scripts in `db/migration/`. Scripts are numbered (V1, V2...) and immutable once committed.

**Consequences:** Any developer can recreate the exact schema from scratch. Production deployments are safe and repeatable.

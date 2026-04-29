# LendOS — Lending Operations & Risk Platform

A production-grade multi-tenant lending management system built with Java 17, Spring Boot 3, and PostgreSQL (Supabase).

---

## Project Structure

```
lendos/
├── backend/     # Spring Boot 3 — Java 17
├── frontend/    # React (minimal UI layer)
└── docs/        # Architecture decisions, DB schema
```

---

## Modules

| Module | Status | Description |
|--------|--------|-------------|
| Identity & Access | ✅ Complete | Multi-tenant auth, JWT, RBAC |
| Borrower Onboarding | 🔧 Phase 2 | State machine, profile management |
| Loan Lifecycle | 🔧 Phase 2 | Approval workflow, state machine |
| Payment Engine | 🔧 Phase 2 | EMI schedule, idempotent payments |
| Ledger (Double-Entry) | 🔧 Phase 3 | Append-only financial ledger |
| Risk Engine | 🔧 Phase 4 | Configurable rule-based scoring |

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- A Supabase project (free tier)
- Node.js 18+ (for frontend)

### Backend Setup

```bash
cd backend

# 1. Copy the environment template
cp .env.example .env

# 2. Fill in your Supabase credentials and JWT secret in .env
#    DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET

# 3. Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The app starts on `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Generating a JWT Secret (required)
```bash
openssl rand -hex 32
```
Copy the output into `JWT_SECRET` in your `.env` file.

---

## API Overview (Module 1)

### Public Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register a new tenant + admin user |
| POST | `/api/v1/auth/login` | Login, receive access + refresh tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Revoke all refresh tokens |

### Protected Endpoints (require Bearer token)
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/users/me` | Any | Get current user profile |
| POST | `/api/v1/users` | ADMIN | Create user in tenant |
| GET | `/api/v1/users` | ADMIN | List all users in tenant |
| PATCH | `/api/v1/users/{id}/status` | ADMIN | Update user status |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT 0.12) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL via Supabase |
| Migrations | Flyway |
| Cache | Caffeine |
| Logging | SLF4J + Logback + Logstash Encoder |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Testing | JUnit 5 + Mockito |

---

## Design Principles

1. **Money uses BigDecimal only** — Never double or float
2. **Ledger is append-only** — No UPDATE/DELETE on `ledger_entries`
3. **Idempotency keys** — Payment endpoints are safe to retry
4. **Module isolation** — Modules never access each other's repositories directly
5. **All secrets in .env** — `application.yml` contains zero hardcoded secrets
6. **Correlation IDs** — Every request is traceable end-to-end via `X-Correlation-ID`

---

## Database Migrations (Flyway)

| Version | Description |
|---------|-------------|
| V1 | Identity schema (tenants, users, refresh_tokens) |
| V2 | Borrower & Loan tables |
| V3 | Payment engine tables (emi_schedules, payments) |
| V4 | Ledger & Risk assessment tables |

---

## Environment Variables

See `backend/.env.example` for the full list. The most critical ones:

```
DB_URL              — Supabase JDBC connection string
DB_USERNAME         — Database username
DB_PASSWORD         — Database password
JWT_SECRET          — Min 256-bit hex string (use openssl rand -hex 32)
```
# lendos
# lendos. 

# Hospital Order Management System
**CSCI-P532 — Project 2 | Spring 2026**

A hospital order management system built with Spring Boot (backend) and plain HTML/CSS/JS (frontend), applying The Method's four-layer architecture and five required design patterns.

[![CI](https://github.com/P532-SPRING2026-rdevaraj/project2/actions/workflows/ci.yml/badge.svg?branch=week2)](https://github.com/P532-SPRING2026-rdevaraj/project2/actions/workflows/ci.yml)

---

## Live Deployments

| | Week 1 (`main`) | Week 2 (`week2`) |
|---|---|---|
| **Full App** | https://p532-spring2026-rdevaraj.github.io/project2 | https://project2-week2.onrender.com |
| **Backend API** | https://hospital-order-system.onrender.com/api/orders | https://project2-week2.onrender.com/api/orders |

> **Note:** Render free-tier services spin down after 15 minutes of inactivity and may take up to 60 seconds to cold-start. This is expected.
>
> Week 2 serves both frontend and backend from the single Render URL (static files bundled into the Spring Boot jar).

---

## Week 2 Changes

| Change | Description | Existing files modified |
|---|---|---|
| **Change 1** | Department-aware triage — 3 selectable strategies (Priority First, Load Balancing, Deadline First), switchable at runtime from the UI. **Load Balancing auto-assigns orders** to the least-loaded staff member — no manual claim needed. | 1 (`OrderManager`) |
| **Change 2a** | Multi-channel notifications — Console, In-app badge, Email (mock); configurable via Settings panel | 0 |
| **Change 2b** | Order processing decorators — Priority escalation (URGENT→STAT within 5 min window) + STAT audit logging | 1 (`Order`) |
| **Change 3** | Command undo & replay — single-level undo via `UndoableCommandDecorator`; replay any past command from audit log | 1 (`OrderAccess`) |

---

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 17 + Spring Boot 3.2 |
| Frontend | HTML + CSS + JavaScript (plain) |
| Persistence | In-memory (ConcurrentHashMap) |
| Build | Maven |
| Container | Docker (multi-stage) |
| Deployment | Render.com |
| CI/CD | GitHub Actions |
| Testing | JUnit 5 + Mockito (45 tests) |

---

## Project Structure

```
Project2/
├── Dockerfile                  ← multi-stage build (repo root)
├── DESIGN.md                   ← design document (Week 1 + Week 2)
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/hospital/ordersystem/
│       │   │   ├── access/         ← Resource Access layer
│       │   │   │   ├── OrderAccess.java
│       │   │   │   ├── CommandLogAccess.java
│       │   │   │   └── StaffAccess.java      ← staff registry (load-balancing)
│       │   │   ├── client/         ← REST controllers (Client layer)
│       │   │   │   ├── OrderController.java
│       │   │   │   ├── TriageController.java
│       │   │   │   ├── StaffController.java  ← staff management + auto-assign
│       │   │   │   ├── AuditController.java
│       │   │   │   ├── NotificationController.java
│       │   │   │   └── UndoController.java
│       │   │   ├── command/        ← Command pattern
│       │   │   ├── config/         ← Spring config (Clock, Strategies, CORS)
│       │   │   ├── decorator/      ← Decorator pattern
│       │   │   ├── engine/         ← TriagingEngine
│       │   │   ├── factory/        ← Factory pattern
│       │   │   ├── manager/        ← OrderManager (orchestrator)
│       │   │   ├── model/          ← Domain model
│       │   │   ├── observer/       ← Observer pattern
│       │   │   ├── strategy/       ← Strategy pattern
│       │   │   └── utility/        ← NotificationService + implementations
│       │   └── resources/static/   ← Frontend (served by Spring Boot)
│       └── test/                   ← JUnit 5 + Mockito (45 tests)
└── docs/                       ← Frontend source (Week 1: GitHub Pages)
```

---

## Architecture — Four-Layer Design (The Method)

| Layer | Components |
|---|---|
| **Client** | `OrderController`, `AuditController`, `TriageController`, `NotificationController`, `UndoController`, `StaffController` |
| **Business Logic** | `OrderManager`, `TriagingEngine`, `OrderFactory`, Commands, Decorators, Observers |
| **Resource Access** | `OrderAccess`, `CommandLogAccess`, `StaffAccess` |
| **Resource** | `Order`, `LabOrder`, `MedicationOrder`, `ImagingOrder`, `CommandLogEntry` |
| **Utility** | `NotificationService`, `CompositeNotificationService`, `ConsoleNotificationService`, `InAppNotificationService`, `EmailNotificationService` |

---

## Design Patterns

| Pattern | Key Classes | Justification |
|---|---|---|
| **Strategy** | `TriageStrategy`, `PriorityFirstTriageStrategy`, `LoadBalancingTriageStrategy`, `DeadlineFirstTriageStrategy` | Swap triage algorithm at runtime without touching `TriagingEngine` |
| **Observer** | `OrderEventPublisher`, `OrderObserver`, `NotificationObserver` | Add new reactions to order events with zero changes to existing classes |
| **Decorator** | `OrderHandler`, `ValidationDecorator`, `AuditLoggingDecorator`, `PriorityEscalationDecorator`, `StatAuditDecorator` | Stack processing steps around a base handler without modifying it |
| **Factory** | `OrderFactory`, `LabOrder`, `MedicationOrder`, `ImagingOrder` | Decouple callers from concrete order subtypes via a registry map |
| **Command** | `SubmitOrderCommand`, `ClaimOrderCommand`, `CompleteOrderCommand`, `CancelOrderCommand`, `UndoableCommandDecorator` | Encapsulate actions as objects; `UndoableCommandDecorator` adds undo to any command without modifying it |

---

## Running Locally

### Option 1 — Maven (fastest)
```bash
cd backend
mvn spring-boot:run
# Full app at http://localhost:8080
```

### Option 2 — Docker (matches Render exactly)
```bash
docker build -t ordersystem .
docker run -p 8080:8080 ordersystem
# Full app at http://localhost:8080
```

### Run Tests
```bash
cd backend
mvn test
# 45 tests — JUnit 5 + Mockito
```

---

## Load Balancing Triage — How It Works

When **Load Balancing** is selected as the triage strategy, manual order claiming is replaced by automatic assignment:

1. **Register staff** — go to Settings → Staff Management and add staff members (e.g. `Tech-1`, `Tech-2`)
2. **Switch strategy** — on the Order Queue tab select **Load Balancing** and click Apply
   - All existing pending orders are immediately auto-assigned to the least-loaded staff member
3. **Submit orders** — new orders skip the PENDING state and are instantly claimed by whoever has the fewest in-progress orders at that moment
4. **Complete orders** — staff enter their ID and click Complete; no Claim button is shown in this mode
5. **Switch back** — selecting Priority First or Deadline First restores manual claiming

---

## API Reference

### Week 1 Endpoints
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/orders` | Get full order queue (triage-sorted) |
| `POST` | `/api/orders` | Submit a new order |
| `POST` | `/api/orders/{id}/claim` | Claim a pending order |
| `POST` | `/api/orders/{id}/complete` | Complete an in-progress order |
| `POST` | `/api/orders/{id}/cancel` | Cancel a pending order |
| `GET` | `/api/audit` | Get command audit log |

### Week 2 Endpoints
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/triage/strategy` | Get current triage strategy |
| `PUT` | `/api/triage/strategy` | Set triage strategy at runtime |
| `GET` | `/api/notifications/preferences` | Get channel preferences |
| `PUT` | `/api/notifications/preferences` | Update channel preferences |
| `GET` | `/api/notifications/badge` | Get in-app badge count |
| `POST` | `/api/notifications/badge/reset` | Reset badge count |
| `POST` | `/api/orders/undo` | Undo last command |
| `POST` | `/api/orders/replay/{index}` | Replay command from audit log |
| `GET` | `/api/staff` | List all registered fulfilment staff |
| `POST` | `/api/staff` | Register a staff member `{ "staffId": "Tech-1" }` |
| `DELETE` | `/api/staff/{staffId}` | Remove a staff member |
| `POST` | `/api/staff/auto-assign` | Auto-assign all pending orders to least-loaded staff |

---

## CI/CD Pipeline

Three jobs run on every push to `main` or `week2`:

| Job | What it does |
|---|---|
| `test` | Runs `mvn test`, uploads Surefire report as artifact |
| `build` | Packages jar + builds Docker image |
| `deploy-main` | Triggers Render deploy for Week 1 (on `main` push only) |
| `deploy-week2` | Triggers Render deploy for Week 2 (on `week2` push only) |

See [.github/workflows/ci.yml](.github/workflows/ci.yml)

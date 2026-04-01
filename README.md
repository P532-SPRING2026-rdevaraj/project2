# Hospital Order Management System
**CSCI-P532 — Project 2 | Spring 2026**

A hospital order management system built with Spring Boot (backend) and plain HTML/CSS/JS (frontend), applying The Method's four-layer architecture and five required design patterns.

---

## Live Demo

| | URL |
|---|---|
| **Frontend** | https://p532-spring2026-rdevaraj.github.io/project2 |
| **Backend API** | https://hospital-order-system.onrender.com/api/orders |

---

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 17 + Spring Boot 3.2 |
| Frontend | HTML + CSS + JavaScript (plain) |
| Persistence | In-memory (ConcurrentHashMap) |
| Build | Maven |
| Container | Docker (multi-stage) |
| Deployment | Render.com (backend) + GitHub Pages (frontend) |
| CI/CD | GitHub Actions |
| Testing | JUnit 5 + Mockito (25 tests) |

---

## Project Structure

```
Project2/
├── Dockerfile                  ← multi-stage build (repo root)
├── DESIGN.md                   ← design document
├── backend/                    ← Spring Boot application
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/hospital/ordersystem/
│       │   ├── access/         ← Resource Access layer
│       │   ├── client/         ← REST controllers (Client layer)
│       │   ├── command/        ← Command pattern
│       │   ├── config/         ← CORS + observer wiring
│       │   ├── decorator/      ← Decorator pattern
│       │   ├── engine/         ← TriagingEngine (Engine layer)
│       │   ├── factory/        ← Factory pattern
│       │   ├── manager/        ← OrderManager (Manager layer)
│       │   ├── model/          ← Domain model (Resource layer)
│       │   ├── observer/       ← Observer pattern
│       │   ├── strategy/       ← Strategy pattern
│       │   └── utility/        ← NotificationService (Utility layer)
│       └── test/               ← JUnit 5 + Mockito tests
└── docs/                       ← Frontend (served via GitHub Pages)
    ├── index.html
    ├── style.css
    └── app.js
```

---

## Architecture — Four-Layer Design (The Method)

| Layer | Components |
|---|---|
| **Client** | `OrderController`, `AuditController` |
| **Business Logic** | `OrderManager`, `TriagingEngine`, `OrderFactory`, all Command/Decorator/Observer classes |
| **Resource Access** | `OrderAccess`, `CommandLogAccess` |
| **Resource** | `Order`, `LabOrder`, `MedicationOrder`, `ImagingOrder`, `CommandLogEntry` |
| **Utility** | `NotificationService`, `ConsoleNotificationService` |

> REST controllers contain zero business logic — they delegate immediately to `OrderManager`.

---

## Design Patterns

| Pattern | Key Classes | Justification |
|---|---|---|
| **Strategy** | `TriageStrategy`, `PriorityFirstTriageStrategy` | Swap triage algorithm at runtime without touching `TriagingEngine` |
| **Observer** | `OrderEventPublisher`, `OrderObserver`, `NotificationObserver` | Add new reactions to order events with zero changes to existing classes |
| **Decorator** | `OrderHandler`, `ValidationDecorator`, `AuditLoggingDecorator` | Stack processing steps around a base handler without modifying it |
| **Factory** | `OrderFactory`, `LabOrder`, `MedicationOrder`, `ImagingOrder` | Decouple callers from concrete order subtypes via a registry map |
| **Command** | `SubmitOrderCommand`, `ClaimOrderCommand`, `CompleteOrderCommand`, `CancelOrderCommand` | Encapsulate actions as objects for queuing, logging, and Week 2 undo |

---

## Core Use Cases

### 1. Submit an Order
```
Browser → OrderController → OrderManager → OrderFactory
       → SubmitOrderCommand → [ValidationDecorator → AuditLoggingDecorator]
       → OrderAccess (save) → OrderEventPublisher → NotificationService
       → CommandLogAccess (log)
```

### 2. Fulfil an Order (Claim → Complete)
```
Browser → OrderController → OrderManager → ClaimOrderCommand
       → OrderAccess (find + update to IN_PROGRESS) → OrderEventPublisher
       → CommandLogAccess (log)

Browser → OrderController → OrderManager → CompleteOrderCommand
       → OrderAccess (find + update to COMPLETED) → OrderEventPublisher
       → CommandLogAccess (log)
```

---

## Running Locally

### Backend
```bash
cd backend
mvn spring-boot:run
# API available at http://localhost:8080
```

### Frontend
```bash
cd docs
python3 -m http.server 3000
# Open http://localhost:3000
```

### Docker (from repo root)
```bash
docker build -t hospital-order-system .
docker run -p 8080:8080 hospital-order-system
```

### Tests
```bash
cd backend
mvn test
# 25 tests — JUnit 5 + Mockito
```

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/orders` | Get full order queue (triage-sorted) |
| `POST` | `/api/orders` | Submit a new order |
| `POST` | `/api/orders/{id}/claim` | Claim a pending order |
| `POST` | `/api/orders/{id}/complete` | Complete an in-progress order |
| `POST` | `/api/orders/{id}/cancel` | Cancel a pending order |
| `GET` | `/api/audit` | Get command audit log |

### Submit Order — Request Body
```json
{
  "type": "LAB",
  "patientName": "Alice Smith",
  "clinician": "Dr. Jones",
  "description": "CBC blood panel",
  "priority": "STAT"
}
```

---

## UI Features

- **Order Queue** — sorted list of all orders (STAT → URGENT → ROUTINE), auto-refreshes every 3 seconds
- **Submit Order** — form to create Lab, Medication, or Imaging orders
- **Fulfilment Controls** — Claim and Complete buttons; staff ID required
- **Audit Trail** — scrollable log of every command executed

---

## CI/CD

GitHub Actions runs on every push to `main`:
1. Build and test (`mvn verify`)
2. Docker build from repo root (`docker build .`)

See [.github/workflows/ci.yml](.github/workflows/ci.yml)

---

## Verbal Demo Answer

> *"A new triage algorithm has been requested. Show us exactly where in your code that change lives and how many existing files you would touch."*

**Answer:** Create one new file implementing `TriageStrategy` (e.g. `DepartmentTriageStrategy.java`). Touch **zero existing files** — call `triagingEngine.setTriageStrategy(new DepartmentTriageStrategy())` from a new endpoint or config. The Strategy interface, `TriagingEngine`, and `OrderManager` are all unchanged.

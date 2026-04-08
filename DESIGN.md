# Design Document — Hospital Order Management System
**CSCI-P532 | Spring 2026 | Rohith Gowda Devaraju**

---

## Submission URLs

| Item | URL |
|---|---|
| **GitHub Repository** | https://github.com/P532-SPRING2026-rdevaraj/project2 |
| **Week 1 — Live App** | https://p532-spring2026-rdevaraj.github.io/project2 |
| **Week 1 — Backend API** | https://hospital-order-system.onrender.com |
| **Week 2 — Live App (branch: week2)** | https://project2-week2.onrender.com |
| **Week 2 — Backend API** | https://project2-week2.onrender.com/api/orders |

---

## 1. Layered Component Diagram

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                              CLIENT LAYER                                    ║
║                                                                              ║
║  OrderController        AuditController     TriageController   (Week 2)     ║
║  POST /api/orders       GET /api/audit      PUT /api/triage/strategy        ║
║  POST /{id}/claim                                                            ║
║  POST /{id}/complete    NotificationController (Week 2)                     ║
║  POST /{id}/cancel      GET/PUT /api/notifications/preferences              ║
║                         GET /api/notifications/badge                        ║
║  UndoController (Week 2)                                                    ║
║  POST /api/orders/undo                                                      ║
║  POST /api/orders/replay/{index}                                            ║
╚════════════════════════════╤════════════════════════════════════════════════╝
                             │ delegates — no business logic in controllers
╔════════════════════════════▼════════════════════════════════════════════════╗
║                         BUSINESS LOGIC LAYER                                ║
║                                                                              ║
║  ┌─────────────────────────────────────────────────────────────────────┐    ║
║  │                         OrderManager                                │    ║
║  │  submitOrder()  claimOrder()  completeOrder()  cancelOrder()        │    ║
║  │  getQueue()  getCommandLog()  setTriageStrategy()  undo()  replay() │    ║
║  │  dispatch(cmd) → wraps in UndoableCommandDecorator → logs           │    ║
║  └──────────┬─────────────────────────────────────┬────────────────────┘    ║
║             │                                     │                         ║
║  ┌──────────▼──────────┐             ┌────────────▼──────────────────┐      ║
║  │   TriagingEngine    │             │         OrderFactory          │      ║
║  │  getAllOrders()     │             │  createOrder(type,...)        │      ║
║  │  setStrategy()      │             │  registry: LAB/MED/IMAGING    │      ║
║  └──────────┬──────────┘             └───────────────────────────────┘      ║
║             │ uses                                                           ║
║  ┌──────────▼──────────┐                                                    ║
║  │   TriageStrategy    │◄── PriorityFirstTriageStrategy  (Week 1)           ║
║  │   «interface»       │◄── LoadBalancingTriageStrategy  (Week 2)           ║
║  └─────────────────────┘◄── DeadlineFirstTriageStrategy  (Week 2)           ║
║                                                                              ║
║  ── Commands ──────────────────────────────────────────────────────────     ║
║  OrderCommand «interface»                                                   ║
║  SubmitOrderCommand  StatAwareSubmitOrderCommand(W2)                        ║
║  ClaimOrderCommand   CompleteOrderCommand   CancelOrderCommand              ║
║  UndoableCommandDecorator (Week 2) — wraps any command with undo()         ║
║                                                                              ║
║  ── Decorator Chain (inside StatAwareSubmitOrderCommand) ───────────────    ║
║  StatAuditDecorator(W2) → PriorityEscalationDecorator(W2)                  ║
║    → AuditLoggingDecorator → ValidationDecorator → BaseOrderHandler         ║
║                                                                              ║
║  ── Observer Chain ─────────────────────────────────────────────────────    ║
║  OrderEventPublisher ──► NotificationObserver ──► NotificationService       ║
╚══════════════════════════════════════════════════════════════════════════════╝
                             │
╔════════════════════════════▼════════════════════════════════════════════════╗
║                        RESOURCE ACCESS LAYER                                ║
║                                                                              ║
║  OrderAccess                          CommandLogAccess                      ║
║  saveOrder()  findOrderById()         append(CommandLogEntry)               ║
║  listAllOrders()  listPendingOrders() getAll()                              ║
║  deleteOrder() (Week 2)                                                     ║
╚════════════════════════════╤════════════════════════════════════════════════╝
                             │
╔════════════════════════════▼════════════════════════════════════════════════╗
║                           RESOURCE LAYER                                    ║
║                                                                              ║
║  In-Memory Store (ConcurrentHashMap + synchronized List)                    ║
║  Order (abstract) ◄── LabOrder | MedicationOrder | ImagingOrder             ║
║  CommandLogEntry   OrderStatus (enum)   OrderPriority (enum)                ║
╚══════════════════════════════════════════════════════════════════════════════╝

╔══════════════════════════════════════════════════════════════════════════════╗
║                           UTILITY LAYER                                     ║
║                                                                              ║
║  NotificationService «interface»  notify(Order, String event)               ║
║  ConsoleNotificationService       (Week 1)                                  ║
║  CompositeNotificationService     (Week 2, @Primary — routes to channels)   ║
║  InAppNotificationService         (Week 2 — badge counter)                  ║
║  EmailNotificationService         (Week 2 — mock email)                     ║
║  NotificationPreferences          (Week 2 — per-channel on/off flags)       ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

---

## 2. Call Chain Diagrams

### Use Case 1 — Submit an Order

```
Clinician (Browser)
      │  POST /api/orders  {type, patientName, clinician, description, priority}
      ▼
OrderController                                            [CLIENT]
      │  orderManager.submitOrder(type, patient, clinician, desc, priority)
      ▼
OrderManager                                               [MANAGER]
      │  1. orderFactory.createOrder(type, ...)
      ▼
OrderFactory                                               [ENGINE / BUSINESS LOGIC]
      │     registry map → new LabOrder / MedicationOrder / ImagingOrder
      ◄─────── returns Order
      │
      │  2. new StatAwareSubmitOrderCommand(order, ...)
      │  3. dispatch(cmd)
      │       → wraps in UndoableCommandDecorator (snapshots state)
      │       → undoable.execute()
      ▼
StatAwareSubmitOrderCommand.execute()                      [COMMAND]
      │  Decorator chain:
      │  StatAuditDecorator
      │    → PriorityEscalationDecorator  (URGENT→STAT if recent STAT same type)
      │        → AuditLoggingDecorator    (logs [AUDIT] line)
      │            → ValidationDecorator  (rejects blank fields)
      │                → BaseOrderHandler (returns order unchanged)
      │  orderAccess.saveOrder(processed)
      ▼
OrderAccess                                                [RESOURCE ACCESS]
      │     store.put(orderId, order)
      ▼
In-Memory Store                                            [RESOURCE]
      ◄─────────────────────────────────────────────────────────────
      │  eventPublisher.publish(order, "ORDER_SUBMITTED")
      ▼
OrderEventPublisher → NotificationObserver                 [OBSERVER]
      │                  → compositeNotificationService.notify(order, event)
      ▼
CompositeNotificationService                               [UTILITY]
      │  → ConsoleNotificationService (if enabled)
      │  → InAppNotificationService   (if enabled — increments badge)
      │  → EmailNotificationService   (if enabled — logs mock email)
      ◄─────────────────────────────────────────────────────────────
      │  commandLogAccess.append(CommandLogEntry("SUBMIT", orderId, actor))
      ▼
CommandLogAccess                                           [RESOURCE ACCESS]
      ◄─────────────────────────────────────────────────────────────
OrderManager returns Order → OrderController returns 200 OK {OrderDto}
```

---

### Use Case 2 — Fulfil an Order (Claim → Complete)

```
Staff Member (Browser)
      │  POST /api/orders/{id}/claim  {actor: "Tech-1"}
      ▼
OrderController                                            [CLIENT]
      │  orderManager.claimOrder(orderId, "Tech-1")
      ▼
OrderManager                                               [MANAGER]
      │  new ClaimOrderCommand(orderId, "Tech-1", orderAccess, eventPublisher)
      │  dispatch(cmd) → UndoableCommandDecorator snapshots PENDING state
      │  → cmd.execute()
      ▼
ClaimOrderCommand.execute()                                [COMMAND]
      │  a. orderAccess.findOrderById(orderId) → Order
      │  b. guard: status == PENDING, claimedBy == null
      │  c. order.setStatus(IN_PROGRESS), order.setClaimedBy("Tech-1")
      │  d. orderAccess.saveOrder(order)
      │  e. eventPublisher.publish(order, "ORDER_CLAIMED")
      ▼
CompositeNotificationService                               [UTILITY]
      │     notifies all active channels
      ◄─────────────────────────────────────────────────────────────
      │  commandLogAccess.append(CommandLogEntry("CLAIM", orderId, "Tech-1"))
      ◄─────────────────────────────────────────────────────────────
OrderController returns 200 OK

      ─── Staff clicks Complete ────────────────────────────────────

      │  POST /api/orders/{id}/complete  {actor: "Tech-1"}
      ▼
OrderController → OrderManager                             [CLIENT → MANAGER]
      │  new CompleteOrderCommand(orderId, "Tech-1", orderAccess, eventPublisher)
      │  dispatch(cmd) → UndoableCommandDecorator snapshots IN_PROGRESS state
      ▼
CompleteOrderCommand.execute()                             [COMMAND]
      │  a. orderAccess.findOrderById(orderId) → Order
      │  b. guard: status == IN_PROGRESS, claimedBy == "Tech-1"
      │  c. order.setStatus(COMPLETED)
      │  d. orderAccess.saveOrder(order)
      │  e. eventPublisher.publish(order, "ORDER_COMPLETED")
      │  f. commandLogAccess.append(CommandLogEntry("COMPLETE", orderId, "Tech-1"))
      ◄─────────────────────────────────────────────────────────────
OrderController returns 200 OK
Staff Member sees order status updated on next 3-second poll
```

---

## 3. Design Pattern Justifications

| Pattern | Classes Involved | Layer | One-Sentence Justification |
|---|---|---|---|
| **Strategy** | `TriageStrategy` (interface), `PriorityFirstTriageStrategy`, `LoadBalancingTriageStrategy`, `DeadlineFirstTriageStrategy` | Business Logic | Encapsulates interchangeable triage algorithms behind a common interface so the active policy can be swapped at runtime via `TriagingEngine.setTriageStrategy()` without modifying `OrderManager` or `TriagingEngine`. |
| **Observer** | `OrderObserver` (interface), `OrderEventPublisher`, `NotificationObserver`, `NotificationService` | Business Logic / Utility | Decouples order state changes from the components that react to them so new notification channels (in-app badge, email) are added as new classes in Week 2 with zero changes to `OrderManager` or any command object. |
| **Decorator** | `OrderHandler` (interface), `BaseOrderHandler`, `OrderHandlerDecorator`, `ValidationDecorator`, `AuditLoggingDecorator`, `PriorityEscalationDecorator`, `StatAuditDecorator` | Business Logic | Stacks order-processing steps transparently around a base handler so Week 2 escalation and audit steps are added in a new command class (`StatAwareSubmitOrderCommand`) with zero changes to existing decorators or `SubmitOrderCommand`. |
| **Factory** | `OrderFactory`, `OrderCreator` (functional interface), `LabOrder`, `MedicationOrder`, `ImagingOrder` | Business Logic | Centralises order-subtype creation in a registry map so callers are fully decoupled from concrete classes and a new order type requires adding one registry entry with no changes to `OrderManager` or any controller. |
| **Command** | `OrderCommand` (interface), `SubmitOrderCommand`, `ClaimOrderCommand`, `CompleteOrderCommand`, `CancelOrderCommand`, `UndoableCommandDecorator` | Business Logic | Encapsulates each order action as a self-contained object so `OrderManager` can dispatch, log, and undo any action; in Week 2 a single `UndoableCommandDecorator` adds undo by snapshotting state before `execute()` without touching any individual command class. |

---

## 4. Week 2 — Pre-Existing Files Modified Per Change

| Change | Files Modified | Count |
|---|---|---|
| **Change 1 — Department-Aware Triage** | `OrderManager.java` (added `setTriageStrategy`, `getTriageStrategyName`, `Clock` + strategy map to constructor) | **1** |
| **Change 2a — Multi-Channel Notifications** | *(none)* | **0** |
| **Change 2b — Order Processing Decorators** | `Order.java` (removed `final` from `priority` field, added `setPriority()` — priority was incorrectly immutable in Week 1) | **1** |
| **Change 3 — Command Undo and Replay** | `OrderAccess.java` (added `deleteOrder()` needed for SUBMIT undo; `OrderManager` already counted in Change 1) | **1** |

**Total pre-existing files modified across all four changes: 3**
(`OrderManager`, `Order`, `OrderAccess` — each touched exactly once)

### How the low count was achieved

- **Change 1**: `StrategyConfig` (new file) re-exposes `PriorityFirstTriageStrategy` as `@Primary` — so that existing file is untouched despite Spring now having 3 `TriageStrategy` beans.
- **Change 2a**: `CompositeNotificationService` (new, `@Primary`) is injected into the existing `NotificationObserver` automatically — zero existing files touched.
- **Change 2b**: `StatAwareSubmitOrderCommand` (new file) carries the Week 2 decorator chain — `SubmitOrderCommand` is untouched at its Week 1 state.
- **Change 3**: `UndoableCommandDecorator` (new file) snapshots state before any command executes and restores on `undo()` — zero individual command classes modified. `UndoController` (new file) handles the HTTP endpoints — `OrderController` untouched.

---

## 5. Volatility Missed in Week 1

**What was missed:** `Order.priority` was declared `final`, treating priority as immutable after construction. The `PriorityEscalationDecorator` (Change 2b) must mutate priority at processing time, requiring `final` to be removed and `setPriority()` to be added — one file modified that ideally should have been zero.

**What I would do differently:** Separate the clinician's submitted priority (immutable) from the system's effective priority (mutable by the processing chain) — either via a `ProcessedOrder` wrapper or a second `effectivePriority` field — keeping the domain model immutable below the business-logic layer.

---

## 6. Pattern Audit (Week 2)

| Pattern | Result |
|---|---|
| **Strategy** | Adding two new strategies required zero changes to `TriagingEngine` or `OrderManager` — new implementations plus a `StrategyConfig` bean file were sufficient. The interface fully absorbed Change 1's volatility. |
| **Observer** | Adding three notification channels required zero changes to `OrderManager`, `OrderEventPublisher`, or `NotificationObserver`. `@Primary` on `CompositeNotificationService` redirected the chain automatically. |
| **Decorator** | Adding escalation and audit decorators required zero changes to `SubmitOrderCommand` or existing decorators — a new `StatAwareSubmitOrderCommand` carries the extended chain and `OrderManager` dispatches it instead. |
| **Factory** | `OrderFactory` was untouched across both weeks — no new order types introduced in Week 2, confirming the registry map correctly isolates creation from callers. |
| **Command** | `UndoableCommandDecorator` delivered undo for all four command types by wrapping at the `dispatch()` site — zero individual command classes were modified, validating the Command pattern's encapsulation payoff. |

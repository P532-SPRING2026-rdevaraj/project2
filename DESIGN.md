# Design Document — Hospital Order Management System
**CSCI-P532 | Spring 2026 | Rohith Gowda Devaraju**

---

## 1. Layered Component Diagram

```
╔══════════════════════════════════════════════════════════════════════╗
║                         CLIENT LAYER                                 ║
║                                                                      ║
║   ┌─────────────────────────┐   ┌─────────────────────────┐          ║
║   │    OrderController      │   │    AuditController      │          ║
║   │  POST /api/orders       │   │  GET  /api/audit        │          ║
║   │  POST /api/orders/{id}/ │   │                         │          ║
║   │    claim | complete     │   │                         │          ║
║   │    cancel               │   │                         │          ║
║   └────────────┬────────────┘   └───────────────┬─────────┘          ║
╚════════════════│════════════════════════════════│════════════════════╝
                 │ delegates all calls            │ delegates all calls
╔════════════════▼════════════════════════════════▼════════════════════╗
║                    BUSINESS LOGIC LAYER                              ║
║                                                                      ║
║   ┌──────────────────────────────────────────────────────────────┐   ║
║   │                      OrderManager                            │   ║
║   │  submitOrder()  claimOrder()  completeOrder()  cancelOrder() │   ║
║   │  getQueue()     getCommandLog()                              │   ║
║   │  dispatch(OrderCommand) → logs to CommandLogAccess           │   ║
║   └───────┬──────────────────────────────────┬───────────────────┘   ║
║           │                                  │                       ║
║   ┌───────▼──────────────┐   ┌───────────────▼─────────────────┐     ║
║   │   TriagingEngine     │   │        OrderFactory             │     ║
║   │  getTriagedQueue()   │   │  createOrder(type,...)          │     ║
║   │  getAllOrders()      │   │  registry map of OrderCreators  │     ║
║   │  setTriageStrategy() │   └─────────────────────────────────┘     ║
║   └───────┬──────────────┘                                           ║
║           │ uses                                                     ║
║   ┌───────▼──────────────┐                                           ║
║   │   TriageStrategy     │◄── PriorityFirstTriageStrategy            ║
║   │  «interface»         │    (STAT > URGENT > ROUTINE, FIFO)        ║
║   └──────────────────────┘                                           ║
║                                                                      ║
║   ─── Command Objects ──────────────────────────────────────────     ║
║   SubmitOrderCommand  ClaimOrderCommand                              ║
║   CompleteOrderCommand  CancelOrderCommand                           ║
║   (all implement OrderCommand interface with execute() / undo())     ║
║                                                                      ║
║   ─── Decorator Chain (runs inside SubmitOrderCommand) ──────────    ║
║   AuditLoggingDecorator → ValidationDecorator → BaseOrderHandler     ║
║                                                                      ║
║   ─── Observer Chain ───────────────────────────────────────────     ║
║   OrderEventPublisher ──► NotificationObserver                       ║
║                                (implements OrderObserver)            ║
╚══════════════════════════════════════════════════════════════════════╝
                 │                              │
╔════════════════▼══════════════════════════════▼══════════════════════╗
║                    RESOURCE ACCESS LAYER                             ║
║                                                                      ║
║   ┌──────────────────────────┐   ┌──────────────────────────┐        ║
║   │       OrderAccess        │   │    CommandLogAccess      │        ║
║   │  saveOrder()             │   │  append(entry)           │        ║
║   │  findOrderById()         │   │  getAll()                │        ║
║   │  listAllOrders()         │   └──────────────┬───────────┘        ║
║   │  listPendingOrders()     │                  │                    ║
║   └──────────────┬───────────┘                  │                    ║
╚══════════════════│══════════════════════════════│════════════════════╝
                   │                              │
╔══════════════════▼══════════════════════════════▼════════════════════╗
║                      RESOURCE LAYER                                  ║
║                                                                      ║
║   ┌──────────────────────────────────────────────────────────────┐   ║
║   │   In-Memory Store  (ConcurrentHashMap + synchronized List)   │   ║
║   │   Order subclasses: LabOrder | MedicationOrder | ImagingOrder│   ║
║   │   CommandLogEntry list                                       │   ║
║   └──────────────────────────────────────────────────────────────┘   ║
╚══════════════════════════════════════════════════════════════════════╝

╔══════════════════════════════════════════════════════════════════════╗
║                       UTILITY LAYER                                  ║
║                                                                      ║
║   ┌──────────────────────────────────────────────────────────┐       ║
║   │  NotificationService «interface»                         │       ║
║   │    notify(Order, String event)                           │       ║
║   │  ConsoleNotificationService (Week 1 implementation)      │       ║
║   └──────────────────────────────────────────────────────────┘       ║
╚══════════════════════════════════════════════════════════════════════╝
```

---

## 2. Call Chain Diagrams

### Use Case 1 — Submit an Order

```
Clinician (Browser)
      │
      │  POST /api/orders  {type, patientName, clinician, description, priority}
      ▼
OrderController                                          [CLIENT]
      │  orderManager.submitOrder(type, patient, clinician, desc, priority)
      ▼
OrderManager                                             [MANAGER]
      │  1. orderFactory.createOrder(type, ...)
      ▼
OrderFactory                                             [BUSINESS LOGIC]
      │     looks up registry map → new LabOrder / MedicationOrder / ImagingOrder
      │     returns Order
      ◄─────────────────────────────────────────────────────────
      │
      │  2. new SubmitOrderCommand(order, orderAccess, triagingEngine, eventPublisher)
      │  3. dispatch(cmd)  →  cmd.execute()
      ▼
SubmitOrderCommand.execute()                             [COMMAND]
      │  a. AuditLoggingDecorator
      │       → ValidationDecorator
      │           → BaseOrderHandler.handle(order)       [DECORATOR CHAIN]
      │  b. orderAccess.saveOrder(order)
      ▼
OrderAccess                                              [RESOURCE ACCESS]
      │     store.put(orderId, order)
      ▼
In-Memory Store                                          [RESOURCE]
      ◄─────────────────────────────────────────────────────────
      │  c. triagingEngine.requeue()
      │  d. eventPublisher.publish(order, "ORDER_SUBMITTED")
      ▼
OrderEventPublisher → NotificationObserver               [OBSERVER]
      │                    → notificationService.notify(order, event)
      ▼
ConsoleNotificationService                               [UTILITY]
      │     prints structured log line
      ◄─────────────────────────────────────────────────────────
      │  4. commandLogAccess.append(new CommandLogEntry("SUBMIT", orderId, actor))
      ▼
CommandLogAccess                                         [RESOURCE ACCESS]
      │     log.add(entry)
      ◄─────────────────────────────────────────────────────────
OrderManager returns Order
OrderController returns 200 OK  {OrderDto}
      ▼
Clinician sees confirmation message
```

---

### Use Case 2 — Fulfil an Order (Claim → Complete)

```
Staff Member (Browser)
      │
      │  POST /api/orders/{id}/claim  {actor: "Tech-1"}
      ▼
OrderController                                          [CLIENT]
      │  orderManager.claimOrder(orderId, staffMember)
      ▼
OrderManager                                             [MANAGER]
      │  new ClaimOrderCommand(orderId, staffMember, orderAccess, eventPublisher)
      │  dispatch(cmd)  →  cmd.execute()
      ▼
ClaimOrderCommand.execute()                              [COMMAND]
      │  a. orderAccess.findOrderById(orderId)
      ▼
OrderAccess                                              [RESOURCE ACCESS]
      │     store.get(orderId) → Order
      ◄─────────────────────────────────────────────────────────
      │  b. guard: status must be PENDING, claimedBy must be null
      │  c. order.setStatus(IN_PROGRESS), order.setClaimedBy("Tech-1")
      │  d. orderAccess.saveOrder(order)
      │  e. eventPublisher.publish(order, "ORDER_CLAIMED")
      ▼
ConsoleNotificationService                               [UTILITY]
      │     prints structured log line
      ◄─────────────────────────────────────────────────────────
      │  f. commandLogAccess.append(CommandLogEntry("CLAIM", orderId, "Tech-1"))
      ◄─────────────────────────────────────────────────────────
OrderController returns 200 OK

      ── (Staff clicks Complete) ──────────────────────────────

      │  POST /api/orders/{id}/complete  {actor: "Tech-1"}
      ▼
OrderController → OrderManager                           [CLIENT → MANAGER]
      │  new CompleteOrderCommand(orderId, "Tech-1", orderAccess, eventPublisher)
      │  dispatch(cmd)  →  cmd.execute()
      ▼
CompleteOrderCommand.execute()                           [COMMAND]
      │  a. orderAccess.findOrderById(orderId) → Order
      │  b. guard: status must be IN_PROGRESS, claimedBy must equal "Tech-1"
      │  c. order.setStatus(COMPLETED)
      │  d. orderAccess.saveOrder(order)
      │  e. eventPublisher.publish(order, "ORDER_COMPLETED")
      │  f. commandLogAccess.append(CommandLogEntry("COMPLETE", orderId, "Tech-1"))
      ◄─────────────────────────────────────────────────────────
OrderController returns 200 OK
      ▼
Staff Member sees order disappear from their queue (next 3-second poll)
```

---

## 3. Design Pattern Justifications

| Pattern | Classes Involved | Layer | One-Sentence Justification |
|---|---|---|---|
| **Strategy** | `TriageStrategy` (interface), `PriorityFirstTriageStrategy` | Business Logic | Encapsulates the triage algorithm behind a common interface so a new scheduling policy (e.g. round-robin, department-based) can be swapped at runtime by calling `TriagingEngine.setTriageStrategy()` without touching any other class. |
| **Observer** | `OrderObserver` (interface), `OrderEventPublisher`, `NotificationObserver` | Business Logic | Decouples order state changes from the components that react to them so a new reaction (e.g. WebSocket push, email alert) can be added in Week 2 by creating a single `@Component` class with zero changes to `OrderManager` or the command objects. |
| **Decorator** | `OrderHandler` (interface), `BaseOrderHandler`, `OrderHandlerDecorator`, `ValidationDecorator`, `AuditLoggingDecorator` | Business Logic | Stacks order-processing steps (validation, audit logging) transparently around a base handler so each step can be added, removed, or reordered without modifying the handler it wraps or the `SubmitOrderCommand` that builds the chain. |
| **Factory** | `OrderFactory`, `OrderCreator` (functional interface), `LabOrder`, `MedicationOrder`, `ImagingOrder` | Business Logic | Centralises order subtype creation in a registry map so callers (`OrderManager`) are fully decoupled from concrete classes and a new order type requires adding only one entry to `OrderFactory` with no changes elsewhere. |
| **Command** | `OrderCommand` (interface), `SubmitOrderCommand`, `ClaimOrderCommand`, `CompleteOrderCommand`, `CancelOrderCommand`, `UndoableCommandDecorator` | Business Logic | Encapsulates each order action as a self-contained object carrying all required data so `OrderManager` can dispatch, log, and undo any action; in Week 2 a single `UndoableCommandDecorator` wraps any command at the dispatch site, adding undo without touching any command class. |

---

## 4. Week 2 Changes — File-Count Audit

| Change | Pre-existing files modified | New files added |
|---|---|---|
| Change 1 — Department-Aware Triage | 1 (`OrderManager`) | `LoadBalancingTriageStrategy`, `DeadlineFirstTriageStrategy`, `TriageController`, `StrategyConfig`, `ClockConfig` |
| Change 2a — Multi-Channel Notifications | 0 | `NotificationPreferences`, `InAppNotificationService`, `EmailNotificationService`, `CompositeNotificationService`, `NotificationController` |
| Change 2b — Order Processing Decorators | 1 (`Order.setPriority`) | `PriorityEscalationDecorator`, `StatAuditDecorator`, `StatAwareSubmitOrderCommand` |
| Change 3 — Command Undo and Replay | 1 (`OrderAccess.deleteOrder`; `OrderManager` already counted in Change 1) | `UndoableCommandDecorator`, `UndoController` |

`StrategyConfig` (new) injects and re-exposes the existing `PriorityFirstTriageStrategy` component as `@Primary`, eliminating the need to touch that file. `StatAwareSubmitOrderCommand` (new) carries the Week 2 decorator chain, so `SubmitOrderCommand` remains at its Week 1 state. `OrderManager` is counted once under Change 1 since all four changes' additions landed in a single file.

---

## 5. Week 2 — Volatility Missed in Week 1 & What I Would Do Differently

**Volatility missed:** The `Order` domain model had `priority` declared `final`, assuming priority was immutable after creation. The `PriorityEscalationDecorator` (Change 2b) needs to mutate priority at processing time, which required removing `final` and adding `setPriority()`. A better Week 1 design would have treated priority as mutable from the start — or introduced a separate `ProcessedOrder` value object that carries the resolved priority — so the domain model would not need touching for a processing-layer concern.

**What I would do differently:** Separate the "submitted priority" (immutable intent from the clinician) from the "effective priority" (possibly escalated by the decorator chain) using a wrapper or a dedicated field on the command, keeping `Order` fully immutable below the business-logic layer.

---

## 6. Week 2 — Pattern Audit (Two Sentences per Pattern)

| Pattern | Audit |
|---|---|
| **Strategy** | Adding `LoadBalancingTriageStrategy` and `DeadlineFirstTriageStrategy` required zero changes to `TriagingEngine` or `OrderManager` — a new `StrategyConfig` bean and a selector endpoint were sufficient. The strategy interface absorbed the entire Change 1 volatility as intended. |
| **Observer** | Adding `InAppNotificationService`, `EmailNotificationService`, and the `CompositeNotificationService` required zero changes to `OrderManager`, `OrderEventPublisher`, or any command class. `@Primary` on `CompositeNotificationService` redirected the existing observer chain without touching `NotificationObserver`. |
| **Decorator** | Stacking `PriorityEscalationDecorator` and `StatAuditDecorator` required zero changes to `SubmitOrderCommand` — a new `StatAwareSubmitOrderCommand` carries the extended chain and `OrderManager` dispatches it instead. The `OrderHandler` interface and all existing decorators were untouched. |
| **Factory** | `OrderFactory` was untouched across both weeks; no new order types were introduced in Week 2, confirming the registry map isolates callers from concrete classes as designed. |
| **Command** | `UndoableCommandDecorator` delivered Change 3 (undo + replay) by wrapping commands at the `OrderManager.dispatch()` site — zero individual command classes were modified. The Command pattern's object-encapsulation paid off exactly as the spec predicted. |

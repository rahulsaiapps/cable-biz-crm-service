# Cable Pulse CRM — API Contract Specification

**Base URL (Production):** `https://cable-biz-crm-service.onrender.com/api/v1`

**Source of truth:** This document is generated from the Spring Boot controllers in `cable-crm-service` (`*Controller.java`). When code and docs disagree, trust the controllers.

**Flutter client:** `cable-crm` (`lib/src/data/repositories/`). Each endpoint below notes where the mobile app calls it, when applicable.

---

## Conventions

### Authentication

All routes except **`/api/v1/auth/**`** require a Firebase ID token:

```
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

`FirebaseAuthenticationFilter` verifies the token and assigns Spring Security roles:

| Role | Meaning |
|---|---|
| `ROLE_OWNER` | Operator / admin — full workspace control |
| `ROLE_COLLECTION_BOY` | Field staff — limited RBAC (e.g. no financial roll-ups) |

Roles are resolved from Firebase custom claims (`role`, `owner`, `collection_boy`), then from the persisted `employees` row for the Firebase UID. If no employee row exists or reconciliation fails, the user receives **`ROLE_COLLECTION_BOY`** (least privilege — never `ROLE_OWNER`). On first sign-in, a `PENDING-*` placeholder row can be claimed by matching email during `POST /auth/token-swap`.

**Note:** Spring Security may return **403 Forbidden** (sometimes with an empty body) when the token is missing, invalid, or lacks the required role — not always 401.

### Role-gated routes

| Pattern | Access |
|---|---|
| `/api/v1/auth/**` | Public (`permitAll`) |
| `PATCH /api/v1/employees/profile` | Any authenticated user |
| `DELETE /api/v1/employees/profile` | Any authenticated user |
| `/api/v1/plans`, `/api/v1/plans/**` | `ROLE_OWNER` only |
| `/api/v1/employees/**` (except profile PATCH/DELETE) | `ROLE_OWNER` only |
| `DELETE /api/v1/workspace/territories/{id}` | `ROLE_OWNER` only (`@PreAuthorize`) |
| All other authenticated routes | Any authenticated role |

### Tracing headers

`WebHeaderInterceptor` requires these on **every `/api/v1/**` route except `/api/v1/auth/**`**:

| Header | Required | Format |
|---|---|---|
| `X-E2E-ID` | Yes | Valid UUID |
| `X-Session-ID` | Yes | Valid UUID |

Missing or blank `X-E2E-ID` → **400**. Malformed `X-Session-ID` → **401**.

### Standard response envelope

Most endpoints return:

```json
{
  "timestamp": "2026-06-10T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": { }
}
```

On error: `status` is `"ERROR"`, `error` contains a message, `data` is usually `null`. Duplicate-resource handlers may return the existing entity in `data` with HTTP **409**.

### JSON field naming

Request/response bodies use **snake_case** where annotated with `@JsonProperty` (e.g. `full_name`, `location_name`, `channels_text`). Some DTOs also accept **camelCase aliases** (e.g. `monthsPaid`, `tier_name` / `tierName`). Send plaintext JSON — do not wrap bodies in encrypted envelopes.

**Finance analytics routes** (`GET /finance/metrics`, `/expenses`, `/performance`, `/disbursements`, `/health`) and **`GET /alerts/target-size`** return **flat JSON** (no standard envelope) to match existing Flutter parsers.

---

## Quick reference

| # | Method | Path | Auth | Flutter consumer |
|---|---|---|---|---|
| 1 | POST | `/auth/token-swap` | Public | `auth_repository.dart` |
| 2 | GET | `/auth/health` | Public | `login_page.dart` |
| 3 | GET | `/dashboard/metrics` | Bearer | `dashboard_repository.dart` |
| 4 | POST | `/customers` | Bearer | `customers_repository.dart` |
| 5 | GET | `/customers/{id}` | Bearer | `customer_ledger_repository.dart` |
| 6 | GET | `/customers/{id}/ledger` | Bearer | `customer_ledger_repository.dart` |
| 7 | POST | `/customers/{id}/payments` | Bearer | `customer_ledger_repository.dart` |
| 8 | PUT | `/customers/{id}/subscription` | Bearer | `customer_ledger_repository.dart` |
| 9 | GET | `/customers/search` | Bearer | `customers_repository.dart` *(unwired)* |
| 10 | GET | `/workspace/territories` | Bearer | `dashboard_repository.dart`, `territory_repository.dart` |
| 11 | GET | `/workspace/territories/active-locations` | Bearer | `territory_repository.dart`, `dashboard_repository.dart` |
| 12 | GET | `/workspace/territories/{id}/blocks` | Bearer | `workspace_repository.dart` |
| 13 | DELETE | `/workspace/territories/{id}` | Bearer + OWNER | `territory_repository.dart` |
| 14 | GET | `/workspace/customers` | Bearer | `workspace_repository.dart` |
| 15 | GET | `/workspace/providers` | Bearer | `plans_repository.dart`, `workspace_repository.dart` |
| 16 | POST | `/workspace/providers` | Bearer | `territory_repository.dart`, `plans_repository.dart` |
| 17 | GET | `/plans` | Bearer + OWNER | `plans_repository.dart` |
| 18 | POST | `/plans` | Bearer + OWNER | `plans_repository.dart` |
| 19 | DELETE | `/plans/{id}` | Bearer + OWNER | `plans_repository.dart` |
| 20 | GET | `/employees` | Bearer + OWNER | `employee_repository.dart` |
| 21 | POST | `/employees` | Bearer + OWNER | `employee_repository.dart` |
| 22 | PATCH | `/employees/profile` | Bearer | `employee_repository.dart` (Settings) |
| 23 | DELETE | `/employees/profile` | Bearer | `settings_view_model.dart` *(stub)* |
| 24 | GET | `/saas/pricing` | Bearer | `saas_pricing_repository.dart` |
| 25 | POST | `/saas/upgrade-intent` | Bearer | `saas_pricing_repository.dart` |
| 26 | POST | `/sync/synchronize` | Bearer | `sync_manager.dart` *(unwired)* |
| 27 | POST | `/transactions/expense` | Bearer | `finance_repository.dart` |
| 28 | POST | `/transactions/isp-settlement` | Bearer | `finance_repository.dart` |
| 29 | GET | `/transactions/daily-summary` | Bearer | `daily_ledger_repository.dart`, `finance_repository.dart` |
| 30 | GET | `/finance/daily-ledger` | Bearer | `daily_ledger_repository.dart`, `finance_repository.dart` |
| 31 | POST | `/finance/daily-ledger/transactions` | Bearer | `daily_ledger_repository.dart` |
| 32 | GET | `/finance/metrics` | Bearer | `finance_repository.dart` |
| 33 | GET | `/finance/expenses` | Bearer | `finance_repository.dart` |
| 34 | GET | `/finance/performance` | Bearer | `finance_repository.dart` |
| 35 | GET | `/finance/disbursements` | Bearer | `finance_repository.dart` |
| 36 | GET | `/finance/health` | Bearer | `finance_repository.dart` |
| 37 | POST | `/broadcasts/pending-reminder` | Bearer | `dashboard_repository.dart` |
| 38 | POST | `/broadcasts/active-reminder` | Bearer | `dashboard_repository.dart` |
| 39 | POST | `/bulletins/outage` | Bearer | `dashboard_repository.dart` *(dead)* |
| 40 | POST | `/notifications/broadcast-outage` | Bearer | `alerts_repository.dart` |
| 41 | POST | `/notifications/dispatched-alert` | Bearer | `alerts_repository.dart`, `customer_ledger_repository.dart` |
| 42 | POST | `/notifications/dispatch-alert` | Bearer | `notification_service.dart` *(unwired)* |
| 43 | GET | `/alerts/target-size` | Bearer | `alerts_repository.dart` |

---

## 1. Auth (`/api/v1/auth`)

Public module — no Bearer token or trace headers required.

### 1.1 POST `/api/v1/auth/token-swap`

**Purpose:** Exchange a client-side Firebase ID token for a session payload (access token + user profile). Called once after Google/Firebase sign-in.

**Flutter:** `auth_repository.dart` → `AuthProvider.signIn()`.

**Request body:**
```json
{
  "firebaseIdToken": "eyJhbGciOiJSUzI1NiIs..."
}
```

**Success (200):**
```json
{
  "timestamp": "2026-06-10T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "accessToken": "<same Firebase JWT echoed back>",
    "accessTokenExpiresInSeconds": 3600,
    "refreshToken": "mock-refresh-token",
    "userProfile": {
      "userId": "firebase-uid-abc123",
      "fullName": "Rahul Sai",
      "role": "ROLE_OWNER"
    }
  }
}
```

**Errors:** `400` — token verification failed.

**Notes:** Resolves `Employee` by Firebase UID; if missing, attempts email-based reconciliation of `PENDING-*` rows created via `POST /employees`.

---

### 1.2 GET `/api/v1/auth/health`

**Purpose:** Lightweight liveness probe before login or for uptime monitors.

**Flutter:** `login_page.dart` warm-up ping.

**Request body:** None.

**Success (200):**
```json
{
  "status": "UP",
  "timestamp": "2026-06-10T10:15:30.123",
  "platform": "Cable Pulse Utility Backend"
}
```

**Notes:** Does not use the standard `{ status, data, error }` envelope.

---

## 2. Dashboard (`/api/v1/dashboard`)

### 2.1 GET `/api/v1/dashboard/metrics`

**Purpose:** Home-screen aggregate metrics — customer counts and (for owners) a financial summary for the current billing cycle.

**Flutter:** `dashboard_repository.dart` → `DashboardViewModel` / `MetricsGrid`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Request body:** None.

**Success (200):**
```json
{
  "timestamp": "2026-06-10T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "customerSummary": {
      "totalCustomers": 482,
      "pendingCustomers": 37
    },
    "financialSummary": {
      "amountPaid": 12850.00,
      "amountPending": 4200.00,
      "currency": "INR",
      "billingCyclePeriod": "JUNE-2026"
    }
  }
}
```

**Computation (server-side aggregates — not hardcoded):**

| Field | Source |
|---|---|
| `totalCustomers` | `COUNT(*)` on `customers` |
| `pendingCustomers` | Distinct customers with incomplete activation (`plan_id` and `custom_rate_override` both null) **or** any ledger row with `due_amount > 0` |
| `amountPaid` | `SUM(amount_collected)` from `daily_transactions` for the current calendar month |
| `amountPending` | `SUM(due_amount)` from `customer_ledgers` for the current billing month/year where `due_amount > 0` |
| `billingCyclePeriod` | `{MONTH}-{YEAR}` derived from server clock (e.g. `JUNE-2026`) |
| `currency` | Always `INR` |

**RBAC:** `financialSummary` is **`null`** for `ROLE_COLLECTION_BOY`.

---

## 3. Customers (`/api/v1/customers`)

### 3.1 POST `/api/v1/customers`

**Purpose:** Register a new subscriber/customer from the Add Customer form.

**Flutter:** `customers_repository.dart` → `AddNewCustomerScreen`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`, `Content-Type: application/json`.

**Request body:**
```json
{
  "name": "Satish Kumar",
  "territory_id": "ter_abc123",
  "territory_name": "Kolamuru",
  "phone_number": "9876543210",
  "street": "Ramalayam Street",
  "door_number": "12-34",
  "plan_name": "Gold HD Pack",
  "plan_monthly_rate": 350.00,
  "box_number": "BX-001",
  "card_number": "CR-9912",
  "connection_type": "CABLE"
}
```

| Field | Required | Notes |
|---|---|---|
| `name` | Yes | Customer full name |
| `territory_id` | Yes | Must reference an existing territory |
| `territory_name` | No | Display hint; not authoritative |
| `phone_number`, `street`, `door_number` | No | Address/contact |
| `plan_name`, `plan_monthly_rate` | `plan_monthly_rate` required | Rate ≥ 0 |
| `box_number`, `card_number`, `connection_type` | No | Hardware tracking |

**Success (201):**
```json
{
  "timestamp": "2026-06-10T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "newCustomerId": "cust_generated_uuid"
  }
}
```

**Errors:** `404` — territory not found. `400` — validation failure.

---

### 3.2 GET `/api/v1/customers/{id}`

**Purpose:** Customer profile hero card for the Customer Ledger screen (name, plan, balance, contact).

**Flutter:** `customer_ledger_repository.dart` → `fetchCustomerProfile()`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Path param:** `id` — customer ID.

**Success (200):**
```json
{
  "timestamp": "2026-06-10T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "customerId": "cust-001",
    "fullName": "Satish Kumar",
    "doorNumber": "12-34",
    "streetName": "Ramalayam Street",
    "territory_id": "ter_abc123",
    "territory_name": "Kolamuru",
    "activePlanName": "Gold HD Pack",
    "monthlyRate": 350.00,
    "paymentStatus": "UNPAID",
    "balanceDue": 700.00,
    "phone_number": "9876543210"
  }
}
```

**Errors:** `404` — customer not found.

**Notes:** `paymentStatus` is `UNPAID` when the customer's total outstanding ledger `due_amount` sum is greater than zero; otherwise `PAID`. `balanceDue` is that aggregated ledger sum (not the monthly plan rate).

**Flutter mapping:** `Customer.fromJson()` accepts `customerId`/`fullName`/`streetName`/`activePlanName`/`monthlyRate`/`paymentStatus`/`balanceDue`/`phone_number`.

---

### 3.3 GET `/api/v1/customers/{id}/ledger`

**Purpose:** Monthly billing ledger for the Customer Ledger screen (payment history, balance due).

**Flutter:** `customer_ledger_repository.dart` → `fetchPaymentLedger()`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Path param:** `id` — customer ID.

**Query params:** `year` — optional; sent by Flutter but **ignored** by server (all ledger rows returned).

**Success (200):**
```json
{
  "data": {
    "customerId": "cust-001",
    "fullName": "Satish Kumar",
    "totalBalanceDue": 700.00,
    "ledger": [
      {
        "month": "JUN",
        "year": 2026,
        "status": "UNPAID",
        "paidAmount": 0.00,
        "dueAmount": 350.00
      }
    ]
  }
}
```

**Notes:** `month` may be 3-letter (`JUN`) or full (`JUNE`). Flutter `PaymentRecord._monthToInt` should handle both.

---

### 3.4 POST `/api/v1/customers/{id}/payments`

**Purpose:** Record a cash/UPI payment against one or more billing months.

**Flutter:** `customer_ledger_repository.dart` → `collectPayment()`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`, `Content-Type: application/json`.

**Request body:**
```json
{
  "amount": 500,
  "monthsPaid": ["JAN", "FEB"],
  "year": 2026,
  "transaction_ref": "optional-upi-ref"
}
```

| Field | Required | Notes |
|---|---|---|
| `amount` | Yes | Positive integer (rupees) |
| `monthsPaid` | Yes | 3-letter month abbreviations (`JAN`…`DEC`) or full names |
| `year` | No | Defaults to current year |
| `payment_mode` | No | `CASH` or `ONLINE_UPI` / `UPI` |
| `transaction_ref` | No | UPI reference |

**Aliases:** `months_paid` accepted instead of `monthsPaid`.

**Success (201):** Standard envelope, `data: null`.

**Errors:** `404` — customer not found. `400` — validation failure.

**Side effects:** Updates `customer_ledgers` rows and inserts a `daily_transactions` row attributed to the authenticated employee.

---

### 3.5 PUT `/api/v1/customers/{id}/subscription`

**Purpose:** Change the customer's active plan and monthly rate.

**Flutter:** `customer_ledger_repository.dart` → `updateSubscriptionPlan()`.

**Request body:**
```json
{
  "plan_name": "Premium HD",
  "plan_monthly_rate": 350
}
```

**Success (200):** Returns updated profile in `data` (same shape as §3.2).

**Notes:** If `plan_name` does not match an existing `global_plans` row, server creates an ad-hoc plan record.

---

### 3.6 GET `/api/v1/customers/search`

**Purpose:** Typeahead search when finding customers by name (optional block filter).

**Flutter:** `customers_repository.dart`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Query params:**

| Param | Required | Description |
|---|---|---|
| `name` | Yes | Substring match on full name |
| `block` | No | Further filter by block/street |

**Success (200):**
```json
{
  "data": [
    {
      "customerId": "cust-001",
      "label": "1. Satish Kumar (Ramalayam Street)"
    }
  ]
}
```

---

## 4. Workspace (`/api/v1/workspace`)

Territory management, field-collection customer roster, and ISP provider categories share this module.

### 4.1 GET `/api/v1/workspace/territories`

**Purpose:** List all territories with customer counts for the home dashboard territory cards.

**Flutter:** `dashboard_repository.dart`, `territory_repository.dart` (`fetchTerritories()`).

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Success (200):**
```json
{
  "data": [
    {
      "territory_id": "ter_abc123",
      "location_name": "Kolamuru",
      "customer_count": 12,
      "active_count": 12,
      "pending_count": 0
    }
  ]
}
```

---

### 4.2 GET `/api/v1/workspace/territories/active-locations`

**Purpose:** Distinct active location **names** only — lightweight list for dropdowns (e.g. Add Team Member village picker).

**Flutter:** `territory_repository.dart` (`fetchActiveTerritoryNames()`), `dashboard_repository.dart` (fallback).

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Success (200):**
```json
{
  "data": ["Kolamuru", "Diwancheruvu", "Hukumpeta"]
}
```

---

### 4.3 GET `/api/v1/workspace/territories/{id}/blocks`

**Purpose:** Block/street names within a territory for the Add Customer block dropdown.

**Flutter:** `workspace_repository.dart` (`fetchBlockNames()`).

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Path param:** `id` — territory ID.

**Success (200):**
```json
{
  "data": ["Ramalayam Street", "School Road", "Ganga Lane"]
}
```

**Errors:** `404` — territory not found.

---

### 4.4 DELETE `/api/v1/workspace/territories/{id}`

**Purpose:** Soft-delete a territory (sets `is_deleted = true`).

**Flutter:** `territory_repository.dart` → `DeleteTerritoryDialog`.

**Access:** `ROLE_OWNER` only.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Success (200):** Standard envelope, `data: null`.

**Errors:** `404` — territory not found.

---

### 4.5 GET `/api/v1/workspace/customers`

**Purpose:** Field-collection workspace view — all customers in a territory with plan, rate, and ledger-derived payment status.

**Flutter:** `workspace_repository.dart` → `WorkspaceScreen` / `WorkspaceViewModel`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Query params:**

| Param | Required | Description |
|---|---|---|
| `locationId` | Yes | Territory ID |

**Success (200):**
```json
{
  "data": {
    "locationId": "ter_abc123",
    "locationName": "Kolamuru",
    "customers": [
      {
        "customerId": "cust-001",
        "serialNumber": 1,
        "fullName": "Satish Kumar",
        "doorNumber": "12-34",
        "streetName": "Ramalayam Street",
        "activePlanName": "Gold HD Pack",
        "monthlyRate": 350.00,
        "paymentStatus": "PAID",
        "balanceDue": 0.00,
        "connectionType": "CABLE",
        "boxNumber": "BX-001",
        "cardNumber": "CR-9912"
      }
    ]
  }
}
```

**Notes:** `paymentStatus` and `balanceDue` are computed per customer from aggregated `customer_ledgers.due_amount` (batch query — no per-row defaults). `UNPAID` when total due > 0; `PAID` when zero. Example above shows a fully paid customer; an overdue customer would show `"paymentStatus": "UNPAID"` and a positive `balanceDue`.

---

### 4.6 GET `/api/v1/workspace/providers`

**Purpose:** List ISP / connection-provider **categories** used to group subscription plans (e.g. "Skynet Cable Networks").

**Flutter:** `plans_repository.dart` (category chips), `workspace_repository.dart`, `dashboard_repository.dart` (legacy fallback).

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Success (200):**
```json
{
  "data": [
    {
      "id": 1,
      "name": "Skynet Cable Networks",
      "registeredAt": "2026-01-15T09:30:00"
    }
  ]
}
```

**Notes:** This lists `connection_providers` rows — **not** territory locations. For territories use §4.1 or §4.2.

---

### 4.7 POST `/api/v1/workspace/providers`

**Purpose:** Dual-purpose create endpoint — dispatches by body shape:

| Body contains | Creates | Used for |
|---|---|---|
| `location_name` (+ optional `blocks`) | **Territory** | Onboarding a village/location |
| `name` only (no `location_name`) | **ISP category** | Plan Management provider chips |

**Flutter:**
- Territory save → `territory_repository.dart` (`createTerritory()`)
- ISP category → `plans_repository.dart` (`createCategory()`)

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`, `Content-Type: application/json`.

#### Territory onboarding body
```json
{
  "location_name": "Kolamuru",
  "blocks": ["Ramalayam Street", "School Road"]
}
```

| Field | Required | Max length |
|---|---|---|
| `location_name` | Yes (territory flow) | 100 |
| `blocks` | No | Each entry ≤ 100 chars |

**Success (201) — territory:**
```json
{
  "data": {
    "territory_id": "ter_abc123",
    "location_name": "Kolamuru",
    "customer_count": 0,
    "active_count": 0,
    "pending_count": 0
  }
}
```

**Conflict (409):** Duplicate `location_name` — `error`: `"Territory already exists"`, existing territory in `data`. Flutter should auto-select the existing location.

#### ISP category body
```json
{
  "name": "Skynet Cable Networks"
}
```

| Field | Required | Max length |
|---|---|---|
| `name` | Yes (category flow) | 50 |

**Success (201) — provider:**
```json
{
  "data": {
    "id": 3,
    "name": "Skynet Cable Networks",
    "registeredAt": "2026-06-10T10:15:30"
  }
}
```

**Conflict (409):** Duplicate `name` — `error`: `"Provider category already exists"`, existing provider in `data`.

**Errors:** `400` — blank `name` (category flow) or validation failure.

---

## 5. Plans (`/api/v1/plans`) — `ROLE_OWNER` only

### 5.1 GET `/api/v1/plans`

**Purpose:** List subscription plans for a given ISP provider category.

**Flutter:** `plans_repository.dart` → `PlanManagementScreen` (requires `providerName` query param).

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Query params:**

| Param | Required | Description |
|---|---|---|
| `providerName` | Yes | Exact `connection_providers.name` match |

**Success (200):**
```json
{
  "data": [
    {
      "id": "plan-abc123",
      "name": "Gold HD Pack",
      "price": 350.00,
      "channels_text": "120+ Channels, HD Support"
    }
  ]
}
```

**Notes:** Response uses `id` and `channels_text` (not `planId` / `details`). Flutter `PricingPlan.fromJson` accepts both conventions.

---

### 5.2 POST `/api/v1/plans`

**Purpose:** Create a new global subscription plan under a provider category.

**Flutter:** `plans_repository.dart` → `PlansViewModel.createPlan()`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`, `Content-Type: application/json`.

**Request body:**
```json
{
  "name": "Gold HD Pack",
  "price": 350,
  "channels_text": "120+ Channels, HD Support",
  "is_hd": true,
  "provider": "Skynet Cable Networks"
}
```

| Field | Required | Notes |
|---|---|---|
| `name` | Yes | Plan display name |
| `price` | Yes | Positive integer monthly rate |
| `provider` | Yes | Must match an existing provider `name` |
| `channels_text` | No | Free-text channel/details description |
| `is_hd` | No | Defaults `false` |

**Success (201):**
```json
{
  "data": {
    "createdPlanId": "plan-uuid-abc"
  }
}
```

**Errors:** `404` — provider category not found.

---

### 5.3 DELETE `/api/v1/plans/{id}`

**Purpose:** Remove a subscription plan from the catalog.

**Flutter:** `plans_repository.dart` → `deletePlan()`.

**Access:** `ROLE_OWNER` only.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Path param:** `id` — plan ID (`global_plans.plan_id`).

**Success (200):** Standard envelope, `data: null`.

**Errors:** `404` — plan not found.

---

## 6. Employees (`/api/v1/employees`)

Admin team management (`ROLE_OWNER`) plus self-service profile edit (any authenticated user).

### 6.1 GET `/api/v1/employees`

**Purpose:** Roster of all employees for the Team Management screen.

**Flutter:** `employee_repository.dart` → `EmployeeViewModel.loadEmployees()`.

**Access:** `ROLE_OWNER` only.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Success (200):**
```json
{
  "data": [
    {
      "employee_id": "emp-001",
      "full_name": "Ramesh Kumar",
      "role": "COLLECTION_BOY",
      "email": "ramesh@example.com",
      "assigned_villages": [],
      "today_collection": 0,
      "phone_number": null
    }
  ]
}
```

**Notes:** `assigned_villages` is persisted (see §6.2). `today_collection` and `phone_number` remain placeholders (`0`/`null`).

---

### 6.2 POST `/api/v1/employees`

**Purpose:** Pre-provision a team member before their first Firebase sign-in. Creates a `PENDING-*` `employee_id` linked on first `token-swap` via email.

**Flutter:** `employee_repository.dart` → Add Team Member form.

**Access:** `ROLE_OWNER` only.

**Request body:**
```json
{
  "full_name": "Ramesh Kumar",
  "role": "COLLECTION_BOY",
  "email": "ramesh@example.com",
  "assigned_villages": ["Kolamuru", "Diwancheruvu"]
}
```

| Field | Required | Values |
|---|---|---|
| `full_name` | Yes | Display name |
| `role` | Yes | `OWNER` or `COLLECTION_BOY` |
| `email` | No | Used for first-sign-in reconciliation |
| `assigned_villages` | No | Territory location names to assign |

**Success (201):**
```json
{
  "data": {
    "employee_id": "PENDING-uuid",
    "full_name": "Ramesh Kumar",
    "role": "COLLECTION_BOY",
    "email": "ramesh@example.com",
    "assigned_villages": [],
    "today_collection": 0,
    "phone_number": null
  }
}
```

---

### 6.3 PATCH `/api/v1/employees/profile`

**Purpose:** Signed-in user updates their own profile (name, email, description). Target resolved from Firebase UID on the token — no ID in the path.

**Flutter:** `employee_repository.dart` → `SettingsViewModel.saveProfile()`.

**Access:** Any authenticated user (including `COLLECTION_BOY`).

**Request body** (send only changed fields):
```json
{
  "full_name": "Updated Name",
  "email": "user@example.com",
  "description": "Operator caption text"
}
```

**Success (200):**
```json
{
  "data": {
    "employee_id": "firebase-uid-123",
    "full_name": "Updated Name",
    "email": "user@example.com",
    "description": "Operator caption text",
    "role": "OWNER"
  }
}
```

**Errors:** `404` — no employee row for Firebase UID. `400` — e.g. blank `full_name`.

---

### 6.4 DELETE `/api/v1/employees/profile`

**Purpose:** Delete the signed-in user's employee record (account deletion from Settings).

**Flutter:** `settings_view_model.dart` → `deleteAccount()` *(currently local stub)*.

**Access:** Any authenticated user.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Request body:** None.

**Success (200):** Standard envelope, `data: null`.

**Errors:** `404` — no employee row for Firebase UID.

**Flutter action:** Call this endpoint, then `AuthProvider.signOut()`.

---

## 7. SaaS pricing (`/api/v1/saas`)

### 7.1 GET `/api/v1/saas/pricing`

**Purpose:** Platform subscription tier matrix for the in-app App Subscription Plans screen.

**Flutter:** `saas_pricing_repository.dart` → `AppSubscriptionPlansPage`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Optional header:** `X-User-Country-Code` (default `IN`).

**Success (200):**
```json
{
  "data": {
    "promotionalTrialActive": true,
    "trialEndsAt": "2026-09-10T10:15:30",
    "currencyCode": "INR",
    "tiers": [
      {
        "tierName": "Pro",
        "billingCycle": "MONTHLY",
        "retailPrice": 999.0,
        "discountedPrice": 799.0
      }
    ]
  }
}
```

---

### 7.2 POST `/api/v1/saas/upgrade-intent`

**Purpose:** Record that the operator initiated a paid SaaS upgrade (analytics / billing workflow).

**Flutter:** `saas_pricing_repository.dart`.

**Request body** (camelCase or snake_case):
```json
{
  "tierName": "Pro",
  "billingCycle": "MONTHLY",
  "amount": 799.0
}
```

**Snake_case alias (Flutter today):**
```json
{
  "tier_name": "Pro",
  "billing_cycle": "MONTHLY",
  "amount": 799.0
}
```

**Success (201):**
```json
{
  "data": {
    "id": 42
  }
}
```

---

## 8. Transactions & sync

### 8.1 POST `/api/v1/sync/synchronize`

**Purpose:** Flush the offline sync outbox — batch upload of queued field events after connectivity returns.

**Flutter:** `sync_manager.dart`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`, `Content-Type: application/json`.

**Request body:**
```json
{
  "syncBatchId": "550e8400-e29b-41d4-a716-446655440000",
  "events": [
    {
      "eventId": "evt-001",
      "actionType": "RECORD_PAYMENT",
      "idempotencyToken": "550e8400-e29b-41d4-a716-446655440001",
      "payload": { }
    }
  ]
}
```

**Success (200):**
```json
{
  "data": {
    "syncResolution": {
      "processedEventIds": ["evt-001"],
      "rejectedEventIds": []
    }
  }
}
```

---

### 8.2 POST `/api/v1/transactions/expense`

**Purpose:** Log a daily business expense (Daily Book / Finance).

**Flutter:** `finance_repository.dart`.

**Request body** (entity fields):
```json
{
  "amount": 500.00,
  "description": "Cable wire maintenance",
  "expenseCategory": "MAINTENANCE",
  "loggedByEmployeeId": "firebase-uid-123"
}
```

**Success (201):**
```json
{
  "data": {
    "expenseId": 17
  }
}
```

---

### 8.3 POST `/api/v1/transactions/isp-settlement`

**Purpose:** Log a vendor/ISP settlement payment.

**Flutter:** `finance_repository.dart`.

**Request body:**
```json
{
  "connectionTypeName": "Fiber ISP",
  "amountPaid": 24500.00,
  "paymentStatus": "SETTLED",
  "settlementNotes": "March dues"
}
```

**Success (201):**
```json
{
  "data": {
    "settlementId": 8
  }
}
```

---

### 8.4 GET `/api/v1/transactions/daily-summary`

**Purpose:** Daily cash summary card — collections, expenses, settlements, net cash in hand.

**Flutter:** `daily_ledger_repository.dart`, `finance_repository.dart`.

**Query params:**

| Param | Required | Format |
|---|---|---|
| `targetDate` | Yes | ISO date `YYYY-MM-DD` |

**Success (200):**
```json
{
  "data": {
    "totalCollectedToday": 12500.00,
    "totalExpensedToday": 800.00,
    "totalIspSettlementsToday": 5000.00,
    "netCashInHand": 6700.00
  }
}
```

---

## 9. Finance analytics (`/api/v1/finance`)

### 9.1 GET `/api/v1/finance/daily-ledger`

**Purpose:** Daily Payment Ledger Book — summary banner + mixed transaction list (collections, expenses, ISP settlements).

**Flutter:** `daily_ledger_repository.dart`, `finance_repository.dart`.

**Headers:** `Authorization`, `X-E2E-ID`, `X-Session-ID`.

**Query params:**

| Param | Required | Format |
|---|---|---|
| `targetDate` | Yes | ISO date `YYYY-MM-DD` |

**Success (200):**
```json
{
  "data": {
    "summary": {
      "collectedAmountToday": 12450.00,
      "totalSettledHomesCount": 32
    },
    "transactions": [
      {
        "transactionId": "trx-001",
        "customerName": "S. Srinivasa Rao",
        "blockLocation": "Ramalayam Street",
        "timestamp": "2026-06-04T09:30:00",
        "amountCollected": 350.00,
        "paymentMode": "CASH",
        "fieldAgentName": "Ramesh Kumar",
        "isExpense": false,
        "isIspSettlement": false
      }
    ]
  }
}
```

---

### 9.2 POST `/api/v1/finance/daily-ledger/transactions`

**Purpose:** Record a manual field collection in the Daily Book.

**Flutter:** `daily_ledger_repository.dart` → `recordTransaction()`.

**Request body:**
```json
{
  "customer_name": "S. Srinivasa Rao",
  "block_code": "Ramalayam Street",
  "amount_collected": 350,
  "payment_type": "CASH",
  "collected_by": "Ramesh Kumar",
  "date": "2026-6-10"
}
```

**Success (201):** Standard envelope, `data: null`.

**Errors:** `400` — customer name not found in DB (must match an existing `customers.full_name`).

---

### 9.3 GET `/api/v1/finance/metrics`

**Purpose:** Finance dashboard KPI card.

**Flutter:** `finance_repository.dart` → `fetchFinanceMetrics()`.

**Query params:** `interval` — `1M`, `3M`, `6M` (default), `1Y`.

**Success (200)** — flat JSON (no envelope):
```json
{
  "net_profit": 142300,
  "trend_text": "+12% vs last quarter",
  "description": "Consolidated operational hub revenue generation and vendor outlays."
}
```

---

### 9.4 GET `/api/v1/finance/expenses`

**Purpose:** Expense donut chart segments.

**Success (200)** — JSON array (no envelope):
```json
[
  {
    "label": "Cable Wire Maintenance",
    "percentage": 60.0,
    "color_hex": 1719603
  }
]
```

---

### 9.5 GET `/api/v1/finance/performance`

**Purpose:** Monthly revenue vs expenses bar chart.

**Success (200)** — JSON array:
```json
[
  { "month": "JAN", "revenue": 120000, "expenses": 45000 }
]
```

---

### 9.6 GET `/api/v1/finance/disbursements`

**Purpose:** Recent vendor disbursement list.

**Success (200)** — JSON array:
```json
[
  {
    "reference": "DSP-8",
    "vendor": "Fiber ISP",
    "status": "FULL_PAYMENT",
    "amount": 24500
  }
]
```

---

### 9.7 GET `/api/v1/finance/health`

**Purpose:** System health counters for Finance screen.

**Success (200)** — flat JSON:
```json
{
  "active_subscriptions": 482,
  "uptime_percentage": 99.9
}
```

---

## 10. Broadcasts & notifications

All broadcast/notification POST routes return **202 ACCEPTED** with a tracking ID. They **log intent server-side** — no external WhatsApp provider is wired yet.

### 10.1 POST `/api/v1/broadcasts/pending-reminder`

**Flutter:** `dashboard_repository.dart` → `sendPendingReminder()`.

**Request body:** None.

**Success (202):**
```json
{
  "data": { "trackingId": "brd-pending-uuid" }
}
```

---

### 10.2 POST `/api/v1/broadcasts/active-reminder`

**Flutter:** `dashboard_repository.dart` → `sendActiveReminder()`.

Same shape as §10.1.

---

### 10.3 POST `/api/v1/bulletins/outage`

**Flutter:** `dashboard_repository.dart` → `createOutageAlert()` *(dead code — not called)*.

**Request body:**
```json
{
  "title": "Power outage",
  "body": "Services resume by 4 PM",
  "territory_id": "ter_abc123"
}
```

---

### 10.4 POST `/api/v1/notifications/broadcast-outage`

**Flutter:** `alerts_repository.dart` → `sendBroadcastOutage()`.

**Request body:**
```json
{
  "title": "Outage notice",
  "body": "Scheduled maintenance",
  "territory_id": "ter_abc123"
}
```

---

### 10.5 POST `/api/v1/notifications/dispatched-alert`

**Flutter:** `alerts_repository.dart`, `customer_ledger_repository.dart` → WhatsApp settlement actions.

**Request body:**
```json
{
  "customerId": "cust-001",
  "months": [1, 2, 3],
  "message": "Your outstanding due is ₹750"
}
```

---

### 10.6 POST `/api/v1/notifications/dispatch-alert`

**Flutter:** `notification_service.dart` *(unwired)*.

**Success (202):**
```json
{
  "data": {
    "notificationTrackingId": "ntf-generic-uuid",
    "estimatedCreditsUsed": 1
  }
}
```

---

### 10.7 GET `/api/v1/alerts/target-size`

**Flutter:** `alerts_repository.dart` → `fetchTargetAudienceSize()`.

**Query params:** `region`, `block`, `customer_types` (comma-separated).

**Success (200)** — flat JSON:
```json
{
  "target_size": 84
}
```

---

## Appendix A — Flutter implementation backlog

Backend routes from the June 2026 audit are **implemented**. Remaining work is on the Flutter client (`cable-crm`). See **`FLUTTER_UI_PROMPT.md`** in this repo for the full task list to hand to the mobile team.

| Priority | Flutter task | Why |
|---|---|---|
| P0 | Surface errors on dashboard broadcast buttons | APIs exist; VM swallows failures |
| P0 | Remove silent plan-update fallback on ledger | Show API errors now that `PUT /subscription` exists |
| P0 | Surface collect-payment errors on ledger | `POST /payments` can fail |
| P1 | Wire `recordNewExpense` / `recordNewIspSettlement` to `finance_repository` | Local-only inserts today |
| P1 | Send `assigned_villages` on `POST /employees` | Backend now persists |
| P1 | Wire `DELETE /employees/profile` in Settings | Replace simulated delay |
| P1 | Instantiate `SyncManager` in `app.dart` DI | Offline sync unwired |
| P1 | Fix `PaymentRecord` month parser for `JUNE` full names | Ledger display |
| P2 | Wire `customers/search` or remove dead API | No screen caller |
| P2 | Replace hardcoded alert regions with territory API | `alerts_view_model.dart` |
| P2 | Add nav to `FinanceScreen` or remove dead import | Screen unreachable |
| P2 | Territory 409 → auto-select existing (like plans) | UX parity |
| P2 | Delete legacy `customer_repository.dart` | Wrong POST shape |
| P3 | Optional: decouple `Future.wait` on ledger/daily ledger loads | Resilience if one call fails |

**Mock note:** `USE_MOCK_ADAPTER=false` by default. Test against production after Render deploy.

---

## Appendix B — Controller map

| Controller | Base path | Endpoints |
|---|---|---|
| `AuthController` | `/api/v1/auth` | `POST /token-swap` |
| `HealthController` | `/api/v1/auth` | `GET /health` |
| `DashboardController` | `/api/v1/dashboard` | `GET /metrics` (delegates to `DashboardService`) |
| `CustomerController` | `/api/v1/customers` | `POST /`, `GET /{id}`, `GET /{id}/ledger`, `POST /{id}/payments`, `PUT /{id}/subscription`, `GET /search` |
| `WorkspaceController` | `/api/v1/workspace` | Territory, customer, provider routes |
| `PlanController` | `/api/v1/plans` | `GET /`, `POST /`, `DELETE /{id}` |
| `EmployeeController` | `/api/v1/employees` | `GET /`, `POST /`, `PATCH /profile`, `DELETE /profile` |
| `SaasPricingController` | `/api/v1/saas` | `GET /pricing`, `POST /upgrade-intent` |
| `TransactionController` | (root) | `/api/v1/sync/synchronize`, `/api/v1/transactions/*` |
| `FinanceController` | `/api/v1/finance` | `GET /daily-ledger`, `POST /daily-ledger/transactions`, `/metrics`, `/expenses`, `/performance`, `/disbursements`, `/health` |
| `NotificationController` | (root) | `/api/v1/broadcasts/*`, `/api/v1/bulletins/*`, `/api/v1/notifications/*`, `/api/v1/alerts/target-size` |

---

*Last aligned with controllers: June 2026 (43 endpoints).*

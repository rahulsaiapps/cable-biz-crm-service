# Cable Pulse CRM — API Contract Specification

**Base URL (Production):** `https://cable-biz-crm-service.onrender.com/api/v1`

## Conventions

- **Authentication:** All endpoints except `Auth` module endpoints require a valid Firebase ID token passed as a Bearer token: `Authorization: Bearer <FIREBASE_JWT_TOKEN>`. The token is verified by `FirebaseAuthenticationFilter`, which derives Spring Security roles (`ROLE_OWNER` / `ROLE_COLLECTION_BOY`) from the token's custom claims (`role`, `owner`, `collection_boy`), falling back to the persisted `Employee` record's role if no claim is present.
- **Role-gated routes:** `/api/v1/plans/**` and `/api/v1/employees/**` require `ROLE_OWNER` (HTTP 403 otherwise). All other non-auth routes simply require *any* authenticated user (HTTP 401 if missing/invalid token).
- **Tracing headers:** Several endpoints require `X-E2E-ID` and `X-Session-ID` request headers — both must be valid UUID strings (HTTP 400 `Bad Request` if missing or malformed).
- **Standard envelope:** Most responses follow this shape:
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": { }
}
```
On error, `status` becomes `"ERROR"`, `error` contains a message string, and `data` is `null`.

---

## 1. Auth Module (`/api/v1/auth`)

Public endpoints — `permitAll()`. No Bearer token required (the `/token-swap` endpoint is how a client *obtains* the session token in the first place).

### 1.1 POST `/api/v1/auth/token-swap`

**Description:** Exchanges a Firebase ID token (obtained client-side via Firebase Auth) for an internal session/access token and an enriched user profile (full name + role), resolved by looking up the `Employee` record matching the Firebase UID.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Content-Type` | Yes | `application/json` |

**Request Body:**
```json
{
  "firebaseIdToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjA...<FIREBASE_ID_TOKEN>"
}
```

**Success Response (200 OK):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjA...<FIREBASE_ID_TOKEN>",
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

**Error Responses:**
- `400 Bad Request` — Firebase token validation failed:
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "ERROR",
  "error": "Token validation failed: Firebase ID token has expired",
  "data": null
}
```

**curl:**
```bash
curl -X POST 'https://cable-biz-crm-service.onrender.com/api/v1/auth/token-swap' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN" \
  -d '{
    "firebaseIdToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjA...<FIREBASE_ID_TOKEN>"
  }'
```

---

### 1.2 GET `/api/v1/auth/health`

**Description:** Lightweight health/liveness probe for the platform — returns service status, current timestamp, and platform name. Used by uptime monitors / load balancers.

**Headers:** None required.

**Request Body:** None (GET request).

**Success Response (200 OK):**
```json
{
  "status": "UP",
  "timestamp": "2026-06-08T10:15:30.123",
  "platform": "Cable Pulse Utility Backend"
}
```

**Error Responses:** None expected — endpoint is unauthenticated and side-effect free.

**curl:**
```bash
curl -X GET 'https://cable-biz-crm-service.onrender.com/api/v1/auth/health' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN"
```

---

## 2. Dashboard Metrics Module (`/api/v1/dashboard`)

### 2.1 GET `/api/v1/dashboard/metrics`

**Description:** Returns aggregate operational metrics for the home dashboard — total/pending customer counts, plus a financial summary (amount paid/pending for the current billing cycle). **RBAC privacy rule:** the `financialSummary` block is `null` for users holding `ROLE_COLLECTION_BOY` (collection staff cannot view financial roll-ups), and is populated for all other authenticated roles (e.g. `ROLE_OWNER`).

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `X-E2E-ID` | Yes | UUID — end-to-end trace identifier |
| `X-Session-ID` | Yes | UUID — client session identifier |
| `Content-Type` | Yes | `application/json` |

**Request Body:** None (GET request).

**Success Response (200 OK) — Owner/Admin role:**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "customerSummary": {
      "totalCustomers": 248,
      "pendingCustomers": 0
    },
    "financialSummary": {
      "amountPaid": 15450.00,
      "amountPending": 3200.00,
      "currency": "INR",
      "billingCyclePeriod": "JUNE-2026"
    }
  }
}
```

**Success Response (200 OK) — Collection Boy role (financials hidden):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "customerSummary": {
      "totalCustomers": 248,
      "pendingCustomers": 0
    },
    "financialSummary": null
  }
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid/expired Bearer token
- `400 Bad Request` — missing or malformed `X-E2E-ID` / `X-Session-ID` headers (must be valid UUIDs)

**curl:**
```bash
curl -X GET 'https://cable-biz-crm-service.onrender.com/api/v1/dashboard/metrics' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN" \
  -H 'X-E2E-ID: 11111111-1111-1111-1111-111111111111' \
  -H 'X-Session-ID: 22222222-2222-2222-2222-222222222222'
```

---

## 3. Customers Module (`/api/v1/customers`)

### 3.1 GET `/api/v1/customers/{id}/ledger`

**Description:** Retrieves a customer's billing ledger history — a month-by-month breakdown of paid/due amounts and statuses, plus the running total balance due. Future months (relative to the system's billing cutoff of June 2026) are filtered out of the response.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `X-E2E-ID` | Yes | UUID — end-to-end trace identifier |
| `X-Session-ID` | Yes | UUID — client session identifier |
| `Content-Type` | Yes | `application/json` |

**Path Parameters:**
| Param | Type | Description |
|---|---|---|
| `id` | String | Customer ID (e.g. `CUST-00123`) |

**Request Body:** None (GET request).

**Success Response (200 OK):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "customerId": "CUST-00123",
    "fullName": "Venkata Ramana",
    "totalBalanceDue": 600.00,
    "ledger": [
      {
        "month": "MAY",
        "year": 2026,
        "status": "PAID",
        "paidAmount": 300.00,
        "dueAmount": 0.00
      },
      {
        "month": "JUNE",
        "year": 2026,
        "status": "PARTIALLY_PAID",
        "paidAmount": 150.00,
        "dueAmount": 150.00
      }
    ]
  }
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token
- `400 Bad Request` — missing or malformed `X-E2E-ID` / `X-Session-ID` headers
- Note: an unknown `{id}` does **not** 404 — it returns `200 OK` with `fullName: "Unknown"` and an empty/partial ledger, since the controller treats a missing customer as a soft-fallback rather than an error.

**curl:**
```bash
curl -X GET 'https://cable-biz-crm-service.onrender.com/api/v1/customers/CUST-00123/ledger' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN" \
  -H 'X-E2E-ID: 11111111-1111-1111-1111-111111111111' \
  -H 'X-Session-ID: 22222222-2222-2222-2222-222222222222'
```

---

### 3.2 GET `/api/v1/customers/search`

**Description:** Type-ahead/autocomplete search for customers by full name, with an optional secondary filter on block/area name. Returns a lightweight list of `{customerId, label}` suggestions (label combines serial number, name, and block).

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `Content-Type` | Yes | `application/json` |

**Query Parameters:**
| Param | Required | Description |
|---|---|---|
| `name` | Yes | Partial/full customer name (case-insensitive contains match) |
| `block` | No | Partial/full block name to additionally filter by (case-insensitive contains match) |

**Request Body:** None (GET request).

**Success Response (200 OK):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": [
    { "customerId": "CUST-00123", "label": "12. Venkata Ramana (Block-A)" },
    { "customerId": "CUST-00456", "label": "47. Venkatesh Rao (Block-C)" }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token
- `400 Bad Request` — missing required `name` query parameter

**curl:**
```bash
curl -X GET 'https://cable-biz-crm-service.onrender.com/api/v1/customers/search?name=Venkat&block=Block-A' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN"
```

---

## 4. Workspace Module (`/api/v1/workspace`)

### 4.1 GET `/api/v1/workspace/customers`

**Description:** Returns the roster of customers assigned to a given territory/location ("workspace view") for field collection — including plan, monthly rate (custom override or plan default), connection/box/card details, and a default `UNPAID` payment status for the current cycle.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `X-E2E-ID` | Yes | UUID — end-to-end trace identifier |
| `X-Session-ID` | Yes | UUID — client session identifier |
| `Content-Type` | Yes | `application/json` |

**Query Parameters:**
| Param | Required | Description |
|---|---|---|
| `locationId` | Yes | Territory/location ID (e.g. `TERR-001`) |

**Request Body:** None (GET request).

**Success Response (200 OK):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "locationId": "TERR-001",
    "locationName": "Gandhi Nagar Block",
    "customers": [
      {
        "customerId": "CUST-00123",
        "serialNumber": 12,
        "fullName": "Venkata Ramana",
        "doorNumber": "12-3-45",
        "streetName": "Block-A",
        "activePlanName": "Gold HD Pack",
        "monthlyRate": 350.00,
        "paymentStatus": "UNPAID",
        "balanceDue": 350.00,
        "connectionType": "DTH",
        "boxNumber": "BOX-9981",
        "cardNumber": "CARD-2231"
      }
    ]
  }
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token
- `400 Bad Request` — missing `locationId` query parameter, or missing/malformed `X-E2E-ID` / `X-Session-ID` headers

**curl:**
```bash
curl -X GET 'https://cable-biz-crm-service.onrender.com/api/v1/workspace/customers?locationId=TERR-001' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN" \
  -H 'X-E2E-ID: 11111111-1111-1111-1111-111111111111' \
  -H 'X-Session-ID: 22222222-2222-2222-2222-222222222222'
```

---

### 4.2 GET `/api/v1/workspace/providers`

**Description:** Lists all registered connection/ISP providers (e.g. cable/DTH/internet vendors) configured for the operator.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `Content-Type` | Yes | `application/json` |

**Request Body:** None (GET request).

**Success Response (200 OK):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": [
    {
      "id": 1,
      "name": "Skynet Cable Networks",
      "registeredAt": "2025-01-15T09:30:00"
    },
    {
      "id": 2,
      "name": "Airtel Digital TV",
      "registeredAt": "2025-03-22T14:05:00"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token

**curl:**
```bash
curl -X GET 'https://cable-biz-crm-service.onrender.com/api/v1/workspace/providers' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN"
```

---

### 4.3 POST `/api/v1/workspace/providers`

**Description:** Registers a new connection/ISP provider for the operator's workspace. Provider name is required, must be non-blank, and must not exceed 50 characters (validated via `@Valid`).

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `Content-Type` | Yes | `application/json` |

**Request Body:**
```json
{
  "name": "Skynet Cable Networks"
}
```

**Success Response (201 Created):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "id": 3,
    "name": "Skynet Cable Networks",
    "registeredAt": "2026-06-08T10:15:30"
  }
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token
- `400 Bad Request` — validation failure, e.g. blank `name` or `name` exceeding 50 characters:
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "errors": ["Provider name is required and cannot be blank"]
}
```
- `409 Conflict` / `500 Internal Server Error` — duplicate provider name (unique constraint violation on `name`)

**curl:**
```bash
curl -X POST 'https://cable-biz-crm-service.onrender.com/api/v1/workspace/providers' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN" \
  -d '{
    "name": "Skynet Cable Networks"
  }'
```

---

## 5. Plans Module (`/api/v1/plans`) — `ROLE_OWNER` only

### 5.1 GET `/api/v1/plans`

**Description:** Lists all global subscription plans offered by a given provider (plan name, monthly rate, and feature highlights joined into a display string). **Restricted to users with `ROLE_OWNER`** — `ROLE_COLLECTION_BOY` users receive `403 Forbidden`.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` — must carry `ROLE_OWNER` |
| `Content-Type` | Yes | `application/json` |

**Query Parameters:**
| Param | Required | Description |
|---|---|---|
| `providerName` | Yes | Exact name of the connection provider (e.g. `Skynet Cable Networks`) |

**Request Body:** None (GET request).

**Success Response (200 OK):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": [
    {
      "planId": "PLAN-GOLD-HD",
      "name": "Gold HD Pack",
      "price": 350.00,
      "details": "120+ Channels, HD Support, Free Installation"
    },
    {
      "planId": "PLAN-SILVER-SD",
      "name": "Silver SD Pack",
      "price": 220.00,
      "details": "80+ Channels, SD Support"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token
- `403 Forbidden` — authenticated but lacking `ROLE_OWNER` (e.g. `ROLE_COLLECTION_BOY`)
- `400 Bad Request` — missing required `providerName` query parameter

**curl:**
```bash
curl -X GET 'https://cable-biz-crm-service.onrender.com/api/v1/plans?providerName=Skynet%20Cable%20Networks' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN"
```

---

## 6. SaaS Pricing Module (`/api/v1/saas`)

### 6.1 GET `/api/v1/saas/pricing`

**Description:** Returns the platform's SaaS subscription pricing matrix (tier names, billing cycles, retail/discounted prices, currency) along with promotional-trial eligibility — the trial is active (3-month free period) while the total number of registered operator tenants remains under 100.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `X-User-Country-Code` | No | ISO country code (e.g. `IN`, `US`); defaults to `IN` if omitted — currently informational only and does not change pricing output |
| `Content-Type` | Yes | `application/json` |

**Request Body:** None (GET request).

**Success Response (200 OK):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "promotionalTrialActive": true,
    "trialEndsAt": "2026-09-08T10:15:30",
    "currencyCode": "INR",
    "tiers": [
      {
        "tierName": "Starter",
        "billingCycle": "MONTHLY",
        "retailPrice": 999.0,
        "discountedPrice": 699.0
      },
      {
        "tierName": "Professional",
        "billingCycle": "ANNUAL",
        "retailPrice": 9999.0,
        "discountedPrice": 6999.0
      }
    ]
  }
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token

**curl:**
```bash
curl -X GET 'https://cable-biz-crm-service.onrender.com/api/v1/saas/pricing' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN" \
  -H 'X-User-Country-Code: IN'
```

---

## 7. Transactions & Sync Module (`/api/v1`)

### 7.1 POST `/api/v1/sync/synchronize`

**Description:** Accepts a batch of offline-queued client events (e.g. payments recorded while offline) and reconciles them server-side, returning which event IDs were processed successfully versus rejected. Used by mobile/field clients to flush their local offline queue once connectivity is restored.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `X-E2E-ID` | Yes | UUID — end-to-end trace identifier |
| `X-Session-ID` | Yes | UUID — client session identifier |
| `Content-Type` | Yes | `application/json` |

**Request Body:**
```json
{
  "syncBatchId": "33333333-3333-3333-3333-333333333333",
  "events": [
    {
      "eventId": "EVT-1001",
      "actionType": "PAYMENT_RECORDED",
      "idempotencyToken": "44444444-4444-4444-4444-444444444444",
      "payload": {
        "customerId": "CUST-00123",
        "amount": 350.00,
        "billingMonth": "JUNE",
        "billingYear": 2026,
        "collectedBy": "EMP-009"
      }
    }
  ]
}
```

**Success Response (200 OK):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "syncResolution": {
      "processedEventIds": ["EVT-1001"],
      "rejectedEventIds": []
    }
  }
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token
- `400 Bad Request` — malformed payload, invalid `actionType`, or business-rule rejection raised as `IllegalArgumentException`:
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "ERROR",
  "error": "Duplicate idempotency token detected for event EVT-1001",
  "data": null
}
```
- Missing/malformed `X-E2E-ID` / `X-Session-ID` headers also yield `400 Bad Request`

**curl:**
```bash
curl -X POST 'https://cable-biz-crm-service.onrender.com/api/v1/sync/synchronize' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN" \
  -H 'X-E2E-ID: 11111111-1111-1111-1111-111111111111' \
  -H 'X-Session-ID: 22222222-2222-2222-2222-222222222222' \
  -d '{
    "syncBatchId": "33333333-3333-3333-3333-333333333333",
    "events": [
      {
        "eventId": "EVT-1001",
        "actionType": "PAYMENT_RECORDED",
        "idempotencyToken": "44444444-4444-4444-4444-444444444444",
        "payload": {
          "customerId": "CUST-00123",
          "amount": 350.00,
          "billingMonth": "JUNE",
          "billingYear": 2026,
          "collectedBy": "EMP-009"
        }
      }
    ]
  }'
```

---

### 7.2 POST `/api/v1/transactions/expense`

**Description:** Logs a daily operational expense (e.g. wages, fuel, repairs) against the operator's cash ledger. `expenseCategory` must be one of: `WIRE`, `FUEL`, `REPAIR`, `WAGES`, `MISC`. Amount is capped at `10,000,000.00`.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `Content-Type` | Yes | `application/json` |

**Request Body:**
```json
{
  "amount": 1500.00,
  "description": "Cable wire purchase for Block-C re-wiring",
  "expenseCategory": "WIRE",
  "loggedByEmployeeId": "EMP-009"
}
```

**Success Response (201 Created):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "expenseId": 87
  }
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token
- `400 Bad Request` — invalid `expenseCategory` enum value, missing required fields, or amount exceeding the `10,000,000.00` business limit:
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "ERROR",
  "error": "Transaction amount exceeds maximum permissible business limit",
  "data": null
}
```

**curl:**
```bash
curl -X POST 'https://cable-biz-crm-service.onrender.com/api/v1/transactions/expense' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN" \
  -d '{
    "amount": 1500.00,
    "description": "Cable wire purchase for Block-C re-wiring",
    "expenseCategory": "WIRE",
    "loggedByEmployeeId": "EMP-009"
  }'
```

---

### 7.3 POST `/api/v1/transactions/isp-settlement`

**Description:** Logs a settlement payment made to an upstream ISP/connection vendor (e.g. monthly dues paid to the cable provider), recording amount, payment status, and optional notes.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `Content-Type` | Yes | `application/json` |

**Request Body:**
```json
{
  "connectionTypeName": "Skynet Cable Networks",
  "amountPaid": 25000.00,
  "paymentStatus": "PAID",
  "settlementNotes": "Monthly vendor dues settled via bank transfer for May 2026"
}
```

**Success Response (201 Created):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "settlementId": 34
  }
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token
- `400 Bad Request` — missing required fields or `amountPaid` exceeding the `10,000,000.00` business limit:
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "ERROR",
  "error": "Transaction amount exceeds maximum permissible business limit",
  "data": null
}
```

**curl:**
```bash
curl -X POST 'https://cable-biz-crm-service.onrender.com/api/v1/transactions/isp-settlement' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN" \
  -d '{
    "connectionTypeName": "Skynet Cable Networks",
    "amountPaid": 25000.00,
    "paymentStatus": "PAID",
    "settlementNotes": "Monthly vendor dues settled via bank transfer for May 2026"
  }'
```

---

### 7.4 GET `/api/v1/transactions/daily-summary`

**Description:** Returns a same-day cash-position summary for the operator — total amount collected from customers, total expenses logged, total ISP settlements paid, and the resulting net cash-in-hand for the specified date.

**Headers:**
| Header | Required | Notes |
|---|---|---|
| `Authorization` | Yes | `Bearer <FIREBASE_JWT_TOKEN>` |
| `Content-Type` | Yes | `application/json` |

**Query Parameters:**
| Param | Required | Description |
|---|---|---|
| `targetDate` | Yes | ISO-8601 date (`yyyy-MM-dd`), e.g. `2026-06-08` |

**Request Body:** None (GET request).

**Success Response (200 OK):**
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "SUCCESS",
  "error": null,
  "data": {
    "totalCollectedToday": 18500.00,
    "totalExpensedToday": 1500.00,
    "totalIspSettlementsToday": 25000.00,
    "netCashInHand": -8000.00
  }
}
```

**Error Responses:**
- `401 Unauthorized` — missing/invalid Bearer token
- `400 Bad Request` — missing `targetDate` parameter or invalid date format (must be `yyyy-MM-dd`):
```json
{
  "timestamp": "2026-06-08T10:15:30",
  "status": "ERROR",
  "error": "Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'",
  "data": null
}
```

**curl:**
```bash
curl -X GET 'https://cable-biz-crm-service.onrender.com/api/v1/transactions/daily-summary?targetDate=2026-06-08' \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer YOUR_MOCK_OR_REAL_TOKEN"
```

---

## Endpoint Summary Table

| Module | Method | Path | Auth | Role |
|---|---|---|---|---|
| Auth | POST | `/api/v1/auth/token-swap` | Public | — |
| Auth | GET | `/api/v1/auth/health` | Public | — |
| Dashboard | GET | `/api/v1/dashboard/metrics` | Bearer + trace headers | Any authenticated |
| Customers | GET | `/api/v1/customers/{id}/ledger` | Bearer + trace headers | Any authenticated |
| Customers | GET | `/api/v1/customers/search` | Bearer | Any authenticated |
| Workspace | GET | `/api/v1/workspace/customers` | Bearer + trace headers | Any authenticated |
| Workspace | GET | `/api/v1/workspace/providers` | Bearer | Any authenticated |
| Workspace | POST | `/api/v1/workspace/providers` | Bearer | Any authenticated |
| Plans | GET | `/api/v1/plans` | Bearer | `ROLE_OWNER` |
| SaaS Pricing | GET | `/api/v1/saas/pricing` | Bearer | Any authenticated |
| Transactions | POST | `/api/v1/sync/synchronize` | Bearer + trace headers | Any authenticated |
| Transactions | POST | `/api/v1/transactions/expense` | Bearer | Any authenticated |
| Transactions | POST | `/api/v1/transactions/isp-settlement` | Bearer | Any authenticated |
| Transactions | GET | `/api/v1/transactions/daily-summary` | Bearer | Any authenticated |

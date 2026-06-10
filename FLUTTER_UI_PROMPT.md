# Cable Pulse Flutter — Implementation Prompt

**Hand this document + [`api_contracts.md`](./api_contracts.md) to the Flutter (`cable-crm`) agent or developer.**

**Production API:** `https://cable-biz-crm-service.onrender.com/api/v1`  
**Mock default:** `USE_MOCK_ADAPTER=false` (`lib/src/core/constants/app_constants.dart`)

---

## Context

The backend (`cable-crm-service`) was audited against the Flutter app and **all previously missing API routes are now implemented** (43 endpoints total). The Flutter repositories already call most routes, but several screens have **silent failures**, **local-only stubs**, **dead code**, or **unwired services**.

Your job: align `cable-crm` with the updated **`api_contracts.md`** so every user-facing flow works against the **real API** (not mock-only).

**Do not change backend code** unless you discover a contract bug — fix Flutter to match `api_contracts.md`.

---

## Backend changes summary (what’s new for Flutter)

| Area | New / updated endpoints | Flutter file(s) |
|---|---|---|
| Customer profile | `GET /customers/{id}` | `customer_ledger_repository.dart` |
| Collect payment | `POST /customers/{id}/payments` | `customer_ledger_repository.dart` |
| Change plan | `PUT /customers/{id}/subscription` | `customer_ledger_repository.dart` |
| Delete plan | `DELETE /plans/{id}` | `plans_repository.dart` |
| Daily book list | `GET /finance/daily-ledger` | `daily_ledger_repository.dart` |
| Record collection | `POST /finance/daily-ledger/transactions` | `daily_ledger_repository.dart` |
| Finance charts | `GET /finance/metrics`, `/expenses`, `/performance`, `/disbursements`, `/health` | `finance_repository.dart` |
| Dashboard broadcasts | `POST /broadcasts/pending-reminder`, `/active-reminder` | `dashboard_repository.dart` |
| Outage / alerts | `POST /notifications/*`, `GET /alerts/target-size` | `alerts_repository.dart` |
| Account delete | `DELETE /employees/profile` | `settings_view_model.dart` |
| Employee villages | `assigned_villages` on `POST /employees` | `employee_repository.dart` |
| SaaS upgrade | `tier_name` / `billing_cycle` accepted (snake_case) | `saas_pricing_repository.dart` |

**Deploy note:** Production Render must run the latest backend build. Until deployed, some routes may still 404.

---

## P0 — Fix broken / misleading UX (do first)

### 1. Customer ledger — error surfacing

**Files:** `customer_ledger_view_model.dart`, `customer_ledger_screen.dart`

**Today:**
- `loadLedger()` uses `Future.wait` on profile + ledger — if profile fails, whole screen errors (backend now has `GET /customers/{id}`, so this should work after deploy).
- `collectPayment()` swallows errors — no snackbar/dialog.
- `updateSubscriptionPlan()` has **silent local fallback** on API failure (`catch (_) { ... fake local update }`).

**Do:**
- Keep `Future.wait` OR load ledger first and profile second with graceful degradation (hero card from ledger envelope if profile fails).
- On `collectPayment` failure: set `_errorMessage` and show `SnackBar`.
- On `updateSubscriptionPlan` failure: show error; **remove** silent local fallback now that `PUT /subscription` exists.

**API:** §3.2–3.5 in `api_contracts.md`

---

### 2. Dashboard broadcasts — surface errors

**Files:** `dashboard_view_model.dart` (`sendPendingReminder`, `sendActiveReminder`), `dashboard_screen.dart`

**Today:** API calls fail silently (no user feedback).

**Do:**
- Catch `DioException`; set `broadcastErrorMessage` on ViewModel.
- Show `SnackBar` on success (202 ACCEPTED) and failure.
- Disable buttons while in-flight (already partially done).

**API:** §10.1–10.2

---

### 3. Daily ledger — wire expense/settlement to real APIs

**Files:** `daily_ledger_view_model.dart` (`recordNewExpense`, `recordNewIspSettlement`)

**Today:** Inserts **local-only** rows into `_transactions` — never hits server.

**Do:**
- Call `FinanceRepository.logExpense()` and `logIspSettlement()` (already implemented in `finance_repository.dart`).
- On success, call `loadLedger()` to refresh from `GET /finance/daily-ledger`.
- Surface errors via snackbar.

**API:** §8.2–8.3 (transactions) + §9.1 (refresh)

---

### 4. PaymentRecord month parser

**File:** `lib/src/data/models/payment_record.dart` → `_monthToInt`

**Today:** Only handles 3-letter abbrev (`JUN`). Backend may return `JUNE` or `JUN`.

**Do:** Add full month names (`JANUARY`…`DECEMBER`) to the switch.

---

## P1 — Wire missing integrations

### 5. Settings — real account deletion

**File:** `settings_view_model.dart` → `deleteAccount()`

**Today:** `Future.delayed(800ms)` then `signOut()` — no API.

**Do:**
- Add `EmployeeRepository.deleteAccount()` → `DELETE /api/v1/employees/profile`
- On 200: `signOut()` and return `true`
- On failure: show error, return `false`

**API:** §6.4

---

### 6. Employee create — send `assigned_villages`

**File:** `employee_repository.dart` → `createEmployee()`

**Today:** Comment says villages are not transmitted; backend now persists them.

**Do:** Add to POST body:
```dart
if (assignedVillages.isNotEmpty) 'assigned_villages': assignedVillages,
```

**API:** §6.2

---

### 7. Offline sync — instantiate `SyncManager`

**Files:** `lib/src/app.dart`, `sync_manager.dart`

**Today:** `SyncManager` exported but never created in DI.

**Do:**
- Register `SyncManager` in `MultiProvider` (needs shared `Dio`).
- Call `flushOfflineQueue()` after successful login.
- Ensure payment mutations from customer ledger can enqueue offline when disconnected.

**API:** §8.1

---

### 8. Plans — verify delete works end-to-end

**File:** `plans_view_model.dart`, `plans_repository.dart`

**Today:** `DELETE /plans/{id}` was 404; backend now implements it.

**Do:** Verify delete swipe calls repo and shows success/error (error handling may already exist).

**API:** §5.3

---

## P2 — Polish & cleanup

### 9. Alerts — dynamic regions/blocks

**File:** `alerts_view_model.dart`

**Today:** Hardcoded `Kolamuru Village`, `Ramalayam Street`, etc. Audience size falls back to `84` on error.

**Do:**
- Load regions from `GET /workspace/territories` or `active-locations`.
- Load blocks from `GET /workspace/territories/{id}/blocks`.
- Keep `84` fallback only as last resort; prefer showing error.

**API:** §4.1–4.3, §10.7

---

### 10. Finance screen — navigation or removal

**Files:** `dashboard_screen.dart` (unused import), `finance_screen.dart`

**Today:** `FinanceScreen` built but never navigated; finance APIs now exist.

**Do (pick one):**
- **Option A:** Add nav entry for OWNER role → `FinanceScreen` (hide for `COLLECTION_BOY`).
- **Option B:** Remove dead import and defer Finance screen to a later release.

**API:** §9.3–9.7

---

### 11. Territory duplicate — auto-select on 409

**File:** `territory_view_model.dart`

**Today:** Shows error on 409. Plans module auto-selects duplicate category.

**Do:** On 409, parse `data` from response and return existing territory (mirror `plans_view_model.dart` duplicate handling).

**API:** §4.7

---

### 12. Customer search — wire or remove

**File:** `customers_repository.dart` → `search()`

**Today:** No screen calls it; workspace uses client-side filter.

**Do:** Wire to workspace search field OR delete unused method.

**API:** §3.6

---

### 13. Dead code cleanup

| Item | Action |
|---|---|
| `customer_repository.dart` (legacy wrong POST body) | Delete file + remove exports from `data.dart` / `domain.dart` |
| `dashboard_repository.createOutageAlert` | Delete or wire to `POST /bulletins/outage` |
| `notification_service.dart` | Wire or delete |
| `sync_manager` dead methods (`registerCustomer`, `createPlan`) | Delete |
| Unused `finance_screen.dart` import in `dashboard_screen.dart` | Remove or add nav |

---

## P3 — Optional resilience

### 14. Decouple parallel loads (optional)

**Files:** `customer_ledger_view_model.dart`, `daily_ledger_view_model.dart`

If one of two parallel fetches fails, still show partial data:
- Ledger: show ledger grid even if profile 404.
- Daily book: show `daily-summary` banner even if transaction list fails.

---

## Contract alignment checklist

Before marking done, verify against **`api_contracts.md`**:

- [ ] Trace headers `X-E2E-ID` / `X-Session-ID` on all non-auth calls (`crm_http_client.dart`)
- [ ] `POST /customers` snake_case body unchanged
- [ ] `POST /employees` sends `full_name`, `role`, `email`, `assigned_villages`
- [ ] `PATCH /employees/profile` diff-only payload
- [ ] `POST /customers/{id}/payments` sends `monthsPaid` (camelCase OK)
- [ ] `PUT /subscription` sends `plan_name`, `plan_monthly_rate`
- [ ] `saas/upgrade-intent` — snake_case `tier_name` works (backend accepts both)
- [ ] Finance GET routes parse **flat JSON** (no envelope) for metrics/expenses/performance/disbursements/health
- [ ] `GET /finance/daily-ledger` parses `data.summary` + `data.transactions`
- [ ] RBAC: collection boy blocked from employee admin, territory add, finance cards
- [ ] `USE_MOCK_ADAPTER=false` smoke test against production URL

---

## Testing plan

1. **Login** → token-swap → dashboard metrics load
2. **Add territory** → `POST /workspace/providers` with `location_name`
3. **Add customer** → `POST /customers`
4. **Open customer ledger** → profile + ledger + collect payment
5. **Daily ledger** → summary + transaction list + record collection
6. **Plans** → list/create/delete (OWNER)
7. **Team** → create employee with villages
8. **Settings** → profile PATCH + delete account
9. **Dashboard** → broadcast buttons show success/error
10. **Alerts** → audience size + broadcast outage

Run `flutter analyze` on touched files.

---

## Out of scope (backend / product)

- Real WhatsApp delivery (broadcasts return 202 + tracking ID only)
- `today_collection` / `phone_number` on employee roster (still placeholders)
- Render deploy (ops task — required before production testing)

---

*Generated June 2026 — pairs with `api_contracts.md` v43 endpoints.*

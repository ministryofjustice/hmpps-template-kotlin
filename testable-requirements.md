# Testable Requirements — Money to Prisoners API

This document captures every testable requirement extracted from the existing Python/Django
test suite and source code. It is intended to drive the creation of a comprehensive Kotlin
Spring Boot test suite that fully mimics the current service's functions and capabilities.

Requirements are grouped by domain (app) and then by feature area. Each requirement is
specific enough to map directly to one or more test methods.

---

## Table of Contents

1. [Account (Balance)](#1-account-balance)
2. [Credit](#2-credit)
3. [Disbursement](#3-disbursement)
4. [Payment](#4-payment)
5. [Prison](#5-prison)
6. [Security](#6-security)
7. [Authentication & User Management (mtp_auth)](#7-authentication--user-management)
8. [Transaction](#8-transaction)
9. [Notification](#9-notification)
10. [Performance & Reporting](#10-performance--reporting)
11. [Service Availability](#11-service-availability)
12. [User Event Log](#12-user-event-log)
13. [Core Infrastructure](#13-core-infrastructure)
14. [Cross-Cutting Concerns](#14-cross-cutting-concerns)

---

## 1. Account (Balance)

### 1.1 Balance Model

| ID | Requirement | Details |
|----|------------|---------|
| ACC-001 | Balance stores closing amount as integer (pence) | `closing_balance` is BigInteger, not decimal |
| ACC-002 | Balance records date | `date` field is date-only (no time) |
| ACC-003 | Timestamps auto-populated | `created` and `modified` set automatically |
| ACC-004 | Default ordering is newest-first | Ordered by `-date` |
| ACC-005 | String representation includes formatted currency | `__str__` returns "YYYY-MM-DD £X.XX" |

### 1.2 Balance List Endpoint

| ID | Requirement | Details |
|----|------------|---------|
| ACC-010 | GET /balances/ returns 200 | Paginated list of balances |
| ACC-011 | Authentication required | Unauthenticated → 401 |
| ACC-012 | Any authenticated user can list | No specific permission required for list |
| ACC-013 | Results ordered newest-first | Most recent date appears first |
| ACC-014 | Filter by date__lt | Only returns balances before given date |
| ACC-015 | Filter by date__gte | Returns balances on or after given date |
| ACC-016 | Combined date filters work | date__gte + date__lt returns correct range |
| ACC-017 | Empty database returns empty results | 200 with `results: []` |
| ACC-018 | Response includes all fields | id, closing_balance, date, created, modified |

### 1.3 Balance Create Endpoint

| ID | Requirement | Details |
|----|------------|---------|
| ACC-020 | POST /balances/ creates balance | Returns 201 Created |
| ACC-021 | Requires add_balance permission | Bank Admin can create; Prison Clerk gets 403 |
| ACC-022 | Duplicate date rejected | Second POST for same date → 400 with error message |
| ACC-023 | Error message includes date | "Balance exists for date YYYY-MM-DD" |
| ACC-024 | Supports large values | BigInteger handles max long values |
| ACC-025 | Zero balance allowed | closing_balance=0 succeeds |
| ACC-026 | No update/delete endpoints | PATCH, PUT, DELETE → 405 Method Not Allowed |

---

## 2. Credit

### 2.1 Credit Model & Status Lifecycle

| ID | Requirement | Details |
|----|------------|---------|
| CRD-001 | Credit has amount in pence | PositiveIntegerField |
| CRD-002 | Credit links to prisoner | prisoner_number, prisoner_name, prisoner_dob |
| CRD-003 | Credit links to prison | ForeignKey, nullable (set when prisoner located) |
| CRD-004 | Credit has source type | bank_transfer (has transaction) or online (has payment) or unknown (neither) |
| CRD-005 | Credit has resolution status | initial → pending → credited (or failed/refunded/manual) |
| CRD-006 | Resolution transitions enforced | Invalid transitions rejected |
| CRD-007 | received_at timestamp | Set when credit first received |
| CRD-008 | reconciled flag | Boolean, tracks reconciliation state |
| CRD-009 | Credit has owner (user) | User who credited the prisoner |
| CRD-010 | Default manager excludes initial and failed | CompletedCreditManager filters these out |
| CRD-011 | objects_all includes all resolutions | Unfiltered manager for admin/tests |
| CRD-012 | blocked flag | Boolean, prevents credit_pending status, makes refund eligible |
| CRD-013 | reviewed flag | Boolean, set by security staff review action |

### 2.2 Credit Computed Status

| ID | Requirement | Details |
|----|------------|---------|
| CRD-015 | credit_pending status | prison NOT null AND (resolution=pending OR manual) AND blocked=false |
| CRD-016 | credited status | resolution=credited |
| CRD-017 | refund_pending status | (prison IS null OR blocked) AND resolution=pending AND (no transaction OR incomplete_sender_info=false) |
| CRD-018 | refunded status | resolution=refunded |
| CRD-019 | failed status | resolution=failed |

### 2.3 Credit List Endpoint

| ID | Requirement | Details |
|----|------------|---------|
| CRD-020 | GET /credits/ returns paginated list | 200 OK with count, results, page, page_count |
| CRD-021 | Authentication required | 401 if not authenticated |
| CRD-022 | Prison clerk sees own prisons only | Filtered by PrisonUserMapping |
| CRD-023 | Users with view_any_credit see all credits | No prison filtering |
| CRD-024 | Cashbook client has UTC date boundary | Only credits with received_at before midnight UTC today |
| CRD-025 | Requires CreditPermissions | view_credit permission for list action |
| CRD-026 | Accepts Cashbook, NomsOps, or BankAdmin client | Other OAuth clients → 403 |

### 2.4 Credit List Filters — Status

| ID | Requirement | Details |
|----|------------|---------|
| CRD-030 | Filter status=credit_pending | Prison assigned, pending/manual, not blocked |
| CRD-031 | Filter status=credited | Resolution=credited |
| CRD-032 | Filter status=refund_pending | No prison or blocked, pending, sender info complete |
| CRD-033 | Filter status=refunded | Resolution=refunded |
| CRD-034 | Filter status=failed | Resolution=failed |
| CRD-035 | Invalid status returns empty set | 200 OK with count=0 |
| CRD-036 | Filter valid=true | Credits that are credit_pending OR credited |
| CRD-037 | Filter valid=false | Credits NOT (credit_pending OR credited) |

### 2.5 Credit List Filters — Prison

| ID | Requirement | Details |
|----|------------|---------|
| CRD-040 | Filter prison={nomis_id} | Exact match |
| CRD-041 | Filter prison[]={id1},{id2} | Multiple prison IDs |
| CRD-042 | Filter prison__isnull=True | Credits with no prison assigned |
| CRD-043 | Filter prison_region={name} | Case-insensitive substring on prison.region |
| CRD-044 | Filter prison_category={name} | Matches any category in prison.categories |
| CRD-045 | Filter prison_population={name} | Matches any population in prison.populations |
| CRD-046 | Invalid prison ID returns empty set | 200 OK, not error |

### 2.6 Credit List Filters — Amount

| ID | Requirement | Details |
|----|------------|---------|
| CRD-050 | Filter amount={exact} | Exact match in pence |
| CRD-051 | Filter amount__gte={value} | Greater than or equal |
| CRD-052 | Filter amount__lte={value} | Less than or equal |
| CRD-053 | Filter amount__endswith={suffix} | Last digits of amount match |
| CRD-054 | Filter amount__regex={pattern} | Regex on amount value |
| CRD-055 | Filter exclude_amount__endswith | Amount does NOT end with |
| CRD-056 | Filter exclude_amount__regex | Amount does NOT match regex |
| CRD-057 | Multiple amount filters combine with AND | All conditions must match |

### 2.7 Credit List Filters — Sender/Payment

| ID | Requirement | Details |
|----|------------|---------|
| CRD-060 | Filter sender_name | Case-insensitive substring on transaction.sender_name or payment.cardholder_name |
| CRD-061 | Filter sender_sort_code | Exact match on transaction field |
| CRD-062 | Filter sender_account_number | Exact match on transaction field |
| CRD-063 | Filter sender_roll_number | Exact match on transaction field |
| CRD-064 | Filter sender_name__isblank=True | Blank sender_name from transactions |
| CRD-065 | Filter sender_sort_code__isblank=True | Blank sort code |
| CRD-066 | Filter sender_email | Case-insensitive substring on payment.email |
| CRD-067 | Filter sender_ip_address | Exact match on payment.ip_address |
| CRD-068 | Filter card_number_first_digits | Exact match on payment field |
| CRD-069 | Filter card_number_last_digits | Exact match on payment field |
| CRD-070 | Filter card_expiry_date | Exact match on payment field |
| CRD-071 | Filter sender_postcode | Normalized postcode matching on billing address |
| CRD-072 | Filter payment_reference | Prefix match on payment.uuid (first 8 chars) |
| CRD-073 | Filter source=bank_transfer | Credits with transactions (no payment) |
| CRD-074 | Filter source=online | Credits with payments (no transaction) |
| CRD-075 | Filter source=unknown | Credits with neither |

### 2.8 Credit List Filters — Other

| ID | Requirement | Details |
|----|------------|---------|
| CRD-080 | Filter prisoner_name | Case-insensitive substring |
| CRD-081 | Filter prisoner_number | Exact match |
| CRD-082 | Filter user={user_id} | By credit owner |
| CRD-083 | Filter resolution | Exact match on resolution value |
| CRD-084 | Filter reviewed=true/false | Boolean reviewed flag |
| CRD-085 | Filter received_at__gte/lt | Datetime range, ISO format |
| CRD-086 | Filter logged_at__gte/lt | By log creation date (truncated to UTC date) |
| CRD-087 | Filter security_check__isnull=True | No security check exists |
| CRD-088 | Filter security_check__actioned_by__isnull=True | Check not yet actioned |
| CRD-089 | Filter exclude_credit__in={ids} | Exclude specific credit IDs |
| CRD-090 | Filter monitored=true | Credits linked to user's monitored profiles |
| CRD-091 | Filter pk={id1,id2} | Multiple credit IDs |

### 2.9 Credit List Search & Ordering

| ID | Requirement | Details |
|----|------------|---------|
| CRD-095 | search={text} full text search | Searches prisoner_name, prisoner_number, sender_name, amount (£nn.nn format), payment UUID prefix |
| CRD-096 | All search words required (AND logic) | Every word must match somewhere |
| CRD-097 | simple_search={text} | Searches transaction.sender_name, payment.cardholder_name, payment.email, prisoner_number |
| CRD-098 | Ordering by created, received_at, amount | Ascending and descending |
| CRD-099 | Ordering by prisoner_number, prisoner_name | Ascending and descending |

### 2.10 Credit Serialization

| ID | Requirement | Details |
|----|------------|---------|
| CRD-100 | Base serializer includes core fields | id, prisoner_name/number, amount, started_at, received_at, sender_name, sender_email, prison, owner, owner_name, resolution, source |
| CRD-101 | Includes computed timestamps | credited_at, refunded_at, set_manual_at from log entries |
| CRD-102 | short_payment_ref | First 8 chars of payment UUID |
| CRD-103 | anonymous flag | True if transaction with incomplete_sender_info AND blocked |
| CRD-104 | intended_recipient | payment.recipient_name or null |
| CRD-105 | comments nested array | Read-only nested comments |
| CRD-106 | Security serializer adds bank/card details | sort_code, account_number, roll_number, card digits, expiry, ip_address, billing_address |
| CRD-107 | Security serializer adds profile PKs | sender_profile, prisoner_profile IDs |
| CRD-108 | Security check serializer adds check object | Nested CheckSerializer |

### 2.11 Credit Action — Credit Prisoners

| ID | Requirement | Details |
|----|------------|---------|
| CRD-110 | POST /credits/actions/credit/ | Array of {id, credited, nomis_transaction_id?} |
| CRD-111 | Only credit_pending credits eligible | Prison assigned, pending/manual, not blocked |
| CRD-112 | Invalid state credits returned as conflict_ids | HTTP 200 with errors array |
| CRD-113 | Sets resolution=credited, owner=user | Plus optional nomis_transaction_id |
| CRD-114 | Creates log entry (LogAction.credited) | With user reference |
| CRD-115 | Uses select_for_update | Pessimistic locking for transaction safety |
| CRD-116 | Requires CashbookClientIDPermissions | Other clients → 403 |
| CRD-117 | Requires credit_credit permission | Django permission check |
| CRD-118 | Returns 204 on success | Even if conflict_ids exist |
| CRD-119 | Invalid format → 400 | Missing fields or empty list |

### 2.12 Credit Action — Set Manual

| ID | Requirement | Details |
|----|------------|---------|
| CRD-120 | POST /credits/actions/setmanual/ | Request: {credit_ids: [int...]} |
| CRD-121 | Only resolution=pending eligible | Other resolutions → conflict_ids |
| CRD-122 | Sets resolution=manual, owner=user | State change |
| CRD-123 | Creates log entry (LogAction.manual) | Audit trail |
| CRD-124 | Sends credit_set_manual signal | Event notification |
| CRD-125 | Returns 204 on success | No content body |

### 2.13 Credit Action — Review

| ID | Requirement | Details |
|----|------------|---------|
| CRD-130 | POST /credits/actions/review/ | Request: {credit_ids: [int...]} |
| CRD-131 | Sets reviewed=true on all specified credits | No state validation required |
| CRD-132 | Creates log entry (LogAction.reviewed) | For each credit |
| CRD-133 | Uses select_for_update | Concurrency safety |
| CRD-134 | Requires NomsOpsClientIDPermissions | Security staff action |
| CRD-135 | Requires review_credit permission | Django permission |
| CRD-136 | Returns 204 on success | No content body |

### 2.14 Credit Action — Refund

| ID | Requirement | Details |
|----|------------|---------|
| CRD-140 | POST /credits/actions/refund/ | Mark credits for refund |
| CRD-141 | Only refund_pending eligible | (no prison OR blocked) AND pending AND sender info complete |
| CRD-142 | Sets resolution=refunded | Terminal state |
| CRD-143 | Sends credit_refunded signal | Event notification |
| CRD-144 | Raises InvalidCreditStateException on conflict | Strict validation |

### 2.15 Credits Grouped by Credited (Processed View)

| ID | Requirement | Details |
|----|------------|---------|
| CRD-150 | GET /credits/processed/ | Aggregated by date + operator |
| CRD-151 | Groups by logged_at date and owner | UTC date truncation |
| CRD-152 | Returns count, total, comment_count per group | Aggregated values |
| CRD-153 | owner_name = full name or "Unknown" | Handles deleted users |
| CRD-154 | Only credited credits with credited log | Filtered to LogAction.credited |
| CRD-155 | Supports all CreditListFilter params | Same filters as /credits/ |
| CRD-156 | Ordered by logged_at descending | Most recent first |
| CRD-157 | Requires CashbookClientIDPermissions | Cashbook only |

### 2.16 Credit Comments

| ID | Requirement | Details |
|----|------------|---------|
| CRD-160 | POST /comments/ creates comments | Array of {credit, comment} objects |
| CRD-161 | Comment max 3000 chars | Validation enforced |
| CRD-162 | User auto-set to request user | Read-only field |
| CRD-163 | Requires NomsOpsClientIDPermissions | Security staff action |
| CRD-164 | Returns 201 Created | With comment data |

### 2.17 Processing Batches

| ID | Requirement | Details |
|----|------------|---------|
| CRD-170 | POST /batches/ creates batch | Associates credit IDs with batch |
| CRD-171 | GET /batches/ lists user's own batches | Filtered by request.user |
| CRD-172 | DELETE /batches/{id}/ removes batch | Does not modify credits |
| CRD-173 | Batch has expired flag | True if creation >2 mins ago AND last credit modified >2 mins ago |
| CRD-174 | Ordered by -id (newest first) | Reverse chronological |
| CRD-175 | Requires CashbookClientIDPermissions | Cashbook only |

### 2.18 Private Estate Batches

| ID | Requirement | Details |
|----|------------|---------|
| CRD-180 | GET /private-estate-batches/ lists batches | Filter by date, date__gte, date__lt, prison |
| CRD-181 | GET /private-estate-batches/{ref}/ gets batch | Ref format: PRISON_CODE/YYYY-MM-DD |
| CRD-182 | PATCH /private-estate-batches/{ref}/ credits batch | Credits all credit_pending credits in batch |
| CRD-183 | GET /private-estate-batches/{ref}/credits/ lists credits | With billing_address in serializer |
| CRD-184 | total_amount aggregated | Sum of credit amounts |
| CRD-185 | Includes bank_account and remittance_emails | Prison's banking details |
| CRD-186 | Only private_estate=true prisons | Filtered by prison flag |
| CRD-187 | Requires BankAdminClientIDPermissions | Bank admin only |
| CRD-188 | PATCH requires change_privateestatebatch | Additional permission |
| CRD-189 | Auto-created during reconciliation | By manager when credits reconciled |

### 2.19 Credit Reconciliation

| ID | Requirement | Details |
|----|------------|---------|
| CRD-190 | Credits can be reconciled | Sets reconciled=True |
| CRD-191 | Only unreconciled credits eligible | reconciled=False filter |
| CRD-192 | Reconciliation is atomic | @atomic transaction |
| CRD-193 | Creates PrivateEstateBatch for private prisons | Automatic batch creation |
| CRD-194 | Sends credit_reconciled signal | Event notification |
| CRD-195 | Creates log entry (LogAction.reconciled) | Audit trail |

### 2.20 Credit Logging

| ID | Requirement | Details |
|----|------------|---------|
| CRD-200 | Log actions: created, credited, refunded, reconciled, reviewed, manual, failed | Full lifecycle tracking |
| CRD-201 | Log records user (nullable) | Null if user deleted or system action (failed) |
| CRD-202 | Log records timestamp | Auto-set on creation |
| CRD-203 | Logs visible in API response | Nested log_set in credit serializer |
| CRD-204 | Timestamp methods: credited_at, refunded_at, set_manual_at, reconciled_at | Derived from first log of each action type |

### 2.21 Credit Profile Attachment

| ID | Requirement | Details |
|----|------------|---------|
| CRD-210 | attach_profiles creates/updates PrisonerProfile | When prison and prisoner_name exist |
| CRD-211 | attach_profiles creates/updates SenderProfile | When sufficient sender detail exists |
| CRD-212 | Links sender to prisoner profile | Unless credit is failed |
| CRD-213 | Bank transfer requires: sender_name, sort_code, account_number | Minimum for profile creation |
| CRD-214 | Online payment requires: email, cardholder_name, card digits, expiry, address | Minimum for profile creation |
| CRD-215 | Failed credit detaches profiles | update_profiles_on_failed_state removes links |

### 2.22 Credit Prison Assignment

| ID | Requirement | Details |
|----|------------|---------|
| CRD-220 | Post-save signal updates prison from PrisonerLocation | Automatic on credit creation/update |
| CRD-221 | Only for unowned, pending/initial, unreconciled credits | Selective update |
| CRD-222 | Uses prisoner_number and prisoner_dob for lookup | PrisonerLocation match |
| CRD-223 | update_prisons manager method | Bulk update for pending/initial credits |

---

## 3. Disbursement

### 3.1 Disbursement Model

| ID | Requirement | Details |
|----|------------|---------|
| DSB-001 | Amount stored in pence | PositiveIntegerField |
| DSB-002 | Method: bank_transfer or cheque | Choice field |
| DSB-003 | Links to prison by nomis_id | ForeignKey |
| DSB-004 | Links to prisoner | prisoner_number, prisoner_name |
| DSB-005 | Recipient details stored | first_name, last_name, email, address, bank details |
| DSB-006 | recipient_is_company flag | Default false; affects name formatting |
| DSB-007 | recipient_name computed property | "FirstName LastName" or just company name |
| DSB-008 | invoice_number generated on confirm | Format: "PMD" + (BASE + id) |
| DSB-009 | nomis_transaction_id optional | Set during confirm action |

### 3.2 Disbursement State Machine

| ID | Requirement | Details |
|----|------------|---------|
| DSB-010 | Initial state: pending | Set on creation |
| DSB-011 | pending → preconfirmed | Via preconfirm action |
| DSB-012 | pending → rejected | Via reject action |
| DSB-013 | preconfirmed → confirmed | Via confirm action |
| DSB-014 | preconfirmed → pending | Via reset action |
| DSB-015 | preconfirmed → rejected | Via reject action |
| DSB-016 | confirmed → sent | Via send action |
| DSB-017 | rejected → pending | Via reset action |
| DSB-018 | sent is terminal | No transitions from sent |
| DSB-019 | Invalid transitions → 409 Conflict | Response includes invalid IDs |
| DSB-020 | Idempotent transitions | Already in target state = no-op |

### 3.3 Disbursement CRUD

| ID | Requirement | Details |
|----|------------|---------|
| DSB-030 | POST /disbursements/ creates disbursement | Returns 201 |
| DSB-031 | PrisonerInPrisonValidator | Prisoner must be active in specified prison |
| DSB-032 | PrisonPermittedValidator | User must be mapped to the prison |
| DSB-033 | prisoner_name auto-populated | From PrisonerLocation record |
| DSB-034 | Bank admin cannot create | Returns 403 |
| DSB-035 | PATCH only on pending disbursements | Non-pending → 400 "can no longer be modified" |
| DSB-036 | Prison access → 404 not 403 | User without access gets 404 |
| DSB-037 | Log entry on edit | LogAction.edited if changes made |
| DSB-038 | Empty update = no log | No signal/log for no-change PATCH |
| DSB-039 | Resolution field read-only | PATCH to resolution silently ignored |

### 3.4 Disbursement Bulk Actions

| ID | Requirement | Details |
|----|------------|---------|
| DSB-040 | POST /disbursements/actions/reject/ | Reject multiple by ID array |
| DSB-041 | POST /disbursements/actions/preconfirm/ | Preconfirm multiple |
| DSB-042 | POST /disbursements/actions/reset/ | Reset to pending |
| DSB-043 | POST /disbursements/actions/confirm/ | Confirm with optional nomis_transaction_id |
| DSB-044 | POST /disbursements/actions/send/ | Send (bank admin only) |
| DSB-045 | All actions return 204 on success | No response body |
| DSB-046 | All-or-nothing on invalid state | Partial failure = total failure (409) |
| DSB-047 | Confirm uses select_for_update | Pessimistic locking prevents races |
| DSB-048 | Confirm auto-generates invoice_number | "PMD" + (settings.INVOICE_NUMBER_BASE + id) |
| DSB-049 | Send requires BankAdminClientIDPermissions | Prison clerks get 403 |

### 3.5 Disbursement Filtering

| ID | Requirement | Details |
|----|------------|---------|
| DSB-060 | Filter by amount (exact, lte, gte) | Amount filters |
| DSB-061 | Filter by resolution | MultipleValueFilter |
| DSB-062 | Filter by method | bank_transfer or cheque |
| DSB-063 | Filter by prisoner_number | Case-insensitive exact |
| DSB-064 | Filter by prisoner_name | Case-insensitive substring |
| DSB-065 | Filter by recipient_name | Case-insensitive substring |
| DSB-066 | Filter by prison | Multiple prison IDs |
| DSB-067 | Filter by prison_region/category/population | Prison relation filters |
| DSB-068 | Filter by bank details | sort_code, account_number, roll_number |
| DSB-069 | Filter by postcode | Normalized postcode matching |
| DSB-070 | Filter by date ranges | created, logged_at with lt/gte |
| DSB-071 | Filter by monitored | Boolean: monitored prisoner/bank profiles |
| DSB-072 | simple_search across fields | Searches recipient name + prisoner number |
| DSB-073 | Ordering support | created, amount, resolution, method, names |

### 3.6 Disbursement Comments

| ID | Requirement | Details |
|----|------------|---------|
| DSB-080 | POST /disbursements/comments/ | Bulk create comments |
| DSB-081 | Comment links to disbursement | Required FK |
| DSB-082 | Comment max 3000 chars | Validation enforced |
| DSB-083 | Optional category field | Max 100 chars, default empty |
| DSB-084 | User auto-set | Read-only, set to request.user |
| DSB-085 | CashbookClientIDPermissions required | Only cashbook clients |

### 3.7 Disbursement Logging & Signals

| ID | Requirement | Details |
|----|------------|---------|
| DSB-090 | Log on create | LogAction.created |
| DSB-091 | Log on edit | LogAction.edited |
| DSB-092 | Log on reject | LogAction.rejected |
| DSB-093 | Log on confirm | LogAction.confirmed |
| DSB-094 | Log on send | LogAction.sent |
| DSB-095 | Signal on each state change | disbursement_created/edited/rejected/confirmed/sent |
| DSB-096 | All state changes atomic | @atomic on all write operations |

---

## 4. Payment

### 4.1 Payment Model

| ID | Requirement | Details |
|----|------------|---------|
| PAY-001 | UUID primary key | Auto-generated, immutable |
| PAY-002 | Amount in pence | PositiveIntegerField |
| PAY-003 | Service charge field | PositiveIntegerField, default 0 |
| PAY-004 | Card details optional | cardholder_name, first/last digits, expiry, brand |
| PAY-005 | IP address stored | GenericIPAddressField, nullable |
| PAY-006 | OneToOne link to Credit | Cascade delete |
| PAY-007 | Optional link to Batch | ForeignKey, nullable |
| PAY-008 | Billing address FK | Nullable, separate model |

### 4.2 Payment Status Lifecycle

| ID | Requirement | Details |
|----|------------|---------|
| PAY-010 | Initial status: pending | Set on creation |
| PAY-011 | pending → taken | Credit.resolution → pending, received_at set |
| PAY-012 | pending → failed | Credit stays initial (hidden from normal queries) |
| PAY-013 | pending → rejected | Credit.resolution → failed, profiles detached |
| PAY-014 | pending → expired | Credit.resolution → failed, profiles detached |
| PAY-015 | Non-pending → any = 409 Conflict | Cannot update taken/failed/rejected/expired |
| PAY-016 | Error message format | `{"errors": ["Payment cannot be updated in status \"X\""]}` |

### 4.3 Payment Creation

| ID | Requirement | Details |
|----|------------|---------|
| PAY-020 | POST /payments/ creates payment + credit | Returns 201 with uuid |
| PAY-021 | Required: prisoner_number, prisoner_dob, amount | Validation enforced |
| PAY-022 | Card details NOT allowed on create | Only set via subsequent PATCH |
| PAY-023 | IP address optional, nullable | Can be omitted or null |
| PAY-024 | Credit created with resolution=initial | Not visible in normal queries |
| PAY-025 | Credit amount = payment amount | Matched on creation |
| PAY-026 | Atomic creation | Payment + Credit created together or neither |
| PAY-027 | SendMoneyClientIDPermissions required | Other clients get 403 |

### 4.4 Payment Update (PATCH)

| ID | Requirement | Details |
|----|------------|---------|
| PAY-030 | PATCH /payments/{uuid}/ updates payment | Partial update |
| PAY-031 | Only pending payments updatable | Non-pending → 409 |
| PAY-032 | Card details can be added | cardholder_name, digits, expiry, brand |
| PAY-033 | Billing address creates or updates | First PATCH creates; subsequent updates in-place |
| PAY-034 | Only one billing address per payment | No duplicates created |
| PAY-035 | Invalid billing_address → 400 | Must be dict/object |
| PAY-036 | received_at can be set explicitly | ISO datetime; updates Credit.received_at |
| PAY-037 | received_at defaults to now on taken | If not explicitly provided |
| PAY-038 | Atomic update | Payment + billing address updated together |

### 4.5 Payment List & Retrieve

| ID | Requirement | Details |
|----|------------|---------|
| PAY-040 | GET /payments/ lists pending only | Automatic status=pending filter |
| PAY-041 | Filter by modified__lt | ISO datetime less-than |
| PAY-042 | Non-pending never in list | Even without explicit filter |
| PAY-043 | GET /payments/{uuid}/ retrieves single | Full payment details |
| PAY-044 | security_check nested in response | None if no check; object if exists |
| PAY-045 | security_check shows status + user_actioned | Boolean for actioned_by presence |

### 4.6 Payment Batches

| ID | Requirement | Details |
|----|------------|---------|
| PAY-050 | GET /batches/ lists batches | BankAdminClientIDPermissions required |
| PAY-051 | Filter by date | `date=YYYY-MM-DD` exact match |
| PAY-052 | payment_amount aggregated | Sum of all Payment.amount in batch |
| PAY-053 | Batch created during reconciliation | ref_code auto-incremented |

### 4.7 Payment Reconciliation

| ID | Requirement | Details |
|----|------------|---------|
| PAY-060 | Reconcile taken payments by date range | credit.received_at range query |
| PAY-061 | Only unreconciled credits | reconciled=False filter |
| PAY-062 | Creates batch with auto-incremented ref_code | Max previous + 1, or CARD_REF_CODE_BASE |
| PAY-063 | Sets credit.reconciled=True | Via Credit.objects.reconcile() |
| PAY-064 | Atomic operation | All-or-nothing |
| PAY-065 | No batch if no matching payments | Graceful no-op |

### 4.8 Abandoned Payment Cleanup

| ID | Requirement | Details |
|----|------------|---------|
| PAY-070 | Management command: clear_abandoned_payments | Deletes old failed payments |
| PAY-071 | Criteria: status=failed, credit.resolution=initial | Truly abandoned |
| PAY-072 | Age threshold configurable | Default 7 days, minimum 1 |
| PAY-073 | Age < 1 → CommandError | Validation enforced |
| PAY-074 | Deletes payment AND credit | Both removed |

---

## 5. Prison

### 5.1 Prison Model

| ID | Requirement | Details |
|----|------------|---------|
| PRS-001 | nomis_id is primary key | 3-char code (e.g., "INP", "IXB") |
| PRS-002 | short_name strips prefix | Removes HMP, YOI, STC, IRC, HMYOI prefixes |
| PRS-003 | Populations M2M | Links to population categories |
| PRS-004 | Categories M2M | Links to prison categories |
| PRS-005 | pre_approval_required flag | Boolean, default false |
| PRS-006 | private_estate flag | Boolean, default false |
| PRS-007 | use_nomis_for_balances flag | Boolean, default true |
| PRS-008 | GET /prisons/ is public | No authentication required (AllowAny) |
| PRS-009 | Filter: exclude_empty_prisons | Only prisons with active PrisonerLocations |

### 5.2 Prisoner Location Management

| ID | Requirement | Details |
|----|------------|---------|
| PRS-020 | POST /prisoner_locations/ batch creates | Array of location objects |
| PRS-021 | Requires NOMS_OPS or CASHBOOK client | Plus add_prisonerlocation permission |
| PRS-022 | Dict input rejected (must be array) | Returns 400 |
| PRS-023 | Invalid prison → 400 | "No prison found with code..." |
| PRS-024 | Invalid date format → 400 | Must be YYYY-MM-DD |
| PRS-025 | Existing locations deactivated | Same prisoner+prison → old set active=False |
| PRS-026 | New records set active=True | All bulk-created records are active |
| PRS-027 | created_by set to request user | Audit trail |
| PRS-028 | GET /prisoner_locations/{number}/ retrieves | Must be mapped to prisoner's prison |
| PRS-029 | User not mapped to prison → 403 | Specific error message |
| PRS-030 | Inactive location → 404 | Only active=True visible |

### 5.3 Prisoner Location Lifecycle Actions

| ID | Requirement | Details |
|----|------------|---------|
| PRS-040 | GET /prisoner_locations/can-upload/ | Returns {can_upload: bool} |
| PRS-041 | False if recent inactive records exist | Within 10-minute grace period |
| PRS-042 | POST /prisoner_locations/delete_old/ | Deletes active, activates inactive |
| PRS-043 | Requires NOMS_OPS client only | CASHBOOK client gets 403 |
| PRS-044 | Sends credit_prisons_need_updating signal | After delete_old |
| PRS-045 | Sends prisoner_profile_current_prisons_need_updating | After delete_old |
| PRS-046 | POST /prisoner_locations/delete_inactive/ | Deletes only inactive records |
| PRS-047 | delete_inactive sends NO signals | Unlike delete_old |

### 5.4 Prisoner Validity

| ID | Requirement | Details |
|----|------------|---------|
| PRS-050 | GET /prisoner_validity/ checks existence | Query params: prisoner_number, prisoner_dob |
| PRS-051 | Both params required | Missing either → 400 |
| PRS-052 | SendMoneyClientIDPermissions only | Other clients get 403 |
| PRS-053 | Returns count + results | count=1 if found, count=0 if not |
| PRS-054 | Matches active locations only | Inactive locations not matched |
| PRS-055 | Exact match on both fields | Both must match same record |

### 5.5 Prisoner Account Balance

| ID | Requirement | Details |
|----|------------|---------|
| PRS-060 | GET /prisoner_account_balances/{number}/ | Returns {combined_account_balance: int} |
| PRS-061 | NOMIS prison: sums cash+spends+savings | Three accounts from NOMIS API |
| PRS-062 | Private prison: uses PrisonerBalance | Internal record, not NOMIS |
| PRS-063 | Missing PrisonerBalance → 0 | Graceful fallback |
| PRS-064 | NOMIS malformed response → 400 | Missing key or non-integer value |
| PRS-065 | NOMIS 400 → relocate prisoner | Queries NOMIS for new location |
| PRS-066 | NOMIS 500/502/503/504/507 → return 0 | Tolerated errors, payment continues |
| PRS-067 | NOMIS other errors → 400 | Non-tolerated errors propagated |

### 5.6 Prisoner Credit Notice Emails

| ID | Requirement | Details |
|----|------------|---------|
| PRS-070 | GET /prisoner_credit_notice_email/ | Non-paginated list |
| PRS-071 | Filtered to user's mapped prisons | Only prisons user manages |
| PRS-072 | POST creates email config | Requires CashbookClient + IsUserAdmin |
| PRS-073 | PATCH updates email | Lookup by prison nomis_id |
| PRS-074 | Invalid email → 400 | Format validation |
| PRS-075 | User not mapped to prison → 403 | Access denied |

### 5.7 Population & Category

| ID | Requirement | Details |
|----|------------|---------|
| PRS-080 | GET /prison_populations/ lists populations | Requires authentication |
| PRS-081 | GET /prison_categories/ lists categories | Requires authentication |
| PRS-082 | Both are paginated | Standard DRF pagination |

---

## 6. Security

### 6.1 Security Checks

| ID | Requirement | Details |
|----|------------|---------|
| SEC-001 | Check auto-created when credit pending with card details | Requires cardholder_name, card numbers, expiry, address |
| SEC-002 | Non-matching rules → auto-accepted | Status=accepted, description="automatically accepted" |
| SEC-003 | Monitored prisoner → pending | Rule code FIUMONP |
| SEC-004 | Monitored sender → pending | Rule code FIUMONS |
| SEC-005 | Check stores array of rule codes | e.g., ['FIUMONP', 'FIUMONS', 'CSFREQ'] |
| SEC-006 | Check stores array of descriptions | Human-readable rule descriptions |
| SEC-007 | CSFREQ: sender frequency limit | Exceeded transactions in time period |
| SEC-008 | CSNUM: prisoner sender count limit | Too many different senders |
| SEC-009 | CPNUM: sender prisoner count limit | Sender sends to too many prisoners |
| SEC-010 | Incomplete payment details → no check | Missing card info = skipped |
| SEC-011 | Failed/non-initial credits → no check | Only initial resolution checked |

### 6.2 Check Accept/Reject

| ID | Requirement | Details |
|----|------------|---------|
| SEC-020 | POST /security/checks/{id}/accept/ | Requires decision_reason |
| SEC-021 | Accept sets status, actioned_at, actioned_by | All three fields populated |
| SEC-022 | Accept idempotent | Already accepted → no change |
| SEC-023 | Accept rejected check → ValidationError | Status conflict |
| SEC-024 | rejection_reasons not allowed on accept | Returns 400 |
| SEC-025 | POST /security/checks/{id}/reject/ | Requires decision_reason + rejection_reasons |
| SEC-026 | Reject sets status, actioned_at, actioned_by, reasons | All fields populated |
| SEC-027 | Reject idempotent | Already rejected → no change |
| SEC-028 | Reject accepted check → ValidationError | Status conflict |
| SEC-029 | Empty rejection_reasons → 400 | "This field cannot be blank" |
| SEC-030 | Both actions return 204 | No Content on success |

### 6.3 Check Auto-Accept Rules

| ID | Requirement | Details |
|----|------------|---------|
| SEC-040 | POST /security/checks/auto-accept/ creates rule | For (sender, prisoner) pair |
| SEC-041 | Requires states array with exactly 1 element | Validation error if != 1 |
| SEC-042 | Duplicate pair → validation error | Specific error message |
| SEC-043 | Active rule auto-accepts matching checks | Instead of pending |
| SEC-044 | Inactive rule → checks stay pending | Deactivated rule ignored |
| SEC-045 | PATCH adds new state, doesn't replace | Append-only state history |
| SEC-046 | Filter by is_active | Latest state's active value |
| SEC-047 | Filter by sender/prisoner IDs | Exact match filters |

### 6.4 Check List & Filtering

| ID | Requirement | Details |
|----|------------|---------|
| SEC-050 | GET /security/checks/ lists checks | NomsOpsClientIDPermissions required |
| SEC-051 | Prison clerk → 403 | Wrong role |
| SEC-052 | Filter by status | pending, accepted, rejected |
| SEC-053 | Filter by rules (substring) | rules__icontains |
| SEC-054 | Filter by date range | started_at__gte, started_at__lt |
| SEC-055 | Filter by sender_name | Case-insensitive substring |
| SEC-056 | Filter by prisoner_name/number | Substring or exact |
| SEC-057 | Filter by credit_resolution | Exact match |
| SEC-058 | Filter by actioned_by | Boolean: has been actioned |
| SEC-059 | Filter by assigned_to | User ID |

### 6.5 Profile Monitoring

| ID | Requirement | Details |
|----|------------|---------|
| SEC-060 | POST /security/senders/{id}/monitor/ | Add user to monitoring |
| SEC-061 | POST /security/senders/{id}/unmonitor/ | Remove user |
| SEC-062 | POST /security/prisoners/{id}/monitor/ | Add user to monitoring |
| SEC-063 | POST /security/prisoners/{id}/unmonitor/ | Remove user |
| SEC-064 | POST /security/recipients/{id}/monitor/ | Add user to monitoring |
| SEC-065 | POST /security/recipients/{id}/unmonitor/ | Remove user |
| SEC-066 | All return 204 | No content |
| SEC-067 | All are idempotent | Safe to call multiple times |
| SEC-068 | GET /monitored/ returns total count | Sum of all monitored entities |

### 6.6 Sender Profile Endpoints

| ID | Requirement | Details |
|----|------------|---------|
| SEC-070 | GET /security/senders/ lists sender profiles | Paginated with annotations |
| SEC-071 | Filter by simple_search | Across names, emails, card details |
| SEC-072 | Filter by source | bank_transfer, online, unknown |
| SEC-073 | Filter by bank details | sort_code, account_number, roll_number |
| SEC-074 | Filter by card details | last_digits, expiry_date |
| SEC-075 | Filter by prisoner/prison counts | gte/lte ranges |
| SEC-076 | Filter by credit counts/totals | gte/lte ranges |
| SEC-077 | Filter by monitoring status | Boolean |
| SEC-078 | Filter by prison/region/category/population | Prison relation filters |
| SEC-079 | Default ordering: -prisoner_count | Highest first |
| SEC-080 | GET /security/senders/{id}/credits/ | Nested credits for sender |

### 6.7 Prisoner Profile Endpoints

| ID | Requirement | Details |
|----|------------|---------|
| SEC-090 | GET /security/prisoners/ lists prisoner profiles | With annotations |
| SEC-091 | Filter by prisoner_name, prisoner_number | Substring and exact |
| SEC-092 | Filter by current_prison | Current prison ID(s) |
| SEC-093 | Filter by senders | Related sender profile IDs |
| SEC-094 | Filter by sender/credit/disbursement counts | gte/lte ranges |
| SEC-095 | Default ordering: -sender_count | Highest first |
| SEC-096 | GET /security/prisoners/{id}/credits/ | Nested credits |
| SEC-097 | GET /security/prisoners/{id}/disbursements/ | Nested disbursements |
| SEC-098 | provided_names array | Alternative names from payments |

### 6.8 Recipient Profile Endpoints

| ID | Requirement | Details |
|----|------------|---------|
| SEC-100 | GET /security/recipients/ lists recipients | Excludes those without bank details |
| SEC-101 | Filter by bank details | sort_code, account_number, roll_number |
| SEC-102 | Filter by prisoner/prison counts | gte/lte ranges |
| SEC-103 | Filter by disbursement counts/totals | gte/lte ranges |
| SEC-104 | Default ordering: -prisoner_count | Highest first |
| SEC-105 | GET /security/recipients/{id}/disbursements/ | Nested disbursements |

### 6.9 Monitored Partial Email Addresses

| ID | Requirement | Details |
|----|------------|---------|
| SEC-110 | POST /security/monitored-email-addresses/ | Creates keyword (raw string body) |
| SEC-111 | Keyword stored lowercase | Serializer converts |
| SEC-112 | Minimum 3 characters | Returns 400 if shorter |
| SEC-113 | Duplicate keyword (case-insensitive) → 400 | Unique constraint |
| SEC-114 | GET lists all keywords | Alphabetically sorted strings |
| SEC-115 | DELETE by keyword | Case-insensitive lookup, returns 204 |
| SEC-116 | FIU users only | Non-FIU security staff → 403 |
| SEC-117 | Matching is case-insensitive substring | Keyword found within email |

### 6.10 Saved Searches

| ID | Requirement | Details |
|----|------------|---------|
| SEC-120 | POST /security/searches/ creates search | With description, endpoint, filters |
| SEC-121 | GET lists user's own searches only | Scoped to request.user |
| SEC-122 | PATCH updates search | Replaces filters (delete + recreate) |
| SEC-123 | DELETE removes search | 404 if not user's own |
| SEC-124 | Auto-set user to request.user | Read-only field |

### 6.11 Security Profile Background Updates

| ID | Requirement | Details |
|----|------------|---------|
| SEC-130 | update_security_profiles command | Aggregates counts for all profile types |
| SEC-131 | Updates credit_count and credit_total | For sender and prisoner profiles |
| SEC-132 | Updates disbursement_count and disbursement_total | For prisoner and recipient profiles |
| SEC-133 | Links profiles to prisons | Via credit/disbursement associations |
| SEC-134 | Links sender profiles to prisoner profiles | Via credit relationships |
| SEC-135 | Updates cardholder names and emails | For debit card sender details |
| SEC-136 | Incremental on subsequent runs | Only new transactions added |
| SEC-137 | update_current_prisons command | Updates prisoner current_prison from PrisonerLocation |
| SEC-138 | bulk_unmonitor command | Removes user from all monitoring + deletes searches |

---

## 7. Authentication & User Management

### 7.1 OAuth2 & Login

| ID | Requirement | Details |
|----|------------|---------|
| AUTH-001 | OAuth2 password grant token endpoint | Valid credentials → access token |
| AUTH-002 | Failed login tracking | FailedLoginAttempt records created |
| AUTH-003 | Account lockout after N failures | Configurable count and period |
| AUTH-004 | Lockout email notification | Template: api-account-locked |
| AUTH-005 | Login record on success | Login model with user + application |
| AUTH-006 | Service accounts excluded from login tracking | Specific usernames ignored |

### 7.2 User CRUD

| ID | Requirement | Details |
|----|------------|---------|
| AUTH-010 | GET /users/ lists users | Filtered by role/prison access |
| AUTH-011 | GET /users/{id}/ retrieves user details | Includes permissions, prisons, lock status |
| AUTH-012 | POST /users/ creates user | Username, email, role, optional prison |
| AUTH-013 | PATCH /users/{id}/ updates user | Email, name, prisons |
| AUTH-014 | DELETE /users/{id}/ deactivates | Sets is_active=False, not hard delete |
| AUTH-015 | Case-insensitive username uniqueness | Duplicate usernames rejected |
| AUTH-016 | Unique email validation | Duplicate emails rejected |
| AUTH-017 | User unlock endpoint | Clears failed login attempts |
| AUTH-018 | Self-update limited | Cannot change own role/prisons |

### 7.3 Roles

| ID | Requirement | Details |
|----|------------|---------|
| AUTH-020 | GET /roles/ lists all roles | Any authenticated user |
| AUTH-021 | Filter to managed roles | Admin sees only manageable roles |
| AUTH-022 | Role defines group + application | key_group, other_groups, application |
| AUTH-023 | Role assignment sets groups | Adding role adds user to role's groups |

### 7.4 User Flags

| ID | Requirement | Details |
|----|------------|---------|
| AUTH-030 | POST creates flag on user | (user, flag_name) unique pair |
| AUTH-031 | GET lists user's own flags | Or managed users' flags |
| AUTH-032 | DELETE removes flag | Own or managed users' flags |
| AUTH-033 | Non-admin cannot see others' flags | Authorization enforced |

### 7.5 Password Management

| ID | Requirement | Details |
|----|------------|---------|
| AUTH-040 | POST /change_password/ changes password | Requires old + new password |
| AUTH-041 | Wrong old password increments failed count | Security tracking |
| AUTH-042 | Success clears failed attempts | For that application |
| AUTH-043 | POST /reset_password/ sends reset | By username or email |
| AUTH-044 | Reset sends email with temp password or link | Configurable behavior |
| AUTH-045 | Password change via reset code | UUID-based, no auth needed |
| AUTH-046 | Immutable accounts cannot reset | Service accounts protected |
| AUTH-047 | Locked accounts cannot reset | Must be unlocked first |
| AUTH-048 | No email → cannot reset | Email required |
| AUTH-049 | Multiple users same email → disambiguation | System requests username |

### 7.6 Prison User Mapping

| ID | Requirement | Details |
|----|------------|---------|
| AUTH-050 | Assign prisons to user | Many-to-many mapping |
| AUTH-051 | Get prison set for user | All mapped prisons |
| AUTH-052 | Copy prison mapping | From one user to another |
| AUTH-053 | Prison-based access control | Users must share prison for visibility |
| AUTH-054 | FIU exception | FIU users see all FIU users regardless of prison |

### 7.7 Account Requests

| ID | Requirement | Details |
|----|------------|---------|
| AUTH-060 | POST /requests/ creates account request | No auth required |
| AUTH-061 | GET /requests/ lists pending requests | Filtered by manageable roles/prisons |
| AUTH-062 | Detect existing username | Provides existing user's roles |
| AUTH-063 | PATCH accepts request → creates/updates user | Account created or role changed |
| AUTH-064 | Account created email sent | Notification on acceptance |
| AUTH-065 | Role change email sent | Different template |
| AUTH-066 | DELETE rejects request | Rejection email sent |
| AUTH-067 | Ordered by creation date | Newest or oldest first |

### 7.8 Job Information

| ID | Requirement | Details |
|----|------------|---------|
| AUTH-070 | POST creates job info | title, prison_estate, tasks |
| AUTH-071 | Auto-linked to request user | Read-only user field |
| AUTH-072 | Authentication required | 401 if not authenticated |

---

## 8. Transaction

### 8.1 Transaction Model

| ID | Requirement | Details |
|----|------------|---------|
| TXN-001 | Transaction stores amount, category, source | Core fields |
| TXN-002 | Sender info: sort_code, account_number, name, roll_number | Bank details |
| TXN-003 | ref_code for reconciliation | 6-digit code |
| TXN-004 | OneToOne link to Credit | Nullable |
| TXN-005 | incomplete_sender_info flag | Boolean |
| TXN-006 | reference_in_sender_field flag | Boolean |
| TXN-007 | processor_type_code optional | Payment processor tracking |

### 8.2 Transaction Status (Computed)

| ID | Requirement | Details |
|----|------------|---------|
| TXN-010 | Creditable | Credit exists, prison assigned, not blocked |
| TXN-011 | Refundable | Credit exists, sender info complete, no prison or blocked |
| TXN-012 | Unidentified | Incomplete sender, no prison, bank_transfer credit |
| TXN-013 | Anonymous | Incomplete sender, bank_transfer, no credit |
| TXN-014 | Anomalous | Credit category + administrative source |

### 8.3 Transaction Endpoints

| ID | Requirement | Details |
|----|------------|---------|
| TXN-020 | POST /transactions/ bulk creates | Array of transaction objects |
| TXN-021 | Auto-creates Credit for bank_transfer credits | Category=credit, source=bank_transfer |
| TXN-022 | BankAdminClientIDPermissions required | Other clients → 403 |
| TXN-023 | PATCH /transactions/ bulk refunds | Mark as refunded |
| TXN-024 | Refund conflict → 409 | Invalid credit state |
| TXN-025 | Filter by status | creditable, refundable, unidentified, etc. |
| TXN-026 | Filter by received_at range | gte/lt datetime |
| TXN-027 | Filter by multiple IDs | pk parameter |
| TXN-028 | POST /transactions/reconcile/ | Date range reconciliation |
| TXN-029 | Reconcile requires both date boundaries | received_at__gte and received_at__lt |
| TXN-030 | Reconcile creates PrivateEstateBatch | For private prisons |

---

## 9. Notification

### 9.1 Events

| ID | Requirement | Details |
|----|------------|---------|
| NOT-001 | Event has rule code | Validated against known rules |
| NOT-002 | Events can be user-specific or global | user=null for global |
| NOT-003 | GET /events/ lists user's events | Own events + global events |
| NOT-004 | Filter by rule code | Multiple values |
| NOT-005 | Filter by triggered_at range | gte/lt datetime |
| NOT-006 | Ordered by triggered_at desc, then id | Most recent first |
| NOT-007 | GET /events/pages/ returns date pagination | oldest/newest dates + count |
| NOT-008 | GET /rules/ lists enabled rules | With descriptions |

### 9.2 Email Preferences

| ID | Requirement | Details |
|----|------------|---------|
| NOT-010 | GET /emailpreferences/ returns frequency | Default 'never' if none exists |
| NOT-011 | POST /emailpreferences/ sets frequency | DAILY, NEVER, etc. |
| NOT-012 | Update-or-create semantics | Creates if not exists, updates if exists |

---

## 10. Performance & Reporting

### 10.1 Digital Takeup

| ID | Requirement | Details |
|----|------------|---------|
| PRF-001 | Unique on (date, prison) | Constraint enforced |
| PRF-002 | Tracks credits_by_post and credits_by_mtp | Integer counts |
| PRF-003 | Calculated digital_takeup property | mtp / (post + mtp) |
| PRF-004 | Per-month aggregation | Across prison set |
| PRF-005 | Mean digital takeup | Average across queryset |
| PRF-006 | Can exclude private estate | Filter option |

### 10.2 User Satisfaction

| ID | Requirement | Details |
|----|------------|---------|
| PRF-010 | Daily ratings (rated_1 through rated_5) | Very dissatisfied to very satisfied |
| PRF-011 | Date is primary key | One record per day |
| PRF-012 | Percentage satisfied calculation | (rated_4 + rated_5) / total |
| PRF-013 | Mean percentage satisfied | Average across range |

### 10.3 Performance Data Endpoint

| ID | Requirement | Details |
|----|------------|---------|
| PRF-020 | GET /performance/data/ lists weekly data | SendMoneyClientIDPermissions |
| PRF-021 | Default: last 52 weeks | If no date params |
| PRF-022 | Filter by week range | week__gte, week__lt |
| PRF-023 | Returns formatted percentages | e.g., "95%" |
| PRF-024 | Headers response for CSV | Field verbose names |

---

## 11. Service Availability

### 11.1 Downtime

| ID | Requirement | Details |
|----|------------|---------|
| SVC-001 | GET /service-availability/ is public | No authentication |
| SVC-002 | Returns status per service | {status: true/false} for each |
| SVC-003 | Includes wildcard (*) status | True only if all services up |
| SVC-004 | Active downtime includes end time and message | downtime_end, message_to_users |
| SVC-005 | Null end = ongoing downtime | If started but no end set |

### 11.2 Notifications

| ID | Requirement | Details |
|----|------------|---------|
| SVC-010 | GET /notifications/ lists active notifications | Date-filtered |
| SVC-011 | Public notifications visible without auth | public=true flag |
| SVC-012 | Filter by target prefix | target__startswith query param |
| SVC-013 | Notification has level | info, warning, error, success |
| SVC-014 | Active = now between start and end | Time-windowed visibility |

---

## 12. User Event Log

| ID | Requirement | Details |
|----|------------|---------|
| UEL-001 | UserEvent records user actions | BigAutoField ID, timestamped |
| UEL-002 | Captures request user and path | Auto-populated |
| UEL-003 | JSON data field | Nullable, stores structured data |
| UEL-004 | FlexibleDjangoJSONEncoder | Never raises TypeError; uses pk or str() fallback |
| UEL-005 | DateTime serialized as ISO 8601 | With timezone info |
| UEL-006 | Ordered by timestamp desc, pk desc | Most recent first |

---

## 13. Core Infrastructure

### 13.1 File Downloads

| ID | Requirement | Details |
|----|------------|---------|
| COR-001 | GET /file-downloads/ lists downloads | Tracked by label + date |
| COR-002 | POST /file-downloads/ creates record | Label + date unique |
| COR-003 | GET /file-downloads/missing/ reports failures | Missing downloads |

### 13.2 Scheduled Commands

| ID | Requirement | Details |
|----|------------|---------|
| COR-010 | ScheduledCommand stores command + cron | Validated command name |
| COR-011 | next_execution calculated | From cron expression |
| COR-012 | delete_after_next flag | One-time execution support |
| COR-013 | is_scheduled check | now >= next_execution |
| COR-014 | Run updates next_execution | After successful execution |

### 13.3 Permissions Framework

| ID | Requirement | Details |
|----|------------|---------|
| COR-020 | ActionsBasedPermissions | Maps HTTP method to Django permission |
| COR-021 | NomsOpsClientIDPermissions | Restricts to noms-ops OAuth client |
| COR-022 | SendMoneyClientIDPermissions | Restricts to send-money OAuth client |
| COR-023 | BankAdminClientIDPermissions | Restricts to bank-admin client |
| COR-024 | CashbookClientIDPermissions | Restricts to cashbook client |

---

## 14. Cross-Cutting Concerns

### 14.1 Authentication & Authorization Patterns

| ID | Requirement | Details |
|----|------------|---------|
| XCT-001 | All endpoints require IsAuthenticated unless explicitly public | /prisons/ and /service-availability/ are exceptions |
| XCT-002 | OAuth client ID permissions enforce application-level access | Different apps (cashbook, noms-ops, bank-admin, send-money) have different access |
| XCT-003 | ActionsBasedPermissions map CRUD to Django permissions | create→add, update→change, delete→delete, list/retrieve→view |
| XCT-004 | Prison-scoped access control | Users see only data for their mapped prisons |

### 14.2 Pagination

| ID | Requirement | Details |
|----|------------|---------|
| XCT-010 | Standard DRF pagination on list endpoints | count, next, previous, results |
| XCT-011 | Some endpoints explicitly non-paginated | e.g., prisoner_credit_notice_email |

### 14.3 Filtering

| ID | Requirement | Details |
|----|------------|---------|
| XCT-020 | DjangoFilterBackend used throughout | Query parameter filtering |
| XCT-021 | MultipleValueFilter supports comma-separated values | For multi-select fields |
| XCT-022 | PostcodeFilter normalizes postcodes | Removes spaces/hyphens, uppercases |
| XCT-023 | SplitTextInMultipleFieldsFilter for search | Space-separated terms across fields |
| XCT-024 | Date range filters use lt/gte convention | Exclusive upper, inclusive lower |

### 14.4 Atomicity & Data Integrity

| ID | Requirement | Details |
|----|------------|---------|
| XCT-030 | Bulk actions are all-or-nothing | Any invalid item fails entire batch |
| XCT-031 | State transitions use select_for_update | Pessimistic locking where needed |
| XCT-032 | @atomic decorators on serializer create/update | Transaction safety |
| XCT-033 | Signals fired within atomic blocks | Consistent state on notification |

### 14.5 Error Response Conventions

| ID | Requirement | Details |
|----|------------|---------|
| XCT-040 | 400 Bad Request for validation errors | Field-level error details |
| XCT-041 | 401 Unauthorized for missing auth | No token |
| XCT-042 | 403 Forbidden for insufficient permissions | Wrong role/client |
| XCT-043 | 404 Not Found for access-controlled missing resources | Prison filtering returns 404 not 403 |
| XCT-044 | 409 Conflict for invalid state transitions | Includes error message and conflicting IDs |

---

## Requirement Count Summary

| Domain | Requirement Count |
|--------|------------------|
| Account (Balance) | 19 |
| Credit | 128 |
| Disbursement | 47 |
| Payment | 38 |
| Prison | 34 |
| Security | 69 |
| Authentication & User Management | 38 |
| Transaction | 18 |
| Notification | 12 |
| Performance & Reporting | 10 |
| Service Availability | 9 |
| User Event Log | 6 |
| Core Infrastructure | 11 |
| Cross-Cutting Concerns | 15 |
| **Total** | **~454** |

---

## Notes for Kotlin Test Implementation

### Recommended Test Structure
```
src/test/kotlin/uk/gov/justice/digital/hmpps/
  integration/
    account/         # ACC-* requirements
    credit/          # CRD-* requirements
    disbursement/    # DSB-* requirements
    payment/         # PAY-* requirements
    prison/          # PRS-* requirements
    security/        # SEC-* requirements
    auth/            # AUTH-* requirements
    transaction/     # TXN-* requirements
    notification/    # NOT-* requirements
    performance/     # PRF-* requirements
    service/         # SVC-* requirements
  unit/
    model/           # Model validation, computed properties
    service/         # Business logic unit tests
    util/            # Encoder, filter, permission tests
```

### Test Technology Mapping
| Python/Django | Kotlin/Spring Boot |
|--------------|-------------------|
| `django.test.TestCase` | `@SpringBootTest` + `WebTestClient` |
| `model-bakery` (Baker) | Test factory functions or Kotlin test fixtures |
| `Faker` | `io.github.serpro69:kotlin-faker` |
| `django.test.Client` | `WebTestClient` or `MockMvc` |
| Django signals | Spring `ApplicationEvent` / `@EventListener` |
| DRF permissions | Spring Security `@PreAuthorize` / method security |
| `django-filter` | Spring Data JPA Specifications or custom filters |
| `@atomic` | `@Transactional` |
| `select_for_update()` | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |
| OAuth2 provider (DOT) | Spring Security OAuth2 Resource Server |
| WireMock (NOMIS calls) | `org.wiremock:wiremock-standalone` |
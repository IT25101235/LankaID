# LankaID implementation status and remaining workflow

## Current completion estimate: approximately 82%

This is a repository-based engineering estimate, not a count of files or lines of code. Each proposed-system area is weighted by its importance and scored against the workflow described in `README.md`.

| Proposed-system area | Weight | Completed | Repository evidence / gap |
|---|---:|---:|---|
| Requirements and data design | 10% | 8% | EER and relational blueprint are documented; the final assignment PDFs and a schema matching every conceptual entity are still outstanding. |
| Authentication and role accounts | 10% | 9% | Citizen registration, citizen/officer/admin login, account status and role routing work; production security hardening is still needed. |
| NIC/passport applications | 15% | 14% | New, renewal, lost/replacement, correction and resubmission workflows are implemented for both services. |
| Uploads and payment | 10% | 9% | Supporting-document upload and payment state are persisted; payment remains a simulated gateway. |
| Verification and decisions | 10% | 10% | Separate queues, document review, approval, rejection, correction requests and processed views are implemented. |
| Appointments and biometrics | 10% | 9% | Booking, QR passes, check-in and collection state work; hardware capture is intentionally simulated. |
| Printing, issuance and delivery | 10% | 6% | Printing-ready selection, CSV export and delivery/collection tracking work; persistent printing batches and issued identity-document records are missing. |
| Support and notifications | 8% | 7% | Citizen/CSR support requests, responses and notifications work; support is single-response rather than a message thread. |
| Administration and director views | 8% | 7% | Officer/centre management, notification composition, metrics and export are present; director review auditing is not persisted. |
| Quality, security and deployment readiness | 9% | 3% | Automated unit/integration coverage is substantial and the UI now has one responsive portal shell; migrations, production security, browser QA and deployment automation remain. |
| **Total** | **100%** | **82%** | Core end-to-end demonstration workflow is present; the remaining work is mainly schema completeness, production integrations and hardening. |

The percentage should be revised after each remaining phase is accepted against testable completion criteria.

## Remaining-work workflow

### Phase 1 - Align the physical database with the proposed EER

1. Add `ApplicationStatusHistory` and write a row for every application status transition.
2. Add `PrintingBatch` and `PrintingBatchItem`; replace the current export-only operation with a persisted, auditable batch.
3. Add `IdentityDocument`; create the issued NIC/passport record only after a completed printing batch.
4. Add `SupportMessage`; migrate the one-message/one-response support model to a conversation history.
5. Add director review-audit fields/table and, if it remains in the final EER, officer supervision.
6. Introduce versioned migrations (recommended: Flyway) instead of relying on one manual setup script.

Exit criterion: every entity and relationship shown in the submitted EER has a physical table/constraint or a documented mapping decision, and Hibernate validation succeeds on a clean database.

### Phase 2 - Complete the business lifecycle

1. Persist printing batch creation, inclusion, export, re-export and failure state.
2. Generate and persist an issued document number, issue date, expiry date and document status.
3. Connect delivery/collection to the issued document, not only to the application.
4. Add threaded citizen/CSR messages and attachments.
5. Add director audit decisions and an audit trail for administrative changes.

Exit criterion: a new application can move from registration to a persisted issued document, and every important transition can be reconstructed from database history.

### Phase 3 - Replace demonstrations with production integrations

1. Integrate a real payment provider behind a payment-adapter interface; retain a sandbox implementation for demos/tests.
2. Integrate an approved biometric device/provider behind a capture-adapter interface; never store raw biometric data without the required encryption and legal controls.
3. Move uploaded documents to protected storage, validate file signatures, scan uploads, and use expiring authorized download links.
4. Move secrets and environment-specific connection settings out of source-controlled properties.

Exit criterion: simulated integrations are explicitly selected only in development/test profiles, while the production profile fails safely if required providers are not configured.

### Phase 4 - Security and quality gate

1. Add Spring Security route rules, CSRF protection, secure session cookies, session expiry and login throttling.
2. Add authorization tests for every citizen and officer endpoint, including cross-citizen data access.
3. Run desktop/mobile browser tests for all role workflows and complete accessibility checks (keyboard, labels, focus, contrast and screen-reader landmarks).
4. Replace runtime CDN dependencies with versioned local production assets or a controlled build pipeline.
5. Add centralized error pages, structured audit logging, health checks and backup/restore tests.

Exit criterion: all automated tests pass, no high-severity security findings remain, and every live page passes the shared sidebar/logo/color audit.

### Phase 5 - Delivery and assignment completion

1. Produce the required 2-3 page requirement specification PDF.
2. Export the final Chen/EER diagram to the required PDF size and validate notation/cardinalities.
3. Document setup, demo accounts, environment variables, database backup/restore and deployment.
4. Perform a clean-machine rehearsal using `DATABASE_SETUP.sql`, then run the complete end-to-end acceptance workflow.

Exit criterion: a teammate can clone the repository, create the database, configure the application, run tests and demonstrate every accepted workflow using only the delivered documentation.

## Database tables added after the original account/application foundation

Git history shows the original foundation as `UserAccount`, `CitizenAccount`, `StaffAccount`, `GovernmentIdentityRecord`, `Application`, `NICApplication`, `PassportApplication` and `ApplicationDocument`.

The operational workflow later added these 11 tables:

| Table | Purpose |
|---|---|
| `Centre` | Service-centre details and capacity. |
| `Appointment` | Citizen biometric bookings and guest walk-in appointments. |
| `AppointmentCheckIn` | Officer check-in event and method. |
| `BiometricCollection` | Per-application biometric completion record. |
| `Payment` | Payment attempt/result associated with an appointment. |
| `ApplicationReview` | Officer review sequence, decision and correction message. |
| `Notification` | Broadcast or generated notification content. |
| `UserNotification` | Notification recipients, delivery time and read time. |
| `SupportRequest` | Citizen issue/complaint and CSR response state. |
| `DocumentDelivery` | Home-delivery or centre-collection tracking. |
| `WalkInSlot` | CSR-published half-hour walk-in capacity. |

The current application maps **19 unique physical tables** in total. `AdminAccount` and `StaffAccount` are two Java views of the same physical `StaffAccount` table, so they are counted once.

## UI consistency decision

All live citizen and officer pages now load the shared `portal-shell.js` component and `portal-shell.css` theme. The shared shell provides:

- the login-page `account_balance` logo and LankaID wordmark;
- one dark-blue secondary sidebar;
- one white/light-white primary workspace;
- role-specific links for citizens, NIC/passport officers, CSR, director and system administrator;
- active-page indication and a responsive mobile drawer;
- sidebar suppression during printing.

Thirteen disconnected legacy templates were removed after confirming that no controller renders them. Login, registration, the public landing page and the standalone public chatbot intentionally keep task-appropriate public/auth layouts while using the same logo treatment.

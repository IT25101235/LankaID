# Remaining Hardcoded Values Review

This file records the hardcoded values that still remain in the LankaID project.
The fake complaint and walk-in data previously found in the CSR dashboard are not
included because they have already been replaced with database-backed values.

No application code was changed as part of creating this document.

## 1. Critical: default administrator credentials

File: `src/main/java/lk/sliit/identityissuanceservice/Admin_Mgmt/Service/AdminAccountService.java`

Current hardcoded values:

- Administrator email: `admin@gov.lk`
- Administrator password: `admin12345`
- Administrator phone: `+94110000000`
- Employee number: `ADMIN-001`
- Role: `SYSTEM_ADMIN`

The most serious issue is that `createDefaultAdminAccount()` resets the existing
administrator password to `admin12345` whenever the application starts. This means
that changing the password in the database does not permanently secure the account.

The default login is also documented in `README.md` around lines 1569-1570.

Recommended simple solution:

- Read the initial administrator email and password from environment variables.
- Create the administrator only when it does not already exist.
- Never reset an existing administrator password during application startup.

## 2. High: environment-specific server addresses

File: `src/main/resources/application.properties`

Current values:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=identity_issuance_service;integratedSecurity=true;encrypt=true;trustServerCertificate=true;sendTimeAsDatetime=false;
app.sms.url=http://10.128.24.26:8080/sms
```

The database URL only works with the expected local SQL Server setup. The SMS URL
contains a private network IP that may change when the phone reconnects to a network.

Recommended simple solution:

```properties
spring.datasource.url=${LANKAID_DB_URL:jdbc:sqlserver://localhost:1433;databaseName=identity_issuance_service;integratedSecurity=true;encrypt=true;trustServerCertificate=true;sendTimeAsDatetime=false;}
app.sms.url=${LANKAID_SMS_URL:}
```

The SMS API key, email password and chatbot API key already use environment
variables and do not need the same correction.

## 3. Medium: fees fixed in Java and repeated in HTML

### Backend fee values

- `DocumentDeliveryService.java`
  - NIC home delivery: `500.00`
  - Passport home delivery: `2000.00`
- `WalkInPaymentService.java`
  - Walk-in booking fee: `1500.00`
- `AppointmentChangePaymentService.java`
  - Appointment rescheduling fee: `300.00`

### Repeated display values

- `templates/citizen_frontends/booking_change.html` repeats `LKR 300`.
- `templates/citizen_frontends/walkin_appointment_recovery.html` repeats `LKR 300`.
- `templates/citizen_frontends/payment_gateway.html` repeats the `LKR 500.00`
  and `LKR 2,000.00` delivery fees.

If a backend fee changes, the page can display the old amount.

Recommended simple solution:

- Keep each fee in one backend constant or configuration property.
- Pass the amount from the controller to the template.
- A new database table is only necessary if an administrator must be able to edit
  these fees through the website.

The NIC and passport application fees are already stored in
`ApplicationFeeSettings` and managed through the administrator functionality.
Their Java values are fallback defaults rather than the main source of truth.

## 4. Medium: email OTP expiry property mismatch

File: `src/main/resources/application.properties`

The configuration contains:

```properties
app.registration.email-code-minutes=10
```

However, `EmailOtpService.java` reads:

```text
app.otp.email-ttl-seconds
```

The `email-code-minutes` property is therefore unused. Email codes currently last
ten minutes only because the Java fallback is hardcoded to `600` seconds.

Recommended simple solution: use one property name and one unit everywhere, for
example:

```properties
app.otp.email-ttl-seconds=600
```

## 5. Medium: inconsistent officer email domain

The backend registration validation accepts only officer emails ending in
`@gov.lk`. This rule appears in:

- `OfficerAccountService.java`
- `OfficerRegistrationDto.java`
- `OfficerUpdateDto.java`
- `templates/officers_frontends/admin_dashboard.html`

However, `templates/login_portal.html` displays this placeholder:

```text
name@lankaid.gov.lk
```

Recommended simple solution: decide which domain is correct and use it consistently.
If `@gov.lk` is the project requirement, only the login placeholder needs changing.

## 6. Low: fixed public contact and copyright year

File: `src/main/resources/templates/index.html`

Current values:

- Support address: `support@lankaid.gov.lk`
- Copyright year: `2026`

The support address can remain hardcoded if it is the permanent official address.
The year should preferably be produced dynamically so it does not need annual edits.

## 7. Low: duplicated upload limit

The 5 MB document upload limit is declared separately in:

- `CitizenApplicationService.java`
- `DocumentRenewalReplacementService.java`

The values currently match, but they could become inconsistent after a future change.

Recommended simple solution: define one application property such as:

```properties
app.upload.max-file-size=5MB
```

Both services can read the same setting.

## 8. Low: appointment policy numbers

Several appointment rules are fixed directly in Java:

- Appointment booking window: up to 90 days.
- Walk-in schedule display window: 14 days.
- Biometric schedule publication limit: 63 days.
- Appointment slot interval: 30 minutes.
- Biometric cancellation/rescheduling threshold: 3 days.
- Page size: 15 records.

These are valid business rules and do not have to be moved immediately. Named
constants or application properties would make future policy changes easier.

## 9. Low: fixed example years and cache versions

Some placeholders contain examples such as `BIO-2026-...`, `WALK-2026-...` and
`LK-NIC-2026-...`. Several CSS and JavaScript links also use date-based cache
versions such as `?v=20261008`.

These values do not affect saved database references. Real references are generated
using the current year. The examples may nevertheless look outdated in later years.

## 10. Repository cleanup: tracked temporary files

The repository currently tracks files under `tmp/`, including:

- Test logs
- Rendered screenshots
- Captured served HTML
- A captured login page containing an old `jsessionid`

This is not an application hardcoding issue, but temporary output should normally
not be committed.

Recommended simple solution:

```gitignore
/tmp/
```

After adding the ignore rule, already tracked temporary files would also need to be
removed from Git tracking. This should only be done after confirming that none of
the screenshots or review artifacts are still required by the project team.

## Values that should remain in code

Not every fixed value is a problem. The following are normal domain constants and
should not automatically be moved to configuration:

- Status codes such as `ACTIVE`, `PAID`, `COMPLETED` and `RESOLVED`
- Staff roles such as `NIC_OFFICER`, `PASSPORT_OFFICER` and `SYSTEM_ADMIN`
- Document-type codes
- Database column lengths
- Form validation limits
- Example values used only in automated tests

## Suggested correction order

1. Remove the fixed administrator password and startup password reset.
2. Make the SMS URL and database URL environment-configurable.
3. Stop repeating fee amounts in HTML templates.
4. Correct the email OTP property mismatch.
5. Make the officer email domain consistent.
6. Handle the lower-priority maintenance items when convenient.

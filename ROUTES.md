# LankaID UI Route Guide

The project uses Spring MVC controllers, `HttpSession`, services and Spring Data JPA repositories. Citizen application pages read and save data through the existing SQL Server schema.

## How a route works

```java
@GetMapping("/citizen/applications/nic/{formKind:new|renewal|replacement}")
public String nicApplicationForm(@PathVariable String formKind, ...) {
    // The service checks document ownership before returning the shared form.
    return "citizen_frontends/nic_application_form";
}
```

- `@GetMapping` is the URL opened by the browser.
- `Model` passes the form and database values to Thymeleaf.
- The returned string is the HTML file inside `src/main/resources/templates` without `.html`.
- `POST` routes call `CitizenApplicationService` to validate and save the application.

## Public and citizen routes

| Route | Screen | Controller area |
|---|---|---|
| `/` | Landing page | User Management |
| `/login` | Citizen/officer login | User Management |
| `/register` | Citizen registration | User Management |
| `/chatbot` | LankaID virtual assistant demo | Citizen Support Management |
| `/citizen/dashboard` | Eligibility and citizen application dashboard | Citizen Application Management |
| `/citizen/applications` | Application tracking | Citizen Application Management |
| `/citizen/applications/nic/new` | New NIC form | Citizen Application Management |
| `/citizen/applications/nic/renewal` | NIC renewal form | Citizen Application Management |
| `/citizen/applications/nic/replacement` | Lost NIC form | Citizen Application Management |
| `/citizen/applications/passport/new` | New Passport form | Citizen Application Management |
| `/citizen/applications/passport/renewal` | Passport renewal form | Citizen Application Management |
| `/citizen/applications/passport/replacement` | Lost Passport form | Citizen Application Management |
| `/citizen/applications/nic/documents?applicationId={id}` | NIC document upload | Citizen Application Management |
| `/citizen/applications/passport/documents?applicationId={id}` | Passport document upload | Citizen Application Management |
| `/citizen/applications/nic/review?applicationId={id}` | Citizen NIC review | Citizen Application Management |
| `/citizen/applications/passport/review?applicationId={id}` | Citizen Passport review | Citizen Application Management |
| `/citizen/payment` | Demonstration payment | Payment Management |
| `/citizen/notifications` | Citizen notifications | Notification Management |
| `/citizen/appointments/biometric` | Biometric appointment booking | Biometric and Appointment Management |
| `/citizen/appointments/walk-in` | Physical service visit booking | Biometric and Appointment Management |
| `/citizen/support` | Complaint/inquiry history | Citizen Support Management |
| `/citizen/support/new` | New complaint/inquiry | Citizen Support Management |

The dashboard has no demo-state parameter. It matches the logged-in citizen's email and phone against `GovernmentIdentityRecord`. No existing document shows the new application action. An existing NIC or passport shows only renewal and lost replacement actions. The service repeats this check when a form URL is opened or submitted.

## NIC and Passport officer routes

| Route pattern | Purpose |
|---|---|
| `/officer/nic/applications` | NIC pending queue |
| `/officer/passport/applications` | Passport pending queue |
| `/officer/nic/applications/{reference}` | Review one NIC application |
| `/officer/passport/applications/{reference}` | Review one Passport application |
| `/officer/nic/applications/processed` | NIC review history |
| `/officer/passport/applications/processed` | Passport review history |
| `/officer/nic/appointments` | NIC biometric schedule |
| `/officer/passport/appointments` | Passport biometric schedule |
| `/officer/nic/biometrics` | NIC QR/manual check-in and biometric collection |
| `/officer/passport/biometrics` | Passport QR/manual check-in and biometric collection |

`{reference}` is a path variable. For example, `/officer/nic/applications/LK-NIC-2026-00183` passes `LK-NIC-2026-00183` to the review page.

## CSR, Director, and Admin routes

| Route | Purpose |
|---|---|
| `/officer/csr` | CSR dashboard and complaint queue |
| `/officer/csr/complaints/{reference}` | Complaint conversation and reply |
| `/officer/csr/walk-ins` | Accept, reschedule and check in physical visits |
| `/officer/csr/walk-ins/register` | Register an unscheduled visitor |
| `/officer/director` | Statistics dashboard |
| `/officer/director/printing` | Select records and prepare a printing export |
| `/officer/admin` | Officer credential management |
| `/officer/admin?section=citizens` | Citizen accounts and application counts inside the admin dashboard |
| `/officer/admin?section=notifications` | Universal or selected notification composer inside the admin dashboard |

## Citizen application save flow

1. `CitizenApplicationController` checks the citizen's `HttpSession`.
2. `CitizenApplicationService` checks email and phone eligibility.
3. The common citizen details are saved in `CitizenAccount`.
4. A base row is saved in `Application`, followed by its `NICApplication` or `PassportApplication` row.
5. Uploaded files are stored in the local `uploads` folder, while `ApplicationDocument` stores only the relative file path.
6. The application stays `DRAFT` until all required document types exist, then submission changes it to `SUBMITTED`.

The QR, biometric, payment and some officer review screens remain separate later workflow stages.

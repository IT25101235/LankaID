# LankaID UI Route Guide

This project currently uses simple Spring MVC `GET` routes. The pages contain demonstration data because the database and authentication are not connected yet.

## How a route works

```java
@GetMapping("/citizen/applications/nic/new")
public String newNicApplication(Model model) {
    model.addAttribute("applicationType", "new");
    return "citizen_frontends/nic_application_form";
}
```

- `@GetMapping` is the URL opened by the browser.
- `Model` passes small display values to Thymeleaf.
- The returned string is the HTML file inside `src/main/resources/templates` without `.html`.
- No route saves data yet. Form buttons only demonstrate the intended next screen.

## Public and citizen routes

| Route | Screen | Controller area |
|---|---|---|
| `/` | Landing page | User Management |
| `/login` | Citizen/officer login | User Management |
| `/register` | Citizen registration | User Management |
| `/citizen/dashboard` | Conditional citizen dashboard | User Management |
| `/citizen/applications` | Application tracking | Citizen Application Management |
| `/citizen/applications/nic/new` | New NIC form | Citizen Application Management |
| `/citizen/applications/nic/renewal` | NIC renewal form | Renewal and Replacement Management |
| `/citizen/applications/nic/replacement` | Lost/damaged NIC form | Renewal and Replacement Management |
| `/citizen/applications/passport/new` | New Passport form | Citizen Application Management |
| `/citizen/applications/passport/renewal` | Passport renewal form | Renewal and Replacement Management |
| `/citizen/applications/passport/replacement` | Lost/damaged Passport form | Renewal and Replacement Management |
| `/citizen/applications/nic/documents` | NIC upload | Citizen Application Management |
| `/citizen/applications/passport/documents` | Passport upload | Citizen Application Management |
| `/citizen/applications/nic/review` | Citizen NIC review | Citizen Application Management |
| `/citizen/applications/passport/review` | Citizen Passport review | Citizen Application Management |
| `/citizen/applications/nic/{reference}/correct` | Correct returned NIC fields | Citizen Application Management |
| `/citizen/applications/passport/{reference}/correct` | Correct returned Passport fields | Citizen Application Management |
| `/citizen/payment` | Demonstration payment | Payment Management |
| `/citizen/notifications` | Citizen notifications | Notification Management |
| `/citizen/appointments/biometric` | Biometric appointment booking | Biometric and Appointment Management |
| `/citizen/appointments/walk-in` | Physical service visit booking | Biometric and Appointment Management |
| `/citizen/support` | Complaint/inquiry history | Citizen Support Management |
| `/citizen/support/new` | New complaint/inquiry | Citizen Support Management |

The dashboard supports temporary demo states:

- `/citizen/dashboard?profile=new`
- `/citizen/dashboard?profile=nic-only`
- `/citizen/dashboard?profile=existing`

Later, the database will replace this query parameter with the citizen's real NIC and Passport ownership.

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
| `/officer/admin/citizens` | Citizen accounts and application counts |
| `/officer/admin/notifications` | Universal or selected notification composer |

## Before database integration

- Keep controllers as page-routing controllers only.
- Do not treat JavaScript success messages as saved records.
- QR scan buttons are simulations; later a QR library should decode only an appointment/reference value.
- Biometric files must eventually be stored securely by a backend service, not in browser JavaScript.
- Officer roles are selected on the temporary login screen only for demonstrating each dashboard.

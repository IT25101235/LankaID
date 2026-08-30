# Walk-in appointment EER update

Use this guide to update the existing Chen-style EER diagram. Keep the existing `Appointment`, `Centre`, and `Citizen` entities; add only one new entity named `WalkInSlot`.

## 1. Update the existing Appointment entity

Attach the following optional attribute ovals to the existing `Appointment` rectangle:

- `visitor_name` — visitor's name
- `visitor_phone` — OTP-verified phone number (already present in the database)
- `visitor_service` — either `NIC_APPLICATION_HELP` or `PASSPORT_APPLICATION_HELP`

These attributes are nullable because normal biometric appointments belong to a registered citizen and do not use the anonymous visitor fields.

Do not create separate `NIC Walk-in` and `Passport Walk-in` entities. They are two allowed values of `visitor_service`, so one simple `Appointment` structure is enough.

## 2. Show optional citizen participation

Keep the existing `BOOKS` relationship diamond between `Citizen` and `Appointment`, but show it as:

`Citizen (0,N) — BOOKS — Appointment (0,1)`

- A citizen may book zero or many appointments.
- A registered biometric appointment belongs to one citizen.
- An anonymous walk-in appointment belongs to no citizen, so the Appointment side is optional.
- Use a single line from `Appointment` to `BOOKS` because participation is partial for walk-ins.

## 3. Add the WalkInSlot entity

Draw a new rectangle named `WalkInSlot`. Connect these attribute ovals to it:

- `slot_id` — primary key; underline this attribute
- `centre_id` — foreign key to `Centre`
- `slot_date`
- `slot_time`
- `slot_capacity`
- `is_available`

The database rule `(centre_id, slot_date, slot_time)` is unique. Write this as a note beside the entity so the same half-hour slot cannot be published twice for one centre.

## 4. Connect WalkInSlot to Centre

Draw a relationship diamond named `PUBLISHES` between the existing `Centre` rectangle and the new `WalkInSlot` rectangle:

`Centre (0,N) — PUBLISHES — WalkInSlot (1,1)`

Participation and lines:

- `Centre` has partial participation: use a single line. A centre can exist before any slots are published.
- `WalkInSlot` has total participation: use a double line. Every slot must belong to exactly one centre.
- Put `0..N` near the Centre side and `1..1` near the WalkInSlot side.

A compact layout is:

```text
[Centre] -- (0,N) -- <PUBLISHES> == (1,1) == [WalkInSlot]
                                                     |
                     slot_id, centre_id, slot_date, slot_time,
                              slot_capacity, is_available
```

In the actual draw.io file, rectangles represent entities, ovals represent attributes, and the `PUBLISHES` shape must be a diamond. The double line beside `WalkInSlot` represents total participation.

## 5. Do not add a direct Appointment–WalkInSlot relationship

The current database does not store `slot_id` inside `Appointment`. A booking is matched to a published slot by the appointment's `centre_id` and `scheduled_at` date/time. Therefore, adding a `RESERVES` diamond would incorrectly suggest a foreign key that does not exist.

The existing `Appointment`–`Centre` connection remains unchanged. `WalkInSlot` only defines which centre/date/time combinations the CSR has made available.

## 6. Final checklist

- One new rectangle: `WalkInSlot`
- Six attribute ovals connected without overlap
- `slot_id` underlined as the primary key
- One new diamond: `PUBLISHES`
- Single line on the Centre side and double line on the WalkInSlot side
- Cardinalities shown as Centre `0..N` and WalkInSlot `1..1`
- Appointment has optional `visitor_name`, `visitor_phone`, and `visitor_service`
- `visitor_service` is limited to NIC and Passport; it is not another entity
- No direct `Appointment`–`WalkInSlot` relationship in the current schema

Run `WALK_IN_SCHEMA_UPDATE.sql` once in SSMS for every team member's database. The script is safe to rerun because it checks whether the columns and table already exist.

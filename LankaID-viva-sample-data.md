# LankaID Viva Citizen Sample Data

The prepared demo-account passwords are stored in `LankaID-viva-passwords.csv`. For the Aakil account, use the password entered when that account was registered.

## Recommended three-account demonstration

| Account | Purpose | Recommended demonstration |
|---|---|---|
| `new.citizen.demo@example.com` | Start from the beginning | New NIC, then optionally New Passport |
| `existing.citizen.demo@example.com` | Existing government record | NIC Renewal and Passport Lost |
| `aakilahamed2005@gmail.com` | Missed biometric appointment | Show both the citizen and officer rescheduling options |

Do not start a second active NIC application or a second active passport application for the same citizen. The system intentionally blocks duplicate active applications for the same service.

## 1. New citizen account

Login: `new.citizen.demo@example.com`

Use these details for either the New NIC or New Passport form:

| Field | Sample value |
|---|---|
| Family name | Silva |
| Given name | Kavindi |
| Surname | Silva |
| Gender | Female |
| Civil status | Single |
| Occupation | Undergraduate Student |
| Date of birth | 2003-06-18 |
| Birth certificate number | BC-CMB-2003-0618 |
| Birth place | Colombo |
| Birth division | Thimbirigasyaya |
| Birth district | Colombo |
| Country of birth | Sri Lanka |
| Birth city | Colombo |
| Permanent house number | 42 |
| Permanent street | Temple Road |
| Permanent city | Colombo |
| Permanent postal code | 00500 |
| Postal address same as permanent | Yes |
| Dual citizen | No |
| Declaration | Accept |

### New NIC

Required uploads:

- Photograph
- Birth certificate

### New Passport

| Passport field | Sample value |
|---|---|
| Service level | Normal |
| Travel document type | All Countries |
| Citizen NIC | Leave blank |
| Parent NIC | 197512345678 |

Required uploads:

- Photograph
- Birth certificate
- NIC proof (use the parent's NIC proof for this demo)

## 2. Existing citizen account

Login: `existing.citizen.demo@example.com`

This citizen is already linked to the government identity record below:

| Record | Stored value |
|---|---|
| Name | Nimal Perera |
| Date of birth | 2000-05-15 |
| NIC number | 200012345678 |
| NIC issued date | 2020-01-15 |
| Passport number | N1234567 |

Profile values, if the form asks for them:

| Field | Sample value |
|---|---|
| Family name | Perera |
| Given name | Nimal |
| Surname | Perera |
| Gender | Male |
| Civil status | Single |
| Occupation | Software Engineering Student |
| Date of birth | 2000-05-15 |
| Birth certificate number | BC-DEMO-2000-001 |
| Birth place | Colombo |
| Birth division | Colombo |
| Birth district | Colombo |
| Country of birth | Sri Lanka |
| Birth city | Colombo |
| Permanent house number | 25 |
| Permanent street | Demo Avenue |
| Permanent city | Colombo |
| Permanent postal code | 00100 |
| Postal address same as permanent | Yes |
| Dual citizen | No |
| Declaration | Accept |

### NIC Renewal

- NIC number should be verified and filled as `200012345678`.
- Issued date should be filled from the record as `2020-01-15`.
- Upload Photograph and Birth certificate.

### NIC Lost replacement

Use this only if you did not already start the NIC Renewal application.

| Lost NIC field | Sample value |
|---|---|
| Police station | Kollupitiya Police Station |
| Police report date | 2026-10-08 |

The NIC number and issued date should come from the government record. Upload Photograph, Birth certificate, and Police report.

### Passport Renewal

Use the verified citizen NIC `200012345678`. Service level is Normal and travel document type is All Countries. Upload Photograph, Birth certificate, and NIC proof.

### Passport Lost replacement

Use this only if you did not already start the Passport Renewal application. Use the verified citizen NIC `200012345678`. Upload Photograph, Birth certificate, NIC proof, and Police report.

For the smoothest viva, demonstrate **NIC Renewal** and **Passport Lost** with this account. That shows both renewal and lost workflows without creating duplicate applications for either service.

## 3. Missed biometric appointment account

Login: `aakilahamed2005@gmail.com`

This account is already prepared. Do not create another application for it.

| Item | Value |
|---|---|
| Citizen | Aakil Ahamed |
| Application | LK-NIC-2026-AAKIL01 |
| Appointment reference | BIO-2026-A4C1A105 |
| Original appointment | 2026-10-08 at 09:00 |
| Appointment type | Biometric |
| Status | Confirmed but missed |
| Payment | Paid, LKR 2,000 |
| Viva date | 2026-10-15 |
| Available reschedule slots | 2026-10-15, every 30 minutes from 08:00 to 17:30 at Colombo Demo Service Centre |

Use `LankaID-aakil-missed-biometric-QR.png` when demonstrating the officer QR scan. To demonstrate both sides with one appointment, first show the citizen's reschedule option without submitting it. Then scan the QR as the NIC officer, choose an available date and time, and press **Confirm reschedule**. The booking becomes Confirmed and keeps the same QR reference.

### October 15 viva sequence

1. Sign in as Aakil and open the appointment pass. The original October 8 appointment date is still in the past, so the pass displays **EXPIRED** and a missed-booking warning beside the same QR.
2. Open **Cancel or reschedule booking** to show the citizen's available options. Leave the booking unchanged so the officer can demonstrate rescheduling the same missed appointment.
3. Sign in as the NIC officer in a separate browser profile or private window and scan the same QR. The expired/missed warning blocks check-in and offers date and time selectors. Scanning does not reschedule the booking.
4. During the October 15 demonstration, select October 15 and a time that has not yet passed, then press **Confirm reschedule**. The officer chooses the slot; it is no longer chosen automatically. The reference remains `BIO-2026-A4C1A105`.
5. Refresh Aakil's pass to show the new appointment date and time, then continue officer check-in and biometric collection. Check-in requires the booked date to be October 15; it does not require waiting until the exact booked time.

Do not submit a reschedule during rehearsal on October 14 if you want the booking to remain missed for the viva. Tomorrow's slots run only until 17:30, so demonstrate rescheduling before that time. Slots whose start time has already passed cannot be selected. Completing either citizen or officer rescheduling updates the same booking, so show both options before submitting one.

Preparation on October 14: added 39 missing biometric slot records (19 NIC and 20 passport), resulting in 20 slots per service, each with capacity 10. The original October 8 appointment, payment, application, and QR reference were preserved. The database keeps the workflow status `CONFIRMED`; the past date identifies the booking as missed and blocks check-in until rescheduled.

The saved QR PNG contained the obsolete `BIO-2026-AAKIL01` value. It was replaced with a verified image for the existing `BIO-2026-A4C1A105` database reference, matching the citizen pass. Use this updated file rather than an older downloaded copy.

After the rescheduling rehearsal, Aakil's booking was restored from October 15 at 08:00 to its original October 8 at 09:00 slot. Payment, application status, and QR were retained. Restart the application to load the new expired-pass display and officer-selected rescheduling form.

## Demo upload checklist

Prepare small PDF/JPG/PNG files under 5 MB with obvious demo names:

- `demo-photo.jpg`
- `demo-birth-certificate.pdf`
- `demo-nic-proof.pdf`
- `demo-police-report.pdf`

These can be clearly marked **DEMO ONLY**; they do not need to look like genuine government documents.

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
| Appointment reference | BIO-2026-AAKIL01 |
| Original appointment | 2026-10-08 at 09:00 |
| Appointment type | Biometric |
| Status | Confirmed but missed |
| Payment | Paid, LKR 2,000 |
| Available reschedule slot | First available future NIC slot at Colombo Demo Service Centre |

Use `LankaID-aakil-missed-biometric-QR.png` when demonstrating the officer QR scan. To demonstrate both sides with one appointment, first show the citizen's reschedule option without submitting it. Then scan the QR as the NIC officer and select **Move to next available slot**. The booking becomes Confirmed and keeps the same QR reference.

## Demo upload checklist

Prepare small PDF/JPG/PNG files under 5 MB with obvious demo names:

- `demo-photo.jpg`
- `demo-birth-certificate.pdf`
- `demo-nic-proof.pdf`
- `demo-police-report.pdf`

These can be clearly marked **DEMO ONLY**; they do not need to look like genuine government documents.

# LankaID EER Diagram and SQL Database Design Guide

This guide turns the current LankaID Identity Issuance Service into a formal Enhanced Entity Relationship (EER) design and a practical SQL database plan. It follows the notation and mapping methods taught in IT2140 Lectures 04-06 and targets every Part B category in the assignment marking scheme.

> **Runnable application database:** the implemented Spring Boot application uses Microsoft SQL Server and validates the 19-table schema mapped by its JPA entities. Run [`DATABASE_SETUP.sql`](DATABASE_SETUP.sql) in SQL Server Management Studio for a clean or existing teammate database. The later MySQL-style relational blueprint describes the broader proposed EER and is not the executable application schema.

> **Implementation status:** see [`PROJECT_STATUS_AND_REMAINING_WORK.md`](PROJECT_STATUS_AND_REMAINING_WORK.md) for the evidence-based completion estimate and remaining delivery workflow.

## Registration email verification setup

For walk-in mobile verification, see [SMS OTP setup and lecturer guide](SMS_OTP_GUIDE.md).

Citizen registration sends a six-digit code through Gmail SMTP and creates the account only after the code is verified. Do not place the Gmail password or App Password in this repository.

1. Enable 2-Step Verification on the Google account used to send LankaID mail.
2. Open <https://myaccount.google.com/apppasswords>, create an App Password named `LankaID`, and copy the generated 16-character value. A normal Gmail password will not work here.
3. Before starting the application in PowerShell, set the credentials for that terminal session:

```powershell
$env:LANKAID_MAIL_USERNAME="your-sender@gmail.com"
$env:LANKAID_MAIL_APP_PASSWORD="the-16-character-app-password"
```

Optionally set `LANKAID_MAIL_FROM` to the sender address when it differs from the login username. Configure the same environment variables in the IDE run configuration when launching from IntelliJ IDEA or Eclipse. The code expires after 10 minutes, allows five attempts, and has a 60-second resend cooldown.

> Important: the assignment still requires two separate PDF submissions: a 2-3 page requirement specification and the EER diagram. This README is a design guide, not either final PDF.

## 1. What the database must support

LankaID supports these users:

- Citizen
- NIC Application Officer
- Passport Application Officer
- Citizen Service Representative (CSR)
- Identity Service Director
- System Administrator

The main workflow is:

1. A citizen registers and submits a new, renewal, or replacement NIC/Passport application.
2. The citizen uploads supporting documents and makes a payment.
3. The matching application officer reviews the application and may approve it, reject it, or request a correction.
4. An approved citizen books an appointment at a service centre.
5. An officer checks in the citizen and records the biometric collection.
6. A director prepares a printing batch for applications that are ready for printing.
7. The issued identity document is recorded and its status can be tracked.
8. Citizens can open support requests, exchange messages with a CSR, and receive notifications.
9. Administrators manage officer accounts and notifications; directors monitor reviews and performance.

## 2. Lecture notation that must be used

Use Chen/EER notation, not Crow's Foot notation.

| Concept | Required notation |
|---|---|
| Strong entity | Single rectangle |
| Weak entity | Double rectangle |
| Relationship | Diamond with a verb name |
| Identifying relationship | Double diamond |
| Attribute | Oval |
| Primary/key attribute | Underlined attribute name |
| Composite attribute | Main oval connected to component ovals |
| Multivalued attribute | Double oval |
| Derived attribute | Dashed oval |
| ISA specialization/generalization | Triangle labelled `ISA` |
| Total participation | Double line |
| Partial participation | Single line |
| Cardinality | Place `1`, `N`, or `M` at each relationship end; also show `(min,max)` where possible |
| Recursive relationship | One entity connected to the same diamond twice, with role names |
| Aggregation | Box around the participating entities and their relationship; relate the aggregate to another entity |

Entity names should be nouns in singular PascalCase. Relationship names should be verbs or verb phrases.

## 3. Recommended conceptual EER design

### 3.1 ISA hierarchies

Use these hierarchies because the roles and application types share common data but also have different relationships and attributes.

```text
User
  ISA - total and disjoint
  |- Citizen
  `- Officer
       ISA - total and disjoint
       |- ApplicationOfficer
       |    ISA - total and disjoint
       |    |- NICApplicationOfficer
       |    `- PassportApplicationOfficer
       |- CitizenServiceRepresentative
       |- IdentityServiceDirector
       `- SystemAdministrator

Application
  ISA - total and disjoint
  |- NICApplication
  `- PassportApplication
```

Why these constraints are correct:

- Every system user is either a citizen or an officer, so `Citizen` and `Officer` cover `User` (total specialization).
- One account cannot be both a citizen and an officer in this project, so the subclasses are disjoint.
- Every officer has exactly one operational role, so the officer specialization is total and disjoint.
- Every application is exactly one of NIC or Passport, so the application specialization is total and disjoint.

### 3.2 Entity and attribute catalogue

Underline each listed primary key in the diagram. Do not draw SQL foreign keys as conceptual attributes when the relationship already represents them.

#### Users and organization

| Entity | Attributes to draw |
|---|---|
| `User` | **userId** (key), `Name` (composite: firstName, middleName, lastName), `email` (candidate key/unique), `contactNumber` (multivalued), `passwordHash`, `accountStatus`, `createdAt` |
| `Citizen` | `dateOfBirth`, `age` (derived from dateOfBirth), `gender`, `Address` (composite: line1, line2, city, district, province, postalCode) |
| `Officer` | `employeeNo` (candidate key/unique), `hiredDate`, `officerStatus` |
| `ApplicationOfficer` | No additional attribute is required; it owns the common review relationship |
| `NICApplicationOfficer` | No additional attribute is required; its subtype restricts NIC reviews |
| `PassportApplicationOfficer` | No additional attribute is required; its subtype restricts Passport reviews |
| `CitizenServiceRepresentative` | No additional attribute is required; it handles support requests |
| `IdentityServiceDirector` | No additional attribute is required; it audits reviews and creates printing batches |
| `SystemAdministrator` | No additional attribute is required; it manages officer access and composes notifications |
| `ServiceCenter` | **centerId** (key), `centerName`, `Address` (composite: line1, city, district, postalCode), `contactNumber`, `centerStatus` |

#### Applications and issuance

| Entity | Attributes to draw |
|---|---|
| `Application` | **applicationId** (key), `referenceNo` (candidate key/unique), `applicationPurpose` (NEW/RENEWAL/REPLACEMENT), `currentStatus`, `submittedAt`, `lastUpdatedAt` |
| `NICApplication` | `fullNameLocal`, `fullNameEnglish`, `placeOfBirth`, `civilStatus`, `occupation`, `religion`, `fatherName`, `fatherNIC`, `motherName`, `motherNIC` |
| `PassportApplication` | `passportType`, `profession`, `previousPassportNo`, `previousIssueDate`, `previousExpiryDate`, `previousIssueAuthority` |
| `SupportingDocument` | **documentId** (key), `documentType`, `fileUri`, `fileHash`, `uploadedAt`, `verificationStatus` |
| `ApplicationStatusHistory` | `sequenceNo` (partial key, dashed underline), `status`, `changedAt`, `remarks` - draw this as a weak entity |
| `Payment` | **paymentId** (key), `transactionReference` (candidate key/unique), `amount`, `paymentMethod`, `paymentStatus`, `paidAt` |
| `Appointment` | **appointmentId** (key), `appointmentReference` (candidate key/unique), `appointmentType` (BIOMETRIC/WALK_IN), `scheduledAt`, `appointmentStatus`, `checkedInAt`, `tokenNo` |
| `BiometricRecord` | **biometricId** (key), `captureReference` (unique), `encryptedFileUri`, `capturedAt`, `verificationStatus`, `officerNotes` |
| `PrintingBatch` | **batchId** (key), `batchReference` (unique), `exportFormat`, `createdAt`, `batchStatus` |
| `IdentityDocument` | **identityDocumentId** (key), `documentNumber` (candidate key/unique), `documentType` (NIC/PASSPORT), `issueDate`, `expiryDate`, `documentStatus` |

#### Support and communication

| Entity | Attributes to draw |
|---|---|
| `SupportRequest` | **supportRequestId** (key), `caseReference` (unique), `category`, `subject`, `caseStatus`, `openedAt`, `closedAt` |
| `SupportMessage` | **messageId** (key), `messageText`, `attachmentUri`, `sentAt` |
| `Notification` | **notificationId** (key), `title`, `message`, `notificationType`, `createdAt`, `expiresAt` |

### 3.3 Relationships, cardinalities, and participation

The `(min,max)` values below are from the viewpoint of the entity named in that row. Use a double participation line wherever the minimum is `1`.

| Relationship | Entity A participation | Entity B participation | Meaning and relationship attributes |
|---|---:|---:|---|
| `AssignedTo` | `Officer (1,1)` | `ServiceCenter (0,N)` | Every officer is assigned to one centre; a centre may have many officers |
| `Submits` | `Citizen (0,N)` | `Application (1,1)` | An application cannot exist without its citizen; total participation on `Application` |
| `SupportedBy` | `Application (0,N)` | `SupportingDocument (1,1)` | Every supporting document belongs to one application; total on `SupportingDocument` |
| `Records` (identifying) | `Application (1,N)` | `ApplicationStatusHistory (1,1)` | Double diamond; the weak entity key is `(applicationId, sequenceNo)` |
| `ChangesStatus` | `User (0,N)` | `ApplicationStatusHistory (0,1)` | A user may create a history entry; participation is partial because the system may change status automatically |
| `Reviews` | `ApplicationOfficer (0,N)` | `Application (0,N)` | M:N over time; descriptive attributes: `decision`, `remarks`, `reviewedAt` |
| `HasPaymentAttempt` | `Application (0,N)` | `Payment (1,1)` | Supports failed/retried payment attempts; total on `Payment` |
| `Books` | `Citizen (0,N)` | `Appointment (1,1)` | Every appointment belongs to one citizen; total on `Appointment` |
| `ForApplication` | `Application (0,N)` | `Appointment (0,1)` | A walk-in may have no application; every biometric appointment must have one application |
| `Hosts` | `ServiceCenter (0,N)` | `Appointment (1,1)` | Every appointment is at one centre; total on `Appointment` |
| `HandlesAppointment` | `Officer (0,N)` | `Appointment (0,1)` | An appointment can be unassigned before check-in; one officer handles it when served |
| `ProducesBiometric` | `Appointment (0,1)` | `BiometricRecord (1,1)` | Only a completed biometric appointment produces a record; total on `BiometricRecord` |
| `Collects` | `ApplicationOfficer (0,N)` | `BiometricRecord (1,1)` | Every biometric record is collected by one authorized application officer |
| `HasBiometric` | `Application (0,1)` | `BiometricRecord (1,1)` | At most one active biometric record per application in this simplified design |
| `ProducesDocument` | `Application (0,1)` | `IdentityDocument (0,1)` | Only an approved/printed application produces an identity document |
| `Owns` | `Citizen (0,N)` | `IdentityDocument (1,1)` | Every issued document has exactly one owner; total on `IdentityDocument` |
| `Opens` | `Citizen (0,N)` | `SupportRequest (1,1)` | Every case is opened by one citizen; total on `SupportRequest` |
| `Handles` | `CitizenServiceRepresentative (0,N)` | `SupportRequest (0,1)` | A new case can be unassigned; an assigned case has one CSR |
| `Contains` | `SupportRequest (1,N)` | `SupportMessage (1,1)` | A case begins with at least one message; total on `SupportMessage` |
| `Authors` | `User (0,N)` | `SupportMessage (1,1)` | A citizen or CSR authors each message; total on `SupportMessage` |
| `Creates` | `SystemAdministrator (0,N)` | `Notification (0,1)` | Notification participation is partial because some notices are system-generated |
| `Receives` | `User (0,N)` | `Notification (0,N)` | M:N; descriptive attributes: `deliveryStatus`, `deliveredAt`, `readAt` |
| `CreatesBatch` | `IdentityServiceDirector (0,N)` | `PrintingBatch (1,1)` | Every batch is created by one director; total on `PrintingBatch` |
| `Includes` | `PrintingBatch (1,N)` | `Application (0,N)` | M:N to retain re-export/reprint history; descriptive attributes: `itemStatus`, `includedAt` |

Additional advanced relationships:

- Recursive: `Officer SUPERVISES Officer`. Use role names `Supervisor` and `Subordinate`. A supervisor has `(0,N)` subordinates; a subordinate has `(0,1)` supervisor.
- Aggregation: place an aggregation box around `ApplicationOfficer - Reviews - Application`. Connect `IdentityServiceDirector` to the boxed review occurrence through an `Audits` diamond. One director audits `(0,N)` review occurrences; one review occurrence is audited by `(0,1)` director. Put `auditOutcome`, `auditNote`, and `auditedAt` on `Audits`.

Business rules to show in a note beside the diagram:

- A `NICApplicationOfficer` may review only a `NICApplication`.
- A `PassportApplicationOfficer` may review only a `PassportApplication`.
- A biometric appointment must reference an approved application.
- Only a payment-confirmed, approved application with completed biometrics may enter a printing batch.
- An application may produce at most one active identity document.
- `age` is derived and must not be stored.

## 4. Claude-ready prompt for generating the EER diagram

Copy the complete prompt below into Claude. If Claude returns only a textual explanation, reply: **"Generate the actual SVG artifact now; do not explain it again."**

```text
You are a senior database designer and technical diagram artist. Create one polished, formal Enhanced Entity Relationship (EER) diagram for a system called "LankaID Identity Issuance Service".

OUTPUT REQUIREMENT
- Generate the actual diagram as a single self-contained SVG artifact, suitable for export to PDF at A2 landscape size. A3 may be used only if every label remains readable.
- Do not return Mermaid, Crow's Foot notation, UML class boxes, SQL-table boxes, or a prose-only answer.
- Keep all labels readable at 100% zoom. Use orthogonal or gently curved connectors, prevent crossings where possible, and do not allow shapes or text to overlap.
- Use a white background, black connectors, and very light section colours. The formal shapes, not colour, must carry the meaning.

MANDATORY CHEN/EER NOTATION
- Strong entity = single rectangle.
- Weak entity = double rectangle.
- Relationship = diamond containing a verb or verb phrase.
- Identifying relationship = double diamond.
- Attribute = oval connected to its entity or relationship.
- Primary/key attribute = underlined attribute name.
- Candidate key = underlined and labelled AK/unique in a small note.
- Composite attribute = main oval connected to smaller component ovals.
- Multivalued attribute = double oval.
- Derived attribute = dashed oval.
- ISA = triangle labelled ISA.
- Total participation = double line; partial participation = single line.
- Place 1, N, or M beside relationship ends and also show (min,max) values.
- Recursive relationship must show both role names.
- Aggregation must be a box surrounding the participating entities and their relationship.

SPECIALIZATION HIERARCHIES
1. User ISA Citizen and Officer. Mark it TOTAL and DISJOINT.
2. Officer ISA ApplicationOfficer, CitizenServiceRepresentative, IdentityServiceDirector, and SystemAdministrator. Mark it TOTAL and DISJOINT.
3. ApplicationOfficer ISA NICApplicationOfficer and PassportApplicationOfficer. Mark it TOTAL and DISJOINT.
4. Application ISA NICApplication and PassportApplication. Mark it TOTAL and DISJOINT.

ENTITIES AND ATTRIBUTES
Underline every primary key.

User: userId (PK); Name composite {firstName, middleName, lastName}; email (candidate key/unique); contactNumber (multivalued/double oval); passwordHash; accountStatus; createdAt.
Citizen: dateOfBirth; age (derived/dashed oval); gender; Address composite {line1, line2, city, district, province, postalCode}.
Officer: employeeNo (candidate key/unique); hiredDate; officerStatus.
ApplicationOfficer: no extra attributes.
NICApplicationOfficer: no extra attributes.
PassportApplicationOfficer: no extra attributes.
CitizenServiceRepresentative: no extra attributes.
IdentityServiceDirector: no extra attributes.
SystemAdministrator: no extra attributes.
ServiceCenter: centerId (PK); centerName; Address composite {line1, city, district, postalCode}; contactNumber; centerStatus.

Application: applicationId (PK); referenceNo (candidate key/unique); applicationPurpose {NEW, RENEWAL, REPLACEMENT}; currentStatus; submittedAt; lastUpdatedAt.
NICApplication: fullNameLocal; fullNameEnglish; placeOfBirth; civilStatus; occupation; religion; fatherName; fatherNIC; motherName; motherNIC.
PassportApplication: passportType; profession; previousPassportNo; previousIssueDate; previousExpiryDate; previousIssueAuthority.
SupportingDocument: documentId (PK); documentType; fileUri; fileHash; uploadedAt; verificationStatus.
ApplicationStatusHistory: draw as WEAK ENTITY; sequenceNo is a PARTIAL KEY with dashed underline; status; changedAt; remarks.
Payment: paymentId (PK); transactionReference (candidate key/unique); amount; paymentMethod; paymentStatus; paidAt.
Appointment: appointmentId (PK); appointmentReference (candidate key/unique); appointmentType {BIOMETRIC, WALK_IN}; scheduledAt; appointmentStatus; checkedInAt; tokenNo.
BiometricRecord: biometricId (PK); captureReference (unique); encryptedFileUri; capturedAt; verificationStatus; officerNotes.
PrintingBatch: batchId (PK); batchReference (unique); exportFormat; createdAt; batchStatus.
IdentityDocument: identityDocumentId (PK); documentNumber (candidate key/unique); documentType {NIC, PASSPORT}; issueDate; expiryDate; documentStatus.

SupportRequest: supportRequestId (PK); caseReference (unique); category; subject; caseStatus; openedAt; closedAt.
SupportMessage: messageId (PK); messageText; attachmentUri; sentAt.
Notification: notificationId (PK); title; message; notificationType; createdAt; expiresAt.

RELATIONSHIPS AND EXACT CONSTRAINTS
Write every relationship name inside a diamond. Show the stated min-max values and participation lines.

Officer (1,1) ASSIGNED_TO ServiceCenter (0,N). Officer participation is total.
Citizen (0,N) SUBMITS Application (1,1). Application participation is total.
Application (0,N) SUPPORTED_BY SupportingDocument (1,1). SupportingDocument participation is total.
Application (1,N) RECORDS ApplicationStatusHistory (1,1). RECORDS is a double-diamond identifying relationship and ApplicationStatusHistory has total participation. Its complete key is {applicationId, sequenceNo}.
User (0,N) CHANGES_STATUS ApplicationStatusHistory (0,1). Keep history participation partial because a status may be changed automatically by the system.
ApplicationOfficer (0,N) REVIEWS Application (0,N). This is M:N. Attach decision, remarks, and reviewedAt as ovals to the REVIEWS diamond.
Application (0,N) HAS_PAYMENT_ATTEMPT Payment (1,1). Payment participation is total.
Citizen (0,N) BOOKS Appointment (1,1). Appointment participation is total.
Application (0,N) FOR_APPLICATION Appointment (0,1). Add a note that BIOMETRIC appointments must participate, while WALK_IN appointments may not.
ServiceCenter (0,N) HOSTS Appointment (1,1). Appointment participation is total.
Officer (0,N) HANDLES_APPOINTMENT Appointment (0,1). An appointment may be unassigned before check-in.
Appointment (0,1) PRODUCES_BIOMETRIC BiometricRecord (1,1). BiometricRecord participation is total.
ApplicationOfficer (0,N) COLLECTS BiometricRecord (1,1). BiometricRecord participation is total.
Application (0,1) HAS_BIOMETRIC BiometricRecord (1,1). BiometricRecord participation is total.
Application (0,1) PRODUCES_DOCUMENT IdentityDocument (0,1).
Citizen (0,N) OWNS IdentityDocument (1,1). IdentityDocument participation is total.
Citizen (0,N) OPENS SupportRequest (1,1). SupportRequest participation is total.
CitizenServiceRepresentative (0,N) HANDLES SupportRequest (0,1).
SupportRequest (1,N) CONTAINS SupportMessage (1,1). SupportMessage participation is total.
User (0,N) AUTHORS SupportMessage (1,1). SupportMessage participation is total.
SystemAdministrator (0,N) CREATES Notification (0,1). Keep Notification participation partial because notices may be generated automatically.
User (0,N) RECEIVES Notification (0,N). This is M:N. Attach deliveryStatus, deliveredAt, and readAt to the RECEIVES diamond.
IdentityServiceDirector (0,N) CREATES_BATCH PrintingBatch (1,1). PrintingBatch participation is total.
PrintingBatch (1,N) INCLUDES Application (0,N). This is M:N. Attach itemStatus and includedAt to the INCLUDES diamond.

ADVANCED EER FEATURES
1. Recursive relationship: Officer SUPERVISES Officer. Label the two roles Supervisor and Subordinate. Supervisor participation is (0,N); Subordinate participation is (0,1).
2. Aggregation: draw an aggregation box around ApplicationOfficer - REVIEWS - Application. Connect IdentityServiceDirector to this aggregate using an AUDITS diamond. One director audits (0,N) review occurrences; a review occurrence is audited by (0,1) director. Attach auditOutcome, auditNote, and auditedAt to AUDITS.

BUSINESS-RULE NOTE BOX
- NICApplicationOfficer reviews only NICApplication.
- PassportApplicationOfficer reviews only PassportApplication.
- A biometric appointment references an approved application.
- Only a payment-confirmed, approved application with completed biometrics can be included in a printing batch.
- One application produces at most one active identity document.
- age is derived from dateOfBirth and is not stored.

LAYOUT
- Use A2 landscape (or A3 only when still readable) and divide the page into three visually labelled zones.
- Left/top: User and Officer ISA hierarchies, ServiceCenter, and recursive supervision.
- Centre: Citizen, Application ISA hierarchy, documents, history, review, payment, appointment, biometrics, printing, and issued document.
- Right/bottom: support requests/messages and notifications.
- Put the title and a compact notation legend at the top.
- Place relationship attributes close to their diamonds.
- Do not duplicate inherited attributes on subclasses.
- Do not show SQL foreign-key attributes in this conceptual diagram.

Before producing the SVG, internally validate that every entity is connected, every relationship has both cardinalities, all mandatory participations use double lines, primary keys are underlined, the weak entity and identifying relationship are double-lined, the ISA constraints are labelled total/disjoint, and no label overlaps another shape. Then output the completed SVG artifact.
```

## 5. Recommended physical SQL design

Use MySQL 8 for the first implementation unless the team has already selected another relational DBMS. PostgreSQL or SQL Server can use the same logical tables with small datatype and identity-syntax changes.

### 5.1 EER-to-relational mapping choices

These choices follow Lecture 05:

- Map `User -> Citizen/Officer` using ISA Option 1: a `user_account` supertype table plus `citizen` and `officer` subtype tables whose primary keys are also foreign keys.
- Map the lower officer role hierarchy using ISA Option 3: keep one `officer` table with a checked `role_code`, because these disjoint role subtypes currently have few role-specific attributes.
- Map `Application -> NICApplication/PassportApplication` using ISA Option 1 because each subtype has several different attributes.
- Map M:N relationships into bridge tables: `application_review`, `notification_recipient`, and `printing_batch_item`.
- Flatten composite attributes into atomic columns for 1NF.
- Map the multivalued `contactNumber` attribute into `user_phone`; never store comma-separated numbers.
- Do not store derived `age`; calculate it from `date_of_birth` when needed.
- Map the weak status entity with the composite primary key `(application_id, sequence_no)`.
- Map director auditing of the review aggregation into nullable audit columns on `application_review`, because one review has at most one director audit in this simplified design.

### 5.2 Relational schema blueprint

`PK` means primary key, `FK` means foreign key, and `UQ` means unique.

| Table | Important columns |
|---|---|
| `user_account` | `user_id PK`, `user_type`, `first_name`, `middle_name`, `last_name`, `email UQ`, `password_hash`, `account_status`, `created_at`, `updated_at` |
| `user_phone` | `user_id PK/FK -> user_account`, `phone_number PK`, `phone_type`, `is_primary` |
| `citizen` | `user_id PK/FK -> user_account`, `date_of_birth`, `gender`, `address_line1`, `address_line2`, `city`, `district`, `province`, `postal_code` |
| `service_center` | `center_id PK`, `center_name UQ`, address columns, `contact_number`, `center_status` |
| `officer` | `user_id PK/FK -> user_account`, `employee_no UQ`, `role_code`, `center_id FK`, `supervisor_officer_id FK -> officer`, `hired_date`, `officer_status` |
| `application` | `application_id PK`, `reference_no UQ`, `citizen_id FK -> citizen`, `application_type`, `application_purpose`, `current_status`, `submitted_at`, `created_at`, `updated_at` |
| `nic_application` | `application_id PK/FK -> application`, local/English names, birth/civil/occupation/religion and parent fields |
| `passport_application` | `application_id PK/FK -> application`, passport type/profession and previous-passport fields |
| `supporting_document` | `document_id PK`, `application_id FK`, `document_type`, `file_uri`, `file_hash`, `verification_status`, `uploaded_at` |
| `application_status_history` | `application_id PK/FK`, `sequence_no PK`, `status`, `changed_by_user_id FK`, `changed_at`, `remarks` |
| `application_review` | `review_id PK`, `application_id FK`, `reviewer_officer_id FK`, `decision`, `remarks`, `reviewed_at`, `audited_by_director_id FK NULL`, `audit_outcome`, `audit_note`, `audited_at` |
| `payment` | `payment_id PK`, `application_id FK`, `transaction_reference UQ`, `amount DECIMAL(10,2)`, `payment_method`, `payment_status`, `paid_at`, `created_at` |
| `appointment` | `appointment_id PK`, `appointment_reference UQ`, `citizen_id FK`, `application_id FK NULL`, `center_id FK`, `handled_by_officer_id FK NULL`, `appointment_type`, `scheduled_at`, `appointment_status`, `checked_in_at`, `token_no` |
| `biometric_record` | `biometric_id PK`, `application_id FK UQ`, `appointment_id FK UQ`, `collected_by_officer_id FK`, `capture_reference UQ`, `encrypted_file_uri`, `verification_status`, `officer_notes`, `captured_at` |
| `printing_batch` | `batch_id PK`, `batch_reference UQ`, `created_by_director_id FK`, `export_format`, `batch_status`, `created_at` |
| `printing_batch_item` | `batch_id PK/FK`, `application_id PK/FK`, `item_status`, `included_at` |
| `identity_document` | `identity_document_id PK`, `application_id FK UQ NULL`, `owner_citizen_id FK`, `document_number UQ`, `document_type`, `issue_date`, `expiry_date NULL`, `document_status` |
| `support_request` | `support_request_id PK`, `case_reference UQ`, `citizen_id FK`, `assigned_csr_id FK NULL`, `related_application_id FK NULL`, `category`, `subject`, `case_status`, `opened_at`, `closed_at` |
| `support_message` | `message_id PK`, `support_request_id FK`, `author_user_id FK`, `message_text`, `attachment_uri`, `sent_at` |
| `notification` | `notification_id PK`, `created_by_admin_id FK NULL`, `title`, `message`, `notification_type`, `created_at`, `expires_at` |
| `notification_recipient` | `notification_id PK/FK`, `user_id PK/FK`, `delivery_status`, `delivered_at`, `read_at` |

### 5.3 Suggested SQL domains/checks

Use `BIGINT` auto-generated IDs, `VARCHAR` for references, `DATE` for civil dates, `DATETIME`/`TIMESTAMP` for events, and `DECIMAL(10,2)` for money. Use `CHECK` constraints or lookup tables for these stable code sets:

- `user_type`: `CITIZEN`, `OFFICER`
- `role_code`: `NIC_APPLICATION_OFFICER`, `PASSPORT_APPLICATION_OFFICER`, `CSR`, `DIRECTOR`, `SYSTEM_ADMIN`
- `application_type`: `NIC`, `PASSPORT`
- `application_purpose`: `NEW`, `RENEWAL`, `REPLACEMENT`
- `appointment_type`: `BIOMETRIC`, `WALK_IN`
- statuses such as `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `CORRECTION_REQUIRED`, `APPROVED`, `BIOMETRIC_BOOKED`, `READY_FOR_PRINTING`, `PRINTED`, `ISSUED`, `REJECTED`, `CANCELLED`

Do not use a database `ENUM` if status values are expected to change often. A small lookup table is easier to extend. For this student project, `VARCHAR` plus `CHECK` is an understandable starting point.

### 5.4 Small SQL mapping example

This example shows the most important mapping patterns; create the remaining tables from the blueprint above in the same style.

```sql
CREATE TABLE user_account (
    user_id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_type       VARCHAR(20) NOT NULL,
    first_name      VARCHAR(80) NOT NULL,
    middle_name     VARCHAR(80),
    last_name       VARCHAR(80) NOT NULL,
    email           VARCHAR(254) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    account_status  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_type
        CHECK (user_type IN ('CITIZEN', 'OFFICER'))
);

CREATE TABLE citizen (
    user_id         BIGINT PRIMARY KEY,
    date_of_birth   DATE NOT NULL,
    gender          VARCHAR(20) NOT NULL,
    address_line1   VARCHAR(150) NOT NULL,
    address_line2   VARCHAR(150),
    city            VARCHAR(80) NOT NULL,
    district        VARCHAR(80) NOT NULL,
    province        VARCHAR(80) NOT NULL,
    postal_code     VARCHAR(10),
    CONSTRAINT fk_citizen_user
        FOREIGN KEY (user_id) REFERENCES user_account(user_id)
);

CREATE TABLE application (
    application_id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    reference_no         VARCHAR(40) NOT NULL UNIQUE,
    citizen_id           BIGINT NOT NULL,
    application_type     VARCHAR(20) NOT NULL,
    application_purpose  VARCHAR(20) NOT NULL,
    current_status       VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    submitted_at         TIMESTAMP NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                         ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_application_citizen
        FOREIGN KEY (citizen_id) REFERENCES citizen(user_id),
    CONSTRAINT chk_application_type
        CHECK (application_type IN ('NIC', 'PASSPORT')),
    CONSTRAINT chk_application_purpose
        CHECK (application_purpose IN ('NEW', 'RENEWAL', 'REPLACEMENT'))
);

CREATE TABLE nic_application (
    application_id   BIGINT PRIMARY KEY,
    full_name_local  VARCHAR(200) NOT NULL,
    full_name_english VARCHAR(200) NOT NULL,
    place_of_birth   VARCHAR(120) NOT NULL,
    civil_status     VARCHAR(20),
    occupation       VARCHAR(120),
    religion         VARCHAR(40),
    father_name      VARCHAR(200),
    father_nic       VARCHAR(20),
    mother_name      VARCHAR(200),
    mother_nic       VARCHAR(20),
    CONSTRAINT fk_nic_application
        FOREIGN KEY (application_id) REFERENCES application(application_id)
);

CREATE TABLE application_status_history (
    application_id     BIGINT NOT NULL,
    sequence_no        INT NOT NULL,
    status             VARCHAR(40) NOT NULL,
    changed_by_user_id BIGINT,
    changed_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks            VARCHAR(500),
    PRIMARY KEY (application_id, sequence_no),
    CONSTRAINT fk_history_application
        FOREIGN KEY (application_id) REFERENCES application(application_id),
    CONSTRAINT fk_history_user
        FOREIGN KEY (changed_by_user_id) REFERENCES user_account(user_id)
);

CREATE INDEX idx_application_queue
    ON application (application_type, current_status, submitted_at);

CREATE INDEX idx_application_citizen
    ON application (citizen_id, created_at);
```

Add `ON DELETE` actions deliberately:

- Prefer `RESTRICT` for citizens, applications, payments, reviews, biometric records, and issued documents so legal/audit history is not accidentally removed.
- `CASCADE` is reasonable for pure dependent rows such as `user_phone`, `notification_recipient`, and draft-only child records if the business rules allow deletion.
- Prefer account/document status fields over hard deletion for production records.

### 5.5 Security and file-storage rules

- Store only salted password hashes created by the application (Argon2 or BCrypt), never plain-text passwords.
- Do not store uploaded PDFs, images, or biometric files as large SQL BLOBs in this student design. Store them in protected file/object storage and save only an opaque `file_uri`, hash, type, size, and verification status in SQL.
- Biometric file locations must be encrypted/protected and accessible only to authorized officers.
- Use parameterized queries/JPA rather than SQL string concatenation.
- Record every application status change instead of overwriting the only history.
- Validate that the reviewer role matches the application type in the service layer; optionally add a database trigger later.
- Execute application submission, payment confirmation, review decisions, and printing-batch creation inside transactions.

### 5.6 Normalization check

The proposed relational design is intended to reach at least 3NF:

- User names/contact details are stored once, not copied into every role table.
- Centre details are separated from officers and appointments, preventing update anomalies.
- NIC-only and Passport-only facts are placed in separate subtype tables.
- Phone numbers and M:N relationships are separated into their own relations for 1NF.
- Payment attempts, reviews, messages, and status history are separate repeating facts.
- Non-key attributes describe their table's key, with no avoidable dependency on another non-key attribute.

Before implementation, write down the functional dependencies for each table and verify that every determinant is a candidate key. If a table violates 3NF/BCNF, decompose it only when the decomposition is lossless and preferably dependency-preserving.

## 6. Part A requirement-document outline

The EER is worth 60 marks; the other 40 marks require a separate 2-3 page requirement specification.

Use these headings:

1. **System Purpose** - provide a secure central service for applying for, verifying, issuing, renewing, and replacing Sri Lankan NICs and passports.
2. **Users** - describe the six concrete user roles and each role's responsibilities.
3. **Functional Requirements** - registration/login, application submission, uploads, payments, officer review/corrections, appointment booking/check-in, biometrics, printing batches, tracking, support, notifications, dashboards, and account administration.
4. **Non-Functional Requirements** - authentication/authorization, encryption, privacy, availability, response time, scalability, auditability, backup/recovery, and usability/accessibility.
5. **Constraints and Assumptions** - SQL DBMS, one role per officer, one citizen per application, secure external file storage, simulated external biometric/payment/printing services for the current project, and valid NIC/Passport formats.
6. **Evidence of Requirement Gathering** - include clear, ordered screenshots of the questions and useful answers from the bot interaction. Captions should state what requirement each screenshot proves.

Do not paste this README unchanged as the 2-3 page submission. Convert it into concise, professional prose and include only confirmed group requirements.

## 7. Final marking checklist

### Entity and Attribute Design - 15 marks

- [ ] Every entity has an underlined primary key.
- [ ] `Name` and `Address` demonstrate composite attributes.
- [ ] `contactNumber` demonstrates a multivalued attribute.
- [ ] `age` demonstrates a derived attribute.
- [ ] `ApplicationStatusHistory` is a valid weak entity with a partial key.
- [ ] Attributes belong to the correct entity and are not unnecessarily duplicated.

### Relationships and Cardinalities - 15 marks

- [ ] Every diamond has cardinalities at both ends.
- [ ] Mandatory participation uses double lines.
- [ ] `Receives`, `Reviews`, and `Includes` correctly show M:N relationships and relationship attributes.
- [ ] Every relationship agrees with the requirement specification.

### Advanced EER Features - 15 marks

- [ ] All ISA triangles state total/partial and disjoint/overlapping constraints.
- [ ] The recursive `Supervises` relationship has role names.
- [ ] The director `Audits` relationship correctly uses aggregation.
- [ ] The weak entity uses a double rectangle and double-diamond identifying relationship.

### Diagram Clarity and Tool Usage - 15 marks

- [ ] A2/A3 landscape layout is readable after PDF export and at normal PDF zoom.
- [ ] There are no overlapping labels or ambiguous crossing lines.
- [ ] A compact legend explains every notation used.
- [ ] The diagram contains conceptual entities/attributes, not physical SQL table boxes.
- [ ] The final filename follows `GroupID_Assignment01_EER.pdf`.
- [ ] The requirement document follows `GroupID_Assignment01_Requirements.pdf` and is 2-3 pages excluding evidence screenshots.

## 8. Assumptions to confirm with the group

Confirm these before final submission and update both the requirement document and EER if an answer changes:

- Each officer has exactly one role and one primary service centre.
- Citizens and officers use separate accounts; one account cannot be both.
- An application can have multiple payment attempts and review rounds.
- One application has at most one current biometric record and one active issued document.
- Walk-in appointments may be created before an application exists.
- The director audits selected reviews rather than every review.
- A printing batch can include a previously exported application when a controlled reprint is required.

The final EER must agree with the final requirement specification. A simpler diagram with justified rules is stronger than a larger diagram containing unsupported entities or relationships.

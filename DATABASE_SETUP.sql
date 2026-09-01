/*
    LankaID Identity Issuance Service - SQL Server setup
    Target: Microsoft SQL Server 2019+ / SQL Server Management Studio

    This script is safe to run more than once. It creates the database and any
    missing application tables without deleting existing data.
*/

USE master;
GO

IF DB_ID(N'identity_issuance_service') IS NULL
BEGIN
    CREATE DATABASE identity_issuance_service;
END;
GO

USE identity_issuance_service;
GO

IF OBJECT_ID(N'dbo.GovernmentIdentityRecord', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.GovernmentIdentityRecord
    (
        government_record_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_GovernmentIdentityRecord PRIMARY KEY,
        nic_no               VARCHAR(12) NULL,
        passport_no          VARCHAR(20) NULL,
        registered_email     VARCHAR(100) NULL,
        registered_phone     VARCHAR(15) NULL,
        identity_status      VARCHAR(20) NULL
    );
    CREATE UNIQUE INDEX UQ_GovernmentIdentityRecord_Nic
        ON dbo.GovernmentIdentityRecord(nic_no) WHERE nic_no IS NOT NULL;
    CREATE UNIQUE INDEX UQ_GovernmentIdentityRecord_Passport
        ON dbo.GovernmentIdentityRecord(passport_no) WHERE passport_no IS NOT NULL;
END;
GO

IF OBJECT_ID(N'dbo.UserAccount', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.UserAccount
    (
        user_id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_UserAccount PRIMARY KEY,
        email           VARCHAR(100) NOT NULL,
        phone           VARCHAR(15) NOT NULL,
        password_hash   VARCHAR(255) NOT NULL,
        account_status  VARCHAR(20) NOT NULL CONSTRAINT DF_UserAccount_Status DEFAULT ('ACTIVE'),
        email_verified  CHAR(1) NOT NULL CONSTRAINT DF_UserAccount_EmailVerified DEFAULT ('N'),
        phone_verified  CHAR(1) NOT NULL CONSTRAINT DF_UserAccount_PhoneVerified DEFAULT ('N'),
        registered_at   DATETIME2(0) NOT NULL CONSTRAINT DF_UserAccount_RegisteredAt DEFAULT (SYSDATETIME()),
        CONSTRAINT UQ_UserAccount_Email UNIQUE (email),
        CONSTRAINT UQ_UserAccount_Phone UNIQUE (phone),
        CONSTRAINT CK_UserAccount_EmailVerified CHECK (email_verified IN ('Y', 'N')),
        CONSTRAINT CK_UserAccount_PhoneVerified CHECK (phone_verified IN ('Y', 'N'))
    );
END;
GO

IF OBJECT_ID(N'dbo.CitizenAccount', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CitizenAccount
    (
        user_id                       INT NOT NULL CONSTRAINT PK_CitizenAccount PRIMARY KEY,
        government_record_id          INT NULL,
        family_name                   VARCHAR(100) NULL,
        given_name                    VARCHAR(150) NULL,
        surname                       VARCHAR(100) NULL,
        gender                        VARCHAR(10) NULL,
        civil_status                  VARCHAR(15) NULL,
        occupation                    VARCHAR(150) NULL,
        date_of_birth                 DATETIME2(0) NULL,
        birth_certificate_no          VARCHAR(50) NULL,
        birth_place                   VARCHAR(100) NULL,
        birth_division                VARCHAR(100) NULL,
        birth_district                VARCHAR(100) NULL,
        country_of_birth              VARCHAR(100) NULL,
        birth_city                    VARCHAR(100) NULL,
        permanent_house_no            VARCHAR(50) NULL,
        permanent_street              VARCHAR(150) NULL,
        permanent_city                VARCHAR(100) NULL,
        permanent_postal_code         VARCHAR(10) NULL,
        postal_same_as_permanent      CHAR(1) NULL,
        postal_house_no               VARCHAR(50) NULL,
        postal_street                 VARCHAR(150) NULL,
        postal_city                   VARCHAR(100) NULL,
        postal_postal_code            VARCHAR(10) NULL,
        dual_citizen                  CHAR(1) NULL,
        citizenship_certificate_no    VARCHAR(50) NULL,
        citizenship_issued_date       DATETIME2(0) NULL,
        CONSTRAINT FK_CitizenAccount_UserAccount FOREIGN KEY (user_id) REFERENCES dbo.UserAccount(user_id),
        CONSTRAINT FK_CitizenAccount_GovernmentIdentityRecord FOREIGN KEY (government_record_id) REFERENCES dbo.GovernmentIdentityRecord(government_record_id),
        CONSTRAINT CK_CitizenAccount_PostalSame CHECK (postal_same_as_permanent IS NULL OR postal_same_as_permanent IN ('Y', 'N')),
        CONSTRAINT CK_CitizenAccount_DualCitizen CHECK (dual_citizen IS NULL OR dual_citizen IN ('Y', 'N'))
    );
END;
GO

IF OBJECT_ID(N'dbo.StaffAccount', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.StaffAccount
    (
        user_id       INT NOT NULL CONSTRAINT PK_StaffAccount PRIMARY KEY,
        employee_no   VARCHAR(20) NOT NULL,
        staff_role    VARCHAR(30) NOT NULL,
        staff_status  VARCHAR(20) NOT NULL CONSTRAINT DF_StaffAccount_Status DEFAULT ('ACTIVE'),
        CONSTRAINT UQ_StaffAccount_EmployeeNo UNIQUE (employee_no),
        CONSTRAINT FK_StaffAccount_UserAccount FOREIGN KEY (user_id) REFERENCES dbo.UserAccount(user_id),
        CONSTRAINT CK_StaffAccount_Role CHECK (staff_role IN ('NIC_OFFICER', 'PASSPORT_OFFICER', 'CSR', 'DIRECTOR', 'SYSTEM_ADMIN'))
    );
END;
GO

IF OBJECT_ID(N'dbo.Centre', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Centre
    (
        centre_id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Centre PRIMARY KEY,
        centre_name     VARCHAR(100) NOT NULL,
        street          VARCHAR(150) NULL,
        city            VARCHAR(100) NOT NULL,
        district        VARCHAR(100) NOT NULL,
        daily_capacity  INT NOT NULL,
        CONSTRAINT UQ_Centre_NameCity UNIQUE (centre_name, city),
        CONSTRAINT CK_Centre_DailyCapacity CHECK (daily_capacity BETWEEN 1 AND 2000)
    );
END;
GO

IF OBJECT_ID(N'dbo.Application', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Application
    (
        application_id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Application PRIMARY KEY,
        application_no       VARCHAR(30) NOT NULL,
        citizen_id           INT NOT NULL,
        service_type         VARCHAR(20) NOT NULL,
        application_kind     VARCHAR(20) NOT NULL,
        current_status       VARCHAR(30) NULL CONSTRAINT DF_Application_Status DEFAULT ('DRAFT'),
        submitted_at         DATETIME2(0) NULL,
        declaration_accepted CHAR(1) NULL CONSTRAINT DF_Application_Declaration DEFAULT ('N'),
        created_at           DATETIME2(0) NULL CONSTRAINT DF_Application_CreatedAt DEFAULT (SYSDATETIME()),
        updated_at           DATETIME2(0) NULL CONSTRAINT DF_Application_UpdatedAt DEFAULT (SYSDATETIME()),
        CONSTRAINT UQ_Application_Number UNIQUE (application_no),
        CONSTRAINT FK_Application_CitizenAccount FOREIGN KEY (citizen_id) REFERENCES dbo.CitizenAccount(user_id),
        CONSTRAINT CK_Application_ServiceType CHECK (service_type IN ('NIC', 'PASSPORT')),
        CONSTRAINT CK_Application_Declaration CHECK (declaration_accepted IS NULL OR declaration_accepted IN ('Y', 'N'))
    );
END;
GO

IF OBJECT_ID(N'dbo.NICApplication', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.NICApplication
    (
        application_id     INT NOT NULL CONSTRAINT PK_NICApplication PRIMARY KEY,
        last_nic_no        VARCHAR(12) NULL,
        last_issued_date   DATETIME2(0) NULL,
        police_station     VARCHAR(100) NULL,
        police_report_date DATETIME2(0) NULL,
        CONSTRAINT FK_NICApplication_Application FOREIGN KEY (application_id) REFERENCES dbo.Application(application_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.PassportApplication', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PassportApplication
    (
        application_id       INT NOT NULL CONSTRAINT PK_PassportApplication PRIMARY KEY,
        service_level        VARCHAR(20) NULL,
        travel_document_type VARCHAR(30) NULL,
        nic_proof_no         VARCHAR(12) NULL,
        parent_nic_no        VARCHAR(12) NULL,
        CONSTRAINT FK_PassportApplication_Application FOREIGN KEY (application_id) REFERENCES dbo.Application(application_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.ApplicationDocument', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ApplicationDocument
    (
        document_id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ApplicationDocument PRIMARY KEY,
        application_id       INT NOT NULL,
        document_type        VARCHAR(40) NOT NULL,
        file_path            VARCHAR(500) NOT NULL,
        uploaded_at          DATETIME2(0) NULL CONSTRAINT DF_ApplicationDocument_UploadedAt DEFAULT (SYSDATETIME()),
        verification_status  VARCHAR(20) NULL CONSTRAINT DF_ApplicationDocument_Status DEFAULT ('PENDING'),
        verified_by_staff_id INT NULL,
        verified_at          DATETIME2(0) NULL,
        CONSTRAINT FK_ApplicationDocument_Application FOREIGN KEY (application_id) REFERENCES dbo.Application(application_id),
        CONSTRAINT FK_ApplicationDocument_StaffAccount FOREIGN KEY (verified_by_staff_id) REFERENCES dbo.StaffAccount(user_id)
    );
    CREATE INDEX IX_ApplicationDocument_ApplicationId ON dbo.ApplicationDocument(application_id);
END;
GO

IF OBJECT_ID(N'dbo.ApplicationReview', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ApplicationReview
    (
        review_id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ApplicationReview PRIMARY KEY,
        application_id    INT NOT NULL,
        staff_id          INT NOT NULL,
        review_sequence   INT NOT NULL,
        reviewed_at       DATETIME2(0) NULL CONSTRAINT DF_ApplicationReview_ReviewedAt DEFAULT (SYSDATETIME()),
        decision          VARCHAR(30) NOT NULL,
        correction_message VARCHAR(1000) NULL,
        CONSTRAINT UQ_ApplicationReview_Sequence UNIQUE (application_id, review_sequence),
        CONSTRAINT FK_ApplicationReview_Application FOREIGN KEY (application_id) REFERENCES dbo.Application(application_id),
        CONSTRAINT FK_ApplicationReview_StaffAccount FOREIGN KEY (staff_id) REFERENCES dbo.StaffAccount(user_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.Appointment', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Appointment
    (
        appointment_id     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Appointment PRIMARY KEY,
        citizen_id         INT NULL,
        application_id     INT NULL,
        centre_id          INT NOT NULL,
        appointment_type   VARCHAR(20) NOT NULL,
        scheduled_at       DATETIME2(0) NOT NULL,
        appointment_status VARCHAR(25) NULL CONSTRAINT DF_Appointment_Status DEFAULT ('PENDING_PAYMENT'),
        reference_no       VARCHAR(50) NOT NULL,
        qr_code            VARCHAR(500) NULL,
        visitor_phone      VARCHAR(15) NULL,
        visitor_name       VARCHAR(100) NULL,
        visitor_service    VARCHAR(50) NULL,
        created_at         DATETIME2(0) NULL CONSTRAINT DF_Appointment_CreatedAt DEFAULT (SYSDATETIME()),
        CONSTRAINT UQ_Appointment_ReferenceNo UNIQUE (reference_no),
        CONSTRAINT FK_Appointment_CitizenAccount FOREIGN KEY (citizen_id) REFERENCES dbo.CitizenAccount(user_id),
        CONSTRAINT FK_Appointment_Application FOREIGN KEY (application_id) REFERENCES dbo.Application(application_id),
        CONSTRAINT FK_Appointment_Centre FOREIGN KEY (centre_id) REFERENCES dbo.Centre(centre_id),
        CONSTRAINT CK_Appointment_Type CHECK (appointment_type IN ('BIOMETRIC', 'WALK_IN'))
    );
    CREATE INDEX IX_Appointment_CitizenId ON dbo.Appointment(citizen_id);
    CREATE INDEX IX_Appointment_ApplicationId ON dbo.Appointment(application_id);
    CREATE INDEX IX_Appointment_CentreSchedule ON dbo.Appointment(centre_id, scheduled_at);
END;
GO

/* Upgrade support for databases created before the walk-in workflow. */
IF COL_LENGTH(N'dbo.Appointment', N'visitor_name') IS NULL
    ALTER TABLE dbo.Appointment ADD visitor_name VARCHAR(100) NULL;
GO
IF COL_LENGTH(N'dbo.Appointment', N'visitor_service') IS NULL
    ALTER TABLE dbo.Appointment ADD visitor_service VARCHAR(50) NULL;
GO

IF OBJECT_ID(N'dbo.AppointmentCheckIn', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppointmentCheckIn
    (
        appointment_id INT NOT NULL CONSTRAINT PK_AppointmentCheckIn PRIMARY KEY,
        staff_id        INT NOT NULL,
        checked_in_at   DATETIME2(0) NULL CONSTRAINT DF_AppointmentCheckIn_CheckedInAt DEFAULT (SYSDATETIME()),
        check_in_method VARCHAR(25) NOT NULL,
        CONSTRAINT FK_AppointmentCheckIn_Appointment FOREIGN KEY (appointment_id) REFERENCES dbo.Appointment(appointment_id),
        CONSTRAINT FK_AppointmentCheckIn_StaffAccount FOREIGN KEY (staff_id) REFERENCES dbo.StaffAccount(user_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.Payment', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Payment
    (
        payment_id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Payment PRIMARY KEY,
        appointment_id       INT NOT NULL,
        transaction_reference VARCHAR(50) NOT NULL,
        amount               DECIMAL(10,2) NOT NULL,
        paid_at              DATETIME2(0) NULL,
        payment_status       VARCHAR(20) NULL CONSTRAINT DF_Payment_Status DEFAULT ('PENDING'),
        created_at           DATETIME2(0) NULL CONSTRAINT DF_Payment_CreatedAt DEFAULT (SYSDATETIME()),
        CONSTRAINT UQ_Payment_TransactionReference UNIQUE (transaction_reference),
        CONSTRAINT FK_Payment_Appointment FOREIGN KEY (appointment_id) REFERENCES dbo.Appointment(appointment_id),
        CONSTRAINT CK_Payment_Amount CHECK (amount >= 0)
    );
    CREATE INDEX IX_Payment_AppointmentId ON dbo.Payment(appointment_id);
END;
GO

IF OBJECT_ID(N'dbo.BiometricCollection', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BiometricCollection
    (
        application_id   INT NOT NULL CONSTRAINT PK_BiometricCollection PRIMARY KEY,
        staff_id         INT NOT NULL,
        collected_at     DATETIME2(0) NULL CONSTRAINT DF_BiometricCollection_CollectedAt DEFAULT (SYSDATETIME()),
        collection_status VARCHAR(20) NULL CONSTRAINT DF_BiometricCollection_Status DEFAULT ('COLLECTED'),
        CONSTRAINT FK_BiometricCollection_Application FOREIGN KEY (application_id) REFERENCES dbo.Application(application_id),
        CONSTRAINT FK_BiometricCollection_StaffAccount FOREIGN KEY (staff_id) REFERENCES dbo.StaffAccount(user_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.Notification', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notification
    (
        notification_id              INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Notification PRIMARY KEY,
        sent_by_staff_id              INT NULL,
        generated_by_status_update_id INT NULL,
        notification_type             VARCHAR(40) NOT NULL,
        message                       VARCHAR(2000) NOT NULL,
        created_at                    DATETIME2(0) NULL CONSTRAINT DF_Notification_CreatedAt DEFAULT (SYSDATETIME()),
        is_broadcast                  CHAR(1) NULL CONSTRAINT DF_Notification_IsBroadcast DEFAULT ('N'),
        CONSTRAINT FK_Notification_StaffAccount FOREIGN KEY (sent_by_staff_id) REFERENCES dbo.StaffAccount(user_id),
        CONSTRAINT CK_Notification_IsBroadcast CHECK (is_broadcast IS NULL OR is_broadcast IN ('Y', 'N'))
    );
END;
GO

IF OBJECT_ID(N'dbo.UserNotification', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.UserNotification
    (
        notification_id INT NOT NULL,
        user_id          INT NOT NULL,
        delivered_at     DATETIME2(0) NULL CONSTRAINT DF_UserNotification_DeliveredAt DEFAULT (SYSDATETIME()),
        read_at          DATETIME2(0) NULL,
        CONSTRAINT PK_UserNotification PRIMARY KEY (notification_id, user_id),
        CONSTRAINT FK_UserNotification_Notification FOREIGN KEY (notification_id) REFERENCES dbo.Notification(notification_id),
        CONSTRAINT FK_UserNotification_UserAccount FOREIGN KEY (user_id) REFERENCES dbo.UserAccount(user_id)
    );
    CREATE INDEX IX_UserNotification_UserRead ON dbo.UserNotification(user_id, read_at);
END;
GO

IF OBJECT_ID(N'dbo.SupportRequest', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SupportRequest
    (
        request_id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SupportRequest PRIMARY KEY,
        citizen_id          INT NOT NULL,
        handled_by_staff_id INT NULL,
        request_type        VARCHAR(20) NOT NULL,
        subject             VARCHAR(200) NOT NULL,
        message             VARCHAR(2000) NOT NULL,
        created_at          DATETIME2(0) NULL CONSTRAINT DF_SupportRequest_CreatedAt DEFAULT (SYSDATETIME()),
        request_status      VARCHAR(20) NULL CONSTRAINT DF_SupportRequest_Status DEFAULT ('OPEN'),
        responded_at        DATETIME2(0) NULL,
        response            VARCHAR(2000) NULL,
        CONSTRAINT FK_SupportRequest_CitizenAccount FOREIGN KEY (citizen_id) REFERENCES dbo.CitizenAccount(user_id),
        CONSTRAINT FK_SupportRequest_StaffAccount FOREIGN KEY (handled_by_staff_id) REFERENCES dbo.StaffAccount(user_id)
    );
    CREATE INDEX IX_SupportRequest_CitizenCreated ON dbo.SupportRequest(citizen_id, created_at DESC);
    CREATE INDEX IX_SupportRequest_Status ON dbo.SupportRequest(request_status, request_type);
END;
GO

IF OBJECT_ID(N'dbo.DocumentDelivery', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DocumentDelivery
    (
        delivery_id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_DocumentDelivery PRIMARY KEY,
        application_id    INT NOT NULL,
        centre_id         INT NULL,
        delivery_method   VARCHAR(25) NOT NULL,
        delivery_address  VARCHAR(500) NULL,
        delivery_status   VARCHAR(25) NOT NULL CONSTRAINT DF_DocumentDelivery_Status DEFAULT ('PREPARING'),
        tracking_reference VARCHAR(50) NOT NULL,
        created_at        DATETIME2(0) NOT NULL CONSTRAINT DF_DocumentDelivery_CreatedAt DEFAULT (SYSDATETIME()),
        completed_at      DATETIME2(0) NULL,
        CONSTRAINT UQ_DocumentDelivery_Application UNIQUE (application_id),
        CONSTRAINT UQ_DocumentDelivery_TrackingReference UNIQUE (tracking_reference),
        CONSTRAINT FK_DocumentDelivery_Application FOREIGN KEY (application_id) REFERENCES dbo.Application(application_id),
        CONSTRAINT FK_DocumentDelivery_Centre FOREIGN KEY (centre_id) REFERENCES dbo.Centre(centre_id)
    );
END;
GO

IF OBJECT_ID(N'dbo.WalkInSlot', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.WalkInSlot
    (
        slot_id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_WalkInSlot PRIMARY KEY,
        centre_id     INT NOT NULL,
        slot_date     DATE NOT NULL,
        slot_time     TIME(0) NOT NULL,
        slot_capacity INT NOT NULL,
        is_available  CHAR(1) NOT NULL CONSTRAINT DF_WalkInSlot_Available DEFAULT ('Y'),
        CONSTRAINT UQ_WalkInSlot_CentreDateTime UNIQUE (centre_id, slot_date, slot_time),
        CONSTRAINT FK_WalkInSlot_Centre FOREIGN KEY (centre_id) REFERENCES dbo.Centre(centre_id),
        CONSTRAINT CK_WalkInSlot_Capacity CHECK (slot_capacity BETWEEN 1 AND 100),
        CONSTRAINT CK_WalkInSlot_Available CHECK (is_available IN ('Y', 'N'))
    );
END;
GO

PRINT 'LankaID database setup completed successfully.';
GO

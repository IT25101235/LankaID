/*
    LankaID / Identity Issuance Service
    Current database structure snapshot

    Source database : identity_issuance_service on localhost
    Generated       : 2026-09-19 19:16:55 +05:30
    Contents        : Schema only (no application or personal data)
    SQL dialect     : Microsoft SQL Server (T-SQL)

    Usage:
      1. Run this entire file in SQL Server Management Studio or Azure Data Studio.
      2. The database is created if it does not exist.
      3. For safety, execution stops if the target database already contains any
         application tables. Use a fresh/empty database for this snapshot.
*/

USE [master];
GO

IF DB_ID(N'identity_issuance_service') IS NULL
BEGIN
    CREATE DATABASE [identity_issuance_service] COLLATE SQL_Latin1_General_CP1_CI_AS;
END;
GO

USE [identity_issuance_service];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

IF EXISTS
(
    SELECT 1
    FROM sys.tables AS t
    WHERE t.is_ms_shipped = 0
      AND t.name <> N'sysdiagrams'
)
BEGIN
    THROW 50001,
          'Schema creation stopped: the target database already contains application tables. Use a fresh or empty database.',
          1;
END;
GO
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[Centre](
	[centre_id] [int] IDENTITY(1,1) NOT NULL,
	[centre_name] [varchar](100) NOT NULL,
	[street] [varchar](150) NULL,
	[city] [varchar](100) NOT NULL,
	[district] [varchar](100) NOT NULL,
	[daily_capacity] [int] NOT NULL,
 CONSTRAINT [pk_centre] PRIMARY KEY CLUSTERED 
(
	[centre_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[GovernmentIdentityRecord](
	[government_record_id] [int] IDENTITY(1,1) NOT NULL,
	[nic_no] [varchar](12) NULL,
	[passport_no] [varchar](20) NULL,
	[registered_email] [varchar](100) NULL,
	[registered_phone] [varchar](15) NULL,
	[identity_status] [varchar](20) NULL,
 CONSTRAINT [pk_government_record] PRIMARY KEY CLUSTERED 
(
	[government_record_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[UserAccount](
	[user_id] [int] IDENTITY(1,1) NOT NULL,
	[email] [varchar](100) NOT NULL,
	[phone] [varchar](15) NOT NULL,
	[password_hash] [varchar](255) NOT NULL,
	[account_status] [varchar](20) NULL,
	[email_verified] [char](1) NULL,
	[phone_verified] [char](1) NULL,
	[registered_at] [datetime] NULL,
 CONSTRAINT [pk_user_account] PRIMARY KEY CLUSTERED 
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_user_email] UNIQUE NONCLUSTERED 
(
	[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_user_phone] UNIQUE NONCLUSTERED 
(
	[phone] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[WalkInSlot](
	[slot_id] [int] IDENTITY(1,1) NOT NULL,
	[centre_id] [int] NOT NULL,
	[slot_date] [date] NOT NULL,
	[slot_time] [time](0) NOT NULL,
	[slot_capacity] [int] NOT NULL,
	[is_available] [char](1) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[slot_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_walk_in_slot] UNIQUE NONCLUSTERED 
(
	[centre_id] ASC,
	[slot_date] ASC,
	[slot_time] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[StaffAccount](
	[user_id] [int] NOT NULL,
	[employee_no] [varchar](20) NOT NULL,
	[staff_role] [varchar](30) NOT NULL,
	[staff_status] [varchar](20) NULL,
	[created_by_staff_id] [int] NULL,
	[staff_created_at] [datetime] NULL,
 CONSTRAINT [pk_staff_account] PRIMARY KEY CLUSTERED 
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_employee_no] UNIQUE NONCLUSTERED 
(
	[employee_no] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[CitizenAccount](
	[user_id] [int] NOT NULL,
	[government_record_id] [int] NULL,
	[family_name] [varchar](100) NULL,
	[given_name] [varchar](150) NULL,
	[surname] [varchar](100) NULL,
	[gender] [varchar](10) NULL,
	[civil_status] [varchar](15) NULL,
	[occupation] [varchar](150) NULL,
	[date_of_birth] [datetime] NULL,
	[birth_certificate_no] [varchar](50) NULL,
	[birth_place] [varchar](100) NULL,
	[birth_division] [varchar](100) NULL,
	[birth_district] [varchar](100) NULL,
	[country_of_birth] [varchar](100) NULL,
	[birth_city] [varchar](100) NULL,
	[permanent_house_no] [varchar](50) NULL,
	[permanent_street] [varchar](150) NULL,
	[permanent_city] [varchar](100) NULL,
	[permanent_postal_code] [varchar](10) NULL,
	[postal_same_as_permanent] [char](1) NULL,
	[postal_house_no] [varchar](50) NULL,
	[postal_street] [varchar](150) NULL,
	[postal_city] [varchar](100) NULL,
	[postal_postal_code] [varchar](10) NULL,
	[dual_citizen] [char](1) NULL,
	[citizenship_certificate_no] [varchar](50) NULL,
	[citizenship_issued_date] [datetime] NULL,
 CONSTRAINT [pk_citizen_account] PRIMARY KEY CLUSTERED 
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[BiometricAppointmentSlot](
	[slot_id] [int] IDENTITY(1,1) NOT NULL,
	[centre_id] [int] NOT NULL,
	[service_type] [varchar](20) NOT NULL,
	[slot_date] [date] NOT NULL,
	[slot_time] [time](0) NOT NULL,
	[slot_capacity] [int] NOT NULL,
	[is_available] [char](1) NOT NULL,
	[created_by] [int] NOT NULL,
	[created_at] [datetime2](7) NOT NULL,
	[updated_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[slot_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [UQ_BiometricAppointmentSlot] UNIQUE NONCLUSTERED 
(
	[centre_id] ASC,
	[service_type] ASC,
	[slot_date] ASC,
	[slot_time] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[Application](
	[application_id] [int] IDENTITY(1,1) NOT NULL,
	[application_no] [varchar](30) NOT NULL,
	[citizen_id] [int] NOT NULL,
	[service_type] [varchar](20) NOT NULL,
	[application_kind] [varchar](20) NOT NULL,
	[current_status] [varchar](30) NULL,
	[submitted_at] [datetime] NULL,
	[declaration_accepted] [char](1) NULL,
	[created_at] [datetime] NULL,
	[updated_at] [datetime] NULL,
 CONSTRAINT [pk_application] PRIMARY KEY CLUSTERED 
(
	[application_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_application_no] UNIQUE NONCLUSTERED 
(
	[application_no] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[SupportRequest](
	[request_id] [int] IDENTITY(1,1) NOT NULL,
	[citizen_id] [int] NOT NULL,
	[handled_by_staff_id] [int] NULL,
	[request_type] [varchar](20) NOT NULL,
	[subject] [varchar](200) NOT NULL,
	[message] [varchar](2000) NOT NULL,
	[created_at] [datetime] NULL,
	[request_status] [varchar](20) NULL,
	[responded_at] [datetime] NULL,
	[response] [varchar](2000) NULL,
 CONSTRAINT [pk_support_request] PRIMARY KEY CLUSTERED 
(
	[request_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[NICApplication](
	[application_id] [int] NOT NULL,
	[last_nic_no] [varchar](12) NULL,
	[last_issued_date] [datetime] NULL,
	[police_station] [varchar](100) NULL,
	[police_report_date] [datetime] NULL,
 CONSTRAINT [pk_nic_application] PRIMARY KEY CLUSTERED 
(
	[application_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[StatusUpdate](
	[status_update_id] [int] IDENTITY(1,1) NOT NULL,
	[application_id] [int] NOT NULL,
	[changed_by_staff_id] [int] NULL,
	[status] [varchar](30) NOT NULL,
	[changed_at] [datetime] NULL,
	[note] [varchar](1000) NULL,
 CONSTRAINT [pk_status_update] PRIMARY KEY CLUSTERED 
(
	[status_update_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[DocumentDelivery](
	[delivery_id] [int] IDENTITY(1,1) NOT NULL,
	[application_id] [int] NOT NULL,
	[centre_id] [int] NULL,
	[delivery_method] [varchar](25) NOT NULL,
	[delivery_address] [varchar](500) NULL,
	[delivery_status] [varchar](25) NOT NULL,
	[tracking_reference] [varchar](50) NOT NULL,
	[created_at] [datetime] NOT NULL,
	[completed_at] [datetime] NULL,
 CONSTRAINT [pk_document_delivery] PRIMARY KEY CLUSTERED 
(
	[delivery_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_document_delivery_application] UNIQUE NONCLUSTERED 
(
	[application_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_document_delivery_tracking] UNIQUE NONCLUSTERED 
(
	[tracking_reference] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[PassportApplication](
	[application_id] [int] NOT NULL,
	[service_level] [varchar](20) NULL,
	[travel_document_type] [varchar](30) NULL,
	[nic_proof_no] [varchar](12) NULL,
	[parent_nic_no] [varchar](12) NULL,
 CONSTRAINT [pk_passport_application] PRIMARY KEY CLUSTERED 
(
	[application_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[Appointment](
	[appointment_id] [int] IDENTITY(1,1) NOT NULL,
	[citizen_id] [int] NULL,
	[application_id] [int] NULL,
	[centre_id] [int] NOT NULL,
	[appointment_type] [varchar](20) NOT NULL,
	[scheduled_at] [datetime] NOT NULL,
	[appointment_status] [varchar](25) NULL,
	[reference_no] [varchar](50) NOT NULL,
	[qr_code] [varchar](500) NULL,
	[visitor_phone] [varchar](15) NULL,
	[created_at] [datetime] NULL,
	[visitor_name] [varchar](100) NULL,
	[visitor_service] [varchar](50) NULL,
	[booking_version] [bigint] NOT NULL,
	[biometric_slot_id] [int] NULL,
 CONSTRAINT [pk_appointment] PRIMARY KEY CLUSTERED 
(
	[appointment_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_appointment_reference] UNIQUE NONCLUSTERED 
(
	[reference_no] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[ApplicationReview](
	[review_id] [int] IDENTITY(1,1) NOT NULL,
	[application_id] [int] NOT NULL,
	[staff_id] [int] NOT NULL,
	[review_sequence] [int] NOT NULL,
	[reviewed_at] [datetime] NULL,
	[decision] [varchar](30) NOT NULL,
	[correction_message] [varchar](1000) NULL,
 CONSTRAINT [pk_application_review] PRIMARY KEY CLUSTERED 
(
	[review_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_review_sequence] UNIQUE NONCLUSTERED 
(
	[application_id] ASC,
	[review_sequence] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[ApplicationDocument](
	[document_id] [int] IDENTITY(1,1) NOT NULL,
	[application_id] [int] NOT NULL,
	[document_type] [varchar](40) NOT NULL,
	[file_path] [varchar](500) NOT NULL,
	[uploaded_at] [datetime] NULL,
	[verification_status] [varchar](20) NULL,
	[verified_by_staff_id] [int] NULL,
	[verified_at] [datetime] NULL,
 CONSTRAINT [pk_application_document] PRIMARY KEY CLUSTERED 
(
	[document_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[BiometricCollection](
	[application_id] [int] NOT NULL,
	[staff_id] [int] NOT NULL,
	[collected_at] [datetime] NULL,
	[collection_status] [varchar](20) NULL,
 CONSTRAINT [pk_biometric_collection] PRIMARY KEY CLUSTERED 
(
	[application_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[Payment](
	[payment_id] [int] IDENTITY(1,1) NOT NULL,
	[appointment_id] [int] NOT NULL,
	[transaction_reference] [varchar](50) NOT NULL,
	[amount] [money] NOT NULL,
	[paid_at] [datetime] NULL,
	[payment_status] [varchar](20) NULL,
	[created_at] [datetime] NULL,
 CONSTRAINT [pk_payment] PRIMARY KEY CLUSTERED 
(
	[payment_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_payment_appointment] UNIQUE NONCLUSTERED 
(
	[appointment_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [uq_transaction_reference] UNIQUE NONCLUSTERED 
(
	[transaction_reference] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[AppointmentCheckIn](
	[appointment_id] [int] NOT NULL,
	[staff_id] [int] NOT NULL,
	[checked_in_at] [datetime] NULL,
	[check_in_method] [varchar](25) NOT NULL,
 CONSTRAINT [pk_appointment_check_in] PRIMARY KEY CLUSTERED 
(
	[appointment_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[AppointmentChangePayment](
	[change_payment_id] [int] IDENTITY(1,1) NOT NULL,
	[appointment_id] [int] NOT NULL,
	[request_token] [varchar](36) NOT NULL,
	[change_type] [varchar](20) NOT NULL,
	[amount] [decimal](19, 2) NOT NULL,
	[previous_scheduled_at] [datetime2](7) NOT NULL,
	[new_scheduled_at] [datetime2](7) NOT NULL,
	[paid_at] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[change_payment_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
UNIQUE NONCLUSTERED 
(
	[request_token] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[Notification](
	[notification_id] [int] IDENTITY(1,1) NOT NULL,
	[sent_by_staff_id] [int] NULL,
	[generated_by_status_update_id] [int] NULL,
	[notification_type] [varchar](40) NOT NULL,
	[message] [varchar](2000) NOT NULL,
	[created_at] [datetime] NULL,
	[is_broadcast] [char](1) NULL,
 CONSTRAINT [pk_notification] PRIMARY KEY CLUSTERED 
(
	[notification_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[UserNotification](
	[notification_id] [int] NOT NULL,
	[user_id] [int] NOT NULL,
	[delivered_at] [datetime] NULL,
	[read_at] [datetime] NULL,
 CONSTRAINT [pk_user_notification] PRIMARY KEY CLUSTERED 
(
	[notification_id] ASC,
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO

SET ANSI_PADDING ON

GO

CREATE NONCLUSTERED INDEX [IX_BiometricAppointmentSlot_calendar] ON [dbo].[BiometricAppointmentSlot]
(
	[service_type] ASC,
	[centre_id] ASC,
	[slot_date] ASC,
	[is_available] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO

SET ANSI_PADDING ON

GO

CREATE NONCLUSTERED INDEX [IX_Appointment_biometric_slot_status] ON [dbo].[Appointment]
(
	[biometric_slot_id] ASC,
	[appointment_status] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO

ALTER TABLE [dbo].[GovernmentIdentityRecord] ADD  DEFAULT ('ACTIVE') FOR [identity_status]
GO

ALTER TABLE [dbo].[UserAccount] ADD  DEFAULT ('ACTIVE') FOR [account_status]
GO

ALTER TABLE [dbo].[UserAccount] ADD  DEFAULT ('N') FOR [email_verified]
GO

ALTER TABLE [dbo].[UserAccount] ADD  DEFAULT ('N') FOR [phone_verified]
GO

ALTER TABLE [dbo].[UserAccount] ADD  DEFAULT (getdate()) FOR [registered_at]
GO

ALTER TABLE [dbo].[WalkInSlot] ADD  CONSTRAINT [df_walk_in_slot_available]  DEFAULT ('Y') FOR [is_available]
GO

ALTER TABLE [dbo].[StaffAccount] ADD  DEFAULT ('ACTIVE') FOR [staff_status]
GO

ALTER TABLE [dbo].[StaffAccount] ADD  DEFAULT (getdate()) FOR [staff_created_at]
GO

ALTER TABLE [dbo].[CitizenAccount] ADD  DEFAULT ('Y') FOR [postal_same_as_permanent]
GO

ALTER TABLE [dbo].[CitizenAccount] ADD  DEFAULT ('N') FOR [dual_citizen]
GO

ALTER TABLE [dbo].[BiometricAppointmentSlot] ADD  CONSTRAINT [DF_BiometricAppointmentSlot_created_at]  DEFAULT (sysdatetime()) FOR [created_at]
GO

ALTER TABLE [dbo].[BiometricAppointmentSlot] ADD  CONSTRAINT [DF_BiometricAppointmentSlot_updated_at]  DEFAULT (sysdatetime()) FOR [updated_at]
GO

ALTER TABLE [dbo].[Application] ADD  DEFAULT ('DRAFT') FOR [current_status]
GO

ALTER TABLE [dbo].[Application] ADD  DEFAULT ('N') FOR [declaration_accepted]
GO

ALTER TABLE [dbo].[Application] ADD  DEFAULT (getdate()) FOR [created_at]
GO

ALTER TABLE [dbo].[Application] ADD  DEFAULT (getdate()) FOR [updated_at]
GO

ALTER TABLE [dbo].[SupportRequest] ADD  DEFAULT (getdate()) FOR [created_at]
GO

ALTER TABLE [dbo].[SupportRequest] ADD  DEFAULT ('OPEN') FOR [request_status]
GO

ALTER TABLE [dbo].[StatusUpdate] ADD  DEFAULT (getdate()) FOR [changed_at]
GO

ALTER TABLE [dbo].[DocumentDelivery] ADD  CONSTRAINT [df_document_delivery_status]  DEFAULT ('PREPARING') FOR [delivery_status]
GO

ALTER TABLE [dbo].[DocumentDelivery] ADD  CONSTRAINT [df_document_delivery_created_at]  DEFAULT (getdate()) FOR [created_at]
GO

ALTER TABLE [dbo].[PassportApplication] ADD  DEFAULT ('NORMAL') FOR [service_level]
GO

ALTER TABLE [dbo].[PassportApplication] ADD  DEFAULT ('ALL_COUNTRIES') FOR [travel_document_type]
GO

ALTER TABLE [dbo].[Appointment] ADD  DEFAULT ('PENDING_PAYMENT') FOR [appointment_status]
GO

ALTER TABLE [dbo].[Appointment] ADD  DEFAULT (getdate()) FOR [created_at]
GO

ALTER TABLE [dbo].[Appointment] ADD  CONSTRAINT [DF_Appointment_booking_version]  DEFAULT ((0)) FOR [booking_version]
GO

ALTER TABLE [dbo].[ApplicationReview] ADD  DEFAULT (getdate()) FOR [reviewed_at]
GO

ALTER TABLE [dbo].[ApplicationDocument] ADD  DEFAULT (getdate()) FOR [uploaded_at]
GO

ALTER TABLE [dbo].[ApplicationDocument] ADD  DEFAULT ('PENDING') FOR [verification_status]
GO

ALTER TABLE [dbo].[BiometricCollection] ADD  DEFAULT (getdate()) FOR [collected_at]
GO

ALTER TABLE [dbo].[BiometricCollection] ADD  DEFAULT ('COLLECTED') FOR [collection_status]
GO

ALTER TABLE [dbo].[Payment] ADD  DEFAULT ('PENDING') FOR [payment_status]
GO

ALTER TABLE [dbo].[Payment] ADD  DEFAULT (getdate()) FOR [created_at]
GO

ALTER TABLE [dbo].[AppointmentCheckIn] ADD  DEFAULT (getdate()) FOR [checked_in_at]
GO

ALTER TABLE [dbo].[Notification] ADD  DEFAULT (getdate()) FOR [created_at]
GO

ALTER TABLE [dbo].[Notification] ADD  DEFAULT ('N') FOR [is_broadcast]
GO

ALTER TABLE [dbo].[UserNotification] ADD  DEFAULT (getdate()) FOR [delivered_at]
GO

ALTER TABLE [dbo].[WalkInSlot]  WITH CHECK ADD  CONSTRAINT [fk_walk_in_slot_centre] FOREIGN KEY([centre_id])
REFERENCES [dbo].[Centre] ([centre_id])
GO

ALTER TABLE [dbo].[WalkInSlot] CHECK CONSTRAINT [fk_walk_in_slot_centre]
GO

ALTER TABLE [dbo].[StaffAccount]  WITH CHECK ADD  CONSTRAINT [fk_staff_creator] FOREIGN KEY([created_by_staff_id])
REFERENCES [dbo].[StaffAccount] ([user_id])
GO

ALTER TABLE [dbo].[StaffAccount] CHECK CONSTRAINT [fk_staff_creator]
GO

ALTER TABLE [dbo].[StaffAccount]  WITH CHECK ADD  CONSTRAINT [fk_staff_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[UserAccount] ([user_id])
GO

ALTER TABLE [dbo].[StaffAccount] CHECK CONSTRAINT [fk_staff_user]
GO

ALTER TABLE [dbo].[CitizenAccount]  WITH CHECK ADD  CONSTRAINT [fk_citizen_government] FOREIGN KEY([government_record_id])
REFERENCES [dbo].[GovernmentIdentityRecord] ([government_record_id])
GO

ALTER TABLE [dbo].[CitizenAccount] CHECK CONSTRAINT [fk_citizen_government]
GO

ALTER TABLE [dbo].[CitizenAccount]  WITH CHECK ADD  CONSTRAINT [fk_citizen_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[UserAccount] ([user_id])
GO

ALTER TABLE [dbo].[CitizenAccount] CHECK CONSTRAINT [fk_citizen_user]
GO

ALTER TABLE [dbo].[BiometricAppointmentSlot]  WITH CHECK ADD FOREIGN KEY([centre_id])
REFERENCES [dbo].[Centre] ([centre_id])
GO

ALTER TABLE [dbo].[Application]  WITH CHECK ADD  CONSTRAINT [fk_application_citizen] FOREIGN KEY([citizen_id])
REFERENCES [dbo].[CitizenAccount] ([user_id])
GO

ALTER TABLE [dbo].[Application] CHECK CONSTRAINT [fk_application_citizen]
GO

ALTER TABLE [dbo].[SupportRequest]  WITH CHECK ADD  CONSTRAINT [fk_support_citizen] FOREIGN KEY([citizen_id])
REFERENCES [dbo].[CitizenAccount] ([user_id])
GO

ALTER TABLE [dbo].[SupportRequest] CHECK CONSTRAINT [fk_support_citizen]
GO

ALTER TABLE [dbo].[SupportRequest]  WITH CHECK ADD  CONSTRAINT [fk_support_staff] FOREIGN KEY([handled_by_staff_id])
REFERENCES [dbo].[StaffAccount] ([user_id])
GO

ALTER TABLE [dbo].[SupportRequest] CHECK CONSTRAINT [fk_support_staff]
GO

ALTER TABLE [dbo].[NICApplication]  WITH CHECK ADD  CONSTRAINT [fk_nic_application] FOREIGN KEY([application_id])
REFERENCES [dbo].[Application] ([application_id])
GO

ALTER TABLE [dbo].[NICApplication] CHECK CONSTRAINT [fk_nic_application]
GO

ALTER TABLE [dbo].[StatusUpdate]  WITH CHECK ADD  CONSTRAINT [fk_status_application] FOREIGN KEY([application_id])
REFERENCES [dbo].[Application] ([application_id])
GO

ALTER TABLE [dbo].[StatusUpdate] CHECK CONSTRAINT [fk_status_application]
GO

ALTER TABLE [dbo].[StatusUpdate]  WITH CHECK ADD  CONSTRAINT [fk_status_staff] FOREIGN KEY([changed_by_staff_id])
REFERENCES [dbo].[StaffAccount] ([user_id])
GO

ALTER TABLE [dbo].[StatusUpdate] CHECK CONSTRAINT [fk_status_staff]
GO

ALTER TABLE [dbo].[DocumentDelivery]  WITH CHECK ADD  CONSTRAINT [fk_document_delivery_application] FOREIGN KEY([application_id])
REFERENCES [dbo].[Application] ([application_id])
GO

ALTER TABLE [dbo].[DocumentDelivery] CHECK CONSTRAINT [fk_document_delivery_application]
GO

ALTER TABLE [dbo].[DocumentDelivery]  WITH CHECK ADD  CONSTRAINT [fk_document_delivery_centre] FOREIGN KEY([centre_id])
REFERENCES [dbo].[Centre] ([centre_id])
GO

ALTER TABLE [dbo].[DocumentDelivery] CHECK CONSTRAINT [fk_document_delivery_centre]
GO

ALTER TABLE [dbo].[PassportApplication]  WITH CHECK ADD  CONSTRAINT [fk_passport_application] FOREIGN KEY([application_id])
REFERENCES [dbo].[Application] ([application_id])
GO

ALTER TABLE [dbo].[PassportApplication] CHECK CONSTRAINT [fk_passport_application]
GO

ALTER TABLE [dbo].[Appointment]  WITH CHECK ADD  CONSTRAINT [fk_appointment_application] FOREIGN KEY([application_id])
REFERENCES [dbo].[Application] ([application_id])
GO

ALTER TABLE [dbo].[Appointment] CHECK CONSTRAINT [fk_appointment_application]
GO

ALTER TABLE [dbo].[Appointment]  WITH CHECK ADD  CONSTRAINT [FK_Appointment_BiometricAppointmentSlot] FOREIGN KEY([biometric_slot_id])
REFERENCES [dbo].[BiometricAppointmentSlot] ([slot_id])
GO

ALTER TABLE [dbo].[Appointment] CHECK CONSTRAINT [FK_Appointment_BiometricAppointmentSlot]
GO

ALTER TABLE [dbo].[Appointment]  WITH CHECK ADD  CONSTRAINT [fk_appointment_centre] FOREIGN KEY([centre_id])
REFERENCES [dbo].[Centre] ([centre_id])
GO

ALTER TABLE [dbo].[Appointment] CHECK CONSTRAINT [fk_appointment_centre]
GO

ALTER TABLE [dbo].[Appointment]  WITH CHECK ADD  CONSTRAINT [fk_appointment_citizen] FOREIGN KEY([citizen_id])
REFERENCES [dbo].[CitizenAccount] ([user_id])
GO

ALTER TABLE [dbo].[Appointment] CHECK CONSTRAINT [fk_appointment_citizen]
GO

ALTER TABLE [dbo].[ApplicationReview]  WITH CHECK ADD  CONSTRAINT [fk_review_application] FOREIGN KEY([application_id])
REFERENCES [dbo].[Application] ([application_id])
GO

ALTER TABLE [dbo].[ApplicationReview] CHECK CONSTRAINT [fk_review_application]
GO

ALTER TABLE [dbo].[ApplicationReview]  WITH CHECK ADD  CONSTRAINT [fk_review_staff] FOREIGN KEY([staff_id])
REFERENCES [dbo].[StaffAccount] ([user_id])
GO

ALTER TABLE [dbo].[ApplicationReview] CHECK CONSTRAINT [fk_review_staff]
GO

ALTER TABLE [dbo].[ApplicationDocument]  WITH CHECK ADD  CONSTRAINT [fk_document_application] FOREIGN KEY([application_id])
REFERENCES [dbo].[Application] ([application_id])
GO

ALTER TABLE [dbo].[ApplicationDocument] CHECK CONSTRAINT [fk_document_application]
GO

ALTER TABLE [dbo].[ApplicationDocument]  WITH CHECK ADD  CONSTRAINT [fk_document_staff] FOREIGN KEY([verified_by_staff_id])
REFERENCES [dbo].[StaffAccount] ([user_id])
GO

ALTER TABLE [dbo].[ApplicationDocument] CHECK CONSTRAINT [fk_document_staff]
GO

ALTER TABLE [dbo].[BiometricCollection]  WITH CHECK ADD  CONSTRAINT [fk_biometric_application] FOREIGN KEY([application_id])
REFERENCES [dbo].[Application] ([application_id])
GO

ALTER TABLE [dbo].[BiometricCollection] CHECK CONSTRAINT [fk_biometric_application]
GO

ALTER TABLE [dbo].[BiometricCollection]  WITH CHECK ADD  CONSTRAINT [fk_biometric_staff] FOREIGN KEY([staff_id])
REFERENCES [dbo].[StaffAccount] ([user_id])
GO

ALTER TABLE [dbo].[BiometricCollection] CHECK CONSTRAINT [fk_biometric_staff]
GO

ALTER TABLE [dbo].[Payment]  WITH CHECK ADD  CONSTRAINT [fk_payment_appointment] FOREIGN KEY([appointment_id])
REFERENCES [dbo].[Appointment] ([appointment_id])
GO

ALTER TABLE [dbo].[Payment] CHECK CONSTRAINT [fk_payment_appointment]
GO

ALTER TABLE [dbo].[AppointmentCheckIn]  WITH CHECK ADD  CONSTRAINT [fk_check_in_appointment] FOREIGN KEY([appointment_id])
REFERENCES [dbo].[Appointment] ([appointment_id])
GO

ALTER TABLE [dbo].[AppointmentCheckIn] CHECK CONSTRAINT [fk_check_in_appointment]
GO

ALTER TABLE [dbo].[AppointmentCheckIn]  WITH CHECK ADD  CONSTRAINT [fk_check_in_staff] FOREIGN KEY([staff_id])
REFERENCES [dbo].[StaffAccount] ([user_id])
GO

ALTER TABLE [dbo].[AppointmentCheckIn] CHECK CONSTRAINT [fk_check_in_staff]
GO

ALTER TABLE [dbo].[AppointmentChangePayment]  WITH CHECK ADD FOREIGN KEY([appointment_id])
REFERENCES [dbo].[Appointment] ([appointment_id])
GO

ALTER TABLE [dbo].[Notification]  WITH CHECK ADD  CONSTRAINT [fk_notification_staff] FOREIGN KEY([sent_by_staff_id])
REFERENCES [dbo].[StaffAccount] ([user_id])
GO

ALTER TABLE [dbo].[Notification] CHECK CONSTRAINT [fk_notification_staff]
GO

ALTER TABLE [dbo].[Notification]  WITH CHECK ADD  CONSTRAINT [fk_notification_status] FOREIGN KEY([generated_by_status_update_id])
REFERENCES [dbo].[StatusUpdate] ([status_update_id])
GO

ALTER TABLE [dbo].[Notification] CHECK CONSTRAINT [fk_notification_status]
GO

ALTER TABLE [dbo].[UserNotification]  WITH CHECK ADD  CONSTRAINT [fk_notification_user] FOREIGN KEY([user_id])
REFERENCES [dbo].[UserAccount] ([user_id])
GO

ALTER TABLE [dbo].[UserNotification] CHECK CONSTRAINT [fk_notification_user]
GO

ALTER TABLE [dbo].[UserNotification]  WITH CHECK ADD  CONSTRAINT [fk_user_notification] FOREIGN KEY([notification_id])
REFERENCES [dbo].[Notification] ([notification_id])
GO

ALTER TABLE [dbo].[UserNotification] CHECK CONSTRAINT [fk_user_notification]
GO

ALTER TABLE [dbo].[Centre]  WITH CHECK ADD  CONSTRAINT [chk_centre_capacity] CHECK  (([daily_capacity]>(0)))
GO

ALTER TABLE [dbo].[Centre] CHECK CONSTRAINT [chk_centre_capacity]
GO

ALTER TABLE [dbo].[GovernmentIdentityRecord]  WITH CHECK ADD  CONSTRAINT [chk_government_status] CHECK  (([identity_status]='CANCELLED' OR [identity_status]='DAMAGED' OR [identity_status]='LOST' OR [identity_status]='EXPIRED' OR [identity_status]='ACTIVE'))
GO

ALTER TABLE [dbo].[GovernmentIdentityRecord] CHECK CONSTRAINT [chk_government_status]
GO

ALTER TABLE [dbo].[UserAccount]  WITH CHECK ADD  CONSTRAINT [chk_email_verified] CHECK  (([email_verified]='N' OR [email_verified]='Y'))
GO

ALTER TABLE [dbo].[UserAccount] CHECK CONSTRAINT [chk_email_verified]
GO

ALTER TABLE [dbo].[UserAccount]  WITH CHECK ADD  CONSTRAINT [chk_phone_verified] CHECK  (([phone_verified]='N' OR [phone_verified]='Y'))
GO

ALTER TABLE [dbo].[UserAccount] CHECK CONSTRAINT [chk_phone_verified]
GO

ALTER TABLE [dbo].[UserAccount]  WITH CHECK ADD  CONSTRAINT [chk_user_status] CHECK  (([account_status]='REMOVED' OR [account_status]='SUSPENDED' OR [account_status]='ACTIVE'))
GO

ALTER TABLE [dbo].[UserAccount] CHECK CONSTRAINT [chk_user_status]
GO

ALTER TABLE [dbo].[WalkInSlot]  WITH CHECK ADD  CONSTRAINT [chk_walk_in_slot_available] CHECK  (([is_available]='N' OR [is_available]='Y'))
GO

ALTER TABLE [dbo].[WalkInSlot] CHECK CONSTRAINT [chk_walk_in_slot_available]
GO

ALTER TABLE [dbo].[WalkInSlot]  WITH CHECK ADD  CONSTRAINT [chk_walk_in_slot_capacity] CHECK  (([slot_capacity]>=(1) AND [slot_capacity]<=(100)))
GO

ALTER TABLE [dbo].[WalkInSlot] CHECK CONSTRAINT [chk_walk_in_slot_capacity]
GO

ALTER TABLE [dbo].[StaffAccount]  WITH CHECK ADD  CONSTRAINT [chk_staff_role] CHECK  (([staff_role]='SYSTEM_ADMIN' OR [staff_role]='DIRECTOR' OR [staff_role]='CSR' OR [staff_role]='PASSPORT_OFFICER' OR [staff_role]='NIC_OFFICER'))
GO

ALTER TABLE [dbo].[StaffAccount] CHECK CONSTRAINT [chk_staff_role]
GO

ALTER TABLE [dbo].[StaffAccount]  WITH CHECK ADD  CONSTRAINT [chk_staff_status] CHECK  (([staff_status]='REMOVED' OR [staff_status]='SUSPENDED' OR [staff_status]='ACTIVE'))
GO

ALTER TABLE [dbo].[StaffAccount] CHECK CONSTRAINT [chk_staff_status]
GO

ALTER TABLE [dbo].[CitizenAccount]  WITH CHECK ADD  CONSTRAINT [chk_citizen_civil_status] CHECK  (([civil_status] IS NULL OR ([civil_status]='DIVORCED' OR [civil_status]='WIDOWED' OR [civil_status]='SINGLE' OR [civil_status]='MARRIED')))
GO

ALTER TABLE [dbo].[CitizenAccount] CHECK CONSTRAINT [chk_citizen_civil_status]
GO

ALTER TABLE [dbo].[CitizenAccount]  WITH CHECK ADD  CONSTRAINT [chk_citizen_gender] CHECK  (([gender] IS NULL OR ([gender]='FEMALE' OR [gender]='MALE')))
GO

ALTER TABLE [dbo].[CitizenAccount] CHECK CONSTRAINT [chk_citizen_gender]
GO

ALTER TABLE [dbo].[CitizenAccount]  WITH CHECK ADD  CONSTRAINT [chk_dual_citizen] CHECK  (([dual_citizen]='N' OR [dual_citizen]='Y'))
GO

ALTER TABLE [dbo].[CitizenAccount] CHECK CONSTRAINT [chk_dual_citizen]
GO

ALTER TABLE [dbo].[CitizenAccount]  WITH CHECK ADD  CONSTRAINT [chk_postal_same] CHECK  (([postal_same_as_permanent]='N' OR [postal_same_as_permanent]='Y'))
GO

ALTER TABLE [dbo].[CitizenAccount] CHECK CONSTRAINT [chk_postal_same]
GO

ALTER TABLE [dbo].[BiometricAppointmentSlot]  WITH CHECK ADD  CONSTRAINT [CK_BiometricAppointmentSlot_available] CHECK  (([is_available]='N' OR [is_available]='Y'))
GO

ALTER TABLE [dbo].[BiometricAppointmentSlot] CHECK CONSTRAINT [CK_BiometricAppointmentSlot_available]
GO

ALTER TABLE [dbo].[BiometricAppointmentSlot]  WITH CHECK ADD  CONSTRAINT [CK_BiometricAppointmentSlot_capacity] CHECK  (([slot_capacity]>=(1) AND [slot_capacity]<=(100)))
GO

ALTER TABLE [dbo].[BiometricAppointmentSlot] CHECK CONSTRAINT [CK_BiometricAppointmentSlot_capacity]
GO

ALTER TABLE [dbo].[BiometricAppointmentSlot]  WITH CHECK ADD  CONSTRAINT [CK_BiometricAppointmentSlot_service_type] CHECK  (([service_type]='PASSPORT' OR [service_type]='NIC'))
GO

ALTER TABLE [dbo].[BiometricAppointmentSlot] CHECK CONSTRAINT [CK_BiometricAppointmentSlot_service_type]
GO

ALTER TABLE [dbo].[Application]  WITH CHECK ADD  CONSTRAINT [chk_application_kind] CHECK  (([application_kind]='CHANGE' OR [application_kind]='DAMAGED' OR [application_kind]='LOST' OR [application_kind]='RENEWAL' OR [application_kind]='NEW'))
GO

ALTER TABLE [dbo].[Application] CHECK CONSTRAINT [chk_application_kind]
GO

ALTER TABLE [dbo].[Application]  WITH CHECK ADD  CONSTRAINT [chk_application_status] CHECK  (([current_status]='CANCELLED' OR [current_status]='REJECTED' OR [current_status]='ISSUED' OR [current_status]='PROCESSING' OR [current_status]='BIOMETRIC_COLLECTED' OR [current_status]='APPOINTMENT_BOOKED' OR [current_status]='PAYMENT_PENDING' OR [current_status]='APPROVED' OR [current_status]='RESUBMITTED' OR [current_status]='CORRECTION_REQUIRED' OR [current_status]='UNDER_REVIEW' OR [current_status]='SUBMITTED' OR [current_status]='DRAFT'))
GO

ALTER TABLE [dbo].[Application] CHECK CONSTRAINT [chk_application_status]
GO

ALTER TABLE [dbo].[Application]  WITH CHECK ADD  CONSTRAINT [chk_declaration] CHECK  (([declaration_accepted]='N' OR [declaration_accepted]='Y'))
GO

ALTER TABLE [dbo].[Application] CHECK CONSTRAINT [chk_declaration]
GO

ALTER TABLE [dbo].[Application]  WITH CHECK ADD  CONSTRAINT [chk_service_type] CHECK  (([service_type]='PASSPORT' OR [service_type]='NIC'))
GO

ALTER TABLE [dbo].[Application] CHECK CONSTRAINT [chk_service_type]
GO

ALTER TABLE [dbo].[SupportRequest]  WITH CHECK ADD  CONSTRAINT [chk_support_status] CHECK  (([request_status]='CLOSED' OR [request_status]='RESOLVED' OR [request_status]='IN_PROGRESS' OR [request_status]='OPEN'))
GO

ALTER TABLE [dbo].[SupportRequest] CHECK CONSTRAINT [chk_support_status]
GO

ALTER TABLE [dbo].[SupportRequest]  WITH CHECK ADD  CONSTRAINT [chk_support_type] CHECK  (([request_type]='CONTACT' OR [request_type]='COMPLAINT'))
GO

ALTER TABLE [dbo].[SupportRequest] CHECK CONSTRAINT [chk_support_type]
GO

ALTER TABLE [dbo].[StatusUpdate]  WITH CHECK ADD  CONSTRAINT [chk_status_update] CHECK  (([status]='CANCELLED' OR [status]='REJECTED' OR [status]='ISSUED' OR [status]='PROCESSING' OR [status]='BIOMETRIC_COLLECTED' OR [status]='APPOINTMENT_BOOKED' OR [status]='PAYMENT_PENDING' OR [status]='APPROVED' OR [status]='RESUBMITTED' OR [status]='CORRECTION_REQUIRED' OR [status]='UNDER_REVIEW' OR [status]='SUBMITTED' OR [status]='DRAFT'))
GO

ALTER TABLE [dbo].[StatusUpdate] CHECK CONSTRAINT [chk_status_update]
GO

ALTER TABLE [dbo].[DocumentDelivery]  WITH CHECK ADD  CONSTRAINT [chk_document_delivery_destination] CHECK  (([delivery_method]='HOME_DELIVERY' AND [delivery_address] IS NOT NULL AND [centre_id] IS NULL OR [delivery_method]='CENTRE_COLLECTION' AND [centre_id] IS NOT NULL AND [delivery_address] IS NULL))
GO

ALTER TABLE [dbo].[DocumentDelivery] CHECK CONSTRAINT [chk_document_delivery_destination]
GO

ALTER TABLE [dbo].[DocumentDelivery]  WITH CHECK ADD  CONSTRAINT [chk_document_delivery_method] CHECK  (([delivery_method]='CENTRE_COLLECTION' OR [delivery_method]='HOME_DELIVERY'))
GO

ALTER TABLE [dbo].[DocumentDelivery] CHECK CONSTRAINT [chk_document_delivery_method]
GO

ALTER TABLE [dbo].[DocumentDelivery]  WITH CHECK ADD  CONSTRAINT [chk_document_delivery_status] CHECK  (([delivery_status]='CANCELLED' OR [delivery_status]='COLLECTED' OR [delivery_status]='DELIVERED' OR [delivery_status]='READY_FOR_COLLECTION' OR [delivery_status]='DISPATCHED' OR [delivery_status]='PREPARING'))
GO

ALTER TABLE [dbo].[DocumentDelivery] CHECK CONSTRAINT [chk_document_delivery_status]
GO

ALTER TABLE [dbo].[PassportApplication]  WITH CHECK ADD  CONSTRAINT [chk_passport_service] CHECK  (([service_level]='NORMAL'))
GO

ALTER TABLE [dbo].[PassportApplication] CHECK CONSTRAINT [chk_passport_service]
GO

ALTER TABLE [dbo].[PassportApplication]  WITH CHECK ADD  CONSTRAINT [chk_travel_document] CHECK  (([travel_document_type]='ALL_COUNTRIES'))
GO

ALTER TABLE [dbo].[PassportApplication] CHECK CONSTRAINT [chk_travel_document]
GO

ALTER TABLE [dbo].[Appointment]  WITH CHECK ADD  CONSTRAINT [chk_appointment_status] CHECK  (([appointment_status]='MISSED' OR [appointment_status]='CANCELLED' OR [appointment_status]='COMPLETED' OR [appointment_status]='CHECKED_IN' OR [appointment_status]='CONFIRMED' OR [appointment_status]='PENDING_PAYMENT'))
GO

ALTER TABLE [dbo].[Appointment] CHECK CONSTRAINT [chk_appointment_status]
GO

ALTER TABLE [dbo].[Appointment]  WITH CHECK ADD  CONSTRAINT [chk_appointment_type] CHECK  (([appointment_type]='WALK_IN' OR [appointment_type]='BIOMETRIC'))
GO

ALTER TABLE [dbo].[Appointment] CHECK CONSTRAINT [chk_appointment_type]
GO

ALTER TABLE [dbo].[ApplicationReview]  WITH CHECK ADD  CONSTRAINT [chk_review_decision] CHECK  (([decision]='REJECTED' OR [decision]='CORRECTION_REQUIRED' OR [decision]='APPROVED'))
GO

ALTER TABLE [dbo].[ApplicationReview] CHECK CONSTRAINT [chk_review_decision]
GO

ALTER TABLE [dbo].[ApplicationReview]  WITH CHECK ADD  CONSTRAINT [chk_review_sequence] CHECK  (([review_sequence]>(0)))
GO

ALTER TABLE [dbo].[ApplicationReview] CHECK CONSTRAINT [chk_review_sequence]
GO

ALTER TABLE [dbo].[ApplicationDocument]  WITH CHECK ADD  CONSTRAINT [chk_document_status] CHECK  (([verification_status]='REJECTED' OR [verification_status]='VERIFIED' OR [verification_status]='PENDING'))
GO

ALTER TABLE [dbo].[ApplicationDocument] CHECK CONSTRAINT [chk_document_status]
GO

ALTER TABLE [dbo].[ApplicationDocument]  WITH CHECK ADD  CONSTRAINT [chk_document_type] CHECK  (([document_type]='OTHER' OR [document_type]='CITIZENSHIP_CERTIFICATE' OR [document_type]='NIC_PROOF' OR [document_type]='POLICE_REPORT' OR [document_type]='BIRTH_CERTIFICATE' OR [document_type]='PHOTOGRAPH'))
GO

ALTER TABLE [dbo].[ApplicationDocument] CHECK CONSTRAINT [chk_document_type]
GO

ALTER TABLE [dbo].[BiometricCollection]  WITH CHECK ADD  CONSTRAINT [chk_biometric_status] CHECK  (([collection_status]='FAILED' OR [collection_status]='COLLECTED' OR [collection_status]='PENDING'))
GO

ALTER TABLE [dbo].[BiometricCollection] CHECK CONSTRAINT [chk_biometric_status]
GO

ALTER TABLE [dbo].[Payment]  WITH CHECK ADD  CONSTRAINT [chk_payment_amount] CHECK  (([amount]>(0)))
GO

ALTER TABLE [dbo].[Payment] CHECK CONSTRAINT [chk_payment_amount]
GO

ALTER TABLE [dbo].[Payment]  WITH CHECK ADD  CONSTRAINT [chk_payment_status] CHECK  (([payment_status]='REFUNDED' OR [payment_status]='FAILED' OR [payment_status]='PAID' OR [payment_status]='PENDING'))
GO

ALTER TABLE [dbo].[Payment] CHECK CONSTRAINT [chk_payment_status]
GO

ALTER TABLE [dbo].[AppointmentCheckIn]  WITH CHECK ADD  CONSTRAINT [chk_check_in_method] CHECK  (([check_in_method]='REFERENCE_NUMBER' OR [check_in_method]='QR'))
GO

ALTER TABLE [dbo].[AppointmentCheckIn] CHECK CONSTRAINT [chk_check_in_method]
GO

ALTER TABLE [dbo].[Notification]  WITH CHECK ADD  CONSTRAINT [chk_notification_broadcast] CHECK  (([is_broadcast]='N' OR [is_broadcast]='Y'))
GO

ALTER TABLE [dbo].[Notification] CHECK CONSTRAINT [chk_notification_broadcast]
GO

PRINT 'LankaID database schema created successfully.';
GO
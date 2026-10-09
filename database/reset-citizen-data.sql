USE identity_issuance_service;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @staff_count_before INT = (SELECT COUNT(*) FROM dbo.StaffAccount);
    DECLARE @staff_user_count_before INT = (
        SELECT COUNT(DISTINCT ua.user_id)
        FROM dbo.UserAccount ua
        INNER JOIN dbo.StaffAccount sa ON sa.user_id = ua.user_id
    );

    IF @staff_count_before <> @staff_user_count_before
        THROW 51000, 'Cleanup stopped: one or more staff accounts do not have a valid user account.', 1;

    /* Notifications must be removed before their status-update sources. */
    DELETE FROM dbo.UserNotification;
    DELETE FROM dbo.Notification;

    /* Appointment and walk-in transaction history. Reusable slot schedules remain. */
    DELETE FROM dbo.AppointmentChangePayment;
    DELETE FROM dbo.Payment;
    DELETE FROM dbo.AppointmentCheckIn;
    DELETE FROM dbo.Appointment;

    /* Application verification, approval, printing, and delivery history. */
    DELETE FROM dbo.BiometricCollection;
    DELETE FROM dbo.DirectorReviewReferral;
    DELETE FROM dbo.ApplicationDocument;
    DELETE FROM dbo.ApplicationReview;
    DELETE FROM dbo.DocumentDelivery;
    DELETE FROM dbo.NICApplication;
    DELETE FROM dbo.PassportApplication;
    DELETE FROM dbo.StatusUpdate;
    DELETE FROM dbo.Application;

    /* Citizen support, account, and government identity information. */
    DELETE FROM dbo.SupportRequest;
    DELETE FROM dbo.CitizenAccount;
    DELETE FROM dbo.GovernmentIdentityRecord;

    /* Preserve every UserAccount used by an officer or administrator. */
    DELETE ua
    FROM dbo.UserAccount ua
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.StaffAccount sa
        WHERE sa.user_id = ua.user_id
    );

    IF (SELECT COUNT(*) FROM dbo.StaffAccount) <> @staff_count_before
        THROW 51001, 'Cleanup stopped: the staff account count changed.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.StaffAccount sa
        LEFT JOIN dbo.UserAccount ua ON ua.user_id = sa.user_id
        WHERE ua.user_id IS NULL
    )
        THROW 51002, 'Cleanup stopped: an officer or administrator user account would be missing.', 1;

    IF EXISTS (SELECT 1 FROM dbo.CitizenAccount)
       OR EXISTS (SELECT 1 FROM dbo.GovernmentIdentityRecord)
       OR EXISTS (SELECT 1 FROM dbo.Application)
       OR EXISTS (SELECT 1 FROM dbo.Appointment)
       OR EXISTS (SELECT 1 FROM dbo.SupportRequest)
       OR EXISTS (
            SELECT 1
            FROM dbo.UserAccount ua
            WHERE NOT EXISTS (
                SELECT 1 FROM dbo.StaffAccount sa WHERE sa.user_id = ua.user_id
            )
       )
        THROW 51003, 'Cleanup stopped: citizen or guest data remains.', 1;

    /* Restart identifiers for the emptied operational tables. */
    DBCC CHECKIDENT ('dbo.Application', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.SupportRequest', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.StatusUpdate', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.DocumentDelivery', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.Appointment', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.ApplicationReview', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.DirectorReviewReferral', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.ApplicationDocument', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.Payment', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.AppointmentChangePayment', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.Notification', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.GovernmentIdentityRecord', RESEED, 0) WITH NO_INFOMSGS;

    COMMIT TRANSACTION;

    SELECT
        (SELECT COUNT(*) FROM dbo.StaffAccount) AS staff_accounts_kept,
        (SELECT COUNT(*) FROM dbo.UserAccount) AS staff_user_accounts_kept,
        (SELECT COUNT(*) FROM dbo.CitizenAccount) AS citizen_accounts_remaining,
        (SELECT COUNT(*) FROM dbo.GovernmentIdentityRecord) AS government_records_remaining,
        (SELECT COUNT(*) FROM dbo.Application) AS applications_remaining,
        (SELECT COUNT(*) FROM dbo.Appointment) AS appointments_remaining,
        (SELECT COUNT(*) FROM dbo.SupportRequest) AS support_requests_remaining;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

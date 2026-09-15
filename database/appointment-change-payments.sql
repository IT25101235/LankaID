-- Run once against identity_issuance_service before starting the updated app.
-- Original payments remain unchanged; every successful booking change has its own receipt.
IF COL_LENGTH('dbo.Appointment', 'booking_version') IS NULL
    ALTER TABLE dbo.Appointment ADD booking_version BIGINT NOT NULL CONSTRAINT DF_Appointment_booking_version DEFAULT 0;

IF OBJECT_ID(N'dbo.AppointmentChangePayment', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppointmentChangePayment (
        change_payment_id INT IDENTITY(1,1) PRIMARY KEY,
        appointment_id INT NOT NULL REFERENCES dbo.Appointment(appointment_id),
        request_token VARCHAR(36) NOT NULL UNIQUE,
        change_type VARCHAR(20) NOT NULL,
        amount DECIMAL(19,2) NOT NULL,
        previous_scheduled_at DATETIME2 NOT NULL,
        new_scheduled_at DATETIME2 NOT NULL,
        paid_at DATETIME2 NOT NULL
    );
END;

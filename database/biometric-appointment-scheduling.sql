-- Run once against identity_issuance_service before starting this version of the app.
-- Adds officer-published, service-specific biometric appointment slots.

IF OBJECT_ID(N'dbo.BiometricAppointmentSlot', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BiometricAppointmentSlot (
        slot_id INT IDENTITY(1,1) PRIMARY KEY,
        centre_id INT NOT NULL REFERENCES dbo.Centre(centre_id),
        service_type VARCHAR(20) NOT NULL,
        slot_date DATE NOT NULL,
        slot_time TIME(0) NOT NULL,
        slot_capacity INT NOT NULL,
        is_available CHAR(1) NOT NULL,
        created_by INT NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_BiometricAppointmentSlot_created_at DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_BiometricAppointmentSlot_updated_at DEFAULT SYSDATETIME(),
        CONSTRAINT CK_BiometricAppointmentSlot_service_type CHECK (service_type IN ('NIC', 'PASSPORT')),
        CONSTRAINT CK_BiometricAppointmentSlot_capacity CHECK (slot_capacity BETWEEN 1 AND 100),
        CONSTRAINT CK_BiometricAppointmentSlot_available CHECK (is_available IN ('Y', 'N')),
        CONSTRAINT UQ_BiometricAppointmentSlot UNIQUE (centre_id, service_type, slot_date, slot_time)
    );
END;

IF COL_LENGTH('dbo.Appointment', 'biometric_slot_id') IS NULL
    ALTER TABLE dbo.Appointment ADD biometric_slot_id INT NULL;

-- Preserve existing future biometric bookings by publishing matching service slots.
-- created_by = 0 identifies slots produced by this migration rather than an officer action.
INSERT INTO dbo.BiometricAppointmentSlot
    (centre_id, service_type, slot_date, slot_time, slot_capacity, is_available, created_by, created_at, updated_at)
SELECT
    appointment.centre_id,
    application.service_type,
    CAST(appointment.scheduled_at AS DATE),
    CAST(appointment.scheduled_at AS TIME(0)),
    COUNT(*),
    'Y',
    0,
    SYSDATETIME(),
    SYSDATETIME()
FROM dbo.Appointment appointment
JOIN dbo.Application application ON application.application_id = appointment.application_id
WHERE appointment.appointment_type = 'BIOMETRIC'
  AND appointment.appointment_status IN ('CONFIRMED', 'CHECKED_IN', 'COMPLETED')
  AND appointment.scheduled_at >= CAST(GETDATE() AS DATE)
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.BiometricAppointmentSlot slot
      WHERE slot.centre_id = appointment.centre_id
        AND slot.service_type = application.service_type
        AND slot.slot_date = CAST(appointment.scheduled_at AS DATE)
        AND slot.slot_time = CAST(appointment.scheduled_at AS TIME(0))
  )
GROUP BY appointment.centre_id, application.service_type,
         CAST(appointment.scheduled_at AS DATE), CAST(appointment.scheduled_at AS TIME(0));

-- Link existing biometric appointments to the matching migrated slot.
UPDATE appointment
SET biometric_slot_id = slot.slot_id
FROM dbo.Appointment appointment
JOIN dbo.Application application ON application.application_id = appointment.application_id
JOIN dbo.BiometricAppointmentSlot slot
  ON slot.centre_id = appointment.centre_id
 AND slot.service_type = application.service_type
 AND slot.slot_date = CAST(appointment.scheduled_at AS DATE)
 AND slot.slot_time = CAST(appointment.scheduled_at AS TIME(0))
WHERE appointment.appointment_type = 'BIOMETRIC'
  AND appointment.biometric_slot_id IS NULL;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Appointment_BiometricAppointmentSlot')
    ALTER TABLE dbo.Appointment
        ADD CONSTRAINT FK_Appointment_BiometricAppointmentSlot
        FOREIGN KEY (biometric_slot_id) REFERENCES dbo.BiometricAppointmentSlot(slot_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Appointment_biometric_slot_status')
    CREATE INDEX IX_Appointment_biometric_slot_status
        ON dbo.Appointment(biometric_slot_id, appointment_status);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_BiometricAppointmentSlot_calendar')
    CREATE INDEX IX_BiometricAppointmentSlot_calendar
        ON dbo.BiometricAppointmentSlot(service_type, centre_id, slot_date, is_available);

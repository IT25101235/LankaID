-- Run once against identity_issuance_service before starting this version.
-- Stores the single system-wide maintenance mode setting.

IF OBJECT_ID('dbo.SystemMaintenance', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.SystemMaintenance (
        maintenance_id INT NOT NULL CONSTRAINT pk_system_maintenance PRIMARY KEY,
        maintenance_active BIT NOT NULL
            CONSTRAINT df_system_maintenance_active DEFAULT 0,
        maintenance_message VARCHAR(300) NOT NULL
            CONSTRAINT df_system_maintenance_message DEFAULT
            'LankaID is temporarily unavailable for scheduled system maintenance. Please try again later.',
        updated_at DATETIME2 NULL
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.SystemMaintenance WHERE maintenance_id = 1)
BEGIN
    INSERT INTO dbo.SystemMaintenance (
        maintenance_id,
        maintenance_active,
        maintenance_message
    ) VALUES (
        1,
        0,
        'LankaID is temporarily unavailable for scheduled system maintenance. Please try again later.'
    );
END;

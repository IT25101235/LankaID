-- Run once against identity_issuance_service before starting this version.
-- Stores the two administrator-controlled application service fees.

IF OBJECT_ID('dbo.ApplicationFeeSettings', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ApplicationFeeSettings (
        settings_id INT NOT NULL CONSTRAINT pk_application_fee_settings PRIMARY KEY,
        nic_fee DECIMAL(12, 2) NOT NULL,
        passport_fee DECIMAL(12, 2) NOT NULL,
        updated_at DATETIME2 NULL,
        CONSTRAINT chk_application_fee_settings_nic CHECK (nic_fee > 0),
        CONSTRAINT chk_application_fee_settings_passport CHECK (passport_fee > 0)
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.ApplicationFeeSettings WHERE settings_id = 1)
BEGIN
    INSERT INTO dbo.ApplicationFeeSettings (settings_id, nic_fee, passport_fee)
    VALUES (1, 2000.00, 10000.00);
END;

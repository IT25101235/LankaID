-- Run once against identity_issuance_service before starting this version of the app.
-- Adds the status used to activate or deactivate service centres.

IF COL_LENGTH('dbo.Centre', 'centre_status') IS NULL
BEGIN
    EXEC('ALTER TABLE dbo.Centre
        ADD centre_status VARCHAR(20) NOT NULL
            CONSTRAINT df_centre_status DEFAULT ''ACTIVE''');
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints WHERE name = 'chk_centre_status'
)
BEGIN
    EXEC('ALTER TABLE dbo.Centre WITH CHECK
        ADD CONSTRAINT chk_centre_status
        CHECK (centre_status IN (''ACTIVE'', ''INACTIVE''))');
END;

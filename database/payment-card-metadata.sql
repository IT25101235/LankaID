-- Run once against identity_issuance_service before starting the updated app.
-- Only masked card metadata is stored. Full card numbers and CVVs are never persisted.

IF COL_LENGTH('dbo.Payment', 'payment_method') IS NULL
    ALTER TABLE dbo.Payment ADD payment_method VARCHAR(20) NULL;

IF COL_LENGTH('dbo.Payment', 'card_brand') IS NULL
    ALTER TABLE dbo.Payment ADD card_brand VARCHAR(20) NULL;

IF COL_LENGTH('dbo.Payment', 'card_last_four') IS NULL
    ALTER TABLE dbo.Payment ADD card_last_four VARCHAR(4) NULL;

IF COL_LENGTH('dbo.Payment', 'cardholder_name') IS NULL
    ALTER TABLE dbo.Payment ADD cardholder_name VARCHAR(150) NULL;

IF COL_LENGTH('dbo.Payment', 'card_expiry') IS NULL
    ALTER TABLE dbo.Payment ADD card_expiry VARCHAR(5) NULL;

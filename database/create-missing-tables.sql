-- Run this script against the identity_issuance_service database.
-- It creates missing tables and adds missing columns only.
-- Existing columns and their data are not changed.

SET NOCOUNT ON;

/* =========================================================
   Centre
   ========================================================= */
IF OBJECT_ID(N'dbo.Centre', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Centre (
        centre_id INT IDENTITY(1,1) NOT NULL
            CONSTRAINT pk_centre PRIMARY KEY,
        centre_name VARCHAR(100) NOT NULL,
        street VARCHAR(150) NULL,
        city VARCHAR(100) NOT NULL,
        district VARCHAR(100) NOT NULL,
        daily_capacity INT NOT NULL,
        centre_status VARCHAR(20) NOT NULL
            CONSTRAINT df_centre_status DEFAULT 'ACTIVE'
    );

    PRINT 'Created dbo.Centre.';
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.Centre', N'centre_id') IS NULL
    BEGIN
        ALTER TABLE dbo.Centre
            ADD centre_id INT IDENTITY(1,1) NOT NULL;
        PRINT 'Added Centre.centre_id.';
    END;

    IF COL_LENGTH(N'dbo.Centre', N'centre_name') IS NULL
    BEGIN
        ALTER TABLE dbo.Centre
            ADD centre_name VARCHAR(100) NOT NULL
                CONSTRAINT df_centre_name DEFAULT 'Unnamed Centre';
        PRINT 'Added Centre.centre_name.';
    END;

    IF COL_LENGTH(N'dbo.Centre', N'street') IS NULL
    BEGIN
        ALTER TABLE dbo.Centre ADD street VARCHAR(150) NULL;
        PRINT 'Added Centre.street.';
    END;

    IF COL_LENGTH(N'dbo.Centre', N'city') IS NULL
    BEGIN
        ALTER TABLE dbo.Centre
            ADD city VARCHAR(100) NOT NULL
                CONSTRAINT df_centre_city DEFAULT 'Unknown';
        PRINT 'Added Centre.city.';
    END;

    IF COL_LENGTH(N'dbo.Centre', N'district') IS NULL
    BEGIN
        ALTER TABLE dbo.Centre
            ADD district VARCHAR(100) NOT NULL
                CONSTRAINT df_centre_district DEFAULT 'Unknown';
        PRINT 'Added Centre.district.';
    END;

    IF COL_LENGTH(N'dbo.Centre', N'daily_capacity') IS NULL
    BEGIN
        ALTER TABLE dbo.Centre
            ADD daily_capacity INT NOT NULL
                CONSTRAINT df_centre_daily_capacity DEFAULT 100;
        PRINT 'Added Centre.daily_capacity.';
    END;

    IF COL_LENGTH(N'dbo.Centre', N'centre_status') IS NULL
    BEGIN
        ALTER TABLE dbo.Centre
            ADD centre_status VARCHAR(20) NOT NULL
                CONSTRAINT df_centre_status DEFAULT 'ACTIVE';
        PRINT 'Added Centre.centre_status.';
    END;

    PRINT 'Checked dbo.Centre. Existing columns were not changed.';
END;

/* =========================================================
   System maintenance
   ========================================================= */
IF OBJECT_ID(N'dbo.SystemMaintenance', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SystemMaintenance (
        maintenance_id INT NOT NULL
            CONSTRAINT pk_system_maintenance PRIMARY KEY,
        maintenance_active BIT NOT NULL
            CONSTRAINT df_system_maintenance_active DEFAULT 0,
        maintenance_message VARCHAR(300) NOT NULL
            CONSTRAINT df_system_maintenance_message DEFAULT
            'LankaID is temporarily unavailable for scheduled system maintenance. Please try again later.',
        updated_at DATETIME2 NULL
    );

    INSERT INTO dbo.SystemMaintenance (
        maintenance_id,
        maintenance_active,
        maintenance_message
    ) VALUES (
        1,
        0,
        'LankaID is temporarily unavailable for scheduled system maintenance. Please try again later.'
    );

    PRINT 'Created dbo.SystemMaintenance.';
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.SystemMaintenance', N'maintenance_id') IS NULL
    BEGIN
        ALTER TABLE dbo.SystemMaintenance
            ADD maintenance_id INT IDENTITY(1,1) NOT NULL;
        PRINT 'Added SystemMaintenance.maintenance_id.';
    END;

    IF COL_LENGTH(N'dbo.SystemMaintenance', N'maintenance_active') IS NULL
    BEGIN
        ALTER TABLE dbo.SystemMaintenance
            ADD maintenance_active BIT NOT NULL
                CONSTRAINT df_system_maintenance_active DEFAULT 0;
        PRINT 'Added SystemMaintenance.maintenance_active.';
    END;

    IF COL_LENGTH(N'dbo.SystemMaintenance', N'maintenance_message') IS NULL
    BEGIN
        ALTER TABLE dbo.SystemMaintenance
            ADD maintenance_message VARCHAR(300) NOT NULL
                CONSTRAINT df_system_maintenance_message DEFAULT
                'LankaID is temporarily unavailable for scheduled system maintenance. Please try again later.';
        PRINT 'Added SystemMaintenance.maintenance_message.';
    END;

    IF COL_LENGTH(N'dbo.SystemMaintenance', N'updated_at') IS NULL
    BEGIN
        ALTER TABLE dbo.SystemMaintenance ADD updated_at DATETIME2 NULL;
        PRINT 'Added SystemMaintenance.updated_at.';
    END;

    PRINT 'Checked dbo.SystemMaintenance. Existing columns were not changed.';
END;

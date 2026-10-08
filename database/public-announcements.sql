-- Run once against identity_issuance_service before starting this version.
-- Stores the public notice shown directly below the LankaID landing-page header.

IF OBJECT_ID('dbo.PublicAnnouncement', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.PublicAnnouncement (
        announcement_id INT IDENTITY(1,1) NOT NULL
            CONSTRAINT pk_public_announcement PRIMARY KEY,
        title NVARCHAR(140) NOT NULL,
        message NVARCHAR(1200) NOT NULL,
        announcement_type VARCHAR(30) NOT NULL,
        source_label NVARCHAR(80) NULL,
        source_url VARCHAR(500) NULL,
        starts_at DATETIME2 NOT NULL,
        expires_at DATETIME2 NULL,
        is_active CHAR(1) NOT NULL
            CONSTRAINT df_public_announcement_active DEFAULT 'Y',
        created_by_staff_id INT NULL,
        created_at DATETIME2 NOT NULL
            CONSTRAINT df_public_announcement_created DEFAULT SYSDATETIME(),
        CONSTRAINT chk_public_announcement_type CHECK (
            announcement_type IN ('IMPORTANT', 'PRESIDENTIAL', 'EMERGENCY', 'SERVICE_UPDATE')
        ),
        CONSTRAINT chk_public_announcement_active CHECK (is_active IN ('Y', 'N')),
        CONSTRAINT chk_public_announcement_dates CHECK (
            expires_at IS NULL OR expires_at > starts_at
        )
    );

    CREATE INDEX ix_public_announcement_display
        ON dbo.PublicAnnouncement (is_active, starts_at, expires_at, created_at DESC);
END;

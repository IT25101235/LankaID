/*
  Run once on an existing LankaID database before starting the updated application.
  The script is idempotent and preserves every referral for audit purposes.
*/
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'dbo.DirectorReviewReferral', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DirectorReviewReferral (
        referral_id INT IDENTITY(1,1) NOT NULL,
        application_id INT NOT NULL,
        referred_by_staff_id INT NOT NULL,
        referral_category VARCHAR(30) NOT NULL,
        referral_reason VARCHAR(1000) NOT NULL,
        referral_status VARCHAR(20) NOT NULL CONSTRAINT df_director_referral_status DEFAULT ('PENDING'),
        created_at DATETIME2 NOT NULL CONSTRAINT df_director_referral_created DEFAULT (SYSDATETIME()),
        director_id INT NULL,
        recommendation VARCHAR(30) NULL,
        director_comment VARCHAR(1000) NULL,
        responded_at DATETIME2 NULL,
        withdrawn_by_staff_id INT NULL,
        withdrawal_reason VARCHAR(500) NULL,
        withdrawn_at DATETIME2 NULL,
        CONSTRAINT pk_director_review_referral PRIMARY KEY (referral_id),
        CONSTRAINT fk_director_referral_application FOREIGN KEY (application_id)
            REFERENCES dbo.Application(application_id),
        CONSTRAINT fk_director_referral_referrer FOREIGN KEY (referred_by_staff_id)
            REFERENCES dbo.StaffAccount(user_id),
        CONSTRAINT fk_director_referral_director FOREIGN KEY (director_id)
            REFERENCES dbo.StaffAccount(user_id),
        CONSTRAINT fk_director_referral_withdrawer FOREIGN KEY (withdrawn_by_staff_id)
            REFERENCES dbo.StaffAccount(user_id),
        CONSTRAINT chk_director_referral_category CHECK (referral_category IN
            ('IDENTITY_CONFLICT', 'DOCUMENT_CONCERN', 'POSSIBLE_DUPLICATE', 'POLICY_EXCEPTION', 'OTHER')),
        CONSTRAINT chk_director_referral_status CHECK (referral_status IN ('PENDING', 'RESPONDED', 'WITHDRAWN')),
        CONSTRAINT chk_director_referral_recommendation CHECK (recommendation IS NULL OR recommendation IN
            ('APPROVE', 'REJECT', 'REQUEST_CORRECTION')),
        CONSTRAINT chk_director_referral_state CHECK (
            (referral_status = 'PENDING' AND director_id IS NULL AND recommendation IS NULL
                AND responded_at IS NULL AND withdrawn_at IS NULL)
            OR
            (referral_status = 'RESPONDED' AND director_id IS NOT NULL AND recommendation IS NOT NULL
                AND director_comment IS NOT NULL AND responded_at IS NOT NULL AND withdrawn_at IS NULL)
            OR
            (referral_status = 'WITHDRAWN' AND withdrawn_by_staff_id IS NOT NULL
                AND withdrawal_reason IS NOT NULL AND withdrawn_at IS NOT NULL AND responded_at IS NULL)
        )
    );

END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = N'ux_director_referral_pending_application'
                 AND object_id = OBJECT_ID(N'dbo.DirectorReviewReferral'))
BEGIN
    CREATE UNIQUE INDEX ux_director_referral_pending_application
        ON dbo.DirectorReviewReferral(application_id)
        WHERE referral_status = 'PENDING';
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = N'ix_director_referral_queue'
                 AND object_id = OBJECT_ID(N'dbo.DirectorReviewReferral'))
BEGIN
    CREATE INDEX ix_director_referral_queue
        ON dbo.DirectorReviewReferral(referral_status, created_at, referral_id);
END;
GO

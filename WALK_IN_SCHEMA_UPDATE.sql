USE identity_issuance_service;
GO

IF COL_LENGTH('dbo.Appointment', 'visitor_name') IS NULL
BEGIN
    ALTER TABLE dbo.Appointment
    ADD visitor_name VARCHAR(100) NULL;
END;
GO

IF COL_LENGTH('dbo.Appointment', 'visitor_service') IS NULL
BEGIN
    ALTER TABLE dbo.Appointment
    ADD visitor_service VARCHAR(50) NULL;
END;
GO

IF OBJECT_ID('dbo.WalkInSlot', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.WalkInSlot
    (
        slot_id INT IDENTITY(1,1) PRIMARY KEY,
        centre_id INT NOT NULL,
        slot_date DATE NOT NULL,
        slot_time TIME(0) NOT NULL,
        slot_capacity INT NOT NULL,
        is_available CHAR(1) NOT NULL
            CONSTRAINT df_walk_in_slot_available DEFAULT 'Y',

        CONSTRAINT fk_walk_in_slot_centre
            FOREIGN KEY (centre_id) REFERENCES dbo.Centre(centre_id),
        CONSTRAINT uq_walk_in_slot
            UNIQUE (centre_id, slot_date, slot_time),
        CONSTRAINT chk_walk_in_slot_capacity
            CHECK (slot_capacity BETWEEN 1 AND 100),
        CONSTRAINT chk_walk_in_slot_available
            CHECK (is_available IN ('Y', 'N'))
    );
END;
GO

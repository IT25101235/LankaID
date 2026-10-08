/*
  LankaID - authoritative NIC issue date
  Run this once in SQL Server Management Studio against the LankaID database.

  The value belongs to the verified government identity record. It must not be
  populated from an unverified lost-card application. Existing records remain
  valid because the new column is nullable.
*/

IF COL_LENGTH('dbo.GovernmentIdentityRecord', 'nic_issued_date') IS NULL
BEGIN
    ALTER TABLE dbo.GovernmentIdentityRecord
        ADD nic_issued_date DATE NULL;
END;
GO

/*
  After deployment, an authorised data administrator can backfill verified
  dates from the official NIC source. Example (do not run with sample values):

  UPDATE dbo.GovernmentIdentityRecord
  SET nic_issued_date = '2020-01-15'
  WHERE nic_no = '200012345678';
*/

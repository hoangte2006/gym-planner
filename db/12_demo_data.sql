/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   Script 12: demo data - move the sample dates so they are "around today"

   WHY
   The sample data (script 04) has fixed dates (2026-10-05 .. 2026-11-01).
   The views and the dashboard work with GETDATE(), so on the day of the
   presentation the "expiring memberships", "classes today" and "overdue
   invoices" screens would be empty. This script shifts ALL sample dates by
   the same number of days, so the story in the sample data stays the same
   but starts from today.

   HOW
   - Anchor: Session 1 (Yoga Flow, 07:00) is moved to TODAY. Every other
     sample date is moved by the same number of days.
   - Only the sample rows are touched (IDs of script 04): visitors 1-8,
     memberships 1-6, bills 1-8, sessions 1-8, maintenance logs (1,1) (4,1)
     (5,1). Rows added later (script 06 bulk data, rows created by the app)
     are NOT changed.
   - Safe to run again: it always re-anchors Session 1 to the current day,
     and does nothing when the data is already aligned with today.
   - All triggers stay ON. The equipment reservations of the sample sessions
     are removed and re-inserted (same Usage_ID) because triggers
     trg_SEU_TimeCheck and trg_SESSION_ReservationsInside check each other.
   - All-or-nothing: on any error the whole script is rolled back.

   RUN
   Run on the sample data, after scripts 01 - 04, 02, 03, 08 (and 10, 11).
   If the bulk data (script 06) is loaded and a shifted session collides with
   a bulk session in the same room or trainer slot, the trigger blocks the
   change; run 06b_remove_bulk_data.sql and run this script again.

   WHAT YOU GET (with Session 1 = today)
   - Classes today: Yoga Flow 07:00, HIIT Burn 18:00 (3 of 3 seats taken),
     PT at 09:00; tomorrow: Yoga Flow and a PT; day after: Spinning.
   - Memberships ending in 3, 8 and 20 days (member 4, 3, 2): fills both
     the "7 days" and the "30 days" expiry lists.
   - Overdue bills: member 3 (Pending) and member 5 (Failed).
   - Equipment: one Under Maintenance, one Broken.
   - Member 6 has no photo and no membership (rule BR-MEM-03 demo).
   ===================================================================== */

USE GymPlanner;
GO

SET DATEFORMAT ymd;
SET XACT_ABORT ON;
-- MEMBERSHIP has a filtered unique index (BR-MEM-10): updates on it need these two ON.
-- SSMS has them ON already; the command-line tool sqlcmd has QUOTED_IDENTIFIER OFF by default.
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

DECLARE @Today  DATE = CAST(GETDATE() AS DATE);
DECLARE @Anchor DATE;
DECLARE @Shift  INT;

-- 0) The sample rows must be there (script 04 loaded on empty tables)
IF NOT EXISTS (SELECT 1
               FROM dbo.[SESSION] s
               JOIN dbo.CLASS c ON c.Session_ID = s.Session_ID
               WHERE s.Session_ID = 1 AND c.Class_Name = 'Yoga Flow')
    THROW 51001, 'Sample data not found: Session 1 (Yoga Flow) is missing. Run script 04 on empty tables first.', 1;

SELECT @Anchor = CAST(Start_Time AS DATE) FROM dbo.[SESSION] WHERE Session_ID = 1;
SET @Shift = DATEDIFF(DAY, @Anchor, @Today);

IF @Shift = 0
BEGIN
    PRINT 'Sample dates are already aligned with today. Nothing to do.';
END
ELSE
BEGIN
    PRINT 'Moving sample dates by ' + CAST(@Shift AS VARCHAR(10)) + ' day(s): '
          + CONVERT(VARCHAR(10), @Anchor, 120) + ' -> ' + CONVERT(VARCHAR(10), @Today, 120);

    BEGIN TRY
        BEGIN TRANSACTION;

        -- 1) Set the equipment reservations of sample sessions aside (no trigger on DELETE)
        IF OBJECT_ID('tempdb..#usage') IS NOT NULL DROP TABLE #usage;
        SELECT Usage_ID, Equipment_ID, Session_ID, Reserved_From, Reserved_To
        INTO #usage
        FROM dbo.SESSION_EQUIPMENT_USAGE
        WHERE Session_ID BETWEEN 1 AND 8;

        DELETE FROM dbo.SESSION_EQUIPMENT_USAGE WHERE Session_ID BETWEEN 1 AND 8;

        -- 2) Move the 8 sample sessions with ONE statement (the overlap triggers
        --    look at the final state, so no false conflicts between them)
        UPDATE dbo.[SESSION]
        SET Start_Time = DATEADD(DAY, @Shift, Start_Time),
            End_Time   = DATEADD(DAY, @Shift, End_Time)
        WHERE Session_ID BETWEEN 1 AND 8;

        -- 3) Put the reservations back, same IDs, same offset inside the session
        SET IDENTITY_INSERT dbo.SESSION_EQUIPMENT_USAGE ON;
        INSERT INTO dbo.SESSION_EQUIPMENT_USAGE (Usage_ID, Equipment_ID, Session_ID, Reserved_From, Reserved_To)
        SELECT Usage_ID, Equipment_ID, Session_ID,
               DATEADD(DAY, @Shift, Reserved_From), DATEADD(DAY, @Shift, Reserved_To)
        FROM #usage;
        SET IDENTITY_INSERT dbo.SESSION_EQUIPMENT_USAGE OFF;

        DROP TABLE #usage;

        -- 4) Class registrations (the sessions 1-8 are the sample sessions)
        UPDATE dbo.CLASS_REGISTRATION
        SET Registration_Date = DATEADD(DAY, @Shift, Registration_Date)
        WHERE Session_ID BETWEEN 1 AND 8;

        -- 5) Memberships and bills (sample IDs only)
        UPDATE dbo.MEMBERSHIP
        SET Start_Date = DATEADD(DAY, @Shift, Start_Date),
            End_Date   = DATEADD(DAY, @Shift, End_Date)
        WHERE Membership_ID BETWEEN 1 AND 6;

        UPDATE dbo.BILLING_LOG
        SET Invoice_Date = DATEADD(DAY, @Shift, Invoice_Date),
            Due_Date     = DATEADD(DAY, @Shift, Due_Date),
            Payment_Date = DATEADD(DAY, @Shift, Payment_Date)      -- NULL stays NULL
        WHERE Bill_ID BETWEEN 1 AND 8;

        -- 6) Members, daily passes, maintenance logs (sample IDs only)
        UPDATE dbo.MEMBER
        SET Registration_Date = DATEADD(DAY, @Shift, Registration_Date)
        WHERE Visitor_ID BETWEEN 1 AND 6;

        UPDATE dbo.DAILY_PASS_TICKET
        SET Visit_Date = DATEADD(DAY, @Shift, Visit_Date)
        WHERE Visitor_ID BETWEEN 7 AND 8;

        UPDATE dbo.MAINTENANCE_LOG
        SET Maintenance_Date = DATEADD(DAY, @Shift, Maintenance_Date)
        WHERE (Equipment_ID = 1 AND Log_no = 1)
           OR (Equipment_ID = 4 AND Log_no = 1)
           OR (Equipment_ID = 5 AND Log_no = 1);

        COMMIT TRANSACTION;
        PRINT 'Done. Sample dates now start from today.';
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        DECLARE @Msg NVARCHAR(2048) = ERROR_MESSAGE();
        RAISERROR('Could not move the sample dates, the database is unchanged. Reason: %s  (If the bulk data of script 06 is loaded, run 06b_remove_bulk_data.sql and run this script again.)',
                  16, 1, @Msg);
    END CATCH
END
GO

/* =====================================================================
   Check: what the demo screens will show (days are relative to today)
   ===================================================================== */
SELECT 'Sessions' AS What, s.Session_ID AS Id, s.Session_Type AS Info,
       CONVERT(VARCHAR(16), s.Start_Time, 120) AS Starts,
       DATEDIFF(DAY, CAST(GETDATE() AS DATE), CAST(s.Start_Time AS DATE)) AS Days_From_Today
FROM dbo.[SESSION] s
WHERE s.Session_ID BETWEEN 1 AND 8
ORDER BY s.Start_Time;

SELECT 'Memberships' AS What, ms.Membership_ID AS Id, ms.Status AS Info,
       CONVERT(VARCHAR(10), ms.End_Date, 120) AS Ends,
       DATEDIFF(DAY, CAST(GETDATE() AS DATE), ms.End_Date) AS Days_Left
FROM dbo.MEMBERSHIP ms
WHERE ms.Membership_ID BETWEEN 1 AND 6
ORDER BY ms.End_Date;

SELECT 'Bills not paid' AS What, b.Bill_ID AS Id, b.Billing_Status AS Info,
       CONVERT(VARCHAR(10), b.Due_Date, 120) AS Due,
       DATEDIFF(DAY, b.Due_Date, CAST(GETDATE() AS DATE)) AS Days_Overdue
FROM dbo.BILLING_LOG b
WHERE b.Bill_ID BETWEEN 1 AND 8 AND b.Billing_Status IN ('Pending', 'Failed');
GO

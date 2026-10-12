/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   INT1313 - Database Basics
   Phase 3 - Script 07: checks at scale (run after 06_bulk_data.sql)

   Part A: the rules still work with thousands of rows (each test is rolled back).
   Part B: typical queries, with and without the index (compare logical reads).
   Part C: how long a rule-checked INSERT takes on the large tables.
   ===================================================================== */
USE GymPlanner;
GO

-- Read date literals as year-month-day on every server language setting
SET DATEFORMAT ymd;
GO

/* ---------------- PART A: rules at scale ---------------- */
-- Each test must END with an error (that is the expected result).
-- Expected messages are written in the comment above each test.

-- A1. Expected: error 50013 (BR-SCH-03, room already used)
BEGIN TRAN;
BEGIN TRY
    DECLARE @r INT, @t INT, @s DATETIME, @e DATETIME;
    SELECT TOP 1 @r = Room_ID, @s = Start_Time, @e = End_Time FROM dbo.[SESSION] WHERE Session_ID >= 1001 AND Session_Type = 'Class';
    SELECT TOP 1 @t = Staff_ID FROM dbo.TRAINER WHERE Staff_ID >= 1001 ORDER BY Staff_ID DESC;
    INSERT INTO dbo.[SESSION] (Session_Type, Room_ID, Trainer_ID, Start_Time, End_Time) VALUES ('Class', @r, @t, @s, @e);
    PRINT 'A1 WRONG: the insert was accepted';
END TRY
BEGIN CATCH
    PRINT 'A1 ' + CAST(ERROR_NUMBER() AS VARCHAR(10)) + ' - ' + ERROR_MESSAGE();
END CATCH;
IF @@TRANCOUNT > 0 ROLLBACK;
GO

-- A2. Expected: error 2601 (second Active membership for one member, BR-MEM-10)
BEGIN TRAN;
BEGIN TRY
    DECLARE @m INT;
    SELECT TOP 1 @m = Visitor_ID FROM dbo.MEMBERSHIP WHERE Membership_ID >= 1001 AND Status = 'Active';
    INSERT INTO dbo.MEMBERSHIP (Visitor_ID, Plan_ID, Start_Date, End_Date, Status) VALUES (@m, 1, '2026-10-01', '2026-11-01', 'Active');
    PRINT 'A2 WRONG: the insert was accepted';
END TRY
BEGIN CATCH
    PRINT 'A2 ' + CAST(ERROR_NUMBER() AS VARCHAR(10)) + ' - ' + ERROR_MESSAGE();
END CATCH;
IF @@TRANCOUNT > 0 ROLLBACK;
GO

-- A3. Expected: error 50012 (BR-SCH-02, class is full)
-- Fills one class to Max_Capacity, then adds one more member.
BEGIN TRAN;
BEGIN TRY
    DECLARE @c INT, @max INT, @taken INT;
    SELECT TOP 1 @c = c.Session_ID, @max = c.Max_Capacity
    FROM dbo.CLASS c WHERE c.Session_ID >= 1001 ORDER BY c.Session_ID;
    -- remove current registrations of this class, then fill every seat
    DELETE FROM dbo.CLASS_REGISTRATION WHERE Session_ID = @c;
    INSERT INTO dbo.CLASS_REGISTRATION (Session_ID, Visitor_ID, Attendance_Status)
    SELECT TOP (@max) @c, Visitor_ID, 'Confirmed' FROM dbo.MEMBERSHIP
    WHERE Membership_ID >= 1001 AND Status = 'Active' ORDER BY Visitor_ID;
    -- one more member -> must fail
    INSERT INTO dbo.CLASS_REGISTRATION (Session_ID, Visitor_ID, Attendance_Status)
    SELECT TOP 1 @c, Visitor_ID, 'Confirmed' FROM dbo.MEMBERSHIP
    WHERE Membership_ID >= 1001 AND Status = 'Active' ORDER BY Visitor_ID DESC;
    PRINT 'A3 WRONG: the insert was accepted';
END TRY
BEGIN CATCH
    PRINT 'A3 ' + CAST(ERROR_NUMBER() AS VARCHAR(10)) + ' - ' + ERROR_MESSAGE();
END CATCH;
IF @@TRANCOUNT > 0 ROLLBACK;
GO

-- A4. Expected: error 50017 (BR-SCH-05, member has no active membership)
BEGIN TRAN;
BEGIN TRY
    DECLARE @cl INT, @v INT;
    SELECT TOP 1 @cl = Session_ID FROM dbo.CLASS WHERE Session_ID >= 1001 ORDER BY Session_ID DESC;
    SELECT TOP 1 @v = mb.Visitor_ID FROM dbo.MEMBER mb
    WHERE mb.Visitor_ID >= 1001
      AND mb.Photo IS NOT NULL   -- has a photo, so BR-MEM-03 does not fire first
      AND NOT EXISTS (SELECT 1 FROM dbo.MEMBERSHIP ms WHERE ms.Visitor_ID = mb.Visitor_ID AND ms.Status = 'Active');
    INSERT INTO dbo.CLASS_REGISTRATION (Session_ID, Visitor_ID) VALUES (@cl, @v);
    PRINT 'A4 WRONG: the insert was accepted';
END TRY
BEGIN CATCH
    PRINT 'A4 ' + CAST(ERROR_NUMBER() AS VARCHAR(10)) + ' - ' + ERROR_MESSAGE();
END CATCH;
IF @@TRANCOUNT > 0 ROLLBACK;
GO

-- A5. Expected: error 50003 (BR-MEM-03, member without photo)
BEGIN TRAN;
BEGIN TRY
    DECLARE @np INT;
    SELECT TOP 1 @np = Visitor_ID FROM dbo.MEMBER WHERE Visitor_ID >= 1001 AND Photo IS NULL;
    INSERT INTO dbo.MEMBERSHIP (Visitor_ID, Plan_ID, Start_Date, End_Date, Status) VALUES (@np, 1, '2026-10-01', '2026-11-01', 'Active');
    PRINT 'A5 WRONG: the insert was accepted';
END TRY
BEGIN CATCH
    PRINT 'A5 ' + CAST(ERROR_NUMBER() AS VARCHAR(10)) + ' - ' + ERROR_MESSAGE();
END CATCH;
IF @@TRANCOUNT > 0 ROLLBACK;
GO

-- A6. Expected: error 50021 (BR-SCH-07, equipment not Active)
BEGIN TRAN;
BEGIN TRY
    DECLARE @eq INT, @ss INT, @f DATETIME, @tt DATETIME;
    SELECT TOP 1 @eq = Equipment_ID FROM dbo.EQUIPMENT WHERE Equipment_ID >= 1001 AND Operational_Status = 'Broken';
    SELECT TOP 1 @ss = Session_ID, @f = Start_Time, @tt = End_Time FROM dbo.[SESSION] WHERE Session_ID >= 1001 ORDER BY Session_ID DESC;
    INSERT INTO dbo.SESSION_EQUIPMENT_USAGE (Equipment_ID, Session_ID, Reserved_From, Reserved_To) VALUES (@eq, @ss, @f, @tt);
    PRINT 'A6 WRONG: the insert was accepted';
END TRY
BEGIN CATCH
    PRINT 'A6 ' + CAST(ERROR_NUMBER() AS VARCHAR(10)) + ' - ' + ERROR_MESSAGE();
END CATCH;
IF @@TRANCOUNT > 0 ROLLBACK;
GO

/* ---------------- PART B: speed of typical queries ---------------- */
SET STATISTICS IO ON;
SET STATISTICS TIME ON;
GO

-- B1. Timetable of one room for one week, WITH the index IX_SESSION_Room_Time
SELECT Session_ID, Session_Type, Trainer_ID, Start_Time, End_Time
FROM dbo.[SESSION] WITH (INDEX(IX_SESSION_Room_Time))
WHERE Room_ID = 1001 AND Start_Time >= '20261012' AND Start_Time < '20261019'
ORDER BY Start_Time;
GO
-- B1 again, WITHOUT the index (full scan of the table)
SELECT Session_ID, Session_Type, Trainer_ID, Start_Time, End_Time
FROM dbo.[SESSION] WITH (INDEX(0))
WHERE Room_ID = 1001 AND Start_Time >= '20261012' AND Start_Time < '20261019'
ORDER BY Start_Time;
GO

-- B2. Weekly schedule of one trainer, WITH and WITHOUT the index
SELECT Session_ID, Room_ID, Start_Time
FROM dbo.[SESSION] WITH (INDEX(IX_SESSION_Trainer_Time))
WHERE Trainer_ID = 1001 AND Start_Time >= '20261012' AND Start_Time < '20261019';
GO
SELECT Session_ID, Room_ID, Start_Time
FROM dbo.[SESSION] WITH (INDEX(0))
WHERE Trainer_ID = 1001 AND Start_Time >= '20261012' AND Start_Time < '20261019';
GO

-- B3. All classes of one member, WITH and WITHOUT the index IX_CLASS_REG_Visitor
SELECT r.Session_ID, r.Attendance_Status
FROM dbo.CLASS_REGISTRATION r WITH (INDEX(IX_CLASS_REG_Visitor))
WHERE r.Visitor_ID = 1001;
GO
SELECT r.Session_ID, r.Attendance_Status
FROM dbo.CLASS_REGISTRATION r WITH (INDEX(0))
WHERE r.Visitor_ID = 1001;
GO

-- B4. Report: seats taken per class (join of 3 tables, 2,600 classes)
SELECT TOP 10 c.Session_ID, c.Class_Name, c.Max_Capacity,
       COUNT(r.Visitor_ID) AS Seats_Taken
FROM dbo.CLASS c
JOIN dbo.CLASS_REGISTRATION r ON r.Session_ID = c.Session_ID AND r.Attendance_Status <> 'Cancelled'
GROUP BY c.Session_ID, c.Class_Name, c.Max_Capacity
ORDER BY Seats_Taken DESC;
GO

SET STATISTICS IO OFF;
SET STATISTICS TIME OFF;
GO

/* ---------------- PART C: INSERT with all triggers on, at scale ---------------- */
-- Inserts 200 valid sessions in a free week of 2027-06 (rolled back afterwards).
-- Every row goes through trg_SESSION_NoOverlap and the other session triggers.
BEGIN TRAN;
DECLARE @t0 DATETIME2 = SYSDATETIME();
;WITH n AS (SELECT TOP (200) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 AS k FROM sys.all_objects)
INSERT INTO dbo.[SESSION] (Session_Type, Room_ID, Trainer_ID, Start_Time, End_Time)
SELECT 'Class', 1001 + (k % 8), 1001 + (k % 25),
       DATEADD(HOUR, 1 + (k / 8), CAST('20270601' AS DATETIME)),
       DATEADD(HOUR, 2 + (k / 8), CAST('20270601' AS DATETIME))
FROM n;
SELECT 200 AS Rows_Inserted, DATEDIFF(MILLISECOND, @t0, SYSDATETIME()) AS Milliseconds;
ROLLBACK;
GO

/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   INT1313 - Database Basics
   Phase 3 - Script 05: tests for the business rules

   Run after 01, 04, 02 and 03, on the sample data.
   Every test runs inside BEGIN TRAN ... ROLLBACK, so the data is not
   changed. Read the Messages tab: each test prints one result line.
     "Blocked"   = the rule rejected the change (this is the expected result)
     "Accepted"  = the change was allowed (expected only for the tests
                   marked "should be accepted")
     "NOT BLOCKED - rule failed" = a rule is not working
   Note: a rejected INSERT still uses up an IDENTITY number; IDs of later
   inserts may skip numbers. This is normal.
   ===================================================================== */

USE GymPlanner;
GO

-- Read date literals as year-month-day on every server language setting
SET DATEFORMAT ymd;
GO
SET NOCOUNT ON;
GO

/* ---- BR-MEM-01: a member cannot also hold a daily pass ---- */
PRINT '--- BR-MEM-01 (1/2): visitor 7 holds a daily pass; make visitor 7 a member';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.MEMBER (Visitor_ID, Email) VALUES (7, 'huy.do@example.com');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-MEM-01 (2/2): visitor 1 is a member; give visitor 1 a daily pass';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.DAILY_PASS_TICKET (Visitor_ID) VALUES (1);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-MEM-02: a daily pass holder cannot register for a class (FK to MEMBER) ---- */
PRINT '--- BR-MEM-02: daily pass visitor 7 registers for class 1';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.CLASS_REGISTRATION (Session_ID, Visitor_ID) VALUES (1, 7);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-MEM-03: a member without a photo cannot buy a membership ---- */
PRINT '--- BR-MEM-03: member 6 has no photo and buys a monthly plan';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.MEMBERSHIP (Visitor_ID, Plan_ID, Start_Date, End_Date, Status)
    VALUES (6, 1, CAST(GETDATE() AS DATE), DATEADD(MONTH, 1, CAST(GETDATE() AS DATE)), 'Active');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-MEM-07: the bill's Visitor_ID must be the membership owner ---- */
PRINT '--- BR-MEM-07: bill for visitor 2 points to membership 1 (owned by visitor 1)';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.BILLING_LOG (Visitor_ID, Membership_ID, Due_Date, Amount_Paid, Billing_Status)
    VALUES (2, 1, CAST(GETDATE() AS DATE), 0.00, 'Pending');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-MEM-08: delete rules ---- */
PRINT '--- BR-MEM-08 (1/2): delete visitor 1, who has bills and a membership';
BEGIN TRY
    BEGIN TRAN;
    DELETE FROM dbo.VISITOR WHERE Visitor_ID = 1;
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-MEM-08 (2/2), should be accepted: delete a visitor entered by mistake (no other records)';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.VISITOR (First_Name, Last_Name, Phone) VALUES ('Test', 'Mistake', '0999999999');
    DELETE FROM dbo.VISITOR WHERE Phone = '0999999999';
    PRINT 'Accepted: the visitor was deleted';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Unexpected error: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-MEM-10: one active membership per member (needs script 03) ---- */
PRINT '--- BR-MEM-10: member 1 already has an Active membership; add a second one';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.MEMBERSHIP (Visitor_ID, Plan_ID, Start_Date, End_Date, Status)
    VALUES (1, 1, CAST(GETDATE() AS DATE), DATEADD(MONTH, 1, CAST(GETDATE() AS DATE)), 'Active');
    PRINT 'NOT BLOCKED - rule failed (is script 03 run?)';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-SCH-01: a staff member is a trainer or a receptionist, not both ---- */
PRINT '--- BR-SCH-01 (1/4): staff 1 is a trainer; make staff 1 a receptionist';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.RECEPTIONIST (Staff_ID, Shift_Type) VALUES (1, 'Morning');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-01 (2/4): staff 3 is a receptionist; make staff 3 a trainer';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.TRAINER (Staff_ID) VALUES (3);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-01 (3/4): staff 5 is a Manager; add staff 5 as a trainer';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.TRAINER (Staff_ID) VALUES (5);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-01 (4/4): staff 1 already has a TRAINER row; change staff 1 Role to Manager';
BEGIN TRY
    BEGIN TRAN;
    UPDATE dbo.STAFF SET Role = 'Manager' WHERE Staff_ID = 1;
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-SCH-02: capacity ---- */
PRINT '--- BR-SCH-02 (1/3): a class with Max_Capacity 25 in the Yoga Room (Capacity 20)';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.[SESSION] (Session_Type, Room_ID, Trainer_ID, Start_Time, End_Time)
    VALUES ('Class', 1, 6, '2026-10-20 10:00', '2026-10-20 11:00');
    INSERT INTO dbo.CLASS (Session_ID, Class_Name, Max_Capacity)
    VALUES (SCOPE_IDENTITY(), 'Big Class', 25);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-02 (2/3): class 2 (3 seats, all taken); member 3 cancels then re-confirms';
BEGIN TRY
    BEGIN TRAN;
    UPDATE dbo.CLASS_REGISTRATION SET Attendance_Status = 'Confirmed'
    WHERE Session_ID = 2 AND Visitor_ID = 3;
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-02 (3/3), should be accepted: member 1 registers for class 3 (2 of 15 seats used after this)';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.CLASS_REGISTRATION (Session_ID, Visitor_ID) VALUES (3, 1);
    PRINT 'Accepted: registration allowed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Unexpected error: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-SCH-03: no overlapping room schedule ---- */
PRINT '--- BR-SCH-03: new session in the Yoga Room 07:30-08:30 on 2026-10-12 (class 1 uses it 07:00-08:00)';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.[SESSION] (Session_Type, Room_ID, Trainer_ID, Start_Time, End_Time)
    VALUES ('Class', 1, 6, '2026-10-12 07:30', '2026-10-12 08:30');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-SCH-04: no overlapping trainer schedule ---- */
PRINT '--- BR-SCH-04: trainer 1 (leads class 1, 07:00-08:00) gets a PT session 07:30-08:30 in the free PT Studio';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.[SESSION] (Session_Type, Room_ID, Trainer_ID, Start_Time, End_Time)
    VALUES ('PT', 3, 1, '2026-10-12 07:30', '2026-10-12 08:30');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-SCH-03/04: a cancelled PT booking frees its slot, and re-checks when reactivated ---- */
PRINT '--- BR-SCH-03 (cancelled booking): cancel PT 4, reuse its slot, then un-cancel PT 4';
BEGIN TRY
    BEGIN TRAN;
    UPDATE dbo.PT_BOOKING SET Booking_Status = 'Cancelled' WHERE Session_ID = 4;
    PRINT 'Step 1 accepted: booking 4 cancelled';
    INSERT INTO dbo.[SESSION] (Session_Type, Room_ID, Trainer_ID, Start_Time, End_Time)
    VALUES ('PT', 3, 2, '2026-10-12 09:00', '2026-10-12 10:00');
    PRINT 'Step 2 accepted: the freed slot was reused';
    UPDATE dbo.PT_BOOKING SET Booking_Status = 'Scheduled' WHERE Session_ID = 4;
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Step 3 blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-SCH-05: only members with an active membership register ---- */
PRINT '--- BR-SCH-05: member 5 (membership Cancelled) registers for class 6';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.CLASS_REGISTRATION (Session_ID, Visitor_ID) VALUES (6, 5);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-SCH-06: a session is a class or a PT booking, never both ---- */
PRINT '--- BR-SCH-06 (1/3): add a CLASS row for session 4, which is a PT session';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.CLASS (Session_ID, Class_Name, Max_Capacity) VALUES (4, 'Wrong', 3);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-06 (2/3): add a PT_BOOKING row for session 1, which is a Class session';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.PT_BOOKING (Session_ID, Visitor_ID) VALUES (1, 1);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-06 (3/3): change Session_Type of session 1 from Class to PT';
BEGIN TRY
    BEGIN TRAN;
    UPDATE dbo.[SESSION] SET Session_Type = 'PT' WHERE Session_ID = 1;
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-SCH-07 / BR-FAC-06: only Active equipment can be reserved ---- */
PRINT '--- BR-SCH-07 (1/2): reserve equipment 4 (Under Maintenance) for session 1';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.SESSION_EQUIPMENT_USAGE (Equipment_ID, Session_ID, Reserved_From, Reserved_To)
    VALUES (4, 1, '2026-10-12 07:00', '2026-10-12 08:00');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-07 (2/2): reserve equipment 5 (Broken) for session 1';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.SESSION_EQUIPMENT_USAGE (Equipment_ID, Session_ID, Reserved_From, Reserved_To)
    VALUES (5, 1, '2026-10-12 07:00', '2026-10-12 08:00');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-SCH-08: equipment reservation time check ---- */
PRINT '--- BR-SCH-08 (1/3): reserve the yoga mats (equipment 3, already used 07:00-08:00 by class 1) for another class at the same time';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.[SESSION] (Session_Type, Room_ID, Trainer_ID, Start_Time, End_Time)
    VALUES ('Class', 2, 6, '2026-10-12 07:00', '2026-10-12 08:00');
    PRINT 'Step 1 accepted: new session in another room with another trainer';
    INSERT INTO dbo.SESSION_EQUIPMENT_USAGE (Equipment_ID, Session_ID, Reserved_From, Reserved_To)
    VALUES (3, SCOPE_IDENTITY(), '2026-10-12 07:00', '2026-10-12 08:00');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Step 2 blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-08 (2/3): reserve a spin bike (equipment 2) 09:30-10:30 for PT session 4, which ends at 10:00';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.SESSION_EQUIPMENT_USAGE (Equipment_ID, Session_ID, Reserved_From, Reserved_To)
    VALUES (2, 4, '2026-10-12 09:30', '2026-10-12 10:30');
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '--- BR-SCH-08 (3/3): shorten class 1 to end at 07:30, but the yoga mats are reserved until 08:00';
BEGIN TRY
    BEGIN TRAN;
    UPDATE dbo.[SESSION] SET End_Time = '2026-10-12 07:30' WHERE Session_ID = 1;
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-FAC-05: signer of a maintenance log ---- */
PRINT '--- BR-FAC-05: a Trainer (staff 1) signs a maintenance log';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.MAINTENANCE_LOG (Equipment_ID, Log_no, Maintenance_Date, Cost, Description, Logged_By)
    VALUES (1, 2, '2026-10-07', 10.00, 'Test log', 1);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-FAC-02/03: Log_no cannot repeat for the same equipment ---- */
PRINT '--- BR-FAC-03: a second log with Log_no = 1 for equipment 4';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.MAINTENANCE_LOG (Equipment_ID, Log_no, Maintenance_Date, Cost, Description, Logged_By)
    VALUES (4, 1, '2026-10-07', 10.00, 'Duplicate number', 3);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

/* ---- BR-FAC-07: cost cannot be negative ---- */
PRINT '--- BR-FAC-07: a maintenance log with Cost = -50';
BEGIN TRY
    BEGIN TRAN;
    INSERT INTO dbo.MAINTENANCE_LOG (Equipment_ID, Log_no, Maintenance_Date, Cost, Description, Logged_By)
    VALUES (1, 2, '2026-10-07', -50.00, 'Negative cost', 3);
    PRINT 'NOT BLOCKED - rule failed';
    ROLLBACK;
END TRY
BEGIN CATCH
    PRINT 'Blocked: ' + ERROR_MESSAGE();
    IF @@TRANCOUNT > 0 ROLLBACK;
END CATCH;
GO

PRINT '=== End of tests. Every line should say "Blocked" except the ones marked "should be accepted". ===';
GO

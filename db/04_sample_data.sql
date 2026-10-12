/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   INT1313 - Database Basics
   Phase 3 - Script 04: sample data

   Run order: 01_create_tables.sql -> 04_sample_data.sql -> 02_triggers.sql
   -> 03_indexes.sql. Load the data BEFORE the triggers: some triggers use the
   current date (BR-SCH-05), so loading first keeps the sample data valid
   whatever day it is run.
   Run on EMPTY tables (IDENTITY must start at 1, because the rows below
   refer to each other by ID number).
   "Today" in this sample = 2026-10-07.

   The data follows the business rules in the report, so it will also be
   accepted by the triggers in script 02:
   - each Visitor is either a Member (1-6) or a Daily Pass visitor (7-8)
   - each Member has at most one 'Active' membership
   - only members with an active membership are registered for classes
   - no two sessions overlap in the same room or for the same trainer
   - equipment is reserved only while 'Active', inside the session time
   - maintenance logs are signed by a Receptionist or the Manager
   ===================================================================== */

USE GymPlanner;
GO

-- Read date literals as year-month-day on every server language setting
SET DATEFORMAT ymd;
GO

/* ---------------------------------------------------------------------
   Optional reset (comment block). Use only to wipe the data and reload.
   --------------------------------------------------------------------- */
/*
DELETE FROM dbo.SESSION_EQUIPMENT_USAGE;
DELETE FROM dbo.MAINTENANCE_LOG;
DELETE FROM dbo.EQUIPMENT;
DELETE FROM dbo.PT_BOOKING;
DELETE FROM dbo.CLASS_REGISTRATION;
DELETE FROM dbo.CLASS;
DELETE FROM dbo.[SESSION];
DELETE FROM dbo.ROOM;
DELETE FROM dbo.RECEPTIONIST;
DELETE FROM dbo.TRAINER;
DELETE FROM dbo.STAFF;
DELETE FROM dbo.BILLING_LOG;
DELETE FROM dbo.MEMBERSHIP;
DELETE FROM dbo.MEMBERSHIP_PLAN;
DELETE FROM dbo.DAILY_PASS_TICKET;
DELETE FROM dbo.MEMBER;
DELETE FROM dbo.VISITOR;
DBCC CHECKIDENT ('dbo.SESSION_EQUIPMENT_USAGE', RESEED, 0);
DBCC CHECKIDENT ('dbo.EQUIPMENT', RESEED, 0);
DBCC CHECKIDENT ('dbo.[SESSION]', RESEED, 0);
DBCC CHECKIDENT ('dbo.ROOM', RESEED, 0);
DBCC CHECKIDENT ('dbo.STAFF', RESEED, 0);
DBCC CHECKIDENT ('dbo.BILLING_LOG', RESEED, 0);
DBCC CHECKIDENT ('dbo.MEMBERSHIP', RESEED, 0);
DBCC CHECKIDENT ('dbo.MEMBERSHIP_PLAN', RESEED, 0);
DBCC CHECKIDENT ('dbo.VISITOR', RESEED, 0);
GO
*/

/* =====================================================================
   1) VISITOR: 6 members (IDs 1-6) and 2 daily-pass visitors (IDs 7-8)
   ===================================================================== */
INSERT INTO dbo.VISITOR (First_Name, Last_Name, Phone) VALUES
('An',   'Nguyen', '0912000001'),   -- 1 member
('Binh', 'Tran',   '0912000002'),   -- 2 member
('Cuong','Le',     '0912000003'),   -- 3 member
('Dung', 'Pham',   '0912000004'),   -- 4 member (renewed membership)
('Em',   'Vo',     '0912000005'),   -- 5 member (cancelled membership)
('Giang','Dang',   '0912000006'),   -- 6 member (no membership yet)
('Huy',  'Do',     '0912000007'),   -- 7 daily pass
('Lan',  'Bui',    '0912000008');   -- 8 daily pass
GO

/* 2) MEMBER */
-- BR-MEM-03: a member needs a photo before buying a membership or booking.
-- Members 1-5 get a small placeholder value (0xFFD8FFE0 = start of a JPEG file).
-- Member 6 has NO photo on purpose (used to test BR-MEM-03).
INSERT INTO dbo.MEMBER (Visitor_ID, Email, Birth_Date, Photo, Registration_Date) VALUES
(1, 'an.nguyen@example.com',    '1998-04-12', 0xFFD8FFE0, '2025-12-28'),
(2, 'binh.tran@example.com',    '2001-09-30', 0xFFD8FFE0, '2026-07-25'),
(3, 'cuong.le@example.com',     '1995-01-18', 0xFFD8FFE0, '2026-09-18'),
(4, 'dung.pham@example.com',    '2000-06-05', 0xFFD8FFE0, '2026-06-25'),
(5, 'em.vo@example.com',        '2003-11-22', 0xFFD8FFE0, '2026-05-28'),
(6, 'giang.dang@example.com',   '1999-02-14', NULL,       '2026-10-01');
GO

/* 3) DAILY_PASS_TICKET */
INSERT INTO dbo.DAILY_PASS_TICKET (Visitor_ID, Visit_Date, Daily_Rate) VALUES
(7, '2026-10-05', 15.00),
(8, '2026-10-06', 15.00);
GO

/* =====================================================================
   4) MEMBERSHIP_PLAN
   ===================================================================== */
INSERT INTO dbo.MEMBERSHIP_PLAN (Plan_Name, Duration_Months, Price, Description) VALUES
('Monthly',   1,  30.00, 'One month, access to all open gym areas'),
('Quarterly', 3,  80.00, 'Three months, access to all open gym areas'),
('Annual',   12, 280.00, 'Twelve months, access to all open gym areas');
GO

/* 5) MEMBERSHIP (member 4 has an Expired one and a newer Active one) */
INSERT INTO dbo.MEMBERSHIP (Visitor_ID, Plan_ID, Start_Date, End_Date, Status) VALUES
(1, 3, '2026-09-01', '2027-09-01', 'Active'),     -- 1
(2, 2, '2026-08-01', '2026-11-01', 'Active'),     -- 2
(3, 1, '2026-09-20', '2026-10-20', 'Active'),     -- 3
(4, 1, '2026-07-01', '2026-08-01', 'Expired'),    -- 4
(5, 2, '2026-06-01', '2026-09-01', 'Cancelled'),  -- 5
(4, 1, '2026-09-15', '2026-10-15', 'Active');     -- 6
GO

/* 6) BILLING_LOG (Visitor_ID matches the owner of the membership) */
INSERT INTO dbo.BILLING_LOG
    (Visitor_ID, Membership_ID, Invoice_Date, Due_Date, Payment_Date, Amount_Paid, Billing_Status) VALUES
(1, 1,    '2026-09-01', '2026-09-08', '2026-09-02', 280.00, 'Paid'),
(2, 2,    '2026-08-01', '2026-08-08', '2026-08-01',  80.00, 'Paid'),
(3, 3,    '2026-09-20', '2026-09-27', NULL,           0.00, 'Pending'),   -- not paid yet
(4, 4,    '2026-07-01', '2026-07-08', '2026-07-01',  30.00, 'Paid'),
(5, 5,    '2026-06-01', '2026-06-08', NULL,           0.00, 'Failed'),    -- card declined
(4, 6,    '2026-09-15', '2026-09-22', '2026-09-15',  30.00, 'Paid'),
(7, NULL, '2026-10-05', '2026-10-05', '2026-10-05',  15.00, 'Paid'),      -- daily pass: no membership
(8, NULL, '2026-10-06', '2026-10-06', '2026-10-06',  15.00, 'Paid');
GO

/* =====================================================================
   7) STAFF: 3 trainers, 2 receptionists, 1 manager
   ===================================================================== */
INSERT INTO dbo.STAFF (Full_Name, Phone, Role, Hire_Date, Salary) VALUES
('Khoa Nguyen Minh', '0901000001', 'Trainer',      '2025-03-01', 800.00),   -- 1
('Ha Tran Thu',      '0901000002', 'Trainer',      '2025-06-15', 850.00),   -- 2
('Trang Le Thu',     '0901000003', 'Receptionist', '2025-04-10', 500.00),   -- 3
('Long Pham Hai',    '0901000004', 'Receptionist', '2025-08-20', 500.00),   -- 4
('Phuong Vu Mai',    '0901000005', 'Manager',      '2024-01-05', 1200.00),  -- 5
('Duc Hoang Anh',    '0901000006', 'Trainer',      '2026-02-01', 780.00);   -- 6
GO

INSERT INTO dbo.TRAINER (Staff_ID, Specialty, Certification) VALUES
(1, 'Yoga',     'RYT-200'),
(2, 'Strength', 'NASM-CPT'),
(6, 'Cardio',   NULL);
GO

INSERT INTO dbo.RECEPTIONIST (Staff_ID, Shift_Type) VALUES
(3, 'Morning'),
(4, 'Evening');
GO

/* =====================================================================
   8) ROOM, EQUIPMENT, MAINTENANCE_LOG
   ===================================================================== */
INSERT INTO dbo.ROOM (Room_Name, Capacity, Floor_Location) VALUES
('Yoga Room',   20, 'Floor 1'),   -- 1
('Cardio Zone', 30, 'Floor 2'),   -- 2
('PT Studio',    4, 'Floor 1');   -- 3
GO

INSERT INTO dbo.EQUIPMENT (Equipment_Name, Room_ID, Operational_Status, Purchase_Date) VALUES
('Treadmill T-01',      2, 'Active',            '2024-05-10'),   -- 1
('Spin Bike S-01',      2, 'Active',            '2024-06-01'),   -- 2
('Yoga Mat Set',        1, 'Active',            '2025-01-15'),   -- 3
('Cable Machine C-01',  3, 'Under Maintenance', '2023-11-20'),   -- 4
('Rowing Machine R-01', 2, 'Broken',            '2023-09-02'),   -- 5
('Barbell Set B-01',    3, 'Active',            '2024-02-18');   -- 6
GO

INSERT INTO dbo.MAINTENANCE_LOG (Equipment_ID, Log_no, Maintenance_Date, Cost, Description, Logged_By) VALUES
(1, 1, '2026-08-15',  45.00, 'Routine belt lubrication',           3),
(4, 1, '2026-10-02', 120.00, 'Replace worn cable, waiting for test', 3),
(5, 1, '2026-09-28',   0.00, 'Motor fault, waiting for spare part',  5);
GO

/* =====================================================================
   9) SESSION, CLASS, PT_BOOKING
   Sessions 1,2,3,6,7 = classes; sessions 4,5,8 = PT bookings.
   ===================================================================== */
INSERT INTO dbo.[SESSION] (Session_Type, Room_ID, Trainer_ID, Start_Time, End_Time) VALUES
('Class', 1, 1, '2026-10-12 07:00', '2026-10-12 08:00'),   -- 1 Yoga Flow
('Class', 2, 2, '2026-10-12 18:00', '2026-10-12 19:00'),   -- 2 HIIT Burn (small, 3 seats)
('Class', 1, 1, '2026-10-13 07:00', '2026-10-13 08:00'),   -- 3 Yoga Flow
('PT',    3, 2, '2026-10-12 09:00', '2026-10-12 10:00'),   -- 4 PT
('PT',    3, 1, '2026-10-13 09:00', '2026-10-13 10:00'),   -- 5 PT
('Class', 2, 6, '2026-10-14 18:00', '2026-10-14 19:00'),   -- 6 Spinning
('Class', 1, 1, '2026-10-05 07:00', '2026-10-05 08:00'),   -- 7 Yoga Flow (already held)
('PT',    3, 2, '2026-10-05 09:00', '2026-10-05 10:00');   -- 8 PT (already held)
GO

INSERT INTO dbo.CLASS (Session_ID, Class_Name, Max_Capacity) VALUES
(1, 'Yoga Flow',  15),
(2, 'HIIT Burn',   3),
(3, 'Yoga Flow',  15),
(6, 'Spinning',   25),
(7, 'Yoga Flow',  15);
GO

INSERT INTO dbo.PT_BOOKING (Session_ID, Visitor_ID, Booking_Status) VALUES
(4, 1, 'Scheduled'),
(5, 2, 'Scheduled'),
(8, 3, 'Completed');
GO

/* 10) CLASS_REGISTRATION
   Class 2 (3 seats): 3 seats are taken ('Confirmed'); member 3 'Cancelled'
   does not hold a seat.
   Class 7 already took place: two members 'Attended', one 'Cancelled'. */
INSERT INTO dbo.CLASS_REGISTRATION (Session_ID, Visitor_ID, Registration_Date, Attendance_Status) VALUES
(1, 1, '2026-10-06 09:00', 'Confirmed'),
(1, 2, '2026-10-06 10:30', 'Confirmed'),
(1, 3, '2026-10-06 11:00', 'Confirmed'),
(2, 1, '2026-10-06 09:05', 'Confirmed'),
(2, 2, '2026-10-06 10:35', 'Confirmed'),
(2, 4, '2026-10-06 12:00', 'Confirmed'),
(2, 3, '2026-10-06 11:05', 'Cancelled'),
(3, 4, '2026-10-06 12:10', 'Confirmed'),
(6, 1, '2026-10-06 09:10', 'Confirmed'),
(6, 2, '2026-10-06 10:40', 'Confirmed'),
(7, 1, '2026-10-03 08:00', 'Attended'),
(7, 2, '2026-10-03 08:10', 'Attended'),
(7, 3, '2026-10-03 08:20', 'Cancelled');
GO

/* 11) SESSION_EQUIPMENT_USAGE (Active equipment only, inside the session time) */
INSERT INTO dbo.SESSION_EQUIPMENT_USAGE (Equipment_ID, Session_ID, Reserved_From, Reserved_To) VALUES
(3, 1, '2026-10-12 07:00', '2026-10-12 08:00'),   -- Yoga mats for class 1
(1, 2, '2026-10-12 18:00', '2026-10-12 19:00'),   -- Treadmill for class 2
(3, 3, '2026-10-13 07:00', '2026-10-13 08:00'),   -- Yoga mats for class 3
(6, 4, '2026-10-12 09:00', '2026-10-12 10:00'),   -- Barbell for PT 4
(6, 5, '2026-10-13 09:00', '2026-10-13 10:00'),   -- Barbell for PT 5
(2, 6, '2026-10-14 18:00', '2026-10-14 19:00'),   -- Spin bike for class 6
(3, 7, '2026-10-05 07:00', '2026-10-05 08:00');   -- Yoga mats for class 7
GO

/* =====================================================================
   Checks: expected row counts
   VISITOR 8, MEMBER 6, DAILY_PASS_TICKET 2, MEMBERSHIP_PLAN 3, MEMBERSHIP 6,
   BILLING_LOG 8, STAFF 6, TRAINER 3, RECEPTIONIST 2, ROOM 3, SESSION 8,
   CLASS 5, PT_BOOKING 3, CLASS_REGISTRATION 13, EQUIPMENT 6,
   MAINTENANCE_LOG 3, SESSION_EQUIPMENT_USAGE 7
   ===================================================================== */
SELECT 'VISITOR' AS Table_Name, COUNT(*) AS Rows_Count FROM dbo.VISITOR
UNION ALL SELECT 'MEMBER',                  COUNT(*) FROM dbo.MEMBER
UNION ALL SELECT 'DAILY_PASS_TICKET',       COUNT(*) FROM dbo.DAILY_PASS_TICKET
UNION ALL SELECT 'MEMBERSHIP_PLAN',         COUNT(*) FROM dbo.MEMBERSHIP_PLAN
UNION ALL SELECT 'MEMBERSHIP',              COUNT(*) FROM dbo.MEMBERSHIP
UNION ALL SELECT 'BILLING_LOG',             COUNT(*) FROM dbo.BILLING_LOG
UNION ALL SELECT 'STAFF',                   COUNT(*) FROM dbo.STAFF
UNION ALL SELECT 'TRAINER',                 COUNT(*) FROM dbo.TRAINER
UNION ALL SELECT 'RECEPTIONIST',            COUNT(*) FROM dbo.RECEPTIONIST
UNION ALL SELECT 'ROOM',                    COUNT(*) FROM dbo.ROOM
UNION ALL SELECT 'SESSION',                 COUNT(*) FROM dbo.[SESSION]
UNION ALL SELECT 'CLASS',                   COUNT(*) FROM dbo.CLASS
UNION ALL SELECT 'PT_BOOKING',              COUNT(*) FROM dbo.PT_BOOKING
UNION ALL SELECT 'CLASS_REGISTRATION',      COUNT(*) FROM dbo.CLASS_REGISTRATION
UNION ALL SELECT 'EQUIPMENT',               COUNT(*) FROM dbo.EQUIPMENT
UNION ALL SELECT 'MAINTENANCE_LOG',         COUNT(*) FROM dbo.MAINTENANCE_LOG
UNION ALL SELECT 'SESSION_EQUIPMENT_USAGE', COUNT(*) FROM dbo.SESSION_EQUIPMENT_USAGE;
GO

-- Seats taken per class (every registration that is not 'Cancelled')
SELECT c.Session_ID, c.Class_Name, c.Max_Capacity,
       COUNT(r.Visitor_ID)                    AS Da_Dang_Ky,
       c.Max_Capacity - COUNT(r.Visitor_ID)   AS Con_Trong
FROM dbo.CLASS c
LEFT JOIN dbo.CLASS_REGISTRATION r
       ON r.Session_ID = c.Session_ID AND r.Attendance_Status <> 'Cancelled'
GROUP BY c.Session_ID, c.Class_Name, c.Max_Capacity
ORDER BY c.Session_ID;
GO

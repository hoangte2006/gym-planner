/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   INT1313 - Database Basics
   Phase 3 - Script 08: views

   Run after 01, 04, 02 and 03. Safe to run again (CREATE OR ALTER).

   A view is a saved SELECT. The application and the report users read
   the view instead of repeating a long query. Views are also used for
   security (script 10): a role can be allowed to read a view without being
   allowed to read the tables behind it, so a view can hide columns such as
   Salary and Phone.

   vw_Active_Membership      members with a membership whose Status = 'Active'
   vw_Class_Schedule         every class with room, trainer and seats taken/left
   vw_Member_Billing_Summary bills of every member: total paid, unpaid bills
   vw_Trainer_Public         trainers without Salary and Phone (safe to show)
   vw_Equipment_Status       equipment with its last maintenance and total cost

   Code style (SQLStyle.guide): SQL keywords in capitals, one column per
   line, JOIN conditions indented under the JOIN, every alias written with AS.
   ===================================================================== */

USE GymPlanner;
GO

/* ---------------------------------------------------------------------
   View 1: active memberships (BR-MEM-10 allows at most one per member)
   Days_Left counts from the date on which the view is read.
   --------------------------------------------------------------------- */
CREATE OR ALTER VIEW dbo.vw_Active_Membership
AS
SELECT
    ms.Membership_ID,
    v.Visitor_ID,
    v.First_Name + ' ' + v.Last_Name                        AS Member_Name,
    p.Plan_Name,
    ms.Start_Date,
    ms.End_Date,
    DATEDIFF(DAY, CAST(GETDATE() AS DATE), ms.End_Date)    AS Days_Left
FROM dbo.MEMBERSHIP AS ms
INNER JOIN dbo.MEMBER AS m
    ON m.Visitor_ID = ms.Visitor_ID
INNER JOIN dbo.VISITOR AS v
    ON v.Visitor_ID = m.Visitor_ID
INNER JOIN dbo.MEMBERSHIP_PLAN AS p
    ON p.Plan_ID = ms.Plan_ID
WHERE ms.Status = 'Active';
GO

/* ---------------------------------------------------------------------
   View 2: class schedule with seats
   A seat is taken by every registration that is not 'Cancelled' (BR-SCH-02).
   --------------------------------------------------------------------- */
CREATE OR ALTER VIEW dbo.vw_Class_Schedule
AS
SELECT
    c.Session_ID,
    c.Class_Name,
    s.Start_Time,
    s.End_Time,
    r.Room_Name,
    st.Full_Name                             AS Trainer_Name,
    c.Max_Capacity,
    COUNT(cr.Visitor_ID)                     AS Seats_Taken,
    c.Max_Capacity - COUNT(cr.Visitor_ID)    AS Seats_Left
FROM dbo.CLASS AS c
INNER JOIN dbo.[SESSION] AS s
    ON s.Session_ID = c.Session_ID
INNER JOIN dbo.ROOM AS r
    ON r.Room_ID = s.Room_ID
INNER JOIN dbo.STAFF AS st
    ON st.Staff_ID = s.Trainer_ID
LEFT JOIN dbo.CLASS_REGISTRATION AS cr
    ON cr.Session_ID = c.Session_ID
   AND cr.Attendance_Status <> 'Cancelled'
GROUP BY
    c.Session_ID,
    c.Class_Name,
    s.Start_Time,
    s.End_Time,
    r.Room_Name,
    st.Full_Name,
    c.Max_Capacity;
GO

/* ---------------------------------------------------------------------
   View 3: billing summary of every member (members without a bill show 0)
   Open_Bills = bills that are still 'Pending' or 'Failed'.
   --------------------------------------------------------------------- */
CREATE OR ALTER VIEW dbo.vw_Member_Billing_Summary
AS
SELECT
    v.Visitor_ID,
    v.First_Name + ' ' + v.Last_Name AS Member_Name,
    COUNT(b.Bill_ID)                 AS Bills,
    COALESCE(SUM(CASE WHEN b.Billing_Status = 'Paid'
                      THEN b.Amount_Paid END), 0.00) AS Total_Paid,
    COALESCE(SUM(CASE WHEN b.Billing_Status IN ('Pending', 'Failed')
                      THEN 1 END), 0)                AS Open_Bills
FROM dbo.MEMBER AS m
INNER JOIN dbo.VISITOR AS v
    ON v.Visitor_ID = m.Visitor_ID
LEFT JOIN dbo.BILLING_LOG AS b
    ON b.Visitor_ID = v.Visitor_ID
GROUP BY
    v.Visitor_ID,
    v.First_Name,
    v.Last_Name;
GO

/* ---------------------------------------------------------------------
   View 4: trainers for public display. Salary and Phone are left out on
   purpose; the role role_Member (script 10) may read this view only.
   --------------------------------------------------------------------- */
CREATE OR ALTER VIEW dbo.vw_Trainer_Public
AS
SELECT
    st.Staff_ID,
    st.Full_Name,
    t.Specialty,
    t.Certification
FROM dbo.TRAINER AS t
INNER JOIN dbo.STAFF AS st
    ON st.Staff_ID = t.Staff_ID;
GO

/* ---------------------------------------------------------------------
   View 5: equipment status with its maintenance history
   Equipment with no log shows Last_Maintenance = NULL and Total_Cost = 0.
   --------------------------------------------------------------------- */
CREATE OR ALTER VIEW dbo.vw_Equipment_Status
AS
SELECT
    e.Equipment_ID,
    e.Equipment_Name,
    r.Room_Name,
    e.Operational_Status,
    MAX(ml.Maintenance_Date)    AS Last_Maintenance,
    COUNT(ml.Log_no)            AS Maintenance_Count,
    COALESCE(SUM(ml.Cost), 0.00) AS Total_Cost
FROM dbo.EQUIPMENT AS e
LEFT JOIN dbo.ROOM AS r
    ON r.Room_ID = e.Room_ID
LEFT JOIN dbo.MAINTENANCE_LOG AS ml
    ON ml.Equipment_ID = e.Equipment_ID
GROUP BY
    e.Equipment_ID,
    e.Equipment_Name,
    r.Room_Name,
    e.Operational_Status;
GO

-- Quick check: read each view once (the sample data of script 04 must be loaded)
SELECT * FROM dbo.vw_Active_Membership      ORDER BY Membership_ID;
SELECT * FROM dbo.vw_Class_Schedule         ORDER BY Start_Time;
SELECT * FROM dbo.vw_Member_Billing_Summary ORDER BY Visitor_ID;
SELECT * FROM dbo.vw_Trainer_Public         ORDER BY Staff_ID;
SELECT * FROM dbo.vw_Equipment_Status       ORDER BY Equipment_ID;
GO

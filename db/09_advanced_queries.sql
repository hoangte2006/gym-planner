/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   INT1313 - Database Basics
   Phase 3 - Script 09: 13 advanced queries

   Run after 01, 04, 02, 03 and 08, on the sample data of script 04.
   Run the whole file in one go. To run only one query, select the DECLARE
   line together with that query (down to its semicolon) and execute the
   selection. The Results tab shows one result set per query, in the
   order Q1 ... Q13.

   Reference date: the sample data was written for "today" = 2026-10-07,
   so the queries use the variable @today below instead of GETDATE().
   This makes the results the same on any day (they match the report).
   For live use, replace the first line of the variable with
       DECLARE @today DATE = CAST(GETDATE() AS DATE);

   Query  Technique
   Q1     INNER JOIN of 4 tables + LEFT JOIN + GROUP BY
   Q2     INNER JOIN of 4 tables + BETWEEN + DATEDIFF
   Q3     GROUP BY + HAVING over a 3-table join
   Q4     NOT EXISTS (anti-join, correlated subquery)
   Q5     CTE + CASE + percentage
   Q6     LEFT JOIN + conditional aggregation (SUM(CASE ...))
   Q7     JOIN + CASE + date arithmetic
   Q8     relational division (double NOT EXISTS)
   Q9     LEFT JOIN + MAX + SUM + GROUP BY
   Q10    CTE + UNION ALL + window function DENSE_RANK
   Q11    CTE + window function SUM() OVER (running total)
   Q12    CTE + scalar subquery (AVG)
   Q13    self-join: audit query that must return 0 rows (BR-SCH-03/04)

   Code style (SQLStyle.guide): SQL keywords in capitals, one column per
   line, JOIN conditions indented under the JOIN, every alias written with AS.
   ===================================================================== */

USE GymPlanner;
GO

SET DATEFORMAT ymd;
GO

DECLARE @today DATE = '2026-10-07';

/* ---------------------------------------------------------------------
   Q1. Upcoming classes: when, where, who teaches and how many seats are
       taken. A seat is taken by every registration that is not 'Cancelled'.
       Technique: 4 INNER JOINs, LEFT JOIN, GROUP BY, COUNT.
   --------------------------------------------------------------------- */
SELECT
    c.Session_ID,
    c.Class_Name,
    s.Start_Time,
    r.Room_Name,
    st.Full_Name          AS Trainer_Name,
    c.Max_Capacity,
    COUNT(cr.Visitor_ID)  AS Seats_Taken
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
WHERE s.Start_Time >= @today
GROUP BY
    c.Session_ID,
    c.Class_Name,
    s.Start_Time,
    r.Room_Name,
    st.Full_Name,
    c.Max_Capacity
ORDER BY s.Start_Time;

/* ---------------------------------------------------------------------
   Q2. Members whose membership is active today, with the days left.
       "Active" follows BR-SCH-05: Status = 'Active' and today lies between
       Start_Date and End_Date.
       Technique: 4 INNER JOINs, BETWEEN, DATEDIFF.
   --------------------------------------------------------------------- */
SELECT
    v.Visitor_ID,
    v.First_Name + ' ' + v.Last_Name AS Member_Name,
    p.Plan_Name,
    ms.End_Date,
    DATEDIFF(DAY, @today, ms.End_Date) AS Days_Left
FROM dbo.MEMBERSHIP AS ms
INNER JOIN dbo.MEMBER AS m
    ON m.Visitor_ID = ms.Visitor_ID
INNER JOIN dbo.VISITOR AS v
    ON v.Visitor_ID = m.Visitor_ID
INNER JOIN dbo.MEMBERSHIP_PLAN AS p
    ON p.Plan_ID = ms.Plan_ID
WHERE ms.Status = 'Active'
  AND @today BETWEEN ms.Start_Date AND ms.End_Date
ORDER BY Days_Left, v.Visitor_ID;

/* ---------------------------------------------------------------------
   Q3. Plans that earned more than 70.00 from paid membership bills.
       WHERE keeps only 'Paid' bills (row filter); HAVING keeps only plans
       with a large total (group filter).
       Technique: 3-table join, GROUP BY, HAVING, SUM, COUNT.
   --------------------------------------------------------------------- */
SELECT
    p.Plan_Name,
    COUNT(b.Bill_ID)    AS Paid_Bills,
    SUM(b.Amount_Paid)  AS Revenue
FROM dbo.BILLING_LOG AS b
INNER JOIN dbo.MEMBERSHIP AS ms
    ON ms.Membership_ID = b.Membership_ID
INNER JOIN dbo.MEMBERSHIP_PLAN AS p
    ON p.Plan_ID = ms.Plan_ID
WHERE b.Billing_Status = 'Paid'
GROUP BY p.Plan_Name
HAVING SUM(b.Amount_Paid) > 70.00
ORDER BY Revenue DESC;

/* ---------------------------------------------------------------------
   Q4. Members who have never booked a personal-training session.
       Technique: NOT EXISTS (correlated subquery). The same result can be
       written as a LEFT JOIN ... WHERE pt.Session_ID IS NULL.
   --------------------------------------------------------------------- */
SELECT
    v.Visitor_ID,
    v.First_Name + ' ' + v.Last_Name AS Member_Name,
    m.Registration_Date
FROM dbo.MEMBER AS m
INNER JOIN dbo.VISITOR AS v
    ON v.Visitor_ID = m.Visitor_ID
WHERE NOT EXISTS (SELECT 1
                  FROM dbo.PT_BOOKING AS pt
                  WHERE pt.Visitor_ID = m.Visitor_ID)
ORDER BY v.Visitor_ID;

/* ---------------------------------------------------------------------
   Q5. How full is every class? Percent of seats taken and a label.
       Technique: CTE (WITH), CASE, CAST for a decimal percentage.
   --------------------------------------------------------------------- */
WITH seats AS (
    SELECT
        c.Session_ID,
        c.Class_Name,
        c.Max_Capacity,
        COUNT(cr.Visitor_ID) AS Seats_Taken
    FROM dbo.CLASS AS c
    LEFT JOIN dbo.CLASS_REGISTRATION AS cr
        ON cr.Session_ID = c.Session_ID
       AND cr.Attendance_Status <> 'Cancelled'
    GROUP BY
        c.Session_ID,
        c.Class_Name,
        c.Max_Capacity
)
SELECT
    Session_ID,
    Class_Name,
    Seats_Taken,
    Max_Capacity,
    CAST(100.0 * Seats_Taken / Max_Capacity AS DECIMAL(5, 1)) AS Percent_Full,
    CASE
        WHEN Seats_Taken >= Max_Capacity        THEN 'Full'
        WHEN Seats_Taken >= 0.8 * Max_Capacity  THEN 'Almost full'
        ELSE 'Seats available'
    END AS Class_Status
FROM seats
ORDER BY Percent_Full DESC, Session_ID;

/* ---------------------------------------------------------------------
   Q6. Workload of every trainer: sessions by type and teaching hours.
       The LEFT JOIN keeps a trainer who has no session yet (shown with 0).
       Technique: LEFT JOIN, SUM(CASE ...), DATEDIFF.
   --------------------------------------------------------------------- */
SELECT
    st.Staff_ID,
    st.Full_Name,
    COUNT(s.Session_ID)                                    AS Sessions,
    SUM(CASE WHEN s.Session_Type = 'Class' THEN 1 ELSE 0 END) AS Class_Sessions,
    SUM(CASE WHEN s.Session_Type = 'PT'    THEN 1 ELSE 0 END) AS PT_Sessions,
    CAST(COALESCE(SUM(DATEDIFF(MINUTE, s.Start_Time, s.End_Time)), 0) / 60.0
         AS DECIMAL(6, 1))                                 AS Teaching_Hours
FROM dbo.TRAINER AS t
INNER JOIN dbo.STAFF AS st
    ON st.Staff_ID = t.Staff_ID
LEFT JOIN dbo.[SESSION] AS s
    ON s.Trainer_ID = t.Staff_ID
GROUP BY
    st.Staff_ID,
    st.Full_Name
ORDER BY Teaching_Hours DESC, st.Staff_ID;

/* ---------------------------------------------------------------------
   Q7. Overdue bills: not 'Paid' and the due date is before @today.
       Technique: INNER JOIN, CASE, date arithmetic.
   --------------------------------------------------------------------- */
SELECT
    b.Bill_ID,
    v.First_Name + ' ' + v.Last_Name  AS Member_Name,
    b.Billing_Status,
    b.Due_Date,
    DATEDIFF(DAY, b.Due_Date, @today) AS Days_Overdue,
    CASE
        WHEN DATEDIFF(DAY, b.Due_Date, @today) > 30 THEN 'Contact the member'
        ELSE 'Send a reminder'
    END AS Suggested_Action
FROM dbo.BILLING_LOG AS b
INNER JOIN dbo.VISITOR AS v
    ON v.Visitor_ID = b.Visitor_ID
WHERE b.Billing_Status <> 'Paid'
  AND b.Due_Date < @today
ORDER BY Days_Overdue DESC;

/* ---------------------------------------------------------------------
   Q8. Members who have joined a class (not cancelled) in EVERY room that
       hosts classes. This is relational division: "there is no room with
       classes in which the member has no registration".
       Technique: double NOT EXISTS.
   --------------------------------------------------------------------- */
SELECT
    v.Visitor_ID,
    v.First_Name + ' ' + v.Last_Name AS Member_Name
FROM dbo.MEMBER AS m
INNER JOIN dbo.VISITOR AS v
    ON v.Visitor_ID = m.Visitor_ID
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.ROOM AS r
    WHERE EXISTS (SELECT 1
                  FROM dbo.CLASS AS c
                  INNER JOIN dbo.[SESSION] AS s
                      ON s.Session_ID = c.Session_ID
                  WHERE s.Room_ID = r.Room_ID)
      AND NOT EXISTS (SELECT 1
                      FROM dbo.CLASS_REGISTRATION AS cr
                      INNER JOIN dbo.[SESSION] AS s2
                          ON s2.Session_ID = cr.Session_ID
                      WHERE cr.Visitor_ID = m.Visitor_ID
                        AND cr.Attendance_Status <> 'Cancelled'
                        AND s2.Room_ID = r.Room_ID)
)
ORDER BY v.Visitor_ID;

/* ---------------------------------------------------------------------
   Q9. Equipment that is not 'Active': room, last maintenance, total cost.
       Technique: LEFT JOIN (twice), MAX, SUM, GROUP BY.
   --------------------------------------------------------------------- */
SELECT
    e.Equipment_ID,
    e.Equipment_Name,
    e.Operational_Status,
    r.Room_Name,
    MAX(ml.Maintenance_Date)       AS Last_Maintenance,
    COALESCE(SUM(ml.Cost), 0.00)   AS Total_Cost
FROM dbo.EQUIPMENT AS e
LEFT JOIN dbo.ROOM AS r
    ON r.Room_ID = e.Room_ID
LEFT JOIN dbo.MAINTENANCE_LOG AS ml
    ON ml.Equipment_ID = e.Equipment_ID
WHERE e.Operational_Status <> 'Active'
GROUP BY
    e.Equipment_ID,
    e.Equipment_Name,
    e.Operational_Status,
    r.Room_Name
ORDER BY e.Equipment_ID;

/* ---------------------------------------------------------------------
   Q10. Ranking of members by activity: classes joined plus PT bookings,
        not counting cancelled ones. DENSE_RANK gives equal rank to ties and
        does not skip rank numbers.
        Technique: CTE, UNION ALL, LEFT JOIN, window function DENSE_RANK.
   --------------------------------------------------------------------- */
WITH activity AS (
    SELECT Visitor_ID
    FROM dbo.CLASS_REGISTRATION
    WHERE Attendance_Status <> 'Cancelled'
    UNION ALL
    SELECT Visitor_ID
    FROM dbo.PT_BOOKING
    WHERE Booking_Status <> 'Cancelled'
)
SELECT
    v.Visitor_ID,
    v.First_Name + ' ' + v.Last_Name                   AS Member_Name,
    COUNT(a.Visitor_ID)                                AS Activities,
    DENSE_RANK() OVER (ORDER BY COUNT(a.Visitor_ID) DESC) AS Activity_Rank
FROM dbo.MEMBER AS m
INNER JOIN dbo.VISITOR AS v
    ON v.Visitor_ID = m.Visitor_ID
LEFT JOIN activity AS a
    ON a.Visitor_ID = m.Visitor_ID
GROUP BY
    v.Visitor_ID,
    v.First_Name,
    v.Last_Name
ORDER BY Activity_Rank, v.Visitor_ID;

/* ---------------------------------------------------------------------
   Q11. Revenue per month (paid bills) with a running total.
        Technique: CTE, DATEFROMPARTS, window function SUM() OVER.
   --------------------------------------------------------------------- */
WITH monthly AS (
    SELECT
        DATEFROMPARTS(YEAR(Invoice_Date), MONTH(Invoice_Date), 1) AS Revenue_Month,
        SUM(Amount_Paid)                                          AS Revenue
    FROM dbo.BILLING_LOG
    WHERE Billing_Status = 'Paid'
    GROUP BY DATEFROMPARTS(YEAR(Invoice_Date), MONTH(Invoice_Date), 1)
)
SELECT
    Revenue_Month,
    Revenue,
    SUM(Revenue) OVER (ORDER BY Revenue_Month
                       ROWS UNBOUNDED PRECEDING) AS Running_Total
FROM monthly
ORDER BY Revenue_Month;

/* ---------------------------------------------------------------------
   Q12. Members who paid more than the average member (members who paid
        nothing count as 0 in the average).
        Technique: CTE used twice, scalar subquery with AVG.
   --------------------------------------------------------------------- */
WITH member_paid AS (
    SELECT
        v.Visitor_ID,
        v.First_Name + ' ' + v.Last_Name AS Member_Name,
        COALESCE(SUM(CASE WHEN b.Billing_Status = 'Paid'
                          THEN b.Amount_Paid END), 0.00) AS Total_Paid
    FROM dbo.MEMBER AS m
    INNER JOIN dbo.VISITOR AS v
        ON v.Visitor_ID = m.Visitor_ID
    LEFT JOIN dbo.BILLING_LOG AS b
        ON b.Visitor_ID = v.Visitor_ID
    GROUP BY
        v.Visitor_ID,
        v.First_Name,
        v.Last_Name
)
SELECT
    Visitor_ID,
    Member_Name,
    Total_Paid
FROM member_paid
WHERE Total_Paid > (SELECT AVG(Total_Paid) FROM member_paid)
ORDER BY Total_Paid DESC;

/* ---------------------------------------------------------------------
   Q13. Audit: two sessions that overlap in the same room or with the same
        trainer. The rules BR-SCH-03 and BR-SCH-04 forbid this, so the
        result must be EMPTY (0 rows).
        Overlap test: each session starts before the other one ends
        (times are half-open: a session may start when the previous one ends).
        Technique: self-join.
   --------------------------------------------------------------------- */
SELECT
    s1.Session_ID AS Session_A,
    s2.Session_ID AS Session_B,
    s1.Room_ID,
    s1.Trainer_ID
FROM dbo.[SESSION] AS s1
INNER JOIN dbo.[SESSION] AS s2
    ON s1.Session_ID < s2.Session_ID
   AND s1.Start_Time < s2.End_Time
   AND s2.Start_Time < s1.End_Time
WHERE s1.Room_ID = s2.Room_ID
   OR s1.Trainer_ID = s2.Trainer_ID;
GO

/* ---------------------------------------------------------------------
   Performance check (optional). Turn on the statistics, run a query, and
   read the Messages tab: "logical reads" = pages read, "elapsed time" in ms.
   Compare the numbers before and after creating the indexes of script 03,
   or on the larger data set of script 06 (see script 07).
   --------------------------------------------------------------------- */
/*
SET STATISTICS IO ON;
SET STATISTICS TIME ON;
-- run Q13 here
SET STATISTICS IO OFF;
SET STATISTICS TIME OFF;
*/

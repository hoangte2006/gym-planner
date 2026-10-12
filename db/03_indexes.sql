/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   INT1313 - Database Basics
   Phase 3 - Script 03: indexes

   Run after 01_create_tables.sql (and 04_sample_data.sql).
   PRIMARY KEY and UNIQUE constraints already create their own indexes;
   the ones below are extra.
   ===================================================================== */

USE GymPlanner;
GO

/* ---------------------------------------------------------------------
   BR-MEM-10: a member can have at most one membership with Status = 'Active'.
   A filtered unique index: uniqueness is checked only on the 'Active' rows,
   so many 'Expired' or 'Cancelled' rows of the same member are allowed.
   --------------------------------------------------------------------- */
CREATE UNIQUE NONCLUSTERED INDEX UX_MEMBERSHIP_One_Active
    ON dbo.MEMBERSHIP (Visitor_ID)
    WHERE Status = 'Active';
GO

/* ---------------------------------------------------------------------
   NFR-PER: speed up the overlap checks of BR-SCH-03 and BR-SCH-04, which
   search SESSION by room or trainer and by time range.
   --------------------------------------------------------------------- */
CREATE NONCLUSTERED INDEX IX_SESSION_Room_Time
    ON dbo.[SESSION] (Room_ID, Start_Time, End_Time);
GO

CREATE NONCLUSTERED INDEX IX_SESSION_Trainer_Time
    ON dbo.[SESSION] (Trainer_ID, Start_Time, End_Time);
GO

/* ---------------------------------------------------------------------
   Foreign-key columns that are searched often but are not the first column
   of a primary key: "classes of a member", "PT bookings of a member",
   "reservations of a session".
   --------------------------------------------------------------------- */
CREATE NONCLUSTERED INDEX IX_CLASS_REG_Visitor
    ON dbo.CLASS_REGISTRATION (Visitor_ID);
GO

CREATE NONCLUSTERED INDEX IX_PT_BOOKING_Visitor
    ON dbo.PT_BOOKING (Visitor_ID);
GO

CREATE NONCLUSTERED INDEX IX_SEU_Session
    ON dbo.SESSION_EQUIPMENT_USAGE (Session_ID);
GO

-- List the indexes that were created by this script
SELECT OBJECT_NAME(i.object_id) AS On_Table, i.name AS Index_Name,
       i.is_unique, i.has_filter, i.filter_definition
FROM sys.indexes i
WHERE i.name IN ('UX_MEMBERSHIP_One_Active', 'IX_SESSION_Room_Time', 'IX_SESSION_Trainer_Time',
                 'IX_CLASS_REG_Visitor', 'IX_PT_BOOKING_Visitor', 'IX_SEU_Session')
ORDER BY On_Table, Index_Name;
GO

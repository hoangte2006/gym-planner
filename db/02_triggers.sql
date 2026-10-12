/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   INT1313 - Database Basics
   Phase 3 - Script 02: triggers for the business rules

   Run order: 01_create_tables.sql -> 04_sample_data.sql -> 02_triggers.sql
              -> 03_indexes.sql -> 05_trigger_tests.sql

   How every trigger below works
   - SQL Server has no BEFORE trigger. These are AFTER triggers: the row is
     written first, then the trigger checks the rule. If the rule is broken,
     the trigger undoes the whole statement (ROLLBACK) and raises an error
     with THROW (error numbers 50001 and up).
   - "inserted" is a temporary table holding the new version of the rows
     being inserted or updated. A trigger fires once per statement, so it
     checks all affected rows at once.

   Rule -> trigger map
   BR-MEM-01  trg_MEMBER_Disjoint, trg_DAILY_PASS_Disjoint
   BR-MEM-03  trg_MEMBERSHIP_PhotoRequired, trg_CLASS_REG_PhotoRequired,
              trg_PT_BOOKING_PhotoRequired
   BR-MEM-07  trg_BILLING_MembershipOwner   (Visitor_ID matches the membership)
   BR-SCH-01  trg_TRAINER_Disjoint, trg_RECEPTIONIST_Disjoint,
              trg_TRAINER_RoleMatch, trg_RECEPTIONIST_RoleMatch,
              trg_STAFF_RoleMatch
   BR-SCH-02  trg_CLASS_RoomCapacity, trg_SESSION_RoomCapacity,
              trg_ROOM_RoomCapacity, trg_CLASS_REG_SeatLimit
   BR-SCH-03/04  trg_SESSION_NoOverlap,
              trg_PT_BOOKING_RecheckOverlap
   BR-SCH-05  trg_CLASS_REG_ActiveMembership
   BR-SCH-06  trg_CLASS_SessionType, trg_PT_BOOKING_SessionType,
              trg_SESSION_TypeLocked
   BR-SCH-07 / BR-FAC-06  trg_SEU_EquipmentActive
   BR-SCH-08  trg_SEU_TimeCheck, trg_SESSION_ReservationsInside
              (and the equipment check inside trg_PT_BOOKING_RecheckOverlap)
   BR-FAC-05  trg_MLOG_SignerRole
   BR-MEM-10  filtered unique index (script 03, not a trigger)
   ===================================================================== */

USE GymPlanner;
GO

/* =====================================================================
   BR-MEM-01: a visitor is a MEMBER or a DAILY_PASS_TICKET holder, never both
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_MEMBER_Disjoint
ON dbo.MEMBER
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.DAILY_PASS_TICKET d ON d.Visitor_ID = i.Visitor_ID)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50001, 'BR-MEM-01: this visitor already holds a daily pass, so cannot also be a member.', 1;
    END
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_DAILY_PASS_Disjoint
ON dbo.DAILY_PASS_TICKET
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.MEMBER m ON m.Visitor_ID = i.Visitor_ID)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50002, 'BR-MEM-01: this visitor is already a member, so cannot also hold a daily pass.', 1;
    END
END;
GO

/* =====================================================================
   BR-MEM-03: a member needs a photo on file before buying a membership,
   registering for a class, or booking a PT session
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_MEMBERSHIP_PhotoRequired
ON dbo.MEMBERSHIP
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.MEMBER m ON m.Visitor_ID = i.Visitor_ID
               WHERE m.Photo IS NULL)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50003, 'BR-MEM-03: the member has no photo on file; a membership cannot be created yet.', 1;
    END
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_CLASS_REG_PhotoRequired
ON dbo.CLASS_REGISTRATION
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.MEMBER m ON m.Visitor_ID = i.Visitor_ID
               WHERE m.Photo IS NULL)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50004, 'BR-MEM-03: the member has no photo on file; cannot register for a class yet.', 1;
    END
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_PT_BOOKING_PhotoRequired
ON dbo.PT_BOOKING
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.MEMBER m ON m.Visitor_ID = i.Visitor_ID
               WHERE m.Photo IS NULL)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50005, 'BR-MEM-03: the member has no photo on file; cannot book a PT session yet.', 1;
    END
END;
GO

/* =====================================================================
   BR-MEM-07: a bill that points to a membership must carry the Visitor_ID
   of that membership's owner (BILLING_LOG.Visitor_ID is controlled
   redundancy, see the normalization section)
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_BILLING_MembershipOwner
ON dbo.BILLING_LOG
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.MEMBERSHIP ms ON ms.Membership_ID = i.Membership_ID
               WHERE i.Membership_ID IS NOT NULL
                 AND ms.Visitor_ID <> i.Visitor_ID)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50006, 'BR-MEM-07: BILLING_LOG.Visitor_ID must be the owner of the membership in Membership_ID.', 1;
    END
END;
GO

/* =====================================================================
   BR-SCH-01: a staff member is a TRAINER or a RECEPTIONIST, never both
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_TRAINER_Disjoint
ON dbo.TRAINER
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.RECEPTIONIST r ON r.Staff_ID = i.Staff_ID)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50007, 'BR-SCH-01: this staff member is already a receptionist, so cannot also be a trainer.', 1;
    END
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_RECEPTIONIST_Disjoint
ON dbo.RECEPTIONIST
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.TRAINER t ON t.Staff_ID = i.Staff_ID)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50008, 'BR-SCH-01: this staff member is already a trainer, so cannot also be a receptionist.', 1;
    END
END;
GO

/* BR-SCH-01 (role must match the subclass table) */
CREATE OR ALTER TRIGGER dbo.trg_TRAINER_RoleMatch
ON dbo.TRAINER
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.STAFF s ON s.Staff_ID = i.Staff_ID
               WHERE s.Role <> 'Trainer')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50026, 'BR-SCH-01: only staff with Role = ''Trainer'' can have a TRAINER row.', 1;
    END
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_RECEPTIONIST_RoleMatch
ON dbo.RECEPTIONIST
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.STAFF s ON s.Staff_ID = i.Staff_ID
               WHERE s.Role <> 'Receptionist')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50027, 'BR-SCH-01: only staff with Role = ''Receptionist'' can have a RECEPTIONIST row.', 1;
    END
END;
GO

-- Changing STAFF.Role must not leave a subclass row with a different role.
CREATE OR ALTER TRIGGER dbo.trg_STAFF_RoleMatch
ON dbo.STAFF
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(Role)
       AND EXISTS (SELECT 1 FROM inserted i
                   WHERE (i.Role <> 'Trainer'      AND EXISTS (SELECT 1 FROM dbo.TRAINER t      WHERE t.Staff_ID = i.Staff_ID))
                      OR (i.Role <> 'Receptionist' AND EXISTS (SELECT 1 FROM dbo.RECEPTIONIST r WHERE r.Staff_ID = i.Staff_ID)))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50028, 'BR-SCH-01: the new Role does not match the subclass table that already holds this staff member.', 1;
    END
END;
GO

/* =====================================================================
   BR-SCH-02 (part 1): CLASS.Max_Capacity cannot exceed the room's Capacity.
   Three triggers cover the three ways the comparison can change:
   a class is inserted/updated, its session moves to another room,
   or the room's Capacity is lowered.
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_CLASS_RoomCapacity
ON dbo.CLASS
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i
               JOIN dbo.[SESSION] s ON s.Session_ID = i.Session_ID
               JOIN dbo.ROOM r      ON r.Room_ID    = s.Room_ID
               WHERE i.Max_Capacity > r.Capacity)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50009, 'BR-SCH-02: CLASS.Max_Capacity cannot be larger than the Capacity of the room.', 1;
    END
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_SESSION_RoomCapacity
ON dbo.[SESSION]
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(Room_ID)
       AND EXISTS (SELECT 1 FROM inserted i
                   JOIN dbo.CLASS c ON c.Session_ID = i.Session_ID
                   JOIN dbo.ROOM r  ON r.Room_ID    = i.Room_ID
                   WHERE c.Max_Capacity > r.Capacity)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50010, 'BR-SCH-02: the new room is too small for the class Max_Capacity.', 1;
    END
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_ROOM_RoomCapacity
ON dbo.ROOM
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(Capacity)
       AND EXISTS (SELECT 1 FROM inserted i
                   JOIN dbo.[SESSION] s ON s.Room_ID    = i.Room_ID
                   JOIN dbo.CLASS c     ON c.Session_ID = s.Session_ID
                   WHERE c.Max_Capacity > i.Capacity)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50011, 'BR-SCH-02: a class in this room has a Max_Capacity larger than the new room Capacity.', 1;
    END
END;
GO

/* BR-SCH-02 (part 2): registrations that hold a seat cannot exceed Max_Capacity.
   A registration holds a seat unless Attendance_Status = 'Cancelled'
   ('Confirmed' and 'Attended' both count). */
CREATE OR ALTER TRIGGER dbo.trg_CLASS_REG_SeatLimit
ON dbo.CLASS_REGISTRATION
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1
               FROM dbo.CLASS c
               WHERE c.Session_ID IN (SELECT Session_ID FROM inserted)
                 AND (SELECT COUNT(*)
                      FROM dbo.CLASS_REGISTRATION r
                      WHERE r.Session_ID = c.Session_ID
                        AND r.Attendance_Status <> 'Cancelled') > c.Max_Capacity)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50012, 'BR-SCH-02: registration failed, class capacity reached.', 1;
    END
END;
GO

/* =====================================================================
   BR-SCH-03 and BR-SCH-04: no overlapping sessions in the same room or
   for the same trainer.
   Two intervals [S1,E1) and [S2,E2) overlap when S1 < E2 AND S2 < E1.
   A session whose PT_BOOKING row is 'Cancelled' is ignored (its room and
   trainer slot are free).

   How to read the checks below: a pair of sessions is a CONFLICT only when
   ALL conditions are true (same room or trainer, times overlap, neither
   session is a cancelled PT). If any single condition is false, the pair
   is not a conflict and the row is accepted.
   Names: new_s   = the session just added or changed,
          other_s = every other session already in the table.
   ===================================================================== */
-- Older versions of this script used a helper function; it is no longer needed.
DROP FUNCTION IF EXISTS dbo.fn_SessionConflicts;
GO

CREATE OR ALTER TRIGGER dbo.trg_SESSION_NoOverlap
ON dbo.[SESSION]
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- BR-SCH-03: a room cannot hold two sessions at the same time
    IF EXISTS (
        SELECT 1
        FROM inserted AS new_s                              -- the session just added or changed
        JOIN dbo.[SESSION] AS other_s                       -- every other session
             ON other_s.Session_ID <> new_s.Session_ID      -- not itself
        WHERE other_s.Room_ID = new_s.Room_ID               -- same room
          AND new_s.Start_Time < other_s.End_Time           -- times overlap
          AND other_s.Start_Time < new_s.End_Time
          AND NOT EXISTS (SELECT 1 FROM dbo.PT_BOOKING b    -- new session is not a cancelled PT
                          WHERE b.Session_ID = new_s.Session_ID
                            AND b.Booking_Status = 'Cancelled')
          AND NOT EXISTS (SELECT 1 FROM dbo.PT_BOOKING b    -- other session is not a cancelled PT
                          WHERE b.Session_ID = other_s.Session_ID
                            AND b.Booking_Status = 'Cancelled')
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50013, 'BR-SCH-03: Room Schedule Conflict. The room is already used during this time.', 1;
    END

    -- BR-SCH-04: a trainer cannot lead two sessions at the same time
    IF EXISTS (
        SELECT 1
        FROM inserted AS new_s
        JOIN dbo.[SESSION] AS other_s
             ON other_s.Session_ID <> new_s.Session_ID
        WHERE other_s.Trainer_ID = new_s.Trainer_ID         -- same trainer
          AND new_s.Start_Time < other_s.End_Time
          AND other_s.Start_Time < new_s.End_Time
          AND NOT EXISTS (SELECT 1 FROM dbo.PT_BOOKING b
                          WHERE b.Session_ID = new_s.Session_ID
                            AND b.Booking_Status = 'Cancelled')
          AND NOT EXISTS (SELECT 1 FROM dbo.PT_BOOKING b
                          WHERE b.Session_ID = other_s.Session_ID
                            AND b.Booking_Status = 'Cancelled')
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50014, 'BR-SCH-04: Trainer Schedule Conflict. The trainer is already busy during this time.', 1;
    END
END;
GO

/* =====================================================================
   A cancelled PT booking frees its room, trainer and equipment, so another
   session may take that slot. If the booking is later restored (status goes
   from 'Cancelled' back to 'Scheduled' or 'Completed'), the checks must be
   run again, because the slot may have been taken in the meantime.
   In this trigger the "restored" booking is the row in "inserted" whose old
   status (in "deleted") was 'Cancelled' and whose new status is not.
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_PT_BOOKING_RecheckOverlap
ON dbo.PT_BOOKING
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT UPDATE(Booking_Status) RETURN;       -- nothing to check if the status did not change

    -- BR-SCH-03: another session took the room while this booking was cancelled
    IF EXISTS (
        SELECT 1
        FROM inserted AS i                                          -- booking after the update
        JOIN deleted  AS d ON d.Session_ID = i.Session_ID           -- same booking before the update
        JOIN dbo.[SESSION] AS restored_s ON restored_s.Session_ID = i.Session_ID
        JOIN dbo.[SESSION] AS other_s                               -- every other session
             ON other_s.Session_ID <> restored_s.Session_ID
        WHERE d.Booking_Status = 'Cancelled'                        -- it was cancelled ...
          AND i.Booking_Status <> 'Cancelled'                       -- ... and is not any more
          AND other_s.Room_ID = restored_s.Room_ID                  -- same room
          AND restored_s.Start_Time < other_s.End_Time              -- times overlap
          AND other_s.Start_Time < restored_s.End_Time
          AND NOT EXISTS (SELECT 1 FROM dbo.PT_BOOKING b            -- the other session is not a cancelled PT
                          WHERE b.Session_ID = other_s.Session_ID
                            AND b.Booking_Status = 'Cancelled')
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50015, 'BR-SCH-03: Room Schedule Conflict. The slot was taken after this booking was cancelled.', 1;
    END

    -- BR-SCH-04: another session took the trainer while this booking was cancelled
    IF EXISTS (
        SELECT 1
        FROM inserted AS i
        JOIN deleted  AS d ON d.Session_ID = i.Session_ID
        JOIN dbo.[SESSION] AS restored_s ON restored_s.Session_ID = i.Session_ID
        JOIN dbo.[SESSION] AS other_s
             ON other_s.Session_ID <> restored_s.Session_ID
        WHERE d.Booking_Status = 'Cancelled'
          AND i.Booking_Status <> 'Cancelled'
          AND other_s.Trainer_ID = restored_s.Trainer_ID            -- same trainer
          AND restored_s.Start_Time < other_s.End_Time
          AND other_s.Start_Time < restored_s.End_Time
          AND NOT EXISTS (SELECT 1 FROM dbo.PT_BOOKING b
                          WHERE b.Session_ID = other_s.Session_ID
                            AND b.Booking_Status = 'Cancelled')
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50016, 'BR-SCH-04: Trainer Schedule Conflict. The trainer took another session after this booking was cancelled.', 1;
    END

    -- BR-SCH-08: the equipment reserved for this session may have been reserved by someone else meanwhile
    IF EXISTS (
        SELECT 1
        FROM inserted AS i
        JOIN deleted  AS d ON d.Session_ID = i.Session_ID
        JOIN dbo.SESSION_EQUIPMENT_USAGE AS mine                    -- reservations of the restored session
             ON mine.Session_ID = i.Session_ID
        JOIN dbo.SESSION_EQUIPMENT_USAGE AS theirs                  -- other reservations of the same machine
             ON  theirs.Equipment_ID = mine.Equipment_ID
             AND theirs.Usage_ID <> mine.Usage_ID
        WHERE d.Booking_Status = 'Cancelled'
          AND i.Booking_Status <> 'Cancelled'
          AND mine.Reserved_From < theirs.Reserved_To               -- times overlap
          AND theirs.Reserved_From < mine.Reserved_To
          AND NOT EXISTS (SELECT 1 FROM dbo.PT_BOOKING b            -- that other reservation is not from a cancelled PT
                          WHERE b.Session_ID = theirs.Session_ID
                            AND b.Booking_Status = 'Cancelled')
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50029, 'BR-SCH-08: the equipment of this booking was reserved by another session after the booking was cancelled.', 1;
    END
END;
GO

/* =====================================================================
   BR-SCH-05: only members with an active membership can register for a class.
   Active = Status 'Active' and today between Start_Date and End_Date.
   Reads as: reject if some registered member has NO such active membership.
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_CLASS_REG_ActiveMembership
ON dbo.CLASS_REGISTRATION
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted AS new_reg
               WHERE NOT EXISTS (SELECT 1 FROM dbo.MEMBERSHIP AS ms
                                 WHERE ms.Visitor_ID = new_reg.Visitor_ID
                                   AND ms.Status = 'Active'
                                   AND CAST(GETDATE() AS DATE) BETWEEN ms.Start_Date AND ms.End_Date))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50017, 'BR-SCH-05: the member has no active membership, so cannot register for a class.', 1;
    END
END;
GO

/* =====================================================================
   BR-SCH-06: every SESSION is a CLASS or a PT_BOOKING, never both.
   Session_Type tells which one; the subclass tables must agree with it.
   (Total participation = every SESSION row gets its subclass row in the
   same transaction; this is done by the application / insert script.)
   ===================================================================== */
-- A CLASS row is allowed only for a 'Class' session that has no PT booking.
CREATE OR ALTER TRIGGER dbo.trg_CLASS_SessionType
ON dbo.CLASS
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted AS new_class
               JOIN dbo.[SESSION] AS sess ON sess.Session_ID = new_class.Session_ID
               WHERE sess.Session_Type <> 'Class')              -- the session is not a Class session
       OR EXISTS (SELECT 1 FROM inserted AS new_class
                  JOIN dbo.PT_BOOKING AS pt ON pt.Session_ID = new_class.Session_ID)   -- or it already is a PT booking
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50018, 'BR-SCH-06: this session is not a Class session (or is already a PT booking).', 1;
    END
END;
GO

-- A PT_BOOKING row is allowed only for a 'PT' session that is not a class.
CREATE OR ALTER TRIGGER dbo.trg_PT_BOOKING_SessionType
ON dbo.PT_BOOKING
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted AS new_pt
               JOIN dbo.[SESSION] AS sess ON sess.Session_ID = new_pt.Session_ID
               WHERE sess.Session_Type <> 'PT')                 -- the session is not a PT session
       OR EXISTS (SELECT 1 FROM inserted AS new_pt
                  JOIN dbo.CLASS AS cls ON cls.Session_ID = new_pt.Session_ID)         -- or it already is a class
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50019, 'BR-SCH-06: this session is not a PT session (or is already a Class).', 1;
    END
END;
GO

-- Session_Type cannot be changed once the subclass row exists.
CREATE OR ALTER TRIGGER dbo.trg_SESSION_TypeLocked
ON dbo.[SESSION]
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(Session_Type)
       AND EXISTS (SELECT 1 FROM inserted AS new_s
                   JOIN deleted AS old_s ON old_s.Session_ID = new_s.Session_ID
                   WHERE new_s.Session_Type <> old_s.Session_Type          -- the type was changed
                     AND (EXISTS (SELECT 1 FROM dbo.CLASS      AS cls WHERE cls.Session_ID = new_s.Session_ID)
                       OR EXISTS (SELECT 1 FROM dbo.PT_BOOKING AS pt  WHERE pt.Session_ID  = new_s.Session_ID)))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50020, 'BR-SCH-06: Session_Type cannot be changed after the Class or PT_BOOKING row exists.', 1;
    END
END;
GO

/* =====================================================================
   BR-SCH-07 and BR-FAC-06: new reservations only for 'Active' equipment
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_SEU_EquipmentActive
ON dbo.SESSION_EQUIPMENT_USAGE
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted AS new_usage
               JOIN dbo.EQUIPMENT AS eq ON eq.Equipment_ID = new_usage.Equipment_ID
               WHERE eq.Operational_Status <> 'Active')
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50021, 'BR-SCH-07: this equipment is Broken or Under Maintenance and cannot be reserved.', 1;
    END
END;
GO

/* =====================================================================
   BR-SCH-08: equipment reservation time check
   (a) the reservation lies inside the time of its session;
   (b) no other reservation of the same machine overlaps it
       (reservations of sessions whose PT booking is 'Cancelled' are ignored).
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_SEU_TimeCheck
ON dbo.SESSION_EQUIPMENT_USAGE
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- (a) the reservation must start no earlier and end no later than its session
    IF EXISTS (SELECT 1 FROM inserted AS new_usage
               JOIN dbo.[SESSION] AS sess ON sess.Session_ID = new_usage.Session_ID
               WHERE new_usage.Reserved_From < sess.Start_Time
                  OR new_usage.Reserved_To   > sess.End_Time)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50023, 'BR-SCH-08: the reservation must lie inside the time of its session.', 1;
    END

    -- (b) the same machine cannot be reserved twice at the same time
    IF EXISTS (
        SELECT 1
        FROM inserted AS new_usage                                  -- the reservation just added or changed
        JOIN dbo.SESSION_EQUIPMENT_USAGE AS other_usage             -- other reservations of the same machine
             ON  other_usage.Equipment_ID = new_usage.Equipment_ID
             AND other_usage.Usage_ID <> new_usage.Usage_ID
        WHERE new_usage.Reserved_From < other_usage.Reserved_To     -- times overlap
          AND other_usage.Reserved_From < new_usage.Reserved_To
          AND NOT EXISTS (SELECT 1 FROM dbo.PT_BOOKING b            -- the other reservation is not from a cancelled PT
                          WHERE b.Session_ID = other_usage.Session_ID
                            AND b.Booking_Status = 'Cancelled')
          AND NOT EXISTS (SELECT 1 FROM dbo.PT_BOOKING b            -- the new reservation is not from a cancelled PT
                          WHERE b.Session_ID = new_usage.Session_ID
                            AND b.Booking_Status = 'Cancelled')
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50024, 'BR-SCH-08: this equipment is already reserved by another session during this time.', 1;
    END
END;
GO

-- Changing a session's time must not leave a reservation outside it.
CREATE OR ALTER TRIGGER dbo.trg_SESSION_ReservationsInside
ON dbo.[SESSION]
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF (UPDATE(Start_Time) OR UPDATE(End_Time))
       AND EXISTS (SELECT 1 FROM inserted AS new_s
                   JOIN dbo.SESSION_EQUIPMENT_USAGE AS usage_row ON usage_row.Session_ID = new_s.Session_ID
                   WHERE usage_row.Reserved_From < new_s.Start_Time
                      OR usage_row.Reserved_To   > new_s.End_Time)
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50025, 'BR-SCH-08: an equipment reservation of this session would fall outside the new session time.', 1;
    END
END;
GO

/* =====================================================================
   BR-FAC-05: a maintenance log is signed off by a Receptionist or a Manager
   ===================================================================== */
CREATE OR ALTER TRIGGER dbo.trg_MLOG_SignerRole
ON dbo.MAINTENANCE_LOG
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted AS new_log
               JOIN dbo.STAFF AS signer ON signer.Staff_ID = new_log.Logged_By
               WHERE signer.Role NOT IN ('Receptionist', 'Manager'))
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50022, 'BR-FAC-05: a maintenance log must be signed off by a Receptionist or a Manager.', 1;
    END
END;
GO

-- List the triggers that were created
SELECT t.name AS Trigger_Name, OBJECT_NAME(t.parent_id) AS On_Table
FROM sys.triggers t
WHERE t.parent_class = 1
ORDER BY OBJECT_NAME(t.parent_id), t.name;
GO

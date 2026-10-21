/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   INT1313 - Database Basics
   Phase 3 - Script 10: role-based access control (RBAC)

   Run after 01, 04, 02, 03 and 08. Safe to run again.
   Run it with an administrator login (for example sa or a Windows
   administrator): it creates logins, users and roles.

   Idea
   - A LOGIN is an account of the SQL Server (what a person types to connect).
   - A USER is that login inside the database GymPlanner.
   - A ROLE is a named set of permissions. Permissions are given to roles,
     never to single users, so a new employee only needs to join a role.
   - GRANT gives a permission, DENY forbids it (DENY always wins over
     GRANT), REVOKE removes an earlier GRANT or DENY.
   - Least privilege: every role gets only what its work needs.

   Role             Who                Short summary of the permissions
   role_Manager     manager            read and change everything, except
                                       deleting bills; cannot change the design
   role_Receptionist front desk        members, memberships, bills, bookings;
                                       cannot see STAFF (salary)
   role_Trainer     trainers           read the schedule, mark attendance;
                                       cannot see bills, phones or salaries
   role_Member      members (portal)   read the class schedule, trainer list
                                       and plans only

   The demo logins and passwords below are for the course project only.
   CHECK_POLICY = OFF is used (as for the login gym_app in
   db/setup_local_sqlserver.ps1) because the Windows password policy refuses a
   password that contains a part of the login name ("gym"). In a real
   system keep CHECK_POLICY = ON and use strong, different passwords.

   Code style (SQLStyle.guide): SQL keywords in capitals, one object per line.
   ===================================================================== */

USE GymPlanner;
GO

/* ---------------------------------------------------------------------
   1) Logins (server level) - one demo account per role
   --------------------------------------------------------------------- */
IF SUSER_ID(N'gym_manager') IS NULL
    CREATE LOGIN gym_manager   WITH PASSWORD = N'Gym@Manager2026',   CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF, DEFAULT_DATABASE = GymPlanner;
IF SUSER_ID(N'gym_reception') IS NULL
    CREATE LOGIN gym_reception WITH PASSWORD = N'Gym@Reception2026', CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF, DEFAULT_DATABASE = GymPlanner;
IF SUSER_ID(N'gym_trainer') IS NULL
    CREATE LOGIN gym_trainer   WITH PASSWORD = N'Gym@Trainer2026',   CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF, DEFAULT_DATABASE = GymPlanner;
IF SUSER_ID(N'gym_member') IS NULL
    CREATE LOGIN gym_member    WITH PASSWORD = N'Gym@Member2026',    CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF, DEFAULT_DATABASE = GymPlanner;
GO

/* ---------------------------------------------------------------------
   2) Users (database level), one per login
   --------------------------------------------------------------------- */
IF DATABASE_PRINCIPAL_ID(N'gym_manager') IS NULL
    CREATE USER gym_manager   FOR LOGIN gym_manager;
IF DATABASE_PRINCIPAL_ID(N'gym_reception') IS NULL
    CREATE USER gym_reception FOR LOGIN gym_reception;
IF DATABASE_PRINCIPAL_ID(N'gym_trainer') IS NULL
    CREATE USER gym_trainer   FOR LOGIN gym_trainer;
IF DATABASE_PRINCIPAL_ID(N'gym_member') IS NULL
    CREATE USER gym_member    FOR LOGIN gym_member;
GO

/* ---------------------------------------------------------------------
   3) Roles
   --------------------------------------------------------------------- */
IF DATABASE_PRINCIPAL_ID(N'role_Manager') IS NULL
    CREATE ROLE role_Manager;
IF DATABASE_PRINCIPAL_ID(N'role_Receptionist') IS NULL
    CREATE ROLE role_Receptionist;
IF DATABASE_PRINCIPAL_ID(N'role_Trainer') IS NULL
    CREATE ROLE role_Trainer;
IF DATABASE_PRINCIPAL_ID(N'role_Member') IS NULL
    CREATE ROLE role_Member;
GO

/* ---------------------------------------------------------------------
   4) Put each user in its role
   --------------------------------------------------------------------- */
IF IS_ROLEMEMBER(N'role_Manager', N'gym_manager') = 0
    ALTER ROLE role_Manager ADD MEMBER gym_manager;
IF IS_ROLEMEMBER(N'role_Receptionist', N'gym_reception') = 0
    ALTER ROLE role_Receptionist ADD MEMBER gym_reception;
IF IS_ROLEMEMBER(N'role_Trainer', N'gym_trainer') = 0
    ALTER ROLE role_Trainer ADD MEMBER gym_trainer;
IF IS_ROLEMEMBER(N'role_Member', N'gym_member') = 0
    ALTER ROLE role_Member ADD MEMBER gym_member;
GO

/* =====================================================================
   5) Permissions
   Views are in the same schema (dbo) and have the same owner as the tables,
   so a role may read a view without any permission on the tables behind it
   ("ownership chaining"). This is how role_Member sees trainer names but
   not the STAFF table with salaries.
   ===================================================================== */

/* ---- role_Manager: all data, no design changes ---- */
GRANT SELECT, INSERT, UPDATE, DELETE ON SCHEMA::dbo TO role_Manager;
-- Bills are never deleted (the foreign keys of BILLING_LOG use ON DELETE
-- RESTRICT for the same reason). DENY wins over the GRANT above.
DENY DELETE ON dbo.BILLING_LOG TO role_Manager;
GO

/* ---- role_Receptionist: front-desk work ---- */
GRANT SELECT, INSERT, UPDATE ON dbo.VISITOR             TO role_Receptionist;
GRANT SELECT, INSERT, UPDATE ON dbo.MEMBER              TO role_Receptionist;
GRANT SELECT, INSERT, UPDATE ON dbo.DAILY_PASS_TICKET   TO role_Receptionist;
GRANT SELECT, INSERT, UPDATE ON dbo.MEMBERSHIP          TO role_Receptionist;
GRANT SELECT, INSERT, UPDATE ON dbo.BILLING_LOG         TO role_Receptionist;
GRANT SELECT, INSERT, UPDATE ON dbo.CLASS_REGISTRATION  TO role_Receptionist;
GRANT SELECT, INSERT, UPDATE ON dbo.PT_BOOKING          TO role_Receptionist;
GRANT SELECT, INSERT         ON dbo.MAINTENANCE_LOG     TO role_Receptionist;
GRANT SELECT                 ON dbo.MEMBERSHIP_PLAN         TO role_Receptionist;
GRANT SELECT                 ON dbo.ROOM                    TO role_Receptionist;
GRANT SELECT                 ON dbo.[SESSION]               TO role_Receptionist;
GRANT SELECT                 ON dbo.CLASS                   TO role_Receptionist;
GRANT SELECT                 ON dbo.EQUIPMENT               TO role_Receptionist;
GRANT SELECT                 ON dbo.SESSION_EQUIPMENT_USAGE TO role_Receptionist;
GRANT SELECT ON dbo.vw_Active_Membership      TO role_Receptionist;
GRANT SELECT ON dbo.vw_Class_Schedule         TO role_Receptionist;
GRANT SELECT ON dbo.vw_Member_Billing_Summary TO role_Receptionist;
GRANT SELECT ON dbo.vw_Trainer_Public         TO role_Receptionist;
GRANT SELECT ON dbo.vw_Equipment_Status       TO role_Receptionist;
-- A receptionist reports a broken machine: only the status column may change.
GRANT UPDATE (Operational_Status) ON dbo.EQUIPMENT TO role_Receptionist;
GO

-- Tighten the permission on bills with REVOKE: the table-wide UPDATE is
-- removed, then only the columns that record a payment are allowed. A
-- receptionist can no longer change the invoice date, due date or owner.
REVOKE UPDATE ON dbo.BILLING_LOG FROM role_Receptionist;
GRANT UPDATE (Payment_Date, Amount_Paid, Billing_Status) ON dbo.BILLING_LOG TO role_Receptionist;
GO

/* ---- role_Trainer: schedule and attendance ---- */
GRANT SELECT ON dbo.[SESSION]               TO role_Trainer;
GRANT SELECT ON dbo.CLASS                   TO role_Trainer;
GRANT SELECT ON dbo.PT_BOOKING              TO role_Trainer;
GRANT SELECT ON dbo.CLASS_REGISTRATION      TO role_Trainer;
GRANT SELECT ON dbo.ROOM                    TO role_Trainer;
GRANT SELECT ON dbo.EQUIPMENT               TO role_Trainer;
GRANT SELECT ON dbo.SESSION_EQUIPMENT_USAGE TO role_Trainer;
GRANT SELECT ON dbo.vw_Class_Schedule       TO role_Trainer;
GRANT SELECT ON dbo.vw_Trainer_Public       TO role_Trainer;
-- A trainer sees the names of the people in a session, but not their phone.
GRANT SELECT (Visitor_ID, First_Name, Last_Name) ON dbo.VISITOR TO role_Trainer;
-- A trainer marks attendance and the result of a PT booking, nothing else.
GRANT UPDATE (Attendance_Status) ON dbo.CLASS_REGISTRATION TO role_Trainer;
GRANT UPDATE (Booking_Status)    ON dbo.PT_BOOKING         TO role_Trainer;
GO

/* ---- role_Member: public information only ---- */
GRANT SELECT ON dbo.vw_Class_Schedule TO role_Member;
GRANT SELECT ON dbo.vw_Trainer_Public TO role_Member;
GRANT SELECT ON dbo.MEMBERSHIP_PLAN   TO role_Member;
GO

/* ---------------------------------------------------------------------
   6) List the permissions that were created (for the report)
   --------------------------------------------------------------------- */
SELECT
    pr.name                                              AS Role_Name,
    pe.state_desc                                        AS State,
    pe.permission_name                                   AS Permission,
    CASE pe.class
        WHEN 3 THEN 'SCHEMA::' + SCHEMA_NAME(pe.major_id)
        WHEN 1 THEN OBJECT_NAME(pe.major_id)
    END                                                  AS On_Object,
    COL_NAME(pe.major_id, pe.minor_id)                   AS On_Column
FROM sys.database_permissions AS pe
INNER JOIN sys.database_principals AS pr
    ON pr.principal_id = pe.grantee_principal_id
WHERE pr.name LIKE N'role[_]%'
ORDER BY pr.name, On_Object, pe.permission_name;
GO

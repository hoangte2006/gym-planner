/* =====================================================================
   Gym Membership & Personal Training Planner  -  Team Dong Vui (#9)
   INT1313 - Database Basics
   Phase 3 - Script 01: create database and 17 tables (DDL)
   Target: Microsoft SQL Server (T-SQL), run in SSMS
   Source: Report section 2.2 (schema), 2.3 (FK policy), 4 (Data Dictionary)

   Notes
   - SQL Server has no RESTRICT keyword: RESTRICT in the report = NO ACTION here.
   - ON UPDATE is NO ACTION everywhere (all PKs are IDENTITY and never updated).
   - Triggers and indexes are in later scripts (02, 03).
   ===================================================================== */

-- 1) Create the database (only if it does not exist yet)
IF DB_ID(N'GymPlanner') IS NULL
    CREATE DATABASE GymPlanner;
GO

USE GymPlanner;
GO

/* ---------------------------------------------------------------------
   Optional reset: remove all tables so the script can be run again.
   Uncomment ONLY when you want to delete everything and start over.
   --------------------------------------------------------------------- */
-- DROP TABLE IF EXISTS dbo.SESSION_EQUIPMENT_USAGE;
-- DROP TABLE IF EXISTS dbo.MAINTENANCE_LOG;
-- DROP TABLE IF EXISTS dbo.CLASS_REGISTRATION;
-- DROP TABLE IF EXISTS dbo.PT_BOOKING;
-- DROP TABLE IF EXISTS dbo.CLASS;
-- DROP TABLE IF EXISTS dbo.[SESSION];
-- DROP TABLE IF EXISTS dbo.EQUIPMENT;
-- DROP TABLE IF EXISTS dbo.ROOM;
-- DROP TABLE IF EXISTS dbo.RECEPTIONIST;
-- DROP TABLE IF EXISTS dbo.TRAINER;
-- DROP TABLE IF EXISTS dbo.STAFF;
-- DROP TABLE IF EXISTS dbo.BILLING_LOG;
-- DROP TABLE IF EXISTS dbo.MEMBERSHIP;
-- DROP TABLE IF EXISTS dbo.MEMBERSHIP_PLAN;
-- DROP TABLE IF EXISTS dbo.DAILY_PASS_TICKET;
-- DROP TABLE IF EXISTS dbo.MEMBER;
-- DROP TABLE IF EXISTS dbo.VISITOR;
-- GO

/* =====================================================================
   MEMBER & BILLING DOMAIN
   ===================================================================== */

-- Table 1: VISITOR (superclass)
CREATE TABLE dbo.VISITOR (
    Visitor_ID  INT IDENTITY(1,1) NOT NULL,
    First_Name  VARCHAR(50)       NOT NULL,
    Last_Name   VARCHAR(50)       NOT NULL,
    Phone       VARCHAR(15)       NOT NULL,
    CONSTRAINT PK_VISITOR       PRIMARY KEY (Visitor_ID),
    CONSTRAINT UQ_VISITOR_Phone UNIQUE (Phone)
);
GO

-- Table 2: MEMBER (subclass of VISITOR)
CREATE TABLE dbo.MEMBER (
    Visitor_ID        INT            NOT NULL,
    Email             VARCHAR(100)   NOT NULL,
    Birth_Date        DATE           NULL,
    Photo             VARBINARY(MAX) NULL,
    Registration_Date DATETIME       NOT NULL
        CONSTRAINT DF_MEMBER_RegDate DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_MEMBER       PRIMARY KEY (Visitor_ID),
    CONSTRAINT UQ_MEMBER_Email UNIQUE (Email),
    CONSTRAINT FK_MEMBER_VISITOR FOREIGN KEY (Visitor_ID)
        REFERENCES dbo.VISITOR (Visitor_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION
);
GO

-- Table 3: DAILY_PASS_TICKET (subclass of VISITOR)
CREATE TABLE dbo.DAILY_PASS_TICKET (
    Visitor_ID INT           NOT NULL,
    Visit_Date DATE          NOT NULL
        CONSTRAINT DF_DPT_VisitDate DEFAULT CAST(GETDATE() AS DATE),
    Daily_Rate DECIMAL(10,2) NOT NULL
        CONSTRAINT DF_DPT_Rate DEFAULT 15.00,
    CONSTRAINT PK_DAILY_PASS_TICKET PRIMARY KEY (Visitor_ID),
    CONSTRAINT FK_DPT_VISITOR FOREIGN KEY (Visitor_ID)
        REFERENCES dbo.VISITOR (Visitor_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT CK_DPT_Rate CHECK (Daily_Rate > 0.00)
);
GO

-- Table 4: MEMBERSHIP_PLAN
CREATE TABLE dbo.MEMBERSHIP_PLAN (
    Plan_ID         INT IDENTITY(1,1) NOT NULL,
    Plan_Name       VARCHAR(50)       NOT NULL,
    Duration_Months INT               NOT NULL
        CONSTRAINT DF_PLAN_Duration DEFAULT 1,
    Price           DECIMAL(10,2)     NOT NULL
        CONSTRAINT DF_PLAN_Price DEFAULT 0.00,
    Description     VARCHAR(255)      NULL,
    CONSTRAINT PK_MEMBERSHIP_PLAN      PRIMARY KEY (Plan_ID),
    CONSTRAINT UQ_PLAN_Name            UNIQUE (Plan_Name),
    CONSTRAINT CK_PLAN_Duration        CHECK (Duration_Months > 0),
    CONSTRAINT CK_PLAN_Price           CHECK (Price >= 0.00)
);
GO

-- Table 5: MEMBERSHIP
CREATE TABLE dbo.MEMBERSHIP (
    Membership_ID INT IDENTITY(1,1) NOT NULL,
    Visitor_ID    INT               NOT NULL,
    Plan_ID       INT               NOT NULL,
    Start_Date    DATE              NOT NULL,
    End_Date      DATE              NOT NULL,
    Status        VARCHAR(20)       NOT NULL
        CONSTRAINT DF_MEMBERSHIP_Status DEFAULT 'Active',
    CONSTRAINT PK_MEMBERSHIP PRIMARY KEY (Membership_ID),
    CONSTRAINT FK_MEMBERSHIP_MEMBER FOREIGN KEY (Visitor_ID)
        REFERENCES dbo.MEMBER (Visitor_ID)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_MEMBERSHIP_PLAN FOREIGN KEY (Plan_ID)
        REFERENCES dbo.MEMBERSHIP_PLAN (Plan_ID)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_MEMBERSHIP_Dates  CHECK (End_Date > Start_Date),
    CONSTRAINT CK_MEMBERSHIP_Status CHECK (Status IN ('Active','Expired','Cancelled'))
);
GO

-- Table 6: BILLING_LOG
CREATE TABLE dbo.BILLING_LOG (
    Bill_ID        INT IDENTITY(1,1) NOT NULL,
    Visitor_ID     INT               NOT NULL,
    Membership_ID  INT               NULL,
    Invoice_Date   DATETIME          NOT NULL
        CONSTRAINT DF_BILL_InvoiceDate DEFAULT CURRENT_TIMESTAMP,
    Due_Date       DATE              NOT NULL,
    Payment_Date   DATETIME          NULL,
    Amount_Paid    DECIMAL(10,2)     NOT NULL
        CONSTRAINT DF_BILL_Amount DEFAULT 0.00,
    Billing_Status VARCHAR(20)       NOT NULL
        CONSTRAINT DF_BILL_Status DEFAULT 'Pending',
    CONSTRAINT PK_BILLING_LOG PRIMARY KEY (Bill_ID),
    CONSTRAINT FK_BILL_VISITOR FOREIGN KEY (Visitor_ID)
        REFERENCES dbo.VISITOR (Visitor_ID)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_BILL_MEMBERSHIP FOREIGN KEY (Membership_ID)
        REFERENCES dbo.MEMBERSHIP (Membership_ID)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_BILL_Amount CHECK (Amount_Paid >= 0.00),
    CONSTRAINT CK_BILL_Status CHECK (Billing_Status IN ('Pending','Paid','Failed'))
);
GO

/* =====================================================================
   STAFF
   ===================================================================== */

-- Table 7: STAFF (superclass)
CREATE TABLE dbo.STAFF (
    Staff_ID  INT IDENTITY(1,1) NOT NULL,
    Full_Name VARCHAR(100)      NOT NULL,
    Phone     VARCHAR(15)       NOT NULL,
    Role      VARCHAR(30)       NOT NULL
        CONSTRAINT DF_STAFF_Role DEFAULT 'Receptionist',
    Hire_Date DATE              NOT NULL
        CONSTRAINT DF_STAFF_HireDate DEFAULT CAST(GETDATE() AS DATE),
    Salary    DECIMAL(10,2)     NOT NULL
        CONSTRAINT DF_STAFF_Salary DEFAULT 500.00,
    CONSTRAINT PK_STAFF       PRIMARY KEY (Staff_ID),
    CONSTRAINT UQ_STAFF_Phone UNIQUE (Phone),
    CONSTRAINT CK_STAFF_Role   CHECK (Role IN ('Trainer','Receptionist','Manager')),
    CONSTRAINT CK_STAFF_Salary CHECK (Salary > 0.00)
);
GO

-- Table 8: TRAINER (subclass of STAFF)
CREATE TABLE dbo.TRAINER (
    Staff_ID      INT          NOT NULL,
    Specialty     VARCHAR(50)  NOT NULL
        CONSTRAINT DF_TRAINER_Specialty DEFAULT 'General',
    Certification VARCHAR(100) NULL,
    CONSTRAINT PK_TRAINER PRIMARY KEY (Staff_ID),
    CONSTRAINT FK_TRAINER_STAFF FOREIGN KEY (Staff_ID)
        REFERENCES dbo.STAFF (Staff_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION
);
GO

-- Table 9: RECEPTIONIST (subclass of STAFF)
CREATE TABLE dbo.RECEPTIONIST (
    Staff_ID   INT         NOT NULL,
    Shift_Type VARCHAR(20) NOT NULL
        CONSTRAINT DF_RECEP_Shift DEFAULT 'Morning',
    CONSTRAINT PK_RECEPTIONIST PRIMARY KEY (Staff_ID),
    CONSTRAINT FK_RECEP_STAFF FOREIGN KEY (Staff_ID)
        REFERENCES dbo.STAFF (Staff_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT CK_RECEP_Shift CHECK (Shift_Type IN ('Morning','Evening','Night'))
);
GO

/* =====================================================================
   ROOMS, SESSIONS, CLASSES, PT BOOKINGS
   ===================================================================== */

-- Table 10: ROOM
CREATE TABLE dbo.ROOM (
    Room_ID        INT IDENTITY(1,1) NOT NULL,
    Room_Name      VARCHAR(50)       NOT NULL,
    Capacity       INT               NOT NULL
        CONSTRAINT DF_ROOM_Capacity DEFAULT 20,
    Floor_Location VARCHAR(30)       NULL
        CONSTRAINT DF_ROOM_Floor DEFAULT 'Floor 1',
    CONSTRAINT PK_ROOM      PRIMARY KEY (Room_ID),
    CONSTRAINT UQ_ROOM_Name UNIQUE (Room_Name),
    CONSTRAINT CK_ROOM_Capacity CHECK (Capacity > 0)
);
GO

-- Table 11: SESSION (superclass of CLASS and PT_BOOKING)
CREATE TABLE dbo.[SESSION] (
    Session_ID   INT IDENTITY(1,1) NOT NULL,
    Session_Type VARCHAR(10)       NOT NULL,
    Room_ID      INT               NOT NULL,
    Trainer_ID   INT               NOT NULL,
    Start_Time   DATETIME          NOT NULL,
    End_Time     DATETIME          NOT NULL,
    CONSTRAINT PK_SESSION PRIMARY KEY (Session_ID),
    CONSTRAINT FK_SESSION_ROOM FOREIGN KEY (Room_ID)
        REFERENCES dbo.ROOM (Room_ID)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_SESSION_TRAINER FOREIGN KEY (Trainer_ID)
        REFERENCES dbo.TRAINER (Staff_ID)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_SESSION_Type CHECK (Session_Type IN ('Class','PT')),
    CONSTRAINT CK_SESSION_Time CHECK (End_Time > Start_Time)
);
GO

-- Table 12: CLASS (subclass of SESSION)
CREATE TABLE dbo.CLASS (
    Session_ID   INT         NOT NULL,
    Class_Name   VARCHAR(50) NOT NULL,
    Max_Capacity INT         NOT NULL
        CONSTRAINT DF_CLASS_MaxCap DEFAULT 20,
    CONSTRAINT PK_CLASS PRIMARY KEY (Session_ID),
    CONSTRAINT FK_CLASS_SESSION FOREIGN KEY (Session_ID)
        REFERENCES dbo.[SESSION] (Session_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT CK_CLASS_MaxCap CHECK (Max_Capacity > 0)
);
GO

-- Table 13: CLASS_REGISTRATION (composite PK)
CREATE TABLE dbo.CLASS_REGISTRATION (
    Session_ID        INT         NOT NULL,
    Visitor_ID        INT         NOT NULL,
    Registration_Date DATETIME    NOT NULL
        CONSTRAINT DF_CREG_Date DEFAULT CURRENT_TIMESTAMP,
    Attendance_Status VARCHAR(20) NOT NULL
        CONSTRAINT DF_CREG_Status DEFAULT 'Confirmed',
    CONSTRAINT PK_CLASS_REGISTRATION PRIMARY KEY (Session_ID, Visitor_ID),
    CONSTRAINT FK_CREG_CLASS FOREIGN KEY (Session_ID)
        REFERENCES dbo.CLASS (Session_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT FK_CREG_MEMBER FOREIGN KEY (Visitor_ID)
        REFERENCES dbo.MEMBER (Visitor_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT CK_CREG_Status CHECK (Attendance_Status IN ('Confirmed','Attended','Cancelled'))
);
GO

-- Table 14: PT_BOOKING (subclass of SESSION)
CREATE TABLE dbo.PT_BOOKING (
    Session_ID     INT         NOT NULL,
    Visitor_ID     INT         NOT NULL,
    Booking_Status VARCHAR(20) NOT NULL
        CONSTRAINT DF_PT_Status DEFAULT 'Scheduled',
    CONSTRAINT PK_PT_BOOKING PRIMARY KEY (Session_ID),
    CONSTRAINT FK_PT_SESSION FOREIGN KEY (Session_ID)
        REFERENCES dbo.[SESSION] (Session_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT FK_PT_MEMBER FOREIGN KEY (Visitor_ID)
        REFERENCES dbo.MEMBER (Visitor_ID)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_PT_Status CHECK (Booking_Status IN ('Scheduled','Completed','Cancelled'))
);
GO

/* =====================================================================
   FACILITY & MAINTENANCE DOMAIN
   ===================================================================== */

-- Table 15: EQUIPMENT
CREATE TABLE dbo.EQUIPMENT (
    Equipment_ID       INT IDENTITY(1,1) NOT NULL,
    Equipment_Name     VARCHAR(100)      NOT NULL,
    Room_ID            INT               NULL,
    Operational_Status VARCHAR(20)       NOT NULL
        CONSTRAINT DF_EQUIP_Status DEFAULT 'Active',
    Purchase_Date      DATE              NULL,
    CONSTRAINT PK_EQUIPMENT PRIMARY KEY (Equipment_ID),
    CONSTRAINT FK_EQUIP_ROOM FOREIGN KEY (Room_ID)
        REFERENCES dbo.ROOM (Room_ID)
        ON DELETE SET NULL ON UPDATE NO ACTION,
    CONSTRAINT CK_EQUIP_Status CHECK (Operational_Status IN ('Active','Under Maintenance','Broken'))
);
GO

-- Table 16: MAINTENANCE_LOG (weak entity, composite PK)
CREATE TABLE dbo.MAINTENANCE_LOG (
    Equipment_ID     INT           NOT NULL,
    Log_no           INT           NOT NULL,
    Maintenance_Date DATE          NOT NULL
        CONSTRAINT DF_MLOG_Date DEFAULT CAST(GETDATE() AS DATE),
    Cost             DECIMAL(10,2) NOT NULL
        CONSTRAINT DF_MLOG_Cost DEFAULT 0.00,
    Description      VARCHAR(255)  NOT NULL,
    Logged_By        INT           NOT NULL,
    CONSTRAINT PK_MAINTENANCE_LOG PRIMARY KEY (Equipment_ID, Log_no),
    CONSTRAINT FK_MLOG_EQUIPMENT FOREIGN KEY (Equipment_ID)
        REFERENCES dbo.EQUIPMENT (Equipment_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT FK_MLOG_STAFF FOREIGN KEY (Logged_By)
        REFERENCES dbo.STAFF (Staff_ID)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT CK_MLOG_Cost CHECK (Cost >= 0.00)
);
GO

-- Table 17: SESSION_EQUIPMENT_USAGE
CREATE TABLE dbo.SESSION_EQUIPMENT_USAGE (
    Usage_ID      INT IDENTITY(1,1) NOT NULL,
    Equipment_ID  INT               NOT NULL,
    Session_ID    INT               NOT NULL,
    Reserved_From DATETIME          NOT NULL,
    Reserved_To   DATETIME          NOT NULL,
    CONSTRAINT PK_SESSION_EQUIPMENT_USAGE PRIMARY KEY (Usage_ID),
    CONSTRAINT FK_SEU_EQUIPMENT FOREIGN KEY (Equipment_ID)
        REFERENCES dbo.EQUIPMENT (Equipment_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT FK_SEU_SESSION FOREIGN KEY (Session_ID)
        REFERENCES dbo.[SESSION] (Session_ID)
        ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT CK_SEU_Time CHECK (Reserved_To > Reserved_From)
);
GO

-- 2) Quick check: list the 17 tables that were created
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
GO

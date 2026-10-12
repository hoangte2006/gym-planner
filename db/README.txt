GymPlanner - SQL Server database (Team Dong Vui, INT1313, project #9)

Requirements: SQL Server (2016 or newer) and SSMS.
Open each file in SSMS and press Execute (F5), in this order:

  1. 01_create_tables.sql   create database GymPlanner and 17 tables
  2. 04_sample_data.sql     sample data (small)
  3. 02_triggers.sql        25 triggers (business rules)
  4. 03_indexes.sql         6 indexes
  5. 05_trigger_tests.sql   30 rule tests (rolled back, changes nothing)
  6. 06_bulk_data.sql       optional: about 38,800 extra rows
  7. 07_scale_checks.sql    optional: tests on the large data set

To remove the extra rows of step 6: run 06b_remove_bulk_data.sql.
To start again from zero: uncomment the DROP TABLE lines at the top of 01.

Notes
- Rule BR-SCH-05 uses today's date. The sample memberships are valid until
  2027-09-01 (member 1), and the 'Active' memberships of script 06 end
  between 2027-03-01 and 2027-10-01. Run scripts 06/07 before 2027-03-01,
  or the class registrations of some members are rejected as expired.
- Scripts 04-07 start with SET DATEFORMAT ymd, so dates are read the same
  way whatever the language setting of the SQL Server login.

GymPlanner - SQL scripts (Team Dong Vui, project #9, INT1313)
SQL Server 2016 SP1 or newer. Run each script in SSMS (or VS Code) in this order:

  01_create_tables.sql   database + 17 tables
  04_sample_data.sql     sample data
  02_triggers.sql        25 triggers (business rules)
  03_indexes.sql         6 indexes
  08_views.sql           5 views
  05_trigger_tests.sql   30 rule tests (expect 28 Blocked, 2 Accepted)
  09_advanced_queries.sql  13 queries (run on the sample data only)
  10_security_rbac.sql   4 demo logins/users/roles + GRANT/DENY/REVOKE (run as administrator)
  11_security_tests.sql  19 access tests (expect 19 PASS)

Optional (large data set): 06_bulk_data.sql, 07_scale_checks.sql, 06b_remove_bulk_data.sql (removes 06).
Run 09 before 06 or after 06b.

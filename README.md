# Gym Membership & Personal Training Planner

Đồ án Lập trình hướng đối tượng (Java Swing) – nhóm Đồng Vui, đề tài số 9.

## Yêu cầu
- JDK 17 trở lên
- Maven 3.9+
- SQL Server (Express được), database `GymPlanner` đã tạo từ các script trong `db/`

## Cài đặt (làm một lần)
1. **Tạo CSDL**: chạy lần lượt `db/01_create_tables.sql`, `02_triggers.sql`, `03_indexes.sql`, `04_sample_data.sql`.
2. **Cấu hình SQL Server cho Java** (cần quyền Administrator): mở PowerShell bằng *Run as administrator*, rồi
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\db\setup_local_sqlserver.ps1
   ```
   Script bật TCP/IP cổng 1433, bật đăng nhập SQL Server và tạo login `gym_app`.
3. **Tạo file cấu hình**: sao chép `config.example.properties` thành `config.local.properties`
   và điền `db.password` (trùng mật khẩu đã dùng ở bước 2). File này không được commit.

## Chạy
```bash
mvn verify                       # biên dịch + test + đóng gói JAR
java -jar target/gym-planner-0.1.0.jar
```
Hoặc chạy lớp `com.gym.app.Main` trong IDE.

## Cấu trúc
| Package | Chủ sở hữu | Nội dung |
|---|---|---|
| `core/`, `app/`, `dashboard/`, `pom.xml` | TV1 | Core dùng chung, khung ứng dụng, dashboard |
| `member/` | TV2 | Hội viên |
| `billing/` | TV3 | Gói tập & thanh toán |
| `facility/` | TV4 | Nhân sự & cơ sở vật chất |
| `scheduling/` | TV5 | Lịch tập |

Luồng gọi một chiều: UI → Service → DAO → DB. Xem `docs/ke_hoach/` để biết quy trình Git và phân công.

# Gym Membership & Personal Training Planner

Đồ án Lập trình hướng đối tượng (Java Swing) – nhóm Đồng Vui, đề tài số 9.

## Yêu cầu
- JDK 17 trở lên
- Maven 3.9+
- SQL Server (Express được), database `GymPlanner` đã tạo từ các script trong `db/`

## Cài đặt (làm một lần)
1. **Tạo CSDL** (mở từng file trong SSMS và chạy, theo đúng thứ tự):
   `db/01_create_tables.sql` → `04_sample_data.sql` → `02_triggers.sql` → `03_indexes.sql` → `08_views.sql`
   → kiểm tra bằng `05_trigger_tests.sql` (kết quả đúng: **28 Blocked + 2 Accepted**).
2. **Phân quyền SQL** (script 10 và 11, chạy bằng quyền Administrator): `db/10_security_rbac.sql` rồi
   `db/11_security_tests.sql` (kết quả đúng: **19 PASS**). Script 09 chỉ là truy vấn tham khảo cho dashboard,
   chạy trên dữ liệu mẫu (không cần chạy khi cài đặt).
3. **Cấu hình SQL Server cho Java** (cần quyền Administrator): mở PowerShell bằng *Run as administrator*, rồi
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\db\setup_local_sqlserver.ps1
   ```
   Script bật TCP/IP cổng 1433, bật đăng nhập SQL Server và tạo login `gym_app`
   (phải chạy sau script 01 vì cần database `GymPlanner` đã tồn tại).
4. **Tạo file cấu hình**: sao chép `config.example.properties` thành `config.local.properties`
   và điền `db.password` (trùng mật khẩu đã dùng ở bước 3). File này không được commit.

## Dữ liệu demo (trước buổi thử và buổi trình bày)
Dữ liệu mẫu có ngày cố định nên qua vài ngày các màn hình "gói sắp hết hạn", "lớp hôm nay", "hóa đơn quá hạn" sẽ trống.
Chạy `db/12_demo_data.sql` (SSMS hoặc sqlcmd) để dời toàn bộ ngày của dữ liệu mẫu về sát hôm nay:
buổi Yoga Flow 07:00 thành hôm nay, các gói hết hạn sau 3, 8 và 20 ngày, hóa đơn quá hạn còn nguyên.
Chạy lại lúc nào cũng được (cùng ngày thì không làm gì). Chỉ đổi dòng dữ liệu mẫu (script 04),
không đụng dữ liệu lớn (script 06) hay dữ liệu do ứng dụng tạo.

## Đăng nhập
Đăng nhập bằng **số điện thoại nhân viên** (cột `STAFF.Phone`) và mật khẩu chung, mặc định `gym123`
(đổi được qua `AuthService.changePassword`). Ví dụ: Quản lý `0901000005`, Lễ tân `0901000003`, HLV `0901000001`.

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

Luồng gọi một chiều: UI → Service → DAO → DB. Xem `docs/ke_hoach/` để biết quy trình Git và phân công, và **`docs/CORE_GUIDE.md`** để biết cách dùng DAO, UiKit,
Validator, ErrorTranslator khi viết module.

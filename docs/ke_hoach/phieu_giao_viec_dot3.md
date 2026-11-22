# Phiếu giao việc ĐỢT 3 (26/10 12:00 → 30/10 23:59)

Gửi: TV2 Trí · TV3 Tiến · TV4 Quân · TV5 Ku Líx. Người gửi: TV1.
Mục tiêu đợt này: **luật nghiệp vụ, giải thuật, UX và unit test** của từng module, để TV1 gắn tag **v0.8 – đủ chức năng (CN 01/11)**.

**Tình hình hiện tại của `main` (26/10):** đã có Core, Dashboard 8 chỉ số, trang chủ, module Hội viên (Trí) và module Gói tập & Thanh toán (Tiến); `mvn clean verify` xanh với 137 test.
**Chưa vào `main`:** module Lịch tập (Ku Líx) và module Nhân sự & Cơ sở vật chất (Quân). Vì vậy **chưa gắn được tag v0.5** (kế hoạch yêu cầu cả 4 module mở được từ menu). Hai bạn ở mục TV4 và TV5 làm phần "Sửa trước" và mở PR sớm nhất có thể.

## 0. Việc chung (ai cũng làm trước khi code)

1. `git fetch` rồi `git merge origin/main` vào nhánh của mình (không rebase nhánh đã đẩy lên chung).
2. **Chạy `mvn clean verify` xanh trước khi đẩy** (dùng `clean`, không dùng `mvn compile` vì có thể báo thành công giả). Nhánh không biên dịch được sẽ không được merge.
3. **Luật nộp bài:**
   - **Không sửa file trong `core/`.** Cần truy vấn riêng thì tạo lớp con trong package của mình (ví dụ `facility/dao/FacilityQueryDao extends EquipmentDao`), xem `docs/CORE_GUIDE.md` mục "Cần truy vấn riêng ngoài danh sách trên?". Thiếu hàm dùng chung: mở Issue `core-request` (TV1 xử lý trong 24 giờ).
   - **Core không được `import` từ module nào** (Core là tầng dưới, không biết module).
   - UI chỉ gọi **Service**, không `new XxxDao()` trong `ui/`. Việc gọi DB chạy bằng `Async.run(...)`.
   - **Service tự kiểm tra quyền theo vai trò** (UI ẩn nút chỉ là lớp ngoài). Chưa đăng nhập hoặc sai vai trò thì ném `BusinessException`. Mẫu: `DashboardService.requireAccess`.
   - Thao tác ghi **nhiều DAO** phải bọc trong `Tx.run(...)`. Thao tác chỉ ghi một DAO thì không cần.
   - SQL luôn dùng `?`. Lỗi hiện bằng tiếng Việt qua `ErrorTranslator`/`BusinessException`.
   - PR nhỏ (khoảng 400 dòng trở xuống), tên commit `feat(module): mô tả` (không đặt tên kiểu `nhiem-vu-tuan-1`). Có ảnh chụp màn hình trong PR.
   - Test **không giả định lượng dữ liệu**; test dùng DB phải tự dọn dữ liệu của mình (mẫu: `DashboardDaoTest`, đo trước, thêm đúng một dữ liệu đã biết, đo lại, rồi dọn).
4. **Mốc:** Draft PR từ **Thứ Tư 28/10** để TV1 góp ý sớm · Hạn nộp PR **Thứ Sáu 30/10, 23:59** · Họp 15 phút **Thứ Năm 29/10** (mỗi người nói tiến độ và demo module).
5. Việc nào không kịp thì báo **trước Thứ Tư 28/10** để cắt việc phụ hoặc chuyển người.
6. Cần thêm cột/chỉ mục trong DB: Issue `db-request`.
7. TV1 rà soát code đã merge theo danh sách TECH-01…TECH-11 và ghi kết quả vào GitHub Issues cho từng chủ module.

---

## TV2 – Hữu Trí: Hội viên (module `member/`)

Đợt 2 đã merge và đạt (đã sửa Tx, Validator, ErrorTranslator, kiểm tra quyền, có test).

**Sửa nhỏ trước:** `MemberService.checkPermission()` hiện **cho qua khi chưa đăng nhập**. Phải chặn luôn trường hợp này (`Session.getRole()` trả về Huấn luyện viên khi chưa đăng nhập, nên chỉ cần kiểm tra vai trò, không cần điều kiện `isLoggedIn()`). Thêm test cho ca này.

**Việc đợt 3:**
1. Hồ sơ hội viên đủ **5 tab**; mỗi tab có đường dẫn nhanh tới màn hình của module khác (mở bằng `ModuleRegistry`, **không import trực tiếp** module khác).
2. **Nâng cấp vé ngày thành hội viên** trong một `Tx`, có hộp thoại xác nhận nêu rõ hệ quả (hỏi xác nhận **trước** khi gọi `Tx`, không đợi người dùng bấm nút bên trong `Tx`).
3. Xuất CSV danh sách hội viên, **UTF-8 có BOM** để Excel đọc đúng tiếng Việt (mẫu: `dashboard/service/DashboardCsvWriter`).
4. UX-11, 12, 17 (trạng thái rỗng, phím tắt, nút "?") và checklist UX-01…UX-17 cho mọi màn hình của module.
5. ≥ 10 unit test (đã có 4): bổ sung `PhotoUtils`, luật kiểm tra dữ liệu, nâng cấp vé ngày.

**Xong khi:** đủ chức năng trong phạm vi; qua checklist UX cho mọi màn hình; ≥ 10 unit test xanh. **Tag mục tiêu:** v0.8.

---

## TV3 – Đức Tiến: Gói tập & Thanh toán (module `billing/`)

Đợt 2 đã merge. Đã có sẵn nhiều thứ của đợt 3 (gia hạn, tự hết hạn, màn hình sắp hết hạn, xuất PDF hóa đơn, báo cáo): đợt này là **kiểm chứng và hoàn thiện**.

**Sửa trước (từ review):**
1. **Service chưa kiểm tra quyền** (`BillingService`, `PlanService`...). Thêm kiểm tra vai trò Quản lý/Lễ tân ở Service và test cho ca bị chặn.
2. **Chưa có test chạm DB** cho `BillingTransactionDao` (bán gói, hủy gói, gia hạn là nghiệp vụ nhạy cảm). Viết test theo mẫu "đo trước, thêm, đo lại, dọn".
3. `BillingTransactionDao` tự giữ kết nối riêng nên không chạy chung `Tx` bên ngoài. Giữ được, nhưng ghi rõ lý do trong chú thích đầu lớp.

**Việc đợt 3:**
1. Gia hạn gói theo ALG-04 (gói cũ chuyển Expired rồi tạo gói mới, giữ ngày chưa dùng); thử đủ 3 kịch bản: còn hạn, hết hạn hôm nay, đã hết 10 ngày.
2. Tự hết hạn khi mở app và mỗi 60 phút; màn hình "Sắp hết hạn 7/30 ngày" (vàng/đỏ + chữ).
3. Kiểm tra mọi luật ALG-05 (Paid là trạng thái cuối, số tiền khớp giá gói).
4. UX-01…UX-17 cho mọi màn hình; báo cáo doanh thu theo gói, công nợ (dùng `vw_Member_Billing_Summary`).
5. ≥ 12 unit test (ngày, trạng thái hóa đơn, gia hạn).

**Xong khi:** đủ chức năng trong phạm vi; gia hạn 3 kịch bản cho kết quả ngày đúng; ≥ 12 unit test xanh. **Tag mục tiêu:** v0.8.

---

## TV4 – Hồ Qui Quân: Nhân sự & Cơ sở vật chất (module `facility/`)

Nhánh `feature/facility` build và test xanh, UI đã đi qua Service. **Nhưng chưa vào `main`** vì còn các lỗi dưới đây.

**Sửa trước (bắt buộc, rồi mở PR):**
1. **Không sửa `core/`.** Nhánh đang sửa 5 file Core:
   - `core/dao/FacilityQueries.java` (file mới): chuyển sang `facility/dao/`.
   - `StaffDao`: `findByNormalizedPhone`, `findDuplicateByNormalizedPhone`: chuyển sang `facility/dao/StaffQueryDao extends StaffDao`.
   - `RoomDao.findRoomsPaged`: **xóa**, Core đã có `findPage` (trả `PageResult` có cả tổng số dòng); hàm của bạn còn nhân `(page-1)*pageSize` không kiểm tra tràn số.
   - `EquipmentDao`: hoàn lại (chỉ thêm một dòng trống).
   - `FormPanel.addComponent`: mở Issue `core-request`, TV1 xử lý trong 24 giờ.
   - Sau khi chuyển code xong, hoàn lại Core về bản `main`: `git checkout origin/main -- src/main/java/com/gym/core`.
2. **Service chưa kiểm tra quyền** (module chỉ dành cho Quản lý; chỉ `MaintenanceLogService` có kiểm tra người ký). Thêm kiểm tra vai trò ở mọi Service và test cho ca bị chặn.
3. **Đặt lại tên commit** (`nv-tuan1`, `nhiem-vu-tuan-2`...) theo dạng `feat(facility): ...` khi mở PR.
4. `StaffService.createStaff`: `TrainerDao`/`ReceptionistDao` đã tự ghi hai bảng trong một transaction nên không cần thêm `Tx`; chỉ dùng `Tx` khi ghi qua **nhiều DAO khác nhau** trong một thao tác.

**Việc đợt 3:**
1. Đổi trạng thái thiết bị sang Under Maintenance/Broken → hiện **danh sách lịch đặt tương lai bị ảnh hưởng** (ALG-07). Chặn xóa nhân sự/phòng/thiết bị đang được tham chiếu, kèm lý do bằng tiếng Việt.
2. Hồ sơ huấn luyện viên (lịch sắp tới, số session); **không hạ sức chứa phòng dưới `Max_Capacity` của lớp**.
3. UX-01…UX-17. Màn hình "Sức khỏe thiết bị" (hỏng hoặc quá 90 ngày chưa bảo trì, dùng `vw_Equipment_Status`) là mục làm thêm nếu còn thời gian.
4. Truy vấn thiết bị khả dụng theo khung giờ (`findAvailableEquipment`) là dùng chung với TV5 (đặt thiết bị cho session): mở Issue `core-request` để TV1 đưa vào `EquipmentDao` của Core thay vì để trong module của bạn.
5. ≥ 10 unit test (Log_no, thiết bị khả dụng, chống xóa); đã có 31.

**Xong khi:** đủ chức năng trong phạm vi; mọi ca "không được xóa" đều có thông báo hướng dẫn; ≥ 10 unit test xanh. **Tag mục tiêu:** v0.8.

---

## TV5 – Ku Líx: Lịch tập (module `scheduling/`)

Phần đã làm tốt: lịch tuần, đặt lớp, đặt PT, tìm giờ trống, Service có kiểm tra quyền, `SchedulingServicesTest`. **Nhưng nhánh `feature/scheduling` đang không biên dịch được**, nên chưa merge được.

**Sửa trước (bắt buộc, rồi mở PR):**
1. **Build hỏng:** `map(ResultSet) is not public in ClassSessionDao` (và `PtSessionDao`). Nguyên nhân: chuyển `SessionDao` sang `scheduling.dao` nên không còn gọi được các hàm chỉ để mức cùng package của Core.
2. **Core đang `import com.gym.scheduling...`** (trong `ClassSessionDao`, `PtSessionDao`, `DaoIntegrationTest`): Core không được phụ thuộc vào module.
3. **Cách sửa đúng:** để `SessionDao` ở `core/dao` như `main`, hoàn lại mọi file Core đã sửa (`git checkout origin/main -- src/main/java/com/gym/core`), rồi tạo `scheduling/dao/SchedulingSessionDao extends SessionDao` chứa các hàm tìm xung đột (`findPotentialConflicts`, `findRoomConflicts`, `findTrainerConflicts`). Gộp ba hàm gần giống nhau thành một nếu được.
4. `ClassWizardPanel` còn dùng `RoomDao`/`TrainerDao`: đã chuyển qua Service (tốt), kiểm tra lại không còn `import com.gym.core.dao` nào trong `ui/`.
5. Các Service mới (`AvailabilityService`, `ClassSchedulingService`, `PtSchedulingService`) cần kiểm tra quyền và test cho ca bị chặn.

**Việc đợt 3:**
1. Sửa/dời session (chạy lại mọi kiểm tra); trạng thái PT Scheduled → Completed/Cancelled, khôi phục từ Cancelled có kiểm tra lại (lỗi 50015, 50016, 50029).
2. Đặt thiết bị cho session (dùng truy vấn thiết bị khả dụng, xem mục TV4 ý 4), chặn thiết bị hỏng/bảo trì.
3. **Khóa dòng khi đếm sĩ số (ALG-03, `UPDLOCK`)**; xử lý ca hai lễ tân giành chỗ cuối.
4. Xóa session đúng luật (cảnh báo số đăng ký bị xóa kèm); UX-01…UX-17.
5. ≥ 15 unit test (chồng lấn, sĩ số, vòng đời trạng thái). Màn hình "Lịch của tôi" cho Trainer chuyển sang đợt 4.

**Xong khi:** đủ chức năng lõi của module (trừ Lịch của tôi); qua mọi ca trong `05_trigger_tests.sql` ở mức ứng dụng; ≥ 15 unit test xanh. **Tag mục tiêu:** v0.8.

---

## Việc của TV1 trong đợt này

- Duyệt PR trong 24 giờ; chỉ sửa lỗi Core nhỏ và tương thích ngược.
- Xử lý `core-request` (đưa `findAvailableEquipment` vào Core, `FormPanel.addComponent` nếu hợp lý).
- Khi cả 4 module đã vào `main`: chạy hồi quy (script `db/05`, `db/11`, mở từng module) rồi gắn **tag v0.5**.
- Rà soát TECH-01…TECH-11 trên mã đã merge, ghi kết quả vào Issues.
- Dashboard 8 chỉ số, lọc thời gian, xuất CSV/PDF, trang chủ: **đã xong**, viết unit test và hoàn thiện khi cần.

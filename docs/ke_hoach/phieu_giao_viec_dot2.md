# Phiếu giao việc ĐỢT 2 (19/10 12:00 → 23/10 23:59)

Gửi: TV2 Trí · TV3 Tiến · TV4 Quân · TV5 Ku Líx. Người gửi: TV1.
Mục tiêu đợt này: mỗi module có **luồng chính chạy với DB thật** để TV1 gắn tag **v0.5 (CN 25/10)**.

## 0. Việc chung (ai cũng làm trước khi code)

1. `git fetch` rồi `git merge origin/main` vào nhánh của mình (không rebase nhánh đã đẩy lên chung). Main đã có thêm:
   - **`Tx`**: giao dịch dùng chung cho nhiều DAO (xem `docs/CORE_GUIDE.md`, mục "Tx").
   - `ErrorTranslator` dịch thêm lỗi thiếu quyền (229, 230, 262).
   - `db/12_demo_data.sql`: dời ngày dữ liệu mẫu về sát hôm nay.
2. Chạy `mvn verify` xanh trước khi mở PR. Đọc `docs/CORE_GUIDE.md` (nhất là mục 3 về DAO và mục 6 về khuôn màn hình) nếu chưa đọc.
3. **Luật nộp bài (xem mục 8.1 kế hoạch):**
   - UI chỉ gọi **Service**, không `new XxxDao()` trong `ui/`. Việc gọi DB chạy bằng `Async.run(...)`.
   - Dùng sẵn Core: model ở `core/model`, DAO ở `core/dao`, `DataTable`, `FormPanel`, `Dialogs.error(this, ex)`.
   - SQL luôn dùng `?`. Lỗi hiện bằng tiếng Việt qua `ErrorTranslator`.
   - PR nhỏ (khoảng 400 dòng trở xuống), tên commit `feat(module): mô tả`. Có ảnh chụp màn hình trong PR.
   - Viết test **không giả định lượng dữ liệu** (máy chỉ nạp dữ liệu mẫu có 6 hội viên).
4. **Mốc:** Draft PR từ **Thứ Năm 22/10** để TV1 góp ý sớm · Hạn nộp PR **Thứ Sáu 23/10, 23:59** · Họp 15 phút Thứ Sáu (mỗi người demo module).
5. Việc nào không kịp thì báo **trước Thứ Năm** để cắt hoặc chuyển người.
6. Ai cần Core thêm gì: mở Issue gắn nhãn `core-request` (TV1 xử lý trong 24 giờ). Cần thêm cột/chỉ mục: Issue `db-request`.
7. Mỗi chương báo cáo có thêm mục ngắn "View và truy vấn đã dùng".

---

## TV2 – Hữu Trí: Hội viên (module `member/`)

**Sửa trước (quan trọng nhất, làm xong mới thêm tính năng):**
- Bỏ `member/model/` (Visitor, Member, DailyPass) và `member/dao/MemberDao`; dùng `core.model.Member`, `core.dao.MemberDao`
  (đã có `search`, `findPhoto`, `savePhoto`, `existsEmail`). Danh sách **không tải ảnh** (nặng); lấy ảnh khi mở hồ sơ.
- Thêm `MemberService`; `MemberListPanel` chỉ gọi Service, không gọi DAO.
- Gọi DB bằng `Async.run`, không chạy trên luồng giao diện. Dùng `DataTable` và `Dialogs.error(this, ex)`.

**Việc đợt 2:**
1. Form thêm/sửa hội viên: kiểm tra từng ô bằng `Validator`, chọn ngày sinh bằng date picker, tải ảnh có xem trước (`PhotoUtils`).
2. Bán vé ngày (hộp thoại nhanh): chọn ngày, tiền mặc định 15.00; tạo khách + vé + hóa đơn **Paid trong một `Tx`**.
3. Hồ sơ hội viên: tab Thông tin và Lịch sử gói.
4. Xóa khách theo BR-MEM-08: kiểm tra bản ghi liên quan, hiện lý do không xóa được.
5. `ViewDao` chỉ đọc cho `vw_Active_Membership` (mục 5.5).

**Xong khi:** thêm, sửa, xóa, tìm hội viên và bán vé ngày dùng được; không lưu được dữ liệu sai; mọi lỗi là thông báo tiếng Việt; ≥ 10 unit test xanh. **Tag mục tiêu:** v0.5.

---

## TV3 – Đức Tiến: Gói tập & Thanh toán (module `billing/`)

Bạn đã làm gần đủ chức năng của 4 tuần. Đợt này tập trung **kiểm chứng, làm gọn và hỗ trợ**:

1. **Tách thành nhiều PR nhỏ** (hiện 1 commit 2.336 dòng): ví dụ bảng giá, bán gói, hóa đơn, hết hạn, báo cáo. Bỏ `import ...BillingTransactionDao` thừa trong `SellWizardPanel`.
2. **Đối chiếu từng tiêu chí** ở mục 6.3 kế hoạch và checklist UX-01…UX-17 cho mọi màn hình; ghi lại ca nào chưa đạt.
3. Thử đủ 3 kịch bản gia hạn (còn hạn, hết hạn hôm nay, đã hết hạn 10 ngày) và ghi kết quả vào bảng test case (≥ 25 ca).
4. Thử dữ liệu lớn (script 06): danh sách hóa đơn hàng nghìn dòng phải mượt (dưới 1 giây).
5. **Kiểm thử chéo** module của TV2 và TV5 (bán gói cho hội viên chưa có ảnh, hội viên trùng, đăng ký lớp khi gói vừa hủy); ghi lỗi vào GitHub Issues.
6. `ViewDao` chỉ đọc cho `vw_Member_Billing_Summary` (công nợ).
7. Viết nháp chương "Module Gói tập & Thanh toán" (nhấn mạnh ALG-04, ALG-05).

`Tx` là tùy chọn với bạn: `BillingTransactionDao` đang tự giữ kết nối riêng nên **không gộp** vào `Tx` bên ngoài. Giữ nguyên cũng được.

**Xong khi:** mọi tiêu chí mục 6.3 đạt hoặc có ghi chú rõ; ≥ 12 unit test xanh (đã có 30); bảng test case ≥ 25 ca đạt 100 phần trăm. **Tag mục tiêu:** v0.5.

---

## TV4 – Hồ Qui Quân: Nhân sự & Cơ sở vật chất (module `facility/`)

**Chưa thấy nhánh `feature/facility` trên GitHub.** Hãy tạo nhánh ngay hôm nay và đẩy lên để TV1 thấy tiến độ; nếu đang vướng gì, nhắn trong ngày.

**Việc đợt 2:**
1. Danh sách nhân sự và phòng chạy với DB thật (dùng `StaffDao`, `RoomDao` của Core).
2. Form nhân sự: trường đổi theo vai trò (Trainer: chuyên môn, chứng chỉ; Receptionist: ca làm; Manager); kiểm tra SĐT trùng; tạo Trainer/Receptionist bằng `TrainerDao`/`ReceptionistDao` (đã ghi hai bảng trong một transaction).
3. Form phòng + màn hình chi tiết phòng (thiết bị trong phòng).
4. Danh sách thiết bị, form thiết bị, đổi trạng thái (`EquipmentDao`).
5. Phiếu bảo trì: `MaintenanceLogDao` đã tự đánh số `Log_no`; hộp chọn người ký chỉ gồm Receptionist/Manager.
6. `ViewDao` chỉ đọc cho `vw_Equipment_Status` và `vw_Trainer_Public`.

**Xong khi:** thêm, sửa nhân sự, phòng, thiết bị và ghi phiếu bảo trì dùng được; hai người ghi phiếu cùng thiết bị không trùng `Log_no`. **Tag mục tiêu:** v0.5.

---

## TV5 – Ku Líx: Lịch tập (module `scheduling/`)

**Sửa trước (hai lỗi nghiệp vụ):**
1. `SessionService.findRoomConflict/findTrainerConflict` đang gọi `sessionDao.findAll()` (tải **toàn bộ** buổi tập mỗi lần kiểm tra). Chỉ lấy buổi cùng phòng hoặc cùng huấn luyện viên trong khung giờ cần kiểm tra
   (cần hàm lọc theo phòng/giờ thì mở `core-request`, hoặc viết truy vấn riêng trong package của bạn).
2. Kiểm tra chồng lấn **phải bỏ qua buổi PT có trạng thái Cancelled** (ALG-01); thêm test cho ca này.

**Việc đợt 2:**
1. Wizard tạo lớp: bước 1 chọn phòng/huấn luyện viên; bước 2 chọn ngày và giờ trống bằng nút bấm (`FreeSlotFinder`); bước 3 tên lớp, sĩ số (giới hạn bởi sức chứa phòng). Ghi SESSION + CLASS (+ đặt thiết bị) trong **một `Tx`**.
2. Đặt lịch PT: chọn hội viên bằng hộp gợi ý; hội viên chưa có ảnh bị chặn kèm lý do.
3. Đăng ký lớp: danh sách lớp "còn X chỗ"; chỉ liệt kê hội viên có gói hợp lệ.
4. Hủy đăng ký, điểm danh.
5. `ViewDao` chỉ đọc cho `vw_Class_Schedule`.
6. Đã có tuần đầu rất tốt: giữ phong cách hàm thuần + unit test như `FreeSlotFinder`.

**Xong khi:** tạo lớp, đặt PT, đăng ký lớp chạy trọn luồng; đặt trùng phòng hoặc huấn luyện viên → thông báo nêu rõ buổi đang chiếm chỗ; ≥ 15 unit test xanh. **Tag mục tiêu:** v0.5.

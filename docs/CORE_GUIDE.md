# CORE_GUIDE – Cách dùng phần lõi (TV1) cho TV2–TV5

Tài liệu này đủ để bạn viết module của mình mà **không phải hỏi TV1**. Mọi ví dụ đều là mã chạy được.
Gặp thiếu sót thì mở Issue gắn nhãn `core-request`.

## 0. Chạy thử trước đã

```powershell
mvn verify                                   # biên dịch + 31 test (test DAO cần DB, tự bỏ qua nếu không nối được)
java -jar target\gym-planner-0.1.0.jar        # chạy ứng dụng
java -cp "target\gym-planner-0.1.0.jar;target\test-classes" com.gym.core.ui.UiKitDemo   # xem thử bộ UiKit
```

Tài khoản đăng nhập thử (mật khẩu chung mặc định `gym123`):

| Vai trò | Số điện thoại |
|---|---|
| Manager (Quản lý) | `0901000005` |
| Receptionist (Lễ tân) | `0901000003` |
| Trainer (HLV) | `0901000001` |

## 1. Quy tắc tầng (bắt buộc)

```
UI  →  Service  →  DAO  →  DB          (một chiều)
```

| Tầng | Nằm ở | Được làm | Bị cấm |
|---|---|---|---|
| model | `core/model` | dữ liệu thuần, kiểm tra trong setter | SQL, Swing |
| dao | `core/dao` | CRUD và truy vấn | luật nghiệp vụ, hộp thoại |
| service | `<module>/service` | luật nghiệp vụ, giải thuật, transaction | `import javax.swing` |
| ui | `<module>/ui` | màn hình, gọi Service trong `Async` | SQL, gọi DAO trực tiếp |

Mỗi bạn chỉ sửa trong package của mình: TV2 `member/`, TV3 `billing/`, TV4 `facility/`, TV5 `scheduling/`.
**Không sửa** `core/`, `app/`, `dashboard/`, `pom.xml`.

## 2. Model và enum

17 lớp model nằm ở `com.gym.core.model`, tên khớp bảng:

| Bảng | Lớp | Ghi chú |
|---|---|---|
| VISITOR | `Visitor` (abstract) | cha của `Member`, `DailyPassTicket` |
| MEMBER / DAILY_PASS_TICKET | `Member` / `DailyPassTicket` | |
| STAFF | `Staff` (abstract) | cha của `Trainer`, `Receptionist`, `Manager` |
| TRAINER / RECEPTIONIST | `Trainer` / `Receptionist` | `Manager` không có bảng riêng (Role = Manager) |
| SESSION | `Session` (abstract) | cha của `ClassSession`, `PtSession` |
| CLASS / PT_BOOKING | `ClassSession` / `PtSession` | |
| MEMBERSHIP_PLAN, MEMBERSHIP, BILLING_LOG, ROOM, CLASS_REGISTRATION, EQUIPMENT, MAINTENANCE_LOG, SESSION_EQUIPMENT_USAGE | `MembershipPlan`, `Membership`, `BillingLog`, `Room`, `ClassRegistration`, `Equipment`, `MaintenanceLog`, `SessionEquipmentUsage` | |

Setter có kiểm tra: `room.setCapacity(0)` ném `IllegalArgumentException("Sức chứa phải lớn hơn 0.")`.
Id kiểu `Integer`, `null` nghĩa là chưa lưu.

**Enum** (`MembershipStatus`, `BillingStatus`, `BookingStatus`, `AttendanceStatus`, `EquipmentStatus`, `ShiftType`, `SessionType`, `StaffRole`) đều có:

```java
status.getLabel();      // "Đang hiệu lực"  → hiện trên giao diện
status.getColorHex();   // "#2E7D32"        → tô màu
status.getDbValue();    // "Active"         → chỉ DAO dùng
```
Trong code luôn so sánh bằng enum: `if (m.getStatus() == MembershipStatus.ACTIVE)`, không so chuỗi.

## 3. DAO

Mỗi bảng một DAO trong `com.gym.core.dao`. Tạo bằng `new` (DAO không giữ trạng thái, mỗi lệnh tự mở và đóng kết nối).

| Bảng | DAO | Khóa | Truy vấn riêng có sẵn |
|---|---|---|---|
| ROOM | `RoomDao` | `Integer` | `findByName` |
| MEMBERSHIP_PLAN | `MembershipPlanDao` | `Integer` | `findByName` |
| MEMBERSHIP | `MembershipDao` | `Integer` | `findByVisitor`, `findActiveEndingBetween` |
| BILLING_LOG | `BillingLogDao` | `Integer` | `findByVisitor` |
| VISITOR (mọi khách) | `VisitorDao` | `Integer` | `findByPhone`, `existsPhone` |
| MEMBER | `MemberDao` | `Integer` | `search`, `findByEmail`, `existsEmail`, `findPhoto`, `savePhoto` |
| DAILY_PASS_TICKET | `DailyPassTicketDao` | `Integer` | |
| STAFF (mọi nhân viên) | `StaffDao` | `Integer` | `findByPhone`, `findByRole` |
| TRAINER | `TrainerDao` | `Integer` | `findBySpecialty` |
| RECEPTIONIST | `ReceptionistDao` | `Integer` | |
| SESSION (mọi buổi) | `SessionDao` | `Integer` | `findStartingBetween` |
| CLASS | `ClassSessionDao` | `Integer` | |
| PT_BOOKING | `PtSessionDao` | `Integer` | `findByMember` |
| CLASS_REGISTRATION | `ClassRegistrationDao` | `ClassRegistrationKey(sessionId, visitorId)` | `findBySession`, `findByVisitor` |
| EQUIPMENT | `EquipmentDao` | `Integer` | `findByStatus`, `findByRoom` |
| MAINTENANCE_LOG | `MaintenanceLogDao` | `MaintenanceLogKey(equipmentId, logNo)` | `findByEquipment` |
| SESSION_EQUIPMENT_USAGE | `SessionEquipmentUsageDao` | `Integer` | `findBySession`, `findByEquipment` |

Sáu hàm chung cho mọi DAO:

```java
Optional<T> findById(id);          // không thấy thì Optional rỗng
List<T> findAll();                 // chỉ dùng cho bảng nhỏ (phòng, gói, thiết bị...)
PageResult<T> findPage(page, size);// bảng lớn: page bắt đầu từ 1
T insert(T entity);                // trả về chính đối tượng đã có id và giá trị mặc định của DB
void update(T entity);
void deleteById(id);
```

Điểm cần nhớ:

1. **`insert` cập nhật lại đối tượng bạn truyền vào** (id do DB sinh, giá trị mặc định như `status = ACTIVE`).
2. **Kế thừa**: `MemberDao.insert` ghi vào VISITOR rồi MEMBER trong **một transaction** (lỗi thì hủy cả hai).
   `VisitorDao`/`StaffDao`/`SessionDao` đọc ra đúng kiểu con (đa hình): `Visitor v = visitorDao.findById(5)` có thể là `Member` hoặc `DailyPassTicket`.
3. **Ảnh hội viên không tải theo danh sách** (nặng). `member.hasPhoto()` cho biết có ảnh chưa; lấy ảnh bằng `memberDao.findPhoto(id)`, lưu bằng `savePhoto(id, bytes)`. `update(member)` không đụng tới ảnh.
4. **Xóa khách/nhân viên/buổi** là xóa ở bảng cha, bảng con tự bị xóa theo (CASCADE). Bị khóa ngoại chặn thì lỗi 547, `ErrorTranslator` đổi thành "Không thể xóa vì dữ liệu này đang được sử dụng ở nơi khác."
5. **`MaintenanceLog`**: `Log_no` do DAO tự đánh số (lớn nhất của thiết bị đó + 1).
6. **Cột NULL**: `roomId` của `Equipment`, `membershipId`/`paymentDate` của `BillingLog`... là `null` được.

### Cần truy vấn riêng ngoài danh sách trên?

Đừng sửa DAO của Core. Tạo lớp con trong package của bạn, dùng sẵn `selectSql()`, `queryList`, `queryOne`, `count`:

```java
package com.gym.billing.service;

public class BillingQueryDao extends BillingLogDao {

    /** Hóa đơn chưa thanh toán quá hạn. */
    public List<BillingLog> findOverdue(LocalDate today) {
        return queryList(selectSql() + " WHERE Billing_Status = 'Pending' AND Due_Date < ? ORDER BY Due_Date",
                ps -> Sql.setDate(ps, 1, today));
    }
}
```
Mẹo: gõ thử câu SQL trong SSMS cho chạy đúng trước, rồi đưa vào Java và thay giá trị bằng `?`.
Luôn dùng `?` (PreparedStatement), không nối chuỗi người dùng nhập vào SQL.

## 4. Lỗi và thông báo tiếng Việt

```
DB / trigger ném SQLException  →  DAO bọc thành DataAccessException  →  UI gọi Dialogs.error(...)
Service tự kiểm tra luật       →  throw new BusinessException("câu tiếng Việt")
```

- Lỗi trigger 50001–50029, trùng khóa (2601/2627), vi phạm ràng buộc (547) đều được `ErrorTranslator` dịch.
- **UI chỉ cần một dòng**: `Dialogs.error(this, exception)`. Không tự hiện `e.getMessage()`.
- Thiếu mã lỗi nào thì báo TV1 thêm vào `ErrorTranslator` (có test kiểm tra đủ 50001–50029).
- Không `catch (Exception e) {}` rồi bỏ qua, không `printStackTrace()`.

## 5. Validator, DateTimeUtils

```java
// Gom MỌI lỗi nhập liệu rồi báo một lần
Validator v = new Validator()
        .name("Họ", lastName)
        .phone("Số điện thoại", phone)          // 10–11 chữ số, bắt đầu bằng 0
        .email("Email", email)
        .birthDate("Ngày sinh", birthDate, LocalDate.now())
        .nonNegative("Giá", price);
v.throwIfInvalid();                              // ném BusinessException liệt kê tất cả lỗi
```

```java
DateTimeUtils.format(date);                      // "25/12/2026"
DateTimeUtils.parseDate("25/12/2026");           // sai thì BusinessException tiếng Việt (31/02 cũng bị từ chối)
DateTimeUtils.overlaps(aStart, aEnd, bStart, bEnd);   // chồng lấn; kết thúc đúng lúc bắt đầu KHÔNG tính là chồng
DateTimeUtils.startOfWeek(date);                 // thứ Hai của tuần
DateTimeUtils.validateSessionSlot(start, end, LocalDateTime.now());   // luật 06:00–22:00, 30–180 phút, bước 15
DateTimeUtils.isExpiringSoon(endDate, today, 7);
```

## 6. UiKit (`com.gym.core.ui`)

| Thành phần | Dùng để |
|---|---|
| `Theme.install()` | Giao diện FlatLaf (đã gọi sẵn ở `Main`) |
| `DataTable<T>` | Bảng có phân trang; ngày, tiền, enum tự định dạng |
| `FormPanel` | Biểu mẫu nhập liệu, ô bắt buộc có dấu `*` |
| `DatePickers` | Ô chọn ngày / ngày giờ (dd/MM/yyyy, giờ bước 15 phút) |
| `Async.run(...)` | Chạy việc nặng ở luồng nền, có lớp phủ "Đang xử lý..." |
| `Dialogs` | `info`, `warn`, `error`, `confirm`, `confirmDelete` |
| `Toast.show(...)` | Thông báo nhỏ tự biến mất ("Đã lưu") |

### Khuôn một màn hình danh sách + form

```java
public class RoomPanel extends JPanel {

    private final RoomService service = new RoomService();            // Service của bạn
    private final DataTable<Room> table = new DataTable<>(20);
    private final FormPanel form = new FormPanel();

    public RoomPanel() {
        setLayout(new BorderLayout(12, 12));

        table.addColumn("Tên phòng", Room::getRoomName, 200)
             .addColumn("Sức chứa", Room::getCapacity, 80)
             .addColumn("Tầng", Room::getFloorLocation, 100)
             .setLoader((page, size) -> service.findPage(page, size))   // chạy ở luồng nền
             .onDoubleClick(this::fillForm);

        form.addText("name", "Tên phòng", true);
        form.addText("capacity", "Sức chứa", true);
        form.addText("floor", "Tầng", false);

        JButton save = new JButton("Lưu");
        save.addActionListener(e -> save());

        add(table, BorderLayout.CENTER);
        add(form, BorderLayout.EAST);
        add(save, BorderLayout.SOUTH);
        table.reload();
    }

    private void fillForm(Room r) {
        form.setText("name", r.getRoomName());
        form.setText("capacity", String.valueOf(r.getCapacity()));
        form.setText("floor", r.getFloorLocation());
    }

    private void save() {
        String name = form.getText("name");
        String capacity = form.getText("capacity");
        String floor = form.getText("floor");
        // Gọi Service ở luồng nền; lỗi tự hiện hộp thoại tiếng Việt
        Async.run(this, () -> service.create(name, capacity, floor), saved -> {
            Toast.show(this, "Đã lưu phòng " + saved.getRoomName());
            form.clear();
            table.reload();
        });
    }
}
```

Và Service tương ứng (không có `javax.swing`):

```java
public class RoomService {
    private final RoomDao dao = new RoomDao();

    public PageResult<Room> findPage(int page, int size) {
        return dao.findPage(page, size);
    }

    public Room create(String name, String capacityText, String floor) {
        Validator v = new Validator().required("Tên phòng", name).required("Sức chứa", capacityText);
        v.throwIfInvalid();
        int capacity;
        try {
            capacity = Integer.parseInt(capacityText);
        } catch (NumberFormatException e) {
            throw new BusinessException("Sức chứa phải là số nguyên.");
        }
        Room room = new Room();
        room.setRoomName(name);          // setter tự kiểm tra
        room.setCapacity(capacity);
        room.setFloorLocation(floor);
        return dao.insert(room);
    }
}
```

Chạy `UiKitDemo` để xem các thành phần hoạt động thật.

## 7. Đăng ký module và phân quyền

`app/ModuleRegistry` đã đăng ký sẵn 4 module. Bạn chỉ sửa lớp module của mình (ví dụ `member/MemberModule`):

```java
@Override
public JPanel createPanel() {
    return new MemberListPanel();           // thay cho PlaceholderPanel
}

@Override
public Set<StaffRole> getAllowedRoles() {   // ai được thấy mục menu này
    return Set.of(StaffRole.MANAGER, StaffRole.RECEPTIONIST);
}
```

Người đang đăng nhập:

```java
Session.getStaff();                  // Staff (Trainer / Receptionist / Manager)
Session.getRole();                   // StaffRole
Session.hasRole(StaffRole.MANAGER);  // ẩn nút nguy hiểm với vai trò thấp
```
Ví dụ: chỉ Manager mới thấy nút "Xóa" → `deleteButton.setVisible(Session.hasRole(StaffRole.MANAGER));`

## 8. Viết test

- Hàm giải thuật / luật (Service): test thuần, không cần DB, đặt trong `src/test/java/com/gym/<module>/`.
- Cần DB: xem mẫu `core/dao/DaoIntegrationTest` — tạo dữ liệu có tiền tố `ZZ-`, kiểm tra, rồi **luôn xóa lại** trong `finally`, và `assumeTrue(...)` để tự bỏ qua khi không có DB.
- Dữ liệu mẫu hết hạn (BR-SCH-05 dùng ngày hôm nay): xem rủi ro trong kế hoạch, dùng `08_demo_data.sql`.

## 9. Việc TV1 lo, đừng tự làm

| Việc | Làm thế nào |
|---|---|
| Thêm thư viện vào `pom.xml` | Issue `core-request` |
| Thêm cột / chỉ mục / bảng | Issue `db-request` |
| Thiếu hàm trong DAO / UiKit / `ErrorTranslator` | Issue `core-request` (xử lý trong 24 giờ). Trong lúc chờ, tự viết tạm trong package của bạn |
| Đổi mật khẩu đăng nhập | `new AuthService().changePassword(cu, moi)` (ghi vào `config.local.properties`) |

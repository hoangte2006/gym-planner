# Chương 7. Module Lịch tập

## 7.1. Mục tiêu và phạm vi

Module Lịch tập quản lý các buổi tập nhóm (`ClassSession`) và buổi huấn luyện cá nhân
(`PtSession`). Nhân viên có thể xem lịch theo tuần; quản lý và lễ tân có thể tạo, dời,
hủy hoặc xóa buổi theo quyền được cấp. Module cũng hỗ trợ đăng ký lớp, điểm danh và
đặt thiết bị theo phòng, thời gian của buổi tập.

Các màn hình chính gồm lịch tuần, wizard tạo lớp, đặt lịch PT, đăng ký lớp và quản lý
thiết bị của buổi tập. Tác vụ truy vấn từ màn hình chạy nền để không khóa giao diện.

## 7.2. Kiến trúc và mô hình dữ liệu

Module tuân theo luồng một chiều:

```text
UI → Service → DAO → SQL Server
```

`Session` là lớp cha dùng chung cho hai loại buổi. `ClassSession` bổ sung tên lớp và
sĩ số; `PtSession` bổ sung hội viên và trạng thái đặt lịch. DAO đọc lịch theo khoảng
thời gian và ánh xạ kết quả về đúng lớp con. Các luật nghiệp vụ nằm trong Service;
trigger và transaction của cơ sở dữ liệu là lớp bảo vệ cuối cùng khi nhiều nhân viên
thao tác đồng thời.

Lịch tuần truy vấn theo khoảng nửa mở `[thứ Hai 00:00, thứ Hai tuần sau 00:00)` và
được sắp xếp ngay tại SQL Server. Service giữ thứ tự truy vấn thay vì sắp xếp lại lần
nữa; trên giao diện, toàn bộ dữ liệu tuần được đưa vào bảng trong một lần cập nhật
model thay vì phát một sự kiện cho từng dòng. Các chỉ mục hiện có của bảng SESSION
tập trung vào truy vấn xung đột theo phòng hoặc huấn luyện viên; nếu cần chỉ mục
riêng cho Start_Time, đây là thay đổi schema cần TV1 duyệt theo quy trình db-request.

## 7.3. Phát hiện chồng lấn - ALG-01

Hai khoảng thời gian `[S1, E1)` và `[S2, E2)` chồng lấn khi:

```text
S1 < E2 và S2 < E1
```

Do dùng khoảng nửa mở, một buổi kết thúc đúng lúc buổi kế tiếp bắt đầu là hợp lệ.
Quy tắc được áp dụng riêng cho phòng và huấn luyện viên; khi dời buổi, chính
`Session_ID` đang sửa không được xem là xung đột. Buổi PT ở trạng thái `Cancelled`
không chiếm lịch.

Service truy vấn các buổi có cùng tài nguyên và thời gian giao nhau để hiển thị thông
báo có tên, mã và thời gian của buổi đang chiếm lịch. Kiểm tra ở Service giúp phản hồi
sớm; trigger SQL Server vẫn kiểm tra trong giao dịch ghi để không cho hai thao tác
đồng thời cùng chiếm một khung giờ.
Kiểm thử tích hợp tạo hai lượt đặt với hai kết nối DB sau khi cả hai lượt đã qua bước
kiểm tra ở Service; kết quả xác nhận chỉ một lượt ghi được thành công. Giao dịch bị
SQL Server chọn làm deadlock victim được dịch thành thông báo tiếng Việt qua
`ErrorTranslator`.

## 7.4. Gợi ý giờ trống - ALG-02

Giờ hoạt động là 06:00–22:00. Với phòng và huấn luyện viên đã chọn, hệ thống lấy lịch
trong tuần, giữ lại các buổi chiếm một trong hai tài nguyên và giao với cửa sổ của
ngày cần đặt. Sau đó:

1. Sắp xếp các khoảng bận theo giờ bắt đầu.
2. Gộp các khoảng chồng lấn hoặc tiếp giáp.
3. Tính các khoảng trống giữa giờ mở cửa và các khoảng bận.
4. Sinh giờ bắt đầu theo bước 15 phút và chỉ giữ slot đủ thời lượng.

Việc sắp xếp chi phối độ phức tạp `O(n log n)`; các bước gộp và duyệt khoảng trống
tuyến tính. Buổi PT đã hủy không được tính là bận. Thời lượng đầu vào hợp lệ từ
30 đến 180 phút, theo bước 15 phút; buổi phải nằm trong cùng ngày và trong giờ hoạt
động. Các điều kiện này được kiểm tra trong Service, không chỉ dựa vào tùy chọn UI.

## 7.5. Sĩ số và đăng ký lớp - ALG-03

Số chỗ đã giữ là số đăng ký có trạng thái khác `Cancelled`; vì vậy cả `Confirmed` và
`Attended` đều chiếm chỗ. Số chỗ còn lại được tính bằng:

```text
Max_Capacity - số đăng ký đang giữ chỗ
```

Khi nhận chỗ, DAO khóa dòng lớp bằng `UPDLOCK, HOLDLOCK`, đếm lại đăng ký trong
cùng transaction rồi mới thêm đăng ký. Cách làm này tránh việc hai lễ tân cùng nhận
chỗ cuối cùng. Hội viên phải có ảnh hồ sơ và gói tập `Active` còn hạn trong ngày đăng
ký; các điều kiện được kiểm tra trước khi DAO nhận chỗ.

## 7.6. Tạo, dời và vòng đời buổi tập - ALG-08

Tạo lớp hoặc PT ghi bản ghi `SESSION` cùng dữ liệu bảng con trong transaction của
DAO. Nếu bất kỳ thao tác nào thất bại, transaction bị rollback. Trước khi ghi, Service
kiểm tra:

- Buổi mới bắt đầu trong tương lai.
- Bắt đầu và kết thúc trong cùng ngày, trong khoảng 06:00–22:00.
- Thời lượng từ 30 đến 180 phút và các mốc giờ theo bước 15 phút.
- Phòng, huấn luyện viên và sức chứa phù hợp; không có lịch trùng.
- Hội viên PT có ảnh hồ sơ.

Dời lịch chạy lại kiểm tra giờ, phòng, huấn luyện viên và sức chứa; đồng thời xác nhận
thiết bị đã đặt vẫn hoạt động, đúng phòng, không bị đặt trùng và nằm trong khung giờ
mới trước khi cập nhật.
Đổi trạng thái PT từ `Cancelled` về `Scheduled` cũng kiểm tra lại tài nguyên vì khung
giờ có thể đã được dùng sau khi hủy. Buổi PT `Completed` là trạng thái cuối; lớp đã
bắt đầu không nhận đăng ký mới. Khi xóa lớp có đăng ký, giao diện báo rõ số đăng ký
sẽ bị xóa kèm và yêu cầu xác nhận.

```text
PT:    Scheduled ──→ Completed
          │
          └────────→ Cancelled ──→ Scheduled (kiểm tra lại tài nguyên)

Lớp:   Confirmed ──→ Attended
          │
          └────────→ Cancelled
```

## 7.7. Kiểm thử và kết quả

Bộ kiểm thử scheduling bao phủ thuật toán khoảng trống, chồng lấn, phân quyền, vòng
đời PT, đăng ký lớp, hội viên hết hạn/không ảnh, lịch tuần 2.600 buổi và đặt đồng thời
trên SQL Server. Danh sách ca và kết quả được lưu tại
[bảng test case của Module Lịch tập](./testcases/scheduling-week4-testcases.md).

# Test cases - Module Quản lý Hội viên (Tuần 4)

| Mã TC | Hạng mục kiểm thử | Các bước thực hiện / Dữ liệu đầu vào | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :---: |
| MEM-01 | Tạo hội viên hợp lệ | Điền đủ Họ, Tên, SĐT 10 số (0912345678), Email hợp lệ, Ngày sinh quá khứ. | Tạo mới thành công, hiển thị trên bảng, DB lưu bản ghi vào VISITOR và MEMBER. | Đạt | **PASS** |
| MEM-02 | Bỏ trống Họ | Để trống trường Họ, điền các trường còn lại. | Báo lỗi `Validator`: Họ không được để trống. Chặn lưu. | Đạt | **PASS** |
| MEM-03 | Bỏ trống Tên | Để trống trường Tên, điền các trường còn lại. | Báo lỗi `Validator`: Tên không được để trống. Chặn lưu. | Đạt | **PASS** |
| MEM-04 | SĐT sai định dạng | Nhập SĐT chứa chữ cái hoặc ít hơn 10 chữ số (vd: `12345`). | Báo lỗi định dạng số điện thoại không hợp lệ. | Đạt | **PASS** |
| MEM-05 | Email sai định dạng | Nhập email không chứa ký tự `@` hoặc sai tên miền (`abc.xyz`). | Báo lỗi định dạng email không hợp lệ. | Đạt | **PASS** |
| MEM-06 | Ngày sinh tương lai | Chọn ngày sinh lớn hơn ngày hiện tại. | Báo lỗi: Ngày sinh không thể ở tương lai. | Đạt | **PASS** |
| MEM-07 | Hội viên chưa đủ 12 tuổi | Chọn ngày sinh cách hiện tại dưới 12 năm. | Báo lỗi độ tuổi tối thiểu theo quy định phòng tập. | Đạt | **PASS** |
| MEM-08 | Cập nhật thông tin | Sửa SĐT và Email của hội viên hiện có -> Lưu. | CSDL cập nhật chính xác, không thay đổi ID và ngày đăng ký. | Đạt | **PASS** |
| MEM-09 | Xóa hội viên (Cascade) | Chọn một hội viên và bấm Xóa -> Xác nhận. | Dòng VISITOR bị xóa, bản ghi MEMBER bị xóa theo nhờ cascade. | Đạt | **PASS** |
| **MEM-10 | Tải ảnh chân dung hợp lệ | Tải tệp JPG/PNG dung lượng dưới 2MB. | Ảnh hiển thị trên Avatar, lưu đúng byte[] vào cột `Photo`. | Đạt | **PASS** |
| MEM-11** | Đọc danh sách phân trang | Mở màn hình danh sách có 2.500 bản ghi. | Tải đúng 20 dòng/trang, chuyển trang mượt mà, không bị giật lag UI. | Đạt | **PASS** |
| MEM-12** | Tìm kiếm theo Tên | Nhập từ khóa tên vào ô tìm kiếm -> Enter. | Trả về đúng danh sách hội viên có tên tương ứng dưới 50ms. | Đạt | **PASS** |
| **MEM-13 | Tìm kiếm theo SĐT | Nhập chuỗi số điện thoại đầy đủ hoặc một phần. | Lọc chính xác các hội viên khớp đầu số qua chỉ mục index. | Đạt | **PASS** |
| MEM-14 | Bán vé ngày hợp lệ | Nhập Họ, Tên, SĐT khách vãng lai -> Xác nhận bán vé. | Tạo bản ghi vé ngày, in mã Visitor ID tạm thời. | Đạt | **PASS** |
| MEM-15 | Bán vé ngày thiếu tên | Để trống Tên khách khi bán vé ngày. | Hệ thống chặn và ném `BusinessException`. | Đạt | **PASS** |
| MEM-16 | Nâng cấp vé ngày thành hội viên | Nhập mã vé ngày hợp lệ -> Xác nhận hộp thoại hệ quả. | Xóa bản ghi vé ngày, tạo bản ghi MEMBER chính thức trong 1 Transaction. | Đạt | **PASS** |
| MEM-17 | Nâng cấp vé ngày không tồn tại | Nhập mã vé ngày âm hoặc không có trong DB (vd: `-999`). | Báo lỗi: Không tìm thấy thông tin vé ngày của khách. | Đạt | **PASS** |
| MEM-18 | Xem hồ sơ 5 tab | Nhấp đúp chuột vào một hội viên trên danh sách. | Mở `MemberDetailDialog` đủ 5 tab: Thông tin, Gói tập, Hóa đơn, Lịch tập, Lịch sử ra vào. | Đạt | PASS |
| MEM-19 | Điều hướng nhanh tab Gói tập | Bấm nút chuyển nhanh sang module Gói tập tại Tab 2. | Kiểm tra `ModuleRegistry`, đóng dialog và mở màn hình Bán gói. | Đạt | **PASS** |
| MEM-20 | Empty state tab Hóa đơn | Xem hồ sơ hội viên mới chưa có giao dịch nào. | Hiển thị thông báo thân thiện kèm nút `[+ Mua gói/Tạo hóa đơn]` (UX-11). | Đạt | **PASS** |
| MEM-21 | Phím tắt Ctrl+F | Nhấn `Ctrl + F` tại màn hình danh sách. | Con trỏ nhảy ngay vào ô tìm kiếm và bôi đen toàn bộ text (UX-12). | Đạt | **PASS** |
| MEM-22 | Phím tắt Ctrl+N | Nhấn `Ctrl + N` tại màn hình danh sách. | Bật ngay hộp thoại Thêm hội viên mới. | Đạt | **PASS** |
| MEM-23 | Hướng dẫn màn hình UX-17 | Bấm nút `?` trên thanh công cụ. | Hiển thị hộp thoại hướng dẫn thao tác ngắn gọn đúng 3 dòng. | Đạt | **PASS** |
| MEM-24 | Xuất danh sách ra CSV | Bấm nút "Xuất CSV" (hoặc `Ctrl + E`) -> Chọn file lưu. | Xuất file CSV định dạng UTF-8 có BOM, mở bằng Excel hiển thị đúng dấu tiếng Việt. | Đạt | **PASS** |
| MEM-25 | Hiệu năng 2.500+ hội viên | Thực hiện tìm kiếm và chuyển trang liên tục trên tập 2.500 dòng. | Thời gian phản hồi câu truy vấn < 10ms nhờ Non-Clustered Indexes. | Đạt | **PASS** |
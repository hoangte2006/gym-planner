"""Vẽ hai sơ đồ tổng thể cho báo cáo (Chương 3) bằng Pillow:
  - kien_truc_phan_tang.png : kiến trúc phân tầng UI -> Service -> DAO -> CSDL của toàn ứng dụng
  - ke_thua_tong_the.png    : ba cây kế thừa của mô hình (Visitor, Staff, Session)
Chạy: python docs/diagrams/ve_so_do_tong_the.py   (cần Pillow và font Arial của Windows)
Muốn đổi nội dung thì sửa các danh sách chữ trong hai hàm bên dưới rồi chạy lại.

Cỡ chữ: ảnh rộng 1300 px sẽ được chèn vào báo cáo với bề ngang 17,4 cm (khoảng 190 điểm ảnh mỗi inch),
nên chữ 21 px khi in là khoảng 8 pt. Đừng giảm cỡ chữ xuống dưới 19 px.
"""
import math
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
FONT_REGULAR = "C:/Windows/Fonts/arial.ttf"
FONT_BOLD = "C:/Windows/Fonts/arialbd.ttf"

INK = (31, 41, 55)
MUTED = (90, 100, 115)
BLUE = (31, 78, 121)
LINE = (120, 135, 155)
WHITE = (255, 255, 255)

C_USER = (232, 240, 252)
C_APP = (255, 244, 214)
C_UI = (222, 238, 255)
C_SERVICE = (225, 245, 228)
C_DAO = (255, 236, 222)
C_CORE = (240, 236, 250)
C_DB = (238, 238, 238)
C_PARENT = (222, 238, 255)
C_CHILD = (240, 247, 255)

TITLE = 26
TEXT = 21
SMALL = 19


def font(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REGULAR, size)


def wrap(draw, text, fnt, max_width):
    out = []
    for paragraph in text.split("\n"):
        line = ""
        for w in paragraph.split(" "):
            trial = w if not line else line + " " + w
            if draw.textlength(trial, font=fnt) <= max_width:
                line = trial
            else:
                if line:
                    out.append(line)
                line = w
        out.append(line)
    return out


def measure(draw, w, title, lines, title_size=TITLE, text_size=TEXT, pad=14):
    ft, fl = font(title_size, True), font(text_size)
    h = pad
    if title:
        h += len(wrap(draw, title, ft, w - 2 * pad)) * (title_size + 6) + 4
    for t in lines:
        h += len(wrap(draw, t, fl, w - 2 * pad)) * (text_size + 6) + 2
    return h + pad - 2


def box(draw, x, y, w, title, lines, fill, h=None, title_size=TITLE, text_size=TEXT, pad=14, outline=LINE,
        center=False):
    ft, fl = font(title_size, True), font(text_size)
    if h is None:
        h = measure(draw, w, title, lines, title_size, text_size, pad)
    draw.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=fill, outline=outline, width=2)
    cy = y + pad
    if title:
        for ln in wrap(draw, title, ft, w - 2 * pad):
            tx = x + (w - draw.textlength(ln, font=ft)) / 2 if center else x + pad
            draw.text((tx, cy), ln, font=ft, fill=BLUE)
            cy += title_size + 6
        cy += 4
    for t in lines:
        for ln in wrap(draw, t, fl, w - 2 * pad):
            tx = x + (w - draw.textlength(ln, font=fl)) / 2 if center else x + pad
            draw.text((tx, cy), ln, font=fl, fill=INK)
            cy += text_size + 6
        cy += 2
    return h


def arrow(draw, p1, p2, color=LINE, width=3, head=13):
    draw.line([p1, p2], fill=color, width=width)
    ang = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
    for da in (math.pi * 0.85, -math.pi * 0.85):
        draw.line([p2, (p2[0] + head * math.cos(ang + da), p2[1] + head * math.sin(ang + da))], fill=color, width=width)


def hollow_triangle(draw, tip, color=LINE, size=17):
    x, y = tip
    pts = [(x, y), (x - size * 0.8, y + size * 1.4), (x + size * 0.8, y + size * 1.4)]
    draw.polygon(pts, fill=WHITE)
    draw.line(pts + [pts[0]], fill=color, width=2)


def kien_truc_phan_tang():
    W = 1300
    img = Image.new("RGB", (W, 1500), WHITE)
    d = ImageDraw.Draw(img)
    cx = W // 2

    # Người dùng
    box(d, 330, 20, 640, "", ["Người dùng: Quản lý · Lễ tân · Huấn luyện viên"], C_USER, center=True)
    # app
    app_y = 120
    app_h = box(d, 20, app_y, W - 40, "app/ - khung ứng dụng (TV1)", [
        "LoginFrame → Session (nhớ người đăng nhập và vai trò) → MainFrame: menu bên trái lọc theo vai trò, "
        "mỗi mục menu là một thẻ trong CardLayout.",
        "ModuleRegistry giữ danh sách 5 module, mỗi module tự khai báo vai trò được vào. Trang chủ cũng nằm ở đây."], C_APP)
    arrow(d, (cx, 20 + 56), (cx, app_y))

    # 5 module (chỉ tên)
    names = ["Dashboard (TV1)", "Hội viên (TV2)", "Gói tập và Thanh toán (TV3)", "Lịch tập (TV5)",
             "Nhân sự và Cơ sở vật chất (TV4)"]
    colw, gap = 236, 20
    mod_y = app_y + app_h + 46
    mod_h = max(measure(d, colw, n, [], title_size=22) for n in names)
    centers = []
    for i, n in enumerate(names):
        x = 20 + i * (colw + gap)
        box(d, x, mod_y, colw, n, [], WHITE, h=mod_h, title_size=22, center=True, outline=BLUE)
        arrow(d, (x + colw // 2, app_y + app_h), (x + colw // 2, mod_y))
        centers.append(x + colw // 2)

    # ba tầng
    y = mod_y + mod_h + 46
    for c in centers:
        arrow(d, (c, mod_y + mod_h), (c, y))
    layers = [
        ("Giao diện (ui/)", "Mỗi module có các màn hình riêng. Màn hình chỉ vẽ và lắng nghe thao tác, không có SQL; "
                            "việc gọi cơ sở dữ liệu chạy nền bằng Async.", C_UI),
        ("Service (service/)", "Luật nghiệp vụ và kiểm tra quyền theo vai trò. Thao tác ghi nhiều bảng được bọc trong Tx. "
                               "Không có mã giao diện.", C_SERVICE),
        ("DAO (dao/)", "Nơi duy nhất viết SQL, luôn dùng PreparedStatement. DAO dùng chung của core cho 17 bảng; "
                       "truy vấn riêng của module viết thêm trong module.", C_DAO),
    ]
    for i, (t, txt, col) in enumerate(layers):
        h = box(d, 20, y, W - 40, t, [txt], col)
        if i < len(layers) - 1:
            arrow(d, (cx, y + h), (cx, y + h + 34))
            y += h + 34
        else:
            y += h

    # core
    core_y = y + 46
    arrow(d, (cx, y), (cx, core_y))
    inner_w = (W - 40 - 3 * 14) // 2
    inner = [
        ("model/", "17 thực thể và các enum trạng thái (DbEnum); ba cây kế thừa Visitor, Staff, Session."),
        ("dao/ của core", "AbstractJdbcDao dùng chung, 17 DAO, Tx (giao dịch nhiều DAO), DBConnection, PageResult."),
        ("ui/ (bộ UiKit)", "DataTable, FormPanel, Dialogs, Async, Toast, DatePickers, Theme."),
        ("Tiện ích chung", "Validator, ErrorTranslator, DateTimeUtils, Config, AuthService."),
    ]
    ih = max(measure(d, inner_w, t, [x]) for t, x in inner)
    outer_h = 56 + 2 * ih + 14 + 14
    d.rounded_rectangle([20, core_y, W - 20, core_y + outer_h], radius=14, fill=C_CORE, outline=LINE, width=2)
    d.text((36, core_y + 12), "core/ - phần lõi dùng chung (TV1), mọi module đều dùng", font=font(TITLE, True), fill=BLUE)
    for i, (t, x) in enumerate(inner):
        bx = 34 + (i % 2) * (inner_w + 14)
        by = core_y + 56 + (i // 2) * (ih + 14)
        box(d, bx, by, inner_w, t, [x], WHITE, h=ih)

    # CSDL
    db_y = core_y + outer_h + 46
    arrow(d, (cx, core_y + outer_h), (cx, db_y))
    db_h = box(d, 120, db_y, W - 240, "SQL Server: cơ sở dữ liệu GymPlanner",
               ["17 bảng · 25 trigger · 6 index · 5 view · 4 role phân quyền. Luồng gọi một chiều: "
                "giao diện → Service → DAO → cơ sở dữ liệu."], C_DB)
    img = img.crop((0, 0, W, db_y + db_h + 20))
    img.save(os.path.join(HERE, "kien_truc_phan_tang.png"))
    print("đã vẽ kien_truc_phan_tang.png", img.size)


def ke_thua_tong_the():
    groups = [
        ("Visitor (abstract)", ["Visitor_ID · First_Name · Last_Name · Phone"],
         [("Member", ["Email · Birth_Date · Photo · Registration_Date", "Hội viên có hợp đồng dài hạn"]),
          ("DailyPassTicket", ["Visit_Date · Daily_Rate", "Khách mua vé tập một ngày"])],
         "Hai lớp con rời nhau và phủ hết: mỗi khách là Member hoặc DailyPassTicket."),
        ("Staff (abstract)", ["Staff_ID · Full_Name · Phone · Role · Hire_Date · Salary"],
         [("Trainer", ["Specialty · Certification", "Huấn luyện viên"]),
          ("Receptionist", ["Shift_Type", "Lễ tân; ca sáng, chiều, tối"]),
          ("Manager", ["Không thêm thuộc tính", "Không có bảng riêng"])],
         "Role quyết định lớp con. Manager chỉ là Staff có Role = Manager nên không có bảng lớp con."),
        ("Session (abstract)", ["Session_ID · Session_Type · Room_ID · Trainer_ID · Start_Time · End_Time"],
         [("ClassSession", ["Class_Name · Max_Capacity", "Lớp tập nhóm"]),
          ("PtSession", ["Visitor_ID (hội viên đặt) · Booking_Status", "Buổi kèm riêng 1 kèm 1"])],
         "Mỗi buổi tập là lớp nhóm hoặc buổi PT, không bao giờ cả hai (trigger BR-SCH-06)."),
    ]
    W = 1300
    img = Image.new("RGB", (W, 1500), WHITE)
    d = ImageDraw.Draw(img)
    y = 20
    for pname, pl, children, note in groups:
        pw = 860
        px = (W - pw) // 2
        ph = box(d, px, y, pw, pname, pl, C_PARENT, center=True)
        n = len(children)
        gap = 24
        cw = (W - 40 - (n - 1) * gap) // n
        ch = max(measure(d, cw, c[0], c[1], title_size=24, text_size=TEXT) for c in children)
        cy = y + ph + 70
        mid_x = px + pw // 2
        d.line([(mid_x, y + ph + 24), (mid_x, cy - 36)], fill=LINE, width=3)
        hollow_triangle(d, (mid_x, y + ph), size=15)
        first = 20 + cw // 2
        last = 20 + (n - 1) * (cw + gap) + cw // 2
        d.line([(first, cy - 36), (last, cy - 36)], fill=LINE, width=3)
        for i, (cname, cl) in enumerate(children):
            x = 20 + i * (cw + gap)
            box(d, x, cy, cw, cname, cl, C_CHILD, h=ch, title_size=24, center=True)
            d.line([(x + cw // 2, cy), (x + cw // 2, cy - 36)], fill=LINE, width=3)
        ny = cy + ch + 14
        for ln in wrap(d, note, font(SMALL), W - 60):
            d.text((30, ny), ln, font=font(SMALL), fill=MUTED)
            ny += SMALL + 6
        y = ny + 36
    img = img.crop((0, 0, W, y))
    img.save(os.path.join(HERE, "ke_thua_tong_the.png"))
    print("đã vẽ ke_thua_tong_the.png", img.size)


if __name__ == "__main__":
    kien_truc_phan_tang()
    ke_thua_tong_the()

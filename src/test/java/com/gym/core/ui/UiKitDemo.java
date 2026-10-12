package com.gym.core.ui;

import com.gym.core.Validator;
import com.gym.core.dao.RoomDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Room;
import com.gym.core.model.StaffRole;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;

/**
 * Chạy thử bộ UiKit (không nằm trong ứng dụng). Chạy từ IDE hoặc:
 *   java -cp target/gym-planner-0.1.0.jar;target/test-classes com.gym.core.ui.UiKitDemo
 *
 * Cho thấy cách ghép DataTable + FormPanel + Validator + Dialogs + Toast + Async.
 * Các màn hình thật của TV2–TV5 viết theo đúng khuôn này.
 */
public final class UiKitDemo {

    private UiKitDemo() {
    }

    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(UiKitDemo::show);
    }

    private static void show() {
        RoomDao dao = new RoomDao();

        // Bảng phân trang
        DataTable<Room> table = new DataTable<>(5);
        table.addColumn("Mã", Room::getRoomId, 60)
                .addColumn("Tên phòng", Room::getRoomName, 200)
                .addColumn("Sức chứa", Room::getCapacity, 80)
                .addColumn("Tầng", Room::getFloorLocation, 100)
                .setLoader((page, size) -> dao.findPage(page, size));

        // Biểu mẫu
        FormPanel form = new FormPanel();
        form.addText("name", "Tên phòng", true);
        form.addText("capacity", "Sức chứa", true);
        form.addText("floor", "Tầng", false);
        form.addCombo("role", "Vai trò thử", false, StaffRole.values());
        form.addDate("date", "Ngày thử", false);

        JButton save = new JButton("Lưu thử (kiểm tra dữ liệu)");
        save.addActionListener(e -> {
            Validator v = new Validator().required(form.label("name"), form.getText("name"))
                    .check(isNumber(form.getText("capacity")), form.label("capacity") + " phải là số nguyên.");
            if (!v.isValid()) {
                Dialogs.warn(null, v.message());
                return;
            }
            Toast.show(table, "Dữ liệu hợp lệ!");
        });

        JButton failDb = new JButton("Gây lỗi DB (thêm phòng trùng tên)");
        failDb.addActionListener(e -> Async.run(table, () -> {
            Room first = dao.findAll().get(0);
            Room dup = new Room();
            dup.setRoomName(first.getRoomName());
            dup.setCapacity(5);
            return dao.insert(dup);               // trùng UQ_ROOM_Name -> lỗi 2627
        }, r -> Toast.show(table, "Không đáng có")));

        JButton failBusiness = new JButton("Gây lỗi nghiệp vụ");
        failBusiness.addActionListener(e -> {
            try {
                throw new BusinessException("Hội viên chưa có ảnh nên chưa thể đăng ký gói tập.");
            } catch (RuntimeException ex) {
                Dialogs.error(table, ex);
            }
        });

        JButton delete = new JButton("Hỏi xác nhận xóa");
        delete.addActionListener(e -> {
            Room selected = table.getSelected();
            String what = selected == null ? "dòng này" : "phòng " + selected.getRoomName();
            if (Dialogs.confirmDelete(table, what)) {
                Toast.show(table, "(Demo) Đã xóa " + what + " (chưa xóa thật)");
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(save);
        buttons.add(failDb);
        buttons.add(failBusiness);
        buttons.add(delete);

        JPanel right = new JPanel(new BorderLayout());
        right.add(form, BorderLayout.NORTH);

        JFrame frame = new JFrame("UiKit Demo");
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(table, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);
        root.add(buttons, BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 520);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        table.reload();
    }

    private static boolean isNumber(String text) {
        try {
            return text != null && new BigDecimal(text).signum() > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

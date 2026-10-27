package com.gym.app.home;

import com.gym.app.Session;
import com.gym.core.DateTimeUtils;
import com.gym.core.ui.Async;
import com.gym.core.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDate;

/**
 * Trang chủ "Việc cần làm hôm nay": lời chào ở trên, danh sách việc ở dưới.
 * Quản lý và Lễ tân thấy 3 dòng việc (gói hết hạn hôm nay, lớp học hôm nay, thiết bị hỏng);
 * Huấn luyện viên chỉ thấy lời chào và một dòng nhắc xem lịch dạy.
 * Màn hình chỉ gọi Service (không gọi DAO) và nạp số liệu bằng Async như DashboardPanel.
 */
public class HomePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] WEEKDAY = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"};

    private final HomeService service = new HomeService();
    private final boolean hasTasks = HomeService.hasTasks(Session.getRole());

    private final TaskRow expiringRow = new TaskRow("gói tập hết hạn hôm nay, cần nhắc khách gia hạn",
            "Không có gói tập nào hết hạn hôm nay", Theme.WARNING);
    private final TaskRow classesRow = new TaskRow("lớp học diễn ra hôm nay",
            "Hôm nay không có lớp học nào", Theme.INFO);
    private final TaskRow brokenRow = new TaskRow("thiết bị đang hỏng, cần xử lý",
            "Không có thiết bị nào hỏng", Theme.DANGER);

    public HomePanel() {
        setLayout(new BorderLayout(0, 20));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        add(buildHeader(), BorderLayout.NORTH);
        add(hasTasks ? buildTaskList() : buildTrainerNote(), BorderLayout.CENTER);

        if (hasTasks) {
            refresh();
        }
    }

    private JPanel buildHeader() {
        JLabel greeting = new JLabel("Xin chào, " + Session.getStaffName());
        greeting.setFont(greeting.getFont().deriveFont(Font.BOLD, 24f));
        JLabel sub = new JLabel(Session.getRole().getLabel() + "  ·  " + dateLabel(LocalDate.now()));
        sub.setForeground(Theme.MUTED);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(greeting);
        text.add(Box.createVerticalStrut(4));
        text.add(sub);

        JPanel header = new JPanel(new BorderLayout());
        header.add(text, BorderLayout.WEST);
        if (hasTasks) {
            JButton reload = new JButton("Làm mới");
            reload.addActionListener(e -> refresh());
            JPanel buttonArea = new JPanel(new BorderLayout());     // BorderLayout.EAST kéo nút cao bằng cả khối, nên bọc lại
            buttonArea.add(reload, BorderLayout.NORTH);              // đặt vào NORTH thì nút giữ chiều cao thường
            header.add(buttonArea, BorderLayout.EAST);
        }
        return header;
    }

    private JPanel buildTaskList() {
        JLabel title = new JLabel("Việc cần làm hôm nay");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(title);
        list.add(Box.createVerticalStrut(10));
        for (TaskRow row : new TaskRow[]{expiringRow, classesRow, brokenRow}) {
            row.setAlignmentX(LEFT_ALIGNMENT);
            list.add(row);
            list.add(Box.createVerticalStrut(10));
        }

        JPanel top = new JPanel(new BorderLayout());       // dồn danh sách lên trên, phần thừa để trống bên dưới
        top.add(list, BorderLayout.NORTH);
        return top;
    }

    private JPanel buildTrainerNote() {
        JLabel note = new JLabel("Bạn có thể xem lịch dạy của mình ở mục \"Lịch tập\" trong menu bên trái.");
        note.setForeground(Theme.MUTED);
        JPanel top = new JPanel(new BorderLayout());
        top.add(note, BorderLayout.NORTH);
        return top;
    }

    /** Nạp lại số liệu ở nền, xong thì hiện lên 3 dòng. Chỉ gọi khi vai trò có danh sách việc. */
    public void refresh() {
        expiringRow.clear();
        classesRow.clear();
        brokenRow.clear();
        Async.run(this, service::todayTasks, tasks -> {
            expiringRow.setCount(tasks.expiringToday());
            classesRow.setCount(tasks.classesToday());
            brokenRow.setCount(tasks.brokenEquipment());
        });
    }

    /** "Thứ Hai, 26/10/2026". Tách riêng thành hàm thuần để test được. */
    static String dateLabel(LocalDate day) {
        return WEEKDAY[day.getDayOfWeek().getValue() - 1] + ", " + DateTimeUtils.format(day);   // getValue(): Thứ Hai = 1 ... Chủ nhật = 7
    }

    /**
     * Một dòng "việc cần làm": con số lớn bên trái và mô tả bên phải, có vạch màu bên trái như StatCard.
     * Có việc (số lớn hơn 0) thì số hiện bằng màu nổi bật và dùng câu mô tả "someText";
     * không có việc thì số xám và dùng câu "noneText" (ví dụ "Không có gói nào hết hạn hôm nay").
     * Cách dùng: dựng dòng với hai câu và màu, rồi gọi setCount(...) khi có số liệu (trên luồng giao diện).
     * Chỉ HomePanel dùng nên đặt luôn bên trong đây (không để mức private để test truy cập được).
     */
    static class TaskRow extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final String EMPTY_VALUE = "—";

        private final String someText;
        private final String noneText;
        private final Color color;
        private final JLabel numberLabel = new JLabel(EMPTY_VALUE, JLabel.CENTER);
        private final JLabel textLabel = new JLabel();

        TaskRow(String someText, String noneText, Color color) {
            super(new BorderLayout(16, 0));
            this.someText = someText;
            this.noneText = noneText;
            this.color = color;

            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 5, 0, 0, color),                   // vạch màu bên trái
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));                        // không giãn cao khi cửa sổ lớn

            numberLabel.setFont(numberLabel.getFont().deriveFont(Font.BOLD, 28f));
            numberLabel.setPreferredSize(new Dimension(70, 40));
            textLabel.setFont(textLabel.getFont().deriveFont(15f));
            add(numberLabel, BorderLayout.WEST);
            add(textLabel, BorderLayout.CENTER);

            clear();
        }

        /** Hiện số việc. 0 nghĩa là không có việc nào. */
        void setCount(int count) {
            numberLabel.setText(String.valueOf(count));
            numberLabel.setForeground(count > 0 ? color : Theme.MUTED);
            textLabel.setText(count > 0 ? someText : noneText);
            textLabel.setForeground(count > 0 ? Color.BLACK : Theme.MUTED);
        }

        /** Về trạng thái chưa có số liệu (đang nạp). */
        void clear() {
            numberLabel.setText(EMPTY_VALUE);
            numberLabel.setForeground(Theme.MUTED);
            textLabel.setText(someText);
            textLabel.setForeground(Theme.MUTED);
        }

        /** Con số đang hiện (để test). */
        String number() {
            return numberLabel.getText();
        }

        /** Câu mô tả đang hiện (để test). */
        String text() {
            return textLabel.getText();
        }
    }
}

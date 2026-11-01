package com.gym.scheduling.ui;

import com.gym.core.DateTimeUtils;
import com.gym.core.model.Session;
import com.gym.core.model.PtSession;
import com.gym.core.model.BookingStatus;
import com.gym.core.ui.Async;
import com.gym.core.ui.Dialogs;
import com.gym.scheduling.service.SessionService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Bảng lịch tập theo tuần; tải dữ liệu ở luồng nền để không khóa giao diện. */
public class WeeklySchedulePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
            "Mã buổi", "Loại", "Tên buổi", "Ngày", "Bắt đầu", "Kết thúc",
            "Phòng", "HLV", "Trạng thái"
    };
    private final SessionService sessionService;
    private final boolean trainerOnly;
    private LocalDate weekDate = LocalDate.now();
    private List<Session> displayedSessions = List.of();
    private final JLabel weekLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public WeeklySchedulePanel() {
        this(new SessionService(), false);
    }

    public WeeklySchedulePanel(SessionService sessionService) {
        this(sessionService, false);
    }

    public WeeklySchedulePanel(SessionService sessionService, boolean trainerOnly) {
        super(new BorderLayout(0, 10));
        this.sessionService = sessionService;
        this.trainerOnly = trainerOnly;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel(trainerOnly ? "Lịch của tôi" : "Lịch tập theo tuần");
        title.setFont(title.getFont().deriveFont(20f));
        JPanel heading = new JPanel(new BorderLayout(0, 8));
        heading.add(title, BorderLayout.NORTH);

        JButton previousButton = new JButton("‹ Tuần trước");
        JButton nextButton = new JButton("Tuần sau ›");
        JButton refreshButton = new JButton("Làm mới");
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        navigation.add(previousButton);
        navigation.add(weekLabel);
        navigation.add(nextButton);
        navigation.add(refreshButton);
        heading.add(navigation, BorderLayout.CENTER);

        JButton rescheduleButton = new JButton("Dời buổi đã chọn");
        JButton statusButton = new JButton("Đổi trạng thái PT");
        JButton deleteButton = new JButton("Xóa buổi đã chọn");
        JButton helpButton = new JButton("?");
        helpButton.setToolTipText("Chọn một dòng để dời lịch, đổi trạng thái PT hoặc xóa buổi.");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        actions.add(rescheduleButton);
        actions.add(statusButton);
        actions.add(deleteButton);
        actions.add(helpButton);
        heading.add(actions, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(28);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        previousButton.addActionListener(e -> {
            weekDate = weekDate.minusWeeks(1);
            reload();
        });
        nextButton.addActionListener(e -> {
            weekDate = weekDate.plusWeeks(1);
            reload();
        });
        refreshButton.addActionListener(e -> reload());
        rescheduleButton.addActionListener(e -> rescheduleSelected());
        statusButton.addActionListener(e -> changeSelectedPtStatus());
        deleteButton.addActionListener(e -> deleteSelected());
        helpButton.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Xem lịch trong tuần bằng nút Tuần trước/Tuần sau.\n"
                        + "Chọn một buổi rồi dùng các nút bên dưới để dời lịch, đổi trạng thái PT hoặc xóa.\n"
                        + "Khi dời lịch, hệ thống kiểm tra trùng phòng và huấn luyện viên.",
                "Hướng dẫn lịch tập", JOptionPane.INFORMATION_MESSAGE));
        reload();
    }

    /** Tải lại lịch của tuần đang được xem. */
    public void refreshSchedule() {
        reload();
    }

    private void reload() {
        LocalDate monday = DateTimeUtils.startOfWeek(weekDate);
        LocalDate sunday = monday.plusDays(6);
        weekLabel.setText(DateTimeUtils.format(monday) + " – " + DateTimeUtils.format(sunday));
        statusLabel.setText("Đang tải lịch...");
        LocalDate selectedWeek = weekDate;
        Async.run(this, () -> trainerOnly
                        ? sessionService.findMyWeeklySchedule(selectedWeek)
                        : sessionService.findWeeklySchedule(selectedWeek),
                this::showSessions,
                error -> {
                    statusLabel.setText("Không tải được lịch tập.");
                    Dialogs.error(this, error);
                });
    }

    private void showSessions(List<Session> sessions) {
        displayedSessions = List.copyOf(sessions);
        Object[][] rows = new Object[sessions.size()][COLUMNS.length];
        for (int i = 0; i < sessions.size(); i++) {
            Session session = sessions.get(i);
            LocalDateTime start = session.getStartTime();
            LocalDateTime end = session.getEndTime();
            rows[i] = new Object[]{
                    session.getSessionId(),
                    session.getSessionType() == null ? "" : session.getSessionType().getLabel(),
                    session.getSessionTitle(),
                    start == null ? "" : DateTimeUtils.format(start.toLocalDate()),
                    DateTimeUtils.formatTime(start),
                    DateTimeUtils.formatTime(end),
                    session.getRoomId(),
                    session.getTrainerId(),
                    session instanceof PtSession pt && pt.getBookingStatus() != null
                            ? pt.getBookingStatus().getLabel() : ""
            };
        }
        tableModel.setDataVector(rows, COLUMNS);
        statusLabel.setText(sessions.isEmpty()
                ? (trainerOnly
                ? "Tuần này bạn chưa được phân lịch."
                : "Chưa có buổi tập trong tuần này. Hãy dùng Tạo lớp hoặc Đặt lịch PT.")
                : "Có " + sessions.size() + " buổi trong tuần này.");
    }

    private Session selectedSession() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một buổi tập trong lịch.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selected);
        if (modelRow >= displayedSessions.size()) {
            return null;
        }
        return displayedSessions.get(modelRow);
    }

    private void rescheduleSelected() {
        if (trainerOnly) {
            JOptionPane.showMessageDialog(this, "Chỉ quản lý hoặc lễ tân được dời lịch buổi tập.");
            return;
        }
        Session selected = selectedSession();
        if (selected == null) {
            return;
        }
        Optional<Session> changed = RescheduleSessionDialog.show(this, selected);
        changed.ifPresent(session -> Async.run(this, () -> {
            sessionService.updateSession(session);
            return null;
        }, ignored -> reload()));
    }

    private void changeSelectedPtStatus() {
        if (trainerOnly) {
            JOptionPane.showMessageDialog(this, "Chỉ quản lý hoặc lễ tân được đổi trạng thái PT.");
            return;
        }
        Session selected = selectedSession();
        if (!(selected instanceof PtSession pt)) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một buổi PT.");
            return;
        }
        if (pt.getBookingStatus() == null) {
            JOptionPane.showMessageDialog(this, "Buổi PT chưa có trạng thái hợp lệ.");
            return;
        }

        Object[] options = switch (pt.getBookingStatus()) {
            case SCHEDULED -> new Object[]{"Hoàn thành", "Hủy"};
            case CANCELLED -> new Object[]{"Khôi phục"};
            case COMPLETED -> new Object[]{};
        };
        if (options.length == 0) {
            JOptionPane.showMessageDialog(this, "Buổi PT đã hoàn thành, không thể đổi trạng thái.");
            return;
        }
        int choice = JOptionPane.showOptionDialog(this, "Chọn trạng thái mới:",
                "Trạng thái PT", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (choice < 0) {
            return;
        }
        BookingStatus status = switch (options[choice].toString()) {
            case "Hoàn thành" -> BookingStatus.COMPLETED;
            case "Hủy" -> BookingStatus.CANCELLED;
            default -> BookingStatus.SCHEDULED;
        };
        if (status == BookingStatus.CANCELLED
                && JOptionPane.showOptionDialog(this,
                "Hủy buổi PT này? Khung giờ sẽ được mở lại cho lịch khác.",
                "Xác nhận hủy PT", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null,
                new Object[]{"Hủy buổi", "Giữ lại"}, "Giữ lại") != 0) {
            return;
        }
        Async.run(this, () -> {
            sessionService.changePtStatus(pt.getSessionId(), status);
            return null;
        }, ignored -> reload());
    }

    private void deleteSelected() {
        if (trainerOnly) {
            JOptionPane.showMessageDialog(this, "Chỉ quản lý hoặc lễ tân được xóa buổi tập.");
            return;
        }
        Session selected = selectedSession();
        if (selected == null) {
            return;
        }
        Async.run(this, () -> sessionService.previewDelete(selected.getSessionId()), impact -> {
            String message = "Xóa \"" + impact.sessionTitle() + "\"?";
            if (impact.deletedRegistrations() > 0) {
                message += "\nThao tác này cũng xóa "
                        + impact.deletedRegistrations() + " đăng ký lớp.";
            }
            message += "\nThao tác này không thể hoàn tác.";
            int answer = JOptionPane.showOptionDialog(this, message, "Xác nhận xóa",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new Object[]{"Xóa buổi", "Không xóa"}, "Không xóa");
            if (answer == 0) {
                Async.run(this, () -> sessionService.deleteSession(selected.getSessionId()),
                        ignored -> reload());
            }
        });
    }
}

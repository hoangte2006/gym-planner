package com.gym.scheduling.ui;

import com.gym.core.DateTimeUtils;
import com.gym.core.model.Session;
import com.gym.core.ui.Async;
import com.gym.core.ui.Dialogs;
import com.gym.scheduling.service.SessionService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
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

/** Bảng lịch tập theo tuần; tải dữ liệu ở luồng nền để không khóa giao diện. */
public class WeeklySchedulePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final SessionService sessionService;
    private LocalDate weekDate = LocalDate.now();
    private final JLabel weekLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã buổi", "Loại", "Tên buổi", "Ngày", "Bắt đầu", "Kết thúc", "Phòng", "HLV"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public WeeklySchedulePanel() {
        this(new SessionService());
    }

    public WeeklySchedulePanel(SessionService sessionService) {
        super(new BorderLayout(0, 10));
        this.sessionService = sessionService;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Lịch tập theo tuần");
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
        Async.run(this, () -> sessionService.findWeeklySchedule(selectedWeek), this::showSessions,
                error -> {
                    statusLabel.setText("Không tải được lịch tập.");
                    Dialogs.error(this, error);
                });
    }

    private void showSessions(List<Session> sessions) {
        tableModel.setRowCount(0);
        for (Session session : sessions) {
            LocalDateTime start = session.getStartTime();
            LocalDateTime end = session.getEndTime();
            tableModel.addRow(new Object[]{
                    session.getSessionId(),
                    session.getSessionType() == null ? "" : session.getSessionType().getLabel(),
                    session.getSessionTitle(),
                    start == null ? "" : DateTimeUtils.format(start.toLocalDate()),
                    DateTimeUtils.formatTime(start),
                    DateTimeUtils.formatTime(end),
                    session.getRoomId(),
                    session.getTrainerId()
            });
        }
        statusLabel.setText("Có " + sessions.size() + " buổi trong tuần này.");
    }
}

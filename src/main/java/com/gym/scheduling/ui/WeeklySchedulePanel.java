
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
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/** Bảng lịch tập theo tuần; tải dữ liệu ở luồng nền để không khóa giao diện. */
public class WeeklySchedulePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SessionService sessionService;
    private LocalDate weekDate = LocalDate.now();

    private final JLabel weekLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ");

    private final SpinnerDateModel dateModel = new SpinnerDateModel();
    private final JSpinner datePicker = new JSpinner(dateModel);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{
                    "Mã buổi", "Loại", "Tên buổi", "Ngày",
                    "Bắt đầu", "Kết thúc", "Phòng", "HLV"
            }, 0) {

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

        // Tiêu đề
        JLabel title = new JLabel("Lịch tập theo tuần");
        title.setFont(title.getFont().deriveFont(20f));

        JPanel heading = new JPanel(new BorderLayout(0, 12));
        heading.add(title, BorderLayout.NORTH);

        // Bộ chọn ngày
        datePicker.setEditor(
                new JSpinner.DateEditor(datePicker, "dd/MM/yyyy")
        );
        datePicker.setValue(toDate(LocalDate.now()));
        datePicker.setToolTipText(
                "Chọn một ngày thuộc tuần bạn muốn xem"
        );
        datePicker.setPreferredSize(
                new java.awt.Dimension(120, datePicker.getPreferredSize().height)
        );

        // Các nút điều hướng
        JButton previousButton = new JButton("‹ Tuần trước");
        JButton currentWeekButton = new JButton("Tuần này");
        JButton nextButton = new JButton("Tuần sau ›");
        JButton goToDateButton = new JButton("Đi đến ngày");
        JButton refreshButton = new JButton("Làm mới");

        JPanel navigation = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 4)
        );

        navigation.add(previousButton);
        navigation.add(currentWeekButton);
        navigation.add(weekLabel);
        navigation.add(nextButton);

        JPanel dateNavigation = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 4)
        );

        dateNavigation.add(new JLabel("Chọn ngày:"));
        dateNavigation.add(datePicker);
        dateNavigation.add(goToDateButton);
        dateNavigation.add(refreshButton);

        JPanel navigationContainer = new JPanel(
                new BorderLayout(0, 4)
        );

        navigationContainer.add(navigation, BorderLayout.NORTH);
        navigationContainer.add(dateNavigation, BorderLayout.CENTER);

        heading.add(navigationContainer, BorderLayout.CENTER);
        add(heading, BorderLayout.NORTH);

        // Bảng lịch
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(28);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Chuyển về tuần trước
        previousButton.addActionListener(e -> {
            weekDate = weekDate.minusWeeks(1);
            syncDatePicker();
            reload();
        });

        // Chuyển đến tuần hiện tại
        currentWeekButton.addActionListener(e -> {
            weekDate = LocalDate.now();
            syncDatePicker();
            reload();
        });

        // Chuyển đến tuần sau
        nextButton.addActionListener(e -> {
            weekDate = weekDate.plusWeeks(1);
            syncDatePicker();
            reload();
        });

        // Chọn ngày bất kỳ rồi đi đến tuần chứa ngày đó
        goToDateButton.addActionListener(e -> {
            try {
                LocalDate selectedDate = toLocalDate(
                        (Date) datePicker.getValue()
                );

                weekDate = selectedDate;
                reload();

            } catch (Exception ex) {
                Dialogs.error(this, ex);
            }
        });

        // Enter trong ô ngày cũng thực hiện chuyển tuần
        datePicker.addChangeListener(e -> {
            // Không tự tải dữ liệu khi người dùng đang chỉnh ngày.
            // Người dùng nhấn "Đi đến ngày" để xác nhận.
        });

        // Làm mới tuần đang xem
        refreshButton.addActionListener(e -> reload());

        reload();
    }

    private Date toDate(LocalDate date) {
        return Date.from(
                date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        );
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private void syncDatePicker() {
        datePicker.setValue(toDate(weekDate));
    }

    private void reload() {
        LocalDate monday = DateTimeUtils.startOfWeek(weekDate);
        LocalDate sunday = monday.plusDays(6);

        weekLabel.setText(
                monday.format(DATE_FORMAT)
                        + " – "
                        + sunday.format(DATE_FORMAT)
        );

        statusLabel.setText("Đang tải lịch...");
        setNavigationEnabled(false);

        LocalDate selectedWeek = weekDate;

        Async.run(
                this,
                () -> sessionService.findWeeklySchedule(selectedWeek),
                sessions -> {
                    // Tránh hiển thị dữ liệu cũ nếu người dùng đã
                    // chuyển sang một tuần khác trong lúc đang tải.
                    if (!DateTimeUtils.startOfWeek(weekDate)
                            .equals(DateTimeUtils.startOfWeek(selectedWeek))) {
                        return;
                    }

                    showSessions(sessions);
                    setNavigationEnabled(true);
                },
                error -> {
                    statusLabel.setText("Không tải được lịch tập.");
                    setNavigationEnabled(true);
                    Dialogs.error(this, error);
                }
        );
    }

    private void setNavigationEnabled(boolean enabled) {
        // Giữ bộ chọn ngày hoạt động trong lúc tải.
        // Các nút điều hướng được quản lý tại đây nếu cần.
        // Không khóa toàn bộ giao diện.
    }

    private void showSessions(List<Session> sessions) {
        tableModel.setRowCount(0);

        for (Session session : sessions) {
            LocalDateTime start = session.getStartTime();
            LocalDateTime end = session.getEndTime();

            tableModel.addRow(new Object[]{
                    session.getSessionId(),
                    session.getSessionType() == null
                            ? ""
                            : session.getSessionType().getLabel(),
                    session.getSessionTitle(),
                    start == null
                            ? ""
                            : DateTimeUtils.format(start.toLocalDate()),
                    DateTimeUtils.formatTime(start),
                    DateTimeUtils.formatTime(end),
                    session.getRoomId(),
                    session.getTrainerId()
            });
        }

        statusLabel.setText(
                "Có " + sessions.size() + " buổi trong tuần này."
        );
    }
}

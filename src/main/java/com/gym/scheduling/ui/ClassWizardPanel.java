package com.gym.scheduling.ui;

import com.gym.core.model.ClassSession;
import com.gym.core.model.Room;
import com.gym.core.model.Trainer;
import com.gym.core.ui.Dialogs;
import com.gym.scheduling.service.ClassSchedulingService;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class ClassWizardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ClassSchedulingService schedulingService;
    private final Runnable onClassCreated;

    private final JTextField nameField = new JTextField(22);
    private final JSpinner capacitySpinner =
            new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));

    private final JComboBox<Room> roomCombo = new JComboBox<>();
    private final JComboBox<Trainer> trainerCombo = new JComboBox<>();

    private final JSpinner dateSpinner =
            new JSpinner(new SpinnerDateModel());

    private final JComboBox<String> startCombo = new JComboBox<>();
    private final JComboBox<String> endCombo = new JComboBox<>();
    private final JComboBox<LocalDateTime> availableStarts = new JComboBox<>();

    private final JButton createButton = new JButton("Tạo lớp");
    private final JButton reloadButton = new JButton("Tải lại danh sách");
    private final JButton findSlotsButton = new JButton("Tìm giờ trống");
    private final JButton useSlotButton = new JButton("Dùng giờ đã chọn");

    public ClassWizardPanel() {
        this(new ClassSchedulingService(), null);
    }

    public ClassWizardPanel(
            ClassSchedulingService schedulingService,
            Runnable onClassCreated) {

        super(new BorderLayout(10, 10));

        this.schedulingService = schedulingService;
        this.onClassCreated = onClassCreated != null ? onClassCreated : () -> {};

        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Tạo lớp tập nhóm");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.NORTH);
        header.add(new JLabel(
                "Nhập thông tin lớp. Hệ thống sẽ kiểm tra sức chứa và lịch trùng."
        ), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, gbc, row++, "Tên lớp:", nameField);
        addRow(form, gbc, row++, "Sức chứa:", capacitySpinner);
        addRow(form, gbc, row++, "Phòng tập:", roomCombo);
        addRow(form, gbc, row++, "Huấn luyện viên:", trainerCombo);

        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        dateSpinner.setValue(toDate(LocalDate.now()));
        addRow(form, gbc, row++, "Ngày tập:", dateSpinner);

        populateTimes();
        addRow(form, gbc, row++, "Giờ bắt đầu:", startCombo);
        addRow(form, gbc, row++, "Giờ kết thúc:", endCombo);
        JPanel availability = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        availability.add(findSlotsButton);
        availability.add(availableStarts);
        availability.add(useSlotButton);
        addRow(form, gbc, row++, "Giờ còn trống:", availability);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(reloadButton, gbc);
        add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Xóa form");
        footer.add(clearButton);
        footer.add(createButton);
        add(footer, BorderLayout.SOUTH);

        reloadButton.addActionListener(e -> loadResources());
        findSlotsButton.addActionListener(e -> findAvailableTimes());
        useSlotButton.addActionListener(e -> applyAvailableTime());
        createButton.addActionListener(e -> createClass());
        clearButton.addActionListener(e -> clearForm());
        loadResources();
    }

    private void findAvailableTimes() {
        Room room = (Room) roomCombo.getSelectedItem();
        Trainer trainer = (Trainer) trainerCombo.getSelectedItem();
        if (room == null || trainer == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn phòng và huấn luyện viên trước.",
                    "Thiếu dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            LocalTime selectedStart = LocalTime.parse(
                    (String) startCombo.getSelectedItem());
            LocalTime selectedEnd = LocalTime.parse(
                    (String) endCombo.getSelectedItem());
            Duration duration = Duration.between(selectedStart, selectedEnd);
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException(
                        "Giờ kết thúc phải sau giờ bắt đầu để xác định thời lượng lớp.");
            }

            LocalDate date = toLocalDate((Date) dateSpinner.getValue());
            findSlotsButton.setEnabled(false);
            SwingWorker<List<LocalDateTime>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<LocalDateTime> doInBackground() {
                    return schedulingService.findAvailableStarts(
                            room.getRoomId(), trainer.getStaffId(), date, duration);
                }

                @Override
                protected void done() {
                    try {
                        List<LocalDateTime> starts = get();
                        DefaultComboBoxModel<LocalDateTime> model =
                                new DefaultComboBoxModel<>();
                        starts.forEach(model::addElement);
                        availableStarts.setModel(model);
                        availableStarts.setRenderer((list, value, index,
                                isSelected, cellHasFocus) -> {
                            JLabel label = new JLabel(value == null ? ""
                                    : value.toLocalTime().toString());
                            label.setOpaque(true);
                            if (isSelected) {
                                label.setBackground(list.getSelectionBackground());
                                label.setForeground(list.getSelectionForeground());
                            }
                            return label;
                        });
                        if (starts.isEmpty()) {
                            JOptionPane.showMessageDialog(
                                    ClassWizardPanel.this,
                                    "Không có giờ trống phù hợp trong ngày đã chọn.",
                                    "Không tìm thấy giờ trống",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    } catch (Exception ex) {
                        Dialogs.error(ClassWizardPanel.this, ex);
                    } finally {
                        findSlotsButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Dữ liệu không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void applyAvailableTime() {
        LocalDateTime start = (LocalDateTime) availableStarts.getSelectedItem();
        if (start == null) {
            return;
        }

        LocalTime currentStart = LocalTime.parse(
                (String) startCombo.getSelectedItem());
        LocalTime currentEnd = LocalTime.parse(
                (String) endCombo.getSelectedItem());
        Duration duration = Duration.between(currentStart, currentEnd);

        dateSpinner.setValue(toDate(start.toLocalDate()));
        startCombo.setSelectedItem(start.toLocalTime().toString());
        endCombo.setSelectedItem(start.plus(duration).toLocalTime().toString());
    }

    private void addRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label,
            JComponent component) {

        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;

        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;

        panel.add(component, gbc);
    }

    private void populateTimes() {
        for (int minute = 6 * 60; minute <= 22 * 60; minute += 30) {
            String time = String.format(
                    "%02d:%02d", minute / 60, minute % 60
            );
            startCombo.addItem(time);
            endCombo.addItem(time);
        }

        startCombo.setSelectedItem("08:00");
        endCombo.setSelectedItem("09:00");
    }

    private void loadResources() {
        createButton.setEnabled(false);
        reloadButton.setEnabled(false);

        SwingWorker<ResourceData, Void> worker =
                new SwingWorker<>() {

            @Override
            protected ResourceData doInBackground() {
                return new ResourceData(
                        schedulingService.findRooms(),
                        schedulingService.findTrainers()
                );
            }

            @Override
            protected void done() {
                try {
                    ResourceData data = get();

                    roomCombo.removeAllItems();
                    for (Room room : data.rooms()) {
                        roomCombo.addItem(room);
                    }

                    trainerCombo.removeAllItems();
                    for (Trainer trainer : data.trainers()) {
                        trainerCombo.addItem(trainer);
                    }

                    roomCombo.setRenderer((list, value, index,
                                           isSelected, cellHasFocus) -> {
                        JLabel label = new JLabel(value == null
                                ? ""
                                : value.getRoomName()
                                  + " (sức chứa " + value.getCapacity() + ")");
                        label.setOpaque(true);
                        if (isSelected) {
                            label.setBackground(list.getSelectionBackground());
                            label.setForeground(list.getSelectionForeground());
                        }
                        return label;
                    });

                    trainerCombo.setRenderer((list, value, index,
                                              isSelected, cellHasFocus) -> {
                        JLabel label = new JLabel(value == null
                                ? ""
                                : value.getFullName());
                        label.setOpaque(true);
                        if (isSelected) {
                            label.setBackground(list.getSelectionBackground());
                            label.setForeground(list.getSelectionForeground());
                        }
                        return label;
                    });

                    if (data.rooms().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                ClassWizardPanel.this,
                                "Chưa có phòng tập trong database.",
                                "Thông báo",
                                JOptionPane.WARNING_MESSAGE
                        );
                    }

                    if (data.trainers().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                ClassWizardPanel.this,
                                "Chưa có huấn luyện viên trong database.",
                                "Thông báo",
                                JOptionPane.WARNING_MESSAGE
                        );
                    }

                } catch (Exception ex) {
                    Dialogs.error(ClassWizardPanel.this, ex);
                } finally {
                    createButton.setEnabled(true);
                    reloadButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void createClass() {
        Room room = (Room) roomCombo.getSelectedItem();
        Trainer trainer = (Trainer) trainerCombo.getSelectedItem();

        if (room == null || trainer == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng tải database và chọn phòng, huấn luyện viên.",
                    "Thiếu dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            LocalDate date = toLocalDate((Date) dateSpinner.getValue());

            LocalTime start = LocalTime.parse(
                    (String) startCombo.getSelectedItem()
            );
            LocalTime end = LocalTime.parse(
                    (String) endCombo.getSelectedItem()
            );

            LocalDateTime startTime = LocalDateTime.of(date, start);
            LocalDateTime endTime = LocalDateTime.of(date, end);

            createButton.setEnabled(false);

            SwingWorker<ClassSession, Void> worker =
                    new SwingWorker<>() {

                @Override
                protected ClassSession doInBackground() {
                    return schedulingService.createClass(
                            nameField.getText(),
                            (Integer) capacitySpinner.getValue(),
                            room.getRoomId(),
                            trainer.getStaffId(),
                            startTime,
                            endTime
                    );
                }

                @Override
                protected void done() {
                    try {
                        ClassSession created = get();

                        JOptionPane.showMessageDialog(
                                ClassWizardPanel.this,
                                "Tạo lớp thành công!\n"
                                + "Mã buổi: " + created.getSessionId()
                                + "\nTên lớp: " + created.getClassName(),
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        clearForm();
                        onClassCreated.run();

                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null
                                ? ex.getCause() : ex;

                        String message = cause.getMessage();
                        JOptionPane.showMessageDialog(
                                ClassWizardPanel.this,
                                message == null
                                        ? "Không thể tạo lớp."
                                        : message,
                                "Không thể tạo lớp",
                                JOptionPane.ERROR_MESSAGE
                        );
                    } finally {
                        createButton.setEnabled(true);
                    }
                }
            };

            worker.execute();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng kiểm tra ngày và giờ tập.",
                    "Dữ liệu không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void clearForm() {
        nameField.setText("");
        capacitySpinner.setValue(20);
        dateSpinner.setValue(toDate(LocalDate.now()));
        startCombo.setSelectedItem("08:00");
        endCombo.setSelectedItem("09:00");
        nameField.requestFocusInWindow();
    }

    private static Date toDate(LocalDate date) {
        return Date.from(
                date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        );
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private record ResourceData(
            List<Room> rooms,
            List<Trainer> trainers) {
    }
}
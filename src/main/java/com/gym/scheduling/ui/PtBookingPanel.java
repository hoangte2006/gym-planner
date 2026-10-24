package com.gym.scheduling.ui;

import com.gym.core.model.Member;
import com.gym.core.model.Room;
import com.gym.core.model.Trainer;
import com.gym.core.ui.Dialogs;
import com.gym.scheduling.service.PtSchedulingService;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class PtBookingPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final PtSchedulingService service;
    private final JTextField memberSearchField = new JTextField(18);
    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JComboBox<Trainer> trainerCombo = new JComboBox<>();
    private final JComboBox<Room> roomCombo = new JComboBox<>();
    private final JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
    private final JComboBox<String> startCombo = new JComboBox<>();
    private final JComboBox<String> endCombo = new JComboBox<>();
    private final JComboBox<LocalDateTime> availableStarts = new JComboBox<>();
    private final JButton searchMembersButton = new JButton("Tìm hội viên");
    private final JButton findSlotsButton = new JButton("Tìm giờ trống");
    private final JButton bookButton = new JButton("Đặt lịch PT");

    public PtBookingPanel() {
        this(new PtSchedulingService());
    }

    public PtBookingPanel(PtSchedulingService service) {
        super(new BorderLayout(10, 10));
        this.service = service;
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 6, 7, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        JPanel memberPanel = new JPanel(new BorderLayout(6, 0));
        memberPanel.add(memberSearchField, BorderLayout.CENTER);
        memberPanel.add(searchMembersButton, BorderLayout.EAST);
        row = addRow(form, gbc, row, "Tìm hội viên:", memberPanel);
        row = addRow(form, gbc, row, "Hội viên:", memberCombo);
        row = addRow(form, gbc, row, "Huấn luyện viên:", trainerCombo);
        row = addRow(form, gbc, row, "Phòng tập:", roomCombo);

        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        dateSpinner.setValue(toDate(LocalDate.now()));
        row = addRow(form, gbc, row, "Ngày tập:", dateSpinner);
        populateTimes();
        row = addRow(form, gbc, row, "Giờ bắt đầu:", startCombo);
        row = addRow(form, gbc, row, "Giờ kết thúc:", endCombo);

        JPanel availability = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        availability.add(findSlotsButton);
        availability.add(availableStarts);
        JButton useSlotButton = new JButton("Dùng giờ đã chọn");
        availability.add(useSlotButton);
        addRow(form, gbc, row, "Giờ còn trống:", availability);
        add(form, BorderLayout.NORTH);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(bookButton);
        add(footer, BorderLayout.SOUTH);

        memberCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : memberText(value));
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        trainerCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : value.getFullName());
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        roomCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? ""
                    : value.getRoomName() + " (sức chứa " + value.getCapacity() + ")");
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        availableStarts.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : value.toLocalTime().toString());
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });

        searchMembersButton.addActionListener(e -> searchMembers());
        findSlotsButton.addActionListener(e -> findAvailableTimes());
        useSlotButton.addActionListener(e -> applyAvailableTime());
        bookButton.addActionListener(e -> bookSession());
        loadResources();
    }

    private static int addRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label,
            JComponent component) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
        return row + 1;
    }

    private void populateTimes() {
        for (int minute = 6 * 60; minute <= 22 * 60; minute += 30) {
            String time = String.format("%02d:%02d", minute / 60, minute % 60);
            startCombo.addItem(time);
            endCombo.addItem(time);
        }
        startCombo.setSelectedItem("09:00");
        endCombo.setSelectedItem("10:00");
    }

    private void loadResources() {
        setBusy(true);
        SwingWorker<ResourceData, Void> worker = new SwingWorker<>() {
            @Override
            protected ResourceData doInBackground() {
                return new ResourceData(service.findRooms(), service.findTrainers());
            }

            @Override
            protected void done() {
                try {
                    ResourceData data = get();
                    data.rooms().forEach(roomCombo::addItem);
                    data.trainers().forEach(trainerCombo::addItem);
                } catch (Exception ex) {
                    Dialogs.error(PtBookingPanel.this, ex);
                } finally {
                    setBusy(false);
                }
            }
        };
        worker.execute();
    }

    private void searchMembers() {
        searchMembersButton.setEnabled(false);
        String keyword = memberSearchField.getText().trim();
        SwingWorker<List<Member>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Member> doInBackground() {
                return service.searchMembers(keyword);
            }

            @Override
            protected void done() {
                try {
                    List<Member> members = get();
                    memberCombo.removeAllItems();
                    members.forEach(memberCombo::addItem);
                    if (members.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                PtBookingPanel.this,
                                "Không tìm thấy hội viên phù hợp.",
                                "Không có kết quả",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        memberCombo.showPopup();
                    }
                } catch (Exception ex) {
                    Dialogs.error(PtBookingPanel.this, ex);
                } finally {
                    searchMembersButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void findAvailableTimes() {
        Member member = (Member) memberCombo.getSelectedItem();
        Trainer trainer = (Trainer) trainerCombo.getSelectedItem();
        Room room = (Room) roomCombo.getSelectedItem();
        if (member == null || trainer == null || room == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn hội viên, huấn luyện viên và phòng.",
                    "Thiếu dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            LocalTime start = LocalTime.parse((String) startCombo.getSelectedItem());
            LocalTime end = LocalTime.parse((String) endCombo.getSelectedItem());
            Duration duration = Duration.between(start, end);
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
            }
            LocalDate date = toLocalDate((Date) dateSpinner.getValue());
            findSlotsButton.setEnabled(false);
            SwingWorker<List<LocalDateTime>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<LocalDateTime> doInBackground() {
                    return service.findAvailableStarts(
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
                        if (starts.isEmpty()) {
                            JOptionPane.showMessageDialog(
                                    PtBookingPanel.this,
                                    "Không có giờ trống phù hợp trong ngày đã chọn.",
                                    "Không tìm thấy giờ trống",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    } catch (Exception ex) {
                        Dialogs.error(PtBookingPanel.this, ex);
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
        LocalTime currentStart = LocalTime.parse((String) startCombo.getSelectedItem());
        LocalTime currentEnd = LocalTime.parse((String) endCombo.getSelectedItem());
        Duration duration = Duration.between(currentStart, currentEnd);
        dateSpinner.setValue(toDate(start.toLocalDate()));
        startCombo.setSelectedItem(start.toLocalTime().toString());
        endCombo.setSelectedItem(start.plus(duration).toLocalTime().toString());
    }

    private void bookSession() {
        Member member = (Member) memberCombo.getSelectedItem();
        Trainer trainer = (Trainer) trainerCombo.getSelectedItem();
        Room room = (Room) roomCombo.getSelectedItem();
        if (member == null || trainer == null || room == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn hội viên, huấn luyện viên và phòng.",
                    "Thiếu dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            LocalDate date = toLocalDate((Date) dateSpinner.getValue());
            LocalDateTime start = LocalDateTime.of(
                    date, LocalTime.parse((String) startCombo.getSelectedItem()));
            LocalDateTime end = LocalDateTime.of(
                    date, LocalTime.parse((String) endCombo.getSelectedItem()));
            bookButton.setEnabled(false);
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    service.bookSession(
                            member.getVisitorId(), room.getRoomId(), trainer.getStaffId(),
                            start, end);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(
                                PtBookingPanel.this,
                                "Đặt lịch PT thành công cho " + member.getFullName() + ".",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                        JOptionPane.showMessageDialog(
                                PtBookingPanel.this,
                                cause.getMessage() == null
                                        ? "Không thể đặt lịch PT."
                                        : cause.getMessage(),
                                "Không thể đặt lịch",
                                JOptionPane.ERROR_MESSAGE
                        );
                    } finally {
                        bookButton.setEnabled(true);
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

    private void setBusy(boolean busy) {
        bookButton.setEnabled(!busy);
        searchMembersButton.setEnabled(!busy);
        findSlotsButton.setEnabled(!busy);
    }

    private static String memberText(Member member) {
        return member.getFullName() + " (#" + member.getVisitorId() + ")"
                + (member.hasPhoto() ? "" : " — chưa có ảnh");
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private record ResourceData(List<Room> rooms, List<Trainer> trainers) {
    }
}

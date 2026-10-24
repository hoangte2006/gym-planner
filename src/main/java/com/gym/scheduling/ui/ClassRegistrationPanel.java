package com.gym.scheduling.ui;

import com.gym.core.model.AttendanceStatus;
import com.gym.core.model.ClassRegistration;
import com.gym.core.model.Member;
import com.gym.core.ui.Dialogs;
import com.gym.scheduling.service.ClassRegistrationService;
import com.gym.scheduling.service.ClassRegistrationService.ClassAvailability;
import com.gym.scheduling.service.ClassRegistrationService.RegistrationEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClassRegistrationPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ClassRegistrationService service;
    private final JComboBox<ClassAvailability> classCombo = new JComboBox<>();
    private final JTextField memberSearchField = new JTextField(16);
    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton searchButton = new JButton("Tìm hội viên");
    private final JButton registerButton = new JButton("Đăng ký");
    private final JButton cancelButton = new JButton("Hủy đăng ký");
    private final JButton attendButton = new JButton("Đánh dấu đã tham gia");
    private final JButton confirmButton = new JButton("Đặt lại chờ điểm danh");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã hội viên", "Họ tên", "Điện thoại", "Ngày đăng ký", "Trạng thái"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable registrationTable = new JTable(tableModel);
    private List<RegistrationEntry> entries = List.of();
    private boolean loadingClasses;

    public ClassRegistrationPanel() {
        this(new ClassRegistrationService());
    }

    public ClassRegistrationPanel(ClassRegistrationService service) {
        super(new BorderLayout(10, 10));
        this.service = service;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel classPicker = new JPanel(new BorderLayout(8, 0));
        classPicker.add(new JLabel("Lớp sắp diễn ra:"), BorderLayout.WEST);
        classPicker.add(classCombo, BorderLayout.CENTER);
        classPicker.add(refreshButton, BorderLayout.EAST);
        add(classPicker, BorderLayout.NORTH);

        JPanel memberControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        memberControls.add(new JLabel("Tìm hội viên:"));
        memberControls.add(memberSearchField);
        memberControls.add(searchButton);
        memberControls.add(memberCombo);
        memberControls.add(registerButton);

        registrationTable.setFillsViewportHeight(true);
        registrationTable.setRowHeight(26);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(memberControls, BorderLayout.NORTH);
        center.add(new JScrollPane(registrationTable), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(cancelButton);
        actions.add(confirmButton);
        actions.add(attendButton);
        add(actions, BorderLayout.SOUTH);

        classCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String text = "";
            if (value != null) {
                var session = value.session();
                text = session.getClassName() + " — "
                        + DATE_TIME.format(session.getStartTime())
                        + " — còn " + value.seatsRemaining() + " chỗ";
            }
            JLabel label = new JLabel(text);
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
        memberCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : memberText(value));
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });

        refreshButton.addActionListener(e -> refreshClasses());
        searchButton.addActionListener(e -> searchMembers());
        registerButton.addActionListener(e -> register());
        cancelButton.addActionListener(e -> cancelRegistration());
        attendButton.addActionListener(e -> markAttendance(AttendanceStatus.ATTENDED));
        confirmButton.addActionListener(e -> markAttendance(AttendanceStatus.CONFIRMED));
        classCombo.addActionListener(e -> {
            if (!loadingClasses) {
                loadRegistrations();
            }
        });
        refreshClasses();
    }

    private void refreshClasses() {
        refreshButton.setEnabled(false);
        loadingClasses = true;
        SwingWorker<List<ClassAvailability>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ClassAvailability> doInBackground() {
                return service.findUpcomingClasses();
            }

            @Override
            protected void done() {
                try {
                    List<ClassAvailability> classes = get();
                    DefaultComboBoxModel<ClassAvailability> model =
                            new DefaultComboBoxModel<>();
                    classes.forEach(model::addElement);
                    classCombo.setModel(model);
                    if (classes.isEmpty()) {
                        tableModel.setRowCount(0);
                        entries = List.of();
                    }
                } catch (Exception ex) {
                    Dialogs.error(ClassRegistrationPanel.this, ex);
                } finally {
                    loadingClasses = false;
                    refreshButton.setEnabled(true);
                    if (classCombo.getSelectedItem() != null) {
                        loadRegistrations();
                    }
                }
            }
        };
        worker.execute();
    }

    private void loadRegistrations() {
        ClassAvailability selected = (ClassAvailability) classCombo.getSelectedItem();
        if (selected == null) {
            tableModel.setRowCount(0);
            entries = List.of();
            return;
        }

        setOperationButtons(false);
        SwingWorker<List<RegistrationEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<RegistrationEntry> doInBackground() {
                return service.findRegistrations(selected.session().getSessionId());
            }

            @Override
            protected void done() {
                try {
                    showRegistrations(get());
                } catch (Exception ex) {
                    Dialogs.error(ClassRegistrationPanel.this, ex);
                } finally {
                    setOperationButtons(true);
                }
            }
        };
        worker.execute();
    }

    private void showRegistrations(List<RegistrationEntry> result) {
        entries = result;
        tableModel.setRowCount(0);
        for (RegistrationEntry entry : result) {
            ClassRegistration registration = entry.registration();
            tableModel.addRow(new Object[]{
                    registration.getVisitorId(),
                    entry.memberName(),
                    entry.phone(),
                    registration.getRegistrationDate() == null ? ""
                            : DATE_TIME.format(registration.getRegistrationDate()),
                    registration.getAttendanceStatus() == null ? ""
                            : registration.getAttendanceStatus().getLabel()
            });
        }
    }

    private void searchMembers() {
        searchButton.setEnabled(false);
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
                                ClassRegistrationPanel.this,
                                "Không tìm thấy hội viên phù hợp.",
                                "Không có kết quả",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        memberCombo.showPopup();
                    }
                } catch (Exception ex) {
                    Dialogs.error(ClassRegistrationPanel.this, ex);
                } finally {
                    searchButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void register() {
        ClassAvailability selected = (ClassAvailability) classCombo.getSelectedItem();
        Member member = (Member) memberCombo.getSelectedItem();
        if (selected == null || member == null) {
            JOptionPane.showMessageDialog(
                    this, "Vui lòng chọn lớp và hội viên.",
                    "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        runOperation("Đăng ký", () -> service.register(
                selected.session().getSessionId(), member.getVisitorId()));
    }

    private void cancelRegistration() {
        RegistrationEntry entry = selectedEntry();
        ClassAvailability selected = (ClassAvailability) classCombo.getSelectedItem();
        if (entry == null || selected == null) {
            return;
        }
        runOperation("Hủy đăng ký", () -> service.cancelRegistration(
                selected.session().getSessionId(),
                entry.registration().getVisitorId()));
    }

    private void markAttendance(AttendanceStatus status) {
        RegistrationEntry entry = selectedEntry();
        ClassAvailability selected = (ClassAvailability) classCombo.getSelectedItem();
        if (entry == null || selected == null) {
            return;
        }
        runOperation("Cập nhật điểm danh", () -> service.markAttendance(
                selected.session().getSessionId(),
                entry.registration().getVisitorId(), status));
    }

    private RegistrationEntry selectedEntry() {
        int row = registrationTable.getSelectedRow();
        return row < 0 || row >= entries.size() ? null : entries.get(row);
    }

    private void runOperation(String title, Runnable operation) {
        setOperationButtons(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                operation.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refreshClasses();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(
                            ClassRegistrationPanel.this,
                            cause.getMessage() == null
                                    ? title + " không thành công."
                                    : cause.getMessage(),
                            title + " thất bại",
                            JOptionPane.ERROR_MESSAGE
                    );
                    setOperationButtons(true);
                }
            }
        };
        worker.execute();
    }

    private void setOperationButtons(boolean enabled) {
        registerButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
        attendButton.setEnabled(enabled);
        confirmButton.setEnabled(enabled);
        searchButton.setEnabled(enabled);
    }

    private static String memberText(Member member) {
        return member.getFullName() + " (#" + member.getVisitorId() + ")"
                + (member.hasPhoto() ? "" : " — chưa có ảnh");
    }
}

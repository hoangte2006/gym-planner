package com.gym.scheduling.ui;

import com.gym.core.dao.SessionEquipmentUsageDao;
import com.gym.core.model.Equipment;
import com.gym.core.model.BookingStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.SessionEquipmentUsage;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Session;
import com.gym.core.ui.Async;
import com.gym.core.ui.Dialogs;
import com.gym.scheduling.service.SessionEquipmentService;
import com.gym.scheduling.service.SessionService;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Equipment availability and reservation management for current-week sessions. */
public class SessionEquipmentPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final SessionService sessionService;
    private final SessionEquipmentService equipmentService;
    private final SessionEquipmentUsageDao usageDao;
    private final JComboBox<Session> sessionCombo = new JComboBox<>();
    private final JComboBox<Equipment> equipmentCombo = new JComboBox<>();
    private final JButton refreshButton = new JButton("Tải lịch tuần này");
    private final JButton reserveButton = new JButton("Đặt thiết bị cho cả buổi");
    private final JButton removeButton = new JButton("Gỡ lượt đặt đã chọn");
    private final JButton helpButton = new JButton("?");
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã đặt", "Mã thiết bị", "Từ", "Đến"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable reservations = new JTable(tableModel);
    private boolean loadingSessions;

    public SessionEquipmentPanel() {
        this(new SessionService(), new SessionEquipmentService(),
                new SessionEquipmentUsageDao());
    }

    public SessionEquipmentPanel(
            SessionService sessionService,
            SessionEquipmentService equipmentService,
            SessionEquipmentUsageDao usageDao) {
        super(new BorderLayout(8, 8));
        this.sessionService = sessionService;
        this.equipmentService = equipmentService;
        this.usageDao = usageDao;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.add(new JLabel("Buổi tập:"));
        controls.add(sessionCombo);
        controls.add(refreshButton);
        controls.add(new JLabel("Thiết bị khả dụng:"));
        controls.add(equipmentCombo);
        controls.add(reserveButton);
        controls.add(helpButton);
        add(controls, BorderLayout.NORTH);

        reservations.setFillsViewportHeight(true);
        reservations.setRowHeight(26);
        add(new JScrollPane(reservations), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(statusLabel);
        actions.add(removeButton);
        add(actions, BorderLayout.SOUTH);

        sessionCombo.setRenderer((list, value, index, selected, focus) -> {
            String label = value == null ? "" : value.getSessionTitle() + " — "
                    + DATE_TIME.format(value.getStartTime());
            JLabel result = new JLabel(label);
            result.setOpaque(true);
            if (selected) {
                result.setBackground(list.getSelectionBackground());
                result.setForeground(list.getSelectionForeground());
            }
            return result;
        });
        equipmentCombo.setRenderer((list, value, index, selected, focus) -> {
            JLabel result = new JLabel(value == null ? ""
                    : value.getEquipmentName() + " (#" + value.getEquipmentId() + ")");
            result.setOpaque(true);
            if (selected) {
                result.setBackground(list.getSelectionBackground());
                result.setForeground(list.getSelectionForeground());
            }
            return result;
        });

        sessionCombo.addActionListener(event -> {
            if (!loadingSessions) {
                refreshSelectedSession();
            }
        });
        refreshButton.addActionListener(event -> loadSessions());
        reserveButton.addActionListener(event -> reserve());
        removeButton.addActionListener(event -> removeSelectedReservation());
        helpButton.setToolTipText("Chọn buổi tập để xem thiết bị còn khả dụng trong khung giờ đó.");
        helpButton.addActionListener(event -> JOptionPane.showMessageDialog(this,
                "Chọn một buổi tập trong tuần này.\n"
                        + "Danh sách chỉ hiện thiết bị đang hoạt động, đúng phòng và chưa bị đặt trùng giờ.\n"
                        + "Bạn có thể gỡ lượt đặt chưa cần dùng.",
                "Hướng dẫn đặt thiết bị", JOptionPane.INFORMATION_MESSAGE));
        boolean manager = com.gym.app.Session.hasRole(StaffRole.MANAGER)
                || com.gym.app.Session.hasRole(StaffRole.RECEPTIONIST);
        reserveButton.setEnabled(manager);
        removeButton.setEnabled(manager);
        loadSessions();
    }

    private void loadSessions() {
        refreshButton.setEnabled(false);
        loadingSessions = true;
        Async.run(this, () -> sessionService.findWeeklySchedule(LocalDate.now()), sessions -> {
            DefaultComboBoxModel<Session> model = new DefaultComboBoxModel<>();
            LocalDateTime now = LocalDateTime.now();
            sessions.stream()
                    .filter(session -> session.getStartTime() != null
                            && session.getStartTime().isAfter(now))
                    .filter(session -> !(session instanceof PtSession pt)
                            || pt.getBookingStatus() == BookingStatus.SCHEDULED)
                    .forEach(model::addElement);
            sessionCombo.setModel(model);
            statusLabel.setText(model.getSize() == 0
                    ? "Tuần này không có buổi tập sắp tới." : " ");
            loadingSessions = false;
            refreshButton.setEnabled(true);
            refreshSelectedSession();
        }, error -> {
            loadingSessions = false;
            refreshButton.setEnabled(true);
            Dialogs.error(this, error);
        });
    }

    private void refreshSelectedSession() {
        Session selected = (Session) sessionCombo.getSelectedItem();
        if (selected == null) {
            equipmentCombo.removeAllItems();
            tableModel.setRowCount(0);
            return;
        }
        Async.run(this, () -> new ReservationData(
                equipmentService.findAvailableEquipment(selected.getSessionId()),
                usageDao.findBySession(selected.getSessionId())),
                this::showReservations);
    }

    private void showReservations(ReservationData data) {
        equipmentCombo.removeAllItems();
        data.available().forEach(equipmentCombo::addItem);
        statusLabel.setText(data.available().isEmpty()
                ? "Không còn thiết bị phù hợp cho buổi đã chọn."
                : data.available().size() + " thiết bị khả dụng.");
        tableModel.setRowCount(0);
        for (SessionEquipmentUsage usage : data.reservations()) {
            tableModel.addRow(new Object[]{
                    usage.getUsageId(), usage.getEquipmentId(),
                    usage.getReservedFrom().format(DATE_TIME),
                    usage.getReservedTo().format(DATE_TIME)
            });
        }
    }

    private void reserve() {
        Session selected = (Session) sessionCombo.getSelectedItem();
        Equipment equipment = (Equipment) equipmentCombo.getSelectedItem();
        if (selected == null || equipment == null) {
            Dialogs.error(this, new IllegalArgumentException(
                    "Hãy chọn buổi tập và một thiết bị khả dụng."));
            return;
        }
        Async.run(this, () -> equipmentService.reserveEquipment(
                        selected.getSessionId(), equipment.getEquipmentId(),
                        selected.getStartTime(), selected.getEndTime()),
                ignored -> refreshSelectedSession());
    }

    private void removeSelectedReservation() {
        int selectedRow = reservations.getSelectedRow();
        if (selectedRow < 0) {
            Dialogs.error(this, new IllegalArgumentException("Hãy chọn lượt đặt cần gỡ."));
            return;
        }
        int modelRow = reservations.convertRowIndexToModel(selectedRow);
        int usageId = (Integer) tableModel.getValueAt(modelRow, 0);
        if (JOptionPane.showOptionDialog(this, "Gỡ lượt đặt thiết bị đã chọn?",
                "Xác nhận gỡ đặt", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null,
                new Object[]{"Gỡ lượt đặt", "Giữ lại"}, "Giữ lại") != 0) {
            return;
        }
        Async.run(this, () -> {
            equipmentService.cancelReservation(usageId);
            return null;
        }, ignored -> refreshSelectedSession());
    }

    private record ReservationData(
            List<Equipment> available,
            List<SessionEquipmentUsage> reservations) {
    }
}

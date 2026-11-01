package com.gym.facility.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.gym.app.Session;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.Manager;
import com.gym.core.model.MaintenanceLog;
import com.gym.core.model.Receptionist;
import com.gym.core.model.Room;
import com.gym.core.model.ShiftType;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.DatePickers;
import com.gym.core.ui.Dialogs;
import com.gym.core.ui.FormPanel;
import com.gym.facility.service.EquipmentService;
import com.gym.facility.service.MaintenanceLogService;
import com.gym.facility.service.RoomService;
import com.gym.facility.service.StaffService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Màn hình TV4: UI chỉ gọi Service; toàn bộ SQL nằm dưới tầng DAO. */
public class FacilityDashboardPanel extends JPanel {

    private static final int PAGE_SIZE = 10;

    private final StaffService staffService;
    private final RoomService roomService;
    private final EquipmentService equipmentService;
    private final MaintenanceLogService maintenanceLogService;

    private DataTable<Staff> staffTable;
    private DataTable<Room> roomTable;
    private DataTable<Equipment> equipmentTable;
    private DataTable<MaintenanceLog> maintenanceTable;
    private JComboBox<EquipmentChoice> maintenanceEquipmentCombo;
    private List<Staff> maintenanceSigners = new ArrayList<>();
    private boolean fillingMaintenanceOptions;

    /** Constructor tiêm Service từ FacilityModule; UI không khởi tạo hoặc gọi DAO trực tiếp. */
    public FacilityDashboardPanel(StaffService staffService,
                                  RoomService roomService,
                                  EquipmentService equipmentService,
                                  MaintenanceLogService maintenanceLogService) {
        this.staffService = Objects.requireNonNull(staffService);
        this.roomService = Objects.requireNonNull(roomService);
        this.equipmentService = Objects.requireNonNull(equipmentService);
        this.maintenanceLogService = Objects.requireNonNull(maintenanceLogService);
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));

        JLabel title = new JLabel("Nhân sự & Cơ sở vật chất");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("Quản lý nhân sự, phòng tập, thiết bị và lịch sử bảo trì.");
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        staffTable = createStaffTable();
        roomTable = createRoomTable();
        equipmentTable = createEquipmentTable();
        maintenanceTable = createMaintenanceTable();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Nhân sự", createStaffTab());
        tabs.addTab("Phòng tập", createRoomTab());
        tabs.addTab("Thiết bị", createEquipmentTab());
        tabs.addTab("Phiếu bảo trì", createMaintenanceTab());
        add(tabs, BorderLayout.CENTER);

        staffTable.reload();
        final boolean[] loaded = {false, false, false};
        tabs.addChangeListener(event -> {
            switch (tabs.getSelectedIndex()) {
                case 1 -> {
                    if (!loaded[0]) { loaded[0] = true; roomTable.reload(); }
                }
                case 2 -> {
                    if (!loaded[1]) { loaded[1] = true; equipmentTable.reload(); }
                }
                case 3 -> {
                    if (!loaded[2]) { loaded[2] = true; loadMaintenanceOptions(); }
                }
                default -> { }
            }
        });
    }

    // ---------------------------------------------------------------------
    // Nhân sự
    // ---------------------------------------------------------------------

    private DataTable<Staff> createStaffTable() {
        DataTable<Staff> table = new DataTable<>(PAGE_SIZE);
        table.addColumn("Mã NV", Staff::getStaffId, 70)
                .addColumn("Họ và tên", Staff::getFullName, 180)
                .addColumn("Số điện thoại", Staff::getPhone, 135)
                .addColumn("Vai trò", Staff::getRoleName, 135)
                .addColumn("Ngày vào làm", Staff::getHireDate, 115)
                .addColumn("Lương", Staff::getSalary, 110)
                .setLoader(staffService::findPage);
        return table;
    }

    private JPanel createStaffTab() {
        JButton add = button("+ Thêm nhân sự");
        JButton edit = button("Sửa");
        JButton delete = dangerButton("Xóa");
        JButton refresh = button("Làm mới");
        edit.setEnabled(false);
        delete.setEnabled(false);
        staffTable.onSelectionChanged(staff -> {
            edit.setEnabled(staff != null);
            delete.setEnabled(staff != null);
        });
        add.addActionListener(e -> openStaffEditor(null));
        edit.addActionListener(e -> {
            Staff selected = staffTable.getSelected();
            if (selected != null) openStaffEditor(selected);
        });
        delete.addActionListener(e -> {
            Staff selected = staffTable.getSelected();
            if (selected != null && Dialogs.confirmDelete(this, "nhân sự " + selected.getFullName())) {
                runUiAction(() -> staffService.deleteStaff(selected.getStaffId()), staffTable::reload);
            }
        });
        refresh.addActionListener(e -> staffTable.reload());
        return listPanel("Danh sách nhân sự", staffTable, add, edit, delete, refresh);
    }

    private void openStaffEditor(Staff existing) {
        boolean editing = existing != null;
        JDialog dialog = createDialog(editing ? "Sửa nhân sự" : "Thêm nhân sự", 540, 600);

        FormPanel common = new FormPanel();
        JTextField fullName = common.addText("fullName", "Họ và tên", true);
        JTextField phone = common.addText("phone", "Số điện thoại", true);
        JTextField salary = common.addText("salary", "Lương (VND)", true);
        DatePicker hireDate = common.addDate("hireDate", "Ngày vào làm", false);
        JComboBox<StaffRole> role = common.addCombo("role", "Vai trò", true, StaffRole.values());
        role.setRenderer(enumRenderer());
        if (editing) {
            role.setSelectedItem(existing.getRole());
            role.setEnabled(false); // Không đổi lớp con khi cập nhật: tránh lệch STAFF / bảng con.
            fullName.setText(existing.getFullName());
            phone.setText(existing.getPhone());
            salary.setText(existing.getSalary() == null ? "" : existing.getSalary().toPlainString());
            hireDate.setDate(existing.getHireDate());
        } else {
            role.setSelectedItem(StaffRole.TRAINER);
            salary.setText("500.00");
        }

        FormPanel trainerForm = new FormPanel();
        JTextField specialty = trainerForm.addText("specialty", "Chuyên môn", true);
        JTextField certification = trainerForm.addText("certification", "Chứng chỉ", false);
        FormPanel receptionistForm = new FormPanel();
        JComboBox<ShiftType> shift = receptionistForm.addCombo("shift", "Ca làm", true, ShiftType.values());
        shift.setRenderer(enumRenderer());
        JPanel managerForm = new JPanel(new BorderLayout());
        managerForm.add(new JLabel("Vai trò Quản lý không có trường riêng."), BorderLayout.NORTH);

        JPanel roleCards = new JPanel(new CardLayout());
        roleCards.add(trainerForm, StaffRole.TRAINER.name());
        roleCards.add(receptionistForm, StaffRole.RECEPTIONIST.name());
        roleCards.add(managerForm, StaffRole.MANAGER.name());
        role.addActionListener(e -> {
            StaffRole selected = (StaffRole) role.getSelectedItem();
            if (selected != null) ((CardLayout) roleCards.getLayout()).show(roleCards, selected.name());
        });

        if (existing instanceof Trainer trainer) {
            specialty.setText(trainer.getSpecialty());
            certification.setText(trainer.getCertification());
        } else if (existing instanceof Receptionist receptionist) {
            shift.setSelectedItem(receptionist.getShiftType());
        }
        StaffRole selectedInitialRole = editing ? existing.getRole() : StaffRole.TRAINER;
        ((CardLayout) roleCards.getLayout()).show(roleCards, selectedInitialRole.name());

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 8, 14));
        content.add(common, BorderLayout.NORTH);
        content.add(roleCards, BorderLayout.CENTER);
        addDialogButtons(dialog, content, () -> {
            try {
                StaffRole selectedRole = (StaffRole) role.getSelectedItem();
                Staff staff;
                if (selectedRole == StaffRole.TRAINER) {
                    Trainer t = new Trainer();
                    if (specialty.getText() != null && !specialty.getText().isBlank()) t.setSpecialty(specialty.getText());
                    t.setCertification(blankToNull(certification.getText()));
                    staff = t;
                } else if (selectedRole == StaffRole.RECEPTIONIST) {
                    Receptionist r = new Receptionist();
                    r.setShiftType((ShiftType) shift.getSelectedItem());
                    staff = r;
                } else {
                    staff = new Manager();
                }
                if (editing) staff.setStaffId(existing.getStaffId());
                staff.setFullName(fullName.getText());
                staff.setPhone(phone.getText());
                staff.setSalary(parseMoney(salary.getText(), "Lương"));
                staff.setHireDate(hireDate.getDate());
                Async.run(dialog, () -> {
                    if (editing) staffService.updateStaff(staff); else staffService.createStaff(staff);
                    return Boolean.TRUE;
                }, ignored -> {
                    Dialogs.info(dialog, editing ? "Đã cập nhật nhân sự." : "Đã thêm nhân sự.");
                    dialog.dispose();
                    staffTable.reload();
                }, ex -> Dialogs.error(dialog, ex));
            } catch (Exception ex) {
                Dialogs.error(dialog, ex);
            }
        });
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    // ---------------------------------------------------------------------
    // Phòng và danh sách thiết bị trong phòng
    // ---------------------------------------------------------------------

    private DataTable<Room> createRoomTable() {
        DataTable<Room> table = new DataTable<>(PAGE_SIZE);
        table.addColumn("Mã phòng", Room::getRoomId, 80)
                .addColumn("Tên phòng", Room::getRoomName, 210)
                .addColumn("Sức chứa", Room::getCapacity, 95)
                .addColumn("Tầng / vị trí", Room::getFloorLocation, 180)
                .setLoader(roomService::findPage);
        return table;
    }

    private JPanel createRoomTab() {
        JButton add = button("+ Thêm phòng");
        JButton edit = button("Sửa");
        JButton detail = button("Thiết bị trong phòng");
        JButton delete = dangerButton("Xóa");
        JButton refresh = button("Làm mới");
        edit.setEnabled(false); detail.setEnabled(false); delete.setEnabled(false);
        roomTable.onSelectionChanged(room -> {
            boolean enabled = room != null;
            edit.setEnabled(enabled); detail.setEnabled(enabled); delete.setEnabled(enabled);
        });
        add.addActionListener(e -> openRoomEditor(null));
        edit.addActionListener(e -> { Room r = roomTable.getSelected(); if (r != null) openRoomEditor(r); });
        detail.addActionListener(e -> { Room r = roomTable.getSelected(); if (r != null) openRoomEquipment(r); });
        delete.addActionListener(e -> {
            Room r = roomTable.getSelected();
            if (r != null && Dialogs.confirmDelete(this, "phòng " + r.getRoomName())) {
                runUiAction(() -> roomService.deleteRoom(r.getRoomId()), roomTable::reload);
            }
        });
        refresh.addActionListener(e -> roomTable.reload());
        return listPanel("Danh sách phòng tập", roomTable, add, edit, detail, delete, refresh);
    }

    private void openRoomEditor(Room existing) {
        boolean editing = existing != null;
        JDialog dialog = createDialog(editing ? "Sửa phòng tập" : "Thêm phòng tập", 470, 360);
        FormPanel form = new FormPanel();
        JTextField name = form.addText("name", "Tên phòng", true);
        JTextField capacity = form.addText("capacity", "Sức chứa", true);
        JTextField floor = form.addText("floor", "Tầng / vị trí", false);
        if (editing) {
            name.setText(existing.getRoomName());
            capacity.setText(String.valueOf(existing.getCapacity()));
            floor.setText(existing.getFloorLocation());
        } else {
            capacity.setText("20");
            floor.setText("Tầng 1");
        }
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 8, 14));
        content.add(form, BorderLayout.CENTER);
        addDialogButtons(dialog, content, () -> {
            try {
                Room room = new Room();
                if (editing) room.setRoomId(existing.getRoomId());
                room.setRoomName(name.getText());
                room.setCapacity(Integer.parseInt(capacity.getText().trim()));
                room.setFloorLocation(blankToNull(floor.getText()));
                Async.run(dialog, () -> {
                    if (editing) roomService.updateRoom(room); else roomService.createRoom(room);
                    return Boolean.TRUE;
                }, ignored -> {
                    Dialogs.info(dialog, editing ? "Đã cập nhật phòng." : "Đã tạo phòng.");
                    dialog.dispose();
                    roomTable.reload();
                }, ex -> Dialogs.error(dialog, ex));
            } catch (Exception ex) {
                Dialogs.error(dialog, ex);
            }
        });
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void openRoomEquipment(Room room) {
        JDialog dialog = createDialog("Thiết bị trong phòng: " + room.getRoomName(), 850, 470);
        DataTable<Equipment> table = new DataTable<>(PAGE_SIZE);
        table.addColumn("Mã thiết bị", Equipment::getEquipmentId, 90)
                .addColumn("Tên thiết bị", Equipment::getEquipmentName, 240)
                .addColumn("Trạng thái", Equipment::getOperationalStatus, 150)
                .addColumn("Ngày mua", Equipment::getPurchaseDate, 120)
                .setItems(List.of());
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(new JLabel("Danh sách thiết bị đang được gán cho phòng " + room.getRoomId()), BorderLayout.NORTH);
        content.add(table, BorderLayout.CENTER);
        JButton close = button("Đóng");
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(close); close.addActionListener(e -> dialog.dispose());
        content.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        Async.run(dialog, () -> equipmentService.findByRoom(room.getRoomId()), table::setItems,
                ex -> Dialogs.error(dialog, ex));
        dialog.setVisible(true);
    }

    // ---------------------------------------------------------------------
    // Thiết bị
    // ---------------------------------------------------------------------

    private DataTable<Equipment> createEquipmentTable() {
        DataTable<Equipment> table = new DataTable<>(PAGE_SIZE);
        table.addColumn("Mã thiết bị", Equipment::getEquipmentId, 90)
                .addColumn("Tên thiết bị", Equipment::getEquipmentName, 220)
                .addColumn("Mã phòng", Equipment::getRoomId, 90)
                .addColumn("Tình trạng", Equipment::getOperationalStatus, 160)
                .addColumn("Ngày mua", Equipment::getPurchaseDate, 120)
                .setLoader(equipmentService::findPage);
        return table;
    }

    private JPanel createEquipmentTab() {
        JButton add = button("+ Thêm thiết bị");
        JButton edit = button("Sửa");
        JButton status = button("Đổi trạng thái");
        JButton delete = dangerButton("Xóa");
        JButton refresh = button("Làm mới");
        edit.setEnabled(false); status.setEnabled(false); delete.setEnabled(false);
        equipmentTable.onSelectionChanged(equipment -> {
            boolean enabled = equipment != null;
            edit.setEnabled(enabled); status.setEnabled(enabled); delete.setEnabled(enabled);
        });
        add.addActionListener(e -> loadRoomsThenOpenEquipmentEditor(null));
        edit.addActionListener(e -> {
            Equipment equipment = equipmentTable.getSelected();
            if (equipment != null) loadRoomsThenOpenEquipmentEditor(equipment);
        });
        status.addActionListener(e -> {
            Equipment equipment = equipmentTable.getSelected();
            if (equipment != null) openEquipmentStatusEditor(equipment);
        });
        delete.addActionListener(e -> {
            Equipment equipment = equipmentTable.getSelected();
            if (equipment != null && Dialogs.confirm(this,
                    "Xóa thiết bị '" + equipment.getEquipmentName() + "'?\n"
                            + "Thao tác này sẽ xóa cả lịch sử bảo trì và lượt sử dụng thiết bị liên quan.")) {
                runUiAction(() -> equipmentService.delete(equipment.getEquipmentId()), equipmentTable::reload);
            }
        });
        refresh.addActionListener(e -> equipmentTable.reload());
        return listPanel("Danh sách thiết bị", equipmentTable, add, edit, status, delete, refresh);
    }

    private void loadRoomsThenOpenEquipmentEditor(Equipment initial) {
        Async.run(this, roomService::findAll, rooms -> openEquipmentEditor(initial, rooms));
    }

    private void openEquipmentEditor(Equipment existing, List<Room> rooms) {
        boolean editing = existing != null;
        JDialog dialog = createDialog(editing ? "Sửa thiết bị" : "Thêm thiết bị", 540, 470);
        FormPanel form = new FormPanel();
        JTextField name = form.addText("name", "Tên thiết bị", true);
        JComboBox<RoomChoice> room = new JComboBox<>();
        room.addItem(new RoomChoice(null));
        for (Room r : rooms) room.addItem(new RoomChoice(r));
        room.setRenderer(roomRenderer());
        form.addComponent("room", "Phòng", false, room);
        JComboBox<EquipmentStatus> status = form.addCombo("status", "Tình trạng", true, EquipmentStatus.values());
        status.setRenderer(enumRenderer());
        DatePicker purchase = form.addDate("purchase", "Ngày mua", false);
        if (editing) {
            name.setText(existing.getEquipmentName());
            setSelectedRoom(room, existing.getRoomId());
            status.setSelectedItem(existing.getOperationalStatus());
            purchase.setDate(existing.getPurchaseDate());
        } else {
            status.setSelectedItem(EquipmentStatus.ACTIVE);
        }
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 8, 14));
        content.add(form, BorderLayout.CENTER);
        addDialogButtons(dialog, content, () -> {
            try {
                Equipment equipment = new Equipment();
                if (editing) equipment.setEquipmentId(existing.getEquipmentId());
                equipment.setEquipmentName(name.getText());
                RoomChoice selectedRoom = (RoomChoice) room.getSelectedItem();
                equipment.setRoomId(selectedRoom == null || selectedRoom.room() == null
                        ? null : selectedRoom.room().getRoomId());
                equipment.setOperationalStatus((EquipmentStatus) status.getSelectedItem());
                equipment.setPurchaseDate(purchase.getDate());
                Async.run(dialog, () -> {
                    if (editing) equipmentService.update(equipment); else equipmentService.create(equipment);
                    return Boolean.TRUE;
                }, ignored -> {
                    Dialogs.info(dialog, editing ? "Đã cập nhật thiết bị." : "Đã thêm thiết bị.");
                    dialog.dispose();
                    equipmentTable.reload();
                    loadMaintenanceOptions();
                }, ex -> Dialogs.error(dialog, ex));
            } catch (Exception ex) {
                Dialogs.error(dialog, ex);
            }
        });
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void openEquipmentStatusEditor(Equipment equipment) {
        JDialog dialog = createDialog("Đổi trạng thái thiết bị", 440, 190);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        content.add(new JLabel("Thiết bị: " + equipment.getEquipmentName() + " (mã " + equipment.getEquipmentId() + ")"), BorderLayout.NORTH);
        JComboBox<EquipmentStatus> status = new JComboBox<>(EquipmentStatus.values());
        status.setRenderer(enumRenderer());
        status.setSelectedItem(equipment.getOperationalStatus());
        content.add(status, BorderLayout.CENTER);
        addDialogButtons(dialog, content, () -> {
            try {
                EquipmentStatus selected = (EquipmentStatus) status.getSelectedItem();
                if (selected == null) throw new BusinessException("Chưa chọn tình trạng thiết bị.");
                Async.run(dialog, () -> equipmentService.changeStatus(equipment.getEquipmentId(), selected), saved -> {
                    Dialogs.info(dialog, "Đã đổi tình trạng thiết bị thành " + saved.getOperationalStatus().getLabel() + ".");
                    dialog.dispose();
                    equipmentTable.reload();
                    loadMaintenanceOptions();
                }, ex -> Dialogs.error(dialog, ex));
            } catch (Exception ex) {
                Dialogs.error(dialog, ex);
            }
        });
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    // ---------------------------------------------------------------------
    // Phiếu bảo trì và lịch sử theo thiết bị
    // ---------------------------------------------------------------------

    private DataTable<MaintenanceLog> createMaintenanceTable() {
        DataTable<MaintenanceLog> table = new DataTable<>(PAGE_SIZE);
        table.addColumn("Log_no", MaintenanceLog::getLogNo, 80)
                .addColumn("Ngày bảo trì", MaintenanceLog::getMaintenanceDate, 120)
                .addColumn("Chi phí", MaintenanceLog::getCost, 120)
                .addColumn("Mô tả", MaintenanceLog::getDescription, 300)
                .addColumn("Người ghi (mã NV)", MaintenanceLog::getLoggedBy, 130);
        return table;
    }

    private JPanel createMaintenanceTab() {
        maintenanceEquipmentCombo = new JComboBox<>();
        maintenanceEquipmentCombo.setRenderer(equipmentChoiceRenderer());
        maintenanceEquipmentCombo.addActionListener(e -> {
            if (!fillingMaintenanceOptions) loadMaintenanceHistory();
        });
        JButton add = button("+ Thêm phiếu bảo trì");
        JButton history = button("Xem lịch sử");
        JButton refresh = button("Làm mới thiết bị");
        add.addActionListener(e -> openMaintenanceEditor());
        history.addActionListener(e -> loadMaintenanceHistory());
        refresh.addActionListener(e -> loadMaintenanceOptions());

        JPanel north = new JPanel(new BorderLayout(8, 8));
        north.add(new JLabel("Lịch sử của thiết bị:"), BorderLayout.WEST);
        north.add(maintenanceEquipmentCombo, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.add(history); controls.add(refresh); controls.add(add);
        north.add(controls, BorderLayout.SOUTH);
        north.setBorder(BorderFactory.createEmptyBorder(4, 2, 10, 2));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 2, 0, 2));
        panel.add(north, BorderLayout.NORTH);
        panel.add(maintenanceTable, BorderLayout.CENTER);
        return panel;
    }

    private void loadMaintenanceOptions() {
        Async.run(this, () -> {
            List<Equipment> equipments = equipmentService.findAll();
            List<Staff> signers = new ArrayList<>();
            signers.addAll(staffService.findStaffByRole(StaffRole.RECEPTIONIST));
            signers.addAll(staffService.findStaffByRole(StaffRole.MANAGER));
            return new MaintenanceOptions(equipments, signers);
        }, options -> {
            fillingMaintenanceOptions = true;
            try {
                maintenanceEquipmentCombo.removeAllItems();
                options.equipments().forEach(e -> maintenanceEquipmentCombo.addItem(new EquipmentChoice(e)));
                maintenanceSigners = options.signers();
                if (maintenanceEquipmentCombo.getItemCount() > 0) maintenanceEquipmentCombo.setSelectedIndex(0);
            } finally {
                fillingMaintenanceOptions = false;
            }
            loadMaintenanceHistory();
        });
    }

    private record MaintenanceOptions(List<Equipment> equipments, List<Staff> signers) { }

    private void loadMaintenanceHistory() {
        EquipmentChoice choice = (EquipmentChoice) maintenanceEquipmentCombo.getSelectedItem();
        if (choice == null || choice.equipment() == null) {
            maintenanceTable.setItems(List.of());
            return;
        }
        int equipmentId = choice.equipment().getEquipmentId();
        Async.run(this, () -> maintenanceLogService.findByEquipment(equipmentId), maintenanceTable::setItems);
    }

    private void openMaintenanceEditor() {
        EquipmentChoice choice = (EquipmentChoice) maintenanceEquipmentCombo.getSelectedItem();
        if (choice == null || choice.equipment() == null) {
            Dialogs.warn(this, "Hãy chọn thiết bị trước khi thêm phiếu bảo trì.");
            return;
        }
        if (maintenanceSigners.isEmpty()) {
            Dialogs.warn(this, "Chưa có nhân sự vai trò Lễ tân hoặc Quản lý để ghi phiếu (BR-FAC-05).");
            return;
        }
        Equipment equipment = choice.equipment();
        JDialog dialog = createDialog("Thêm phiếu bảo trì - " + equipment.getEquipmentName(), 560, 450);
        FormPanel form = new FormPanel();
        DatePicker date = form.addDate("date", "Ngày bảo trì", true);
        date.setDate(LocalDate.now());
        JTextField cost = form.addText("cost", "Chi phí (VND)", true);
        cost.setText("0.00");
        JTextArea description = form.addTextArea("description", "Mô tả", true, 4);
        JComboBox<Staff> signer = form.addCombo("signer", "Người ghi phiếu", true,
                maintenanceSigners.toArray(Staff[]::new));
        signer.setRenderer(staffRenderer());
        Staff sessionStaff = Session.getStaff();
        if (sessionStaff != null) {
            for (Staff s : maintenanceSigners) {
                if (Objects.equals(s.getStaffId(), sessionStaff.getStaffId())) {
                    signer.setSelectedItem(s);
                    break;
                }
            }
        }

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 8, 14));
        content.add(form, BorderLayout.CENTER);
        addDialogButtons(dialog, content, () -> {
            try {
                Staff selectedSigner = (Staff) signer.getSelectedItem();
                if (selectedSigner == null) throw new BusinessException("Chưa chọn người ghi phiếu.");
                MaintenanceLog log = new MaintenanceLog();
                log.setEquipmentId(equipment.getEquipmentId());
                log.setMaintenanceDate(date.getDate());
                log.setCost(parseMoney(cost.getText(), "Chi phí"));
                log.setDescription(description.getText());
                log.setLoggedBy(selectedSigner.getStaffId());
                Async.run(dialog, () -> maintenanceLogService.create(log), saved -> {
                    Dialogs.info(dialog, "Đã tạo phiếu bảo trì số " + saved.getLogNo()
                            + " cho thiết bị " + equipment.getEquipmentId() + ".");
                    dialog.dispose();
                    loadMaintenanceHistory();
                }, ex -> Dialogs.error(dialog, ex));
            } catch (Exception ex) {
                Dialogs.error(dialog, ex);
            }
        });
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    // ---------------------------------------------------------------------
    // Helpers UI
    // ---------------------------------------------------------------------

    private JPanel listPanel(String title, DataTable<?> table, JButton... actions) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 2, 0, 2));
        JLabel sectionTitle = new JLabel(title);
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 16f));
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.add(sectionTitle, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        for (JButton action : actions) right.add(action);
        toolbar.add(right, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(table, BorderLayout.CENTER);
        return panel;
    }

    private JDialog createDialog(String title, int width, int height) {
        Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setMinimumSize(new Dimension(width, height));
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(this);
        return dialog;
    }

    /** Đưa nút Lưu/Hủy vào cuối form và để callback kiểm soát thời điểm đóng hộp thoại. */
    private void addDialogButtons(JDialog dialog, JPanel content, Runnable saveAction) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton cancel = button("Hủy");
        JButton save = button("Lưu");
        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> saveAction.run());
        actions.add(cancel); actions.add(save);
        content.add(actions, BorderLayout.SOUTH);
    }

    private void runUiAction(Runnable action, Runnable onSuccess) {
        Async.run(this, () -> { action.run(); return Boolean.TRUE; }, ignored -> {
            Dialogs.info(this, "Thao tác đã hoàn tất.");
            onSuccess.run();
        });
    }

    private static JButton button(String title) {
        JButton button = new JButton(title);
        button.setFocusPainted(false);
        return button;
    }

    private static JButton dangerButton(String title) {
        JButton button = button(title);
        button.setForeground(new java.awt.Color(0xB91C1C));
        return button;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal parseMoney(String value, String label) {
        if (value == null || value.isBlank()) throw new BusinessException(label + " không được để trống.");
        try {
            // Cho phép nhập tiền kiểu 1,500,000 hoặc 1500000.
            return new BigDecimal(value.trim().replace(",", "").replace(" ", ""));
        } catch (NumberFormatException ex) {
            throw new BusinessException(label + " phải là số hợp lệ.", ex);
        }
    }

    private static ListCellRenderer<Object> enumRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                Object label = value instanceof com.gym.core.model.DbEnum e ? e.getLabel() : value;
                return super.getListCellRendererComponent(list, label, index, selected, focus);
            }
        };
    }

    private static ListCellRenderer<RoomChoice> roomRenderer() {
        return (list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null || value.room() == null
                    ? "Không gán phòng" : value.room().getRoomId() + " - " + value.room().getRoomName());
            if (selected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            return label;
        };
    }

    private static ListCellRenderer<EquipmentChoice> equipmentChoiceRenderer() {
        return (list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null || value.equipment() == null ? "Chọn thiết bị"
                    : value.equipment().getEquipmentId() + " - " + value.equipment().getEquipmentName());
            if (selected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            return label;
        };
    }

    private static ListCellRenderer<Staff> staffRenderer() {
        return (list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "Chọn nhân sự"
                    : value.getFullName() + " — " + value.getRoleName() + " (#" + value.getStaffId() + ")");
            if (selected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            return label;
        };
    }

    private static void setSelectedRoom(JComboBox<RoomChoice> combo, Integer roomId) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            RoomChoice choice = combo.getItemAt(i);
            Integer candidate = choice.room() == null ? null : choice.room().getRoomId();
            if (Objects.equals(candidate, roomId)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(0);
    }

    private record RoomChoice(Room room) { }
    private record EquipmentChoice(Equipment equipment) { }
}

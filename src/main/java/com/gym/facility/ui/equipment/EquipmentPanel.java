package com.gym.facility.ui.equipment;

import com.github.lgooddatepicker.components.DatePicker;

import com.gym.core.exception.BusinessException;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.Room;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.Dialogs;
import com.gym.core.ui.FormPanel;

import com.gym.facility.service.EquipmentService;
import com.gym.facility.service.EquipmentStatusChangeResult;
import com.gym.facility.service.RoomService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.Objects;

import static com.gym.facility.ui.common.FacilityUiHelper.button;
import static com.gym.facility.ui.common.FacilityUiHelper.dangerButton;
import static com.gym.facility.ui.common.FacilityUiHelper.enumRenderer;


/**
 * Giao diện quản lý thiết bị.
 *
 * UI chỉ gọi Service, không gọi DAO trực tiếp.
 *
 * Các chức năng:
 * - Xem danh sách thiết bị.
 * - Thêm thiết bị.
 * - Cập nhật thiết bị.
 * - Xóa thiết bị.
 * - Thay đổi trạng thái thiết bị.
 * - Hiển thị các buổi tập bị ảnh hưởng khi thiết bị
 *   chuyển sang UNDER_MAINTENANCE hoặc BROKEN.
 */
public class EquipmentPanel extends JPanel {

    private static final int PAGE_SIZE = 10;

    private final EquipmentService equipmentService;

    private final RoomService roomService;

    /**
     * Callback được Dashboard truyền vào.
     *
     * Khi thiết bị được thêm, sửa, xóa hoặc đổi trạng thái,
     * callback sẽ yêu cầu MaintenancePanel tải lại dữ liệu.
     */
    private final Runnable onEquipmentChanged;

    private final DataTable<Equipment> equipmentTable;

    /** Bảng tổng hợp các thiết bị cần kiểm tra/bảo trì. */
    private final DataTable<Map<String, Object>> healthTable;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public EquipmentPanel(
            EquipmentService equipmentService,
            RoomService roomService,
            Runnable onEquipmentChanged
    ) {

        this.equipmentService =
                Objects.requireNonNull(equipmentService);

        this.roomService =
                Objects.requireNonNull(roomService);

        this.onEquipmentChanged =
                Objects.requireNonNull(onEquipmentChanged);

        setLayout(new BorderLayout());

        equipmentTable = createEquipmentTable();
        healthTable = createHealthTable();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Quản lý thiết bị", createEquipmentTab());
        tabs.addTab("Sức khỏe thiết bị", createEquipmentHealthTab());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() != null
                    && tabs.getSelectedIndex() == 1) {
                healthTable.reload();
            }
        });

        add(tabs, BorderLayout.CENTER);
    }


    // =========================================================
    // LOAD DATA
    // =========================================================

    /**
     * Tải lại danh sách thiết bị.
     */
    public void loadData() {

        equipmentTable.reload();
        healthTable.reload();
    }


    // =========================================================
    // EQUIPMENT HEALTH TABLE
    // =========================================================

    /**
     * Bảng này nhận dữ liệu tổng hợp trực tiếp từ EquipmentService.
     * Không tạo model/DAO/service riêng cho sức khỏe thiết bị.
     */
    private DataTable<Map<String, Object>> createHealthTable() {
        DataTable<Map<String, Object>> table = new DataTable<>(50);
        table.addColumn("Mã thiết bị", row -> row.get("equipmentId"), 90)
                .addColumn("Tên thiết bị", row -> row.get("equipmentName"), 210)
                .addColumn("Mã phòng", row -> row.get("roomId"), 90)
                .addColumn("Trạng thái", row -> row.get("operationalStatus"), 150)
                .addColumn("Bảo trì gần nhất", row -> row.get("lastMaintenance"), 140)
                .addColumn("Số lần bảo trì", row -> row.get("maintenanceCount"), 110)
                .addColumn("Tổng chi phí", row -> row.get("totalCost"), 130)
                .addColumn("Cảnh báo", this::healthWarningText, 300)
                .setLoader(equipmentService::findHealthWarningsPage);
        return table;
    }

    /** Tạo tab sức khỏe, dùng chung EquipmentService hiện có. */
    private JPanel createEquipmentHealthTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 2, 0, 2));

        JLabel title = new JLabel("Thiết bị cần theo dõi sức khỏe");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel description = new JLabel(
                "Hiển thị thiết bị bị hỏng, chưa từng có nhật ký bảo trì "
                        + "hoặc đã quá 90 ngày kể từ lần bảo trì gần nhất.");
        JButton refresh = button("Làm mới");
        refresh.addActionListener(e -> healthTable.reload());

        JPanel heading = new JPanel(new BorderLayout(8, 6));
        JPanel headingText = new JPanel(new BorderLayout(0, 4));
        headingText.add(title, BorderLayout.NORTH);
        headingText.add(description, BorderLayout.CENTER);
        heading.add(headingText, BorderLayout.CENTER);
        heading.add(refresh, BorderLayout.EAST);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(healthTable, BorderLayout.CENTER);
        return panel;
    }

    /** Sinh nội dung cảnh báo từ dữ liệu tổng hợp do DAO trả về. */
    private String healthWarningText(Map<String, Object> row) {
        List<String> warnings = new java.util.ArrayList<>();
        String status = Objects.toString(row.get("operationalStatus"), "");
        Object maintenanceValue = row.get("lastMaintenance");

        if ("Broken".equalsIgnoreCase(status)
                || "BROKEN".equalsIgnoreCase(status)) {
            warnings.add("Thiết bị đang hỏng");
        }
        if (maintenanceValue == null) {
            warnings.add("Chưa có lịch sử bảo trì");
        } else if (maintenanceValue instanceof LocalDate date
                && date.isBefore(LocalDate.now().minusDays(90))) {
            warnings.add("Quá 90 ngày chưa bảo trì");
        }
        return warnings.isEmpty() ? "Cần kiểm tra" : String.join("; ", warnings);
    }


    // =========================================================
    // EQUIPMENT TABLE
    // =========================================================

    /**
     * Tạo bảng danh sách thiết bị.
     */
    private DataTable<Equipment> createEquipmentTable() {

        DataTable<Equipment> table =
                new DataTable<>(PAGE_SIZE);

        table.addColumn(
                        "Mã thiết bị",
                        Equipment::getEquipmentId,
                        90
                )
                .addColumn(
                        "Tên thiết bị",
                        Equipment::getEquipmentName,
                        220
                )
                .addColumn(
                        "Mã phòng",
                        Equipment::getRoomId,
                        90
                )
                .addColumn(
                        "Tình trạng",
                        Equipment::getOperationalStatus,
                        160
                )
                .addColumn(
                        "Ngày mua",
                        Equipment::getPurchaseDate,
                        120
                )
                .setLoader(equipmentService::findPage);

        return table;
    }


    // =========================================================
    // EQUIPMENT TAB
    // =========================================================

    /**
     * Tạo khu vực danh sách và các nút chức năng.
     */
    private JPanel createEquipmentTab() {

        JButton add = button("+ Thêm thiết bị");

        JButton edit = button("Sửa");

        JButton status = button("Đổi trạng thái");

        JButton delete = dangerButton("Xóa");

        JButton refresh = button("Làm mới");

        // Ban đầu chưa chọn thiết bị.
        edit.setEnabled(false);

        status.setEnabled(false);

        delete.setEnabled(false);


        // =====================================================
        // SELECTION
        // =====================================================

        equipmentTable.onSelectionChanged(equipment -> {

            boolean enabled = equipment != null;

            edit.setEnabled(enabled);

            status.setEnabled(enabled);

            delete.setEnabled(enabled);
        });


        // =====================================================
        // ADD
        // =====================================================

        add.addActionListener(
                e -> loadRoomsThenOpenEquipmentEditor(null)
        );


        // =====================================================
        // EDIT
        // =====================================================

        edit.addActionListener(e -> {

            Equipment equipment =
                    equipmentTable.getSelected();

            if (equipment != null) {

                loadRoomsThenOpenEquipmentEditor(equipment);
            }
        });


        // =====================================================
        // CHANGE STATUS
        // =====================================================

        status.addActionListener(e -> {

            Equipment equipment =
                    equipmentTable.getSelected();

            if (equipment != null) {

                openEquipmentStatusEditor(equipment);
            }
        });


        // =====================================================
        // DELETE
        // =====================================================

        delete.addActionListener(e -> {

            Equipment equipment =
                    equipmentTable.getSelected();

            if (equipment == null) {
                return;
            }

            boolean confirmed = Dialogs.confirm(
                    this,
                    "Xóa thiết bị '"
                            + equipment.getEquipmentName()
                            + "'?\n"
                            + "Nếu thiết bị đang được tham chiếu bởi lịch tập "
                            + "hoặc phiếu bảo trì, hệ thống có thể từ chối xóa."
            );

            if (!confirmed) {
                return;
            }

            runUiAction(
                    () -> equipmentService.delete(
                            equipment.getEquipmentId()
                    ),
                    this::afterEquipmentChanged
            );
        });


        // =====================================================
        // REFRESH
        // =====================================================

        refresh.addActionListener(
                e -> equipmentTable.reload()
        );


        return listPanel(
                "Danh sách thiết bị",
                add,
                edit,
                status,
                delete,
                refresh
        );
    }


    // =========================================================
    // LOAD ROOMS
    // =========================================================

    /**
     * Lấy danh sách phòng trước khi mở form thiết bị.
     *
     * Form thiết bị cần JComboBox để chọn phòng.
     */
    private void loadRoomsThenOpenEquipmentEditor(
            Equipment initial
    ) {

        Async.run(
                this,

                roomService::findAll,

                rooms -> openEquipmentEditor(
                        initial,
                        rooms
                ),

                ex -> Dialogs.error(this, ex)
        );
    }


    // =========================================================
    // EQUIPMENT EDITOR
    // =========================================================

    /**
     * Mở form thêm hoặc sửa thiết bị.
     */
    private void openEquipmentEditor(
            Equipment existing,
            List<Room> rooms
    ) {

        boolean editing = existing != null;

        JDialog dialog = createDialog(
                editing ? "Sửa thiết bị" : "Thêm thiết bị",
                540,
                470
        );

        FormPanel form = new FormPanel();


        // =====================================================
        // EQUIPMENT NAME
        // =====================================================

        JTextField name = form.addText(
                "name",
                "Tên thiết bị",
                true
        );


        // =====================================================
        // ROOM
        // =====================================================

        JComboBox<RoomChoice> room =
                new JComboBox<>();

        room.addItem(
                new RoomChoice(null)
        );

        for (Room r : rooms) {

            room.addItem(
                    new RoomChoice(r)
            );
        }

        room.setRenderer(roomRenderer());

        form.addComponent(
                "room",
                "Phòng",
                false,
                room
        );


        // =====================================================
        // STATUS
        // =====================================================

        JComboBox<EquipmentStatus> status =
                form.addCombo(
                        "status",
                        "Tình trạng",
                        true,
                        EquipmentStatus.values()
                );

        status.setRenderer(enumRenderer());

        /*
         * Không cho đổi trạng thái tại form thêm/sửa.
         *
         * Đổi trạng thái phải thông qua nút riêng
         * để chạy nghiệp vụ changeStatusWithAffectedSessions().
         */
        status.setEnabled(false);

        status.setToolTipText(
                "Sử dụng nút Đổi trạng thái tại danh sách thiết bị."
        );


        // =====================================================
        // PURCHASE DATE
        // =====================================================

        DatePicker purchase = form.addDate(
                "purchase",
                "Ngày mua",
                false
        );


        // =====================================================
        // FILL DATA
        // =====================================================

        if (editing) {

            name.setText(
                    existing.getEquipmentName()
            );

            setSelectedRoom(
                    room,
                    existing.getRoomId()
            );

            status.setSelectedItem(
                    existing.getOperationalStatus()
            );

            purchase.setDate(
                    existing.getPurchaseDate()
            );

        } else {

            status.setSelectedItem(
                    EquipmentStatus.ACTIVE
            );
        }


        // =====================================================
        // CONTENT
        // =====================================================

        JPanel content = new JPanel(
                new BorderLayout()
        );

        content.setBorder(
                BorderFactory.createEmptyBorder(
                        14, 14, 8, 14
                )
        );

        content.add(
                form,
                BorderLayout.CENTER
        );


        // =====================================================
        // BUTTONS
        // =====================================================

        addDialogButtons(
                dialog,
                content,
                () -> saveEquipment(
                        dialog,
                        editing,
                        existing,
                        name,
                        room,
                        purchase
                )
        );

        dialog.setContentPane(content);

        dialog.setVisible(true);
    }


    // =========================================================
    // SAVE EQUIPMENT
    // =========================================================

    /**
     * Thêm hoặc cập nhật thiết bị.
     */
    private void saveEquipment(
            JDialog dialog,
            boolean editing,
            Equipment existing,
            JTextField name,
            JComboBox<RoomChoice> room,
            DatePicker purchase
    ) {

        try {

            Equipment equipment = new Equipment();

            if (editing) {

                equipment.setEquipmentId(
                        existing.getEquipmentId()
                );
            }


            // Tên thiết bị.
            equipment.setEquipmentName(
                    name.getText()
            );


            // Phòng được chọn.
            RoomChoice selectedRoom =
                    (RoomChoice) room.getSelectedItem();

            equipment.setRoomId(
                    selectedRoom == null
                            || selectedRoom.room() == null
                            ? null
                            : selectedRoom.room().getRoomId()
            );


            /*
             * Giữ nguyên trạng thái hiện tại khi sửa.
             * Thiết bị mới mặc định ACTIVE.
             */
            equipment.setOperationalStatus(
                    editing
                            ? existing.getOperationalStatus()
                            : EquipmentStatus.ACTIVE
            );


            // Ngày mua.
            equipment.setPurchaseDate(
                    purchase.getDate()
            );


            // Thực hiện lưu ở background thread.
            Async.run(
                    dialog,

                    () -> {

                        if (editing) {

                            equipmentService.update(equipment);

                        } else {

                            equipmentService.create(equipment);
                        }

                        return Boolean.TRUE;
                    },

                    ignored -> {

                        Dialogs.info(
                                dialog,
                                editing
                                        ? "Đã cập nhật thiết bị."
                                        : "Đã thêm thiết bị."
                        );

                        dialog.dispose();

                        afterEquipmentChanged();
                    },

                    ex -> Dialogs.error(dialog, ex)
            );

        } catch (Exception ex) {

            Dialogs.error(dialog, ex);
        }
    }


    // =========================================================
    // CHANGE EQUIPMENT STATUS
    // =========================================================

    /**
     * Mở dialog đổi trạng thái thiết bị.
     */
    private void openEquipmentStatusEditor(
            Equipment equipment
    ) {

        JDialog dialog = createDialog(
                "Đổi trạng thái thiết bị",
                460,
                220
        );

        JPanel content = new JPanel(
                new BorderLayout(8, 12)
        );

        content.setBorder(
                BorderFactory.createEmptyBorder(
                        16, 16, 8, 16
                )
        );


        // Thông tin thiết bị.
        JLabel equipmentLabel = new JLabel(
                "Thiết bị: "
                        + equipment.getEquipmentName()
                        + " (Mã: "
                        + equipment.getEquipmentId()
                        + ")"
        );

        content.add(
                equipmentLabel,
                BorderLayout.NORTH
        );


        // Chọn trạng thái mới.
        JComboBox<EquipmentStatus> status =
                new JComboBox<>(
                        EquipmentStatus.values()
                );

        status.setRenderer(enumRenderer());

        status.setSelectedItem(
                equipment.getOperationalStatus()
        );

        content.add(
                status,
                BorderLayout.CENTER
        );


        // Nút xác nhận.
        addDialogButtons(
                dialog,
                content,
                () -> changeEquipmentStatus(
                        dialog,
                        equipment,
                        status
                )
        );

        dialog.setContentPane(content);

        dialog.setVisible(true);
    }


    /**
     * Thay đổi trạng thái thiết bị.
     *
     * Sử dụng changeStatusWithAffectedSessions().
     *
     * Không gọi DAO trực tiếp.
     */
    private void changeEquipmentStatus(
            JDialog dialog,
            Equipment equipment,
            JComboBox<EquipmentStatus> status
    ) {

        try {

            EquipmentStatus selected =
                    (EquipmentStatus) status.getSelectedItem();

            if (selected == null) {

                throw new BusinessException(
                        "Chưa chọn tình trạng thiết bị."
                );
            }


            // Không cho chọn lại trạng thái hiện tại.
            if (selected == equipment.getOperationalStatus()) {

                Dialogs.warn(
                        dialog,
                        "Thiết bị đang ở trạng thái này."
                );

                return;
            }


            // Xác nhận trước khi cập nhật.
            boolean confirmed = Dialogs.confirm(
                    dialog,
                    "Bạn có chắc chắn muốn chuyển thiết bị '"
                            + equipment.getEquipmentName()
                            + "' sang trạng thái '"
                            + selected.getLabel()
                            + "'?"
            );

            if (!confirmed) {
                return;
            }


            /*
             * Chạy Service ở background thread.
             *
             * Service trả về:
             * - Equipment sau khi cập nhật.
             * - Danh sách buổi tập bị ảnh hưởng.
             */
            Async.run(
                    dialog,

                    () -> equipmentService.changeStatusWithAffectedSessions(
                            equipment.getEquipmentId(),
                            selected
                    ),

                    result -> {

                        // Đóng dialog đổi trạng thái.
                        dialog.dispose();

                        // Tải lại danh sách thiết bị và bảo trì.
                        afterEquipmentChanged();

                        // Hiển thị kết quả.
                        showAffectedSessionsDialog(result);
                    },

                    ex -> Dialogs.error(dialog, ex)
            );

        } catch (Exception ex) {

            Dialogs.error(dialog, ex);
        }
    }


    // =========================================================
    // SHOW AFFECTED SESSIONS
    // =========================================================

    /**
     * Hiển thị danh sách buổi tập bị ảnh hưởng.
     *
     * Dữ liệu lấy từ EquipmentStatusChangeResult.
     */
    private void showAffectedSessionsDialog(
            EquipmentStatusChangeResult result
    ) {

        Equipment equipment = result.equipment();

        List<com.gym.core.model.Session> sessions =
                result.affectedSessions();


        // =====================================================
        // NO AFFECTED SESSION
        // =====================================================

        if (sessions.isEmpty()) {

            Dialogs.info(
                    this,
                    "Đã đổi trạng thái thiết bị '"
                            + equipment.getEquipmentName()
                            + "' thành "
                            + equipment.getOperationalStatus().getLabel()
                            + ".\n\n"
                            + "Không có lịch tập nào bị ảnh hưởng."
            );

            return;
        }


        // =====================================================
        // CREATE TABLE
        // =====================================================

        DataTable<com.gym.core.model.Session> table =
                new DataTable<>(PAGE_SIZE);

        table.addColumn(
                        "Mã buổi",
                        com.gym.core.model.Session::getSessionId,
                        75
                )
                .addColumn(
                        "Tên buổi tập",
                        com.gym.core.model.Session::getSessionTitle,
                        190
                )
                .addColumn(
                        "Mã HLV",
                        com.gym.core.model.Session::getTrainerId,
                        75
                )
                .addColumn(
                        "Mã phòng",
                        com.gym.core.model.Session::getRoomId,
                        75
                )
                .addColumn(
                        "Bắt đầu",
                        com.gym.core.model.Session::getStartTime,
                        150
                )
                .addColumn(
                        "Kết thúc",
                        com.gym.core.model.Session::getEndTime,
                        150
                );


        // Đưa dữ liệu từ Service vào bảng.
        table.setItems(sessions);


        // =====================================================
        // DIALOG
        // =====================================================

        JDialog dialog = createDialog(
                "Lịch tập bị ảnh hưởng",
                950,
                500
        );

        JPanel content = new JPanel(
                new BorderLayout(0, 12)
        );

        content.setBorder(
                BorderFactory.createEmptyBorder(
                        14, 14, 12, 14
                )
        );


        // =====================================================
        // WARNING
        // =====================================================

        JLabel warning = new JLabel(
                "<html>"
                        + "<b>Thiết bị:</b> "
                        + equipment.getEquipmentName()
                        + " (#"
                        + equipment.getEquipmentId()
                        + ")"
                        + "<br>"
                        + "<b>Trạng thái mới:</b> "
                        + equipment.getOperationalStatus().getLabel()
                        + "<br>"
                        + "<b>Số buổi tập bị ảnh hưởng:</b> "
                        + sessions.size()
                        + "<br><br>"
                        + "<font color='#C62828'>"
                        + "Các buổi tập dưới đây có sử dụng thiết bị này. "
                        + "Vui lòng kiểm tra và sắp xếp lại thiết bị."
                        + "</font>"
                        + "</html>"
        );

        content.add(
                warning,
                BorderLayout.NORTH
        );


        // =====================================================
        // TABLE
        // =====================================================

        content.add(
                table,
                BorderLayout.CENTER
        );


        // =====================================================
        // FOOTER
        // =====================================================

        JButton close = button("Đóng");

        JPanel footer = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        footer.add(close);

        close.addActionListener(
                e -> dialog.dispose()
        );

        content.add(
                footer,
                BorderLayout.SOUTH
        );


        // =====================================================
        // SHOW
        // =====================================================

        dialog.setContentPane(content);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }


    // =========================================================
    // ROOM RENDERER
    // =========================================================

    /**
     * Renderer cho JComboBox phòng.
     */
    private ListCellRenderer<RoomChoice> roomRenderer() {

        return (list, value, index, selected, focus) -> {

            JLabel label = new JLabel(
                    value == null || value.room() == null
                            ? "Không gán phòng"
                            : value.room().getRoomId()
                            + " - "
                            + value.room().getRoomName()
            );

            if (selected) {

                label.setOpaque(true);

                label.setBackground(
                        list.getSelectionBackground()
                );

                label.setForeground(
                        list.getSelectionForeground()
                );
            }

            label.setBorder(
                    BorderFactory.createEmptyBorder(
                            3, 5, 3, 5
                    )
            );

            return label;
        };
    }


    // =========================================================
    // SET SELECTED ROOM
    // =========================================================

    /**
     * Chọn phòng tương ứng khi mở form sửa thiết bị.
     */
    private void setSelectedRoom(
            JComboBox<RoomChoice> combo,
            Integer roomId
    ) {

        for (int i = 0; i < combo.getItemCount(); i++) {

            RoomChoice choice = combo.getItemAt(i);

            Integer candidate =
                    choice.room() == null
                            ? null
                            : choice.room().getRoomId();

            if (Objects.equals(candidate, roomId)) {

                combo.setSelectedIndex(i);

                return;
            }
        }

        combo.setSelectedIndex(0);
    }


    // =========================================================
    // LIST PANEL
    // =========================================================

    /**
     * Tạo panel danh sách và toolbar.
     */
    private JPanel listPanel(
            String title,
            JButton... actions
    ) {

        JPanel panel = new JPanel(
                new BorderLayout(0, 10)
        );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        12, 2, 0, 2
                )
        );


        JLabel sectionTitle = new JLabel(title);

        sectionTitle.setFont(
                sectionTitle.getFont().deriveFont(
                        Font.BOLD,
                        16f
                )
        );


        JPanel toolbar = new JPanel(
                new BorderLayout(8, 8)
        );

        toolbar.add(
                sectionTitle,
                BorderLayout.WEST
        );


        JPanel right = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 6, 0)
        );

        for (JButton action : actions) {

            right.add(action);
        }

        toolbar.add(
                right,
                BorderLayout.EAST
        );


        panel.add(
                toolbar,
                BorderLayout.NORTH
        );

        panel.add(
                equipmentTable,
                BorderLayout.CENTER
        );

        return panel;
    }


    // =========================================================
    // CREATE DIALOG
    // =========================================================

    /**
     * Tạo dialog dùng chung.
     */
    private JDialog createDialog(
            String title,
            int width,
            int height
    ) {

        Window owner =
                javax.swing.SwingUtilities.getWindowAncestor(this);

        JDialog dialog = new JDialog(
                owner,
                title,
                JDialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );

        dialog.setMinimumSize(
                new Dimension(width, height)
        );

        dialog.setSize(width, height);

        dialog.setLocationRelativeTo(this);

        return dialog;
    }


    // =========================================================
    // DIALOG BUTTONS
    // =========================================================

    /**
     * Tạo nút Hủy và Lưu.
     */
    private void addDialogButtons(
            JDialog dialog,
            JPanel content,
            Runnable saveAction
    ) {

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 4)
        );

        JButton cancel = button("Hủy");

        JButton save = button("Lưu");


        cancel.addActionListener(
                e -> dialog.dispose()
        );

        save.addActionListener(
                e -> saveAction.run()
        );


        actions.add(cancel);

        actions.add(save);

        content.add(
                actions,
                BorderLayout.SOUTH
        );
    }


    // =========================================================
    // RUN UI ACTION
    // =========================================================

    /**
     * Chạy nghiệp vụ ở background thread.
     */
    private void runUiAction(
            Runnable action,
            Runnable onSuccess
    ) {

        Async.run(
                this,

                () -> {

                    action.run();

                    return Boolean.TRUE;
                },

                ignored -> {

                    Dialogs.info(
                            this,
                            "Thao tác đã hoàn tất."
                    );

                    onSuccess.run();
                },

                ex -> Dialogs.error(this, ex)
        );
    }


    // =========================================================
    // AFTER EQUIPMENT CHANGED
    // =========================================================

    /**
     * Sau khi thiết bị thay đổi:
     * - Tải lại bảng thiết bị.
     * - Yêu cầu MaintenancePanel tải lại danh sách.
     */
    private void afterEquipmentChanged() {

        equipmentTable.reload();
        healthTable.reload();

        onEquipmentChanged.run();
    }


    // =========================================================
    // ROOM CHOICE
    // =========================================================

    /**
     * Wrapper để JComboBox hiển thị Room.
     */
    private record RoomChoice(Room room) {
    }
}

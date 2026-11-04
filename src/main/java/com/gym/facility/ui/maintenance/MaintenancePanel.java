
package com.gym.facility.ui.maintenance;

import com.github.lgooddatepicker.components.DatePicker;
import com.gym.app.Session;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.MaintenanceLog;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.Dialogs;
import com.gym.core.ui.FormPanel;
import com.gym.facility.service.EquipmentService;
import com.gym.facility.service.MaintenanceLogService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.gym.facility.ui.common.FacilityUiHelper.button;
import static com.gym.facility.ui.common.FacilityUiHelper.parseMoney;

/**
 * Giao diện quản lý phiếu bảo trì.
 */
public class MaintenancePanel extends JPanel {

    private static final int PAGE_SIZE = 10;

    private final EquipmentService equipmentService;
    private final com.gym.facility.service.StaffService staffService;
    private final MaintenanceLogService maintenanceLogService;

    private final DataTable<MaintenanceLog> maintenanceTable;

    private JComboBox<EquipmentChoice> maintenanceEquipmentCombo;

    private List<Staff> maintenanceSigners = new ArrayList<>();

    private boolean fillingMaintenanceOptions;

    public MaintenancePanel(
            EquipmentService equipmentService,
            com.gym.facility.service.StaffService staffService,
            MaintenanceLogService maintenanceLogService
    ) {

        this.equipmentService = Objects.requireNonNull(equipmentService);
        this.staffService = Objects.requireNonNull(staffService);
        this.maintenanceLogService =
                Objects.requireNonNull(maintenanceLogService);

        setLayout(new BorderLayout());

        maintenanceTable = createMaintenanceTable();

        add(createMaintenanceTab(), BorderLayout.CENTER);
    }

    /**
     * Được Dashboard gọi khi mở tab bảo trì.
     * Cũng được EquipmentPanel gọi sau khi thiết bị thay đổi.
     */
    public void reloadOptions() {
        loadMaintenanceOptions();
    }

    private DataTable<MaintenanceLog> createMaintenanceTable() {

        DataTable<MaintenanceLog> table =
                new DataTable<>(PAGE_SIZE);

        table.addColumn(
                        "Log_no",
                        MaintenanceLog::getLogNo,
                        80
                )
                .addColumn(
                        "Ngày bảo trì",
                        MaintenanceLog::getMaintenanceDate,
                        120
                )
                .addColumn(
                        "Chi phí",
                        MaintenanceLog::getCost,
                        120
                )
                .addColumn(
                        "Mô tả",
                        MaintenanceLog::getDescription,
                        300
                )
                .addColumn(
                        "Người ghi (mã NV)",
                        MaintenanceLog::getLoggedBy,
                        130
                );

        return table;
    }

    private JPanel createMaintenanceTab() {

        maintenanceEquipmentCombo = new JComboBox<>();

        maintenanceEquipmentCombo.setRenderer(
                equipmentChoiceRenderer()
        );

        maintenanceEquipmentCombo.addActionListener(e -> {

            if (!fillingMaintenanceOptions) {
                loadMaintenanceHistory();
            }
        });

        JButton add = button("+ Thêm phiếu bảo trì");
        JButton history = button("Xem lịch sử");
        JButton refresh = button("Làm mới thiết bị");

        add.addActionListener(
                e -> openMaintenanceEditor()
        );

        history.addActionListener(
                e -> loadMaintenanceHistory()
        );

        refresh.addActionListener(
                e -> loadMaintenanceOptions()
        );

        JPanel north = new JPanel(
                new BorderLayout(8, 8)
        );

        north.add(
                new JLabel("Lịch sử của thiết bị:"),
                BorderLayout.WEST
        );

        north.add(
                maintenanceEquipmentCombo,
                BorderLayout.CENTER
        );

        JPanel controls = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 6, 0)
        );

        controls.add(history);
        controls.add(refresh);
        controls.add(add);

        north.add(
                controls,
                BorderLayout.SOUTH
        );

        north.setBorder(
                BorderFactory.createEmptyBorder(
                        4, 2, 10, 2
                )
        );

        JPanel panel = new JPanel(
                new BorderLayout(0, 10)
        );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        12, 2, 0, 2
                )
        );

        panel.add(north, BorderLayout.NORTH);
        panel.add(maintenanceTable, BorderLayout.CENTER);

        return panel;
    }

    private void loadMaintenanceOptions() {

        Async.run(
                this,

                () -> {

                    List<Equipment> equipments =
                            equipmentService.findAll();

                    List<Staff> signers = new ArrayList<>();

                    signers.addAll(
                            staffService.findStaffByRole(
                                    StaffRole.RECEPTIONIST
                            )
                    );

                    signers.addAll(
                            staffService.findStaffByRole(
                                    StaffRole.MANAGER
                            )
                    );

                    return new MaintenanceOptions(
                            equipments,
                            signers
                    );
                },

                options -> {

                    fillingMaintenanceOptions = true;

                    try {

                        maintenanceEquipmentCombo.removeAllItems();

                        options.equipments().forEach(
                                equipment ->
                                        maintenanceEquipmentCombo.addItem(
                                                new EquipmentChoice(equipment)
                                        )
                        );

                        maintenanceSigners =
                                options.signers();

                        if (maintenanceEquipmentCombo.getItemCount() > 0) {
                            maintenanceEquipmentCombo.setSelectedIndex(0);
                        }

                    } finally {

                        fillingMaintenanceOptions = false;
                    }

                    loadMaintenanceHistory();
                },

                ex -> Dialogs.error(this, ex)
        );
    }

    private record MaintenanceOptions(
            List<Equipment> equipments,
            List<Staff> signers
    ) {
    }

    private record EquipmentChoice(Equipment equipment) {
    }

    private void loadMaintenanceHistory() {

        if (maintenanceEquipmentCombo == null) {
            return;
        }

        EquipmentChoice choice =
                (EquipmentChoice) maintenanceEquipmentCombo.getSelectedItem();

        if (choice == null || choice.equipment() == null) {

            maintenanceTable.setItems(List.of());

            return;
        }

        int equipmentId =
                choice.equipment().getEquipmentId();

        Async.run(
                this,

                () -> maintenanceLogService.findByEquipment(
                        equipmentId
                ),

                maintenanceTable::setItems,

                ex -> Dialogs.error(this, ex)
        );
    }

    private void openMaintenanceEditor() {

        EquipmentChoice choice =
                (EquipmentChoice) maintenanceEquipmentCombo.getSelectedItem();

        if (choice == null || choice.equipment() == null) {

            Dialogs.warn(
                    this,
                    "Hãy chọn thiết bị trước khi thêm phiếu bảo trì."
            );

            return;
        }

        if (maintenanceSigners.isEmpty()) {

            Dialogs.warn(
                    this,
                    "Chưa có nhân sự vai trò Lễ tân hoặc Quản lý "
                            + "để ghi phiếu (BR-FAC-05)."
            );

            return;
        }

        Equipment equipment = choice.equipment();

        if (equipment.getOperationalStatus()
                == EquipmentStatus.UNDER_MAINTENANCE) {

            Dialogs.warn(
                    this,
                    "Thiết bị '"
                            + equipment.getEquipmentName()
                            + "' đang được bảo trì.\n"
                            + "Không thể tạo thêm phiếu bảo trì cho thiết bị này."
            );

            return;
        }

        JDialog dialog = createDialog(
                "Thêm phiếu bảo trì - "
                        + equipment.getEquipmentName(),
                560,
                450
        );

        FormPanel form = new FormPanel();

        DatePicker date = form.addDate(
                "date",
                "Ngày bảo trì",
                true
        );

        date.setDate(LocalDate.now());

        JTextField cost = form.addText(
                "cost",
                "Chi phí (VND)",
                true
        );

        cost.setText("0.00");

        JTextArea description = form.addTextArea(
                "description",
                "Mô tả",
                true,
                4
        );

        JPanel content = new JPanel(
                new BorderLayout()
        );

        content.setBorder(
                BorderFactory.createEmptyBorder(
                        14, 14, 8, 14
                )
        );

        content.add(form, BorderLayout.CENTER);

        addDialogButtons(
                dialog,
                content,
                () -> saveMaintenanceLog(
                        dialog,
                        equipment,
                        date,
                        cost,
                        description
                )
        );

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void saveMaintenanceLog(
            JDialog dialog,
            Equipment equipment,
            DatePicker date,
            JTextField cost,
            JTextArea description
    ) {

        try {

            Staff selectedSigner = Session.getStaff();

            if (selectedSigner == null) {

                throw new BusinessException(
                        "Không xác định được nhân sự đang đăng nhập."
                );
            }

            boolean allowedSigner = maintenanceSigners.stream()
                    .anyMatch(staff ->
                            Objects.equals(
                                    staff.getStaffId(),
                                    selectedSigner.getStaffId()
                            )
                    );

            if (!allowedSigner) {

                throw new BusinessException(
                        "Chỉ nhân sự Lễ tân hoặc Quản lý "
                                + "mới được ghi phiếu bảo trì."
                );
            }

            MaintenanceLog log = new MaintenanceLog();

            log.setEquipmentId(
                    equipment.getEquipmentId()
            );

            log.setMaintenanceDate(
                    date.getDate()
            );

            log.setCost(
                    parseMoney(
                            cost.getText(),
                            "Chi phí"
                    )
            );

            log.setDescription(
                    description.getText()
            );

            log.setLoggedBy(
                    selectedSigner.getStaffId()
            );

            Async.run(
                    dialog,

                    () -> maintenanceLogService.create(log),

                    saved -> {

                        Dialogs.info(
                                dialog,
                                "Đã tạo phiếu bảo trì số "
                                        + saved.getLogNo()
                                        + " cho thiết bị "
                                        + equipment.getEquipmentId()
                                        + "."
                        );

                        dialog.dispose();

                        loadMaintenanceHistory();
                        loadMaintenanceOptions();
                    },

                    ex -> Dialogs.error(dialog, ex)
            );

        } catch (Exception ex) {

            Dialogs.error(dialog, ex);
        }
    }

    private ListCellRenderer<EquipmentChoice> equipmentChoiceRenderer() {

        return (list, value, index, selected, focus) -> {

            JLabel label = new JLabel(
                    value == null || value.equipment() == null
                            ? "Chọn thiết bị"
                            : value.equipment().getEquipmentId()
                            + " - "
                            + value.equipment().getEquipmentName()
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

        content.add(actions, BorderLayout.SOUTH);
    }
}

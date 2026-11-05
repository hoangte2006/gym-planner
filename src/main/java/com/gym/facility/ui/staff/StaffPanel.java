
package com.gym.facility.ui.staff;

import com.github.lgooddatepicker.components.DatePicker;
import com.gym.core.model.Manager;
import com.gym.core.model.Receptionist;
import com.gym.core.model.ShiftType;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.Dialogs;
import com.gym.core.ui.FormPanel;
import com.gym.facility.service.StaffService;
import com.gym.facility.ui.common.FacilityUiHelper;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.Objects;

import static com.gym.facility.ui.common.FacilityUiHelper.blankToNull;
import static com.gym.facility.ui.common.FacilityUiHelper.button;
import static com.gym.facility.ui.common.FacilityUiHelper.dangerButton;
import static com.gym.facility.ui.common.FacilityUiHelper.enumRenderer;
import static com.gym.facility.ui.common.FacilityUiHelper.parseMoney;

/**
 * Giao diện quản lý nhân sự.
 */
public class StaffPanel extends JPanel {

    private static final int PAGE_SIZE = 10;

    private final StaffService staffService;

    private final DataTable<Staff> staffTable;

    public StaffPanel(StaffService staffService) {

        this.staffService = Objects.requireNonNull(staffService);

        setLayout(new BorderLayout());

        staffTable = createStaffTable();

        add(createStaffTab(), BorderLayout.CENTER);
    }

    /**
     * Tải lại dữ liệu nhân sự.
     */
    public void loadData() {
        staffTable.reload();
    }

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

            boolean selected = staff != null;

            edit.setEnabled(selected);
            delete.setEnabled(selected);
        });

        add.addActionListener(e -> openStaffEditor(null));

        edit.addActionListener(e -> {

            Staff selected = staffTable.getSelected();

            if (selected != null) {
                openStaffEditor(selected);
            }
        });

        delete.addActionListener(e -> {

            Staff selected = staffTable.getSelected();

            if (selected == null) {
                return;
            }

            if (Dialogs.confirmDelete(
                    this,
                    "nhân sự " + selected.getFullName()
            )) {

                runUiAction(
                        () -> staffService.deleteStaff(
                                selected.getStaffId()
                        ),
                        staffTable::reload
                );
            }
        });

        refresh.addActionListener(e -> staffTable.reload());

        return listPanel(
                "Danh sách nhân sự",
                add,
                edit,
                delete,
                refresh
        );
    }

    private void openStaffEditor(Staff existing) {

        boolean editing = existing != null;

        JDialog dialog = createDialog(
                editing ? "Sửa nhân sự" : "Thêm nhân sự",
                540,
                600
        );

        FormPanel common = new FormPanel();

        JTextField fullName = common.addText(
                "fullName",
                "Họ và tên",
                true
        );

        JTextField phone = common.addText(
                "phone",
                "Số điện thoại",
                true
        );

        JTextField salary = common.addText(
                "salary",
                "Lương (VND)",
                true
        );

        DatePicker hireDate = common.addDate(
                "hireDate",
                "Ngày vào làm",
                false
        );

        JComboBox<StaffRole> role = common.addCombo(
                "role",
                "Vai trò",
                true,
                StaffRole.values()
        );

        role.setRenderer(enumRenderer());

        if (editing) {

            role.setSelectedItem(existing.getRole());
            role.setEnabled(false);

            fullName.setText(existing.getFullName());
            phone.setText(existing.getPhone());

            salary.setText(
                    existing.getSalary() == null
                            ? ""
                            : existing.getSalary().toPlainString()
            );

            hireDate.setDate(existing.getHireDate());

        } else {

            role.setSelectedItem(StaffRole.TRAINER);
            salary.setText("500.00");
        }

        // Form riêng cho Trainer.
        FormPanel trainerForm = new FormPanel();

        JTextField specialty = trainerForm.addText(
                "specialty",
                "Chuyên môn",
                true
        );

        JTextField certification = trainerForm.addText(
                "certification",
                "Chứng chỉ",
                false
        );

        // Form riêng cho Receptionist.
        FormPanel receptionistForm = new FormPanel();

        JComboBox<ShiftType> shift = receptionistForm.addCombo(
                "shift",
                "Ca làm",
                true,
                ShiftType.values()
        );

        shift.setRenderer(enumRenderer());

        // Form riêng cho Manager.
        JPanel managerForm = new JPanel(new BorderLayout());

        managerForm.add(
                new JLabel(
                        "Vai trò Quản lý không có trường riêng."
                ),
                BorderLayout.NORTH
        );

        /*
         * CardLayout giúp chuyển đổi form theo vai trò.
         */
        JPanel roleCards = new JPanel(new CardLayout());

        roleCards.add(
                trainerForm,
                StaffRole.TRAINER.name()
        );

        roleCards.add(
                receptionistForm,
                StaffRole.RECEPTIONIST.name()
        );

        roleCards.add(
                managerForm,
                StaffRole.MANAGER.name()
        );

        role.addActionListener(e -> {

            StaffRole selectedRole =
                    (StaffRole) role.getSelectedItem();

            if (selectedRole != null) {

                ((CardLayout) roleCards.getLayout())
                        .show(
                                roleCards,
                                selectedRole.name()
                        );
            }
        });

        // Đổ dữ liệu riêng theo loại nhân sự khi sửa.
        if (existing instanceof Trainer trainer) {

            specialty.setText(trainer.getSpecialty());

            certification.setText(
                    trainer.getCertification()
            );

        } else if (existing instanceof Receptionist receptionist) {

            shift.setSelectedItem(
                    receptionist.getShiftType()
            );
        }

        StaffRole initialRole = editing
                ? existing.getRole()
                : StaffRole.TRAINER;

        ((CardLayout) roleCards.getLayout())
                .show(
                        roleCards,
                        initialRole.name()
                );

        JPanel content = new JPanel(
                new BorderLayout(0, 8)
        );

        content.setBorder(
                BorderFactory.createEmptyBorder(
                        14, 14, 8, 14
                )
        );

        content.add(common, BorderLayout.NORTH);
        content.add(roleCards, BorderLayout.CENTER);

        addDialogButtons(
                dialog,
                content,
                () -> saveStaff(
                        dialog,
                        editing,
                        existing,
                        fullName,
                        phone,
                        salary,
                        hireDate,
                        role,
                        specialty,
                        certification,
                        shift
                )
        );

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void saveStaff(
            JDialog dialog,
            boolean editing,
            Staff existing,
            JTextField fullName,
            JTextField phone,
            JTextField salary,
            DatePicker hireDate,
            JComboBox<StaffRole> role,
            JTextField specialty,
            JTextField certification,
            JComboBox<ShiftType> shift
    ) {

        try {

            StaffRole selectedRole =
                    (StaffRole) role.getSelectedItem();

            if (selectedRole == null) {
                throw new IllegalArgumentException(
                        "Vui lòng chọn vai trò nhân sự."
                );
            }

            Staff staff;

            if (selectedRole == StaffRole.TRAINER) {

                Trainer trainer = new Trainer();

                trainer.setSpecialty(
                        specialty.getText()
                );

                trainer.setCertification(
                        blankToNull(certification.getText())
                );

                staff = trainer;

            } else if (selectedRole == StaffRole.RECEPTIONIST) {

                Receptionist receptionist = new Receptionist();

                receptionist.setShiftType(
                        (ShiftType) shift.getSelectedItem()
                );

                staff = receptionist;

            } else {

                staff = new Manager();
            }

            if (editing) {
                staff.setStaffId(existing.getStaffId());
            }

            staff.setFullName(fullName.getText());
            staff.setPhone(phone.getText());

            staff.setSalary(
                    parseMoney(
                            salary.getText(),
                            "Lương"
                    )
            );

            staff.setHireDate(hireDate.getDate());

            Async.run(
                    dialog,

                    () -> {

                        if (editing) {
                            staffService.updateStaff(staff);
                        } else {
                            staffService.createStaff(staff);
                        }

                        return Boolean.TRUE;
                    },

                    ignored -> {

                        Dialogs.info(
                                dialog,
                                editing
                                        ? "Đã cập nhật nhân sự."
                                        : "Đã thêm nhân sự."
                        );

                        dialog.dispose();
                        staffTable.reload();
                    },

                    ex -> Dialogs.error(dialog, ex)
            );

        } catch (Exception ex) {

            Dialogs.error(dialog, ex);
        }
    }

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

        toolbar.add(right, BorderLayout.EAST);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(staffTable, BorderLayout.CENTER);

        return panel;
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

        cancel.addActionListener(e -> dialog.dispose());

        save.addActionListener(e -> saveAction.run());

        actions.add(cancel);
        actions.add(save);

        content.add(actions, BorderLayout.SOUTH);
    }

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
}

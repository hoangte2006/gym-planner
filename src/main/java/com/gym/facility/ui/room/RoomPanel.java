
package com.gym.facility.ui.room;

import com.gym.core.model.Equipment;
import com.gym.core.model.Room;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.Dialogs;
import com.gym.core.ui.FormPanel;
import com.gym.facility.service.EquipmentService;
import com.gym.facility.service.RoomService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.List;
import java.util.Objects;

import static com.gym.facility.ui.common.FacilityUiHelper.button;
import static com.gym.facility.ui.common.FacilityUiHelper.blankToNull;
import static com.gym.facility.ui.common.FacilityUiHelper.dangerButton;

/**
 * Giao diện quản lý phòng tập.
 */
public class RoomPanel extends JPanel {

    private static final int PAGE_SIZE = 10;

    private final RoomService roomService;
    private final EquipmentService equipmentService;

    private final DataTable<Room> roomTable;

    public RoomPanel(
            RoomService roomService,
            EquipmentService equipmentService
    ) {

        this.roomService = Objects.requireNonNull(roomService);
        this.equipmentService = Objects.requireNonNull(equipmentService);

        setLayout(new BorderLayout());

        roomTable = createRoomTable();

        add(createRoomTab(), BorderLayout.CENTER);
    }

    public void loadData() {
        roomTable.reload();
    }

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

        edit.setEnabled(false);
        detail.setEnabled(false);
        delete.setEnabled(false);

        roomTable.onSelectionChanged(room -> {

            boolean enabled = room != null;

            edit.setEnabled(enabled);
            detail.setEnabled(enabled);
            delete.setEnabled(enabled);
        });

        add.addActionListener(e -> openRoomEditor(null));

        edit.addActionListener(e -> {

            Room room = roomTable.getSelected();

            if (room != null) {
                openRoomEditor(room);
            }
        });

        detail.addActionListener(e -> {

            Room room = roomTable.getSelected();

            if (room != null) {
                openRoomEquipment(room);
            }
        });

        delete.addActionListener(e -> {

            Room room = roomTable.getSelected();

            if (room == null) {
                return;
            }

            if (Dialogs.confirmDelete(
                    this,
                    "phòng " + room.getRoomName()
            )) {

                runUiAction(
                        () -> roomService.deleteRoom(
                                room.getRoomId()
                        ),
                        roomTable::reload
                );
            }
        });

        refresh.addActionListener(e -> roomTable.reload());

        return listPanel(
                "Danh sách phòng tập",
                add,
                edit,
                detail,
                delete,
                refresh
        );
    }

    private void openRoomEditor(Room existing) {

        boolean editing = existing != null;

        JDialog dialog = createDialog(
                editing ? "Sửa phòng tập" : "Thêm phòng tập",
                470,
                360
        );

        FormPanel form = new FormPanel();

        JTextField name = form.addText(
                "name",
                "Tên phòng",
                true
        );

        JTextField capacity = form.addText(
                "capacity",
                "Sức chứa",
                true
        );

        JTextField floor = form.addText(
                "floor",
                "Tầng / vị trí",
                false
        );

        if (editing) {

            name.setText(existing.getRoomName());

            capacity.setText(
                    String.valueOf(existing.getCapacity())
            );

            floor.setText(existing.getFloorLocation());

        } else {

            capacity.setText("20");
            floor.setText("Tầng 1");
        }

        JLabel capacityHint = new JLabel(
                "<html><small>"
                        + "Lưu ý: Không thể giảm sức chứa phòng thấp hơn "
                        + "sức chứa tối đa của lớp đã được xếp trong phòng."
                        + "</small></html>"
        );

        JPanel formContent = new JPanel(
                new BorderLayout(0, 8)
        );

        formContent.add(
                capacityHint,
                BorderLayout.NORTH
        );

        formContent.add(
                form,
                BorderLayout.CENTER
        );

        JPanel content = new JPanel(
                new BorderLayout()
        );

        content.setBorder(
                BorderFactory.createEmptyBorder(
                        14, 14, 8, 14
                )
        );

        content.add(
                formContent,
                BorderLayout.CENTER
        );

        addDialogButtons(
                dialog,
                content,
                () -> saveRoom(
                        dialog,
                        editing,
                        existing,
                        name,
                        capacity,
                        floor
                )
        );

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void saveRoom(
            JDialog dialog,
            boolean editing,
            Room existing,
            JTextField name,
            JTextField capacity,
            JTextField floor
    ) {

        try {

            Room room = new Room();

            if (editing) {
                room.setRoomId(existing.getRoomId());
            }

            room.setRoomName(name.getText());

            room.setCapacity(
                    Integer.parseInt(
                            capacity.getText().trim()
                    )
            );

            room.setFloorLocation(
                    blankToNull(floor.getText())
            );

            Async.run(
                    dialog,

                    () -> {

                        if (editing) {
                            roomService.updateRoom(room);
                        } else {
                            roomService.createRoom(room);
                        }

                        return Boolean.TRUE;
                    },

                    ignored -> {

                        Dialogs.info(
                                dialog,
                                editing
                                        ? "Đã cập nhật phòng."
                                        : "Đã tạo phòng."
                        );

                        dialog.dispose();
                        roomTable.reload();
                    },

                    ex -> Dialogs.error(dialog, ex)
            );

        } catch (Exception ex) {

            Dialogs.error(dialog, ex);
        }
    }

    /**
     * Hiển thị danh sách thiết bị đang được gán vào phòng.
     */
    private void openRoomEquipment(Room room) {

        JDialog dialog = createDialog(
                "Thiết bị trong phòng: " + room.getRoomName(),
                850,
                470
        );

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
                        240
                )
                .addColumn(
                        "Trạng thái",
                        Equipment::getOperationalStatus,
                        150
                )
                .addColumn(
                        "Ngày mua",
                        Equipment::getPurchaseDate,
                        120
                )
                .setItems(List.of());

        JPanel content = new JPanel(
                new BorderLayout(0, 8)
        );

        content.setBorder(
                BorderFactory.createEmptyBorder(
                        12, 12, 12, 12
                )
        );

        content.add(
                new JLabel(
                        "Danh sách thiết bị đang được gán cho phòng "
                                + room.getRoomId()
                ),
                BorderLayout.NORTH
        );

        content.add(table, BorderLayout.CENTER);

        JButton close = button("Đóng");

        JPanel footer = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        footer.add(close);

        close.addActionListener(
                e -> dialog.dispose()
        );

        content.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(content);

        Async.run(
                dialog,

                () -> equipmentService.findByRoom(
                        room.getRoomId()
                ),

                table::setItems,

                ex -> Dialogs.error(dialog, ex)
        );

        dialog.setVisible(true);
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
        panel.add(roomTable, BorderLayout.CENTER);

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

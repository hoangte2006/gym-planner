
package com.gym.facility.ui;

import com.gym.facility.service.*;
import com.gym.facility.ui.equipment.EquipmentPanel;
import com.gym.facility.ui.maintenance.MaintenancePanel;
import com.gym.facility.ui.room.RoomPanel;
import com.gym.facility.ui.staff.StaffPanel;
import com.gym.facility.ui.trainer.TrainerProfilePanel;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.Font;
import java.util.Objects;

/**
 * Màn hình tổng quan của Module Nhân sự và Cơ sở vật chất.
 *
 * Nhiệm vụ:
 * - Khởi tạo các panel chức năng.
 * - Kết nối các panel với nhau khi cần thiết.
 * - Quản lý việc tải dữ liệu theo tab.
 *
 * Không xử lý nghiệp vụ CRUD trực tiếp tại đây.
 */
public class FacilityDashboardPanel extends JPanel {

    private final StaffPanel staffPanel;
    private final TrainerProfilePanel trainerProfilePanel;
    private final RoomPanel roomPanel;
    private final EquipmentPanel equipmentPanel;
    private final MaintenancePanel maintenancePanel;

    private final boolean[] loaded = new boolean[5];

    public FacilityDashboardPanel(
            StaffService staffService,
            RoomService roomService,
            EquipmentService equipmentService,
            MaintenanceLogService maintenanceLogService,
            TrainerProfileService trainerProfileService
    ) {

        Objects.requireNonNull(staffService);
        Objects.requireNonNull(roomService);
        Objects.requireNonNull(equipmentService);
        Objects.requireNonNull(maintenanceLogService);
        Objects.requireNonNull(trainerProfileService);

        /*
         * Khởi tạo MaintenancePanel trước EquipmentPanel
         * vì EquipmentPanel cần callback để cập nhật danh sách
         * thiết bị trong màn hình bảo trì.
         */
        maintenancePanel = new MaintenancePanel(
                equipmentService,
                staffService,
                maintenanceLogService
        );

        staffPanel = new StaffPanel(staffService);

        trainerProfilePanel = new TrainerProfilePanel(
                staffService,
                trainerProfileService
        );

        roomPanel = new RoomPanel(
                roomService,
                equipmentService
        );

        equipmentPanel = new EquipmentPanel(
                equipmentService,
                roomService,
                maintenancePanel::reloadOptions
        );

        buildUi();
    }

    private void buildUi() {

        setLayout(new BorderLayout(0, 16));

        setBorder(
                BorderFactory.createEmptyBorder(
                        22, 24, 20, 24
                )
        );

        add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Nhân sự", staffPanel);
        tabs.addTab("Hồ sơ huấn luyện viên", trainerProfilePanel);
        tabs.addTab("Phòng tập", roomPanel);
        tabs.addTab("Thiết bị", equipmentPanel);
        tabs.addTab("Phiếu bảo trì", maintenancePanel);

        add(tabs, BorderLayout.CENTER);

        /*
         * Tải dữ liệu nhân sự ngay khi mở module.
         */
        staffPanel.loadData();
        loaded[0] = true;

        /*
         * Các tab còn lại chỉ tải dữ liệu khi người dùng
         * mở tab lần đầu.
         */
        tabs.addChangeListener(event -> {

            int selectedIndex = tabs.getSelectedIndex();

            if (selectedIndex < 0 || loaded[selectedIndex]) {
                return;
            }

            loaded[selectedIndex] = true;

            switch (selectedIndex) {

                case 1 -> trainerProfilePanel.loadData();

                case 2 -> roomPanel.loadData();

                case 3 -> equipmentPanel.loadData();

                case 4 -> maintenancePanel.reloadOptions();

                default -> {
                    // Không có thao tác cần tải.
                }
            }
        });
    }

    private JPanel createHeader() {

        JLabel title = new JLabel(
                "Nhân sự & Cơ sở vật chất"
        );

        title.setFont(
                title.getFont().deriveFont(
                        Font.BOLD,
                        24f
                )
        );

        JLabel subtitle = new JLabel(
                "Quản lý nhân sự, phòng tập, thiết bị và lịch sử bảo trì."
        );

        JPanel header = new JPanel();

        header.setOpaque(false);

        header.setLayout(
                new BoxLayout(
                        header,
                        BoxLayout.Y_AXIS
                )
        );

        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);

        return header;
    }
}

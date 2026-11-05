
package com.gym.facility.ui.trainer;

import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.Dialogs;
import com.gym.facility.dto.TrainerProfile;
import com.gym.facility.service.StaffService;
import com.gym.facility.service.TrainerProfileService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;

import static com.gym.facility.ui.common.FacilityUiHelper.button;

/**
 * Giao diện xem hồ sơ và lịch tập của huấn luyện viên.
 */
public class TrainerProfilePanel extends JPanel {

    private static final int PAGE_SIZE = 10;

    private final StaffService staffService;
    private final TrainerProfileService trainerProfileService;

    private final JComboBox<Trainer> trainerCombo;

    private final JLabel totalSessionsLabel;
    private final JLabel upcomingSessionsLabel;

    private final DataTable<com.gym.core.model.Session> sessionTable;

    private final JButton viewButton;
    private final JButton refreshButton;

    public TrainerProfilePanel(
            StaffService staffService,
            TrainerProfileService trainerProfileService
    ) {

        this.staffService = Objects.requireNonNull(staffService);
        this.trainerProfileService =
                Objects.requireNonNull(trainerProfileService);

        trainerCombo = new JComboBox<>();

        totalSessionsLabel =
                new JLabel("Tổng số buổi tập: 0");

        upcomingSessionsLabel =
                new JLabel("Buổi tập sắp tới: 0");

        sessionTable =
                createSessionTable();

        viewButton = button("Xem hồ sơ");
        refreshButton = button("Làm mới");

        buildUi();
    }

    private void buildUi() {

        setLayout(new BorderLayout(0, 10));

        setBorder(
                BorderFactory.createEmptyBorder(
                        12, 2, 0, 2
                )
        );

        trainerCombo.setRenderer(
                (list, trainer, index, selected, focus) -> {

                    JLabel label = new JLabel(
                            trainer == null
                                    ? "Chọn huấn luyện viên"
                                    : trainer.getFullName()
                                    + " (#"
                                    + trainer.getStaffId()
                                    + ")"
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
                }
        );

        JPanel selectionPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 4)
        );

        selectionPanel.add(
                new JLabel("Huấn luyện viên:")
        );

        selectionPanel.add(trainerCombo);
        selectionPanel.add(viewButton);
        selectionPanel.add(refreshButton);

        JPanel statisticsPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 25, 8)
        );

        statisticsPanel.add(totalSessionsLabel);
        statisticsPanel.add(upcomingSessionsLabel);

        JPanel northPanel = new JPanel(
                new BorderLayout(0, 8)
        );

        northPanel.add(
                selectionPanel,
                BorderLayout.NORTH
        );

        northPanel.add(
                statisticsPanel,
                BorderLayout.CENTER
        );

        add(northPanel, BorderLayout.NORTH);
        add(sessionTable, BorderLayout.CENTER);

        viewButton.addActionListener(
                e -> viewTrainerProfile()
        );

        refreshButton.addActionListener(
                e -> loadData()
        );
    }

    private DataTable<com.gym.core.model.Session> createSessionTable() {

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
                        "Mã phòng",
                        com.gym.core.model.Session::getRoomId,
                        85
                )
                .addColumn(
                        "Bắt đầu",
                        com.gym.core.model.Session::getStartTime,
                        160
                )
                .addColumn(
                        "Kết thúc",
                        com.gym.core.model.Session::getEndTime,
                        160
                )
                .setItems(List.of());

        return table;
    }

    /**
     * Tải danh sách huấn luyện viên.
     *
     * Được gọi khi người dùng mở tab hồ sơ huấn luyện viên.
     */
    public void loadData() {

        Async.run(
                this,

                () -> staffService.findStaffByRole(
                                StaffRole.TRAINER
                        )
                        .stream()
                        .filter(Trainer.class::isInstance)
                        .map(Trainer.class::cast)
                        .toList(),

                this::displayTrainers,

                ex -> Dialogs.error(this, ex)
        );
    }

    private void displayTrainers(List<Trainer> trainers) {

        trainerCombo.removeAllItems();

        trainers.forEach(trainerCombo::addItem);

        if (trainers.isEmpty()) {

            clearProfile();

            Dialogs.warn(
                    this,
                    "Hiện chưa có huấn luyện viên nào."
            );

            return;
        }

        trainerCombo.setSelectedIndex(0);

        viewTrainerProfile();
    }

    /**
     * Lấy hồ sơ huấn luyện viên đang được chọn.
     */
    private void viewTrainerProfile() {

        Trainer trainer =
                (Trainer) trainerCombo.getSelectedItem();

        if (trainer == null) {

            Dialogs.warn(
                    this,
                    "Vui lòng chọn huấn luyện viên."
            );

            return;
        }

        Async.run(
                this,

                () -> trainerProfileService.getTrainerProfile(
                        trainer.getStaffId()
                ),

                this::displayProfile,

                ex -> Dialogs.error(this, ex)
        );
    }

    private void displayProfile(TrainerProfile profile) {

        totalSessionsLabel.setText(
                "Tổng số buổi tập: "
                        + profile.totalSessions()
        );

        upcomingSessionsLabel.setText(
                "Buổi tập sắp tới: "
                        + profile.upcomingSessionCount()
        );

        sessionTable.setItems(
                profile.upcomingSessions()
        );
    }

    private void clearProfile() {

        totalSessionsLabel.setText(
                "Tổng số buổi tập: 0"
        );

        upcomingSessionsLabel.setText(
                "Buổi tập sắp tới: 0"
        );

        sessionTable.setItems(List.of());
    }
}

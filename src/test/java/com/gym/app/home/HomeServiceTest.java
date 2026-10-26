package com.gym.app.home;

import com.gym.app.Session;
import com.gym.core.DBConnection;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.Manager;
import com.gym.core.model.StaffRole;
import com.gym.dashboard.dao.DashboardDao;
import com.gym.dashboard.model.EquipmentStatusCount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Luật của trang chủ: ai có danh sách việc, cách đếm thiết bị hỏng, và (cần DB) số liệu khớp với DAO. */
class HomeServiceTest {

    @AfterEach
    void logout() {
        Session.logout();       // Session là biến toàn cục: trả về chưa đăng nhập cho test khác
    }

    @Test
    void onlyManagerAndReceptionistHaveTasks() {
        assertTrue(HomeService.hasTasks(StaffRole.MANAGER));
        assertTrue(HomeService.hasTasks(StaffRole.RECEPTIONIST));
        assertFalse(HomeService.hasTasks(StaffRole.TRAINER));
    }

    @Test
    void serviceRefusesTrainersEvenIfTheScreenIsBypassed() {
        assertDoesNotThrow(() -> HomeService.requireTasksAccess(StaffRole.MANAGER));
        assertDoesNotThrow(() -> HomeService.requireTasksAccess(StaffRole.RECEPTIONIST));
        assertThrows(BusinessException.class, () -> HomeService.requireTasksAccess(StaffRole.TRAINER));
    }

    @Test
    void brokenCountPicksOnlyTheBrokenRowAndIsZeroWhenMissing() {
        List<EquipmentStatusCount> rows = List.of(
                new EquipmentStatusCount(EquipmentStatus.ACTIVE, 88),
                new EquipmentStatusCount(EquipmentStatus.UNDER_MAINTENANCE, 8),
                new EquipmentStatusCount(EquipmentStatus.BROKEN, 6));
        assertEquals(6, HomeService.brokenCount(rows));
        assertEquals(0, HomeService.brokenCount(List.of(new EquipmentStatusCount(EquipmentStatus.ACTIVE, 5))),
                "Không có dòng 'Hỏng' thì là 0, không phải lỗi");
        assertEquals(0, HomeService.brokenCount(List.of()));
    }

    @Test
    void todayTasksMatchTheDashboardNumbers() {
        boolean up;
        try {
            DBConnection.test();
            up = true;
        } catch (RuntimeException e) {
            up = false;
        }
        assumeTrue(up, "Không kết nối được DB, bỏ qua kiểm thử trang chủ.");

        Manager manager = new Manager();
        manager.setFullName("Test Quản lý");
        manager.setPhone("0900000000");
        manager.setSalary(new BigDecimal("100"));
        manager.setRole(StaffRole.MANAGER);
        Session.login(manager);

        DashboardDao dao = new DashboardDao();
        LocalDate today = LocalDate.now();
        HomeService.TodayTasks tasks = new HomeService().todayTasks();

        assertEquals(dao.countExpiringMemberships(today, 0), tasks.expiringToday());
        assertEquals(dao.countClassToday(today), tasks.classesToday());
        assertEquals(HomeService.brokenCount(dao.countEquipmentByStatus()), tasks.brokenEquipment());
    }

    @Test
    void todayTasksAreRefusedWhenLoggedOut() {
        // Chưa đăng nhập thì Session trả về vai trò Huấn luyện viên (không có danh sách việc)
        assertThrows(BusinessException.class, () -> new HomeService().todayTasks());
    }
}

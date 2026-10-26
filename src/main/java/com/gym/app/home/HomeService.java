package com.gym.app.home;

import com.gym.app.Session;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.StaffRole;
import com.gym.dashboard.dao.DashboardDao;
import com.gym.dashboard.model.EquipmentStatusCount;

import java.time.LocalDate;
import java.util.List;

/**
 * Luật nghiệp vụ của trang chủ "Việc cần làm hôm nay". Dùng lại các hàm đếm sẵn có của DashboardDao,
 * không viết thêm câu SQL nào. Quản lý và Lễ tân xem được; Huấn luyện viên chỉ thấy lời chào (xem HomePanel).
 * Không có javax.swing ở đây.
 */
public class HomeService {

    /** Ba con số của trang chủ. */
    public record TodayTasks(int expiringToday, int classesToday, int brokenEquipment) {
    }

    private final DashboardDao dao;

    public HomeService() {
        this(new DashboardDao());
    }

    public HomeService(DashboardDao dao) {
        this.dao = dao;
    }

    /** Vai trò này có danh sách việc cần làm không? (Màn hình dùng để quyết định hiện danh sách hay chỉ lời chào.) */
    public static boolean hasTasks(StaffRole role) {
        return role == StaffRole.MANAGER || role == StaffRole.RECEPTIONIST;
    }

    /** Số gói hết hạn hôm nay, số lớp học hôm nay, số thiết bị đang hỏng. */
    public TodayTasks todayTasks() {
        requireTasksAccess(Session.getRole());
        LocalDate today = LocalDate.now();
        return new TodayTasks(
                dao.countExpiringMemberships(today, 0),          // 0 ngày tới = chỉ những gói hết hạn đúng hôm nay
                dao.countClassToday(today),
                brokenCount(dao.countEquipmentByStatus()));
    }

    /** Service tự chặn khi bị gọi sai, vì việc ẩn danh sách trên màn hình chỉ là lớp ngoài. */
    static void requireTasksAccess(StaffRole role) {
        if (!hasTasks(role)) {
            throw new BusinessException("Danh sách việc cần làm chỉ dành cho Quản lý và Lễ tân.");
        }
    }

    /** Số thiết bị hỏng trong kết quả đếm theo trạng thái (0 nếu không có dòng "Hỏng"). Hàm thuần nên test dễ. */
    static int brokenCount(List<EquipmentStatusCount> rows) {
        return rows.stream()
                .filter(r -> r.status() == EquipmentStatus.BROKEN)
                .mapToInt(EquipmentStatusCount::count)
                .sum();
    }
}


package com.gym.facility.service;

import com.gym.app.Session;
import com.gym.core.Tx;
import com.gym.core.dao.*;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.BookingStatus;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.SessionEquipmentUsage;
import com.gym.core.model.StaffRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Luật nghiệp vụ thiết bị.
 *
 * Mọi thao tác giao tiếp dữ liệu thông qua DAO.
 * UI không được gọi DAO trực tiếp.
 */
public class EquipmentService {

    private final EquipmentDao equipmentDao;
    private final RoomDao roomDao;
    private final SessionDao sessionDao;
    private final SessionEquipmentUsageDao usageDao;

    /*
     * Constructor mặc định tương thích với cách khởi tạo cũ.
     */
    public EquipmentService(
            EquipmentDao equipmentDao,
            RoomDao roomDao
    ) {
        this(
                equipmentDao,
                roomDao,
                new SessionDao(),
                new SessionEquipmentUsageDao()
        );
    }

    /*
     * Constructor hỗ trợ inject đầy đủ DAO.
     */
    public EquipmentService(
            EquipmentDao equipmentDao,
            RoomDao roomDao,
            SessionDao sessionDao,
            SessionEquipmentUsageDao usageDao
    ) {
        this.equipmentDao = Objects.requireNonNull(
                equipmentDao,
                "EquipmentDao không được null"
        );

        this.roomDao = Objects.requireNonNull(
                roomDao,
                "RoomDao không được null"
        );

        this.sessionDao = Objects.requireNonNull(
                sessionDao,
                "SessionDao không được null"
        );

        this.usageDao = Objects.requireNonNull(
                usageDao,
                "SessionEquipmentUsageDao không được null"
        );
    }

    // =========================================================
    // CREATE
    // =========================================================

    /**
     * Thêm thiết bị mới.
     */
    public Equipment create(Equipment equipment) {

        requireEquipmentAccess(Session.getRole());

        validate(equipment, false);

        if (equipment.getEquipmentId() != null) {
            throw new BusinessException(
                    "Thiết bị mới không được tự gán mã thiết bị."
            );
        }

        validateRoom(equipment.getRoomId());

        if (equipment.getOperationalStatus() == null) {
            equipment.setOperationalStatus(EquipmentStatus.ACTIVE);
        }

        return equipmentDao.insert(equipment);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    /**
     * Cập nhật thông tin thiết bị.
     */
    public Equipment update(Equipment equipment) {

        requireEquipmentAccess(Session.getRole());

        validate(equipment, true);

        requirePositiveId(equipment.getEquipmentId());

        if (equipmentDao.findById(equipment.getEquipmentId()).isEmpty()) {
            throw new BusinessException(
                    "Thiết bị cần cập nhật không tồn tại."
            );
        }

        validateRoom(equipment.getRoomId());

        equipmentDao.update(equipment);

        return equipment;
    }

    // =========================================================
    // CHANGE STATUS
    // =========================================================

    /**
     * Chỉ đổi trạng thái thiết bị.
     *
     * Các trường còn lại được giữ nguyên từ bản ghi DB.
     */
    public Equipment changeStatus(
            Integer equipmentId,
            EquipmentStatus status
    ) {

        requireEquipmentAccess(Session.getRole());

        requirePositiveId(equipmentId);

        if (status == null) {
            throw new BusinessException(
                    "Tình trạng mới không được để trống."
            );
        }

        Equipment equipment = findById(equipmentId);

        equipment.setOperationalStatus(status);

        equipmentDao.update(equipment);

        return equipment;
    }

    /**
     * Đổi trạng thái thiết bị và trả về danh sách buổi tập bị ảnh hưởng.
     *
     * Khi chuyển sang UNDER_MAINTENANCE hoặc BROKEN:
     * - Tìm các lượt sử dụng thiết bị chưa kết thúc.
     * - Tìm thông tin buổi tập tương ứng.
     * - Loại bỏ buổi tập đã kết thúc.
     * - Loại bỏ buổi PT đã bị hủy.
     * - Không trả về trùng buổi tập.
     *
     * Khi chuyển về ACTIVE:
     * - Không cần tìm các buổi tập bị ảnh hưởng.
     *
     * Việc tìm lịch và cập nhật thiết bị được thực hiện trong cùng transaction.
     */
    public EquipmentStatusChangeResult changeStatusWithAffectedSessions(
            Integer equipmentId,
            EquipmentStatus newStatus
    ) {

        requireEquipmentAccess(Session.getRole());

        requirePositiveId(equipmentId);

        if (newStatus == null) {
            throw new BusinessException(
                    "Tình trạng mới không được để trống."
            );
        }

        return Tx.call(() -> {

            // 1. Tìm thiết bị trong DB.
            Equipment equipment = equipmentDao.findById(equipmentId)
                    .orElseThrow(() -> new BusinessException(
                            "Không tìm thấy thiết bị."
                    ));

            // 2. Chuẩn bị danh sách buổi tập bị ảnh hưởng.
            List<com.gym.core.model.Session> affectedSessions =
                    new ArrayList<>();

            // 3. Chỉ tìm lịch khi thiết bị không thể sử dụng.
            if (newStatus == EquipmentStatus.UNDER_MAINTENANCE
                    || newStatus == EquipmentStatus.BROKEN) {

                affectedSessions = findAffectedSessions(equipmentId);
            }

            // 4. Cập nhật trạng thái thiết bị.
            equipment.setOperationalStatus(newStatus);

            equipmentDao.update(equipment);

            // 5. Trả kết quả cho UI.
            return new EquipmentStatusChangeResult(
                    equipment,
                    affectedSessions
            );
        });
    }

    // =========================================================
    // FIND
    // =========================================================

    /**
     * Tìm thiết bị theo ID.
     */
    public Equipment findById(Integer equipmentId) {

        requireEquipmentAccess(Session.getRole());

        requirePositiveId(equipmentId);

        return equipmentDao.findById(equipmentId)
                .orElseThrow(() -> new BusinessException(
                        "Không tìm thấy thiết bị."
                ));
    }

    /**
     * Lấy toàn bộ thiết bị.
     */
    public List<Equipment> findAll() {

        requireEquipmentAccess(Session.getRole());

        return equipmentDao.findAll();
    }

    /**
     * Lấy danh sách thiết bị theo phân trang.
     */
    public PageResult<Equipment> findPage(
            int page,
            int pageSize
    ) {

        requireEquipmentAccess(Session.getRole());

        return equipmentDao.findPage(page, pageSize);
    }

    /**
     * Tìm thiết bị theo trạng thái.
     */
    public List<Equipment> findByStatus(EquipmentStatus status) {

        requireEquipmentAccess(Session.getRole());

        if (status == null) {
            throw new BusinessException(
                    "Tình trạng lọc không được để trống."
            );
        }

        return equipmentDao.findByStatus(status);
    }

    /**
     * Tìm thiết bị theo phòng.
     */
    public List<Equipment> findByRoom(Integer roomId) {

        requireEquipmentAccess(Session.getRole());

        requirePositiveId(roomId);

        if (roomDao.findById(roomId).isEmpty()) {
            throw new BusinessException(
                    "Phòng được chọn không tồn tại."
            );
        }

        return equipmentDao.findByRoom(roomId);
    }

    /**
     * Tìm thiết bị theo từ khóa.
     */
    public List<Equipment> searchEquipment(String keyword) {

        requireEquipmentAccess(Session.getRole());

        if (keyword == null || keyword.isBlank()) {
            return equipmentDao.findAll();
        }

        return equipmentDao.search(keyword.trim());
    }

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Xóa thiết bị.
     *
     * UI cần xác nhận trước khi gọi method này.
     */
    public void delete(Integer equipmentId) {

        requireEquipmentAccess(Session.getRole());

        requirePositiveId(equipmentId);

        if (equipmentDao.findById(equipmentId).isEmpty()) {
            throw new BusinessException(
                    "Thiết bị không tồn tại hoặc đã bị xóa."
            );
        }

        /*
         * Schema ON DELETE CASCADE sẽ xử lý các bản ghi
         * liên quan theo cấu hình database.
         */
        equipmentDao.deleteById(equipmentId);
    }

    // =========================================================
    // FIND AFFECTED SESSIONS
    // =========================================================

    /**
     * Tìm các buổi tập đang hoặc sẽ sử dụng thiết bị.
     *
     * Không sửa DAO:
     * - usageDao.findByEquipment() lấy các lượt sử dụng thiết bị.
     * - sessionDao.findById() lấy thông tin buổi tập.
     *
     * Các trường hợp bị loại bỏ:
     * - Lượt sử dụng đã kết thúc.
     * - Buổi tập đã kết thúc.
     * - Buổi PT đã bị hủy.
     *
     * LinkedHashMap giúp loại bỏ các buổi tập trùng nhau
     * và giữ thứ tự xuất hiện.
     */
    private List<com.gym.core.model.Session> findAffectedSessions(
            Integer equipmentId
    ) {

        LocalDateTime now = LocalDateTime.now();

        // 1. Lấy các lượt sử dụng thiết bị.
        List<SessionEquipmentUsage> usages =
                usageDao.findByEquipment(equipmentId);

        // 2. Lưu các buổi tập không trùng Session_ID.
        Map<Integer, com.gym.core.model.Session> result =
                new LinkedHashMap<>();

        // 3. Duyệt từng lượt sử dụng thiết bị.
        for (SessionEquipmentUsage usage : usages) {

            if (usage == null || usage.getSessionId() == null) {
                continue;
            }

            // Lượt sử dụng đã kết thúc hoặc không có thời gian kết thúc.
            if (usage.getReservedTo() == null
                    || !usage.getReservedTo().isAfter(now)) {
                continue;
            }

            // 4. Tìm thông tin buổi tập.
            sessionDao.findById(usage.getSessionId())
                    .ifPresent(session -> {

                        // Buổi tập đã kết thúc.
                        if (session.getEndTime() == null
                                || !session.getEndTime().isAfter(now)) {
                            return;
                        }

                        // Buổi PT đã bị hủy.
                        if (session instanceof PtSession ptSession
                                && ptSession.getBookingStatus()
                                == BookingStatus.CANCELLED) {
                            return;
                        }

                        // Thêm vào Map, tự loại bỏ Session_ID trùng.
                        result.put(
                                session.getSessionId(),
                                session
                        );
                    });
        }

        return new ArrayList<>(result.values());
    }

    /**
     * Lấy danh sách thiết bị cần cảnh báo sức khỏe.
     */
    public PageResult<Map<String, Object>> findHealthWarningsPage(
            int page,
            int pageSize
    ) {
        requireEquipmentAccess(Session.getRole());

        if (page < 1 || pageSize < 1) {
            throw new BusinessException(
                    "Thông tin phân trang không hợp lệ."
            );
        }

        return equipmentDao.findHealthWarningsPage(page, pageSize);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    /**
     * Kiểm tra dữ liệu thiết bị.
     */
    private void validate(
            Equipment equipment,
            boolean updating
    ) {

        requireEquipmentAccess(Session.getRole());

        if (equipment == null) {
            throw new BusinessException(
                    "Thông tin thiết bị không được để trống."
            );
        }

        // Kiểm tra tên thiết bị.
        if (equipment.getEquipmentName() == null
                || equipment.getEquipmentName().isBlank()) {

            throw new BusinessException(
                    "Tên thiết bị không được để trống."
            );
        }

        equipment.setEquipmentName(
                equipment.getEquipmentName().trim()
        );

        if (equipment.getEquipmentName().length() > 100) {
            throw new BusinessException(
                    "Tên thiết bị không được vượt quá 100 ký tự."
            );
        }

        // Khi cập nhật, trạng thái không được null.
        if (updating && equipment.getOperationalStatus() == null) {
            throw new BusinessException(
                    "Tình trạng thiết bị không được để trống."
            );
        }

        // Kiểm tra mã phòng.
        if (equipment.getRoomId() != null
                && equipment.getRoomId() <= 0) {

            throw new BusinessException(
                    "Mã phòng không hợp lệ."
            );
        }

        // Kiểm tra ngày mua.
        LocalDate purchaseDate = equipment.getPurchaseDate();

        if (purchaseDate != null
                && purchaseDate.isAfter(LocalDate.now())) {

            throw new BusinessException(
                    "Ngày mua không được ở tương lai."
            );
        }
    }

    /**
     * Kiểm tra phòng được gán cho thiết bị.
     */
    private void validateRoom(Integer roomId) {

        if (roomId != null && roomDao.findById(roomId).isEmpty()) {
            throw new BusinessException(
                    "Phòng được gán cho thiết bị không tồn tại."
            );
        }
    }

    /**
     * Kiểm tra ID hợp lệ.
     */
    private static void requirePositiveId(Integer id) {

        if (id == null || id <= 0) {
            throw new BusinessException(
                    "Mã thiết bị / phòng không hợp lệ."
            );
        }
    }

    /**
     * Chỉ MANAGER được phép sử dụng nghiệp vụ thiết bị.
     */
    public static void requireEquipmentAccess(StaffRole role) {

        if (role != StaffRole.MANAGER) {
            throw new BusinessException(
                    "Chỉ Quản lý mới dùng được chức năng này."
            );
        }
    }
}

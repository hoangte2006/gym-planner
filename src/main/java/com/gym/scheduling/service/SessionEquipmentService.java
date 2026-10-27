package com.gym.scheduling.service;

import com.gym.core.dao.SessionEquipmentUsageDao;
import com.gym.core.model.BookingStatus;
import com.gym.core.model.Equipment;
import com.gym.core.model.PtSession;
import com.gym.core.model.Session;
import com.gym.core.model.SessionEquipmentUsage;
import com.gym.scheduling.dao.SessionDao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Equipment reservations tied to a session's room and time range. */
public class SessionEquipmentService {

    private final SessionDao sessionDao;
    private final SessionEquipmentUsageDao usageDao;

    public SessionEquipmentService() {
        this(new SessionDao(), new SessionEquipmentUsageDao());
    }

    public SessionEquipmentService(SessionDao sessionDao, SessionEquipmentUsageDao usageDao) {
        this.sessionDao = Objects.requireNonNull(sessionDao);
        this.usageDao = Objects.requireNonNull(usageDao);
    }

    public List<Equipment> findAvailableEquipment(int sessionId) {
        SchedulingAuthorization.requireSchedulingAccess();
        Session session = requireSession(sessionId);
        ensureReservable(session);
        return sessionDao.findAvailableEquipment(
                session.getRoomId(), session.getStartTime(), session.getEndTime(), null);
    }

    public SessionEquipmentUsage reserveEquipment(
            int sessionId,
            int equipmentId,
            LocalDateTime from,
            LocalDateTime to) {
        SchedulingAuthorization.requireManagementAccess();
        Session session = requireSession(sessionId);
        ensureReservable(session);
        if (from == null || to == null
                || from.isBefore(session.getStartTime())
                || to.isAfter(session.getEndTime())
                || !to.isAfter(from)
                || !from.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Thời gian đặt thiết bị phải nằm trong thời gian buổi tập.");
        }
        return sessionDao.reserveEquipment(equipmentId, sessionId, from, to);
    }

    public void cancelReservation(int usageId) {
        SchedulingAuthorization.requireManagementAccess();
        SessionEquipmentUsage usage = usageDao.findById(usageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt đặt thiết bị."));
        Session session = requireSession(usage.getSessionId());
        if (session instanceof PtSession pt
                && pt.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Không thể sửa thiết bị của buổi PT đã hoàn thành.");
        }
        usageDao.deleteById(usageId);
    }

    private Session requireSession(int sessionId) {
        Session session = sessionDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi tập."));
        if (session.getRoomId() == null || session.getStartTime() == null
                || session.getEndTime() == null) {
            throw new IllegalStateException("Buổi tập thiếu phòng hoặc khung giờ hợp lệ.");
        }
        return session;
    }

    private static void ensureReservable(Session session) {
        if (session instanceof PtSession pt
                && pt.getBookingStatus() != BookingStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Chỉ có thể đặt thiết bị cho buổi PT đang ở trạng thái Đã đặt lịch.");
        }
    }
}

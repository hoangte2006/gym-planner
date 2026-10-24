package com.gym.scheduling.service;

import com.gym.core.model.BookingStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.Session;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Finds available 30-minute start times for a room and trainer. */
public class AvailabilityService {

    private final SessionService sessionService;

    public AvailabilityService() {
        this(new SessionService());
    }

    public AvailabilityService(SessionService sessionService) {
        this.sessionService = Objects.requireNonNull(sessionService);
    }

    public List<LocalDateTime> findAvailableStarts(
            Integer roomId,
            Integer trainerId,
            LocalDate date,
            Duration duration) {

        if (roomId == null || trainerId == null || date == null
                || duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Vui lòng chọn phòng, huấn luyện viên, ngày và thời lượng hợp lệ.");
        }

        LocalDateTime dayStart = date.atTime(6, 0);
        LocalDateTime dayEnd = date.atTime(22, 0);
        if (date.isBefore(LocalDate.now())) {
            return List.of();
        }

        List<Session> sessions = sessionService.findWeeklySchedule(date);
        List<FreeSlotFinder.Interval> busy = new ArrayList<>();

        for (Session session : sessions) {
            if (session.getStartTime() == null || session.getEndTime() == null
                    || isCancelled(session)) {
                continue;
            }

            boolean resourceMatches = Objects.equals(roomId, session.getRoomId())
                    || Objects.equals(trainerId, session.getTrainerId());
            boolean overlapsDay = session.getStartTime().isBefore(dayEnd)
                    && session.getEndTime().isAfter(dayStart);
            if (resourceMatches && overlapsDay) {
                LocalDateTime start = session.getStartTime().isBefore(dayStart)
                        ? dayStart : session.getStartTime();
                LocalDateTime end = session.getEndTime().isAfter(dayEnd)
                        ? dayEnd : session.getEndTime();
                busy.add(new FreeSlotFinder.Interval(start, end));
            }
        }

        if (date.equals(LocalDate.now())) {
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
            int nextHalfHour = (now.getMinute() / 30 + 1) * 30;
            LocalDateTime firstFutureSlot = now.withMinute(0)
                    .plusMinutes(nextHalfHour);
            if (firstFutureSlot.isAfter(dayEnd)) {
                return List.of();
            }
            if (firstFutureSlot.isAfter(dayStart)) {
                busy.add(new FreeSlotFinder.Interval(dayStart, firstFutureSlot));
            }
        }

        List<LocalDateTime> available = new ArrayList<>();
        for (FreeSlotFinder.Interval free :
                FreeSlotFinder.findFreeSlots(dayStart, dayEnd, busy)) {
            for (LocalDateTime start = free.start();
                    !start.plus(duration).isAfter(free.end());
                    start = start.plusMinutes(30)) {
                available.add(start);
            }
        }
        return List.copyOf(available);
    }

    private static boolean isCancelled(Session session) {
        return session instanceof PtSession pt
                && pt.getBookingStatus() == BookingStatus.CANCELLED;
    }
}

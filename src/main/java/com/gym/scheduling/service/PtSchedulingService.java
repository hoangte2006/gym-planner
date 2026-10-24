package com.gym.scheduling.service;

import com.gym.core.dao.MemberDao;
import com.gym.core.dao.PtSessionDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.model.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class PtSchedulingService {

    private final PtSessionDao ptDao;
    private final MemberDao memberDao;
    private final RoomDao roomDao;
    private final TrainerDao trainerDao;
    private final SessionService sessionService;
    private final AvailabilityService availabilityService;

    public PtSchedulingService() {
        this(
                new PtSessionDao(),
                new MemberDao(),
                new RoomDao(),
                new TrainerDao(),
                new SessionService()
        );
    }

    public PtSchedulingService(
            PtSessionDao ptDao,
            MemberDao memberDao,
            RoomDao roomDao,
            TrainerDao trainerDao,
            SessionService sessionService) {

        this.ptDao = Objects.requireNonNull(ptDao);
        this.memberDao = Objects.requireNonNull(memberDao);
        this.roomDao = Objects.requireNonNull(roomDao);
        this.trainerDao = Objects.requireNonNull(trainerDao);
        this.sessionService = Objects.requireNonNull(sessionService);
        this.availabilityService = new AvailabilityService(sessionService);
    }

    public List<Member> searchMembers(String keyword) {
        return memberDao.search(keyword, 1, 20).items();
    }

    public List<Room> findRooms() {
        return roomDao.findAll();
    }

    public List<Trainer> findTrainers() {
        return trainerDao.findAll();
    }

    public List<LocalDateTime> findAvailableStarts(
            Integer roomId,
            Integer trainerId,
            LocalDate date,
            Duration duration) {
        return availabilityService.findAvailableStarts(
                roomId, trainerId, date, duration);
    }

    public PtSession bookSession(
            Integer memberId,
            Integer roomId,
            Integer trainerId,
            LocalDateTime start,
            LocalDateTime end) {

        if (memberId == null || roomId == null || trainerId == null) {
            throw new IllegalArgumentException("Vui lòng chọn hội viên, phòng và HLV.");
        }

        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ.");
        }
        if (!start.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Không thể đặt lịch PT trong quá khứ.");
        }

        Member member = memberDao.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội viên."));
        if (!member.hasPhoto()) {
            throw new IllegalArgumentException(
                    "Không thể đặt lịch PT: hội viên chưa có ảnh hồ sơ. "
                    + "Vui lòng cập nhật ảnh trước khi tiếp tục."
            );
        }

        Room room = roomDao.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng."));

        Trainer trainer = trainerDao.findById(trainerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy HLV."));

        PtSession session = new PtSession();
        session.setMemberId(memberId);
        session.setRoomId(room.getRoomId());
        session.setTrainerId(trainer.getStaffId());
        session.setStartTime(start);
        session.setEndTime(end);
        session.setBookingStatus(BookingStatus.SCHEDULED);

        sessionService.findRoomConflict(session)
                .ifPresent(s -> {
                    throw new IllegalStateException("Phòng đã có lịch.");
                });

        sessionService.findTrainerConflict(session)
                .ifPresent(s -> {
                    throw new IllegalStateException("HLV đã có lịch.");
                });

        return ptDao.insert(session);
    }
}
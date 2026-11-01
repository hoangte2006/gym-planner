package com.gym.scheduling.dao;

import com.gym.core.dao.SessionDao;
import com.gym.core.dao.Sql;
import com.gym.core.model.Session;

import java.time.LocalDateTime;
import java.util.List;

/** Scheduling-specific queries for detecting overlapping sessions. */
public class SchedulingSessionDao extends SessionDao {

    /**
     * Finds sessions that overlap the requested interval and share its room or trainer.
     * Cancelled PT bookings are ignored.
     */
    public List<Session> findPotentialConflicts(
            Integer roomId,
            Integer trainerId,
            LocalDateTime start,
            LocalDateTime end) {
        String sql = selectSql()
                + " WHERE (s.Room_ID = ? OR s.Trainer_ID = ?)"
                + " AND s.Start_Time < ?"
                + " AND s.End_Time > ?"
                + " AND (s.Session_Type <> 'PT' OR p.Booking_Status IS NULL"
                + " OR p.Booking_Status <> 'Cancelled')"
                + " ORDER BY s.Start_Time";

        return queryList(sql, ps -> {
            ps.setObject(1, roomId);
            ps.setObject(2, trainerId);
            Sql.setDateTime(ps, 3, end);
            Sql.setDateTime(ps, 4, start);
        });
    }

    public List<Session> findRoomConflicts(
            Integer roomId,
            LocalDateTime start,
            LocalDateTime end) {
        return findRoomConflicts(roomId, start, end, null);
    }

    public List<Session> findRoomConflicts(
            Integer roomId,
            LocalDateTime start,
            LocalDateTime end,
            Integer exceptSessionId) {
        String exclude = exceptSessionId == null ? "" : " AND s.Session_ID <> ?";
        String sql = selectSql()
                + " WHERE s.Room_ID = ?"
                + " AND s.Start_Time < ?"
                + " AND s.End_Time > ?"
                + exclude
                + " AND (s.Session_Type <> 'PT'"
                + " OR p.Booking_Status IS NULL"
                + " OR p.Booking_Status <> 'Cancelled')"
                + " ORDER BY s.Start_Time";

        return queryList(sql, ps -> {
            ps.setObject(1, roomId);
            Sql.setDateTime(ps, 2, end);
            Sql.setDateTime(ps, 3, start);
            if (exceptSessionId != null) {
                ps.setInt(4, exceptSessionId);
            }
        });
    }

    public List<Session> findTrainerConflicts(
            Integer trainerId,
            LocalDateTime start,
            LocalDateTime end) {
        return findTrainerConflicts(trainerId, start, end, null);
    }

    public List<Session> findTrainerConflicts(
            Integer trainerId,
            LocalDateTime start,
            LocalDateTime end,
            Integer exceptSessionId) {
        String exclude = exceptSessionId == null ? "" : " AND s.Session_ID <> ?";
        String sql = selectSql()
                + " WHERE s.Trainer_ID = ?"
                + " AND s.Start_Time < ?"
                + " AND s.End_Time > ?"
                + exclude
                + " AND (s.Session_Type <> 'PT'"
                + " OR p.Booking_Status IS NULL"
                + " OR p.Booking_Status <> 'Cancelled')"
                + " ORDER BY s.Start_Time";

        return queryList(sql, ps -> {
            ps.setObject(1, trainerId);
            Sql.setDateTime(ps, 2, end);
            Sql.setDateTime(ps, 3, start);
            if (exceptSessionId != null) {
                ps.setInt(4, exceptSessionId);
            }
        });
    }
}

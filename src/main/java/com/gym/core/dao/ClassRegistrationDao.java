package com.gym.core.dao;

import com.gym.core.model.AttendanceStatus;
import com.gym.core.model.ClassRegistration;
import com.gym.core.model.ClassRegistrationKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO bảng CLASS_REGISTRATION (đăng ký lớp). Khóa ghép (Session_ID, Visitor_ID)
 * nên findById và deleteById nhận ClassRegistrationKey.
 */
public class ClassRegistrationDao extends AbstractJdbcDao<ClassRegistration, ClassRegistrationKey> {

    @Override
    protected String tableName() {
        return "CLASS_REGISTRATION";
    }

    @Override
    protected String idColumn() {
        return "Session_ID, Visitor_ID";   // dùng để sắp xếp
    }

    @Override
    protected ClassRegistration mapRow(ResultSet rs) throws SQLException {
        ClassRegistration r = new ClassRegistration();
        r.setSessionId(rs.getInt("Session_ID"));
        r.setVisitorId(rs.getInt("Visitor_ID"));
        r.setRegistrationDate(Sql.dateTime(rs, "Registration_Date"));
        r.setAttendanceStatus(AttendanceStatus.fromDb(rs.getString("Attendance_Status")));
        return r;
    }

    @Override
    public Optional<ClassRegistration> findById(ClassRegistrationKey key) {
        return queryOne(selectSql() + " WHERE Session_ID = ? AND Visitor_ID = ?", ps -> {
            ps.setInt(1, key.sessionId());
            ps.setInt(2, key.visitorId());
        });
    }

    @Override
    public void deleteById(ClassRegistrationKey key) {
        execute("DELETE FROM CLASS_REGISTRATION WHERE Session_ID = ? AND Visitor_ID = ?", ps -> {
            ps.setInt(1, key.sessionId());
            ps.setInt(2, key.visitorId());
        });
    }

    @Override
    public ClassRegistration insert(ClassRegistration r) {
        execute("INSERT INTO CLASS_REGISTRATION (Session_ID, Visitor_ID, Registration_Date, Attendance_Status) "
                        + "VALUES (?, ?, COALESCE(?, CURRENT_TIMESTAMP), COALESCE(?, 'Confirmed'))",
                ps -> {
                    ps.setInt(1, r.getSessionId());
                    ps.setInt(2, r.getVisitorId());
                    Sql.setDateTime(ps, 3, r.getRegistrationDate());
                    Sql.setEnum(ps, 4, r.getAttendanceStatus());
                });
        // Nhận lại giá trị mặc định do DB điền (ngày đăng ký, trạng thái)
        findById(new ClassRegistrationKey(r.getSessionId(), r.getVisitorId())).ifPresent(saved -> {
            r.setRegistrationDate(saved.getRegistrationDate());
            r.setAttendanceStatus(saved.getAttendanceStatus());
        });
        return r;
    }

    @Override
    public void update(ClassRegistration r) {
        execute("UPDATE CLASS_REGISTRATION SET Registration_Date = ?, Attendance_Status = ? "
                        + "WHERE Session_ID = ? AND Visitor_ID = ?",
                ps -> {
                    Sql.setDateTime(ps, 1, r.getRegistrationDate());
                    Sql.setEnum(ps, 2, r.getAttendanceStatus());
                    ps.setInt(3, r.getSessionId());
                    ps.setInt(4, r.getVisitorId());
                });
    }

    public List<ClassRegistration> findBySession(int sessionId) {
        return queryList(selectSql() + " WHERE Session_ID = ? ORDER BY Registration_Date",
                ps -> ps.setInt(1, sessionId));
    }

    public List<ClassRegistration> findByVisitor(int visitorId) {
        return queryList(selectSql() + " WHERE Visitor_ID = ? ORDER BY Registration_Date DESC",
                ps -> ps.setInt(1, visitorId));
    }
}

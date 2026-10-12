package com.gym.core.dao;

import com.gym.core.model.ClassSession;
import com.gym.core.model.SessionType;

import java.sql.ResultSet;
import java.sql.SQLException;

/** DAO cho ClassSession (lớp tập nhóm): nằm ở hai bảng SESSION và CLASS. Loại buổi luôn là Class. */
public class ClassSessionDao extends AbstractJdbcDao<ClassSession, Integer> {

    static final String FROM = "[SESSION] s JOIN CLASS c ON c.Session_ID = s.Session_ID";
    static final String COLUMNS = "s.Session_ID, s.Session_Type, s.Room_ID, s.Trainer_ID, s.Start_Time, s.End_Time, "
            + "c.Class_Name, c.Max_Capacity";

    @Override
    protected String tableName() {
        return "CLASS";
    }

    @Override
    protected String idColumn() {
        return "s.Session_ID";
    }

    @Override
    protected String fromClause() {
        return FROM;
    }

    @Override
    protected String selectColumns() {
        return COLUMNS;
    }

    @Override
    protected ClassSession mapRow(ResultSet rs) throws SQLException {
        return map(rs);
    }

    /** Dùng chung với SessionDao (cùng tên cột). Cột con có thể NULL khi đọc qua LEFT JOIN. */
    static ClassSession map(ResultSet rs) throws SQLException {
        ClassSession c = new ClassSession();
        SessionDao.fillBase(c, rs);
        String name = rs.getString("Class_Name");
        if (name != null) {
            c.setClassName(name);
        }
        Integer max = Sql.integer(rs, "Max_Capacity");
        if (max != null) {
            c.setMaxCapacity(max);
        }
        return c;
    }

    @Override
    public ClassSession insert(ClassSession c) {
        int id = inTransaction(cn -> {
            int newId = SessionDao.insertSessionRow(cn, c, SessionType.CLASS);
            exec(cn, "INSERT INTO CLASS (Session_ID, Class_Name, Max_Capacity) "
                    + "VALUES (?, ?, COALESCE(NULLIF(?, 0), 20))", ps -> {
                ps.setInt(1, newId);
                ps.setString(2, c.getClassName());
                ps.setInt(3, c.getMaxCapacity());
            });
            return newId;
        });
        c.setSessionId(id);
        c.setSessionType(SessionType.CLASS);
        findById(id).ifPresent(saved -> c.setMaxCapacity(saved.getMaxCapacity()));   // sức chứa mặc định của DB
        return c;
    }

    @Override
    public void update(ClassSession c) {
        inTransaction(cn -> {
            SessionDao.updateSessionRow(cn, c);
            exec(cn, "UPDATE CLASS SET Class_Name = ?, Max_Capacity = COALESCE(NULLIF(?, 0), Max_Capacity) "
                    + "WHERE Session_ID = ?", ps -> {
                ps.setString(1, c.getClassName());
                ps.setInt(2, c.getMaxCapacity());
                ps.setInt(3, c.getSessionId());
            });
            return null;
        });
    }

    /** Xóa buổi (bảng SESSION); lớp và các đăng ký bị xóa theo nhờ ON DELETE CASCADE. */
    @Override
    public void deleteById(Integer id) {
        execute("DELETE FROM [SESSION] WHERE Session_ID = ?", ps -> ps.setInt(1, id));
    }
}

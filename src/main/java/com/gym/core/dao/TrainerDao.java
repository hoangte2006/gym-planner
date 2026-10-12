package com.gym.core.dao;

import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** DAO cho Trainer (huấn luyện viên): nằm ở hai bảng STAFF và TRAINER. Vai trò luôn là Trainer. */
public class TrainerDao extends AbstractJdbcDao<Trainer, Integer> {

    static final String FROM = "STAFF s JOIN TRAINER t ON t.Staff_ID = s.Staff_ID";
    static final String COLUMNS = "s.Staff_ID, s.Full_Name, s.Phone, s.Role, s.Hire_Date, s.Salary, "
            + "t.Specialty, t.Certification";

    @Override
    protected String tableName() {
        return "TRAINER";
    }

    @Override
    protected String idColumn() {
        return "s.Staff_ID";
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
    protected Trainer mapRow(ResultSet rs) throws SQLException {
        return map(rs);
    }

    /** Dùng chung với StaffDao (cùng tên cột). Cột con có thể NULL khi đọc qua LEFT JOIN. */
    static Trainer map(ResultSet rs) throws SQLException {
        Trainer t = new Trainer();
        StaffDao.fillBase(t, rs);
        String specialty = rs.getString("Specialty");
        if (specialty != null) {
            t.setSpecialty(specialty);
        }
        t.setCertification(rs.getString("Certification"));
        return t;
    }

    @Override
    public Trainer insert(Trainer t) {
        int id = inTransaction(cn -> {
            int newId = insertKey(cn, "INSERT INTO STAFF (Full_Name, Phone, Role, Hire_Date, Salary) "
                    + "VALUES (?, ?, 'Trainer', COALESCE(?, CAST(GETDATE() AS DATE)), COALESCE(?, 500.00))", ps -> {
                ps.setString(1, t.getFullName());
                ps.setString(2, t.getPhone());
                Sql.setDate(ps, 3, t.getHireDate());
                Sql.setDecimal(ps, 4, t.getSalary());
            });
            exec(cn, "INSERT INTO TRAINER (Staff_ID, Specialty, Certification) "
                    + "VALUES (?, COALESCE(?, 'General'), ?)", ps -> {
                ps.setInt(1, newId);
                ps.setString(2, t.getSpecialty());
                ps.setString(3, t.getCertification());
            });
            return newId;
        });
        t.setStaffId(id);
        t.setRole(StaffRole.TRAINER);
        findById(id).ifPresent(saved -> {   // nhận lại giá trị mặc định do DB điền
            t.setHireDate(saved.getHireDate());
            t.setSalary(saved.getSalary());
            t.setSpecialty(saved.getSpecialty());
        });
        return t;
    }

    @Override
    public void update(Trainer t) {
        inTransaction(cn -> {
            StaffDao.updateStaffRow(cn, t);
            exec(cn, "UPDATE TRAINER SET Specialty = COALESCE(?, Specialty), Certification = ? WHERE Staff_ID = ?",
                    ps -> {
                        ps.setString(1, t.getSpecialty());
                        ps.setString(2, t.getCertification());
                        ps.setInt(3, t.getStaffId());
                    });
            return null;
        });
    }

    /** Xóa nhân viên (bảng STAFF); dòng TRAINER bị xóa theo nhờ ON DELETE CASCADE. */
    @Override
    public void deleteById(Integer id) {
        execute("DELETE FROM STAFF WHERE Staff_ID = ?", ps -> ps.setInt(1, id));
    }

    /** Huấn luyện viên theo chuyên môn. */
    public List<Trainer> findBySpecialty(String specialty) {
        return queryList(selectSql() + " WHERE t.Specialty = ? ORDER BY s.Staff_ID", ps -> ps.setString(1, specialty));
    }
}

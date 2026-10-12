package com.gym.core.dao;

import com.gym.core.model.Receptionist;
import com.gym.core.model.ShiftType;
import com.gym.core.model.StaffRole;

import java.sql.ResultSet;
import java.sql.SQLException;

/** DAO cho Receptionist (lễ tân): nằm ở hai bảng STAFF và RECEPTIONIST. Vai trò luôn là Receptionist. */
public class ReceptionistDao extends AbstractJdbcDao<Receptionist, Integer> {

    static final String FROM = "STAFF s JOIN RECEPTIONIST r ON r.Staff_ID = s.Staff_ID";
    static final String COLUMNS = "s.Staff_ID, s.Full_Name, s.Phone, s.Role, s.Hire_Date, s.Salary, r.Shift_Type";

    @Override
    protected String tableName() {
        return "RECEPTIONIST";
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
    protected Receptionist mapRow(ResultSet rs) throws SQLException {
        return map(rs);
    }

    /** Dùng chung với StaffDao (cùng tên cột). */
    static Receptionist map(ResultSet rs) throws SQLException {
        Receptionist r = new Receptionist();
        StaffDao.fillBase(r, rs);
        String shift = rs.getString("Shift_Type");
        if (shift != null) {
            r.setShiftType(ShiftType.fromDb(shift));
        }
        return r;
    }

    @Override
    public Receptionist insert(Receptionist r) {
        int id = inTransaction(cn -> {
            int newId = insertKey(cn, "INSERT INTO STAFF (Full_Name, Phone, Role, Hire_Date, Salary) "
                    + "VALUES (?, ?, 'Receptionist', COALESCE(?, CAST(GETDATE() AS DATE)), COALESCE(?, 500.00))", ps -> {
                ps.setString(1, r.getFullName());
                ps.setString(2, r.getPhone());
                Sql.setDate(ps, 3, r.getHireDate());
                Sql.setDecimal(ps, 4, r.getSalary());
            });
            exec(cn, "INSERT INTO RECEPTIONIST (Staff_ID, Shift_Type) VALUES (?, COALESCE(?, 'Morning'))", ps -> {
                ps.setInt(1, newId);
                Sql.setEnum(ps, 2, r.getShiftType());
            });
            return newId;
        });
        r.setStaffId(id);
        r.setRole(StaffRole.RECEPTIONIST);
        findById(id).ifPresent(saved -> {   // nhận lại giá trị mặc định do DB điền
            r.setHireDate(saved.getHireDate());
            r.setSalary(saved.getSalary());
            r.setShiftType(saved.getShiftType());
        });
        return r;
    }

    @Override
    public void update(Receptionist r) {
        inTransaction(cn -> {
            StaffDao.updateStaffRow(cn, r);
            exec(cn, "UPDATE RECEPTIONIST SET Shift_Type = COALESCE(?, Shift_Type) WHERE Staff_ID = ?", ps -> {
                Sql.setEnum(ps, 1, r.getShiftType());
                ps.setInt(2, r.getStaffId());
            });
            return null;
        });
    }

    /** Xóa nhân viên (bảng STAFF); dòng RECEPTIONIST bị xóa theo nhờ ON DELETE CASCADE. */
    @Override
    public void deleteById(Integer id) {
        execute("DELETE FROM STAFF WHERE Staff_ID = ?", ps -> ps.setInt(1, id));
    }
}

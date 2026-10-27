package com.gym.core.dao;

import com.gym.core.model.Manager;
import com.gym.core.model.Receptionist;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * DAO cho Staff (lớp cha): đọc ra đúng kiểu con theo cột Role (Trainer / Receptionist / Manager);
 * thêm/sửa giao cho DAO của kiểu con. Manager không có bảng con, chỉ ghi vào STAFF.
 */
public class StaffDao extends AbstractJdbcDao<Staff, Integer> {

    private final TrainerDao trainerDao = new TrainerDao();
    private final ReceptionistDao receptionistDao = new ReceptionistDao();

    @Override
    protected String tableName() {
        return "STAFF";
    }

    @Override
    protected String idColumn() {
        return "s.Staff_ID";
    }

    @Override
    protected String fromClause() {
        return "STAFF s "
                + "LEFT JOIN TRAINER t ON t.Staff_ID = s.Staff_ID "
                + "LEFT JOIN RECEPTIONIST r ON r.Staff_ID = s.Staff_ID";
    }

    @Override
    protected String selectColumns() {
        return "s.Staff_ID, s.Full_Name, s.Phone, s.Role, s.Hire_Date, s.Salary, "
                + "t.Specialty, t.Certification, r.Shift_Type";
    }

    @Override
    protected Staff mapRow(ResultSet rs) throws SQLException {
        switch (StaffRole.fromDb(rs.getString("Role"))) {
            case TRAINER:
                return TrainerDao.map(rs);
            case RECEPTIONIST:
                return ReceptionistDao.map(rs);
            default:
                Manager m = new Manager();
                fillBase(m, rs);
                return m;
        }
    }

    /** Điền các cột chung của bảng STAFF vào một đối tượng Staff. */
    static void fillBase(Staff s, ResultSet rs) throws SQLException {
        s.setStaffId(rs.getInt("Staff_ID"));
        s.setFullName(rs.getString("Full_Name"));
        s.setPhone(rs.getString("Phone"));
        s.setRole(StaffRole.fromDb(rs.getString("Role")));
        s.setHireDate(Sql.date(rs, "Hire_Date"));
        s.setSalary(rs.getBigDecimal("Salary"));
    }

    /** UPDATE bảng STAFF (không đổi Role). Dùng trong transaction của DAO con. */
    static void updateStaffRow(Connection cn, Staff s) throws SQLException {
        exec(cn, "UPDATE STAFF SET Full_Name = ?, Phone = ?, Hire_Date = COALESCE(?, Hire_Date), "
                + "Salary = COALESCE(?, Salary) WHERE Staff_ID = ?", ps -> {
            ps.setString(1, s.getFullName());
            ps.setString(2, s.getPhone());
            Sql.setDate(ps, 3, s.getHireDate());
            Sql.setDecimal(ps, 4, s.getSalary());
            ps.setInt(5, s.getStaffId());
        });
    }

    @Override
    public Staff insert(Staff s) {
        if (s instanceof Trainer t) {
            return trainerDao.insert(t);
        }
        if (s instanceof Receptionist r) {
            return receptionistDao.insert(r);
        }
        if (s instanceof Manager m) {
            int id = inTransaction(cn -> insertKey(cn, "INSERT INTO STAFF (Full_Name, Phone, Role, Hire_Date, Salary) "
                    + "VALUES (?, ?, 'Manager', COALESCE(?, CAST(GETDATE() AS DATE)), COALESCE(?, 500.00))", ps -> {
                ps.setString(1, m.getFullName());
                ps.setString(2, m.getPhone());
                Sql.setDate(ps, 3, m.getHireDate());
                Sql.setDecimal(ps, 4, m.getSalary());
            }));
            m.setStaffId(id);
            m.setRole(StaffRole.MANAGER);
            findById(id).ifPresent(saved -> {   // nhận lại giá trị mặc định do DB điền
                m.setHireDate(saved.getHireDate());
                m.setSalary(saved.getSalary());
            });
            return m;
        }
        throw new IllegalArgumentException("Chỉ thêm được Trainer, Receptionist hoặc Manager.");
    }

    @Override
    public void update(Staff s) {
        if (s instanceof Trainer t) {
            trainerDao.update(t);
        } else if (s instanceof Receptionist r) {
            receptionistDao.update(r);
        } else {
            inTransaction(cn -> {
                updateStaffRow(cn, s);
                return null;
            });
        }
    }

    /** Xóa nhân viên (bảng STAFF); dòng TRAINER/RECEPTIONIST bị xóa theo nhờ ON DELETE CASCADE. */
    @Override
    public void deleteById(Integer id) {
        execute("DELETE FROM STAFF WHERE Staff_ID = ?", ps -> ps.setInt(1, id));
    }

    /**
     * Tìm nhân sự theo mã, họ tên, số điện thoại hoặc vai trò.
     */
    public List<Staff> search(String keyword) {
        String sql = selectSql()
                + " WHERE CAST(s.Staff_ID AS VARCHAR(20)) LIKE ?"
                + " OR LOWER(s.Full_Name) LIKE ?"
                + " OR s.Phone LIKE ?"
                + " OR LOWER(s.Role) LIKE ?"
                + " ORDER BY s.Staff_ID";

        String pattern = "%" + (keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT)) + "%";

        return queryList(sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
        });
    }

    /** Dùng khi đăng nhập: nhân viên được nhận diện bằng số điện thoại. */
    public Optional<Staff> findByPhone(String phone) {
        return queryOne(selectSql() + " WHERE s.Phone = ?", ps -> ps.setString(1, phone));
    }

    /**
     * Tìm theo số điện thoại đã chuẩn hóa để UX-04 có thể phát hiện trùng kể cả khi
     * dữ liệu cũ có dấu cách, dấu gạch, ngoặc hoặc dấu +.
     * Tham số normalizedDigits chỉ chứa chữ số.
     */
    public Optional<Staff> findByNormalizedPhone(String normalizedDigits) {
        String expression = normalizedPhoneExpression();
        return queryOne(selectSql() + " WHERE " + expression + " = ?",
                ps -> ps.setString(1, normalizedDigits));
    }

    /**
     * UX-04: kiểm tra có nhân sự KHÁC sử dụng cùng số đã chuẩn hóa hay không.
     * Khi sửa hồ sơ, loại chính mã nhân viên đang sửa ngay trong SQL để không bỏ sót
     * trường hợp dữ liệu cũ có nhiều biến thể định dạng của cùng một số.
     */
    public Optional<Staff> findDuplicateByNormalizedPhone(String normalizedDigits, Integer excludedStaffId) {
        String sql = selectSql() + " WHERE " + normalizedPhoneExpression() + " = ?";
        if (excludedStaffId != null) {
            sql += " AND s.Staff_ID <> ?";
        }
        final String query = sql;
        return queryOne(query, ps -> {
            ps.setString(1, normalizedDigits);
            if (excludedStaffId != null) {
                ps.setInt(2, excludedStaffId);
            }
        });
    }

    private static String normalizedPhoneExpression() {
        return "REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(s.Phone, ' ', ''), '-', ''), '(', ''), ')', ''), '+', ''), '.', '')";
    }

    public List<Staff> findByRole(StaffRole role) {
        return queryList(selectSql() + " WHERE s.Role = ? ORDER BY s.Staff_ID", ps -> ps.setString(1, role.getDbValue()));
    }
}

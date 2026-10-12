package com.gym.core.dao;

import com.gym.core.DBConnection;
import com.gym.core.exception.DataAccessException;
import com.gym.core.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO cho Member. Hội viên nằm ở HAI bảng: VISITOR (họ tên, SĐT) và MEMBER (email, ngày sinh, ảnh...),
 * nên đọc bằng JOIN và ghi bằng transaction hai lệnh. Ảnh KHÔNG được tải khi đọc danh sách (nặng);
 * dùng findPhoto/savePhoto để đọc và ghi ảnh riêng.
 */
public class MemberDao extends AbstractJdbcDao<Member, Integer> {

    static final String FROM = "VISITOR v JOIN MEMBER m ON m.Visitor_ID = v.Visitor_ID";
    static final String COLUMNS = "v.Visitor_ID, v.First_Name, v.Last_Name, v.Phone, "
            + "m.Email, m.Birth_Date, m.Registration_Date, "
            + "CASE WHEN m.Photo IS NULL THEN 0 ELSE 1 END AS Has_Photo";

    @Override
    protected String tableName() {
        return "MEMBER";
    }

    @Override
    protected String idColumn() {
        return "v.Visitor_ID";
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
    protected Member mapRow(ResultSet rs) throws SQLException {
        return map(rs);
    }

    /** Dùng chung với VisitorDao (cùng tên cột). */
    static Member map(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setVisitorId(rs.getInt("Visitor_ID"));
        m.setFirstName(rs.getString("First_Name"));
        m.setLastName(rs.getString("Last_Name"));
        m.setPhone(rs.getString("Phone"));
        m.setEmail(rs.getString("Email"));
        m.setBirthDate(Sql.date(rs, "Birth_Date"));
        m.setRegistrationDate(Sql.dateTime(rs, "Registration_Date"));
        m.setPhotoOnFile(rs.getInt("Has_Photo") == 1);
        return m;
    }

    @Override
    public Member insert(Member m) {
        int id = inTransaction(cn -> {
            int newId = insertKey(cn, "INSERT INTO VISITOR (First_Name, Last_Name, Phone) VALUES (?, ?, ?)", ps -> {
                ps.setString(1, m.getFirstName());
                ps.setString(2, m.getLastName());
                ps.setString(3, m.getPhone());
            });
            exec(cn, "INSERT INTO MEMBER (Visitor_ID, Email, Birth_Date, Photo, Registration_Date) "
                    + "VALUES (?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP))", ps -> {
                ps.setInt(1, newId);
                ps.setString(2, m.getEmail());
                Sql.setDate(ps, 3, m.getBirthDate());
                Sql.setBytes(ps, 4, m.getPhoto());
                Sql.setDateTime(ps, 5, m.getRegistrationDate());
            });
            return newId;
        });
        m.setVisitorId(id);
        findById(id).ifPresent(saved -> {   // nhận lại giá trị do DB điền
            m.setRegistrationDate(saved.getRegistrationDate());
            m.setPhotoOnFile(saved.hasPhoto());
        });
        return m;
    }

    /** Sửa họ tên, SĐT, email, ngày sinh. KHÔNG đụng tới ảnh (dùng savePhoto). */
    @Override
    public void update(Member m) {
        inTransaction(cn -> {
            exec(cn, "UPDATE VISITOR SET First_Name = ?, Last_Name = ?, Phone = ? WHERE Visitor_ID = ?", ps -> {
                ps.setString(1, m.getFirstName());
                ps.setString(2, m.getLastName());
                ps.setString(3, m.getPhone());
                ps.setInt(4, m.getVisitorId());
            });
            exec(cn, "UPDATE MEMBER SET Email = ?, Birth_Date = ? WHERE Visitor_ID = ?", ps -> {
                ps.setString(1, m.getEmail());
                Sql.setDate(ps, 2, m.getBirthDate());
                ps.setInt(3, m.getVisitorId());
            });
            return null;
        });
    }

    /** Xóa khách (bảng VISITOR); dòng MEMBER bị xóa theo nhờ ON DELETE CASCADE. */
    @Override
    public void deleteById(Integer id) {
        execute("DELETE FROM VISITOR WHERE Visitor_ID = ?", ps -> ps.setInt(1, id));
    }

    /** Tìm theo tên, SĐT hoặc email (chứa từ khóa), có phân trang. */
    public PageResult<Member> search(String keyword, int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Trang và kích thước trang phải từ 1 trở lên.");
        }
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String where = " WHERE (v.Last_Name + ' ' + v.First_Name) LIKE ? OR v.Phone LIKE ? OR m.Email LIKE ?";
        StatementBinder filter = ps -> {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
        };
        long total = count("SELECT COUNT(*) FROM " + FROM + where, filter);
        List<Member> items = queryList(selectSql() + where + " ORDER BY v.Visitor_ID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                ps -> {
                    filter.bind(ps);
                    ps.setInt(4, (page - 1) * pageSize);
                    ps.setInt(5, pageSize);
                });
        return new PageResult<>(items, page, pageSize, total);
    }

    public Optional<Member> findByEmail(String email) {
        return queryOne(selectSql() + " WHERE m.Email = ?", ps -> ps.setString(1, email));
    }

    public boolean existsEmail(String email) {
        return count("SELECT COUNT(*) FROM MEMBER WHERE Email = ?", ps -> ps.setString(1, email)) > 0;
    }

    /** Ảnh của hội viên; Optional rỗng nếu chưa có ảnh hoặc không có hội viên. */
    public Optional<byte[]> findPhoto(int visitorId) {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT Photo FROM MEMBER WHERE Visitor_ID = ?")) {
            ps.setInt(1, visitorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getBytes(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không đọc được ảnh hội viên", e);
        }
    }

    /** Lưu ảnh (photo = null thì xóa ảnh). */
    public void savePhoto(int visitorId, byte[] photo) {
        execute("UPDATE MEMBER SET Photo = ? WHERE Visitor_ID = ?", ps -> {
            Sql.setBytes(ps, 1, photo);
            ps.setInt(2, visitorId);
        });
    }
}

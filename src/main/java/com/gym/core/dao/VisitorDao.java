package com.gym.core.dao;

import com.gym.core.model.DailyPassTicket;
import com.gym.core.model.Member;
import com.gym.core.model.Visitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * DAO cho Visitor (lớp cha). Đây là chỗ thể hiện ĐA HÌNH: đọc một dòng VISITOR rồi trả về đối tượng
 * đúng kiểu con (Member hoặc DailyPassTicket); thêm/sửa thì giao cho DAO của kiểu con.
 * Dùng khi cần một danh sách "mọi khách" mà không quan tâm hội viên hay vé ngày.
 */
public class VisitorDao extends AbstractJdbcDao<Visitor, Integer> {

    private final MemberDao memberDao = new MemberDao();
    private final DailyPassTicketDao dailyPassDao = new DailyPassTicketDao();

    @Override
    protected String tableName() {
        return "VISITOR";
    }

    @Override
    protected String idColumn() {
        return "v.Visitor_ID";
    }

    @Override
    protected String fromClause() {
        return "VISITOR v "
                + "LEFT JOIN MEMBER m ON m.Visitor_ID = v.Visitor_ID "
                + "LEFT JOIN DAILY_PASS_TICKET d ON d.Visitor_ID = v.Visitor_ID";
    }

    @Override
    protected String selectColumns() {
        return "v.Visitor_ID, v.First_Name, v.Last_Name, v.Phone, "
                + "m.Email, m.Birth_Date, m.Registration_Date, "
                + "CASE WHEN m.Photo IS NULL THEN 0 ELSE 1 END AS Has_Photo, "
                + "d.Visit_Date, d.Daily_Rate, "
                + "CASE WHEN m.Visitor_ID IS NOT NULL THEN 'M' WHEN d.Visitor_ID IS NOT NULL THEN 'D' ELSE 'V' END AS Kind";
    }

    @Override
    protected Visitor mapRow(ResultSet rs) throws SQLException {
        switch (rs.getString("Kind")) {
            case "M":
                return MemberDao.map(rs);
            case "D":
                return DailyPassTicketDao.map(rs);
            default:
                PlainVisitor v = new PlainVisitor();
                v.setVisitorId(rs.getInt("Visitor_ID"));
                v.setFirstName(rs.getString("First_Name"));
                v.setLastName(rs.getString("Last_Name"));
                v.setPhone(rs.getString("Phone"));
                return v;
        }
    }

    @Override
    public Visitor insert(Visitor v) {
        if (v instanceof Member m) {
            return memberDao.insert(m);
        }
        if (v instanceof DailyPassTicket t) {
            return dailyPassDao.insert(t);
        }
        throw new IllegalArgumentException("Chỉ thêm được hội viên (Member) hoặc vé ngày (DailyPassTicket).");
    }

    @Override
    public void update(Visitor v) {
        if (v instanceof Member m) {
            memberDao.update(m);
        } else if (v instanceof DailyPassTicket t) {
            dailyPassDao.update(t);
        } else {
            throw new IllegalArgumentException("Chỉ sửa được hội viên (Member) hoặc vé ngày (DailyPassTicket).");
        }
    }

    @Override
    public void deleteById(Integer id) {
        execute("DELETE FROM VISITOR WHERE Visitor_ID = ?", ps -> ps.setInt(1, id));
    }

    public Optional<Visitor> findByPhone(String phone) {
        return queryOne(selectSql() + " WHERE v.Phone = ?", ps -> ps.setString(1, phone));
    }

    public boolean existsPhone(String phone) {
        return count("SELECT COUNT(*) FROM VISITOR WHERE Phone = ?", ps -> ps.setString(1, phone)) > 0;
    }

    /** Khách có dòng ở VISITOR nhưng chưa là hội viên hay vé ngày (hiếm gặp). */
    private static class PlainVisitor extends Visitor {
        @Override
        public String getDisplayName() {
            return getFullName() + " (Khách)";
        }
    }
}

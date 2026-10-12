package com.gym.core.dao;

import com.gym.core.model.DbEnum;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Các hàm nhỏ để đọc/ghi những kiểu dữ liệu dễ sai khi làm việc với JDBC:
 * cột cho phép NULL, ngày, ngày giờ, enum. Dùng trong mapRow và các StatementBinder.
 */
public final class Sql {

    private Sql() {
    }

    // ---------- Đọc từ ResultSet ----------

    /** Cột DATE, có thể NULL. */
    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }

    /** Cột DATETIME, có thể NULL. */
    public static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    /** Cột INT có thể NULL. getInt thường trả 0 khi NULL nên phải kiểm tra wasNull. */
    public static Integer integer(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    // ---------- Ghi vào PreparedStatement (hỗ trợ NULL) ----------

    public static void setDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(value));
        }
    }

    public static void setDateTime(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    public static void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    public static void setDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setBigDecimal(index, value);
        }
    }

    public static void setBytes(PreparedStatement ps, int index, byte[] value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARBINARY);
        } else {
            ps.setBytes(index, value);
        }
    }

    /** Ghi enum xuống DB bằng giá trị getDbValue(); null thì ghi NULL. */
    public static void setEnum(PreparedStatement ps, int index, DbEnum value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value.getDbValue());
        }
    }
}

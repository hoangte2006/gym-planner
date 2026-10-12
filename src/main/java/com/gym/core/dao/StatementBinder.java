package com.gym.core.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Điền giá trị vào các dấu ? của một câu SQL. Ví dụ: ps -> ps.setInt(1, 5). */
@FunctionalInterface
public interface StatementBinder {

    /** Dùng khi câu SQL không có dấu ?. */
    StatementBinder NONE = ps -> { };

    void bind(PreparedStatement ps) throws SQLException;
}

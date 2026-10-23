package com.gym.dashboard.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Bảng xếp hạng: hiện đúng số dòng, và hiện thông báo khi chưa có dữ liệu. */
class RankingPanelTest {

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Cần môi trường có màn hình để dựng thành phần Swing.");
    }

    @Test
    void showsTheGivenRowsAndReplacesThemOnTheNextCall() {
        RankingPanel panel = new RankingPanel("Top", "Hạng", "Tên", "Số");
        panel.setRows(List.of(new String[]{"1", "An", "14"}, new String[]{"2", "Bình", "9"}));
        assertEquals(2, panel.dataRowCount());

        panel.setRows(List.<String[]>of(new String[]{"1", "Chi", "5"}));
        assertEquals(1, panel.dataRowCount(), "Nạp lần sau thay hết dòng cũ, không cộng dồn");
    }

    @Test
    void emptyListShowsAMessageInsteadOfABlankTable() {
        RankingPanel panel = new RankingPanel("Top", "Hạng", "Tên", "Số");
        assertEquals(0, panel.dataRowCount(), "Vừa tạo: chưa có dòng dữ liệu nào");

        panel.setRows(List.<String[]>of(new String[]{"1", "An", "14"}));
        panel.setRows(List.of());
        assertEquals(0, panel.dataRowCount(), "Danh sách rỗng chỉ hiện dòng thông báo, không phải dữ liệu");
    }
}

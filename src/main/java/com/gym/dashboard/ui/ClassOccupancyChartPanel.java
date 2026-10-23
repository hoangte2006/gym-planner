package com.gym.dashboard.ui;

import com.gym.core.ui.Theme;
import com.gym.dashboard.model.ClassOccupancy;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Biểu đồ cột "Tỷ lệ lấp đầy lớp học trong tuần": mỗi cột là một ngày (thứ Hai đến Chủ nhật),
 * chiều cao là phần trăm chỗ đã đăng ký trên tổng sức chứa các lớp ngày đó. Ngày không có lớp là 0%.
 * Khung giống MonthlyRevenueChartPanel.
 */
public class ClassOccupancyChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final String[] WEEKDAY = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final ChartPanel chartPanel = new ChartPanel(null);

    public ClassOccupancyChartPanel() {
        super(new BorderLayout());
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setPopupMenu(null);          // bỏ menu chuột phải cho gọn
        add(chartPanel, BorderLayout.CENTER);
        setData(List.of());
    }

    /** Vẽ lại biểu đồ với dữ liệu mới (theo thứ tự các ngày trong tuần). */
    public void setData(List<ClassOccupancy> data) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (ClassOccupancy row : data) {
            String label = WEEKDAY[row.date().getDayOfWeek().getValue() - 1] + " " + DAY_LABEL.format(row.date());
            dataset.addValue(row.percent(), "Lấp đầy", label);      // (phần trăm, nhóm, nhãn cột)
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Tỷ lệ lấp đầy lớp học trong tuần", "Ngày", "Lấp đầy (%)",
                dataset, PlotOrientation.VERTICAL,
                false,      // chú giải: chỉ có một nhóm nên không cần
                true,       // gợi ý khi rê chuột lên cột
                false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(0xDDDDDD));
        plot.setOutlineVisible(false);
        plot.setNoDataMessage("Chưa có lớp trong tuần");
        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);   // nhãn nghiêng để không bị cắt thành "..."
        NumberAxis axis = (NumberAxis) plot.getRangeAxis();
        axis.setRange(0, 100);                                     // luôn từ 0 đến 100% để so sánh các ngày công bằng
        axis.setNumberFormatOverride(new DecimalFormat("0'%'"));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Theme.INFO);                    // cùng màu với thẻ "Lớp học hôm nay"
        renderer.setBarPainter(new StandardBarPainter());          // cột phẳng, bỏ hiệu ứng bóng
        renderer.setShadowVisible(false);

        chart.setBackgroundPaint(Color.WHITE);
        chartPanel.setChart(chart);
    }
}

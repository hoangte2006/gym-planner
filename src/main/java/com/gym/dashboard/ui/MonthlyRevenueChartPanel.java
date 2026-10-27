package com.gym.dashboard.ui;

import com.gym.core.ui.Theme;
import com.gym.dashboard.model.MonthlyRevenue;
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
 * Biểu đồ cột "Doanh thu theo tháng": mỗi cột là doanh thu một tháng (tổng tiền hóa đơn Paid).
 * Khung giống EquipmentStatusChartPanel: giữ một ChartPanel, mỗi lần có dữ liệu mới thì dựng lại
 * JFreeChart rồi chartPanel.setChart(...).
 */
public class MonthlyRevenueChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    private final ChartPanel chartPanel = new ChartPanel(null);

    public MonthlyRevenueChartPanel() {
        super(new BorderLayout());
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setPopupMenu(null);          // bỏ menu chuột phải cho gọn
        add(chartPanel, BorderLayout.CENTER);
        setData(List.of());
    }

    /** Vẽ lại biểu đồ với dữ liệu mới (danh sách theo thứ tự từ cũ đến mới). */
    public void setData(List<MonthlyRevenue> data) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (MonthlyRevenue row : data) {
            dataset.addValue(row.revenue(), "Doanh thu", MONTH_LABEL.format(row.month()));   // (số tiền, nhóm, nhãn cột)
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Doanh thu theo tháng", "Tháng", "Doanh thu",
                dataset, PlotOrientation.VERTICAL,
                false,      // chú giải: chỉ có một nhóm nên không cần
                true,       // gợi ý khi rê chuột lên cột
                false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(0xDDDDDD));
        plot.setOutlineVisible(false);
        plot.setNoDataMessage("Chưa có doanh thu");
        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);       // nhãn tháng nghiêng cho khỏi chồng
        ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("#,##0"));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Theme.ACCENT);                  // cùng màu với thẻ "Doanh thu tháng này"
        renderer.setBarPainter(new StandardBarPainter());          // cột phẳng, bỏ hiệu ứng bóng
        renderer.setShadowVisible(false);

        chart.setBackgroundPaint(Color.WHITE);
        chartPanel.setChart(chart);
    }
}

package com.gym.dashboard.ui;

import com.gym.core.ui.Theme;
import com.gym.dashboard.model.MonthlyMaintenanceCost;
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

/** Biểu đồ cột "Chi phí bảo trì theo tháng": mỗi cột là tổng chi phí các phiếu bảo trì của một tháng. */
public class MaintenanceCostChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    private final ChartPanel chartPanel = new ChartPanel(null);

    public MaintenanceCostChartPanel() {
        super(new BorderLayout());
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setPopupMenu(null);          // bỏ menu chuột phải cho gọn
        add(chartPanel, BorderLayout.CENTER);
        setData(List.of());
    }

    /** Vẽ lại biểu đồ với dữ liệu mới (danh sách theo thứ tự từ cũ đến mới). */
    public void setData(List<MonthlyMaintenanceCost> data) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (MonthlyMaintenanceCost row : data) {
            dataset.addValue(row.cost(), "Chi phí", MONTH_LABEL.format(row.month()));   // (số tiền, nhóm, nhãn cột)
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Chi phí bảo trì theo tháng", "Tháng", "Chi phí",
                dataset, PlotOrientation.VERTICAL,
                false,      // chú giải: chỉ có một nhóm nên không cần
                true,       // gợi ý khi rê chuột lên cột
                false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(0xDDDDDD));
        plot.setOutlineVisible(false);
        plot.setNoDataMessage("Chưa có chi phí bảo trì");
        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("#,##0"));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Theme.WARNING);                 // màu cam: chi phí, khác màu xanh của doanh thu
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);

        chart.setBackgroundPaint(Color.WHITE);
        chartPanel.setChart(chart);
    }
}

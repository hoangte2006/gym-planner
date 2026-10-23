package com.gym.dashboard.ui;

import com.gym.dashboard.model.EquipmentStatusCount;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

/**
 * Biểu đồ tròn "Thiết bị theo trạng thái". Mỗi miền màu là một trạng thái, dùng đúng nhãn tiếng Việt và
 * màu của enum EquipmentStatus (Hoạt động xanh lá, Đang bảo trì cam, Hỏng đỏ) để khớp với bảng dữ liệu.
 *
 * Cách dùng: gọi setData(...) với danh sách lấy từ Service (trên luồng giao diện, ví dụ trong onSuccess của Async).
 * Muốn làm biểu đồ khác (cột, đường) thì tạo lớp tương tự: giữ một ChartPanel, và mỗi lần có dữ liệu mới
 * thì dựng lại JFreeChart rồi gọi chartPanel.setChart(...).
 */
public class EquipmentStatusChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ChartPanel chartPanel = new ChartPanel(null);

    public EquipmentStatusChartPanel() {
        super(new BorderLayout());
        chartPanel.setPreferredSize(new Dimension(380, 280));
        chartPanel.setPopupMenu(null);          // bỏ menu chuột phải (Properties, Save as...) cho gọn
        add(chartPanel, BorderLayout.CENTER);
        setData(List.of());
    }

    /** Vẽ lại biểu đồ với dữ liệu mới. */
    public void setData(List<EquipmentStatusCount> data) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();     // tập dữ liệu: tên miền -> số lượng
        for (EquipmentStatusCount row : data) {
            dataset.setValue(row.status().getLabel(), row.count());
        }

        JFreeChart chart = ChartFactory.createPieChart("Thiết bị theo trạng thái", dataset, true, true, false);

        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        for (EquipmentStatusCount row : data) {
            plot.setSectionPaint(row.status().getLabel(), Color.decode(row.status().getColorHex()));
        }
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})"));   // "Hỏng: 1 (16,7%)"
        plot.setNoDataMessage("Chưa có thiết bị");
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        chart.setBackgroundPaint(Color.WHITE);

        chartPanel.setChart(chart);
    }
}

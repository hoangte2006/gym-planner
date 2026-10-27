package com.gym.dashboard.ui;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * Khung nội dung đặt trong JScrollPane: luôn vừa chiều NGANG của cửa sổ; chiều CAO thì giãn đầy khung khi cửa sổ
 * cao hơn nội dung, còn khi cửa sổ thấp hơn thì giữ chiều cao cần thiết và hiện thanh cuộn dọc.
 * (JPanel thường đặt trong JScrollPane không làm được: nó luôn ép về đúng chiều cao ưa thích và không giãn.)
 */
class ScrollableContent extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    ScrollableContent(LayoutManager layout) {
        super(layout);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(visibleRect.height - 32, 16);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // Cửa sổ cao hơn nội dung: giãn theo cửa sổ. Thấp hơn: giữ chiều cao ưa thích để cuộn được.
        return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
    }
}

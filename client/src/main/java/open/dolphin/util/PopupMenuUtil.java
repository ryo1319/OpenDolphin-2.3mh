package open.dolphin.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPopupMenu;

/**
 * ポップアップの行数が多すぎる場合のworkaround
 * 
 * @author masuda, Masuda Naika
 */
public class PopupMenuUtil {

    public static JPopupMenu createPopupMenu() {
        JPopupMenu popup = new MyPopupMenu();
        popup.setLayout(new MultiColumnMenuLayout());
        return popup;
    }

    public static Rectangle getScreenRect(Component invoker, Point p) {

        GraphicsConfiguration gc = getCurrentGraphicsConfiguration(invoker, p);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Insets screenInsets = toolkit.getScreenInsets(gc);

        Rectangle screenRect = gc.getBounds();
        screenRect.x += screenInsets.left;
        screenRect.y += screenInsets.top;
        screenRect.width -= screenInsets.right;
        screenRect.height -= screenInsets.bottom;

        return screenRect;
    }

    private static GraphicsConfiguration getCurrentGraphicsConfiguration(Component invoker, Point popupLocation) {

        GraphicsConfiguration gc = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (GraphicsDevice gd : ge.getScreenDevices()) {
            if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                GraphicsConfiguration dgc = gd.getDefaultConfiguration();
                if (dgc.getBounds().contains(popupLocation)) {
                    gc = dgc;
                    break;
                }
            }
        }

        // If not found and we have invoker, ask invoker about his gc
        if (gc == null && invoker != null) {
            gc = invoker.getGraphicsConfiguration();
        }
        return gc;
    }
}

/**
 * TaskBarに重ならないPopupMenu
 */
class MyPopupMenu extends JPopupMenu {

    @Override
    public void show(Component invoker, int x, int y) {
        Rectangle screenRect = PopupMenuUtil.getScreenRect(invoker, new Point(x, y));

        Dimension popupSize = getPreferredSize();
        Point invokerScreenLoc = invoker.getLocationOnScreen();
        Point popupScreenLoc = new Point(invokerScreenLoc.x + x, invokerScreenLoc.y + y);

        int offsetX = popupScreenLoc.x + popupSize.width - screenRect.width;
        if (offsetX > 0) {
            x -= offsetX;
        }
        int offsetY = popupScreenLoc.y + popupSize.height - screenRect.height;
        if (offsetY > 0) {
            y -= offsetY;
        }

        super.show(invoker, x, y);
    }
}

/**
 * 画面サイズで折り返して複数列で表示するLayoutManager
 */
class MultiColumnMenuLayout implements LayoutManager {

    private Dimension preferredSize = new Dimension();
    private boolean layoutDone;

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        if (!layoutDone) {
            doLayout(parent);
        }
        Insets i = parent.getInsets();
        Dimension size = new Dimension(preferredSize);
        size.height += i.top + i.bottom;
        size.width += i.left + i.right;
        return size;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        if (layoutDone) {
            return;
        }
        doLayout(parent);
    }

    private void doLayout(Container target) {
        
        final int maxHeight = PopupMenuUtil.getScreenRect(target, new Point(0, 0)).height;

        preferredSize.height = 0;
        preferredSize.width = 0;
        Insets insets = target.getInsets();
        
        // 現在の列の領域
        Rectangle rowRect = new Rectangle(insets.left, insets.top, 0, 0);
        int maxColumnHeight = 0;
        
        List<Component> list = new ArrayList<Component>();
        for (Component comp : target.getComponents()) {
            Dimension compSize = comp.getPreferredSize();
            if (rowRect.height + compSize.height > maxHeight) {
                moveComponents(list, rowRect.x, rowRect.y, rowRect.width);
                rowRect.x += rowRect.width;
                maxColumnHeight = Math.max(maxColumnHeight, rowRect.height);
                // 列が変わると幅高さ初期化
                rowRect.height = 0;
                rowRect.width = 0;
                list.clear();
            }
            rowRect.width = Math.max(rowRect.width, compSize.width);
            rowRect.height += compSize.height;
            list.add(comp);
        }
        // 最終列の処理
        if (!list.isEmpty()) {
            moveComponents(list, rowRect.x, rowRect.y, rowRect.width);
            rowRect.x += rowRect.width;
            maxColumnHeight = Math.max(maxColumnHeight, rowRect.height);
        }

        preferredSize.height = maxColumnHeight;
        preferredSize.width = rowRect.x;
        layoutDone = true;
    }

    private void moveComponents(List<Component> list, int x, int y, int width) {
         for (Component c : list) {
            int height = c.getPreferredSize().height;
            c.setBounds(x, y, width, height);
            y += height;
        }
    }
}

package com.intellijwindowtabs;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Point;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.List;

public final class WindowTabsAction extends DumbAwareAction implements CustomComponentAction {
    private record Tab(JFrame frame, String name, String path) {}

    @Override
    public JComponent createCustomComponent(Presentation presentation, String place) {
        return new Tabs();
    }

    @Override
    public void actionPerformed(AnActionEvent event) {}

    private static final class Tabs extends JPanel {
        private final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, JBUI.scale(3), JBUI.scale(1)));
        private final JScrollPane viewport = new JScrollPane(row, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        private int availableWidth;
        private final Timer timer = new Timer(500, event -> refresh());
        private List<Tab> displayed = List.of();
        private Window selected;

        private Tabs() {
            super(new BorderLayout());
            setOpaque(false);
            row.setOpaque(false);
            viewport.setOpaque(false);
            viewport.getViewport().setOpaque(false);
            viewport.setBorder(null);
            viewport.setWheelScrollingEnabled(false);
            viewport.addMouseWheelListener(event -> {
                var view = viewport.getViewport();
                Point position = view.getViewPosition();
                int limit = Math.max(0, view.getViewSize().width - view.getExtentSize().width);
                int next = Math.max(0, Math.min(limit, position.x + event.getUnitsToScroll() * JBUI.scale(16)));
                if (next != position.x) {
                    view.setViewPosition(new Point(next, position.y));
                    event.consume();
                }
            });
            add(viewport);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Math.max(JBUI.scale(32), remainingWidth()), JBUI.scale(29));
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(JBUI.scale(32), JBUI.scale(29));
        }

        private int remainingWidth() {
            Container section = getParent();
            while (section != null && !(section instanceof ActionToolbar)) section = section.getParent();
            if (section == null || section.getParent() == null) return JBUI.scale(180);

            Container toolbar = section.getParent();
            if (toolbar.getWidth() == 0) return JBUI.scale(180);
            int left = 0;
            int right = 0;
            boolean beforeCenter = true;
            for (Component sibling : toolbar.getComponents()) {
                if (sibling == section) {
                    beforeCenter = false;
                } else if (sibling.isVisible()) {
                    if (beforeCenter) left += sibling.getPreferredSize().width;
                    else right += sibling.getPreferredSize().width;
                }
            }
            int width = Math.min(toolbar.getWidth() / 2,
                    toolbar.getWidth() - 2 * Math.max(left, right) - JBUI.scale(40));
            width -= section.getInsets().left + section.getInsets().right;
            for (Component sibling : section.getComponents()) {
                if (sibling != this && sibling.isVisible() && !SwingUtilities.isDescendingFrom(this, sibling)) {
                    width -= sibling.getPreferredSize().width;
                }
            }
            return width;
        }

        @Override
        public void addNotify() {
            super.addNotify();
            refresh();
            timer.start();
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        private void refresh() {
            int width = remainingWidth();
            if (availableWidth != width) {
                availableWidth = width;
                revalidate();
            }
            var windows = new LinkedHashMap<JFrame, Tab>();
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                JFrame frame = WindowManager.getInstance().getFrame(project);
                if (frame != null) {
                    windows.putIfAbsent(frame, new Tab(frame, project.getName(), project.getBasePath()));
                }
            }

            Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            while (active != null && !windows.containsKey(active)) {
                active = active.getOwner();
            }
            if (active == null && windows.containsKey(selected)) active = selected;
            List<Tab> tabs = List.copyOf(windows.values());
            if (tabs.equals(displayed) && active == selected) return;

            displayed = tabs;
            selected = active;
            Point scrollPosition = viewport.getViewport().getViewPosition();
            row.removeAll();
            JButton activeButton = null;
            for (Tab tab : tabs) {
                boolean isActive = tab.frame() == selected;
                JButton button = new TabButton(tab.name(), isActive);
                button.setToolTipText(tab.path() == null ? tab.name() : tab.path());
                button.addActionListener(event -> activate(tab.frame()));
                row.add(button);
                if (isActive) activeButton = button;
            }
            row.revalidate();
            row.repaint();
            JButton selection = activeButton;
            SwingUtilities.invokeLater(() -> {
                if (!isDisplayable() || (selection != null && selection.getParent() != row)) return;
                var view = viewport.getViewport();
                int limit = Math.max(0, row.getPreferredSize().width - view.getExtentSize().width);
                view.setViewPosition(new Point(Math.min(scrollPosition.x, limit), 0));
                if (selection != null) row.scrollRectToVisible(selection.getBounds());
            });
        }
    }

    private static final class TabButton extends JButton {
        private final boolean active;

        private TabButton(String label, boolean active) {
            super(label);
            this.active = active;
            setFont(JBUI.CurrentTheme.EditorTabs.font());
            setBorder(JBUI.Borders.empty(0, 10));
            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            if (active) setForeground(JBColor.namedColor("Button.default.foreground", JBColor.WHITE));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width, JBUI.scale(27));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D fill = (Graphics2D) graphics.create();
            try {
                fill.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean emphasized = getModel().isRollover() || hasFocus();
                if (active) {
                    fill.setColor(JBColor.namedColor(
                            emphasized ? "Button.default.focusedBackground" : "Button.default.startBackground",
                            JBUI.CurrentTheme.EditorTabs.underlineColor()));
                } else {
                    fill.setColor(JBColor.namedColor(
                            emphasized ? "ActionButton.pressedBackground" : "ActionButton.hoverBackground",
                            new JBColor(0xE5E6E8, 0x414347)));
                }
                int arc = JBUI.scale(8);
                fill.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            } finally {
                fill.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static void activate(JFrame frame) {
        if (!frame.isDisplayable()) return;
        frame.setExtendedState(frame.getExtendedState() & ~JFrame.ICONIFIED);
        frame.toFront();
        frame.requestFocus();
    }
}

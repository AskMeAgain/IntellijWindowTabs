package io.github.askmeagain.projecttabbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
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
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.List;

public final class ProjectTabBarAction extends DumbAwareAction implements CustomComponentAction {
    private record Tab(JFrame frame, String label, String projectName, String path) {}

    @Override
    public JComponent createCustomComponent(Presentation presentation, String place) {
        return new Tabs();
    }

    @Override
    public void actionPerformed(AnActionEvent event) {}

    private static final class Tabs extends JPanel {
        private final JPanel row = new JPanel(new FlowLayout(FlowLayout.TRAILING, JBUI.scale(3), JBUI.scale(1)));
        private final JScrollPane viewport = new JScrollPane(row, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        private final ActionButton newWindowButton = createWorktreeButton();
        private int availableWidth;
        private final Timer timer = new Timer(500, event -> refresh());
        private List<Tab> displayed = List.of();
        private Window selected;
        private boolean closeButtonsDisplayed;

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
            add(viewport, BorderLayout.CENTER);
            add(newWindowButton, BorderLayout.LINE_END);
        }

        @Override
        public Dimension getPreferredSize() {
            int buttonWidth = newWindowButton.getPreferredSize().width;
            int contentWidth = row.getComponentCount() == 0 ? 0 : row.getPreferredSize().width;
            return new Dimension(Math.max(buttonWidth, Math.min(contentWidth + buttonWidth, remainingWidth())),
                    JBUI.scale(29));
        }

        @Override
        public Dimension getMinimumSize() {
            return newWindowButton.getPreferredSize();
        }

        private ActionButton createWorktreeButton() {
            AnAction action = new AnAction("New Worktree", "Create a Git worktree", AllIcons.General.Add) {
                @Override
                public void actionPerformed(AnActionEvent event) {
                    openWorktree();
                }
            };
            int size = JBUI.scale(29);
            return new ActionButton(action, action.getTemplatePresentation().clone(),
                    ActionPlaces.MAIN_TOOLBAR, new Dimension(size, size));
        }

        private void openWorktree() {
            Window frame = SwingUtilities.getWindowAncestor(this);
            Project project = null;
            for (Project candidate : ProjectManager.getInstance().getOpenProjects()) {
                if (WindowManager.getInstance().getFrame(candidate) == frame) {
                    project = candidate;
                    break;
                }
            }
            if (project == null) return;

            var action = ActionManager.getInstance().getAction("Git.CreateNewWorkingTree");
            if (action == null) {
                Messages.showInfoMessage(project, "This IDE does not provide the Git worktree action.", "New Worktree");
                return;
            }
            var context = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build();
            action.actionPerformed(AnActionEvent.createFromAnAction(action, null, "ProjectTabBar.Toolbar", context));
        }

        private int remainingWidth() {
            Container section = getParent();
            while (section != null && !(section instanceof ActionToolbar)) section = section.getParent();
            if (section == null || section.getParent() == null) return JBUI.scale(180);

            Container toolbar = section.getParent();
            if (toolbar.getWidth() == 0) return JBUI.scale(180);
            int width = toolbar.getWidth() - JBUI.scale(40);
            for (Component sibling : toolbar.getComponents()) {
                if (sibling != section && sibling.isVisible()) width -= sibling.getPreferredSize().width;
            }
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
            var options = ProjectTabBarSettings.getInstance().getState();
            var windows = new LinkedHashMap<JFrame, Tab>();
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                JFrame frame = WindowManager.getInstance().getFrame(project);
                if (frame != null) {
                    windows.putIfAbsent(frame, new Tab(frame, tabLabel(project, options),
                            project.getName(), project.getBasePath()));
                }
            }

            Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            while (active != null && !windows.containsKey(active)) {
                active = active.getOwner();
            }
            if (active == null && windows.containsKey(selected)) active = selected;
            List<Tab> tabs = List.copyOf(windows.values());
            if (tabs.equals(displayed) && active == selected && closeButtonsDisplayed == options.addCloseButton) return;

            closeButtonsDisplayed = options.addCloseButton;

            displayed = tabs;
            selected = active;
            Point scrollPosition = viewport.getViewport().getViewPosition();
            row.removeAll();
            JComponent activeTab = null;
            for (Tab tab : tabs) {
                boolean isActive = tab.frame() == selected;
                JButton button = new TabButton(tab.label(), closeButtonsDisplayed);
                button.setToolTipText(tab.path() == null ? tab.projectName() : tab.path());
                button.getAccessibleContext().setAccessibleName(
                        tab.label().isEmpty() ? tab.projectName() : tab.label());
                button.addActionListener(event -> activate(tab.frame()));
                var middleClick = new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent event) {
                        if (SwingUtilities.isMiddleMouseButton(event)
                                && ProjectTabBarSettings.getInstance().getState().allowMiddleClickClose) {
                            closeWindow(tab.frame());
                        }
                    }
                };
                button.addMouseListener(middleClick);

                JPanel tabPanel = new TabPanel(isActive);
                tabPanel.addMouseListener(middleClick);
                tabPanel.add(button, BorderLayout.CENTER);
                if (closeButtonsDisplayed) {
                    var icon = AllIcons.Actions.Close;
                    JButton close = new JButton(icon);
                    close.setBorder(JBUI.Borders.empty());
                    close.setBorderPainted(false);
                    close.setPreferredSize(new Dimension(icon.getIconWidth() + JBUI.scale(2),
                            icon.getIconHeight() + JBUI.scale(4)));
                    close.setContentAreaFilled(false);
                    close.setToolTipText("Close " + tab.projectName());
                    close.getAccessibleContext().setAccessibleName(close.getToolTipText());
                    close.addMouseListener(middleClick);
                    close.addActionListener(event -> {
                        if (ProjectTabBarSettings.getInstance().getState().addCloseButton) {
                            closeWindow(tab.frame());
                        }
                    });
                    JPanel closeArea = new JPanel(new GridBagLayout());
                    closeArea.setOpaque(false);
                    closeArea.setBorder(JBUI.Borders.empty(0, 0, 0, 10));
                    closeArea.addMouseListener(middleClick);
                    closeArea.add(close);
                    tabPanel.add(closeArea, BorderLayout.LINE_END);
                }
                row.add(tabPanel);
                if (isActive) activeTab = tabPanel;
            }
            row.revalidate();
            revalidate();
            row.repaint();
            JComponent selection = activeTab;
            SwingUtilities.invokeLater(() -> {
                if (!isDisplayable() || (selection != null && selection.getParent() != row)) return;
                var view = viewport.getViewport();
                int limit = Math.max(0, row.getPreferredSize().width - view.getExtentSize().width);
                view.setViewPosition(new Point(Math.min(scrollPosition.x, limit), 0));
                if (selection != null) row.scrollRectToVisible(selection.getBounds());
            });
        }
    }

    private static String tabLabel(Project project, ProjectTabBarSettings.Options options) {
        String name = options.includeProjectName ? project.getName() : "";
        if (!options.includeBranchName) return name;

        var provider = project.getService(ProjectTabBranchProvider.class);
        String branch = provider == null ? "" : provider.getBranchName();
        if (branch.isEmpty()) return name;
        return name.isEmpty() ? branch : name + " | " + branch;
    }

    private static final class TabButton extends JButton {
        private final boolean compact;

        private TabButton(String label, boolean compact) {
            super(label);
            this.compact = compact;
            setFont(JBUI.CurrentTheme.EditorTabs.font());
            setBorder(compact ? JBUI.Borders.empty(0, 18, 0, 0) : JBUI.Borders.empty(0, 10));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        @Override
        public Dimension getPreferredSize() {
            int width = super.getPreferredSize().width;
            if (compact) {
                var insets = getInsets();
                width = getFontMetrics(getFont()).stringWidth(getText()) + insets.left + insets.right;
            }
            return new Dimension(width, JBUI.scale(27));
        }
    }

    private static final class TabPanel extends JPanel {
        private final boolean active;

        private TabPanel(boolean active) {
            super(new BorderLayout());
            this.active = active;
            setOpaque(false);
        }

        @Override
        protected void paintChildren(Graphics graphics) {
            super.paintChildren(graphics);
            if (!active) return;

            Graphics underline = graphics.create();
            try {
                underline.setColor(JBUI.CurrentTheme.EditorTabs.underlineColor());
                int thickness = JBUI.scale(2);
                underline.fillRect(0, getHeight() - thickness, getWidth(), thickness);
            } finally {
                underline.dispose();
            }
        }
    }

    private static void closeWindow(JFrame frame) {
        if (frame.isDisplayable()) {
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        }
    }

    private static void activate(JFrame frame) {
        if (!frame.isDisplayable()) return;
        frame.setExtendedState(frame.getExtendedState() & ~JFrame.ICONIFIED);
        frame.toFront();
        frame.requestFocus();
    }
}

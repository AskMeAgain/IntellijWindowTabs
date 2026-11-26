package io.github.askmeagain.projecttabbar.ui;

import io.github.askmeagain.projecttabbar.branch.ProjectTabBranchProvider;
import io.github.askmeagain.projecttabbar.settings.ProjectTabBarOptions;
import io.github.askmeagain.projecttabbar.settings.ProjectTabBarSettings;

import com.intellij.execution.ExecutionManager;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.List;

final class Tabs extends JPanel {
  private static boolean hasRunningProcess(Project project) {
    for (var process : ExecutionManager.getInstance(project).getRunningProcesses()) {
      if (!process.isProcessTerminated()) return true;
    }
    return false;
  }

  private static String tabLabel(Project project, ProjectTabBarOptions options) {
    var name = options.includeProjectName ? project.getName() : "";
    if (!options.includeBranchName) return name;

    var provider = project.getService(ProjectTabBranchProvider.class);
    var branch = provider == null ? "" : provider.getBranchName();
    if (branch.isEmpty()) return name;
    return name.isEmpty() ? branch : name + " | " + branch;
  }

  static void closeWindow(JFrame frame) {
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

  private final JPanel row = new JPanel(new FlowLayout(FlowLayout.TRAILING, JBUI.scale(3), JBUI.scale(1)));
  private final JScrollPane viewport = new JScrollPane(row, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  private final ActionButton newWindowButton = createWorktreeButton();
  private final Timer timer = new Timer(500, event -> refresh());
  private final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
  private final PropertyChangeListener activeWindowListener = event -> SwingUtilities.invokeLater(() -> {
    if (isDisplayable()) refresh();
  });
  private int availableWidth;
  private List<Tab> displayed = List.of();
  private Window selected;
  private boolean closeButtonsDisplayed;

  Tabs() {
    super(new BorderLayout());
    setOpaque(false);
    row.setOpaque(false);
    viewport.setOpaque(false);
    viewport.getViewport().setOpaque(false);
    viewport.setBorder(null);
    viewport.setWheelScrollingEnabled(false);

    viewport.addMouseWheelListener(event -> {
      var view = viewport.getViewport();
      var position = view.getViewPosition();
      var limit = Math.max(0, view.getViewSize().width - view.getExtentSize().width);
      var next = Math.max(0, Math.min(limit, position.x + event.getUnitsToScroll() * JBUI.scale(16)));
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
    var buttonWidth = newWindowButton.getPreferredSize().width;
    var contentWidth = row.getComponentCount() == 0 ? 0 : row.getPreferredSize().width;
    return new Dimension(Math.max(buttonWidth, Math.min(contentWidth + buttonWidth, remainingWidth())),
      JBUI.scale(29));
  }

  @Override
  public Dimension getMinimumSize() {
    return newWindowButton.getPreferredSize();
  }

  private ActionButton createWorktreeButton() {
    var action = new NewWorktreeAction(this);
    var size = JBUI.scale(29);
    return new ActionButton(action, action.getTemplatePresentation().clone(),
      ActionPlaces.MAIN_TOOLBAR, new Dimension(size, size));
  }

  private int remainingWidth() {
    var section = getParent();
    while (section != null && !(section instanceof ActionToolbar)) section = section.getParent();
    if (section == null || section.getParent() == null) return JBUI.scale(180);

    var toolbar = section.getParent();
    if (toolbar.getWidth() == 0) return JBUI.scale(180);

    var width = toolbar.getWidth() - JBUI.scale(40);
    for (var sibling : toolbar.getComponents()) {
      if (sibling != section && sibling.isVisible()) width -= sibling.getPreferredSize().width;
    }

    width -= section.getInsets().left + section.getInsets().right;
    for (var sibling : section.getComponents()) {
      if (sibling != this && sibling.isVisible() && !SwingUtilities.isDescendingFrom(this, sibling)) {
        width -= sibling.getPreferredSize().width;
      }
    }
    return width;
  }

  @Override
  public void addNotify() {
    super.addNotify();
    focusManager.addPropertyChangeListener("activeWindow", activeWindowListener);
    refresh();
    timer.start();
  }

  @Override
  public void removeNotify() {
    timer.stop();
    focusManager.removePropertyChangeListener("activeWindow", activeWindowListener);
    super.removeNotify();
  }

  private void refresh() {
    var width = remainingWidth();
    if (availableWidth != width) {
      availableWidth = width;
      revalidate();
    }

    var options = ProjectTabBarSettings.getInstance().getState();
    var windows = new LinkedHashMap<JFrame, Tab>();
    for (var project : ProjectManager.getInstance().getOpenProjects()) {
      var frame = WindowManager.getInstance().getFrame(project);
      if (frame != null) {
        windows.putIfAbsent(frame, new Tab(frame, tabLabel(project, options),
          project.getName(), project.getBasePath(),
          options.showRunConfiguration && hasRunningProcess(project)));
      }
    }

    var active = focusManager.getActiveWindow();
    while (active != null && !windows.containsKey(active)) {
      active = active.getOwner();
    }
    if (active == null && windows.containsKey(selected)) active = selected;

    var tabs = List.copyOf(windows.values());
    if (tabs.equals(displayed) && active == selected && closeButtonsDisplayed == options.addCloseButton) return;

    closeButtonsDisplayed = options.addCloseButton;
    displayed = tabs;
    selected = active;

    var scrollPosition = viewport.getViewport().getViewPosition();
    row.removeAll();

    JComponent activeTab = null;
    for (var tab : tabs) {
      var isActive = tab.frame() == selected;
      var button = new TabButton(tab.label(), closeButtonsDisplayed);
      if (tab.running()) button.setIcon(AllIcons.Actions.Execute);
      button.setToolTipText(tab.path() == null ? tab.projectName() : tab.path());
      button.getAccessibleContext().setAccessibleName(
        tab.label().isEmpty() ? tab.projectName() : tab.label());

      button.addActionListener(event -> activate(tab.frame()));
      var middleClick = new MiddleClickCloseListener(tab.frame());
      button.addMouseListener(middleClick);

      var tabPanel = new TabPanel(isActive);
      tabPanel.addMouseListener(middleClick);
      tabPanel.add(button, BorderLayout.CENTER);

      if (closeButtonsDisplayed) {
        var icon = AllIcons.Actions.Close;
        var close = new JButton(icon);
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

        var closeArea = new JPanel(new GridBagLayout());
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

    var selection = activeTab;
    SwingUtilities.invokeLater(() -> {
      if (!isDisplayable() || (selection != null && selection.getParent() != row)) return;
      var view = viewport.getViewport();
      var limit = Math.max(0, row.getPreferredSize().width - view.getExtentSize().width);
      view.setViewPosition(new Point(Math.min(scrollPosition.x, limit), 0));
      if (selection != null) row.scrollRectToVisible(selection.getBounds());
    });
  }
}

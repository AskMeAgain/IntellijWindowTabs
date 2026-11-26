package io.github.askmeagain.projecttabbar.ui;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.ui.JBUI;
import io.github.askmeagain.projecttabbar.settings.ProjectTabBarSettings;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Displays open project windows in a scrolling toolbar row and refreshes their labels and active state.
 */
final class Tabs extends JPanel {
  private static final int TAB_HORIZONTAL_GAP = 3;
  private static final int TAB_VERTICAL_GAP = 1;
  private static final int REFRESH_INTERVAL_MS = 500;
  private static final int SCROLL_UNIT_WIDTH = 16;
  private static final int TOOLBAR_HEIGHT = 29;
  private static final int FALLBACK_WIDTH = 180;
  private static final int TOOLBAR_RESERVED_WIDTH = 40;

  private final JPanel row = new JPanel(new FlowLayout(FlowLayout.TRAILING, JBUI.scale(TAB_HORIZONTAL_GAP), JBUI.scale(TAB_VERTICAL_GAP)));
  private final JScrollPane viewport = new JScrollPane(row, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  private final ActionButton newWindowButton = createWorktreeButton();
  private final Timer timer = new Timer(REFRESH_INTERVAL_MS, event -> refresh());
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
      var next = Math.max(0, Math.min(limit, position.x + event.getUnitsToScroll() * JBUI.scale(SCROLL_UNIT_WIDTH)));
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
    var buttonWidth = newWindowButton.isVisible() ? newWindowButton.getPreferredSize().width : 0;
    var contentWidth = row.getComponentCount() == 0 ? 0 : row.getPreferredSize().width;
    return new Dimension(Math.max(buttonWidth, Math.min(contentWidth + buttonWidth, remainingWidth())),
      JBUI.scale(TOOLBAR_HEIGHT));
  }

  @Override
  public Dimension getMinimumSize() {
    return newWindowButton.isVisible() ? newWindowButton.getPreferredSize() : new Dimension(0, JBUI.scale(TOOLBAR_HEIGHT));
  }

  private ActionButton createWorktreeButton() {
    var action = new NewWorktreeAction();
    var size = JBUI.scale(TOOLBAR_HEIGHT);
    return new ActionButton(action, action.getTemplatePresentation().clone(), ActionPlaces.MAIN_TOOLBAR, new Dimension(size, size));
  }

  private int remainingWidth() {
    var section = getParent();
    while (section != null && !(section instanceof ActionToolbar)) section = section.getParent();
    if (section == null || section.getParent() == null) return JBUI.scale(FALLBACK_WIDTH);

    var toolbar = section.getParent();
    if (toolbar.getWidth() == 0) return JBUI.scale(FALLBACK_WIDTH);

    var width = toolbar.getWidth() - JBUI.scale(TOOLBAR_RESERVED_WIDTH);
    for (var sibling : toolbar.getComponents()) {
      if (sibling != section && sibling.isVisible()) width -= sibling.getPreferredSize().width;
    }

    width -= section.getInsets().left + section.getInsets().right;
    for (var sibling : section.getComponents()) {
      if (sibling.isVisible() && !SwingUtilities.isDescendingFrom(this, sibling)) {
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
    newWindowButton.setVisible(options.showCreateWorktreeButton);
    var windows = new LinkedHashMap<JFrame, Tab>();
    for (var project : ProjectManager.getInstance().getOpenProjects()) {
      var frame = WindowManager.getInstance().getFrame(project);
      if (frame != null) {
        windows.computeIfAbsent(frame, window -> Tab.fromProject(window, project, options));
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
      var tabPanel = new TabPanel(tab, isActive, closeButtonsDisplayed);
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


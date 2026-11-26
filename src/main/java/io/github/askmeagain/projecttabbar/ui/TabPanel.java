package io.github.askmeagain.projecttabbar.ui;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBUI;
import io.github.askmeagain.projecttabbar.settings.ProjectTabBarSettings;

import javax.swing.*;
import java.awt.*;

/**
 * Builds an interactive project tab with activation, optional close controls, and an active underline.
 */
final class TabPanel extends JPanel {
  private final boolean active;

  TabPanel(Tab tab, boolean active, boolean closeButtonsDisplayed) {
    super(new BorderLayout());
    this.active = active;
    setOpaque(false);

    var button = new TabButton(tab.label(), closeButtonsDisplayed);
    if (tab.running()) {
      button.setIcon(AllIcons.Actions.Execute);
    }

    button.setToolTipText(tab.path() == null ? tab.projectName() : tab.path());
    button.getAccessibleContext().setAccessibleName(
      tab.label().isEmpty() ? tab.projectName() : tab.label());

    button.addActionListener(event -> tab.activate());
    var middleClick = new MiddleClickCloseListener(tab);
    button.addMouseListener(middleClick);

    addMouseListener(middleClick);
    add(button, BorderLayout.CENTER);

    if (closeButtonsDisplayed) {
      add(createCloseArea(tab, middleClick), BorderLayout.LINE_END);
    }
  }

  private static JPanel createCloseArea(Tab tab, MiddleClickCloseListener middleClick) {
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
        tab.closeWindow();
      }
    });

    var closeArea = new JPanel(new GridBagLayout());
    closeArea.setOpaque(false);
    closeArea.setBorder(JBUI.Borders.empty(0, 0, 0, 10));
    closeArea.addMouseListener(middleClick);
    closeArea.add(close);
    return closeArea;
  }

  @Override
  protected void paintChildren(Graphics graphics) {
    super.paintChildren(graphics);
    if (!active) return;

    var underline = graphics.create();
    try {
      underline.setColor(JBUI.CurrentTheme.EditorTabs.underlineColor());
      var thickness = JBUI.scale(2);
      underline.fillRect(0, getHeight() - thickness, getWidth(), thickness);
    } finally {
      underline.dispose();
    }
  }
}

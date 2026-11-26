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
  private static final int CLOSE_BUTTON_EXTRA_WIDTH = 2;
  private static final int CLOSE_BUTTON_EXTRA_HEIGHT = 4;
  private static final int CLOSE_AREA_RIGHT_PADDING = 10;
  private static final int ACTIVE_UNDERLINE_THICKNESS = 2;

  private final TabButton button;
  private final boolean active;
  private final boolean attention;
  private final Timer animation = new Timer(40, event -> repaint());

  TabPanel(Tab tab, boolean active, boolean closeButtonsDisplayed) {
    super(new BorderLayout());
    this.active = active;
    this.attention = tab.airNeedsAttention();
    setOpaque(false);

    button = new TabButton(tab.label(), closeButtonsDisplayed);

    if (tab.running()) {
      button.setIcon(AllIcons.Actions.Execute);
    }

    button.setToolTipText(tab.path() == null ? tab.projectName() : tab.path());
    button.getAccessibleContext().setAccessibleName(tab.label().isEmpty() ? tab.projectName() : tab.label());

    button.addActionListener(event -> tab.activate());
    var middleClick = new MiddleClickCloseListener(tab);
    button.addMouseListener(middleClick);

    addMouseListener(middleClick);
    add(button, BorderLayout.CENTER);

    if (closeButtonsDisplayed) {
      add(createCloseArea(tab, middleClick), BorderLayout.LINE_END);
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (attention) animation.start();
  }

  @Override
  public void removeNotify() {
    animation.stop();
    super.removeNotify();
  }

  @Override
  protected void paintChildren(Graphics graphics) {
    super.paintChildren(graphics);

    if (!active && !attention) return;

    var underline = (Graphics2D) graphics.create();
    try {
      if (attention) {
        underline.setColor(button.getForeground());
        var pulse = (float) (0.5 - 0.5 * Math.cos(System.nanoTime() / 1_000_000_000.0 * Math.PI));
        underline.setComposite(AlphaComposite.SrcOver.derive(0.7f * pulse));
      } else {
        underline.setColor(JBUI.CurrentTheme.EditorTabs.underlineColor());
      }
      var thickness = JBUI.scale(ACTIVE_UNDERLINE_THICKNESS);
      underline.fillRect(0, getHeight() - thickness, getWidth(), thickness);
    } finally {
      underline.dispose();
    }
  }

  private static JPanel createCloseArea(Tab tab, MiddleClickCloseListener middleClick) {
    var icon = AllIcons.Actions.Close;
    var close = new JButton(icon);
    close.setBorder(JBUI.Borders.empty());
    close.setBorderPainted(false);
    close.setPreferredSize(new Dimension(icon.getIconWidth() + JBUI.scale(CLOSE_BUTTON_EXTRA_WIDTH),
      icon.getIconHeight() + JBUI.scale(CLOSE_BUTTON_EXTRA_HEIGHT)));
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
    closeArea.setBorder(JBUI.Borders.empty(0, 0, 0, CLOSE_AREA_RIGHT_PADDING));
    closeArea.addMouseListener(middleClick);
    closeArea.add(close);
    return closeArea;
  }
}

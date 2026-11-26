package io.github.askmeagain.projecttabbar.ui;

import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;

final class TabPanel extends JPanel {
  private final boolean active;

  TabPanel(boolean active) {
    super(new BorderLayout());
    this.active = active;
    setOpaque(false);
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

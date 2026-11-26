package io.github.askmeagain.projecttabbar.ui;

import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;

/**
 * Renders a tab label and optional icon with toolbar sizing and optional compact spacing.
 */
final class TabButton extends JButton {
  private static final int COMPACT_LEFT_PADDING = 18;
  private static final int HORIZONTAL_PADDING = 10;
  private static final int TAB_HEIGHT = 27;

  private final boolean compact;

  TabButton(String label, boolean compact) {
    super(label);
    this.compact = compact;

    setFont(JBUI.CurrentTheme.EditorTabs.font());
    setBorder(compact ? JBUI.Borders.empty(0, COMPACT_LEFT_PADDING, 0, 0) : JBUI.Borders.empty(0, HORIZONTAL_PADDING));
    setBorderPainted(false);
    setContentAreaFilled(false);
    setOpaque(false);
    setRolloverEnabled(true);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  @Override
  public Dimension getPreferredSize() {
    var width = super.getPreferredSize().width;
    if (compact) {
      var insets = getInsets();
      width = getFontMetrics(getFont()).stringWidth(getText()) + insets.left + insets.right;

      var icon = getIcon();
      if (icon != null) {
        width += icon.getIconWidth();
        if (!getText().isEmpty()) width += getIconTextGap();
      }
    }
    return new Dimension(width, JBUI.scale(TAB_HEIGHT));
  }
}
package io.github.askmeagain.projecttabbar.ui;

import io.github.askmeagain.projecttabbar.settings.ProjectTabBarSettings;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Closes the associated project window on a middle click when enabled in the tab bar settings.
 */
@RequiredArgsConstructor
final class MiddleClickCloseListener extends MouseAdapter {
  private final Tab tab;

  @Override
  public void mouseClicked(MouseEvent event) {
    if (SwingUtilities.isMiddleMouseButton(event) && ProjectTabBarSettings.getInstance().getState().allowMiddleClickClose) {
      tab.closeWindow();
    }
  }
}

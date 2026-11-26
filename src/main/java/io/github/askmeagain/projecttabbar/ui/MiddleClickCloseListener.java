package io.github.askmeagain.projecttabbar.ui;

import io.github.askmeagain.projecttabbar.settings.ProjectTabBarSettings;

import lombok.RequiredArgsConstructor;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@RequiredArgsConstructor
final class MiddleClickCloseListener extends MouseAdapter {
  private final JFrame frame;

  @Override
  public void mouseClicked(MouseEvent event) {
    if (SwingUtilities.isMiddleMouseButton(event)
      && ProjectTabBarSettings.getInstance().getState().allowMiddleClickClose) {
      Tabs.closeWindow(frame);
    }
  }
}

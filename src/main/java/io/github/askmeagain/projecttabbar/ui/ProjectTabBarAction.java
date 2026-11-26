package io.github.askmeagain.projecttabbar.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.DumbAwareAction;
import javax.swing.*;
import java.awt.*;

public final class ProjectTabBarAction extends DumbAwareAction implements CustomComponentAction {
  @Override
  public JComponent createCustomComponent(Presentation presentation, String place) {
    return new Tabs();
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
  }
}

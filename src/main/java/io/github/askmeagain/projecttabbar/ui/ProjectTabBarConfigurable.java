package io.github.askmeagain.projecttabbar.ui;

import io.github.askmeagain.projecttabbar.settings.ProjectTabBarSettings;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class ProjectTabBarConfigurable implements Configurable {
  private final JBCheckBox projectName = new JBCheckBox("Include project name in tab");
  private final JBCheckBox branchName = new JBCheckBox("Include branch name in tab");
  private final JBCheckBox middleClickClose = new JBCheckBox("Allow middle click close");
  private final JBCheckBox closeButton = new JBCheckBox("Add close button");
  private final JBCheckBox showRunConfiguration = new JBCheckBox("Show run configuration");

  @Override
  public @NotNull String getDisplayName() {
    return "Project Tab Bar";
  }

  @Override
  public JComponent createComponent() {
    return FormBuilder.createFormBuilder()
      .addComponent(projectName)
      .addComponent(branchName)
      .addComponent(middleClickClose)
      .addComponent(closeButton)
      .addComponent(showRunConfiguration)
      .addComponentFillVertically(new javax.swing.JPanel(), 0)
      .getPanel();
  }

  @Override
  public boolean isModified() {
    var options = ProjectTabBarSettings.getInstance().getState();

    return projectName.isSelected() != options.includeProjectName
      || branchName.isSelected() != options.includeBranchName
      || middleClickClose.isSelected() != options.allowMiddleClickClose
      || closeButton.isSelected() != options.addCloseButton
      || showRunConfiguration.isSelected() != options.showRunConfiguration;
  }

  @Override
  public void apply() {
    var options = ProjectTabBarSettings.getInstance().getState();

    options.includeProjectName = projectName.isSelected();
    options.includeBranchName = branchName.isSelected();
    options.allowMiddleClickClose = middleClickClose.isSelected();
    options.addCloseButton = closeButton.isSelected();
    options.showRunConfiguration = showRunConfiguration.isSelected();
  }

  @Override
  public void reset() {
    var options = ProjectTabBarSettings.getInstance().getState();

    projectName.setSelected(options.includeProjectName);
    branchName.setSelected(options.includeBranchName);
    middleClickClose.setSelected(options.allowMiddleClickClose);
    closeButton.setSelected(options.addCloseButton);
    showRunConfiguration.setSelected(options.showRunConfiguration);
  }
}

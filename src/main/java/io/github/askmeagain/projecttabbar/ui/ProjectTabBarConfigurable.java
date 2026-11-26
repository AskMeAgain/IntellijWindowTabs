package io.github.askmeagain.projecttabbar.ui;

import io.github.askmeagain.projecttabbar.settings.ProjectTabBarSettings;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Exposes the Project Tab Bar settings page for editing and saving tab display and close options.
 */
public final class ProjectTabBarConfigurable implements Configurable {
  private final JBCheckBox showCreateWorktreeButton = new JBCheckBox("Show create worktree button");
  private final JBCheckBox projectName = new JBCheckBox("Include project name in tab");
  private final JBCheckBox branchName = new JBCheckBox("Include branch name in tab");
  private final JBCheckBox middleClickClose = new JBCheckBox("Allow middle click close");
  private final JBCheckBox closeButton = new JBCheckBox("Add close button");
  private final JBCheckBox showAirAgentSessionStatus = new JBCheckBox("Show air agent session status");
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
      .addComponent(showAirAgentSessionStatus)
      .addComponent(showCreateWorktreeButton)
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
      || showRunConfiguration.isSelected() != options.showRunConfiguration
      || showAirAgentSessionStatus.isSelected() != options.showAirAgentSessionStatus
      || showCreateWorktreeButton.isSelected() != options.showCreateWorktreeButton;
  }

  @Override
  public void apply() {
    var options = ProjectTabBarSettings.getInstance().getState();

    options.showCreateWorktreeButton = showCreateWorktreeButton.isSelected();
    options.includeProjectName = projectName.isSelected();
    options.includeBranchName = branchName.isSelected();
    options.allowMiddleClickClose = middleClickClose.isSelected();
    options.addCloseButton = closeButton.isSelected();
    options.showAirAgentSessionStatus = showAirAgentSessionStatus.isSelected();
    options.showRunConfiguration = showRunConfiguration.isSelected();
  }

  @Override
  public void reset() {
    var options = ProjectTabBarSettings.getInstance().getState();

    showCreateWorktreeButton.setSelected(options.showCreateWorktreeButton);
    projectName.setSelected(options.includeProjectName);
    branchName.setSelected(options.includeBranchName);
    middleClickClose.setSelected(options.allowMiddleClickClose);
    closeButton.setSelected(options.addCloseButton);
    showAirAgentSessionStatus.setSelected(options.showAirAgentSessionStatus);
    showRunConfiguration.setSelected(options.showRunConfiguration);
  }
}



package io.github.askmeagain.projecttabbar;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public final class ProjectTabBarConfigurable implements Configurable {
    private final JBCheckBox projectName = new JBCheckBox("include project name in tab");
    private final JBCheckBox branchName = new JBCheckBox("include branch name in tab");

    @Override
    public @NotNull String getDisplayName() {
        return "Project Tab Bar";
    }

    @Override
    public JComponent createComponent() {
        return FormBuilder.createFormBuilder()
                .addComponent(projectName)
                .addComponent(branchName)
                .addComponentFillVertically(new javax.swing.JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        var options = ProjectTabBarSettings.getInstance().getState();
        return projectName.isSelected() != options.includeProjectName
                || branchName.isSelected() != options.includeBranchName;
    }

    @Override
    public void apply() {
        var options = ProjectTabBarSettings.getInstance().getState();
        options.includeProjectName = projectName.isSelected();
        options.includeBranchName = branchName.isSelected();
    }

    @Override
    public void reset() {
        var options = ProjectTabBarSettings.getInstance().getState();
        projectName.setSelected(options.includeProjectName);
        branchName.setSelected(options.includeBranchName);
    }
}

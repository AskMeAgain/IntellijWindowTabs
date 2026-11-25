package io.github.askmeagain.windowtabs;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public final class WindowTabsConfigurable implements Configurable {
    private final JBCheckBox projectName = new JBCheckBox("include project name in tab");
    private final JBCheckBox branchName = new JBCheckBox("include branch name in tab");

    @Override
    public @NotNull String getDisplayName() {
        return "Window Tab Plugin";
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
        var options = WindowTabsSettings.getInstance().getState();
        return projectName.isSelected() != options.includeProjectName
                || branchName.isSelected() != options.includeBranchName;
    }

    @Override
    public void apply() {
        var options = WindowTabsSettings.getInstance().getState();
        options.includeProjectName = projectName.isSelected();
        options.includeBranchName = branchName.isSelected();
    }

    @Override
    public void reset() {
        var options = WindowTabsSettings.getInstance().getState();
        projectName.setSelected(options.includeProjectName);
        branchName.setSelected(options.includeBranchName);
    }
}

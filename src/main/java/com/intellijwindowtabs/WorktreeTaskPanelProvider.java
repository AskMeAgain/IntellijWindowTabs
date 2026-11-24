package com.intellijwindowtabs;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.TaskBundle;
import com.intellij.openapi.vcs.VcsTaskHandler;
import com.intellij.uiDesigner.core.AbstractLayout;
import com.intellij.tasks.ui.TaskDialogPanel;
import com.intellij.tasks.ui.TaskDialogPanelProvider;
import com.intellij.ui.components.JBCheckBox;
import git4idea.config.GitExecutableManager;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;

public final class WorktreeTaskPanelProvider extends TaskDialogPanelProvider {
    @Override
    public TaskDialogPanel getOpenTaskPanel(Project project, LocalTask task) {
        var repositories = GitRepositoryManager.getInstance(project).getRepositories();
        if (repositories.isEmpty()) return null;
        GitRepository repository = repositories.stream()
                .filter(repo -> repo.getRoot().getPath().equals(project.getBasePath()))
                .findFirst().orElse(repositories.size() == 1 ? repositories.getFirst() : null);

        return new TaskDialogPanel() {
            private final JBCheckBox createWorktree = new JBCheckBox("Create a new Worktree");
            private final JPanel panel = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
                }
            };
            private BranchControls controls;
            private boolean restoreCreate;
            private boolean restoreUse;

            {
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.setOpaque(false);
                panel.add(createWorktree, BorderLayout.LINE_START);
                createWorktree.addActionListener(event -> updateBranchControls());
                createWorktree.setToolTipText("Create the task branch in a sibling Git worktree");
            }

            private void updateBranchControls() {
                if (createWorktree.isSelected()) {
                    controls = null;
                    for (Container ancestor = panel.getParent(); ancestor != null && controls == null;
                         ancestor = ancestor.getParent()) {
                        controls = findBranchControls(ancestor);
                    }
                    if (controls == null) return;
                    restoreCreate = controls.create().isSelected();
                    restoreUse = controls.use() != null && controls.use().isSelected();
                    controls.create().setSelected(false);
                    controls.create().setEnabled(false);
                    if (controls.use() != null) {
                        controls.use().setSelected(false);
                        controls.use().setEnabled(false);
                    }
                    controls.name().setEnabled(true);
                    controls.base().setEnabled(true);
                } else if (controls != null) {
                    controls.create().setEnabled(true);
                    if (controls.use() != null) {
                        controls.use().setEnabled(true);
                        controls.use().setSelected(restoreUse);
                    }
                    controls.create().setSelected(restoreCreate);
                    controls.name().setEnabled(restoreCreate);
                    controls.base().setEnabled(restoreCreate);
                    controls = null;
                }
            }

            @Override
            public JComponent getPanel() {
                return panel;
            }

            @Override
            public ValidationInfo validate() {
                if (!createWorktree.isSelected()) return null;
                if (controls == null || controls.name().getText().isBlank()) {
                    return new ValidationInfo("Enter a branch name in the task's Create branch field.", createWorktree);
                }
                if (repository == null) {
                    return new ValidationInfo("A project Git root is required when multiple repositories are mapped.", createWorktree);
                }
                if (repository.getCurrentRevision() == null || root().getParent() == null) {
                    return new ValidationInfo("The Git repository needs a current commit and a parent directory.", createWorktree);
                }
                if (Files.exists(destination(), LinkOption.NOFOLLOW_LINKS)) {
                    return new ValidationInfo("Worktree location already exists: " + destination(), createWorktree);
                }
                return null;
            }

            private Path root() {
                return repository.getRoot().toNioPath();
            }

            private String taskSlug() {
                String slug = controls.name().getText().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "");
                return slug.isEmpty() ? "task" : slug.substring(0, Math.min(slug.length(), 48));
            }

            private Path destination() {
                return root().resolveSibling(root().getFileName() + "-worktree-" + taskSlug());
            }

            @Override
            public void commit() {
                if (!createWorktree.isSelected() || validate() != null) return;
                Path directory = destination();
                String branch = controls.name().getText().strip();
                String revision = controls.base().getSelectedItem() instanceof VcsTaskHandler.TaskInfo base
                        ? (base.isRemote() ? "refs/remotes/" : "refs/heads/") + base.getName()
                        : repository.getCurrentRevision();
                var executable = GitExecutableManager.getInstance().getExecutable(project, root().toFile());

                new Task.Backgroundable(project, "Creating Git Worktree", false) {
                    private String error;

                    @Override
                    public void run(ProgressIndicator indicator) {
                        if (!executable.isLocal()) {
                            error = "The worktree requires a local Git executable.";
                            return;
                        }
                        try {
                            var command = new GeneralCommandLine(executable.getExePath(), "worktree", "add",
                                    "-b", branch, "--", directory.toString(), revision)
                                    .withWorkDirectory(root().toFile());
                            var output = new CapturingProcessHandler(command).runProcess();
                            if (output.getExitCode() != 0) {
                                error = output.getStderr().isBlank() ? output.getStdout() : output.getStderr();
                                if (error.isBlank()) error = "Git exited with code " + output.getExitCode() + ".";
                            }
                        } catch (ExecutionException exception) {
                            error = exception.getMessage();
                        }
                    }

                    @Override
                    public void onSuccess() {
                        if (project.isDisposed()) return;
                        if (error != null) Messages.showErrorDialog(project, error, "Could Not Create Worktree");
                        else Messages.showInfoMessage(project, directory.toString(), "Git Worktree Created");
                    }
                }.queue();
            }
        };
    }

    private record BranchControls(AbstractButton create, AbstractButton use, JTextField name, JComboBox<?> base) {}

    // Tasks exposes the dialog panel extension, but not its branch fields.
    private static BranchControls findBranchControls(Container container) {
        if (container.getLayout() instanceof AbstractLayout layout) {
            AbstractButton create = null;
            AbstractButton use = null;
            for (Component child : container.getComponents()) {
                if (child instanceof AbstractButton button) {
                    String text = button.getText();
                    if (TaskBundle.message("create.branch").replace("&", "").equals(text)) create = button;
                    if (TaskBundle.message("u.se.branch").replace("&", "").equals(text)) use = button;
                }
            }
            if (create != null) {
                int row = layout.getConstraintsForComponent(create).getRow();
                JTextField name = null;
                JComboBox<?> base = null;
                for (Component child : container.getComponents()) {
                    if (layout.getConstraintsForComponent(child).getRow() != row) continue;
                    if (child instanceof JTextField field) name = field;
                    if (child instanceof JComboBox<?> combo) base = combo;
                }
                if (name != null && base != null) return new BranchControls(create, use, name, base);
            }
        }
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                BranchControls controls = findBranchControls(nested);
                if (controls != null) return controls;
            }
        }
        return null;
    }

    @Override
    public TaskDialogPanel getCloseTaskPanel(Project project, LocalTask task) {
        return null;
    }
}

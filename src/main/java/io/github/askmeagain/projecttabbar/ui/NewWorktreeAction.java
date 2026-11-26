package io.github.askmeagain.projecttabbar.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.WindowManager;

import java.awt.Component;
import javax.swing.SwingUtilities;

final class NewWorktreeAction extends AnAction {
  private final Component owner;

  NewWorktreeAction(Component owner) {
    super("New Worktree", "Create a Git worktree", AllIcons.General.Add);
    this.owner = owner;
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    var frame = SwingUtilities.getWindowAncestor(owner);
    Project project = null;
    for (var candidate : ProjectManager.getInstance().getOpenProjects()) {
      if (WindowManager.getInstance().getFrame(candidate) == frame) {
        project = candidate;
        break;
      }
    }

    if (project == null) return;

    var action = ActionManager.getInstance().getAction("Git.CreateNewWorkingTree");
    if (action == null) {
      Messages.showInfoMessage(project, "This IDE does not provide the Git worktree action.", "New Worktree");
      return;
    }

    var context = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build();
    action.actionPerformed(AnActionEvent.createFromAnAction(action, null, "ProjectTabBar.Toolbar", context));
  }
}

package io.github.askmeagain.projecttabbar.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ui.Messages;

final class NewWorktreeAction extends AnAction {
  NewWorktreeAction() {
    super("New Worktree", "Create a Git worktree", AllIcons.General.Add);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    var project = event.getProject();
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

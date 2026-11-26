package io.github.askmeagain.projecttabbar.ui;

import io.github.askmeagain.projecttabbar.branch.ProjectTabBranchProvider;
import io.github.askmeagain.projecttabbar.settings.ProjectTabBarOptions;
import com.intellij.execution.ExecutionManager;
import com.intellij.openapi.project.Project;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

/**
 * Represents a project window and its displayed tab state, with helpers to activate or close the window.
 */
record Tab(JFrame frame, String label, String projectName, String path, boolean running) {
  static Tab fromProject(JFrame frame, Project project, ProjectTabBarOptions options) {
    return new Tab(frame, tabLabel(project, options), project.getName(), project.getBasePath(),
      options.showRunConfiguration && hasRunningProcess(project));
  }

  private static boolean hasRunningProcess(Project project) {
    for (var process : ExecutionManager.getInstance(project).getRunningProcesses()) {
      if (!process.isProcessTerminated()) return true;
    }
    return false;
  }

  private static String tabLabel(Project project, ProjectTabBarOptions options) {
    var name = options.includeProjectName ? project.getName() : "";
    
    if (!options.includeBranchName) return name;

    var provider = project.getService(ProjectTabBranchProvider.class);
    var branch = provider == null ? "" : provider.getBranchName();
    
    if (branch.isEmpty()) return name;
    
    return name.isEmpty() ? branch : name + " | " + branch;
  }

  void closeWindow() {
    if (frame.isDisplayable()) {
      frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
  }

  void activate() {
    if (!frame.isDisplayable()) return;
    frame.setExtendedState(frame.getExtendedState() & ~JFrame.ICONIFIED);
    frame.toFront();
    frame.requestFocus();
  }
}

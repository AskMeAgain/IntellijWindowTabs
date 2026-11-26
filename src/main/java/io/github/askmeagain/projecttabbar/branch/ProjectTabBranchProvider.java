package io.github.askmeagain.projecttabbar.branch;

/**
 * Defines the project service used by tabs to obtain the current branch label.
 */
public interface ProjectTabBranchProvider {
  String getBranchName();
}

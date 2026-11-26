package io.github.askmeagain.projecttabbar.branch;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;

/**
 * Provides the branch label for project tabs using the Git repository with the shortest root path.
 */
@RequiredArgsConstructor
public final class GitProjectTabBranchProvider implements ProjectTabBranchProvider {
  private final Project project;

  @Override
  public String getBranchName() {
    return GitRepositoryManager.getInstance(project)
      .getRepositories()
      .stream()
      .min(Comparator.comparingInt((GitRepository repository) -> repository.getRoot().getPath().length())
        .thenComparing(repository -> repository.getRoot().getPath()))
      .map(Repository::getCurrentBranchName)
      .orElse("");
  }
}

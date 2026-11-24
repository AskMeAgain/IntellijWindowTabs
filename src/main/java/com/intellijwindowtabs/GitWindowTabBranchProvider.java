package com.intellijwindowtabs;

import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.Comparator;

public final class GitWindowTabBranchProvider implements WindowTabBranchProvider {
    private final Project project;

    public GitWindowTabBranchProvider(Project project) {
        this.project = project;
    }

    @Override
    public String getBranchName() {
        return GitRepositoryManager.getInstance(project).getRepositories().stream()
                .min(Comparator.comparingInt((GitRepository repository) -> repository.getRoot().getPath().length())
                        .thenComparing(repository -> repository.getRoot().getPath()))
                .map(repository -> repository.getCurrentBranchName())
                .orElse("");
    }
}

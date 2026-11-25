package io.github.askmeagain.projecttabbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "WindowTabsSettings", storages = @Storage("windowTabs.xml"))
public final class ProjectTabBarSettings implements PersistentStateComponent<ProjectTabBarSettings.Options> {
    public static final class Options {
        public boolean includeProjectName = true;
        public boolean includeBranchName = true;
        public boolean allowMiddleClickClose;
        public boolean addCloseButton;
    }

    private Options options = new Options();

    static ProjectTabBarSettings getInstance() {
        return ApplicationManager.getApplication().getService(ProjectTabBarSettings.class);
    }

    @Override
    public @NotNull Options getState() {
        return options;
    }

    @Override
    public void loadState(@NotNull Options state) {
        options = state;
    }
}

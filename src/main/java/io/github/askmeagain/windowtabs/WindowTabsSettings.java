package io.github.askmeagain.windowtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "WindowTabsSettings", storages = @Storage("windowTabs.xml"))
public final class WindowTabsSettings implements PersistentStateComponent<WindowTabsSettings.Options> {
    public static final class Options {
        public boolean includeProjectName = true;
        public boolean includeBranchName = true;
    }

    private Options options = new Options();

    static WindowTabsSettings getInstance() {
        return ApplicationManager.getApplication().getService(WindowTabsSettings.class);
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

package io.github.askmeagain.projecttabbar.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Provides application-wide access to tab bar options and persists them in windowTabs.xml.
 */
@State(name = "WindowTabsSettings", storages = @Storage("windowTabs.xml"))
public final class ProjectTabBarSettings implements PersistentStateComponent<ProjectTabBarOptions> {
  @Getter(onMethod_ = {@Override, @NotNull})
  private ProjectTabBarOptions state = new ProjectTabBarOptions();

  public static ProjectTabBarSettings getInstance() {
    return ApplicationManager.getApplication().getService(ProjectTabBarSettings.class);
  }

  @Override
  public void loadState(@NotNull ProjectTabBarOptions state) {
    this.state = state;
  }
}

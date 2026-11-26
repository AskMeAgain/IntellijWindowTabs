package io.github.askmeagain.projecttabbar.air;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import lombok.RequiredArgsConstructor;

import java.awt.KeyboardFocusManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registered only when Air is available; adapts its version-specific frontend catalog.
 */
@RequiredArgsConstructor
public final class AirProjectTabStatusProvider implements ProjectTabAirStatusProvider {
  private static final Logger LOG = Logger.getInstance(AirProjectTabStatusProvider.class);
  private static final Set<String> ACTIONABLE = Set.of("READY", "NEEDS_INPUT", "UNREAD", "REVIEWING");

  private boolean failureReported;
  private Class<?> catalogType;
  private final Map<String, String> acknowledged = new HashMap<>();
  private final Project project;

  @Override
  public boolean needsAttention() {
    if (project.isDisposed()) return false;
    var path = project.getBasePath();
    if (path == null) return false;

    var projectPath = normalize(path);

    try {
      var type = catalogType();
      var state = type.getMethod("getInstance").invoke(null);
      var flow = type.getMethod("getSessions").invoke(state);
      var flowType = type.getClassLoader().loadClass("kotlinx.coroutines.flow.StateFlow");
      var snapshot = flowType.getMethod("getValue").invoke(flow);
      for (var workspace : (Iterable<?>) call(snapshot, "getProjects")) {
        if (matches(workspace, projectPath)) return true;
        for (var worktree : (Iterable<?>) call(workspace, "getWorktrees")) {
          if (matches(worktree, projectPath)) return true;
        }
      }
    } catch (ReflectiveOperationException | LinkageError error) {
      if (!failureReported) {
        failureReported = true;
        LOG.warn("Unable to read Air session status", error);
      }
    }
    return false;
  }

  private Class<?> catalogType() throws ReflectiveOperationException {
    if (catalogType != null) return catalogType;

    // Air content modules have classloaders separate from the main plugin.
    var pluginSet = PluginManagerCore.class.getMethod("getPluginSet").invoke(null);
    for (var module : (Iterable<?>) call(pluginSet, "getEnabledModules")) {
      if (!module.getClass().getSimpleName().equals("ContentModuleDescriptor")) continue;
      if (!"intellij.air.frontend.session.catalog.api".equals(call(module, "getModuleNameString"))) continue;
      var loader = (ClassLoader) call(module, "getPluginClassLoader");
      catalogType = loader.loadClass("com.intellij.air.frontend.session.catalog.SessionDiscoveryState");
      return catalogType;
    }

    throw new ClassNotFoundException("Air session catalog module is unavailable");
  }

  private boolean matches(Object workspace, String projectPath) throws ReflectiveOperationException {
    var path = call(workspace, "getProjectDirectory");
    if (path == null || !projectPath.equalsIgnoreCase(normalize((String) call(path, "getValue")))) return false;

    var foreground = projectWindowIsForeground();
    var attention = false;

    for (var session : (Iterable<?>) call(workspace, "getSessions")) {
      if (Boolean.TRUE.equals(call(session, "getArchived"))) continue;
      var activity = ((Enum<?>) call(session, "getActivity")).name();
      var id = call(session, "getId").toString();
      var version = call(session, "getUpdatedAtMs") + ":" + activity;

      if (foreground || !ACTIONABLE.contains(activity)) {
        acknowledged.put(id, version);
      } else if (!version.equals(acknowledged.get(id))) {
        attention = true;
      }
    }

    return attention;
  }

  private boolean projectWindowIsForeground() {
    var frame = WindowManager.getInstance().getFrame(project);
    var active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

    while (active != null) {
      if (active == frame) return true;
      active = active.getOwner();
    }

    return false;
  }

  private static String normalize(String path) {
    return path.replace('\\', '/').replaceAll("/+$", "");
  }

  private static Object call(Object target, String method) throws ReflectiveOperationException {
    return target.getClass().getMethod(method).invoke(target);
  }
}
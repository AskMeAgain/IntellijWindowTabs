# Window Tabs for IntelliJ IDEA

Window Tabs adds a browser-like project switcher in the main toolbar of each open IntelliJ IDEA window. Every project window shows the same set of tabs, so you can switch between open windows with one click.

Each tab shows its project name. Hover over a tab to see the project's path. The active window is highlighted, and the tabs update as project windows open or close. The tab strip is centered and uses half the toolbar width when space permits. When the tabs overflow, scroll horizontally with the mouse wheel or trackpad.

## Build and try it

The plugin targets IntelliJ IDEA 2024.3 or later and uses Java 21.

On Windows, run `./gradlew.bat runIde` from PowerShell to launch a test IDE with the plugin. Run `./gradlew.bat buildPlugin` to create a distributable ZIP in `build/distributions`.

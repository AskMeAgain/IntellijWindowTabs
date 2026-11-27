# Project Tab Bar for IntelliJ IDEA

Can be added via the marketplace or from here: https://plugins.jetbrains.com/plugin/36474-project-tab-bar

Adds tabs to the toolbar so you can switch between open projects.

![Project Tab Bar demo in IntelliJ IDEA](./project-tab-bar-demo.gif)

Tabs can show each project's name and current Git branch.

![Project tabs with branch labels](./example.png)

A run icon shows which project has a running configuration, even when you're working in another project.

![Run configuration active in another project tab](./run-configuration-running-in-different-tab.png)

An animated indicator shows when a project's Air agent is waiting for input.

![Air agent waiting indicator in a project tab](./ai-waiting-animation.gif)

## Settings

Open **Settings → Tools → Project Tab Bar** to configure the tabs. Settings apply to all project windows.

| Setting | Description | Default |
| --- | --- | --- |
| Include project name in tab | Shows the project name in the tab label. | Enabled |
| Include branch name in tab | Shows the current Git branch in the tab label. When both name options are enabled, the label is `Project name \| branch`. Projects without an available branch show only the project name, if enabled. | Enabled |
| Allow middle click close | Lets you close a project window by clicking its tab with the middle mouse button. | Enabled |
| Add close button | Adds a close button to each tab to close its project window. | Enabled |
| Show run configuration | Shows a run icon on a project's tab while it has a running process. | Enabled |
| Show air agent session status | Displays the Air agent session status on the project's tab when available. | Enabled |
| Show create worktree button | Adds a button to the tab bar for creating a Git worktree. | Enabled |

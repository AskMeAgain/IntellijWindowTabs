package io.github.askmeagain.projecttabbar.air;

/** Optional project service supplying Air session attention state. */
public interface ProjectTabAirStatusProvider {
  boolean needsAttention();
}

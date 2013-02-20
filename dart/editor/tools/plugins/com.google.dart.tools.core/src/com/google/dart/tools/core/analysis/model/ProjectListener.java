package com.google.dart.tools.core.analysis.model;

/**
 * Used by {@link ProjectManager} to notify others when a project has been analyzed.
 */
public interface ProjectListener {

  /**
   * Called on the builder thread (non-UI thread) immediately after a project has been analyzed.
   * 
   * @param event the event describing the analysis (not {@code null})
   */
  void projectAnalyzed(ProjectEvent event);
}

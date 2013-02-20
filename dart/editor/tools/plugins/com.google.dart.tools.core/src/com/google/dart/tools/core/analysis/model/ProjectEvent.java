package com.google.dart.tools.core.analysis.model;

/**
 * Event sent via {@link ProjectListener} to indicate that a project has been analyzed.
 */
public class ProjectEvent {

  private final Project project;

  public ProjectEvent(Project project) {
    this.project = project;
  }

  /**
   * Answer the project that was updated
   * 
   * @return the project (not {@code null})
   */
  public Project getProject() {
    return project;
  }
}

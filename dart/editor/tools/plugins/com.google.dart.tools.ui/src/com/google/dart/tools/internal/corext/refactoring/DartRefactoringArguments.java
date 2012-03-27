package com.google.dart.tools.internal.corext.refactoring;

import com.google.dart.tools.core.refactoring.descriptors.DartRefactoringDescriptor;

import java.util.Map;

/**
 * A wrapper around the Map received from {@link DartRefactoringDescriptor} to access and convert
 * the options.
 */
public final class DartRefactoringArguments {

  /** The attribute map (element type: <code>&lt;String, String&gt;</code>) */
  private final Map<String, String> fAttributes;

  /** The name of the project, or <code>null</code> for the workspace */
  private final String fProject;

  /**
   * Creates a new refactoring arguments from arguments
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param arguments the arguments
   */
  public DartRefactoringArguments(String project, Map<String, String> arguments) {
    fProject = project;
    fAttributes = arguments;
  }

  /**
   * Returns the attribute with the specified name.
   * 
   * @param name the name of the attribute
   * @return the attribute value, or <code>null</code>
   */
  public String getAttribute(final String name) {
    return fAttributes.get(name);
  }

  /**
   * Returns the name of the project.
   * 
   * @return the name of the project, or <code>null</code> for the workspace
   */
  public String getProject() {
    return fProject;
  }

  @Override
  public String toString() {
    return getClass().getName() + fAttributes.toString();
  }
}

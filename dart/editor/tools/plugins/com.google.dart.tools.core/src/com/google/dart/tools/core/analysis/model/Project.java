package com.google.dart.tools.core.analysis.model;

import com.google.dart.engine.context.AnalysisContext;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;

/**
 * Represents an Eclipse project that has a Dart nature
 */
public interface Project {

  /**
   * Discard all analysis contexts in the specified container
   * 
   * @param container the container (not {@code null})
   */
  void discardContextsIn(IContainer container);

  /**
   * Answer the {@link AnalysisContext} used to analyze Dart source in the specified folder,
   * creating a new context or retrieving the parent context if one is not already associated with
   * this container.
   * 
   * @param container a container (not {@code null}) in this project
   * @return the context used for analysis or {@code null} if the context was not cached and could
   *         not be created because the container's location could not be determined
   */
  AnalysisContext getContext(IContainer container);

  /**
   * Answer the Eclipse project associated with this Dart project
   * 
   * @return the Eclipse project (not {@code null})
   */
  IProject getResource();

  /**
   * Called when a pubspec file is added
   * 
   * @param container the container (not {@code null}) to which a pubspec file was added
   */
  void pubspecAdded(IContainer container);

  /**
   * Called when a pubspec file is removed
   * 
   * @param container the container (not {@code null}) from which a pubspec file was removed
   */
  void pubspecRemoved(IContainer container);
}

package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.context.AnalysisContext;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;

/**
 * Represents an Eclipse project that has a Dart nature
 */
public interface Project {

  /**
   * Called when a container is deleted
   * 
   * @param container the container (not {@code null}) that was deleted
   */
  void containerDeleted(IContainer container);

  /**
   * Answer the {@link AnalysisContext} used to analyze Dart source in the specified folder,
   * creating a new context or retrieving the parent context if one is not already associated with
   * this container.
   * 
   * @param container a container (not {@code null}) in this project
   * @return the context used for analysis (not {@code null})
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

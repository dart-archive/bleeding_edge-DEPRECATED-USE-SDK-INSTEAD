package com.google.dart.tools.core.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;

/**
 * Instances of {@code Project} represents an Eclipse project that has a Dart nature.
 *
 * @coverage dart.tools.core.model
 */
public interface Project extends ContextManager {

  /**
   * Discard all analysis contexts in the specified container
   *
   * @param container the container (not {@code null})
   */
  void discardContextsIn(IContainer container);

  /**
   * Answer the default analysis context. If the receiver contains a {@link PubFolder} which
   * contains all of the resources contained in the receiver, then the analysis context for that
   * {@link PubFolder} will be the same the receiver's default analysis context.
   *
   * @return the analysis context (not {@code null})
   */
  AnalysisContext getDefaultContext();

  /**
   * Answer with all the library sources that are in the project. These include all the sdk and
   * external libraries referenced by code in the project
   *
   * @return the {@link Source}[] for all the libraries that are in the project.
   */
  Source[] getLibrarySources();

  /**
   * Answer the {@link PubFolder}s contained in the receiver.
   *
   * @return an array of zero or more folders (not {@code null}, contains no {@code null}s)
   */
  PubFolder[] getPubFolders();

  /**
   * Answer the Eclipse project associated with this Dart project
   *
   * @return the Eclipse project (not {@code null})
   */
  @Override
  IProject getResource();

  /**
   * Test if the given context is associated with this project.
   *
   * @param context the context to test
   * @return {@code true} if the given context is associated with this project
   */
  boolean isContextInProject(AnalysisContext context);

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

  /**
   * Resolve the given file path to a package uri path, if any
   *
   * @param path the file path for the resource wrt to package structure
   * @return the package name or {@code null} if resource is not in a package
   */
  String resolvePathToPackage(String path);

}

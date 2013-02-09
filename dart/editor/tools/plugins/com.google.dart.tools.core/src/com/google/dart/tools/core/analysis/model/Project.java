package com.google.dart.tools.core.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

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
   * Answer the default analysis context. If the receiver contains a {@link PubFolder} which
   * contains all of the resources contained in the receiver, then the analysis context for that
   * {@link PubFolder} will be the same the receiver's default analysis context.
   * 
   * @return the analysis context (not {@code null})
   */
  AnalysisContext getDefaultContext();

  /**
   * Answer the {@link PubFolder} containing the specified resource.
   * 
   * @param container the container (not {@code null}) in this project
   * @return the pub folder or {@code null} if no pub folder contains this resource
   */
  PubFolder getPubFolder(IContainer container);

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
  IProject getResource();

  /**
   * Answer the resource associated with the specified source.
   * 
   * @param source the source (not {@code null})
   * @return the resource or {@code null} if it could not be determined
   */
  IResource getResourceFor(Source source);

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

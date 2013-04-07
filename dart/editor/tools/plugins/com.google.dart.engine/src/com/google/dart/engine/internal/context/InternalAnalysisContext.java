/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.internal.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.Map;

/**
 * The interface {@code InternalAnalysisContext} defines additional behavior for an analysis context
 * that is required by internal users of the context.
 */
public interface InternalAnalysisContext extends AnalysisContext {
  /**
   * Add the given source with the given information to this context.
   * 
   * @param source the source to be added
   * @param info the information about the source
   */
  public void addSourceInfo(Source source, SourceInfo info);

  /**
   * Return an AST structure corresponding to the given source, but ensure that the structure has
   * not already been resolved and will not be resolved by any other threads or in any other
   * library.
   * 
   * @param source the compilation unit for which an AST structure should be returned
   * @return the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public CompilationUnit computeResolvableCompilationUnit(Source source) throws AnalysisException;

  /**
   * Initialize the specified context by removing the specified sources from the receiver and adding
   * them to the specified context.
   * 
   * @param container the container containing sources that should be removed from this context and
   *          added to the returned context
   * @param newContext the context to be initialized
   * @return the analysis context that was initialized
   */
  public InternalAnalysisContext extractContextInto(SourceContainer container,
      InternalAnalysisContext newContext);

  /**
   * Return a namespace containing mappings for all of the public names defined by the given
   * library.
   * 
   * @param library the library whose public namespace is to be returned
   * @return the public namespace of the given library
   */
  public Namespace getPublicNamespace(LibraryElement library);

  /**
   * Return a namespace containing mappings for all of the public names defined by the library
   * defined by the given source.
   * 
   * @param source the source defining the library whose public namespace is to be returned
   * @return the public namespace corresponding to the library defined by the given source
   * @throws AnalysisException if the public namespace could not be computed
   */
  public Namespace getPublicNamespace(Source source) throws AnalysisException;

  /**
   * Given a table mapping the source for the libraries represented by the corresponding elements to
   * the elements representing the libraries, record those mappings.
   * 
   * @param elementMap a table mapping the source for the libraries represented by the elements to
   *          the elements representing the libraries
   */
  public void recordLibraryElements(Map<Source, LibraryElement> elementMap);

  /**
   * Give the resolution errors and line info associated with the given source, add the information
   * to the cache.
   * 
   * @param source the source with which the information is associated
   * @param errors the resolution errors associated with the source
   * @param lineInfo the line information associated with the source
   */
  public void recordResolutionErrors(Source source, AnalysisError[] errors, LineInfo lineInfo);

  /**
   * Give the resolved compilation unit associated with the given source, add the unit to the cache.
   * 
   * @param source the source with which the unit is associated
   * @param unit the compilation unit associated with the source
   */
  public void recordResolvedCompilationUnit(Source source, CompilationUnit unit);
}

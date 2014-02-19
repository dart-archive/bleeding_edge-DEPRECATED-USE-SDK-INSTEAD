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
import com.google.dart.engine.context.AnalysisContentStatistics;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.cache.SourceEntry;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

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
  public void addSourceInfo(Source source, SourceEntry info);

  /**
   * Return an array containing the sources of the libraries that are exported by the library with
   * the given source. The array will be empty if the given source is invalid, if the given source
   * does not represent a library, or if the library does not export any other libraries.
   * 
   * @param source the source representing the library whose exports are to be returned
   * @return the sources of the libraries that are exported by the given library
   * @throws AnalysisException if the exported libraries could not be computed
   */
  public Source[] computeExportedLibraries(Source source) throws AnalysisException;

  /**
   * Return an array containing the sources of the libraries that are imported by the library with
   * the given source. The array will be empty if the given source is invalid, if the given source
   * does not represent a library, or if the library does not import any other libraries.
   * 
   * @param source the source representing the library whose imports are to be returned
   * @return the sources of the libraries that are imported by the given library
   * @throws AnalysisException if the imported libraries could not be computed
   */
  public Source[] computeImportedLibraries(Source source) throws AnalysisException;

  /**
   * Return an AST structure corresponding to the given source, but ensure that the structure has
   * not already been resolved and will not be resolved by any other threads.
   * 
   * @param source the compilation unit for which an AST structure should be returned
   * @return the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public ResolvableHtmlUnit computeResolvableAngularComponentHtmlUnit(Source source)
      throws AnalysisException;

  /**
   * Return an AST structure corresponding to the given source, but ensure that the structure has
   * not already been resolved and will not be resolved by any other threads or in any other
   * library.
   * 
   * @param source the compilation unit for which an AST structure should be returned
   * @return the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public ResolvableCompilationUnit computeResolvableCompilationUnit(Source source)
      throws AnalysisException;

  /**
   * Return an AST structure corresponding to the given source, but ensure that the structure has
   * not already been resolved and will not be resolved by any other threads.
   * 
   * @param source the compilation unit for which an AST structure should be returned
   * @return the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public ResolvableHtmlUnit computeResolvableHtmlUnit(Source source) throws AnalysisException;

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
   * Returns a statistics about this context.
   */
  public AnalysisContentStatistics getStatistics();

  /**
   * Returns a type provider for this context or throws an exception if dart:core cannot be
   * resolved.
   * 
   * @return the type provider (not {@code null})
   * @throws AnalysisException if dart:core cannot be resolved
   */
  public TypeProvider getTypeProvider() throws AnalysisException;

  /**
   * Return a time-stamped parsed AST for the given source.
   * 
   * @param source the source of the compilation unit for which an AST is to be returned
   * @return a time-stamped AST for the source
   * @throws AnalysisException if the source could not be parsed
   */
  public TimestampedData<CompilationUnit> internalParseCompilationUnit(Source source)
      throws AnalysisException;

  /**
   * Return a time-stamped fully-resolved compilation unit for the given source in the given
   * library.
   * 
   * @param unitSource the source of the compilation unit for which a resolved AST structure is to
   *          be returned
   * @param libraryElement the element representing the library in which the compilation unit is to
   *          be resolved
   * @return a time-stamped fully-resolved compilation unit for the source
   * @throws AnalysisException if the resolved compilation unit could not be computed
   */
  public TimestampedData<CompilationUnit> internalResolveCompilationUnit(Source unitSource,
      LibraryElement libraryElement) throws AnalysisException;

  /**
   * Return a time-stamped token stream for the given source.
   * 
   * @param source the source of the compilation unit for which a token stream is to be returned
   * @return a time-stamped token stream for the source
   * @throws AnalysisException if the token stream could not be computed
   */
  public TimestampedData<Token> internalScanTokenStream(Source source) throws AnalysisException;

  /**
   * Given a table mapping the source for the libraries represented by the corresponding elements to
   * the elements representing the libraries, record those mappings.
   * 
   * @param elementMap a table mapping the source for the libraries represented by the elements to
   *          the elements representing the libraries
   */
  public void recordLibraryElements(Map<Source, LibraryElement> elementMap);
}

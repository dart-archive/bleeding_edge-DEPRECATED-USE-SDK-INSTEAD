/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.parser.HtmlParseResult;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;

/**
 * The interface {@code AnalysisContext} defines the behavior of objects that represent a context in
 * which analysis can be performed. The context includes such information as the version of the SDK
 * being analyzed against as well as the package-root used to resolve 'package:' URI's. (Both of
 * which are known indirectly through the {@link SourceFactory source factory}.)
 * <p>
 * They also represent the state of a given analysis, which includes knowing which sources have been
 * included in the analysis (either directly or indirectly) and the results of the analysis. Some
 * analysis results are cached in order to allow the context to balance between memory usage and
 * performance. TODO(brianwilkerson) Decide how this is reflected in the API: a getFoo() and
 * getOrComputeFoo() pair of methods, or a single getFoo(boolean).
 * <p>
 * Analysis engine allows for having more than one context. This can be used, for example, to
 * perform one analysis based on the state of files on disk and a separate analysis based on the
 * state of those files in open editors. It can also be used to perform an analysis based on a
 * proposed future state, such as the state after a refactoring.
 */
public interface AnalysisContext {
  /**
   * Apply the changes specified by the given change set to this context. Any analysis results that
   * have been invalidated by these changes will be removed.
   * 
   * @param changeSet a description of the changes that are to be applied
   */
  public void applyChanges(ChangeSet changeSet);

  /**
   * Clear any cached information that is dependent on resolution. This method should be invoked if
   * the assumptions used by resolution have changed but the contents of the file have not changed.
   * Use {@link #sourceChanged(Source)} and {@link #sourcesDeleted(SourceContainer)} to indicate
   * when the contents of a file or files have changed.
   */
  @Deprecated
  public void clearResolution();

  /**
   * Call this method when this context is no longer going to be used. At this point, the receiver
   * may choose to push some of its information back into the global cache for consumption by
   * another context for performance.
   */
  @Deprecated
  public void discard();

  /**
   * Create a new context in which analysis can be performed. Any sources in the specified container
   * will be removed from this context and added to the newly created context.
   * 
   * @param container the container containing sources that should be removed from this context and
   *          added to the returned context
   * @return the analysis context that was created
   */
  public AnalysisContext extractContext(SourceContainer container);

  /**
   * Return the element referenced by the given location.
   * 
   * @param location the reference describing the element to be returned
   * @return the element referenced by the given location
   */
  public Element getElement(ElementLocation location);

  /**
   * Return an array containing all of the errors associated with the given source. The array will
   * be empty if the source is not known to this context or if there are no errors in the source.
   * 
   * @param source the source whose errors are to be returned
   * @return all of the errors associated with the given source
   * @throws AnalysisException if the errors could not be determined because the analysis could not
   *           be performed
   */
  public AnalysisError[] getErrors(Source source) throws AnalysisException;

  /**
   * Return the element model corresponding to the HTML file defined by the given source.
   * 
   * @param source the source defining the HTML file whose element model is to be returned
   * @return the element model corresponding to the HTML file defined by the given source
   */
  public HtmlElement getHtmlElement(Source source);

  /**
   * Return an array containing all of the sources known to this context that represent HTML files.
   * 
   * @return the sources known to this context that represent HTML files
   */
  public Source[] getHtmlSources();

  /**
   * Return the kind of the given source if it is already known, or {@code null} if the kind is not
   * already known.
   * 
   * @param source the source whose kind is to be returned
   * @return the kind of the given source
   * @see #getOrComputeKindOf(Source)
   */
  public SourceKind getKnownKindOf(Source source);

  /**
   * Return an array containing all of the sources known to this context that represent the defining
   * compilation unit of a library that can be run within a browser. The sources that are returned
   * represent libraries that have a 'main' method and are either referenced by an HTML file or
   * import, directly or indirectly, a client-only library.
   * 
   * @return the sources known to this context that represent the defining compilation unit of a
   *         library that can be run within a browser
   */
  public Source[] getLaunchableClientLibrarySources();

  /**
   * Return an array containing all of the sources known to this context that represent the defining
   * compilation unit of a library that can be run outside of a browser.
   * 
   * @return the sources known to this context that represent the defining compilation unit of a
   *         library that can be run outside of a browser
   */
  public Source[] getLaunchableServerLibrarySources();

  /**
   * Return the sources for the defining compilation units of any libraries of which the given
   * source is a part. The array will normally contain a single library because most Dart sources
   * are only included in a single library, but it is possible to have a part that is contained in
   * multiple identically named libraries. If the source represents the defining compilation unit of
   * a library, then the returned array will contain the given source as its only element. If the
   * source does not represent a Dart source or is not known to this context, the returned array
   * will be empty.
   * 
   * @param source the source contained in the returned libraries
   * @return the sources for the libraries containing the given source
   */
  public Source[] getLibrariesContaining(Source source);

  /**
   * Return the element model corresponding to the library defined by the given source. If the
   * element model does not yet exist it will be created. The process of creating an element model
   * for a library can long-running, depending on the size of the library and the number of
   * libraries that are imported into it that also need to have a model built for them.
   * 
   * @param source the source defining the library whose element model is to be returned
   * @return the element model corresponding to the library defined by the given source or
   *         {@code null} if the element model could not be determined because the analysis could
   *         not be performed
   */
  public LibraryElement getLibraryElement(Source source);

  /**
   * Return the element model corresponding to the library defined by the given source, or
   * {@code null} if the element model does not currently exist or if the analysis could not be
   * performed.
   * 
   * @param source the source defining the library whose element model is to be returned
   * @return the element model corresponding to the library defined by the given source
   */
  public LibraryElement getLibraryElementOrNull(Source source);

  /**
   * Return an array containing all of the sources known to this context that represent the defining
   * compilation unit of a library.
   * 
   * @return the sources known to this context that represent the defining compilation unit of a
   *         library
   */
  public Source[] getLibrarySources();

  /**
   * Return the kind of the given source, computing it's kind if it is not already known.
   * 
   * @param source the source whose kind is to be returned
   * @return the kind of the given source
   * @see #getKnownKindOf(Source)
   */
  public SourceKind getOrComputeKindOf(Source source);

  /**
   * Return the source factory used to create the sources that can be analyzed in this context.
   * 
   * @return the source factory used to create the sources that can be analyzed in this context
   */
  public SourceFactory getSourceFactory();

  /**
   * Add the sources contained in the specified context to this context's collection of sources.
   * This method is called when an existing context's pubspec has been removed, and the contained
   * sources should be reanalyzed as part of this context.
   * 
   * @param context the context being merged
   */
  public void mergeContext(AnalysisContext context);

  /**
   * Parse a single source to produce an AST structure. The resulting AST structure may or may not
   * be resolved, and may have a slightly different structure depending upon whether it is resolved.
   * 
   * @param source the source to be parsed
   * @return the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  @Deprecated
  public CompilationUnit parse(Source source) throws AnalysisException;

  /**
   * Parse a single HTML source to produce an AST structure. The resulting HTML AST structure may or
   * may not be resolved, and may have a slightly different structure depending upon whether it is
   * resolved.
   * 
   * @param source the HTML source to be parsed
   * @return the parse result (not {@code null})
   * @throws AnalysisException if the analysis could not be performed
   */
  @Deprecated
  public HtmlParseResult parseHtml(Source source) throws AnalysisException;

  /**
   * Perform the next unit of work required to keep the analysis results up-to-date and return
   * information about the consequent changes to the analysis results. If there were no results the
   * returned array will be empty. This method can be long running.
   * 
   * @return an array containing notices of changes to the analysis results
   */
  public ChangeNotice[] performAnalysisTask();

  /**
   * Parse and resolve a single source within the given context to produce a fully resolved AST.
   * 
   * @param source the source to be parsed and resolved
   * @param library the library defining the context in which the source file is to be resolved
   * @return the result of resolving the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public CompilationUnit resolve(Source source, LibraryElement library) throws AnalysisException;

  /**
   * Set the source factory used to create the sources that can be analyzed in this context to the
   * given source factory. Clients can safely assume that all analysis results have been
   * invalidated.
   * 
   * @param factory the source factory used to create the sources that can be analyzed in this
   *          context
   */
  public void setSourceFactory(SourceFactory factory);

  /**
   * Given a collection of sources with content that has changed, return an {@link Iterable}
   * identifying the sources that need to be resolved.
   * 
   * @param changedSources an array of sources (not {@code null}, contains no {@code null}s)
   * @return An iterable returning the sources to be resolved
   */
  // Soon to be deprecated, but the replacement isn't quite ready yet
  // * @deprecated Use the ChangeResult returned by {@link #changed(ChangeSet)}.
  @Deprecated
  public Iterable<Source> sourcesToResolve(Source[] changedSources);
}

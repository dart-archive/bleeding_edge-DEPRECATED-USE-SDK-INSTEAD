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
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * The interface {@code AnalysisContext} defines the behavior of objects that represent a context in
 * which a single analysis can be performed and incrementally maintained. The context includes such
 * information as the version of the SDK being analyzed against as well as the package-root used to
 * resolve 'package:' URI's. (Both of which are known indirectly through the {@link SourceFactory
 * source factory}.)
 * <p>
 * An analysis context also represents the state of the analysis, which includes knowing which
 * sources have been included in the analysis (either directly or indirectly) and the results of the
 * analysis. Sources must be added and removed from the context using the method
 * {@link #applyChanges(ChangeSet)}, which is also used to notify the context when sources have been
 * modified and, consequently, previously known results might have been invalidated.
 * <p>
 * There are two ways to access the results of the analysis. The most common is to use one of the
 * 'get' methods to access the results. The 'get' methods have the advantage that they will always
 * return quickly, but have the disadvantage that if the results are not currently available they
 * will return either nothing or in some cases an incomplete result. The second way to access
 * results is by using one of the 'compute' methods. The 'compute' methods will always attempt to
 * compute the requested results but might block the caller for a significant period of time.
 * <p>
 * When results have been invalidated, have never been computed (as is the case for newly added
 * sources), or have been removed from the cache, they are <b>not</b> automatically recreated. They
 * will only be recreated if one of the 'compute' methods is invoked.
 * <p>
 * However, this is not always acceptable. Some clients need to keep the analysis results
 * up-to-date. For such clients there is a mechanism that allows them to incrementally perform
 * needed analysis and get notified of the consequent changes to the analysis results. This
 * mechanism is realized by the method {@link #performAnalysisTask()}.
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
   * Return an array containing all of the errors associated with the given source. If the errors
   * are not already known then the source will be analyzed in order to determine the errors
   * associated with it.
   * 
   * @param source the source whose errors are to be returned
   * @return all of the errors associated with the given source
   * @throws AnalysisException if the errors could not be determined because the analysis could not
   *           be performed
   * @see #getErrors(Source)
   */
  public AnalysisError[] computeErrors(Source source) throws AnalysisException;

  /**
   * Return the element model corresponding to the HTML file defined by the given source. If the
   * element model does not yet exist it will be created. The process of creating an element model
   * for an HTML file can long-running, depending on the size of the file and the number of
   * libraries that are defined in it (via script tags) that also need to have a model built for
   * them.
   * 
   * @param source the source defining the HTML file whose element model is to be returned
   * @return the element model corresponding to the HTML file defined by the given source
   * @throws AnalysisException if the element model could not be determined because the analysis
   *           could not be performed
   * @see #getHtmlElement(Source)
   */
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException;

  /**
   * Return the kind of the given source, computing it's kind if it is not already known. Return
   * {@link SourceKind#UNKNOWN} if the source is not contained in this context.
   * 
   * @param source the source whose kind is to be returned
   * @return the kind of the given source
   * @see #getKindOf(Source)
   */
  public SourceKind computeKindOf(Source source);

  /**
   * Return the element model corresponding to the library defined by the given source. If the
   * element model does not yet exist it will be created. The process of creating an element model
   * for a library can long-running, depending on the size of the library and the number of
   * libraries that are imported into it that also need to have a model built for them.
   * 
   * @param source the source defining the library whose element model is to be returned
   * @return the element model corresponding to the library defined by the given source
   * @throws AnalysisException if the element model could not be determined because the analysis
   *           could not be performed
   * @see #getLibraryElement(Source)
   */
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException;

  /**
   * Return the line information for the given source, or {@code null} if the source is not of a
   * recognized kind (neither a Dart nor HTML file). If the line information was not previously
   * known it will be created. The line information is used to map offsets from the beginning of the
   * source to line and column pairs.
   * 
   * @param source the source whose line information is to be returned
   * @return the line information for the given source
   * @throws AnalysisException if the line information could not be determined because the analysis
   *           could not be performed
   * @see #getLineInfo(Source)
   */
  public LineInfo computeLineInfo(Source source) throws AnalysisException;

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
   * Return the element referenced by the given location, or {@code null} if the element is not
   * immediately available or if there is no element with the given location. The latter condition
   * can occur, for example, if the location describes an element from a different context or if the
   * element has been removed from this context as a result of some change since it was originally
   * obtained.
   * 
   * @param location the reference describing the element to be returned
   * @return the element referenced by the given location
   */
  public Element getElement(ElementLocation location);

  /**
   * Return an analysis error info containing the array of all of the errors and the line info
   * associated with the given source. The array of errors will be empty if the source is not known
   * to this context or if there are no errors in the source. The errors contained in the array can
   * be incomplete.
   * 
   * @param source the source whose errors are to be returned
   * @return all of the errors associated with the given source and the line info
   * @see #computeErrors(Source)
   */
  public AnalysisErrorInfo getErrors(Source source);

  /**
   * Return the element model corresponding to the HTML file defined by the given source, or
   * {@code null} if the source does not represent an HTML file, the element representing the file
   * has not yet been created, or the analysis of the HTML file failed for some reason.
   * 
   * @param source the source defining the HTML file whose element model is to be returned
   * @return the element model corresponding to the HTML file defined by the given source
   * @see #computeHtmlElement(Source)
   */
  public HtmlElement getHtmlElement(Source source);

  /**
   * Return the sources for the HTML files that reference the given compilation unit. If the source
   * does not represent a Dart source or is not known to this context, the returned array will be
   * empty. The contents of the array can be incomplete.
   * 
   * @param source the source referenced by the returned HTML files
   * @return the sources for the HTML files that reference the given compilation unit
   */
  public Source[] getHtmlFilesReferencing(Source source);

  /**
   * Return an array containing all of the sources known to this context that represent HTML files.
   * The contents of the array can be incomplete.
   * 
   * @return the sources known to this context that represent HTML files
   */
  public Source[] getHtmlSources();

  /**
   * Return the kind of the given source, or {@code null} if the kind is not known to this context.
   * 
   * @param source the source whose kind is to be returned
   * @return the kind of the given source
   * @see #computeKindOf(Source)
   */
  public SourceKind getKindOf(Source source);

  /**
   * Return an array containing all of the sources known to this context that represent the defining
   * compilation unit of a library that can be run within a browser. The sources that are returned
   * represent libraries that have a 'main' method and are either referenced by an HTML file or
   * import, directly or indirectly, a client-only library. The contents of the array can be
   * incomplete.
   * 
   * @return the sources known to this context that represent the defining compilation unit of a
   *         library that can be run within a browser
   */
  public Source[] getLaunchableClientLibrarySources();

  /**
   * Return an array containing all of the sources known to this context that represent the defining
   * compilation unit of a library that can be run outside of a browser. The contents of the array
   * can be incomplete.
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
   * will be empty. The contents of the array can be incomplete.
   * 
   * @param source the source contained in the returned libraries
   * @return the sources for the libraries containing the given source
   */
  public Source[] getLibrariesContaining(Source source);

  /**
   * Return the element model corresponding to the library defined by the given source, or
   * {@code null} if the element model does not currently exist or if the library cannot be analyzed
   * for some reason.
   * 
   * @param source the source defining the library whose element model is to be returned
   * @return the element model corresponding to the library defined by the given source
   */
  public LibraryElement getLibraryElement(Source source);

  /**
   * Return an array containing all of the sources known to this context that represent the defining
   * compilation unit of a library. The contents of the array can be incomplete.
   * 
   * @return the sources known to this context that represent the defining compilation unit of a
   *         library
   */
  public Source[] getLibrarySources();

  /**
   * Return the line information for the given source, or {@code null} if the line information is
   * not known. The line information is used to map offsets from the beginning of the source to line
   * and column pairs.
   * 
   * @param source the source whose line information is to be returned
   * @return the line information for the given source
   * @see #computeLineInfo(Source)
   */
  public LineInfo getLineInfo(Source source);

  /**
   * Return a fully resolved AST for a single compilation unit within the given library, or
   * {@code null} if the resolved AST is not already computed.
   * 
   * @param unitSource the source of the compilation unit
   * @param library the library containing the compilation unit
   * @return a fully resolved AST for the compilation unit
   * @see #resolveCompilationUnit(Source, LibraryElement)
   */
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library);

  /**
   * Return a fully resolved AST for a single compilation unit within the given library, or
   * {@code null} if the resolved AST is not already computed.
   * 
   * @param unitSource the source of the compilation unit
   * @param librarySource the source of the defining compilation unit of the library containing the
   *          compilation unit
   * @return a fully resolved AST for the compilation unit
   * @see #resolveCompilationUnit(Source, Source)
   */
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, Source librarySource);

  /**
   * Return the source factory used to create the sources that can be analyzed in this context.
   * 
   * @return the source factory used to create the sources that can be analyzed in this context
   */
  public SourceFactory getSourceFactory();

  /**
   * Return {@code true} if the given source is known to be the defining compilation unit of a
   * library that can be run on a client (references 'dart:html', either directly or indirectly).
   * <p>
   * <b>Note:</b> In addition to the expected case of returning {@code false} if the source is known
   * to be a library that cannot be run on a client, this method will also return {@code false} if
   * the source is not known to be a library or if we do not know whether it can be run on a client.
   * 
   * @param librarySource the source being tested
   * @return {@code true} if the given source is known to be a library that can be run on a client
   */
  public boolean isClientLibrary(Source librarySource);

  /**
   * Return {@code true} if the given source is known to be the defining compilation unit of a
   * library that can be run on the server (does not reference 'dart:html', either directly or
   * indirectly).
   * <p>
   * <b>Note:</b> In addition to the expected case of returning {@code false} if the source is known
   * to be a library that cannot be run on the server, this method will also return {@code false} if
   * the source is not known to be a library or if we do not know whether it can be run on the
   * server.
   * 
   * @param librarySource the source being tested
   * @return {@code true} if the given source is known to be a library that can be run on the server
   */
  public boolean isServerLibrary(Source librarySource);

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
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException;

  /**
   * Parse a single HTML source to produce an AST structure. The resulting HTML AST structure may or
   * may not be resolved, and may have a slightly different structure depending upon whether it is
   * resolved.
   * 
   * @param source the HTML source to be parsed
   * @return the parse result (not {@code null})
   * @throws AnalysisException if the analysis could not be performed
   */
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException;

  /**
   * Perform the next unit of work required to keep the analysis results up-to-date and return
   * information about the consequent changes to the analysis results. If there were no results the
   * returned array will be empty. If there are no more units of work required, then this method
   * returns {@code null}. This method can be long running.
   * 
   * @return an array containing notices of changes to the analysis results
   */
  public ChangeNotice[] performAnalysisTask();

  /**
   * Parse and resolve a single source within the given context to produce a fully resolved AST.
   * 
   * @param unitSource the source to be parsed and resolved
   * @param library the library containing the source to be resolved
   * @return the result of resolving the AST structure representing the content of the source in the
   *         context of the given library
   * @throws AnalysisException if the analysis could not be performed
   * @see #getResolvedCompilationUnit(Source, LibraryElement)
   */
  public CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library)
      throws AnalysisException;

  /**
   * Parse and resolve a single source within the given context to produce a fully resolved AST.
   * 
   * @param unitSource the source to be parsed and resolved
   * @param librarySource the source of the defining compilation unit of the library containing the
   *          source to be resolved
   * @return the result of resolving the AST structure representing the content of the source in the
   *         context of the given library
   * @throws AnalysisException if the analysis could not be performed
   * @see #getResolvedCompilationUnit(Source, Source)
   */
  public CompilationUnit resolveCompilationUnit(Source unitSource, Source librarySource)
      throws AnalysisException;

  /**
   * Parse and resolve a single source within the given context to produce a fully resolved AST.
   * 
   * @param htmlSource the source to be parsed and resolved
   * @return the result of resolving the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public HtmlUnit resolveHtmlUnit(Source htmlSource) throws AnalysisException;

  /**
   * Set the contents of the given source to the given contents and mark the source as having
   * changed. This has the effect of overriding the default contents of the source. If the contents
   * are {@code null} the override is removed so that the default contents will be returned.
   * 
   * @param source the source whose contents are being overridden
   * @param contents the new contents of the source
   */
  public void setContents(Source source, String contents);

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

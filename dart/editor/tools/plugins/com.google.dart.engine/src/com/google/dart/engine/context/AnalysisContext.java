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
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.html.scanner.HtmlScanResult;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;

/**
 * The interface {@code AnalysisContext} defines the behavior of objects that represent a context in
 * which analysis can be performed. The context includes such information as the version of the SDK
 * being analyzed against as well as the package-root used to resolve 'package:' URI's. (The latter
 * is included indirectly through the {@link SourceFactory source factory}.)
 * <p>
 * Analysis engine allows for having more than one context. This can be used, for example, to
 * perform one analysis based on the state of files on disk and a separate analysis based on the
 * state of those files in open editors. It can also be used to perform an analysis based on a
 * proposed future state, such as the state after a refactoring.
 */
public interface AnalysisContext {
  /**
   * Respond to the given set of changes by removing any cached information that might now be
   * out-of-date. The result indicates what operations need to be performed as a result of this
   * change without actually performing those operations.
   * 
   * @param changeSet a description of the changes that have occurred
   * @return a result (not {@code null}) indicating operations to be performed
   */
  public ChangeResult changed(ChangeSet changeSet);

  /**
   * Clear any cached information that is dependent on resolution. This method should be invoked if
   * the assumptions used by resolution have changed but the contents of the file have not changed.
   * Use {@link #sourceChanged(Source)} and {@link #sourcesDeleted(SourceContainer)} to indicate
   * when the contents of a file or files have changed.
   */
  // TODO (danrubel): review the situations under which this method is and should be called
  // with an eye towards removing this method if it is not useful.
  public void clearResolution();

  /**
   * Call this method when this context is no longer going to be used. At this point, the receiver
   * may choose to push some of its information back into the global cache for consumption by
   * another context for performance.
   */
  // TODO (danrubel): review the situations under which this method is and should be called
  // with an eye towards removing this method if it is not useful.
  public void discard();

  /**
   * Create a new context in which analysis can be performed. Any sources in the specified directory
   * in the receiver will be removed from the receiver and added to the newly created context.
   * 
   * @param directory the directory (not {@code null}) containing sources that should be removed
   *          from the receiver and added to the returned context
   * @return the analysis context that was created (not {@code null})
   */
  // TODO (danrubel): review the situations under which this method is and should be called
  // with an eye towards removing this method if it is not useful.
  public AnalysisContext extractAnalysisContext(SourceContainer container);

  /**
   * Return the element referenced by the given location.
   * 
   * @param location the reference describing the element to be returned
   * @return the element referenced by the given location
   */
  public Element getElement(ElementLocation location);

  /**
   * Return an array containing all of the errors associated with the given source.
   * 
   * @param source the source whose errors are to be returned
   * @return all of the errors associated with the given source
   * @throws AnalysisException if the errors could not be determined because the analysis could not
   *           be performed
   */
  // TODO (danrubel): review the situations under which this method is and should be called
  // with an eye towards removing this method if it is not useful.
  public AnalysisError[] getErrors(Source source) throws AnalysisException;

  /**
   * Parse and build an element model for the HTML file defined by the given source.
   * 
   * @param source the source defining the HTML file whose element model is to be returned
   * @return the element model corresponding to the HTML file defined by the given source
   */
  public HtmlElement getHtmlElement(Source source);

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
   * {@code null} if the element model does not yet exist.
   * 
   * @param source the source defining the library whose element model is to be returned
   * @return the element model corresponding to the library defined by the given source
   */
  public LibraryElement getLibraryElementOrNull(Source source);

  /**
   * Return the kind of the given source, computing it's kind if it is not already known.
   * 
   * @param source the source whose kind is to be returned
   * @return the kind of the given source
   * @see #getKnownKindOf(Source)
   */
  public SourceKind getOrComputeKindOf(Source source);

  /**
   * Return an array containing all of the parsing errors associated with the given source.
   * 
   * @param source the source whose errors are to be returned
   * @return all of the parsing errors associated with the given source
   * @throws AnalysisException if the errors could not be determined because the analysis could not
   *           be performed
   */
  // TODO (danrubel): review the situations under which this method is and should be called
  // with an eye towards removing this method if it is not useful.
  public AnalysisError[] getParsingErrors(Source source) throws AnalysisException;

  /**
   * Return an array containing all of the resolution errors associated with the given source.
   * 
   * @param source the source whose errors are to be returned
   * @return all of the resolution errors associated with the given source
   * @throws AnalysisException if the errors could not be determined because the analysis could not
   *           be performed
   */
  // TODO (danrubel): review the situations under which this method is and should be called
  // with an eye towards removing this method if it is not useful.
  public AnalysisError[] getResolutionErrors(Source source) throws AnalysisException;

  /**
   * Return the source factory used to create the sources that can be analyzed in this context.
   * 
   * @return the source factory used to create the sources that can be analyzed in this context
   */
  public SourceFactory getSourceFactory();

  /**
   * Add the sources contained in the specified context to the receiver's collection of sources.
   * This method is called when an existing context's pubspec has been removed, and the contained
   * sources should be reanalyzed as part of the receiver.
   * 
   * @param context the context being merged (not {@code null})
   */
  // TODO (danrubel): review the situations under which this method is and should be called
  // with an eye towards removing this method if it is not useful.
  public void mergeAnalysisContext(AnalysisContext context);

  /**
   * Parse a single source to produce an AST structure. The resulting AST structure may or may not
   * be resolved, and may have a slightly different structure depending upon whether it is resolved.
   * 
   * @param source the source to be parsed
   * @return the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public CompilationUnit parse(Source source) throws AnalysisException;

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
   * Scan a single source to produce a token stream.
   * 
   * @param source the source to be scanned
   * @param errorListener the listener to which errors should be reported
   * @return the head of the token stream representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public Token scan(Source source, AnalysisErrorListener errorListener) throws AnalysisException;

  /**
   * Scan a single source to produce an HTML token stream.
   * 
   * @param source the source to be scanned
   * @return the scan result (not {@code null})
   * @throws AnalysisException if the analysis could not be performed
   */
  public HtmlScanResult scanHtml(Source source) throws AnalysisException;

  /**
   * Set the source factory used to create the sources that can be analyzed in this context to the
   * given source factory.
   * 
   * @param sourceFactory the source factory used to create the sources that can be analyzed in this
   *          context
   */
  public void setSourceFactory(SourceFactory sourceFactory);

  /**
   * Given a collection of sources with content that has changed, return an {@link Iterable}
   * identifying the sources that need to be resolved.
   * 
   * @param changedSources an array of sources (not {@code null}, contains no {@code null}s)
   * @return An iterable returning the sources to be resolved
   */
  // Soon to be deprecated, but the replacement isn't quite ready yet
  // * @deprecated Use the ChangeResult returned by {@link #changed(ChangeSet)}.
  // @Deprecated
  public Iterable<Source> sourcesToResolve(Source[] changedSources);
}

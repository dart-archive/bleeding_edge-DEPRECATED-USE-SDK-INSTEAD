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
import com.google.dart.engine.constant.DeclaredVariables;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.Source.ContentReceiver;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.util.List;

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
   * Add the given listener to the list of objects that are to be notified when various analysis
   * results are produced in this context.
   * 
   * @param listener the listener to be added
   */
  public void addListener(AnalysisListener listener);

  /**
   * Apply the given delta to change the level of analysis that will be performed for the sources
   * known to this context.
   * 
   * @param delta a description of the level of analysis that should be performed for some sources
   */
  public void applyAnalysisDelta(AnalysisDelta delta);

  /**
   * Apply the changes specified by the given change set to this context. Any analysis results that
   * have been invalidated by these changes will be removed.
   * 
   * @param changeSet a description of the changes that are to be applied
   */
  public void applyChanges(ChangeSet changeSet);

  /**
   * Return the documentation comment for the given element as it appears in the original source
   * (complete with the beginning and ending delimiters) for block documentation comments, or lines
   * starting with {@code "///"} and separated with {@code "\n"} characters for end-of-line
   * documentation comments, or {@code null} if the element does not have a documentation comment
   * associated with it. This can be a long-running operation if the information needed to access
   * the comment is not cached.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param element the element whose documentation comment is to be returned
   * @return the element's documentation comment
   * @throws AnalysisException if the documentation comment could not be determined because the
   *           analysis could not be performed
   */
  public String computeDocumentationComment(Element element) throws AnalysisException;

  /**
   * Return an array containing all of the errors associated with the given source. If the errors
   * are not already known then the source will be analyzed in order to determine the errors
   * associated with it.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
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
   * for an HTML file can be long-running, depending on the size of the file and the number of
   * libraries that are defined in it (via script tags) that also need to have a model built for
   * them.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
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
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
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
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
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
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the source whose line information is to be returned
   * @return the line information for the given source
   * @throws AnalysisException if the line information could not be determined because the analysis
   *           could not be performed
   * @see #getLineInfo(Source)
   */
  public LineInfo computeLineInfo(Source source) throws AnalysisException;

  /**
   * Notifies the context that the client is going to stop using this context.
   */
  public void dispose();

  /**
   * Return {@code true} if the given source exists.
   * <p>
   * This method should be used rather than the method {@link Source#exists()} because contexts can
   * have local overrides of the content of a source that the source is not aware of and a source
   * with local content is considered to exist even if there is no file on disk.
   * 
   * @param source the source whose modification stamp is to be returned
   * @return {@code true} if the source exists
   */
  public boolean exists(Source source);

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
   * Return the set of analysis options controlling the behavior of this context. Clients should not
   * modify the returned set of options. The options should only be set by invoking the method
   * {@link #setAnalysisOptions(AnalysisOptions)}.
   * 
   * @return the set of analysis options controlling the behavior of this context
   */
  public AnalysisOptions getAnalysisOptions();

  /**
   * Return the Angular application that contains the HTML file defined by the given source, or
   * {@code null} if the source does not represent an HTML file, the Angular application containing
   * the file has not yet been resolved, or the analysis of the HTML file failed for some reason.
   * 
   * @param htmlSource the source defining the HTML file
   * @return the Angular application that contains the HTML file defined by the given source
   */
  public AngularApplication getAngularApplicationWithHtml(Source htmlSource);

  /**
   * Return the element model corresponding to the compilation unit defined by the given source in
   * the library defined by the given source, or {@code null} if the element model does not
   * currently exist or if the library cannot be analyzed for some reason.
   * 
   * @param unitSource the source of the compilation unit
   * @param librarySource the source of the defining compilation unit of the library containing the
   *          compilation unit
   * @return the element model corresponding to the compilation unit defined by the given source
   */
  public CompilationUnitElement getCompilationUnitElement(Source unitSource, Source librarySource);

  /**
   * Get the contents and timestamp of the given source.
   * <p>
   * This method should be used rather than the method {@link Source#getContents()} because contexts
   * can have local overrides of the content of a source that the source is not aware of.
   * 
   * @param source the source whose content is to be returned
   * @return the contents and timestamp of the source
   * @throws Exception if the contents of the source could not be accessed
   */
  public TimestampedData<CharSequence> getContents(Source source) throws Exception;

  /**
   * Get the contents of the given source and pass it to the given content receiver.
   * <p>
   * This method should be used rather than the method
   * {@link Source#getContentsToReceiver(ContentReceiver)} because contexts can have local overrides
   * of the content of a source that the source is not aware of.
   * 
   * @param source the source whose content is to be returned
   * @param receiver the content receiver to which the content of the source will be passed
   * @throws Exception if the contents of the source could not be accessed
   */
  @Deprecated
  @DartOmit
  public void getContentsToReceiver(Source source, ContentReceiver receiver) throws Exception;

  /**
   * Return the set of declared variables used when computing constant values.
   * 
   * @return the set of declared variables used when computing constant values
   */
  public DeclaredVariables getDeclaredVariables();

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
   * Return the sources for the defining compilation units of any libraries that depend on the given
   * library. One library depends on another if it either imports or exports that library.
   * 
   * @param librarySource the source for the defining compilation unit of the library being depended
   *          on
   * @return the sources for the libraries that depend on the given library
   */
  public Source[] getLibrariesDependingOn(Source librarySource);

  /**
   * Return the sources for the defining compilation units of any libraries that are referenced from
   * the given HTML file.
   * 
   * @param htmlSource the source for the HTML file
   * @return the sources for the libraries that are referenced by the given HTML file
   */
  public Source[] getLibrariesReferencedFromHtml(Source htmlSource);

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
   * Return the modification stamp for the given source. A modification stamp is a non-negative
   * integer with the property that if the contents of the source have not been modified since the
   * last time the modification stamp was accessed then the same value will be returned, but if the
   * contents of the source have been modified one or more times (even if the net change is zero)
   * the stamps will be different.
   * <p>
   * This method should be used rather than the method {@link Source#getModificationStamp()} because
   * contexts can have local overrides of the content of a source that the source is not aware of.
   * 
   * @param source the source whose modification stamp is to be returned
   * @return the modification stamp for the source
   */
  public long getModificationStamp(Source source);

  /**
   * Return an array containing all of the sources known to this context and their resolution state
   * is not valid or flush. So, these sources are not safe to update during refactoring, because we
   * may be don't know all the references in them.
   * 
   * @return the sources known to this context and are not safe for refactoring
   */
  public Source[] getRefactoringUnsafeSources();

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
   * Return a fully resolved HTML unit, or {@code null} if the resolved unit is not already
   * computed.
   * 
   * @param htmlSource the source of the HTML unit
   * @return a fully resolved HTML unit
   * @see #resolveHtmlUnit(Source)
   */
  public HtmlUnit getResolvedHtmlUnit(Source htmlSource);

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
   * Returns {@code true} if this context was disposed using {@link #dispose()}.
   * 
   * @return {@code true} if this context was disposed
   */
  public boolean isDisposed();

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
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
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
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param source the HTML source to be parsed
   * @return the parse result (not {@code null})
   * @throws AnalysisException if the analysis could not be performed
   */
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException;

  /**
   * Perform the next unit of work required to keep the analysis results up-to-date and return
   * information about the consequent changes to the analysis results. This method can be long
   * running.
   * 
   * @return the results of performing the analysis
   */
  public AnalysisResult performAnalysisTask();

  /**
   * Remove the given listener from the list of objects that are to be notified when various
   * analysis results are produced in this context.
   * 
   * @param listener the listener to be removed
   */
  public void removeListener(AnalysisListener listener);

  /**
   * Parse and resolve a single source within the given context to produce a fully resolved AST.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
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
   * Return the resolved AST structure, or {@code null} if the source could not be either parsed or
   * resolved.
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
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
   * <p>
   * <b>Note:</b> This method cannot be used in an async environment.
   * 
   * @param htmlSource the source to be parsed and resolved
   * @return the result of resolving the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  public HtmlUnit resolveHtmlUnit(Source htmlSource) throws AnalysisException;

  /**
   * Set the set of analysis options controlling the behavior of this context to the given options.
   * Clients can safely assume that all necessary analysis results have been invalidated.
   * 
   * @param options the set of analysis options that will control the behavior of this context
   */
  public void setAnalysisOptions(AnalysisOptions options);

  /**
   * Set the order in which sources will be analyzed by {@link #performAnalysisTask()} to match the
   * order of the sources in the given list. If a source that needs to be analyzed is not contained
   * in the list, then it will be treated as if it were at the end of the list. If the list is empty
   * (or {@code null}) then no sources will be given priority over other sources.
   * <p>
   * Changes made to the list after this method returns will <b>not</b> be reflected in the priority
   * order.
   * 
   * @param sources the sources to be given priority over other sources
   */
  public void setAnalysisPriorityOrder(List<Source> sources);

  /**
   * Set the contents of the given source to the given contents and mark the source as having
   * changed. The additional offset and length information is used by the context to determine what
   * reanalysis is necessary.
   * 
   * @param source the source whose contents are being overridden
   * @param contents the text to replace the range in the current contents
   * @param offset the offset into the current contents
   * @param oldLength the number of characters in the original contents that were replaced
   * @param newLength the number of characters in the replacement text
   */
  public void setChangedContents(Source source, String contents, int offset, int oldLength,
      int newLength);

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
}

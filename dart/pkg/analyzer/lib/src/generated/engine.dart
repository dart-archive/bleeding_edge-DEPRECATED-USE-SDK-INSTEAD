// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// This code was auto-generated, is not intended to be edited, and is subject to
// significant change. Please see the README file for more information.

library engine;

import 'java_core.dart';
import 'java_engine.dart';
import 'utilities_collection.dart';
import 'utilities_general.dart';
import 'instrumentation.dart';
import 'error.dart';
import 'source.dart';
import 'scanner.dart';
import 'ast.dart';
import 'parser.dart' show Parser, IncrementalParser;
import 'sdk.dart' show DartSdk;
import 'element.dart';
import 'resolver.dart';
import 'html.dart' as ht;

/**
 * The unique instance of the class `AnalysisEngine` serves as the entry point for the
 * functionality provided by the analysis engine.
 */
class AnalysisEngine {
  /**
   * The suffix used for Dart source files.
   */
  static String SUFFIX_DART = "dart";

  /**
   * The short suffix used for HTML files.
   */
  static String SUFFIX_HTM = "htm";

  /**
   * The long suffix used for HTML files.
   */
  static String SUFFIX_HTML = "html";

  /**
   * The unique instance of this class.
   */
  static AnalysisEngine _UniqueInstance = new AnalysisEngine();

  /**
   * Return the unique instance of this class.
   *
   * @return the unique instance of this class
   */
  static AnalysisEngine get instance => _UniqueInstance;

  /**
   * Return `true` if the given file name is assumed to contain Dart source code.
   *
   * @param fileName the name of the file being tested
   * @return `true` if the given file name is assumed to contain Dart source code
   */
  static bool isDartFileName(String fileName) {
    if (fileName == null) {
      return false;
    }
    return javaStringEqualsIgnoreCase(FileNameUtilities.getExtension(fileName), SUFFIX_DART);
  }

  /**
   * Return `true` if the given file name is assumed to contain HTML.
   *
   * @param fileName the name of the file being tested
   * @return `true` if the given file name is assumed to contain HTML
   */
  static bool isHtmlFileName(String fileName) {
    if (fileName == null) {
      return false;
    }
    String extension = FileNameUtilities.getExtension(fileName);
    return javaStringEqualsIgnoreCase(extension, SUFFIX_HTML) || javaStringEqualsIgnoreCase(extension, SUFFIX_HTM);
  }

  /**
   * The logger that should receive information about errors within the analysis engine.
   */
  Logger _logger = Logger.NULL;

  /**
   * Create a new context in which analysis can be performed.
   *
   * @return the analysis context that was created
   */
  AnalysisContext createAnalysisContext() {
    //
    // If instrumentation is ignoring data, return an uninstrumented analysis context.
    //
    if (Instrumentation.isNullLogger) {
      return new DelegatingAnalysisContextImpl();
    }
    return new InstrumentedAnalysisContextImpl.con1(new DelegatingAnalysisContextImpl());
  }

  /**
   * Return the logger that should receive information about errors within the analysis engine.
   *
   * @return the logger that should receive information about errors within the analysis engine
   */
  Logger get logger => _logger;

  /**
   * Set the logger that should receive information about errors within the analysis engine to the
   * given logger.
   *
   * @param logger the logger that should receive information about errors within the analysis
   *          engine
   */
  void set logger(Logger logger) {
    this._logger = logger == null ? Logger.NULL : logger;
  }
}

/**
 * Container with statistics about the [AnalysisContext].
 */
abstract class AnalysisContentStatistics {
  /**
   * Return the statistics for each kind of cached data.
   *
   * @return the statistics for each kind of cached data
   */
  List<AnalysisContentStatistics_CacheRow> get cacheRows;

  /**
   * Return the exceptions that caused some entries to have a state of [CacheState#ERROR].
   *
   * @return the exceptions that caused some entries to have a state of [CacheState#ERROR]
   */
  List<AnalysisException> get exceptions;

  /**
   * Return an array containing all of the sources in the cache.
   *
   * @return an array containing all of the sources in the cache
   */
  List<Source> get sources;
}

/**
 * Information about single item in the cache.
 */
abstract class AnalysisContentStatistics_CacheRow {
  int get errorCount;

  int get flushedCount;

  int get inProcessCount;

  int get invalidCount;

  String get name;

  int get validCount;
}

/**
 * The interface `AnalysisContext` defines the behavior of objects that represent a context in
 * which a single analysis can be performed and incrementally maintained. The context includes such
 * information as the version of the SDK being analyzed against as well as the package-root used to
 * resolve 'package:' URI's. (Both of which are known indirectly through the [SourceFactory
 ].)
 *
 * An analysis context also represents the state of the analysis, which includes knowing which
 * sources have been included in the analysis (either directly or indirectly) and the results of the
 * analysis. Sources must be added and removed from the context using the method
 * [applyChanges], which is also used to notify the context when sources have been
 * modified and, consequently, previously known results might have been invalidated.
 *
 * There are two ways to access the results of the analysis. The most common is to use one of the
 * 'get' methods to access the results. The 'get' methods have the advantage that they will always
 * return quickly, but have the disadvantage that if the results are not currently available they
 * will return either nothing or in some cases an incomplete result. The second way to access
 * results is by using one of the 'compute' methods. The 'compute' methods will always attempt to
 * compute the requested results but might block the caller for a significant period of time.
 *
 * When results have been invalidated, have never been computed (as is the case for newly added
 * sources), or have been removed from the cache, they are <b>not</b> automatically recreated. They
 * will only be recreated if one of the 'compute' methods is invoked.
 *
 * However, this is not always acceptable. Some clients need to keep the analysis results
 * up-to-date. For such clients there is a mechanism that allows them to incrementally perform
 * needed analysis and get notified of the consequent changes to the analysis results. This
 * mechanism is realized by the method [performAnalysisTask].
 *
 * Analysis engine allows for having more than one context. This can be used, for example, to
 * perform one analysis based on the state of files on disk and a separate analysis based on the
 * state of those files in open editors. It can also be used to perform an analysis based on a
 * proposed future state, such as the state after a refactoring.
 */
abstract class AnalysisContext {
  /**
   * Apply the changes specified by the given change set to this context. Any analysis results that
   * have been invalidated by these changes will be removed.
   *
   * @param changeSet a description of the changes that are to be applied
   */
  void applyChanges(ChangeSet changeSet);

  /**
   * Return the documentation comment for the given element as it appears in the original source
   * (complete with the beginning and ending delimiters), or `null` if the element does not
   * have a documentation comment associated with it. This can be a long-running operation if the
   * information needed to access the comment is not cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param element the element whose documentation comment is to be returned
   * @return the element's documentation comment
   * @throws AnalysisException if the documentation comment could not be determined because the
   *           analysis could not be performed
   */
  String computeDocumentationComment(Element element);

  /**
   * Return an array containing all of the errors associated with the given source. If the errors
   * are not already known then the source will be analyzed in order to determine the errors
   * associated with it.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source whose errors are to be returned
   * @return all of the errors associated with the given source
   * @throws AnalysisException if the errors could not be determined because the analysis could not
   *           be performed
   * @see #getErrors(Source)
   */
  List<AnalysisError> computeErrors(Source source);

  /**
   * Return the element model corresponding to the HTML file defined by the given source. If the
   * element model does not yet exist it will be created. The process of creating an element model
   * for an HTML file can be long-running, depending on the size of the file and the number of
   * libraries that are defined in it (via script tags) that also need to have a model built for
   * them.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source defining the HTML file whose element model is to be returned
   * @return the element model corresponding to the HTML file defined by the given source
   * @throws AnalysisException if the element model could not be determined because the analysis
   *           could not be performed
   * @see #getHtmlElement(Source)
   */
  HtmlElement computeHtmlElement(Source source);

  /**
   * Return the kind of the given source, computing it's kind if it is not already known. Return
   * [SourceKind#UNKNOWN] if the source is not contained in this context.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source whose kind is to be returned
   * @return the kind of the given source
   * @see #getKindOf(Source)
   */
  SourceKind computeKindOf(Source source);

  /**
   * Return the element model corresponding to the library defined by the given source. If the
   * element model does not yet exist it will be created. The process of creating an element model
   * for a library can long-running, depending on the size of the library and the number of
   * libraries that are imported into it that also need to have a model built for them.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source defining the library whose element model is to be returned
   * @return the element model corresponding to the library defined by the given source
   * @throws AnalysisException if the element model could not be determined because the analysis
   *           could not be performed
   * @see #getLibraryElement(Source)
   */
  LibraryElement computeLibraryElement(Source source);

  /**
   * Return the line information for the given source, or `null` if the source is not of a
   * recognized kind (neither a Dart nor HTML file). If the line information was not previously
   * known it will be created. The line information is used to map offsets from the beginning of the
   * source to line and column pairs.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source whose line information is to be returned
   * @return the line information for the given source
   * @throws AnalysisException if the line information could not be determined because the analysis
   *           could not be performed
   * @see #getLineInfo(Source)
   */
  LineInfo computeLineInfo(Source source);

  /**
   * Notifies the context that the client is going to stop using this context.
   */
  void dispose();

  /**
   * Return `true` if the given source exists.
   *
   * This method should be used rather than the method [Source#exists] because contexts can
   * have local overrides of the content of a source that the source is not aware of and a source
   * with local content is considered to exist even if there is no file on disk.
   *
   * @param source the source whose modification stamp is to be returned
   * @return `true` if the source exists
   */
  bool exists(Source source);

  /**
   * Create a new context in which analysis can be performed. Any sources in the specified container
   * will be removed from this context and added to the newly created context.
   *
   * @param container the container containing sources that should be removed from this context and
   *          added to the returned context
   * @return the analysis context that was created
   */
  AnalysisContext extractContext(SourceContainer container);

  /**
   * Return the set of analysis options controlling the behavior of this context. Clients should not
   * modify the returned set of options. The options should only be set by invoking the method
   * [setAnalysisOptions].
   *
   * @return the set of analysis options controlling the behavior of this context
   */
  AnalysisOptions get analysisOptions;

  /**
   * Return the Angular application that contains the HTML file defined by the given source, or
   * `null` if the source does not represent an HTML file, the Angular application containing
   * the file has not yet been resolved, or the analysis of the HTML file failed for some reason.
   *
   * @param htmlSource the source defining the HTML file
   * @return the Angular application that contains the HTML file defined by the given source
   */
  AngularApplication getAngularApplicationWithHtml(Source htmlSource);

  /**
   * Return the element model corresponding to the compilation unit defined by the given source in
   * the library defined by the given source, or `null` if the element model does not
   * currently exist or if the library cannot be analyzed for some reason.
   *
   * @param unitSource the source of the compilation unit
   * @param librarySource the source of the defining compilation unit of the library containing the
   *          compilation unit
   * @return the element model corresponding to the compilation unit defined by the given source
   */
  CompilationUnitElement getCompilationUnitElement(Source unitSource, Source librarySource);

  /**
   * Get the contents and timestamp of the given source.
   *
   * This method should be used rather than the method [Source#getContents] because contexts
   * can have local overrides of the content of a source that the source is not aware of.
   *
   * @param source the source whose content is to be returned
   * @return the contents and timestamp of the source
   * @throws Exception if the contents of the source could not be accessed
   */
  TimestampedData<String> getContents(Source source);

  /**
   * Return the element referenced by the given location, or `null` if the element is not
   * immediately available or if there is no element with the given location. The latter condition
   * can occur, for example, if the location describes an element from a different context or if the
   * element has been removed from this context as a result of some change since it was originally
   * obtained.
   *
   * @param location the reference describing the element to be returned
   * @return the element referenced by the given location
   */
  Element getElement(ElementLocation location);

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
  AnalysisErrorInfo getErrors(Source source);

  /**
   * Return the element model corresponding to the HTML file defined by the given source, or
   * `null` if the source does not represent an HTML file, the element representing the file
   * has not yet been created, or the analysis of the HTML file failed for some reason.
   *
   * @param source the source defining the HTML file whose element model is to be returned
   * @return the element model corresponding to the HTML file defined by the given source
   * @see #computeHtmlElement(Source)
   */
  HtmlElement getHtmlElement(Source source);

  /**
   * Return the sources for the HTML files that reference the given compilation unit. If the source
   * does not represent a Dart source or is not known to this context, the returned array will be
   * empty. The contents of the array can be incomplete.
   *
   * @param source the source referenced by the returned HTML files
   * @return the sources for the HTML files that reference the given compilation unit
   */
  List<Source> getHtmlFilesReferencing(Source source);

  /**
   * Return an array containing all of the sources known to this context that represent HTML files.
   * The contents of the array can be incomplete.
   *
   * @return the sources known to this context that represent HTML files
   */
  List<Source> get htmlSources;

  /**
   * Return the kind of the given source, or `null` if the kind is not known to this context.
   *
   * @param source the source whose kind is to be returned
   * @return the kind of the given source
   * @see #computeKindOf(Source)
   */
  SourceKind getKindOf(Source source);

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
  List<Source> get launchableClientLibrarySources;

  /**
   * Return an array containing all of the sources known to this context that represent the defining
   * compilation unit of a library that can be run outside of a browser. The contents of the array
   * can be incomplete.
   *
   * @return the sources known to this context that represent the defining compilation unit of a
   *         library that can be run outside of a browser
   */
  List<Source> get launchableServerLibrarySources;

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
  List<Source> getLibrariesContaining(Source source);

  /**
   * Return the sources for the defining compilation units of any libraries that depend on the given
   * library. One library depends on another if it either imports or exports that library.
   *
   * @param librarySource the source for the defining compilation unit of the library being depended
   *          on
   * @return the sources for the libraries that depend on the given library
   */
  List<Source> getLibrariesDependingOn(Source librarySource);

  /**
   * Return the sources for the defining compilation units of any libraries that are referenced from
   * the given HTML file.
   *
   * @param htmlSource the source for the HTML file
   * @return the sources for the libraries that are referenced by the given HTML file
   */
  List<Source> getLibrariesReferencedFromHtml(Source htmlSource);

  /**
   * Return the element model corresponding to the library defined by the given source, or
   * `null` if the element model does not currently exist or if the library cannot be analyzed
   * for some reason.
   *
   * @param source the source defining the library whose element model is to be returned
   * @return the element model corresponding to the library defined by the given source
   */
  LibraryElement getLibraryElement(Source source);

  /**
   * Return an array containing all of the sources known to this context that represent the defining
   * compilation unit of a library. The contents of the array can be incomplete.
   *
   * @return the sources known to this context that represent the defining compilation unit of a
   *         library
   */
  List<Source> get librarySources;

  /**
   * Return the line information for the given source, or `null` if the line information is
   * not known. The line information is used to map offsets from the beginning of the source to line
   * and column pairs.
   *
   * @param source the source whose line information is to be returned
   * @return the line information for the given source
   * @see #computeLineInfo(Source)
   */
  LineInfo getLineInfo(Source source);

  /**
   * Return the modification stamp for the given source. A modification stamp is a non-negative
   * integer with the property that if the contents of the source have not been modified since the
   * last time the modification stamp was accessed then the same value will be returned, but if the
   * contents of the source have been modified one or more times (even if the net change is zero)
   * the stamps will be different.
   *
   * This method should be used rather than the method [Source#getModificationStamp] because
   * contexts can have local overrides of the content of a source that the source is not aware of.
   *
   * @param source the source whose modification stamp is to be returned
   * @return the modification stamp for the source
   */
  int getModificationStamp(Source source);

  /**
   * Return an array containing all of the sources known to this context and their resolution state
   * is not valid or flush. So, these sources are not safe to update during refactoring, because we
   * may be don't know all the references in them.
   *
   * @return the sources known to this context and are not safe for refactoring
   */
  List<Source> get refactoringUnsafeSources;

  /**
   * Return a fully resolved AST for a single compilation unit within the given library, or
   * `null` if the resolved AST is not already computed.
   *
   * @param unitSource the source of the compilation unit
   * @param library the library containing the compilation unit
   * @return a fully resolved AST for the compilation unit
   * @see #resolveCompilationUnit(Source, LibraryElement)
   */
  CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library);

  /**
   * Return a fully resolved AST for a single compilation unit within the given library, or
   * `null` if the resolved AST is not already computed.
   *
   * @param unitSource the source of the compilation unit
   * @param librarySource the source of the defining compilation unit of the library containing the
   *          compilation unit
   * @return a fully resolved AST for the compilation unit
   * @see #resolveCompilationUnit(Source, Source)
   */
  CompilationUnit getResolvedCompilationUnit2(Source unitSource, Source librarySource);

  /**
   * Return a fully resolved HTML unit, or `null` if the resolved unit is not already
   * computed.
   *
   * @param htmlSource the source of the HTML unit
   * @return a fully resolved HTML unit
   * @see #resolveHtmlUnit(Source)
   */
  ht.HtmlUnit getResolvedHtmlUnit(Source htmlSource);

  /**
   * Return the source factory used to create the sources that can be analyzed in this context.
   *
   * @return the source factory used to create the sources that can be analyzed in this context
   */
  SourceFactory get sourceFactory;

  /**
   * Return `true` if the given source is known to be the defining compilation unit of a
   * library that can be run on a client (references 'dart:html', either directly or indirectly).
   *
   * <b>Note:</b> In addition to the expected case of returning `false` if the source is known
   * to be a library that cannot be run on a client, this method will also return `false` if
   * the source is not known to be a library or if we do not know whether it can be run on a client.
   *
   * @param librarySource the source being tested
   * @return `true` if the given source is known to be a library that can be run on a client
   */
  bool isClientLibrary(Source librarySource);

  /**
   * Returns `true` if this context was disposed using [dispose].
   *
   * @return `true` if this context was disposed
   */
  bool get isDisposed;

  /**
   * Return `true` if the given source is known to be the defining compilation unit of a
   * library that can be run on the server (does not reference 'dart:html', either directly or
   * indirectly).
   *
   * <b>Note:</b> In addition to the expected case of returning `false` if the source is known
   * to be a library that cannot be run on the server, this method will also return `false` if
   * the source is not known to be a library or if we do not know whether it can be run on the
   * server.
   *
   * @param librarySource the source being tested
   * @return `true` if the given source is known to be a library that can be run on the server
   */
  bool isServerLibrary(Source librarySource);

  /**
   * Add the sources contained in the specified context to this context's collection of sources.
   * This method is called when an existing context's pubspec has been removed, and the contained
   * sources should be reanalyzed as part of this context.
   *
   * @param context the context being merged
   */
  void mergeContext(AnalysisContext context);

  /**
   * Parse a single source to produce an AST structure. The resulting AST structure may or may not
   * be resolved, and may have a slightly different structure depending upon whether it is resolved.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source to be parsed
   * @return the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  CompilationUnit parseCompilationUnit(Source source);

  /**
   * Parse a single HTML source to produce an AST structure. The resulting HTML AST structure may or
   * may not be resolved, and may have a slightly different structure depending upon whether it is
   * resolved.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the HTML source to be parsed
   * @return the parse result (not `null`)
   * @throws AnalysisException if the analysis could not be performed
   */
  ht.HtmlUnit parseHtmlUnit(Source source);

  /**
   * Perform the next unit of work required to keep the analysis results up-to-date and return
   * information about the consequent changes to the analysis results. This method can be long
   * running.
   *
   * @return the results of performing the analysis
   */
  AnalysisResult performAnalysisTask();

  /**
   * Parse and resolve a single source within the given context to produce a fully resolved AST.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param unitSource the source to be parsed and resolved
   * @param library the library containing the source to be resolved
   * @return the result of resolving the AST structure representing the content of the source in the
   *         context of the given library
   * @throws AnalysisException if the analysis could not be performed
   * @see #getResolvedCompilationUnit(Source, LibraryElement)
   */
  CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library);

  /**
   * Parse and resolve a single source within the given context to produce a fully resolved AST.
   * Return the resolved AST structure, or `null` if the source could not be either parsed or
   * resolved.
   *
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
  CompilationUnit resolveCompilationUnit2(Source unitSource, Source librarySource);

  /**
   * Parse and resolve a single source within the given context to produce a fully resolved AST.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param htmlSource the source to be parsed and resolved
   * @return the result of resolving the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  ht.HtmlUnit resolveHtmlUnit(Source htmlSource);

  /**
   * Set the set of analysis options controlling the behavior of this context to the given options.
   * Clients can safely assume that all necessary analysis results have been invalidated.
   *
   * @param options the set of analysis options that will control the behavior of this context
   */
  void set analysisOptions(AnalysisOptions options);

  /**
   * Set the order in which sources will be analyzed by [performAnalysisTask] to match the
   * order of the sources in the given list. If a source that needs to be analyzed is not contained
   * in the list, then it will be treated as if it were at the end of the list. If the list is empty
   * (or `null`) then no sources will be given priority over other sources.
   *
   * Changes made to the list after this method returns will <b>not</b> be reflected in the priority
   * order.
   *
   * @param sources the sources to be given priority over other sources
   */
  void set analysisPriorityOrder(List<Source> sources);

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
  void setChangedContents(Source source, String contents, int offset, int oldLength, int newLength);

  /**
   * Set the contents of the given source to the given contents and mark the source as having
   * changed. This has the effect of overriding the default contents of the source. If the contents
   * are `null` the override is removed so that the default contents will be returned.
   *
   * @param source the source whose contents are being overridden
   * @param contents the new contents of the source
   */
  void setContents(Source source, String contents);

  /**
   * Set the source factory used to create the sources that can be analyzed in this context to the
   * given source factory. Clients can safely assume that all analysis results have been
   * invalidated.
   *
   * @param factory the source factory used to create the sources that can be analyzed in this
   *          context
   */
  void set sourceFactory(SourceFactory factory);
}

/**
 * The interface `AnalysisErrorInfo` contains the analysis errors and line information for the
 * errors.
 */
abstract class AnalysisErrorInfo {
  /**
   * Return the errors that as a result of the analysis, or `null` if there were no errors.
   *
   * @return the errors as a result of the analysis
   */
  List<AnalysisError> get errors;

  /**
   * Return the line information associated with the errors, or `null` if there were no
   * errors.
   *
   * @return the line information associated with the errors
   */
  LineInfo get lineInfo;
}

/**
 * Instances of the class `AnalysisException` represent an exception that occurred during the
 * analysis of one or more sources.
 */
class AnalysisException extends JavaException {
  /**
   * Initialize a newly created exception.
   */
  AnalysisException() : super();

  /**
   * Initialize a newly created exception to have the given message.
   *
   * @param message the message associated with the exception
   */
  AnalysisException.con1(String message) : super(message);

  /**
   * Initialize a newly created exception to have the given message and cause.
   *
   * @param message the message associated with the exception
   * @param cause the underlying exception that caused this exception
   */
  AnalysisException.con2(String message, Exception cause) : super(message, cause);

  /**
   * Initialize a newly created exception to have the given cause.
   *
   * @param cause the underlying exception that caused this exception
   */
  AnalysisException.con3(Exception cause) : super.withCause(cause);
}

/**
 * The interface `AnalysisOptions` defines the behavior of objects that provide access to a
 * set of analysis options used to control the behavior of an analysis context.
 */
abstract class AnalysisOptions {
  /**
   * Return `true` if analysis is to analyze Angular.
   *
   * @return `true` if analysis is to analyze Angular
   */
  bool get analyzeAngular;

  /**
   * Return `true` if analysis is to parse and analyze function bodies.
   *
   * @return `true` if analysis is to parse and analyzer function bodies
   */
  bool get analyzeFunctionBodies;

  /**
   * Return the maximum number of sources for which AST structures should be kept in the cache.
   *
   * @return the maximum number of sources for which AST structures should be kept in the cache
   */
  int get cacheSize;

  /**
   * Return `true` if analysis is to generate dart2js related hint results.
   *
   * @return `true` if analysis is to generate dart2js related hint results
   */
  bool get dart2jsHint;

  /**
   * Return `true` if errors, warnings and hints should be generated for sources in the SDK.
   * The default value is `false`.
   *
   * @return `true` if errors, warnings and hints should be generated for the SDK
   */
  bool get generateSdkErrors;

  /**
   * Return `true` if analysis is to generate hint results (e.g. type inference based
   * information and pub best practices).
   *
   * @return `true` if analysis is to generate hint results
   */
  bool get hint;

  /**
   * Return `true` if incremental analysis should be used.
   *
   * @return `true` if incremental analysis should be used
   */
  bool get incremental;

  /**
   * Return `true` if analysis is to parse comments.
   *
   * @return `true` if analysis is to parse comments
   */
  bool get preserveComments;
}

/**
 * Instances of the class `AnalysisResult`
 */
class AnalysisResult {
  /**
   * The change notices associated with this result, or `null` if there were no changes and
   * there is no more work to be done.
   */
  final List<ChangeNotice> _notices;

  /**
   * The number of milliseconds required to determine which task was to be performed.
   */
  final int getTime;

  /**
   * The name of the class of the task that was performed.
   */
  final String taskClassName;

  /**
   * The number of milliseconds required to perform the task.
   */
  final int performTime;

  /**
   * Initialize a newly created analysis result to have the given values.
   *
   * @param notices the change notices associated with this result
   * @param getTime the number of milliseconds required to determine which task was to be performed
   * @param taskClassName the name of the class of the task that was performed
   * @param performTime the number of milliseconds required to perform the task
   */
  AnalysisResult(this._notices, this.getTime, this.taskClassName, this.performTime);

  /**
   * Return the change notices associated with this result, or `null` if there were no changes
   * and there is no more work to be done.
   *
   * @return the change notices associated with this result
   */
  List<ChangeNotice> get changeNotices => _notices;

  /**
   * Return `true` if there is more to be performed after the task that was performed.
   *
   * @return `true` if there is more to be performed after the task that was performed
   */
  bool get hasMoreWork => _notices != null;
}

/**
 * The interface `ChangeNotice` defines the behavior of objects that represent a change to the
 * analysis results associated with a given source.
 */
abstract class ChangeNotice implements AnalysisErrorInfo {
  /**
   * Return the fully resolved AST that changed as a result of the analysis, or `null` if the
   * AST was not changed.
   *
   * @return the fully resolved AST that changed as a result of the analysis
   */
  CompilationUnit get compilationUnit;

  /**
   * Return the fully resolved HTML that changed as a result of the analysis, or `null` if the
   * HTML was not changed.
   *
   * @return the fully resolved HTML that changed as a result of the analysis
   */
  ht.HtmlUnit get htmlUnit;

  /**
   * Return the source for which the result is being reported.
   *
   * @return the source for which the result is being reported
   */
  Source get source;
}

/**
 * Instances of the class `ChangeSet` indicate what sources have been added, changed, or
 * removed.
 */
class ChangeSet {
  /**
   * A list containing the sources that have been added.
   */
  List<Source> _added = new List<Source>();

  /**
   * A list containing the sources that have been changed.
   */
  List<Source> _changed = new List<Source>();

  /**
   * A list containing the sources that have been removed.
   */
  List<Source> _removed = new List<Source>();

  /**
   * A list containing the source containers specifying additional sources that have been removed.
   */
  final List<SourceContainer> removedContainers = new List<SourceContainer>();

  /**
   * Record that the specified source has been added and that it's content is the default contents
   * of the source.
   *
   * @param source the source that was added
   */
  void addedSource(Source source) {
    _added.add(source);
  }

  /**
   * Record that the specified source has been changed and that it's content is the default contents
   * of the source.
   *
   * @param source the source that was changed
   */
  void changedSource(Source source) {
    _changed.add(source);
  }

  /**
   * Return a collection of the sources that have been added.
   *
   * @return a collection of the sources that have been added
   */
  List<Source> get addedSources => _added;

  /**
   * Return a collection of sources that have been changed.
   *
   * @return a collection of sources that have been changed
   */
  List<Source> get changedSources => _changed;

  /**
   * Return a list containing the sources that were removed.
   *
   * @return a list containing the sources that were removed
   */
  List<Source> get removedSources => _removed;

  /**
   * Return `true` if this change set does not contain any changes.
   *
   * @return `true` if this change set does not contain any changes
   */
  bool get isEmpty => _added.isEmpty && _changed.isEmpty && _removed.isEmpty && removedContainers.isEmpty;

  /**
   * Record that the specified source container has been removed.
   *
   * @param container the source container that was removed
   */
  void removedContainer(SourceContainer container) {
    if (container != null) {
      removedContainers.add(container);
    }
  }

  /**
   * Record that the specified source has been removed.
   *
   * @param source the source that was removed
   */
  void removedSource(Source source) {
    if (source != null) {
      _removed.add(source);
    }
  }

  @override
  String toString() {
    JavaStringBuilder builder = new JavaStringBuilder();
    bool needsSeparator = _appendSources(builder, _added, false, "added");
    needsSeparator = _appendSources(builder, _changed, needsSeparator, "changed");
    _appendSources(builder, _removed, needsSeparator, "removed");
    int count = removedContainers.length;
    if (count > 0) {
      if (_removed.isEmpty) {
        if (needsSeparator) {
          builder.append("; ");
        }
        builder.append("removed: from ");
        builder.append(count);
        builder.append(" containers");
      } else {
        builder.append(", and more from ");
        builder.append(count);
        builder.append(" containers");
      }
    }
    return builder.toString();
  }

  /**
   * Append the given sources to the given builder, prefixed with the given label and possibly a
   * separator.
   *
   * @param builder the builder to which the sources are to be appended
   * @param sources the sources to be appended
   * @param needsSeparator `true` if a separator is needed before the label
   * @param label the label used to prefix the sources
   * @return `true` if future lists of sources will need a separator
   */
  bool _appendSources(JavaStringBuilder builder, List<Source> sources, bool needsSeparator, String label) {
    if (sources.isEmpty) {
      return needsSeparator;
    }
    if (needsSeparator) {
      builder.append("; ");
    }
    builder.append(label);
    String prefix = " ";
    for (Source source in sources) {
      builder.append(prefix);
      builder.append(source.fullName);
      prefix = ", ";
    }
    return true;
  }
}

/**
 * Instances of the class `ObsoleteSourceAnalysisException` represent an analysis attempt that
 * failed because a source was deleted between the time the analysis started and the time the
 * results of the analysis were ready to be recorded.
 */
class ObsoleteSourceAnalysisException extends AnalysisException {
  /**
   * The source that was removed while it was being analyzed.
   */
  Source _source;

  /**
   * Initialize a newly created exception to represent the removal of the given source.
   *
   * @param source the source that was removed while it was being analyzed
   */
  ObsoleteSourceAnalysisException(Source source) : super.con1("The source '${source.fullName}' was removed while it was being analyzed") {
    this._source = source;
  }

  /**
   * Return the source that was removed while it was being analyzed.
   *
   * @return the source that was removed
   */
  Source get source => _source;
}

/**
 * Instances of the class `AnalysisCache` implement an LRU cache of information related to
 * analysis.
 */
class AnalysisCache {
  /**
   * A table mapping the sources known to the context to the information known about the source.
   */
  Map<Source, SourceEntry> _sourceMap = new Map<Source, SourceEntry>();

  /**
   * The maximum number of sources for which AST structures should be kept in the cache.
   */
  int _maxCacheSize = 0;

  /**
   * The policy used to determine which pieces of data to remove from the cache.
   */
  final CacheRetentionPolicy _retentionPolicy;

  /**
   * A list containing the most recently accessed sources with the most recently used at the end of
   * the list. When more sources are added than the maximum allowed then the least recently used
   * source will be removed and will have it's cached AST structure flushed.
   */
  List<Source> _recentlyUsed;

  /**
   * Initialize a newly created cache to maintain at most the given number of AST structures in the
   * cache.
   *
   * @param maxCacheSize the maximum number of sources for which AST structures should be kept in
   *          the cache
   * @param retentionPolicy the policy used to determine which pieces of data to remove from the
   *          cache
   */
  AnalysisCache(int maxCacheSize, this._retentionPolicy) {
    this._maxCacheSize = maxCacheSize;
    _recentlyUsed = new List<Source>();
  }

  /**
   * Record that the AST associated with the given source was just read from the cache.
   *
   * @param source the source whose AST was accessed
   */
  void accessedAst(Source source) {
    if (_recentlyUsed.remove(source)) {
      _recentlyUsed.add(source);
      return;
    }
    while (_recentlyUsed.length >= _maxCacheSize) {
      if (!_flushAstFromCache()) {
        break;
      }
    }
    _recentlyUsed.add(source);
  }

  /**
   * Return a collection containing all of the map entries mapping sources to cache entries. Clients
   * should not modify the returned collection.
   *
   * @return a collection containing all of the map entries mapping sources to cache entries
   */
  Iterable<MapEntry<Source, SourceEntry>> entrySet() => getMapEntrySet(_sourceMap);

  /**
   * Return the entry associated with the given source.
   *
   * @param source the source whose entry is to be returned
   * @return the entry associated with the given source
   */
  SourceEntry get(Source source) => _sourceMap[source];

  /**
   * Associate the given entry with the given source.
   *
   * @param source the source with which the entry is to be associated
   * @param entry the entry to be associated with the source
   */
  void put(Source source, SourceEntry entry) {
    (entry as SourceEntryImpl).fixExceptionState();
    _sourceMap[source] = entry;
  }

  /**
   * Remove all information related to the given source from this cache.
   *
   * @param source the source to be removed
   */
  void remove(Source source) {
    _recentlyUsed.remove(source);
    _sourceMap.remove(source);
  }

  /**
   * Record that the AST associated with the given source was just removed from the cache.
   *
   * @param source the source whose AST was removed
   */
  void removedAst(Source source) {
    _recentlyUsed.remove(source);
  }

  /**
   * Set the maximum size of the cache to the given size.
   *
   * @param size the maximum number of sources for which AST structures should be kept in the cache
   */
  void set maxCacheSize(int size) {
    _maxCacheSize = size;
    while (_recentlyUsed.length > _maxCacheSize) {
      if (!_flushAstFromCache()) {
        break;
      }
    }
  }

  /**
   * Return the number of sources that are mapped to cache entries.
   *
   * @return the number of sources that are mapped to cache entries
   */
  int size() => _sourceMap.length;

  /**
   * Record that the AST associated with the given source was just stored to the cache.
   *
   * @param source the source whose AST was stored
   */
  void storedAst(Source source) {
    if (_recentlyUsed.contains(source)) {
      return;
    }
    while (_recentlyUsed.length >= _maxCacheSize) {
      if (!_flushAstFromCache()) {
        break;
      }
    }
    _recentlyUsed.add(source);
  }

  /**
   * Attempt to flush one AST structure from the cache.
   *
   * @return `true` if a structure was flushed
   */
  bool _flushAstFromCache() {
    Source removedSource = _removeAstToFlush();
    if (removedSource == null) {
      return false;
    }
    SourceEntry sourceEntry = _sourceMap[removedSource];
    if (sourceEntry is HtmlEntry) {
      HtmlEntryImpl htmlCopy = sourceEntry.writableCopy;
      htmlCopy.flushAstStructures();
      _sourceMap[removedSource] = htmlCopy;
    } else if (sourceEntry is DartEntry) {
      DartEntryImpl dartCopy = sourceEntry.writableCopy;
      dartCopy.flushAstStructures();
      _sourceMap[removedSource] = dartCopy;
    }
    return true;
  }

  /**
   * Remove and return one source from the list of recently used sources whose AST structure can be
   * flushed from the cache. The source that will be returned will be the source that has been
   * unreferenced for the longest period of time but that is not a priority for analysis.
   *
   * It is possible for there to be no AST that can be flushed, in which case `null` will be
   * returned. This happens, for example, if the context is reserving the AST's needed to resolve a
   * cycle of libraries and the number of AST's being reserved is larger than the current cache
   * size.
   *
   * @return the source that was removed
   */
  Source _removeAstToFlush() {
    int sourceToRemove = -1;
    for (int i = 0; i < _recentlyUsed.length; i++) {
      Source source = _recentlyUsed[i];
      RetentionPriority priority = _retentionPolicy.getAstPriority(source, _sourceMap[source]);
      if (priority == RetentionPriority.LOW) {
        return _recentlyUsed.removeAt(i);
      } else if (priority == RetentionPriority.MEDIUM && sourceToRemove < 0) {
        sourceToRemove = i;
      }
    }
    if (sourceToRemove < 0) {
      return null;
    }
    return _recentlyUsed.removeAt(sourceToRemove);
  }
}

/**
 * Instances of the class `CacheRetentionPolicy` define the behavior of objects that determine
 * how important it is for data to be retained in the analysis cache.
 */
abstract class CacheRetentionPolicy {
  /**
   * Return the priority of retaining the AST structure for the given source.
   *
   * @param source the source whose AST structure is being considered for removal
   * @param sourceEntry the entry representing the source
   * @return the priority of retaining the AST structure for the given source
   */
  RetentionPriority getAstPriority(Source source, SourceEntry sourceEntry);
}

/**
 * The enumeration `CacheState` defines the possible states of cached data.
 */
class CacheState extends Enum<CacheState> {
  /**
   * The data is not in the cache and the last time an attempt was made to compute the data an
   * exception occurred, making it pointless to attempt.
   *
   * Valid Transitions:
   * * [INVALID] if a source was modified that might cause the data to be computable
   */
  static const CacheState ERROR = const CacheState('ERROR', 0);

  /**
   * The data is not in the cache because it was flushed from the cache in order to control memory
   * usage. If the data is recomputed, results do not need to be reported.
   *
   * Valid Transitions:
   * * [IN_PROCESS] if the data is being recomputed
   * * [INVALID] if a source was modified that causes the data to need to be recomputed
   */
  static const CacheState FLUSHED = const CacheState('FLUSHED', 1);

  /**
   * The data might or might not be in the cache but is in the process of being recomputed.
   *
   * Valid Transitions:
   * * [ERROR] if an exception occurred while trying to compute the data
   * * [VALID] if the data was successfully computed and stored in the cache
   */
  static const CacheState IN_PROCESS = const CacheState('IN_PROCESS', 2);

  /**
   * The data is not in the cache and needs to be recomputed so that results can be reported.
   *
   * Valid Transitions:
   * * [IN_PROCESS] if an attempt is being made to recompute the data
   */
  static const CacheState INVALID = const CacheState('INVALID', 3);

  /**
   * The data is in the cache and up-to-date.
   *
   * Valid Transitions:
   * * [FLUSHED] if the data is removed in order to manage memory usage
   * * [INVALID] if a source was modified in such a way as to invalidate the previous data
   */
  static const CacheState VALID = const CacheState('VALID', 4);

  static const List<CacheState> values = const [ERROR, FLUSHED, IN_PROCESS, INVALID, VALID];

  const CacheState(String name, int ordinal) : super(name, ordinal);
}

/**
 * The interface `DartEntry` defines the behavior of objects that maintain the information
 * cached by an analysis context about an individual Dart file.
 */
abstract class DartEntry implements SourceEntry {
  /**
   * The data descriptor representing the errors reported during Angular resolution.
   */
  static final DataDescriptor<List<AnalysisError>> ANGULAR_ERRORS = new DataDescriptor<List<AnalysisError>>("DartEntry.ANGULAR_ERRORS");

  /**
   * The data descriptor representing the list of libraries that contain this compilation unit.
   */
  static final DataDescriptor<List<Source>> CONTAINING_LIBRARIES = new DataDescriptor<List<Source>>("DartEntry.CONTAINING_LIBRARIES");

  /**
   * The data descriptor representing the library element for the library. This data is only
   * available for Dart files that are the defining compilation unit of a library.
   */
  static final DataDescriptor<LibraryElement> ELEMENT = new DataDescriptor<LibraryElement>("DartEntry.ELEMENT");

  /**
   * The data descriptor representing the list of exported libraries. This data is only available
   * for Dart files that are the defining compilation unit of a library.
   */
  static final DataDescriptor<List<Source>> EXPORTED_LIBRARIES = new DataDescriptor<List<Source>>("DartEntry.EXPORTED_LIBRARIES");

  /**
   * The data descriptor representing the hints resulting from auditing the source.
   */
  static final DataDescriptor<List<AnalysisError>> HINTS = new DataDescriptor<List<AnalysisError>>("DartEntry.HINTS");

  /**
   * The data descriptor representing the list of imported libraries. This data is only available
   * for Dart files that are the defining compilation unit of a library.
   */
  static final DataDescriptor<List<Source>> IMPORTED_LIBRARIES = new DataDescriptor<List<Source>>("DartEntry.IMPORTED_LIBRARIES");

  /**
   * The data descriptor representing the list of included parts. This data is only available for
   * Dart files that are the defining compilation unit of a library.
   */
  static final DataDescriptor<List<Source>> INCLUDED_PARTS = new DataDescriptor<List<Source>>("DartEntry.INCLUDED_PARTS");

  /**
   * The data descriptor representing the client flag. This data is only available for Dart files
   * that are the defining compilation unit of a library.
   */
  static final DataDescriptor<bool> IS_CLIENT = new DataDescriptor<bool>("DartEntry.IS_CLIENT");

  /**
   * The data descriptor representing the launchable flag. This data is only available for Dart
   * files that are the defining compilation unit of a library.
   */
  static final DataDescriptor<bool> IS_LAUNCHABLE = new DataDescriptor<bool>("DartEntry.IS_LAUNCHABLE");

  /**
   * The data descriptor representing the errors resulting from parsing the source.
   */
  static final DataDescriptor<List<AnalysisError>> PARSE_ERRORS = new DataDescriptor<List<AnalysisError>>("DartEntry.PARSE_ERRORS");

  /**
   * The data descriptor representing the parsed AST structure.
   */
  static final DataDescriptor<CompilationUnit> PARSED_UNIT = new DataDescriptor<CompilationUnit>("DartEntry.PARSED_UNIT");

  /**
   * The data descriptor representing the public namespace of the library. This data is only
   * available for Dart files that are the defining compilation unit of a library.
   */
  static final DataDescriptor<Namespace> PUBLIC_NAMESPACE = new DataDescriptor<Namespace>("DartEntry.PUBLIC_NAMESPACE");

  /**
   * The data descriptor representing the errors resulting from resolving the source.
   */
  static final DataDescriptor<List<AnalysisError>> RESOLUTION_ERRORS = new DataDescriptor<List<AnalysisError>>("DartEntry.RESOLUTION_ERRORS");

  /**
   * The data descriptor representing the resolved AST structure.
   */
  static final DataDescriptor<CompilationUnit> RESOLVED_UNIT = new DataDescriptor<CompilationUnit>("DartEntry.RESOLVED_UNIT");

  /**
   * The data descriptor representing the token stream.
   */
  static final DataDescriptor<List<AnalysisError>> SCAN_ERRORS = new DataDescriptor<List<AnalysisError>>("DartEntry.SCAN_ERRORS");

  /**
   * The data descriptor representing the source kind.
   */
  static final DataDescriptor<SourceKind> SOURCE_KIND = new DataDescriptor<SourceKind>("DartEntry.SOURCE_KIND");

  /**
   * The data descriptor representing the token stream.
   */
  static final DataDescriptor<Token> TOKEN_STREAM = new DataDescriptor<Token>("DartEntry.TOKEN_STREAM");

  /**
   * The data descriptor representing the errors resulting from verifying the source.
   */
  static final DataDescriptor<List<AnalysisError>> VERIFICATION_ERRORS = new DataDescriptor<List<AnalysisError>>("DartEntry.VERIFICATION_ERRORS");

  /**
   * Return all of the errors associated with the compilation unit that are currently cached.
   *
   * @return all of the errors associated with the compilation unit
   */
  List<AnalysisError> get allErrors;

  /**
   * Return a valid parsed compilation unit, either an unresolved AST structure or the result of
   * resolving the AST structure in the context of some library, or `null` if there is no
   * parsed compilation unit available.
   *
   * @return a valid parsed compilation unit
   */
  CompilationUnit get anyParsedCompilationUnit;

  /**
   * Return the result of resolving the compilation unit as part of any library, or `null` if
   * there is no cached resolved compilation unit.
   *
   * @return any resolved compilation unit
   */
  CompilationUnit get anyResolvedCompilationUnit;

  /**
   * Return the state of the data represented by the given descriptor in the context of the given
   * library.
   *
   * @param descriptor the descriptor representing the data whose state is to be returned
   * @param librarySource the source of the defining compilation unit of the library that is the
   *          context for the data
   * @return the value of the data represented by the given descriptor and library
   */
  CacheState getStateInLibrary(DataDescriptor descriptor, Source librarySource);

  /**
   * Return the value of the data represented by the given descriptor in the context of the given
   * library, or `null` if the data represented by the descriptor is not in the cache.
   *
   * @param descriptor the descriptor representing which data is to be returned
   * @param librarySource the source of the defining compilation unit of the library that is the
   *          context for the data
   * @return the value of the data represented by the given descriptor and library
   */
  Object getValueInLibrary(DataDescriptor descriptor, Source librarySource);

  @override
  DartEntryImpl get writableCopy;

  /**
   * Return `true` if the data represented by the given descriptor is marked as being invalid.
   * If the descriptor represents library-specific data then this method will return `true` if
   * the data associated with any library it marked as invalid.
   *
   * @param descriptor the descriptor representing which data is being tested
   * @return `true` if the data is marked as being invalid
   */
  bool hasInvalidData(DataDescriptor descriptor);

  /**
   * Return `true` if this entry has an AST structure that can be resolved (even if it needs
   * to be copied).
   *
   * @return `true` if the method [DartEntryImpl#getResolvableCompilationUnit] will
   *         return a non-`null` result
   */
  bool get hasResolvableCompilationUnit;

  /**
   * Return `true` if this data is safe to use in refactoring.
   */
  bool get isRefactoringSafe;
}

/**
 * Instances of the class `DartEntryImpl` implement a [DartEntry].
 */
class DartEntryImpl extends SourceEntryImpl implements DartEntry {
  /**
   * The state of the cached token stream.
   */
  CacheState _tokenStreamState = CacheState.INVALID;

  /**
   * The head of the token stream, or `null` if the token stream is not currently cached.
   */
  Token _tokenStream;

  /**
   * The state of the cached scan errors.
   */
  CacheState _scanErrorsState = CacheState.INVALID;

  /**
   * The errors produced while scanning the compilation unit, or an empty array if the errors are
   * not currently cached.
   */
  List<AnalysisError> _scanErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached source kind.
   */
  CacheState _sourceKindState = CacheState.INVALID;

  /**
   * The kind of this source.
   */
  SourceKind _sourceKind = SourceKind.UNKNOWN;

  /**
   * The state of the cached parsed compilation unit.
   */
  CacheState _parsedUnitState = CacheState.INVALID;

  /**
   * A flag indicating whether the parsed AST structure has been accessed since it was set. This is
   * used to determine whether the structure needs to be copied before it is resolved.
   */
  bool _parsedUnitAccessed = false;

  /**
   * The parsed compilation unit, or `null` if the parsed compilation unit is not currently
   * cached.
   */
  CompilationUnit _parsedUnit;

  /**
   * The state of the cached parse errors.
   */
  CacheState _parseErrorsState = CacheState.INVALID;

  /**
   * The errors produced while parsing the compilation unit, or an empty array if the errors are not
   * currently cached.
   */
  List<AnalysisError> _parseErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached list of imported libraries.
   */
  CacheState _importedLibrariesState = CacheState.INVALID;

  /**
   * The list of libraries imported by the library, or an empty array if the list is not currently
   * cached. The list will be empty if the Dart file is a part rather than a library.
   */
  List<Source> _importedLibraries = Source.EMPTY_ARRAY;

  /**
   * The state of the cached list of exported libraries.
   */
  CacheState _exportedLibrariesState = CacheState.INVALID;

  /**
   * The list of libraries exported by the library, or an empty array if the list is not currently
   * cached. The list will be empty if the Dart file is a part rather than a library.
   */
  List<Source> _exportedLibraries = Source.EMPTY_ARRAY;

  /**
   * The state of the cached list of included parts.
   */
  CacheState _includedPartsState = CacheState.INVALID;

  /**
   * The list of parts included in the library, or an empty array if the list is not currently
   * cached. The list will be empty if the Dart file is a part rather than a library.
   */
  List<Source> _includedParts = Source.EMPTY_ARRAY;

  /**
   * The list of libraries that contain this compilation unit. The list will be empty if there are
   * no known libraries that contain this compilation unit.
   */
  List<Source> _containingLibraries = new List<Source>();

  /**
   * The information known as a result of resolving this compilation unit as part of the library
   * that contains this unit. This field will never be `null`.
   */
  DartEntryImpl_ResolutionState _resolutionState = new DartEntryImpl_ResolutionState();

  /**
   * The state of the cached library element.
   */
  CacheState _elementState = CacheState.INVALID;

  /**
   * The element representing the library, or `null` if the element is not currently cached.
   */
  LibraryElement _element;

  /**
   * The state of the cached public namespace.
   */
  CacheState _publicNamespaceState = CacheState.INVALID;

  /**
   * The public namespace of the library, or `null` if the namespace is not currently cached.
   */
  Namespace _publicNamespace;

  /**
   * The state of the cached client/ server flag.
   */
  CacheState _clientServerState = CacheState.INVALID;

  /**
   * The state of the cached launchable flag.
   */
  CacheState _launchableState = CacheState.INVALID;

  /**
   * The error produced while performing Angular resolution, or an empty array if there are no
   * errors if the error are not currently cached.
   */
  List<AnalysisError> _angularErrors = AnalysisError.NO_ERRORS;

  /**
   * The index of the flag indicating whether this library is launchable (whether the file has a
   * main method).
   */
  static int _LAUNCHABLE_INDEX = 1;

  /**
   * The index of the flag indicating whether the library is client code (whether the library
   * depends on the html library). If the library is not "client code", then it is referred to as
   * "server code".
   */
  static int _CLIENT_CODE_INDEX = 2;

  /**
   * Add the given library to the list of libraries that contain this part. This method should only
   * be invoked on entries that represent a part.
   *
   * @param librarySource the source of the library to be added
   */
  void addContainingLibrary(Source librarySource) {
    _containingLibraries.add(librarySource);
  }

  /**
   * Flush any AST structures being maintained by this entry.
   */
  void flushAstStructures() {
    if (_tokenStreamState == CacheState.VALID) {
      _tokenStreamState = CacheState.FLUSHED;
      _tokenStream = null;
    }
    if (_parsedUnitState == CacheState.VALID) {
      _parsedUnitState = CacheState.FLUSHED;
      _parsedUnitAccessed = false;
      _parsedUnit = null;
    }
    _resolutionState.flushAstStructures();
  }

  @override
  List<AnalysisError> get allErrors {
    List<AnalysisError> errors = new List<AnalysisError>();
    ListUtilities.addAll(errors, _scanErrors);
    ListUtilities.addAll(errors, _parseErrors);
    DartEntryImpl_ResolutionState state = _resolutionState;
    while (state != null) {
      ListUtilities.addAll(errors, state._resolutionErrors);
      ListUtilities.addAll(errors, state._verificationErrors);
      ListUtilities.addAll(errors, state._hints);
      state = state._nextState;
    }
    ListUtilities.addAll(errors, _angularErrors);
    if (errors.length == 0) {
      return AnalysisError.NO_ERRORS;
    }
    return new List.from(errors);
  }

  @override
  CompilationUnit get anyParsedCompilationUnit {
    if (_parsedUnitState == CacheState.VALID) {
      _parsedUnitAccessed = true;
      return _parsedUnit;
    }
    return anyResolvedCompilationUnit;
  }

  @override
  CompilationUnit get anyResolvedCompilationUnit {
    DartEntryImpl_ResolutionState state = _resolutionState;
    while (state != null) {
      if (state._resolvedUnitState == CacheState.VALID) {
        return state._resolvedUnit;
      }
      state = state._nextState;
    }
    ;
    return null;
  }

  /**
   * Return a list containing the libraries that are known to contain this part.
   *
   * @return a list containing the libraries that are known to contain this part
   */
  List<Source> get containingLibraries => _containingLibraries;

  @override
  SourceKind get kind => _sourceKind;

  /**
   * Answer an array of library sources containing the receiver's source.
   */
  List<Source> get librariesContaining {
    DartEntryImpl_ResolutionState state = _resolutionState;
    List<Source> result = new List<Source>();
    while (state != null) {
      if (state._librarySource != null) {
        result.add(state._librarySource);
      }
      state = state._nextState;
    }
    return new List.from(result);
  }

  /**
   * Return a compilation unit that has not been accessed by any other client and can therefore
   * safely be modified by the reconciler, or `null` if the source has not been parsed.
   *
   * @return a compilation unit that can be modified by the reconciler
   */
  CompilationUnit get resolvableCompilationUnit {
    if (_parsedUnitState == CacheState.VALID) {
      if (_parsedUnitAccessed) {
        return _parsedUnit.accept(new AstCloner()) as CompilationUnit;
      }
      CompilationUnit unit = _parsedUnit;
      _parsedUnitState = CacheState.FLUSHED;
      _parsedUnitAccessed = false;
      _parsedUnit = null;
      return unit;
    }
    DartEntryImpl_ResolutionState state = _resolutionState;
    while (state != null) {
      if (state._resolvedUnitState == CacheState.VALID) {
        return state._resolvedUnit.accept(new AstCloner()) as CompilationUnit;
      }
      state = state._nextState;
    }
    ;
    return null;
  }

  @override
  CacheState getState(DataDescriptor descriptor) {
    if (identical(descriptor, DartEntry.ELEMENT)) {
      return _elementState;
    } else if (identical(descriptor, DartEntry.EXPORTED_LIBRARIES)) {
      return _exportedLibrariesState;
    } else if (identical(descriptor, DartEntry.IMPORTED_LIBRARIES)) {
      return _importedLibrariesState;
    } else if (identical(descriptor, DartEntry.INCLUDED_PARTS)) {
      return _includedPartsState;
    } else if (identical(descriptor, DartEntry.IS_CLIENT)) {
      return _clientServerState;
    } else if (identical(descriptor, DartEntry.IS_LAUNCHABLE)) {
      return _launchableState;
    } else if (identical(descriptor, DartEntry.PARSE_ERRORS)) {
      return _parseErrorsState;
    } else if (identical(descriptor, DartEntry.PARSED_UNIT)) {
      return _parsedUnitState;
    } else if (identical(descriptor, DartEntry.PUBLIC_NAMESPACE)) {
      return _publicNamespaceState;
    } else if (identical(descriptor, DartEntry.SCAN_ERRORS)) {
      return _scanErrorsState;
    } else if (identical(descriptor, DartEntry.SOURCE_KIND)) {
      return _sourceKindState;
    } else if (identical(descriptor, DartEntry.TOKEN_STREAM)) {
      return _tokenStreamState;
    } else {
      return super.getState(descriptor);
    }
  }

  @override
  CacheState getStateInLibrary(DataDescriptor descriptor, Source librarySource) {
    DartEntryImpl_ResolutionState state = _resolutionState;
    while (state != null) {
      if (librarySource == state._librarySource) {
        if (identical(descriptor, DartEntry.RESOLUTION_ERRORS)) {
          return state._resolutionErrorsState;
        } else if (identical(descriptor, DartEntry.RESOLVED_UNIT)) {
          return state._resolvedUnitState;
        } else if (identical(descriptor, DartEntry.VERIFICATION_ERRORS)) {
          return state._verificationErrorsState;
        } else if (identical(descriptor, DartEntry.HINTS)) {
          return state._hintsState;
        } else {
          throw new IllegalArgumentException("Invalid descriptor: ${descriptor}");
        }
      }
      state = state._nextState;
    }
    ;
    if (identical(descriptor, DartEntry.RESOLUTION_ERRORS) || identical(descriptor, DartEntry.RESOLVED_UNIT) || identical(descriptor, DartEntry.VERIFICATION_ERRORS) || identical(descriptor, DartEntry.HINTS)) {
      return CacheState.INVALID;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: ${descriptor}");
    }
  }

  @override
  Object getValue(DataDescriptor descriptor) {
    if (identical(descriptor, DartEntry.ANGULAR_ERRORS)) {
      return _angularErrors;
    } else if (identical(descriptor, DartEntry.CONTAINING_LIBRARIES)) {
      return new List.from(_containingLibraries);
    } else if (identical(descriptor, DartEntry.ELEMENT)) {
      return _element;
    } else if (identical(descriptor, DartEntry.EXPORTED_LIBRARIES)) {
      return _exportedLibraries;
    } else if (identical(descriptor, DartEntry.IMPORTED_LIBRARIES)) {
      return _importedLibraries;
    } else if (identical(descriptor, DartEntry.INCLUDED_PARTS)) {
      return _includedParts;
    } else if (identical(descriptor, DartEntry.IS_CLIENT)) {
      return getFlag(_CLIENT_CODE_INDEX);
    } else if (identical(descriptor, DartEntry.IS_LAUNCHABLE)) {
      return getFlag(_LAUNCHABLE_INDEX);
    } else if (identical(descriptor, DartEntry.PARSE_ERRORS)) {
      return _parseErrors;
    } else if (identical(descriptor, DartEntry.PARSED_UNIT)) {
      _parsedUnitAccessed = true;
      return _parsedUnit;
    } else if (identical(descriptor, DartEntry.PUBLIC_NAMESPACE)) {
      return _publicNamespace;
    } else if (identical(descriptor, DartEntry.SCAN_ERRORS)) {
      return _scanErrors;
    } else if (identical(descriptor, DartEntry.SOURCE_KIND)) {
      return _sourceKind;
    } else if (identical(descriptor, DartEntry.TOKEN_STREAM)) {
      return _tokenStream;
    }
    return super.getValue(descriptor);
  }

  @override
  Object getValueInLibrary(DataDescriptor descriptor, Source librarySource) {
    DartEntryImpl_ResolutionState state = _resolutionState;
    while (state != null) {
      if (librarySource == state._librarySource) {
        if (identical(descriptor, DartEntry.RESOLUTION_ERRORS)) {
          return state._resolutionErrors;
        } else if (identical(descriptor, DartEntry.RESOLVED_UNIT)) {
          return state._resolvedUnit;
        } else if (identical(descriptor, DartEntry.VERIFICATION_ERRORS)) {
          return state._verificationErrors;
        } else if (identical(descriptor, DartEntry.HINTS)) {
          return state._hints;
        } else {
          throw new IllegalArgumentException("Invalid descriptor: ${descriptor}");
        }
      }
      state = state._nextState;
    }
    ;
    if (identical(descriptor, DartEntry.RESOLUTION_ERRORS) || identical(descriptor, DartEntry.VERIFICATION_ERRORS) || identical(descriptor, DartEntry.HINTS)) {
      return AnalysisError.NO_ERRORS;
    } else if (identical(descriptor, DartEntry.RESOLVED_UNIT)) {
      return null;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: ${descriptor}");
    }
  }

  @override
  DartEntryImpl get writableCopy {
    DartEntryImpl copy = new DartEntryImpl();
    copy.copyFrom(this);
    return copy;
  }

  @override
  bool hasInvalidData(DataDescriptor descriptor) {
    if (identical(descriptor, DartEntry.ELEMENT)) {
      return _elementState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.EXPORTED_LIBRARIES)) {
      return _exportedLibrariesState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.IMPORTED_LIBRARIES)) {
      return _importedLibrariesState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.INCLUDED_PARTS)) {
      return _includedPartsState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.IS_CLIENT)) {
      return _clientServerState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.IS_LAUNCHABLE)) {
      return _launchableState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.PARSE_ERRORS)) {
      return _parseErrorsState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.PARSED_UNIT)) {
      return _parsedUnitState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.PUBLIC_NAMESPACE)) {
      return _publicNamespaceState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.SCAN_ERRORS)) {
      return _scanErrorsState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.SOURCE_KIND)) {
      return _sourceKindState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.TOKEN_STREAM)) {
      return _tokenStreamState == CacheState.INVALID;
    } else if (identical(descriptor, DartEntry.RESOLUTION_ERRORS) || identical(descriptor, DartEntry.RESOLVED_UNIT) || identical(descriptor, DartEntry.VERIFICATION_ERRORS) || identical(descriptor, DartEntry.HINTS)) {
      DartEntryImpl_ResolutionState state = _resolutionState;
      while (state != null) {
        if (identical(descriptor, DartEntry.RESOLUTION_ERRORS)) {
          return state._resolutionErrorsState == CacheState.INVALID;
        } else if (identical(descriptor, DartEntry.RESOLVED_UNIT)) {
          return state._resolvedUnitState == CacheState.INVALID;
        } else if (identical(descriptor, DartEntry.VERIFICATION_ERRORS)) {
          return state._verificationErrorsState == CacheState.INVALID;
        } else if (identical(descriptor, DartEntry.HINTS)) {
          return state._hintsState == CacheState.INVALID;
        }
      }
      return false;
    } else {
      return super.getState(descriptor) == CacheState.INVALID;
    }
  }

  @override
  bool get hasResolvableCompilationUnit {
    if (_parsedUnitState == CacheState.VALID) {
      return true;
    }
    DartEntryImpl_ResolutionState state = _resolutionState;
    while (state != null) {
      if (state._resolvedUnitState == CacheState.VALID) {
        return true;
      }
      state = state._nextState;
    }
    ;
    return false;
  }

  @override
  void invalidateAllInformation() {
    super.invalidateAllInformation();
    _scanErrors = AnalysisError.NO_ERRORS;
    _scanErrorsState = CacheState.INVALID;
    _tokenStream = null;
    _tokenStreamState = CacheState.INVALID;
    _sourceKind = SourceKind.UNKNOWN;
    _sourceKindState = CacheState.INVALID;
    _parseErrors = AnalysisError.NO_ERRORS;
    _parseErrorsState = CacheState.INVALID;
    _parsedUnit = null;
    _parsedUnitAccessed = false;
    _parsedUnitState = CacheState.INVALID;
    _importedLibraries = Source.EMPTY_ARRAY;
    _importedLibrariesState = CacheState.INVALID;
    _exportedLibraries = Source.EMPTY_ARRAY;
    _exportedLibrariesState = CacheState.INVALID;
    _includedParts = Source.EMPTY_ARRAY;
    _includedPartsState = CacheState.INVALID;
    _discardCachedResolutionInformation();
  }

  /**
   * Invalidate all of the resolution information associated with the compilation unit.
   */
  void invalidateAllResolutionInformation() {
    if (_parsedUnitState == CacheState.FLUSHED) {
      DartEntryImpl_ResolutionState state = _resolutionState;
      while (state != null) {
        if (state._resolvedUnitState == CacheState.VALID) {
          _parsedUnit = state._resolvedUnit;
          _parsedUnitAccessed = true;
          _parsedUnitState = CacheState.VALID;
          break;
        }
        state = state._nextState;
      }
    }
    _discardCachedResolutionInformation();
  }

  @override
  bool get isRefactoringSafe {
    DartEntryImpl_ResolutionState state = _resolutionState;
    while (state != null) {
      CacheState resolvedState = state._resolvedUnitState;
      if (resolvedState != CacheState.VALID && resolvedState != CacheState.FLUSHED) {
        return false;
      }
      state = state._nextState;
    }
    return true;
  }

  @override
  void recordContentError() {
    super.recordContentError();
    recordScanError();
  }

  /**
   * Record that an error occurred while attempting to scan or parse the entry represented by this
   * entry. This will set the state of all information, including any resolution-based information,
   * as being in error.
   */
  void recordParseError() {
    _sourceKind = SourceKind.UNKNOWN;
    _sourceKindState = CacheState.ERROR;
    _parseErrors = AnalysisError.NO_ERRORS;
    _parseErrorsState = CacheState.ERROR;
    _parsedUnit = null;
    _parsedUnitAccessed = false;
    _parsedUnitState = CacheState.ERROR;
    _exportedLibraries = Source.EMPTY_ARRAY;
    _exportedLibrariesState = CacheState.ERROR;
    _importedLibraries = Source.EMPTY_ARRAY;
    _importedLibrariesState = CacheState.ERROR;
    _includedParts = Source.EMPTY_ARRAY;
    _includedPartsState = CacheState.ERROR;
    recordResolutionError();
  }

  /**
   * Record that the parse-related information for the associated source is about to be computed by
   * the current thread.
   */
  void recordParseInProcess() {
    if (_sourceKindState != CacheState.VALID) {
      _sourceKindState = CacheState.IN_PROCESS;
    }
    if (_parseErrorsState != CacheState.VALID) {
      _parseErrorsState = CacheState.IN_PROCESS;
    }
    if (_parsedUnitState != CacheState.VALID) {
      _parsedUnitState = CacheState.IN_PROCESS;
    }
    if (_exportedLibrariesState != CacheState.VALID) {
      _exportedLibrariesState = CacheState.IN_PROCESS;
    }
    if (_importedLibrariesState != CacheState.VALID) {
      _importedLibrariesState = CacheState.IN_PROCESS;
    }
    if (_includedPartsState != CacheState.VALID) {
      _includedPartsState = CacheState.IN_PROCESS;
    }
  }

  /**
   * Record that an in-process parse has stopped without recording results because the results were
   * invalidated before they could be recorded.
   */
  void recordParseNotInProcess() {
    if (getState(SourceEntry.LINE_INFO) == CacheState.IN_PROCESS) {
      setState(SourceEntry.LINE_INFO, CacheState.INVALID);
    }
    if (_sourceKindState == CacheState.IN_PROCESS) {
      _sourceKindState = CacheState.INVALID;
    }
    if (_parseErrorsState == CacheState.IN_PROCESS) {
      _parseErrorsState = CacheState.INVALID;
    }
    if (_parsedUnitState == CacheState.IN_PROCESS) {
      _parsedUnitState = CacheState.INVALID;
    }
    if (_exportedLibrariesState == CacheState.IN_PROCESS) {
      _exportedLibrariesState = CacheState.INVALID;
    }
    if (_importedLibrariesState == CacheState.IN_PROCESS) {
      _importedLibrariesState = CacheState.INVALID;
    }
    if (_includedPartsState == CacheState.IN_PROCESS) {
      _includedPartsState = CacheState.INVALID;
    }
  }

  /**
   * Record that an error occurred while attempting to scan or parse the entry represented by this
   * entry. This will set the state of all resolution-based information as being in error, but will
   * not change the state of any parse results.
   */
  void recordResolutionError() {
    _element = null;
    _elementState = CacheState.ERROR;
    clearFlags([_LAUNCHABLE_INDEX, _CLIENT_CODE_INDEX]);
    _clientServerState = CacheState.ERROR;
    _launchableState = CacheState.ERROR;
    _publicNamespace = null;
    _publicNamespaceState = CacheState.ERROR;
    _resolutionState.recordResolutionError();
  }

  /**
   * Record that an in-process parse has stopped without recording results because the results were
   * invalidated before they could be recorded.
   */
  void recordResolutionNotInProcess() {
    if (_elementState == CacheState.IN_PROCESS) {
      _elementState = CacheState.INVALID;
    }
    if (_clientServerState == CacheState.IN_PROCESS) {
      _clientServerState = CacheState.INVALID;
    }
    if (_launchableState == CacheState.IN_PROCESS) {
      _launchableState = CacheState.INVALID;
    }
    if (_publicNamespaceState == CacheState.IN_PROCESS) {
      _publicNamespaceState = CacheState.INVALID;
    }
    _resolutionState.recordResolutionNotInProcess();
  }

  /**
   * Record that an error occurred while attempting to scan or parse the entry represented by this
   * entry. This will set the state of all information, including any resolution-based information,
   * as being in error.
   */
  void recordScanError() {
    setState(SourceEntry.LINE_INFO, CacheState.ERROR);
    _scanErrors = AnalysisError.NO_ERRORS;
    _scanErrorsState = CacheState.ERROR;
    _tokenStream = null;
    _tokenStreamState = CacheState.ERROR;
    recordParseError();
  }

  /**
   * Record that the scan-related information for the associated source is about to be computed by
   * the current thread.
   */
  void recordScanInProcess() {
    if (getState(SourceEntry.LINE_INFO) != CacheState.VALID) {
      setState(SourceEntry.LINE_INFO, CacheState.IN_PROCESS);
    }
    if (_scanErrorsState != CacheState.VALID) {
      _scanErrorsState = CacheState.IN_PROCESS;
    }
    if (_tokenStreamState != CacheState.VALID) {
      _tokenStreamState = CacheState.IN_PROCESS;
    }
  }

  /**
   * Record that an in-process scan has stopped without recording results because the results were
   * invalidated before they could be recorded.
   */
  void recordScanNotInProcess() {
    if (getState(SourceEntry.LINE_INFO) == CacheState.IN_PROCESS) {
      setState(SourceEntry.LINE_INFO, CacheState.INVALID);
    }
    if (_scanErrorsState == CacheState.IN_PROCESS) {
      _scanErrorsState = CacheState.INVALID;
    }
    if (_tokenStreamState == CacheState.IN_PROCESS) {
      _tokenStreamState = CacheState.INVALID;
    }
  }

  /**
   * Remove the given library from the list of libraries that contain this part. This method should
   * only be invoked on entries that represent a part.
   *
   * @param librarySource the source of the library to be removed
   */
  void removeContainingLibrary(Source librarySource) {
    _containingLibraries.remove(librarySource);
  }

  /**
   * Remove any resolution information associated with this compilation unit being part of the given
   * library, presumably because it is no longer part of the library.
   *
   * @param librarySource the source of the defining compilation unit of the library that used to
   *          contain this part but no longer does
   */
  void removeResolution(Source librarySource) {
    if (librarySource != null) {
      if (librarySource == _resolutionState._librarySource) {
        if (_resolutionState._nextState == null) {
          _resolutionState.invalidateAllResolutionInformation();
        } else {
          _resolutionState = _resolutionState._nextState;
        }
      } else {
        DartEntryImpl_ResolutionState priorState = _resolutionState;
        DartEntryImpl_ResolutionState state = _resolutionState._nextState;
        while (state != null) {
          if (librarySource == state._librarySource) {
            priorState._nextState = state._nextState;
            break;
          }
          priorState = state;
          state = state._nextState;
        }
      }
    }
  }

  /**
   * Set the list of libraries that contain this compilation unit to contain only the given source.
   * This method should only be invoked on entries that represent a library.
   *
   * @param librarySource the source of the single library that the list should contain
   */
  void set containingLibrary(Source librarySource) {
    _containingLibraries.clear();
    _containingLibraries.add(librarySource);
  }

  @override
  void setState(DataDescriptor descriptor, CacheState state) {
    if (identical(descriptor, DartEntry.ELEMENT)) {
      _element = updatedValue(state, _element, null);
      _elementState = state;
    } else if (identical(descriptor, DartEntry.EXPORTED_LIBRARIES)) {
      _exportedLibraries = updatedValue(state, _exportedLibraries, Source.EMPTY_ARRAY);
      _exportedLibrariesState = state;
    } else if (identical(descriptor, DartEntry.IMPORTED_LIBRARIES)) {
      _importedLibraries = updatedValue(state, _importedLibraries, Source.EMPTY_ARRAY);
      _importedLibrariesState = state;
    } else if (identical(descriptor, DartEntry.INCLUDED_PARTS)) {
      _includedParts = updatedValue(state, _includedParts, Source.EMPTY_ARRAY);
      _includedPartsState = state;
    } else if (identical(descriptor, DartEntry.IS_CLIENT)) {
      _updateValueOfFlag(_CLIENT_CODE_INDEX, state);
      _clientServerState = state;
    } else if (identical(descriptor, DartEntry.IS_LAUNCHABLE)) {
      _updateValueOfFlag(_LAUNCHABLE_INDEX, state);
      _launchableState = state;
    } else if (identical(descriptor, DartEntry.PARSE_ERRORS)) {
      _parseErrors = updatedValue(state, _parseErrors, AnalysisError.NO_ERRORS);
      _parseErrorsState = state;
    } else if (identical(descriptor, DartEntry.PARSED_UNIT)) {
      CompilationUnit newUnit = updatedValue(state, _parsedUnit, null);
      if (!identical(newUnit, _parsedUnit)) {
        _parsedUnitAccessed = false;
      }
      _parsedUnit = newUnit;
      _parsedUnitState = state;
    } else if (identical(descriptor, DartEntry.PUBLIC_NAMESPACE)) {
      _publicNamespace = updatedValue(state, _publicNamespace, null);
      _publicNamespaceState = state;
    } else if (identical(descriptor, DartEntry.SCAN_ERRORS)) {
      _scanErrors = updatedValue(state, _scanErrors, AnalysisError.NO_ERRORS);
      _scanErrorsState = state;
    } else if (identical(descriptor, DartEntry.SOURCE_KIND)) {
      _sourceKind = updatedValue(state, _sourceKind, SourceKind.UNKNOWN);
      _sourceKindState = state;
    } else if (identical(descriptor, DartEntry.TOKEN_STREAM)) {
      _tokenStream = updatedValue(state, _tokenStream, null);
      _tokenStreamState = state;
    } else {
      super.setState(descriptor, state);
    }
  }

  /**
   * Set the state of the data represented by the given descriptor in the context of the given
   * library to the given state.
   *
   * @param descriptor the descriptor representing the data whose state is to be set
   * @param librarySource the source of the defining compilation unit of the library that is the
   *          context for the data
   * @param cacheState the new state of the data represented by the given descriptor
   */
  void setStateInLibrary(DataDescriptor descriptor, Source librarySource, CacheState cacheState) {
    DartEntryImpl_ResolutionState state = _getOrCreateResolutionState(librarySource);
    if (identical(descriptor, DartEntry.RESOLUTION_ERRORS)) {
      state._resolutionErrors = updatedValue(cacheState, state._resolutionErrors, AnalysisError.NO_ERRORS);
      state._resolutionErrorsState = cacheState;
    } else if (identical(descriptor, DartEntry.RESOLVED_UNIT)) {
      state._resolvedUnit = updatedValue(cacheState, state._resolvedUnit, null);
      state._resolvedUnitState = cacheState;
    } else if (identical(descriptor, DartEntry.VERIFICATION_ERRORS)) {
      state._verificationErrors = updatedValue(cacheState, state._verificationErrors, AnalysisError.NO_ERRORS);
      state._verificationErrorsState = cacheState;
    } else if (identical(descriptor, DartEntry.HINTS)) {
      state._hints = updatedValue(cacheState, state._hints, AnalysisError.NO_ERRORS);
      state._hintsState = cacheState;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: ${descriptor}");
    }
  }

  @override
  void setValue(DataDescriptor descriptor, Object value) {
    if (identical(descriptor, DartEntry.ANGULAR_ERRORS)) {
      _angularErrors = value == null ? AnalysisError.NO_ERRORS : (value as List<AnalysisError>);
    } else if (identical(descriptor, DartEntry.ELEMENT)) {
      _element = value as LibraryElement;
      _elementState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.EXPORTED_LIBRARIES)) {
      _exportedLibraries = value == null ? Source.EMPTY_ARRAY : (value as List<Source>);
      _exportedLibrariesState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.IMPORTED_LIBRARIES)) {
      _importedLibraries = value == null ? Source.EMPTY_ARRAY : (value as List<Source>);
      _importedLibrariesState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.INCLUDED_PARTS)) {
      _includedParts = value == null ? Source.EMPTY_ARRAY : (value as List<Source>);
      _includedPartsState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.IS_CLIENT)) {
      setFlag(_CLIENT_CODE_INDEX, value as bool);
      _clientServerState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.IS_LAUNCHABLE)) {
      setFlag(_LAUNCHABLE_INDEX, value as bool);
      _launchableState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.PARSE_ERRORS)) {
      _parseErrors = value == null ? AnalysisError.NO_ERRORS : (value as List<AnalysisError>);
      _parseErrorsState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.PARSED_UNIT)) {
      _parsedUnit = value as CompilationUnit;
      _parsedUnitAccessed = false;
      _parsedUnitState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.PUBLIC_NAMESPACE)) {
      _publicNamespace = value as Namespace;
      _publicNamespaceState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.SCAN_ERRORS)) {
      _scanErrors = value == null ? AnalysisError.NO_ERRORS : (value as List<AnalysisError>);
      _scanErrorsState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.SOURCE_KIND)) {
      _sourceKind = value as SourceKind;
      _sourceKindState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.TOKEN_STREAM)) {
      _tokenStream = value as Token;
      _tokenStreamState = CacheState.VALID;
    } else {
      super.setValue(descriptor, value);
    }
  }

  /**
   * Set the value of the data represented by the given descriptor in the context of the given
   * library to the given value, and set the state of that data to [CacheState#VALID].
   *
   * @param descriptor the descriptor representing which data is to have its value set
   * @param librarySource the source of the defining compilation unit of the library that is the
   *          context for the data
   * @param value the new value of the data represented by the given descriptor and library
   */
  void setValueInLibrary(DataDescriptor descriptor, Source librarySource, Object value) {
    DartEntryImpl_ResolutionState state = _getOrCreateResolutionState(librarySource);
    if (identical(descriptor, DartEntry.RESOLUTION_ERRORS)) {
      state._resolutionErrors = value == null ? AnalysisError.NO_ERRORS : (value as List<AnalysisError>);
      state._resolutionErrorsState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.RESOLVED_UNIT)) {
      state._resolvedUnit = value as CompilationUnit;
      state._resolvedUnitState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.VERIFICATION_ERRORS)) {
      state._verificationErrors = value == null ? AnalysisError.NO_ERRORS : (value as List<AnalysisError>);
      state._verificationErrorsState = CacheState.VALID;
    } else if (identical(descriptor, DartEntry.HINTS)) {
      state._hints = value == null ? AnalysisError.NO_ERRORS : (value as List<AnalysisError>);
      state._hintsState = CacheState.VALID;
    }
  }

  @override
  void copyFrom(SourceEntryImpl entry) {
    super.copyFrom(entry);
    DartEntryImpl other = entry as DartEntryImpl;
    _scanErrorsState = other._scanErrorsState;
    _scanErrors = other._scanErrors;
    _tokenStreamState = other._tokenStreamState;
    _tokenStream = other._tokenStream;
    _sourceKindState = other._sourceKindState;
    _sourceKind = other._sourceKind;
    _parsedUnitState = other._parsedUnitState;
    _parsedUnit = other._parsedUnit;
    _parsedUnitAccessed = other._parsedUnitAccessed;
    _parseErrorsState = other._parseErrorsState;
    _parseErrors = other._parseErrors;
    _includedPartsState = other._includedPartsState;
    _includedParts = other._includedParts;
    _exportedLibrariesState = other._exportedLibrariesState;
    _exportedLibraries = other._exportedLibraries;
    _importedLibrariesState = other._importedLibrariesState;
    _importedLibraries = other._importedLibraries;
    _containingLibraries = new List<Source>.from(other._containingLibraries);
    _resolutionState.copyFrom(other._resolutionState);
    _elementState = other._elementState;
    _element = other._element;
    _publicNamespaceState = other._publicNamespaceState;
    _publicNamespace = other._publicNamespace;
    _clientServerState = other._clientServerState;
    _launchableState = other._launchableState;
    _angularErrors = other._angularErrors;
  }

  @override
  bool get hasErrorState => super.hasErrorState || _scanErrorsState == CacheState.ERROR || _tokenStreamState == CacheState.ERROR || _sourceKindState == CacheState.ERROR || _parsedUnitState == CacheState.ERROR || _parseErrorsState == CacheState.ERROR || _importedLibrariesState == CacheState.ERROR || _exportedLibrariesState == CacheState.ERROR || _includedPartsState == CacheState.ERROR || _elementState == CacheState.ERROR || _publicNamespaceState == CacheState.ERROR || _clientServerState == CacheState.ERROR || _launchableState == CacheState.ERROR || _resolutionState.hasErrorState;

  @override
  void writeOn(JavaStringBuilder builder) {
    builder.append("Dart: ");
    super.writeOn(builder);
    builder.append("; tokenStream = ");
    builder.append(_tokenStreamState);
    builder.append("; scanErrors = ");
    builder.append(_scanErrorsState);
    builder.append("; sourceKind = ");
    builder.append(_sourceKindState);
    builder.append("; parsedUnit = ");
    builder.append(_parsedUnitState);
    builder.append(" (");
    builder.append(_parsedUnitAccessed ? "T" : "F");
    builder.append("); parseErrors = ");
    builder.append(_parseErrorsState);
    builder.append("; exportedLibraries = ");
    builder.append(_exportedLibrariesState);
    builder.append("; importedLibraries = ");
    builder.append(_importedLibrariesState);
    builder.append("; includedParts = ");
    builder.append(_includedPartsState);
    builder.append("; element = ");
    builder.append(_elementState);
    builder.append("; publicNamespace = ");
    builder.append(_publicNamespaceState);
    builder.append("; clientServer = ");
    builder.append(_clientServerState);
    builder.append("; launchable = ");
    builder.append(_launchableState);
    //    builder.append("; angularElements = ");
    _resolutionState.writeOn(builder);
  }

  /**
   * Invalidate all of the resolution information associated with the compilation unit.
   */
  void _discardCachedResolutionInformation() {
    _element = null;
    _elementState = CacheState.INVALID;
    clearFlags([_LAUNCHABLE_INDEX, _CLIENT_CODE_INDEX]);
    _clientServerState = CacheState.INVALID;
    _launchableState = CacheState.INVALID;
    _publicNamespace = null;
    _publicNamespaceState = CacheState.INVALID;
    _resolutionState.invalidateAllResolutionInformation();
  }

  /**
   * Return a resolution state for the specified library, creating one as necessary.
   *
   * @param librarySource the library source (not `null`)
   * @return the resolution state (not `null`)
   */
  DartEntryImpl_ResolutionState _getOrCreateResolutionState(Source librarySource) {
    DartEntryImpl_ResolutionState state = _resolutionState;
    if (state._librarySource == null) {
      state._librarySource = librarySource;
      return state;
    }
    while (state._librarySource != librarySource) {
      if (state._nextState == null) {
        DartEntryImpl_ResolutionState newState = new DartEntryImpl_ResolutionState();
        newState._librarySource = librarySource;
        state._nextState = newState;
        return newState;
      }
      state = state._nextState;
    }
    return state;
  }

  /**
   * Given that the specified flag is being transitioned to the given state, set the value of the
   * flag to the value that should be kept in the cache.
   *
   * @param index the index of the flag whose state is being set
   * @param state the state to which the value is being transitioned
   */
  void _updateValueOfFlag(int index, CacheState state) {
    if (state == CacheState.VALID) {
      throw new IllegalArgumentException("Use setValue() to set the state to VALID");
    } else if (state != CacheState.IN_PROCESS) {
      //
      // If the value is in process, we can leave the current value in the cache for any 'get'
      // methods to access.
      //
      setFlag(index, false);
    }
  }
}

/**
 * Instances of the class `ResolutionState` represent the information produced by resolving
 * a compilation unit as part of a specific library.
 */
class DartEntryImpl_ResolutionState {
  /**
   * The next resolution state or `null` if none.
   */
  DartEntryImpl_ResolutionState _nextState;

  /**
   * The source for the defining compilation unit of the library that contains this unit. If this
   * unit is the defining compilation unit for it's library, then this will be the source for this
   * unit.
   */
  Source _librarySource;

  /**
   * The state of the cached resolved compilation unit.
   */
  CacheState _resolvedUnitState = CacheState.INVALID;

  /**
   * The resolved compilation unit, or `null` if the resolved compilation unit is not
   * currently cached.
   */
  CompilationUnit _resolvedUnit;

  /**
   * The state of the cached resolution errors.
   */
  CacheState _resolutionErrorsState = CacheState.INVALID;

  /**
   * The errors produced while resolving the compilation unit, or an empty array if the errors are
   * not currently cached.
   */
  List<AnalysisError> _resolutionErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached verification errors.
   */
  CacheState _verificationErrorsState = CacheState.INVALID;

  /**
   * The errors produced while verifying the compilation unit, or an empty array if the errors are
   * not currently cached.
   */
  List<AnalysisError> _verificationErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached hints.
   */
  CacheState _hintsState = CacheState.INVALID;

  /**
   * The hints produced while auditing the compilation unit, or an empty array if the hints are
   * not currently cached.
   */
  List<AnalysisError> _hints = AnalysisError.NO_ERRORS;

  /**
   * Set this state to be exactly like the given state, recursively copying the next state as
   * necessary.
   *
   * @param other the state to be copied
   */
  void copyFrom(DartEntryImpl_ResolutionState other) {
    _librarySource = other._librarySource;
    _resolvedUnitState = other._resolvedUnitState;
    _resolvedUnit = other._resolvedUnit;
    _resolutionErrorsState = other._resolutionErrorsState;
    _resolutionErrors = other._resolutionErrors;
    _verificationErrorsState = other._verificationErrorsState;
    _verificationErrors = other._verificationErrors;
    _hintsState = other._hintsState;
    _hints = other._hints;
    if (other._nextState != null) {
      _nextState = new DartEntryImpl_ResolutionState();
      _nextState.copyFrom(other._nextState);
    }
  }

  /**
   * Flush any AST structures being maintained by this state.
   */
  void flushAstStructures() {
    if (_resolvedUnitState == CacheState.VALID) {
      _resolvedUnitState = CacheState.FLUSHED;
      _resolvedUnit = null;
    }
    if (_nextState != null) {
      _nextState.flushAstStructures();
    }
  }

  bool get hasErrorState => _resolvedUnitState == CacheState.ERROR || _resolutionErrorsState == CacheState.ERROR || _verificationErrorsState == CacheState.ERROR || _hintsState == CacheState.ERROR || (_nextState != null && _nextState.hasErrorState);

  /**
   * Invalidate all of the resolution information associated with the compilation unit.
   */
  void invalidateAllResolutionInformation() {
    _nextState = null;
    _librarySource = null;
    _resolvedUnitState = CacheState.INVALID;
    _resolvedUnit = null;
    _resolutionErrorsState = CacheState.INVALID;
    _resolutionErrors = AnalysisError.NO_ERRORS;
    _verificationErrorsState = CacheState.INVALID;
    _verificationErrors = AnalysisError.NO_ERRORS;
    _hintsState = CacheState.INVALID;
    _hints = AnalysisError.NO_ERRORS;
  }

  /**
   * Record that an error occurred while attempting to scan or parse the entry represented by this
   * entry. This will set the state of all resolution-based information as being in error, but
   * will not change the state of any parse results.
   */
  void recordResolutionError() {
    _resolvedUnitState = CacheState.ERROR;
    _resolvedUnit = null;
    _resolutionErrorsState = CacheState.ERROR;
    _resolutionErrors = AnalysisError.NO_ERRORS;
    _verificationErrorsState = CacheState.ERROR;
    _verificationErrors = AnalysisError.NO_ERRORS;
    _hintsState = CacheState.ERROR;
    _hints = AnalysisError.NO_ERRORS;
    if (_nextState != null) {
      _nextState.recordResolutionError();
    }
  }

  /**
   * Record that an in-process parse has stopped without recording results because the results
   * were invalidated before they could be recorded.
   */
  void recordResolutionNotInProcess() {
    if (_resolvedUnitState == CacheState.IN_PROCESS) {
      _resolvedUnitState = CacheState.INVALID;
    }
    if (_resolutionErrorsState == CacheState.IN_PROCESS) {
      _resolutionErrorsState = CacheState.INVALID;
    }
    if (_verificationErrorsState == CacheState.IN_PROCESS) {
      _verificationErrorsState = CacheState.INVALID;
    }
    if (_hintsState == CacheState.IN_PROCESS) {
      _hintsState = CacheState.INVALID;
    }
    if (_nextState != null) {
      _nextState.recordResolutionNotInProcess();
    }
  }

  /**
   * Write a textual representation of this state to the given builder. The result will only be
   * used for debugging purposes.
   *
   * @param builder the builder to which the text should be written
   */
  void writeOn(JavaStringBuilder builder) {
    if (_librarySource != null) {
      builder.append("; resolvedUnit = ");
      builder.append(_resolvedUnitState);
      builder.append("; resolutionErrors = ");
      builder.append(_resolutionErrorsState);
      builder.append("; verificationErrors = ");
      builder.append(_verificationErrorsState);
      builder.append("; hints = ");
      builder.append(_hintsState);
      if (_nextState != null) {
        _nextState.writeOn(builder);
      }
    }
  }
}

/**
 * Instances of the class `DataDescriptor` are immutable constants representing data that can
 * be stored in the cache.
 */
class DataDescriptor<E> {
  /**
   * The name of the descriptor, used for debugging purposes.
   */
  final String _name;

  /**
   * Initialize a newly created descriptor to have the given name.
   *
   * @param name the name of the descriptor
   */
  DataDescriptor(this._name);

  @override
  String toString() => _name;
}

/**
 * The interface `HtmlEntry` defines the behavior of objects that maintain the information
 * cached by an analysis context about an individual HTML file.
 */
abstract class HtmlEntry implements SourceEntry {
  /**
   * The data descriptor representing the information about an Angular application this source is
   * used in.
   */
  static final DataDescriptor<AngularApplication> ANGULAR_APPLICATION = new DataDescriptor<AngularApplication>("HtmlEntry.ANGULAR_APPLICATION");

  /**
   * The data descriptor representing the information about an Angular component this source is used
   * as template for.
   */
  static final DataDescriptor<AngularComponentElement> ANGULAR_COMPONENT = new DataDescriptor<AngularComponentElement>("HtmlEntry.ANGULAR_COMPONENT");

  /**
   * The data descriptor representing the information about an Angular application this source is
   * entry point for.
   */
  static final DataDescriptor<AngularApplication> ANGULAR_ENTRY = new DataDescriptor<AngularApplication>("HtmlEntry.ANGULAR_ENTRY");

  /**
   * The data descriptor representing the errors reported during Angular resolution.
   */
  static final DataDescriptor<List<AnalysisError>> ANGULAR_ERRORS = new DataDescriptor<List<AnalysisError>>("HtmlEntry.ANGULAR_ERRORS");

  /**
   * The data descriptor representing the HTML element.
   */
  static final DataDescriptor<HtmlElement> ELEMENT = new DataDescriptor<HtmlElement>("HtmlEntry.ELEMENT");

  /**
   * The data descriptor representing the hints resulting from auditing the source.
   */
  static final DataDescriptor<List<AnalysisError>> HINTS = new DataDescriptor<List<AnalysisError>>("HtmlEntry.HINTS");

  /**
   * The data descriptor representing the errors resulting from parsing the source.
   */
  static final DataDescriptor<List<AnalysisError>> PARSE_ERRORS = new DataDescriptor<List<AnalysisError>>("HtmlEntry.PARSE_ERRORS");

  /**
   * The data descriptor representing the parsed AST structure.
   */
  static final DataDescriptor<ht.HtmlUnit> PARSED_UNIT = new DataDescriptor<ht.HtmlUnit>("HtmlEntry.PARSED_UNIT");

  /**
   * The data descriptor representing the resolved AST structure.
   */
  static final DataDescriptor<ht.HtmlUnit> RESOLVED_UNIT = new DataDescriptor<ht.HtmlUnit>("HtmlEntry.RESOLVED_UNIT");

  /**
   * The data descriptor representing the list of referenced libraries.
   */
  static final DataDescriptor<List<Source>> REFERENCED_LIBRARIES = new DataDescriptor<List<Source>>("HtmlEntry.REFERENCED_LIBRARIES");

  /**
   * The data descriptor representing the errors resulting from resolving the source.
   */
  static final DataDescriptor<List<AnalysisError>> RESOLUTION_ERRORS = new DataDescriptor<List<AnalysisError>>("HtmlEntry.RESOLUTION_ERRORS");

  /**
   * Return all of the errors associated with the compilation unit that are currently cached.
   *
   * @return all of the errors associated with the compilation unit
   */
  List<AnalysisError> get allErrors;

  /**
   * Return a valid parsed unit, either an unresolved AST structure or the result of resolving the
   * AST structure, or `null` if there is no parsed unit available.
   *
   * @return a valid parsed unit
   */
  ht.HtmlUnit get anyParsedUnit;

  @override
  HtmlEntryImpl get writableCopy;
}

/**
 * Instances of the class `HtmlEntryImpl` implement an [HtmlEntry].
 */
class HtmlEntryImpl extends SourceEntryImpl implements HtmlEntry {
  /**
   * The state of the cached parsed (but not resolved) HTML unit.
   */
  CacheState _parsedUnitState = CacheState.INVALID;

  /**
   * The parsed HTML unit, or `null` if the parsed HTML unit is not currently cached.
   */
  ht.HtmlUnit _parsedUnit;

  /**
   * The state of the cached resolved HTML unit.
   */
  CacheState _resolvedUnitState = CacheState.INVALID;

  /**
   * The resolved HTML unit, or `null` if the resolved HTML unit is not currently cached.
   */
  ht.HtmlUnit _resolvedUnit;

  /**
   * The state of the cached parse errors.
   */
  CacheState _parseErrorsState = CacheState.INVALID;

  /**
   * The errors produced while scanning and parsing the HTML, or `null` if the errors are not
   * currently cached.
   */
  List<AnalysisError> _parseErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached resolution errors.
   */
  CacheState _resolutionErrorsState = CacheState.INVALID;

  /**
   * The errors produced while resolving the HTML, or `null` if the errors are not currently
   * cached.
   */
  List<AnalysisError> _resolutionErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached list of referenced libraries.
   */
  CacheState _referencedLibrariesState = CacheState.INVALID;

  /**
   * The list of libraries referenced in the HTML, or `null` if the list is not currently
   * cached. Note that this list does not include libraries defined directly within the HTML file.
   */
  List<Source> _referencedLibraries = Source.EMPTY_ARRAY;

  /**
   * The state of the cached HTML element.
   */
  CacheState _elementState = CacheState.INVALID;

  /**
   * The element representing the HTML file, or `null` if the element is not currently cached.
   */
  HtmlElement _element;

  /**
   * The state of the [angularApplication].
   */
  CacheState _angularApplicationState = CacheState.VALID;

  /**
   * Information about the Angular Application this unit is used in.
   */
  AngularApplication _angularApplication;

  /**
   * The state of the [angularEntry].
   */
  CacheState _angularEntryState = CacheState.INVALID;

  /**
   * Information about the Angular Application this unit is entry point for.
   */
  AngularApplication _angularEntry = null;

  /**
   * The state of the [angularComponent].
   */
  CacheState _angularComponentState = CacheState.VALID;

  /**
   * Information about the [AngularComponentElement] this unit is used as template for.
   */
  AngularComponentElement _angularComponent = null;

  /**
   * The state of the Angular resolution errors.
   */
  CacheState _angularErrorsState = CacheState.INVALID;

  /**
   * The hints produced while performing Angular resolution, or an empty array if the error are not
   * currently cached.
   */
  List<AnalysisError> _angularErrors = AnalysisError.NO_ERRORS;

  /**
   * The state of the cached hints.
   */
  CacheState _hintsState = CacheState.INVALID;

  /**
   * The hints produced while auditing the compilation unit, or an empty array if the hints are not
   * currently cached.
   */
  List<AnalysisError> _hints = AnalysisError.NO_ERRORS;

  /**
   * Flush any AST structures being maintained by this entry.
   */
  void flushAstStructures() {
    if (_parsedUnitState == CacheState.VALID) {
      _parsedUnitState = CacheState.FLUSHED;
      _parsedUnit = null;
    }
    if (_resolvedUnitState == CacheState.VALID) {
      _resolvedUnitState = CacheState.FLUSHED;
      _resolvedUnit = null;
    }
    if (_angularEntryState == CacheState.VALID) {
      _angularEntryState = CacheState.FLUSHED;
    }
    if (_angularErrorsState == CacheState.VALID) {
      _angularErrorsState = CacheState.FLUSHED;
    }
  }

  @override
  List<AnalysisError> get allErrors {
    List<AnalysisError> errors = new List<AnalysisError>();
    if (_parseErrors != null) {
      for (AnalysisError error in _parseErrors) {
        errors.add(error);
      }
    }
    if (_resolutionErrors != null) {
      for (AnalysisError error in _resolutionErrors) {
        errors.add(error);
      }
    }
    if (_angularErrors != null) {
      for (AnalysisError error in _angularErrors) {
        errors.add(error);
      }
    }
    if (_hints != null) {
      for (AnalysisError error in _hints) {
        errors.add(error);
      }
    }
    if (errors.length == 0) {
      return AnalysisError.NO_ERRORS;
    }
    return new List.from(errors);
  }

  @override
  ht.HtmlUnit get anyParsedUnit {
    if (_parsedUnitState == CacheState.VALID) {
      //      parsedUnitAccessed = true;
      return _parsedUnit;
    }
    if (_resolvedUnitState == CacheState.VALID) {
      //      resovledUnitAccessed = true;
      return _resolvedUnit;
    }
    return null;
  }

  @override
  SourceKind get kind => SourceKind.HTML;

  @override
  CacheState getState(DataDescriptor descriptor) {
    if (identical(descriptor, HtmlEntry.ANGULAR_APPLICATION)) {
      return _angularApplicationState;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_COMPONENT)) {
      return _angularComponentState;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_ENTRY)) {
      return _angularEntryState;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_ERRORS)) {
      return _angularErrorsState;
    } else if (identical(descriptor, HtmlEntry.ELEMENT)) {
      return _elementState;
    } else if (identical(descriptor, HtmlEntry.PARSE_ERRORS)) {
      return _parseErrorsState;
    } else if (identical(descriptor, HtmlEntry.PARSED_UNIT)) {
      return _parsedUnitState;
    } else if (identical(descriptor, HtmlEntry.RESOLVED_UNIT)) {
      return _resolvedUnitState;
    } else if (identical(descriptor, HtmlEntry.REFERENCED_LIBRARIES)) {
      return _referencedLibrariesState;
    } else if (identical(descriptor, HtmlEntry.RESOLUTION_ERRORS)) {
      return _resolutionErrorsState;
    } else if (identical(descriptor, HtmlEntry.HINTS)) {
      return _hintsState;
    }
    return super.getState(descriptor);
  }

  @override
  Object getValue(DataDescriptor descriptor) {
    if (identical(descriptor, HtmlEntry.ANGULAR_APPLICATION)) {
      return _angularApplication;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_COMPONENT)) {
      return _angularComponent;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_ENTRY)) {
      return _angularEntry;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_ERRORS)) {
      return _angularErrors;
    } else if (identical(descriptor, HtmlEntry.ELEMENT)) {
      return _element;
    } else if (identical(descriptor, HtmlEntry.PARSE_ERRORS)) {
      return _parseErrors;
    } else if (identical(descriptor, HtmlEntry.PARSED_UNIT)) {
      return _parsedUnit;
    } else if (identical(descriptor, HtmlEntry.RESOLVED_UNIT)) {
      return _resolvedUnit;
    } else if (identical(descriptor, HtmlEntry.REFERENCED_LIBRARIES)) {
      return _referencedLibraries;
    } else if (identical(descriptor, HtmlEntry.RESOLUTION_ERRORS)) {
      return _resolutionErrors;
    } else if (identical(descriptor, HtmlEntry.HINTS)) {
      return _hints;
    }
    return super.getValue(descriptor);
  }

  @override
  HtmlEntryImpl get writableCopy {
    HtmlEntryImpl copy = new HtmlEntryImpl();
    copy.copyFrom(this);
    return copy;
  }

  @override
  void invalidateAllInformation() {
    super.invalidateAllInformation();
    _parseErrors = AnalysisError.NO_ERRORS;
    _parseErrorsState = CacheState.INVALID;
    _parsedUnit = null;
    _parsedUnitState = CacheState.INVALID;
    _resolvedUnit = null;
    _resolvedUnitState = CacheState.INVALID;
    _referencedLibraries = Source.EMPTY_ARRAY;
    _referencedLibrariesState = CacheState.INVALID;
    invalidateAllResolutionInformation();
  }

  /**
   * Invalidate all of the resolution information associated with the HTML file.
   */
  void invalidateAllResolutionInformation() {
    _angularEntry = null;
    _angularEntryState = CacheState.INVALID;
    _angularErrors = AnalysisError.NO_ERRORS;
    _angularErrorsState = CacheState.INVALID;
    _element = null;
    _elementState = CacheState.INVALID;
    _resolutionErrors = AnalysisError.NO_ERRORS;
    _resolutionErrorsState = CacheState.INVALID;
    _hints = AnalysisError.NO_ERRORS;
    _hintsState = CacheState.INVALID;
  }

  @override
  void recordContentError() {
    super.recordContentError();
    recordParseError();
  }

  /**
   * Record that an error was encountered while attempting to parse the source associated with this
   * entry.
   */
  void recordParseError() {
    setState(SourceEntry.LINE_INFO, CacheState.ERROR);
    setState(HtmlEntry.PARSE_ERRORS, CacheState.ERROR);
    setState(HtmlEntry.PARSED_UNIT, CacheState.ERROR);
    setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.ERROR);
    recordResolutionError();
  }

  /**
   * Record that an error was encountered while attempting to resolve the source associated with
   * this entry.
   */
  void recordResolutionError() {
    setState(HtmlEntry.ANGULAR_ERRORS, CacheState.ERROR);
    setState(HtmlEntry.RESOLVED_UNIT, CacheState.ERROR);
    setState(HtmlEntry.ELEMENT, CacheState.ERROR);
    setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.ERROR);
    setState(HtmlEntry.HINTS, CacheState.ERROR);
  }

  @override
  void setState(DataDescriptor descriptor, CacheState state) {
    if (identical(descriptor, HtmlEntry.ANGULAR_APPLICATION)) {
      _angularApplication = updatedValue(state, _angularApplication, null);
      _angularApplicationState = state;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_COMPONENT)) {
      _angularComponent = updatedValue(state, _angularComponent, null);
      _angularComponentState = state;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_ENTRY)) {
      _angularEntry = updatedValue(state, _angularEntry, null);
      _angularEntryState = state;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_ERRORS)) {
      _angularErrors = updatedValue(state, _angularErrors, null);
      _angularErrorsState = state;
    } else if (identical(descriptor, HtmlEntry.ELEMENT)) {
      _element = updatedValue(state, _element, null);
      _elementState = state;
    } else if (identical(descriptor, HtmlEntry.PARSE_ERRORS)) {
      _parseErrors = updatedValue(state, _parseErrors, null);
      _parseErrorsState = state;
    } else if (identical(descriptor, HtmlEntry.PARSED_UNIT)) {
      _parsedUnit = updatedValue(state, _parsedUnit, null);
      _parsedUnitState = state;
    } else if (identical(descriptor, HtmlEntry.RESOLVED_UNIT)) {
      _resolvedUnit = updatedValue(state, _resolvedUnit, null);
      _resolvedUnitState = state;
    } else if (identical(descriptor, HtmlEntry.REFERENCED_LIBRARIES)) {
      _referencedLibraries = updatedValue(state, _referencedLibraries, Source.EMPTY_ARRAY);
      _referencedLibrariesState = state;
    } else if (identical(descriptor, HtmlEntry.RESOLUTION_ERRORS)) {
      _resolutionErrors = updatedValue(state, _resolutionErrors, AnalysisError.NO_ERRORS);
      _resolutionErrorsState = state;
    } else if (identical(descriptor, HtmlEntry.HINTS)) {
      _hints = updatedValue(state, _hints, AnalysisError.NO_ERRORS);
      _hintsState = state;
    } else {
      super.setState(descriptor, state);
    }
  }

  @override
  void setValue(DataDescriptor descriptor, Object value) {
    if (identical(descriptor, HtmlEntry.ANGULAR_APPLICATION)) {
      _angularApplication = value as AngularApplication;
      _angularApplicationState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_COMPONENT)) {
      _angularComponent = value as AngularComponentElement;
      _angularComponentState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_ENTRY)) {
      _angularEntry = value as AngularApplication;
      _angularEntryState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.ANGULAR_ERRORS)) {
      _angularErrors = value as List<AnalysisError>;
      _angularErrorsState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.ELEMENT)) {
      _element = value as HtmlElement;
      _elementState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.PARSE_ERRORS)) {
      _parseErrors = value as List<AnalysisError>;
      _parseErrorsState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.PARSED_UNIT)) {
      _parsedUnit = value as ht.HtmlUnit;
      _parsedUnitState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.RESOLVED_UNIT)) {
      _resolvedUnit = value as ht.HtmlUnit;
      _resolvedUnitState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.REFERENCED_LIBRARIES)) {
      _referencedLibraries = value == null ? Source.EMPTY_ARRAY : (value as List<Source>);
      _referencedLibrariesState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.RESOLUTION_ERRORS)) {
      _resolutionErrors = value as List<AnalysisError>;
      _resolutionErrorsState = CacheState.VALID;
    } else if (identical(descriptor, HtmlEntry.HINTS)) {
      _hints = value as List<AnalysisError>;
      _hintsState = CacheState.VALID;
    } else {
      super.setValue(descriptor, value);
    }
  }

  @override
  void copyFrom(SourceEntryImpl entry) {
    super.copyFrom(entry);
    HtmlEntryImpl other = entry as HtmlEntryImpl;
    _angularApplicationState = other._angularApplicationState;
    _angularApplication = other._angularApplication;
    _angularComponentState = other._angularComponentState;
    _angularComponent = other._angularComponent;
    _angularEntryState = other._angularEntryState;
    _angularEntry = other._angularEntry;
    _angularErrorsState = other._angularErrorsState;
    _angularErrors = other._angularErrors;
    _parseErrorsState = other._parseErrorsState;
    _parseErrors = other._parseErrors;
    _parsedUnitState = other._parsedUnitState;
    _parsedUnit = other._parsedUnit;
    _resolvedUnitState = other._resolvedUnitState;
    _resolvedUnit = other._resolvedUnit;
    _referencedLibrariesState = other._referencedLibrariesState;
    _referencedLibraries = other._referencedLibraries;
    _resolutionErrors = other._resolutionErrors;
    _resolutionErrorsState = other._resolutionErrorsState;
    _elementState = other._elementState;
    _element = other._element;
    _hints = other._hints;
    _hintsState = other._hintsState;
  }

  @override
  bool get hasErrorState => super.hasErrorState || _parsedUnitState == CacheState.ERROR || _resolvedUnitState == CacheState.ERROR || _parseErrorsState == CacheState.ERROR || _resolutionErrorsState == CacheState.ERROR || _referencedLibrariesState == CacheState.ERROR || _elementState == CacheState.ERROR || _angularErrorsState == CacheState.ERROR || _hintsState == CacheState.ERROR;

  @override
  void writeOn(JavaStringBuilder builder) {
    builder.append("Html: ");
    super.writeOn(builder);
    builder.append("; parseErrors = ");
    builder.append(_parseErrorsState);
    builder.append("; parsedUnit = ");
    builder.append(_parsedUnitState);
    builder.append("; resolvedUnit = ");
    builder.append(_resolvedUnitState);
    builder.append("; resolutionErrors = ");
    builder.append(_resolutionErrorsState);
    builder.append("; referencedLibraries = ");
    builder.append(_referencedLibrariesState);
    builder.append("; element = ");
    builder.append(_elementState);
    builder.append("; angularApplication = ");
    builder.append(_angularApplicationState);
    builder.append("; angularComponent = ");
    builder.append(_angularComponentState);
    builder.append("; angularEntry = ");
    builder.append(_angularEntryState);
    builder.append("; angularErrors = ");
    builder.append(_angularErrorsState);
  }
}

/**
 * The enumerated type `RetentionPriority` represents the priority of data in the cache in
 * terms of the desirability of retaining some specified data about a specified source.
 */
class RetentionPriority extends Enum<RetentionPriority> {
  /**
   * A priority indicating that a given piece of data can be removed from the cache without
   * reservation.
   */
  static const RetentionPriority LOW = const RetentionPriority('LOW', 0);

  /**
   * A priority indicating that a given piece of data should not be removed from the cache unless
   * there are no sources for which the corresponding data has a lower priority. Currently used for
   * data that is needed in order to finish some outstanding analysis task.
   */
  static const RetentionPriority MEDIUM = const RetentionPriority('MEDIUM', 1);

  /**
   * A priority indicating that a given piece of data should not be removed from the cache.
   * Currently used for data related to a priority source.
   */
  static const RetentionPriority HIGH = const RetentionPriority('HIGH', 2);

  static const List<RetentionPriority> values = const [LOW, MEDIUM, HIGH];

  const RetentionPriority(String name, int ordinal) : super(name, ordinal);
}

/**
 * The interface `SourceEntry` defines the behavior of objects that maintain the information
 * cached by an analysis context about an individual source, no matter what kind of source it is.
 *
 * Source entries should be treated as if they were immutable unless a writable copy of the entry
 * has been obtained and has not yet been made visible to other threads.
 */
abstract class SourceEntry {
  /**
   * The data descriptor representing the contents of the source.
   */
  static final DataDescriptor<String> CONTENT = new DataDescriptor<String>("DartEntry.CONTENT");

  /**
   * The data descriptor representing the line information.
   */
  static final DataDescriptor<LineInfo> LINE_INFO = new DataDescriptor<LineInfo>("SourceEntry.LINE_INFO");

  /**
   * Return the exception that caused one or more values to have a state of [CacheState#ERROR]
   * .
   *
   * @return the exception that caused one or more values to be uncomputable
   */
  AnalysisException get exception;

  /**
   * Return `true` if the source was explicitly added to the context or `false` if the
   * source was implicitly added because it was referenced by another source.
   *
   * @return `true` if the source was explicitly added to the context
   */
  bool get explicitlyAdded;

  /**
   * Return the kind of the source, or `null` if the kind is not currently cached.
   *
   * @return the kind of the source
   */
  SourceKind get kind;

  /**
   * Return the most recent time at which the state of the source matched the state represented by
   * this entry.
   *
   * @return the modification time of this entry
   */
  int get modificationTime;

  /**
   * Return the state of the data represented by the given descriptor.
   *
   * @param descriptor the descriptor representing the data whose state is to be returned
   * @return the state of the data represented by the given descriptor
   */
  CacheState getState(DataDescriptor descriptor);

  /**
   * Return the value of the data represented by the given descriptor, or `null` if the data
   * represented by the descriptor is not in the cache.
   *
   * @param descriptor the descriptor representing which data is to be returned
   * @return the value of the data represented by the given descriptor
   */
  Object getValue(DataDescriptor descriptor);

  /**
   * Return a new entry that is initialized to the same state as this entry but that can be
   * modified.
   *
   * @return a writable copy of this entry
   */
  SourceEntryImpl get writableCopy;
}

/**
 * Instances of the abstract class `SourceEntryImpl` implement the behavior common to all
 * [SourceEntry].
 */
abstract class SourceEntryImpl implements SourceEntry {
  /**
   * The most recent time at which the state of the source matched the state represented by this
   * entry.
   */
  int _modificationTime = 0;

  /**
   * A bit-encoding of boolean flags associated with this element.
   */
  int _flags = 0;

  /**
   * The exception that caused one or more values to have a state of [CacheState#ERROR].
   */
  AnalysisException exception;

  /**
   * The state of the cached content.
   */
  CacheState _contentState = CacheState.INVALID;

  /**
   * The content of the source, or `null` if the content is not currently cached.
   */
  String _content;

  /**
   * The state of the cached line information.
   */
  CacheState _lineInfoState = CacheState.INVALID;

  /**
   * The line information computed for the source, or `null` if the line information is not
   * currently cached.
   */
  LineInfo _lineInfo;

  /**
   * The index of the flag indicating whether the source was explicitly added to the context or
   * whether the source was implicitly added because it was referenced by another source.
   */
  static int _EXPLICITLY_ADDED_FLAG = 0;

  /**
   * Fix the state of the [exception] to match the current state of the entry.
   */
  void fixExceptionState() {
    if (hasErrorState) {
      if (exception == null) {
        //
        // This code should never be reached, but is a fail-safe in case an exception is not
        // recorded when it should be.
        //
        exception = new AnalysisException.con1("State set to ERROR without setting an exception");
      }
    } else {
      exception = null;
    }
  }

  /**
   * Return `true` if the source was explicitly added to the context or `false` if the
   * source was implicitly added because it was referenced by another source.
   *
   * @return `true` if the source was explicitly added to the context
   */
  @override
  bool get explicitlyAdded => getFlag(_EXPLICITLY_ADDED_FLAG);

  @override
  int get modificationTime => _modificationTime;

  @override
  CacheState getState(DataDescriptor descriptor) {
    if (identical(descriptor, SourceEntry.CONTENT)) {
      return _contentState;
    } else if (identical(descriptor, SourceEntry.LINE_INFO)) {
      return _lineInfoState;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: ${descriptor}");
    }
  }

  @override
  Object getValue(DataDescriptor descriptor) {
    if (identical(descriptor, SourceEntry.CONTENT)) {
      return _content;
    } else if (identical(descriptor, SourceEntry.LINE_INFO)) {
      return _lineInfo;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: ${descriptor}");
    }
  }

  /**
   * Invalidate all of the information associated with this source.
   */
  void invalidateAllInformation() {
    _content = null;
    _contentState = CacheState.INVALID;
    _lineInfo = null;
    _lineInfoState = CacheState.INVALID;
  }

  /**
   * Record that an error occurred while attempting to get the contents of the source represented by
   * this entry. This will set the state of all information, including any resolution-based
   * information, as being in error.
   */
  void recordContentError() {
    _content = null;
    _contentState = CacheState.ERROR;
  }

  /**
   * Set whether the source was explicitly added to the context to match the given value.
   *
   * @param explicitlyAdded `true` if the source was explicitly added to the context
   */
  void set explicitlyAdded(bool explicitlyAdded) {
    setFlag(_EXPLICITLY_ADDED_FLAG, explicitlyAdded);
  }

  /**
   * Set the most recent time at which the state of the source matched the state represented by this
   * entry to the given time.
   *
   * @param time the new modification time of this entry
   */
  void set modificationTime(int time) {
    _modificationTime = time;
  }

  /**
   * Set the state of the data represented by the given descriptor to the given state.
   *
   * @param descriptor the descriptor representing the data whose state is to be set
   * @param the new state of the data represented by the given descriptor
   */
  void setState(DataDescriptor descriptor, CacheState state) {
    if (identical(descriptor, SourceEntry.CONTENT)) {
      _content = updatedValue(state, _content, null);
      _contentState = state;
    } else if (identical(descriptor, SourceEntry.LINE_INFO)) {
      _lineInfo = updatedValue(state, _lineInfo, null);
      _lineInfoState = state;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: ${descriptor}");
    }
  }

  /**
   * Set the value of the data represented by the given descriptor to the given value.
   *
   * @param descriptor the descriptor representing the data whose value is to be set
   * @param value the new value of the data represented by the given descriptor
   */
  void setValue(DataDescriptor descriptor, Object value) {
    if (identical(descriptor, SourceEntry.CONTENT)) {
      _content = value as String;
      _contentState = CacheState.VALID;
    } else if (identical(descriptor, SourceEntry.LINE_INFO)) {
      _lineInfo = value as LineInfo;
      _lineInfoState = CacheState.VALID;
    } else {
      throw new IllegalArgumentException("Invalid descriptor: ${descriptor}");
    }
  }

  @override
  String toString() {
    JavaStringBuilder builder = new JavaStringBuilder();
    writeOn(builder);
    return builder.toString();
  }

  /**
   * Set the value of all of the flags with the given indexes to false.
   *
   * @param indexes the indexes of the flags whose value is to be set to false
   */
  void clearFlags(List<int> indexes) {
    for (int i = 0; i < indexes.length; i++) {
      _flags = BooleanArray.set(_flags, indexes[i], false);
    }
  }

  /**
   * Copy the information from the given cache entry.
   *
   * @param entry the cache entry from which information will be copied
   */
  void copyFrom(SourceEntryImpl entry) {
    _modificationTime = entry._modificationTime;
    _flags = entry._flags;
    exception = entry.exception;
    _contentState = entry._contentState;
    _content = entry._content;
    _lineInfoState = entry._lineInfoState;
    _lineInfo = entry._lineInfo;
  }

  /**
   * Return the value of the flag with the given index.
   *
   * @param index the index of the flag whose value is to be returned
   * @return the value of the flag with the given index
   */
  bool getFlag(int index) => BooleanArray.get(_flags, index);

  /**
   * Return `true` if the state of any data value is [CacheState#ERROR].
   *
   * @return `true` if the state of any data value is [CacheState#ERROR]
   */
  bool get hasErrorState => _contentState == CacheState.ERROR || _lineInfoState == CacheState.ERROR;

  /**
   * Set the value of the flag with the given index to the given value.
   *
   * @param index the index of the flag whose value is to be returned
   * @param value the value of the flag with the given index
   */
  void setFlag(int index, bool value) {
    _flags = BooleanArray.set(_flags, index, value);
  }

  /**
   * Given that some data is being transitioned to the given state, return the value that should be
   * kept in the cache.
   *
   * @param state the state to which the data is being transitioned
   * @param currentValue the value of the data before the transition
   * @param defaultValue the value to be used if the current value is to be removed from the cache
   * @return the value of the data that should be kept in the cache
   */
  Object updatedValue(CacheState state, Object currentValue, Object defaultValue) {
    if (state == CacheState.VALID) {
      throw new IllegalArgumentException("Use setValue() to set the state to VALID");
    } else if (state == CacheState.IN_PROCESS) {
      //
      // We can leave the current value in the cache for any 'get' methods to access.
      //
      return currentValue;
    }
    return defaultValue;
  }

  /**
   * Write a textual representation of this entry to the given builder. The result will only be used
   * for debugging purposes.
   *
   * @param builder the builder to which the text should be written
   */
  void writeOn(JavaStringBuilder builder) {
    builder.append("time = ");
    builder.append(_modificationTime);
    builder.append("; content = ");
    builder.append(_contentState);
    builder.append("; lineInfo = ");
    builder.append(_lineInfoState);
  }
}

/**
 * Implementation of the [AnalysisContentStatistics].
 */
class AnalysisContentStatisticsImpl implements AnalysisContentStatistics {
  Map<String, AnalysisContentStatistics_CacheRow> _dataMap = new Map<String, AnalysisContentStatistics_CacheRow>();

  List<Source> _sources = new List<Source>();

  Set<AnalysisException> _exceptions = new Set<AnalysisException>();

  void addSource(Source source) {
    _sources.add(source);
  }

  @override
  List<AnalysisContentStatistics_CacheRow> get cacheRows {
    Iterable<AnalysisContentStatistics_CacheRow> items = _dataMap.values;
    return new List.from(items);
  }

  @override
  List<AnalysisException> get exceptions => new List.from(_exceptions);

  @override
  List<Source> get sources => new List.from(_sources);

  void putCacheItem(SourceEntry dartEntry, DataDescriptor descriptor) {
    _internalPutCacheItem(dartEntry, descriptor, dartEntry.getState(descriptor));
  }

  void putCacheItemInLibrary(DartEntry dartEntry, Source librarySource, DataDescriptor descriptor) {
    _internalPutCacheItem(dartEntry, descriptor, dartEntry.getStateInLibrary(descriptor, librarySource));
  }

  void _internalPutCacheItem(SourceEntry dartEntry, DataDescriptor rowDesc, CacheState state) {
    String rowName = rowDesc.toString();
    AnalysisContentStatisticsImpl_CacheRowImpl row = _dataMap[rowName] as AnalysisContentStatisticsImpl_CacheRowImpl;
    if (row == null) {
      row = new AnalysisContentStatisticsImpl_CacheRowImpl(rowName);
      _dataMap[rowName] = row;
    }
    row._incState(state);
    if (state == CacheState.ERROR) {
      AnalysisException exception = dartEntry.exception;
      if (exception != null) {
        _exceptions.add(exception);
      }
    }
  }
}

class AnalysisContentStatisticsImpl_CacheRowImpl implements AnalysisContentStatistics_CacheRow {
  final String name;

  int _errorCount = 0;

  int _flushedCount = 0;

  int _inProcessCount = 0;

  int _invalidCount = 0;

  int _validCount = 0;

  AnalysisContentStatisticsImpl_CacheRowImpl(this.name);

  @override
  bool operator ==(Object obj) => obj is AnalysisContentStatisticsImpl_CacheRowImpl && obj.name == name;

  @override
  int get errorCount => _errorCount;

  @override
  int get flushedCount => _flushedCount;

  @override
  int get inProcessCount => _inProcessCount;

  @override
  int get invalidCount => _invalidCount;

  @override
  int get validCount => _validCount;

  @override
  int get hashCode => name.hashCode;

  void _incState(CacheState state) {
    if (state == CacheState.ERROR) {
      _errorCount++;
    }
    if (state == CacheState.FLUSHED) {
      _flushedCount++;
    }
    if (state == CacheState.IN_PROCESS) {
      _inProcessCount++;
    }
    if (state == CacheState.INVALID) {
      _invalidCount++;
    }
    if (state == CacheState.VALID) {
      _validCount++;
    }
  }
}

/**
 * Instances of the class `AnalysisContextImpl` implement an [AnalysisContext].
 */
class AnalysisContextImpl implements InternalAnalysisContext {
  /**
   * The difference between the maximum cache size and the maximum priority order size. The priority
   * list must be capped so that it is less than the cache size. Failure to do so can result in an
   * infinite loop in performAnalysisTask() because re-caching one AST structure can cause another
   * priority source's AST structure to be flushed.
   */
  static int _PRIORITY_ORDER_SIZE_DELTA = 4;

  /**
   * A flag indicating whether trace output should be produced as analysis tasks are performed. Used
   * for debugging.
   */
  static bool _TRACE_PERFORM_TASK = false;

  /**
   * The set of analysis options controlling the behavior of this context.
   */
  AnalysisOptionsImpl _options = new AnalysisOptionsImpl();

  /**
   * A flag indicating whether errors related to sources in the SDK should be generated and
   * reported.
   */
  bool _generateSdkErrors = true;

  /**
   * A flag indicating whether this context is disposed.
   */
  bool _disposed = false;

  /**
   * A cache of content used to override the default content of a source.
   */
  ContentCache _contentCache = new ContentCache();

  /**
   * The source factory used to create the sources that can be analyzed in this context.
   */
  SourceFactory _sourceFactory;

  /**
   * A source representing the core library.
   */
  Source _coreLibrarySource;

  /**
   * A table mapping the sources known to the context to the information known about the source.
   */
  AnalysisCache _cache;

  /**
   * An array containing sources for which data should not be flushed.
   */
  List<Source> _priorityOrder = Source.EMPTY_ARRAY;

  /**
   * An array containing sources whose AST structure is needed in order to resolve the next library
   * to be resolved.
   */
  Set<Source> _neededForResolution = null;

  /**
   * A table mapping sources to the change notices that are waiting to be returned related to that
   * source.
   */
  Map<Source, ChangeNoticeImpl> _pendingNotices = new Map<Source, ChangeNoticeImpl>();

  /**
   * A set containing information about the tasks that have been performed since the last change
   * notification. Used to detect infinite loops in [performAnalysisTask].
   */
  Set<String> _recentTasks = new Set<String>();

  /**
   * The object used to synchronize access to all of the caches. The rules related to the use of
   * this lock object are
   * * no analysis work is done while holding the lock, and
   * * no analysis results can be recorded unless we have obtained the lock and validated that the
   * results are for the same version (modification time) of the source as our current cache
   * content.
   */
  Object _cacheLock = new Object();

  /**
   * The object used to record the results of performing an analysis task.
   */
  AnalysisContextImpl_AnalysisTaskResultRecorder _resultRecorder;

  /**
   * Cached information used in incremental analysis or `null` if none. Synchronize against
   * [cacheLock] before accessing this field.
   */
  IncrementalAnalysisCache _incrementalAnalysisCache;

  /**
   * The object used to manage the list of sources that need to be analyzed.
   */
  WorkManager _workManager = new WorkManager();

  /**
   * The set of [AngularApplication] in this context.
   */
  Set<AngularApplication> _angularApplications = new Set();

  /**
   * Initialize a newly created analysis context.
   */
  AnalysisContextImpl() : super() {
    _resultRecorder = new AnalysisContextImpl_AnalysisTaskResultRecorder(this);
    _cache = new AnalysisCache(AnalysisOptionsImpl.DEFAULT_CACHE_SIZE, new AnalysisContextImpl_ContextRetentionPolicy(this));
  }

  @override
  void addSourceInfo(Source source, SourceEntry info) {
    // This implementation assumes that the access to the cache does not need to be synchronized
    // because no other object can have access to this context while this method is being invoked.
    _cache.put(source, info);
  }

  @override
  void applyChanges(ChangeSet changeSet) {
    if (changeSet.isEmpty) {
      return;
    }
    _recentTasks.clear();
    //
    // First, compute the list of sources that have been removed.
    //
    List<Source> removedSources = new List<Source>.from(changeSet.removedSources);
    for (SourceContainer container in changeSet.removedContainers) {
      _addSourcesInContainer(removedSources, container);
    }
    //
    // Then determine which cached results are no longer valid.
    //
    bool addedDartSource = false;
    for (Source source in changeSet.addedSources) {
      if (_sourceAvailable(source)) {
        addedDartSource = true;
      }
    }
    for (Source source in changeSet.changedSources) {
      _sourceChanged(source);
    }
    for (Source source in removedSources) {
      _sourceRemoved(source);
    }
    if (addedDartSource) {
      // TODO(brianwilkerson) This is hugely inefficient, but we need to re-analyze any libraries
      // that might have been referencing the not-yet-existing source that was just added. Longer
      // term we need to keep track of which libraries are referencing non-existing sources and
      // only re-analyze those libraries.
      //        logInformation("Added Dart sources, invalidating all resolution information");
      List<Source> sourcesToInvalidate = new List<Source>();
      for (MapEntry<Source, SourceEntry> mapEntry in _cache.entrySet()) {
        Source source = mapEntry.getKey();
        SourceEntry sourceEntry = mapEntry.getValue();
        if (!source.isInSystemLibrary && sourceEntry is DartEntry) {
          sourcesToInvalidate.add(source);
        }
      }
      int count = sourcesToInvalidate.length;
      for (int i = 0; i < count; i++) {
        Source source = sourcesToInvalidate[i];
        DartEntry dartEntry = _getReadableDartEntry(source);
        _removeFromParts(source, dartEntry);
        DartEntryImpl dartCopy = dartEntry.writableCopy;
        dartCopy.invalidateAllResolutionInformation();
        _cache.put(source, dartCopy);
        SourcePriority priority = SourcePriority.UNKNOWN;
        SourceKind kind = dartCopy.kind;
        if (kind == SourceKind.LIBRARY) {
          priority = SourcePriority.LIBRARY;
        } else if (kind == SourceKind.PART) {
          priority = SourcePriority.NORMAL_PART;
        }
        _workManager.add(source, priority);
      }
    }
  }

  @override
  String computeDocumentationComment(Element element) {
    if (element == null) {
      return null;
    }
    Source source = element.source;
    if (source == null) {
      return null;
    }
    CompilationUnit unit = parseCompilationUnit(source);
    if (unit == null) {
      return null;
    }
    NodeLocator locator = new NodeLocator.con1(element.nameOffset);
    AstNode nameNode = locator.searchWithin(unit);
    while (nameNode != null) {
      if (nameNode is AnnotatedNode) {
        Comment comment = (nameNode as AnnotatedNode).documentationComment;
        if (comment == null) {
          return null;
        }
        JavaStringBuilder builder = new JavaStringBuilder();
        List<Token> tokens = comment.tokens;
        for (int i = 0; i < tokens.length; i++) {
          if (i > 0) {
            builder.append('\n');
          }
          builder.append(tokens[i].lexeme);
        }
        return builder.toString();
      }
      nameNode = nameNode.parent;
    }
    return null;
  }

  @override
  List<AnalysisError> computeErrors(Source source) {
    bool enableHints = _options.hint;
    SourceEntry sourceEntry = _getReadableSourceEntry(source);
    if (sourceEntry is DartEntry) {
      List<AnalysisError> errors = new List<AnalysisError>();
      try {
        DartEntry dartEntry = sourceEntry;
        ListUtilities.addAll(errors, _getDartScanData(source, dartEntry, DartEntry.SCAN_ERRORS));
        dartEntry = _getReadableDartEntry(source);
        ListUtilities.addAll(errors, _getDartParseData(source, dartEntry, DartEntry.PARSE_ERRORS));
        dartEntry = _getReadableDartEntry(source);
        if (dartEntry.getValue(DartEntry.SOURCE_KIND) == SourceKind.LIBRARY) {
          ListUtilities.addAll(errors, _getDartResolutionData(source, source, dartEntry, DartEntry.RESOLUTION_ERRORS));
          dartEntry = _getReadableDartEntry(source);
          ListUtilities.addAll(errors, _getDartVerificationData(source, source, dartEntry, DartEntry.VERIFICATION_ERRORS));
          if (enableHints) {
            dartEntry = _getReadableDartEntry(source);
            ListUtilities.addAll(errors, _getDartHintData(source, source, dartEntry, DartEntry.HINTS));
          }
        } else {
          List<Source> libraries = getLibrariesContaining(source);
          for (Source librarySource in libraries) {
            ListUtilities.addAll(errors, _getDartResolutionData(source, librarySource, dartEntry, DartEntry.RESOLUTION_ERRORS));
            dartEntry = _getReadableDartEntry(source);
            ListUtilities.addAll(errors, _getDartVerificationData(source, librarySource, dartEntry, DartEntry.VERIFICATION_ERRORS));
            if (enableHints) {
              dartEntry = _getReadableDartEntry(source);
              ListUtilities.addAll(errors, _getDartHintData(source, librarySource, dartEntry, DartEntry.HINTS));
            }
          }
        }
      } on ObsoleteSourceAnalysisException catch (exception) {
        AnalysisEngine.instance.logger.logInformation2("Could not compute errors", exception);
      }
      if (errors.isEmpty) {
        return AnalysisError.NO_ERRORS;
      }
      return new List.from(errors);
    } else if (sourceEntry is HtmlEntry) {
      HtmlEntry htmlEntry = sourceEntry;
      try {
        return _getHtmlResolutionData2(source, htmlEntry, HtmlEntry.RESOLUTION_ERRORS);
      } on ObsoleteSourceAnalysisException catch (exception) {
        AnalysisEngine.instance.logger.logInformation2("Could not compute errors", exception);
      }
    }
    return AnalysisError.NO_ERRORS;
  }

  @override
  List<Source> computeExportedLibraries(Source source) => _getDartParseData2(source, DartEntry.EXPORTED_LIBRARIES, Source.EMPTY_ARRAY);

  @override
  HtmlElement computeHtmlElement(Source source) => _getHtmlResolutionData(source, HtmlEntry.ELEMENT, null);

  @override
  List<Source> computeImportedLibraries(Source source) => _getDartParseData2(source, DartEntry.IMPORTED_LIBRARIES, Source.EMPTY_ARRAY);

  @override
  SourceKind computeKindOf(Source source) {
    SourceEntry sourceEntry = _getReadableSourceEntry(source);
    if (sourceEntry == null) {
      return SourceKind.UNKNOWN;
    } else if (sourceEntry is DartEntry) {
      try {
        return _getDartParseData(source, sourceEntry, DartEntry.SOURCE_KIND);
      } on AnalysisException catch (exception) {
        return SourceKind.UNKNOWN;
      }
    }
    return sourceEntry.kind;
  }

  @override
  LibraryElement computeLibraryElement(Source source) => _getDartResolutionData2(source, source, DartEntry.ELEMENT, null);

  @override
  LineInfo computeLineInfo(Source source) {
    SourceEntry sourceEntry = _getReadableSourceEntry(source);
    try {
      if (sourceEntry is HtmlEntry) {
        return _getHtmlParseData(source, SourceEntry.LINE_INFO, null);
      } else if (sourceEntry is DartEntry) {
        return _getDartScanData2(source, SourceEntry.LINE_INFO, null);
      }
    } on ObsoleteSourceAnalysisException catch (exception) {
      AnalysisEngine.instance.logger.logInformation2("Could not compute ${SourceEntry.LINE_INFO.toString()}", exception);
    }
    return null;
  }

  @override
  ResolvableCompilationUnit computeResolvableCompilationUnit(Source source) {
    DartEntry dartEntry = _getReadableDartEntry(source);
    if (dartEntry == null) {
      throw new AnalysisException.con1("computeResolvableCompilationUnit for non-Dart: ${source.fullName}");
    }
    dartEntry = _cacheDartParseData(source, dartEntry, DartEntry.PARSED_UNIT);
    DartEntryImpl dartCopy = dartEntry.writableCopy;
    CompilationUnit unit = dartCopy.resolvableCompilationUnit;
    if (unit == null) {
      throw new AnalysisException.con2("Internal error: computeResolvableCompilationUnit could not parse ${source.fullName}", dartEntry.exception);
    }
    _cache.put(source, dartCopy);
    return new ResolvableCompilationUnit.con1(dartCopy.modificationTime, unit);
  }

  @override
  void dispose() {
    _disposed = true;
  }

  @override
  bool exists(Source source) {
    if (source == null) {
      return false;
    }
    if (_contentCache.getContents(source) != null) {
      return true;
    }
    return source.exists();
  }

  @override
  AnalysisContext extractContext(SourceContainer container) => extractContextInto(container, AnalysisEngine.instance.createAnalysisContext() as InternalAnalysisContext);

  @override
  InternalAnalysisContext extractContextInto(SourceContainer container, InternalAnalysisContext newContext) {
    List<Source> sourcesToRemove = new List<Source>();
    // Move sources in the specified directory to the new context
    for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
      Source source = entry.getKey();
      if (container.contains(source)) {
        sourcesToRemove.add(source);
        newContext.addSourceInfo(source, entry.getValue().writableCopy);
      }
    }
    return newContext;
  }

  @override
  AnalysisOptions get analysisOptions => _options;

  @override
  AngularApplication getAngularApplicationWithHtml(Source htmlSource) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(htmlSource);
    if (sourceEntry is HtmlEntry) {
      HtmlEntry htmlEntry = sourceEntry;
      AngularApplication application = htmlEntry.getValue(HtmlEntry.ANGULAR_APPLICATION);
      if (application != null) {
        return application;
      }
      return htmlEntry.getValue(HtmlEntry.ANGULAR_ENTRY);
    }
    return null;
  }

  @override
  CompilationUnitElement getCompilationUnitElement(Source unitSource, Source librarySource) {
    LibraryElement libraryElement = getLibraryElement(librarySource);
    if (libraryElement != null) {
      // try defining unit
      CompilationUnitElement definingUnit = libraryElement.definingCompilationUnit;
      if (definingUnit.source == unitSource) {
        return definingUnit;
      }
      // try parts
      for (CompilationUnitElement partUnit in libraryElement.parts) {
        if (partUnit.source == unitSource) {
          return partUnit;
        }
      }
    }
    return null;
  }

  @override
  TimestampedData<String> getContents(Source source) {
    String contents = _contentCache.getContents(source);
    if (contents != null) {
      return new TimestampedData<String>(_contentCache.getModificationStamp(source), contents);
    }
    return source.contents;
  }

  @override
  Element getElement(ElementLocation location) {
    // TODO(brianwilkerson) This should not be a "get" method.
    try {
      List<String> components = (location as ElementLocationImpl).components;
      Source librarySource = _computeSourceFromEncoding(components[0]);
      ElementImpl element = computeLibraryElement(librarySource) as ElementImpl;
      for (int i = 1; i < components.length; i++) {
        if (element == null) {
          return null;
        }
        element = element.getChild(components[i]);
      }
      return element;
    } on AnalysisException catch (exception) {
      return null;
    }
  }

  @override
  AnalysisErrorInfo getErrors(Source source) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(source);
    if (sourceEntry is DartEntry) {
      DartEntry dartEntry = sourceEntry;
      return new AnalysisErrorInfoImpl(dartEntry.allErrors, dartEntry.getValue(SourceEntry.LINE_INFO));
    } else if (sourceEntry is HtmlEntry) {
      HtmlEntry htmlEntry = sourceEntry;
      return new AnalysisErrorInfoImpl(htmlEntry.allErrors, htmlEntry.getValue(SourceEntry.LINE_INFO));
    }
    return new AnalysisErrorInfoImpl(AnalysisError.NO_ERRORS, null);
  }

  @override
  HtmlElement getHtmlElement(Source source) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(source);
    if (sourceEntry is HtmlEntry) {
      return sourceEntry.getValue(HtmlEntry.ELEMENT);
    }
    return null;
  }

  @override
  List<Source> getHtmlFilesReferencing(Source source) {
    SourceKind sourceKind = getKindOf(source);
    if (sourceKind == null) {
      return Source.EMPTY_ARRAY;
    }
    List<Source> htmlSources = new List<Source>();
    while (true) {
      if (sourceKind == SourceKind.LIBRARY) {
      } else if (sourceKind == SourceKind.PART) {
        List<Source> librarySources = getLibrariesContaining(source);
        for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
          SourceEntry sourceEntry = entry.getValue();
          if (sourceEntry.kind == SourceKind.HTML) {
            List<Source> referencedLibraries = (sourceEntry as HtmlEntry).getValue(HtmlEntry.REFERENCED_LIBRARIES);
            if (_containsAny(referencedLibraries, librarySources)) {
              htmlSources.add(entry.getKey());
            }
          }
        }
      }
      break;
    }
    if (htmlSources.isEmpty) {
      return Source.EMPTY_ARRAY;
    }
    return new List.from(htmlSources);
  }

  @override
  List<Source> get htmlSources => _getSources(SourceKind.HTML);

  @override
  SourceKind getKindOf(Source source) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(source);
    if (sourceEntry == null) {
      return SourceKind.UNKNOWN;
    }
    return sourceEntry.kind;
  }

  @override
  List<Source> get launchableClientLibrarySources {
    // TODO(brianwilkerson) This needs to filter out libraries that do not reference dart:html,
    // either directly or indirectly.
    List<Source> sources = new List<Source>();
    for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
      Source source = entry.getKey();
      SourceEntry sourceEntry = entry.getValue();
      if (sourceEntry.kind == SourceKind.LIBRARY && !source.isInSystemLibrary) {
        //          DartEntry dartEntry = (DartEntry) sourceEntry;
        //          if (dartEntry.getValue(DartEntry.IS_LAUNCHABLE) && dartEntry.getValue(DartEntry.IS_CLIENT)) {
        sources.add(source);
      }
    }
    return new List.from(sources);
  }

  @override
  List<Source> get launchableServerLibrarySources {
    // TODO(brianwilkerson) This needs to filter out libraries that reference dart:html, either
    // directly or indirectly.
    List<Source> sources = new List<Source>();
    for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
      Source source = entry.getKey();
      SourceEntry sourceEntry = entry.getValue();
      if (sourceEntry.kind == SourceKind.LIBRARY && !source.isInSystemLibrary) {
        //          DartEntry dartEntry = (DartEntry) sourceEntry;
        //          if (dartEntry.getValue(DartEntry.IS_LAUNCHABLE) && !dartEntry.getValue(DartEntry.IS_CLIENT)) {
        sources.add(source);
      }
    }
    return new List.from(sources);
  }

  @override
  List<Source> getLibrariesContaining(Source source) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(source);
    if (sourceEntry is DartEntry) {
      return sourceEntry.getValue(DartEntry.CONTAINING_LIBRARIES);
    }
    return Source.EMPTY_ARRAY;
  }

  @override
  List<Source> getLibrariesDependingOn(Source librarySource) {
    List<Source> dependentLibraries = new List<Source>();
    for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
      SourceEntry sourceEntry = entry.getValue();
      if (sourceEntry.kind == SourceKind.LIBRARY) {
        if (_contains((sourceEntry as DartEntry).getValue(DartEntry.EXPORTED_LIBRARIES), librarySource)) {
          dependentLibraries.add(entry.getKey());
        }
        if (_contains((sourceEntry as DartEntry).getValue(DartEntry.IMPORTED_LIBRARIES), librarySource)) {
          dependentLibraries.add(entry.getKey());
        }
      }
    }
    if (dependentLibraries.isEmpty) {
      return Source.EMPTY_ARRAY;
    }
    return new List.from(dependentLibraries);
  }

  @override
  List<Source> getLibrariesReferencedFromHtml(Source htmlSource) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(htmlSource);
    if (sourceEntry is HtmlEntry) {
      HtmlEntry htmlEntry = sourceEntry;
      return htmlEntry.getValue(HtmlEntry.REFERENCED_LIBRARIES);
    }
    return Source.EMPTY_ARRAY;
  }

  @override
  LibraryElement getLibraryElement(Source source) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(source);
    if (sourceEntry is DartEntry) {
      return sourceEntry.getValue(DartEntry.ELEMENT);
    }
    return null;
  }

  @override
  List<Source> get librarySources => _getSources(SourceKind.LIBRARY);

  @override
  LineInfo getLineInfo(Source source) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(source);
    if (sourceEntry != null) {
      return sourceEntry.getValue(SourceEntry.LINE_INFO);
    }
    return null;
  }

  @override
  int getModificationStamp(Source source) {
    int stamp = _contentCache.getModificationStamp(source);
    if (stamp != null) {
      return stamp;
    }
    return source.modificationStamp;
  }

  @override
  Namespace getPublicNamespace(LibraryElement library) {
    // TODO(brianwilkerson) Rename this to not start with 'get'. Note that this is not part of the
    // API of the interface.
    Source source = library.definingCompilationUnit.source;
    DartEntry dartEntry = _getReadableDartEntry(source);
    if (dartEntry == null) {
      return null;
    }
    Namespace namespace = null;
    if (identical(dartEntry.getValue(DartEntry.ELEMENT), library)) {
      namespace = dartEntry.getValue(DartEntry.PUBLIC_NAMESPACE);
    }
    if (namespace == null) {
      NamespaceBuilder builder = new NamespaceBuilder();
      namespace = builder.createPublicNamespaceForLibrary(library);
      dartEntry = _getReadableDartEntry(source);
      if (dartEntry == null) {
        AnalysisEngine.instance.logger.logError2("Could not compute the public namespace for ${library.source.fullName}", new AnalysisException.con1("A Dart file became a non-Dart file: ${source.fullName}"));
        return null;
      }
      if (identical(dartEntry.getValue(DartEntry.ELEMENT), library)) {
        DartEntryImpl dartCopy = _getReadableDartEntry(source).writableCopy;
        dartCopy.setValue(DartEntry.PUBLIC_NAMESPACE, namespace);
        _cache.put(source, dartCopy);
      }
    }
    return namespace;
  }

  @override
  List<Source> get refactoringUnsafeSources {
    List<Source> sources = new List<Source>();
    for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
      SourceEntry sourceEntry = entry.getValue();
      if (sourceEntry is DartEntry) {
        if (!sourceEntry.isRefactoringSafe) {
          sources.add(entry.getKey());
        }
      }
    }
    return new List.from(sources);
  }

  @override
  CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    if (library == null) {
      return null;
    }
    return getResolvedCompilationUnit2(unitSource, library.source);
  }

  @override
  CompilationUnit getResolvedCompilationUnit2(Source unitSource, Source librarySource) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(unitSource);
    if (sourceEntry is DartEntry) {
      return sourceEntry.getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
    }
    return null;
  }

  @override
  ht.HtmlUnit getResolvedHtmlUnit(Source htmlSource) {
    SourceEntry sourceEntry = _getReadableSourceEntryOrNull(htmlSource);
    if (sourceEntry is HtmlEntry) {
      HtmlEntry htmlEntry = sourceEntry;
      return htmlEntry.getValue(HtmlEntry.RESOLVED_UNIT);
    }
    return null;
  }

  @override
  SourceFactory get sourceFactory => _sourceFactory;

  /**
   * Return a list of the sources that would be processed by [performAnalysisTask]. This
   * method duplicates, and must therefore be kept in sync with, [getNextAnalysisTask].
   * This method is intended to be used for testing purposes only.
   *
   * @return a list of the sources that would be processed by [performAnalysisTask]
   */
  List<Source> get sourcesNeedingProcessing {
    Set<Source> sources = new Set<Source>();
    bool hintsEnabled = _options.hint;
    //
    // Look for priority sources that need to be analyzed.
    //
    for (Source source in _priorityOrder) {
      _getSourcesNeedingProcessing(source, _cache.get(source), true, hintsEnabled, sources);
    }
    //
    // Look for non-priority sources that need to be analyzed.
    //
    for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
      _getSourcesNeedingProcessing(entry.getKey(), entry.getValue(), false, hintsEnabled, sources);
    }
    return new List<Source>.from(sources);
  }

  @override
  AnalysisContentStatistics get statistics {
    bool hintsEnabled = _options.hint;
    AnalysisContentStatisticsImpl statistics = new AnalysisContentStatisticsImpl();
    for (MapEntry<Source, SourceEntry> mapEntry in _cache.entrySet()) {
      statistics.addSource(mapEntry.getKey());
      SourceEntry entry = mapEntry.getValue();
      if (entry is DartEntry) {
        Source source = mapEntry.getKey();
        DartEntry dartEntry = entry;
        SourceKind kind = dartEntry.getValue(DartEntry.SOURCE_KIND);
        // get library independent values
        statistics.putCacheItem(dartEntry, SourceEntry.LINE_INFO);
        statistics.putCacheItem(dartEntry, DartEntry.PARSE_ERRORS);
        statistics.putCacheItem(dartEntry, DartEntry.PARSED_UNIT);
        statistics.putCacheItem(dartEntry, DartEntry.SOURCE_KIND);
        if (kind == SourceKind.LIBRARY) {
          statistics.putCacheItem(dartEntry, DartEntry.ELEMENT);
          statistics.putCacheItem(dartEntry, DartEntry.EXPORTED_LIBRARIES);
          statistics.putCacheItem(dartEntry, DartEntry.IMPORTED_LIBRARIES);
          statistics.putCacheItem(dartEntry, DartEntry.INCLUDED_PARTS);
          statistics.putCacheItem(dartEntry, DartEntry.IS_CLIENT);
          statistics.putCacheItem(dartEntry, DartEntry.IS_LAUNCHABLE);
        }
        // get library-specific values
        List<Source> librarySources = getLibrariesContaining(source);
        for (Source librarySource in librarySources) {
          statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.RESOLUTION_ERRORS);
          statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.RESOLVED_UNIT);
          if (_generateSdkErrors || !source.isInSystemLibrary) {
            statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.VERIFICATION_ERRORS);
            if (hintsEnabled) {
              statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.HINTS);
            }
          }
        }
      } else if (entry is HtmlEntry) {
        HtmlEntry htmlEntry = entry;
        statistics.putCacheItem(htmlEntry, SourceEntry.LINE_INFO);
        statistics.putCacheItem(htmlEntry, HtmlEntry.PARSE_ERRORS);
        statistics.putCacheItem(htmlEntry, HtmlEntry.PARSED_UNIT);
        statistics.putCacheItem(htmlEntry, HtmlEntry.RESOLUTION_ERRORS);
        statistics.putCacheItem(htmlEntry, HtmlEntry.RESOLVED_UNIT);
      }
    }
    return statistics;
  }

  @override
  TypeProvider get typeProvider {
    Source coreSource = sourceFactory.forUri(DartSdk.DART_CORE);
    return new TypeProviderImpl(computeLibraryElement(coreSource));
  }

  @override
  bool isClientLibrary(Source librarySource) {
    SourceEntry sourceEntry = _getReadableSourceEntry(librarySource);
    if (sourceEntry is DartEntry) {
      DartEntry dartEntry = sourceEntry;
      return dartEntry.getValue(DartEntry.IS_CLIENT) && dartEntry.getValue(DartEntry.IS_LAUNCHABLE);
    }
    return false;
  }

  @override
  bool get isDisposed => _disposed;

  @override
  bool isServerLibrary(Source librarySource) {
    SourceEntry sourceEntry = _getReadableSourceEntry(librarySource);
    if (sourceEntry is DartEntry) {
      DartEntry dartEntry = sourceEntry;
      return !dartEntry.getValue(DartEntry.IS_CLIENT) && dartEntry.getValue(DartEntry.IS_LAUNCHABLE);
    }
    return false;
  }

  @override
  void mergeContext(AnalysisContext context) {
    if (context is InstrumentedAnalysisContextImpl) {
      context = (context as InstrumentedAnalysisContextImpl).basis;
    }
    if (context is! AnalysisContextImpl) {
      return;
    }
    // TODO(brianwilkerson) This does not lock against the other context's cacheLock.
    for (MapEntry<Source, SourceEntry> entry in (context as AnalysisContextImpl)._cache.entrySet()) {
      Source newSource = entry.getKey();
      SourceEntry existingEntry = _getReadableSourceEntry(newSource);
      if (existingEntry == null) {
        // TODO(brianwilkerson) Decide whether we really need to copy the info.
        _cache.put(newSource, entry.getValue().writableCopy);
      } else {
      }
    }
  }

  @override
  CompilationUnit parseCompilationUnit(Source source) => _getDartParseData2(source, DartEntry.PARSED_UNIT, null);

  @override
  ht.HtmlUnit parseHtmlUnit(Source source) => _getHtmlParseData(source, HtmlEntry.PARSED_UNIT, null);

  @override
  AnalysisResult performAnalysisTask() {
    if (_TRACE_PERFORM_TASK) {
      print("----------------------------------------");
    }
    int getStart = JavaSystem.currentTimeMillis();
    AnalysisTask task = nextAnalysisTask;
    int getEnd = JavaSystem.currentTimeMillis();
    if (task == null && _validateCacheConsistency()) {
      task = nextAnalysisTask;
    }
    if (task == null) {
      return new AnalysisResult(_getChangeNotices(true), getEnd - getStart, null, -1);
    }
    String taskDescriptor = task.toString();
    //    if (recentTasks.add(taskDescriptor)) {
    //      logInformation("Performing task: " + taskDescriptor);
    //    } else {
    //      if (TRACE_PERFORM_TASK) {
    //        System.out.print("* ");
    //      }
    //      logInformation("*** Performing repeated task: " + taskDescriptor);
    //    }
    if (_TRACE_PERFORM_TASK) {
      print(taskDescriptor);
    }
    int performStart = JavaSystem.currentTimeMillis();
    try {
      task.perform(_resultRecorder);
    } on ObsoleteSourceAnalysisException catch (exception) {
      AnalysisEngine.instance.logger.logInformation2("Could not perform analysis task: ${taskDescriptor}", exception);
    } on AnalysisException catch (exception) {
      if (exception.cause is! JavaIOException) {
        AnalysisEngine.instance.logger.logError2("Internal error while performing the task: ${task}", exception);
      }
    }
    int performEnd = JavaSystem.currentTimeMillis();
    return new AnalysisResult(_getChangeNotices(false), getEnd - getStart, task.runtimeType.toString(), performEnd - performStart);
  }

  @override
  void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    Source htmlSource = _sourceFactory.forUri(DartSdk.DART_HTML);
    for (MapEntry<Source, LibraryElement> entry in getMapEntrySet(elementMap)) {
      Source librarySource = entry.getKey();
      LibraryElement library = entry.getValue();
      //
      // Cache the element in the library's info.
      //
      DartEntry dartEntry = _getReadableDartEntry(librarySource);
      if (dartEntry != null) {
        DartEntryImpl dartCopy = dartEntry.writableCopy;
        _recordElementData(dartEntry, dartCopy, library, library.source, htmlSource);
        _cache.put(librarySource, dartCopy);
      }
    }
  }

  @override
  CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library) {
    if (library == null) {
      return null;
    }
    return resolveCompilationUnit2(unitSource, library.source);
  }

  @override
  CompilationUnit resolveCompilationUnit2(Source unitSource, Source librarySource) => _getDartResolutionData2(unitSource, librarySource, DartEntry.RESOLVED_UNIT, null);

  @override
  ht.HtmlUnit resolveHtmlUnit(Source htmlSource) {
    computeHtmlElement(htmlSource);
    return parseHtmlUnit(htmlSource);
  }

  @override
  void set analysisOptions(AnalysisOptions options) {
    bool needsRecompute = this._options.analyzeFunctionBodies != options.analyzeFunctionBodies || this._options.generateSdkErrors != options.generateSdkErrors || this._options.dart2jsHint != options.dart2jsHint || (this._options.hint && !options.hint) || this._options.preserveComments != options.preserveComments;
    int cacheSize = options.cacheSize;
    if (this._options.cacheSize != cacheSize) {
      this._options.cacheSize = cacheSize;
      _cache.maxCacheSize = cacheSize;
      //
      // Cap the size of the priority list to being less than the cache size. Failure to do so can
      // result in an infinite loop in performAnalysisTask() because re-caching one AST structure
      // can cause another priority source's AST structure to be flushed.
      //
      int maxPriorityOrderSize = cacheSize - _PRIORITY_ORDER_SIZE_DELTA;
      if (_priorityOrder.length > maxPriorityOrderSize) {
        List<Source> newPriorityOrder = new List<Source>(maxPriorityOrderSize);
        JavaSystem.arraycopy(_priorityOrder, 0, newPriorityOrder, 0, maxPriorityOrderSize);
        _priorityOrder = newPriorityOrder;
      }
    }
    this._options.analyzeFunctionBodies = options.analyzeFunctionBodies;
    this._options.generateSdkErrors = options.generateSdkErrors;
    this._options.dart2jsHint = options.dart2jsHint;
    this._options.hint = options.hint;
    this._options.incremental = options.incremental;
    this._options.preserveComments = options.preserveComments;
    _generateSdkErrors = options.generateSdkErrors;
    if (needsRecompute) {
      _invalidateAllResolutionInformation();
    }
  }

  @override
  void set analysisPriorityOrder(List<Source> sources) {
    if (sources == null || sources.isEmpty) {
      _priorityOrder = Source.EMPTY_ARRAY;
    } else {
      while (sources.remove(null)) {
      }
      if (sources.isEmpty) {
        _priorityOrder = Source.EMPTY_ARRAY;
      }
      //
      // Cap the size of the priority list to being less than the cache size. Failure to do so can
      // result in an infinite loop in performAnalysisTask() because re-caching one AST structure
      // can cause another priority source's AST structure to be flushed.
      //
      int count = Math.min(sources.length, _options.cacheSize - _PRIORITY_ORDER_SIZE_DELTA);
      _priorityOrder = new List<Source>(count);
      for (int i = 0; i < count; i++) {
        _priorityOrder[i] = sources[i];
      }
    }
  }

  @override
  void setChangedContents(Source source, String contents, int offset, int oldLength, int newLength) {
    _recentTasks.clear();
    String originalContents = _contentCache.setContents(source, contents);
    if (contents != null) {
      if (contents != originalContents) {
        if (_options.incremental) {
          _incrementalAnalysisCache = IncrementalAnalysisCache.update(_incrementalAnalysisCache, source, originalContents, contents, offset, oldLength, newLength, _getReadableSourceEntry(source));
        }
        _sourceChanged(source);
        SourceEntry sourceEntry = _cache.get(source);
        if (sourceEntry != null) {
          SourceEntryImpl sourceCopy = sourceEntry.writableCopy;
          sourceCopy.modificationTime = _contentCache.getModificationStamp(source);
          sourceCopy.setValue(SourceEntry.CONTENT, contents);
          _cache.put(source, sourceCopy);
        }
      }
    } else if (originalContents != null) {
      _incrementalAnalysisCache = IncrementalAnalysisCache.clear(_incrementalAnalysisCache, source);
      _sourceChanged(source);
    }
  }

  @override
  void setContents(Source source, String contents) {
    _recentTasks.clear();
    String originalContents = _contentCache.setContents(source, contents);
    if (contents != null) {
      if (contents != originalContents) {
        _incrementalAnalysisCache = IncrementalAnalysisCache.clear(_incrementalAnalysisCache, source);
        _sourceChanged(source);
        SourceEntry sourceEntry = _cache.get(source);
        if (sourceEntry != null) {
          SourceEntryImpl sourceCopy = sourceEntry.writableCopy;
          sourceCopy.modificationTime = _contentCache.getModificationStamp(source);
          sourceCopy.setValue(SourceEntry.CONTENT, contents);
          _cache.put(source, sourceCopy);
        }
      }
    } else if (originalContents != null) {
      _incrementalAnalysisCache = IncrementalAnalysisCache.clear(_incrementalAnalysisCache, source);
      _sourceChanged(source);
    }
  }

  @override
  void set sourceFactory(SourceFactory factory) {
    if (identical(_sourceFactory, factory)) {
      return;
    } else if (factory.context != null) {
      throw new IllegalStateException("Source factories cannot be shared between contexts");
    }
    if (_sourceFactory != null) {
      _sourceFactory.context = null;
    }
    factory.context = this;
    _sourceFactory = factory;
    _coreLibrarySource = _sourceFactory.forUri(DartSdk.DART_CORE);
    _invalidateAllResolutionInformation();
  }

  /**
   * Record the results produced by performing a [ResolveDartLibraryCycleTask]. If the results
   * were computed from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  DartEntry recordResolveDartLibraryCycleTaskResults(ResolveDartLibraryCycleTask task) {
    LibraryResolver2 resolver = task.libraryResolver;
    AnalysisException thrownException = task.exception;
    DartEntry unitEntry = null;
    Source unitSource = task.unitSource;
    if (resolver != null) {
      //
      // The resolver should only be null if an exception was thrown before (or while) it was
      // being created.
      //
      List<ResolvableLibrary> resolvedLibraries = resolver.resolvedLibraries;
      if (resolvedLibraries == null) {
        //
        // The resolved libraries should only be null if an exception was thrown during resolution.
        //
        unitEntry = _getReadableDartEntry(unitSource);
        if (unitEntry == null) {
          throw new AnalysisException.con1("A Dart file became a non-Dart file: ${unitSource.fullName}");
        }
        DartEntryImpl dartCopy = unitEntry.writableCopy;
        dartCopy.recordResolutionError();
        dartCopy.exception = thrownException;
        _cache.put(unitSource, dartCopy);
        _cache.remove(unitSource);
        if (thrownException != null) {
          throw thrownException;
        }
        return dartCopy;
      }
      if (_allModificationTimesMatch(resolvedLibraries)) {
        Source htmlSource = sourceFactory.forUri(DartSdk.DART_HTML);
        RecordingErrorListener errorListener = resolver.errorListener;
        for (ResolvableLibrary library in resolvedLibraries) {
          Source librarySource = library.librarySource;
          for (Source source in library.compilationUnitSources) {
            CompilationUnit unit = library.getAST(source);
            List<AnalysisError> errors = errorListener.getErrorsForSource(source);
            LineInfo lineInfo = getLineInfo(source);
            DartEntry dartEntry = _cache.get(source) as DartEntry;
            int sourceTime = getModificationStamp(source);
            if (dartEntry.modificationTime != sourceTime) {
              // The source has changed without the context being notified. Simulate notification.
              _sourceChanged(source);
              dartEntry = _getReadableDartEntry(source);
              if (dartEntry == null) {
                throw new AnalysisException.con1("A Dart file became a non-Dart file: ${source.fullName}");
              }
            }
            DartEntryImpl dartCopy = dartEntry.writableCopy;
            if (thrownException == null) {
              dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
              dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.FLUSHED);
              dartCopy.setValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource, unit);
              dartCopy.setValueInLibrary(DartEntry.RESOLUTION_ERRORS, librarySource, errors);
              if (source == librarySource) {
                _recordElementData(dartEntry, dartCopy, library.libraryElement, librarySource, htmlSource);
              }
              _cache.storedAst(source);
            } else {
              dartCopy.recordResolutionError();
              _cache.remove(source);
            }
            dartCopy.exception = thrownException;
            _cache.put(source, dartCopy);
            if (source != librarySource) {
              _workManager.add(source, SourcePriority.PRIORITY_PART);
            }
            if (source == unitSource) {
              unitEntry = dartCopy;
            }
            ChangeNoticeImpl notice = _getNotice(source);
            notice.compilationUnit = unit;
            notice.setErrors(dartCopy.allErrors, lineInfo);
          }
        }
      } else {
        PrintStringWriter writer = new PrintStringWriter();
        writer.println("Library resolution results discarded for");
        for (ResolvableLibrary library in resolvedLibraries) {
          for (Source source in library.compilationUnitSources) {
            DartEntry dartEntry = _getReadableDartEntry(source);
            if (dartEntry != null) {
              int resultTime = library.getModificationTime(source);
              writer.println("  ${_debuggingString(source)}; sourceTime = ${getModificationStamp(source)}, resultTime = ${resultTime}, cacheTime = ${dartEntry.modificationTime}");
              DartEntryImpl dartCopy = dartEntry.writableCopy;
              if (thrownException == null || resultTime >= 0) {
                //
                // The analysis was performed on out-of-date sources. Mark the cache so that the
                // sources will be re-analyzed using the up-to-date sources.
                //
                dartCopy.recordResolutionNotInProcess();
              } else {
                //
                // We could not determine whether the sources were up-to-date or out-of-date. Mark
                // the cache so that we won't attempt to re-analyze the sources until there's a
                // good chance that we'll be able to do so without error.
                //
                dartCopy.recordResolutionError();
                _cache.remove(source);
              }
              dartCopy.exception = thrownException;
              _cache.put(source, dartCopy);
              if (source == unitSource) {
                unitEntry = dartCopy;
              }
            } else {
              writer.println("  ${_debuggingString(source)}; sourceTime = ${getModificationStamp(source)}, no entry");
            }
          }
        }
        _logInformation(writer.toString());
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    if (unitEntry == null) {
      unitEntry = _getReadableDartEntry(unitSource);
      if (unitEntry == null) {
        throw new AnalysisException.con1("A Dart file became a non-Dart file: ${unitSource.fullName}");
      }
    }
    return unitEntry;
  }

  DartEntry recordResolveDartLibraryTaskResults(ResolveDartLibraryTask task) {
    LibraryResolver resolver = task.libraryResolver;
    AnalysisException thrownException = task.exception;
    DartEntry unitEntry = null;
    Source unitSource = task.unitSource;
    if (resolver != null) {
      //
      // The resolver should only be null if an exception was thrown before (or while) it was
      // being created.
      //
      Set<Library> resolvedLibraries = resolver.resolvedLibraries;
      if (resolvedLibraries == null) {
        //
        // The resolved libraries should only be null if an exception was thrown during resolution.
        //
        unitEntry = _getReadableDartEntry(unitSource);
        if (unitEntry == null) {
          throw new AnalysisException.con1("A Dart file became a non-Dart file: ${unitSource.fullName}");
        }
        DartEntryImpl dartCopy = unitEntry.writableCopy;
        dartCopy.recordResolutionError();
        dartCopy.exception = thrownException;
        _cache.put(unitSource, dartCopy);
        _cache.remove(unitSource);
        if (thrownException != null) {
          throw thrownException;
        }
        return dartCopy;
      }
      if (_allModificationTimesMatch2(resolvedLibraries)) {
        Source htmlSource = sourceFactory.forUri(DartSdk.DART_HTML);
        RecordingErrorListener errorListener = resolver.errorListener;
        for (Library library in resolvedLibraries) {
          Source librarySource = library.librarySource;
          for (Source source in library.compilationUnitSources) {
            CompilationUnit unit = library.getAST(source);
            List<AnalysisError> errors = errorListener.getErrorsForSource(source);
            LineInfo lineInfo = getLineInfo(source);
            DartEntry dartEntry = _cache.get(source) as DartEntry;
            int sourceTime = getModificationStamp(source);
            if (dartEntry.modificationTime != sourceTime) {
              // The source has changed without the context being notified. Simulate notification.
              _sourceChanged(source);
              dartEntry = _getReadableDartEntry(source);
              if (dartEntry == null) {
                throw new AnalysisException.con1("A Dart file became a non-Dart file: ${source.fullName}");
              }
            }
            DartEntryImpl dartCopy = dartEntry.writableCopy;
            if (thrownException == null) {
              dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
              dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.FLUSHED);
              dartCopy.setValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource, unit);
              dartCopy.setValueInLibrary(DartEntry.RESOLUTION_ERRORS, librarySource, errors);
              if (source == librarySource) {
                _recordElementData(dartEntry, dartCopy, library.libraryElement, librarySource, htmlSource);
              }
              _cache.storedAst(source);
            } else {
              dartCopy.recordResolutionError();
              _cache.remove(source);
            }
            dartCopy.exception = thrownException;
            _cache.put(source, dartCopy);
            if (source != librarySource) {
              _workManager.add(source, SourcePriority.PRIORITY_PART);
            }
            if (source == unitSource) {
              unitEntry = dartCopy;
            }
            ChangeNoticeImpl notice = _getNotice(source);
            notice.compilationUnit = unit;
            notice.setErrors(dartCopy.allErrors, lineInfo);
          }
        }
      } else {
        PrintStringWriter writer = new PrintStringWriter();
        writer.println("Library resolution results discarded for");
        for (Library library in resolvedLibraries) {
          for (Source source in library.compilationUnitSources) {
            DartEntry dartEntry = _getReadableDartEntry(source);
            if (dartEntry != null) {
              int resultTime = library.getModificationTime(source);
              writer.println("  ${_debuggingString(source)}; sourceTime = ${getModificationStamp(source)}, resultTime = ${resultTime}, cacheTime = ${dartEntry.modificationTime}");
              DartEntryImpl dartCopy = dartEntry.writableCopy;
              if (thrownException == null || resultTime >= 0) {
                //
                // The analysis was performed on out-of-date sources. Mark the cache so that the
                // sources will be re-analyzed using the up-to-date sources.
                //
                dartCopy.recordResolutionNotInProcess();
              } else {
                //
                // We could not determine whether the sources were up-to-date or out-of-date. Mark
                // the cache so that we won't attempt to re-analyze the sources until there's a
                // good chance that we'll be able to do so without error.
                //
                dartCopy.recordResolutionError();
                _cache.remove(source);
              }
              dartCopy.exception = thrownException;
              _cache.put(source, dartCopy);
              if (source == unitSource) {
                unitEntry = dartCopy;
              }
            } else {
              writer.println("  ${_debuggingString(source)}; sourceTime = ${getModificationStamp(source)}, no entry");
            }
          }
        }
        _logInformation(writer.toString());
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    if (unitEntry == null) {
      unitEntry = _getReadableDartEntry(unitSource);
      if (unitEntry == null) {
        throw new AnalysisException.con1("A Dart file became a non-Dart file: ${unitSource.fullName}");
      }
    }
    return unitEntry;
  }

  /**
   * Record that we have accessed the AST structure associated with the given source. At the moment,
   * there is no differentiation between the parsed and resolved forms of the AST.
   *
   * @param source the source whose AST structure was accessed
   */
  void _accessedAst(Source source) {
    _cache.accessedAst(source);
  }

  /**
   * Add all of the sources contained in the given source container to the given list of sources.
   *
   * Note: This method must only be invoked while we are synchronized on [cacheLock].
   *
   * @param sources the list to which sources are to be added
   * @param container the source container containing the sources to be added to the list
   */
  void _addSourcesInContainer(List<Source> sources, SourceContainer container) {
    for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
      Source source = entry.getKey();
      if (container.contains(source)) {
        sources.add(source);
      }
    }
  }

  /**
   * Return `true` if the modification times of the sources used by the given library resolver
   * to resolve one or more libraries are consistent with the modification times in the cache.
   *
   * @param resolver the library resolver used to resolve one or more libraries
   * @return `true` if we should record the results of the resolution
   * @throws AnalysisException if any of the modification times could not be determined (this should
   *           not happen)
   */
  bool _allModificationTimesMatch(List<ResolvableLibrary> resolvedLibraries) {
    bool allTimesMatch = true;
    for (ResolvableLibrary library in resolvedLibraries) {
      for (Source source in library.compilationUnitSources) {
        DartEntry dartEntry = _getReadableDartEntry(source);
        if (dartEntry == null) {
          // This shouldn't be possible because we should never have performed the task if the
          // source didn't represent a Dart file, but check to be safe.
          throw new AnalysisException.con1("Internal error: attempting to resolve non-Dart file as a Dart file: ${source.fullName}");
        }
        int sourceTime = getModificationStamp(source);
        int resultTime = library.getModificationTime(source);
        if (sourceTime != resultTime) {
          // The source has changed without the context being notified. Simulate notification.
          _sourceChanged(source);
          allTimesMatch = false;
        }
      }
    }
    return allTimesMatch;
  }

  /**
   * Return `true` if the modification times of the sources used by the given library resolver
   * to resolve one or more libraries are consistent with the modification times in the cache.
   *
   * @param resolver the library resolver used to resolve one or more libraries
   * @return `true` if we should record the results of the resolution
   * @throws AnalysisException if any of the modification times could not be determined (this should
   *           not happen)
   */
  bool _allModificationTimesMatch2(Set<Library> resolvedLibraries) {
    bool allTimesMatch = true;
    for (Library library in resolvedLibraries) {
      for (Source source in library.compilationUnitSources) {
        DartEntry dartEntry = _getReadableDartEntry(source);
        if (dartEntry == null) {
          // This shouldn't be possible because we should never have performed the task if the
          // source didn't represent a Dart file, but check to be safe.
          throw new AnalysisException.con1("Internal error: attempting to resolve non-Dart file as a Dart file: ${source.fullName}");
        }
        int sourceTime = getModificationStamp(source);
        int resultTime = library.getModificationTime(source);
        if (sourceTime != resultTime) {
          // The source has changed without the context being notified. Simulate notification.
          _sourceChanged(source);
          allTimesMatch = false;
        }
      }
    }
    return allTimesMatch;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * the state of the data represented by the given descriptor is either [CacheState#VALID] or
   * [CacheState#ERROR]. This method assumes that the data can be produced by generating hints
   * for the library if the data is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  DartEntry _cacheDartHintData(Source unitSource, Source librarySource, DartEntry dartEntry, DataDescriptor descriptor) {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getStateInLibrary(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      DartEntry libraryEntry = _getReadableDartEntry(librarySource);
      libraryEntry = _cacheDartResolutionData(librarySource, librarySource, libraryEntry, DartEntry.ELEMENT);
      LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
      CompilationUnitElement definingUnit = libraryElement.definingCompilationUnit;
      List<CompilationUnitElement> parts = libraryElement.parts;
      List<TimestampedData<CompilationUnit>> units = new List<TimestampedData>(parts.length + 1);
      units[0] = _getResolvedUnit(definingUnit, librarySource);
      if (units[0] == null) {
        Source source = definingUnit.source;
        units[0] = new TimestampedData<CompilationUnit>(getModificationStamp(source), resolveCompilationUnit(source, libraryElement));
      }
      for (int i = 0; i < parts.length; i++) {
        units[i + 1] = _getResolvedUnit(parts[i], librarySource);
        if (units[i + 1] == null) {
          Source source = parts[i].source;
          units[i + 1] = new TimestampedData<CompilationUnit>(getModificationStamp(source), resolveCompilationUnit(source, libraryElement));
        }
      }
      dartEntry = new GenerateDartHintsTask(this, units, getLibraryElement(librarySource)).perform(_resultRecorder) as DartEntry;
      state = dartEntry.getStateInLibrary(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file, return a cache entry in which the state of the data represented
   * by the given descriptor is either [CacheState#VALID] or [CacheState#ERROR]. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  DartEntry _cacheDartParseData(Source source, DartEntry dartEntry, DataDescriptor descriptor) {
    if (identical(descriptor, DartEntry.PARSED_UNIT)) {
      if (dartEntry.hasResolvableCompilationUnit) {
        return dartEntry;
      }
    }
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      dartEntry = _cacheDartScanData(source, dartEntry, DartEntry.TOKEN_STREAM);
      dartEntry = new ParseDartTask(this, source, dartEntry.modificationTime, dartEntry.getValue(DartEntry.TOKEN_STREAM), dartEntry.getValue(SourceEntry.LINE_INFO)).perform(_resultRecorder) as DartEntry;
      state = dartEntry.getState(descriptor);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * the state of the data represented by the given descriptor is either [CacheState#VALID] or
   * [CacheState#ERROR]. This method assumes that the data can be produced by resolving the
   * source in the context of the library if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  DartEntry _cacheDartResolutionData(Source unitSource, Source librarySource, DartEntry dartEntry, DataDescriptor descriptor) {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = (identical(descriptor, DartEntry.ELEMENT)) ? dartEntry.getState(descriptor) : dartEntry.getStateInLibrary(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      // TODO(brianwilkerson) As an optimization, if we already have the element model for the
      // library we can use ResolveDartUnitTask to produce the resolved AST structure much faster.
      dartEntry = new ResolveDartLibraryTask(this, unitSource, librarySource).perform(_resultRecorder) as DartEntry;
      state = (identical(descriptor, DartEntry.ELEMENT)) ? dartEntry.getState(descriptor) : dartEntry.getStateInLibrary(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file, return a cache entry in which the state of the data represented
   * by the given descriptor is either [CacheState#VALID] or [CacheState#ERROR]. This
   * method assumes that the data can be produced by scanning the source if it is not already
   * cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be scanned
   */
  DartEntry _cacheDartScanData(Source source, DartEntry dartEntry, DataDescriptor descriptor) {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      try {
        if (dartEntry.getState(SourceEntry.CONTENT) != CacheState.VALID) {
          dartEntry = new GetContentTask(this, source).perform(_resultRecorder) as DartEntry;
        }
        dartEntry = new ScanDartTask(this, source, dartEntry.modificationTime, dartEntry.getValue(SourceEntry.CONTENT)).perform(_resultRecorder) as DartEntry;
      } on AnalysisException catch (exception) {
        throw exception;
      } on JavaException catch (exception) {
        throw new AnalysisException.con3(exception);
      }
      state = dartEntry.getState(descriptor);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * the state of the data represented by the given descriptor is either [CacheState#VALID] or
   * [CacheState#ERROR]. This method assumes that the data can be produced by verifying the
   * source in the given library if the data is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  DartEntry _cacheDartVerificationData(Source unitSource, Source librarySource, DartEntry dartEntry, DataDescriptor descriptor) {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getStateInLibrary(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      LibraryElement library = computeLibraryElement(librarySource);
      dartEntry = new GenerateDartErrorsTask(this, unitSource, dartEntry.modificationTime, resolveCompilationUnit(unitSource, library), library).perform(_resultRecorder) as DartEntry;
      state = dartEntry.getStateInLibrary(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for an HTML file, return a cache entry in which all of the data represented by
   * the state of the given descriptors is either [CacheState#VALID] or
   * [CacheState#ERROR]. This method assumes that the data can be produced by parsing the
   * source if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the HTML file
   * @param htmlEntry the cache entry associated with the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  HtmlEntry _cacheHtmlParseData(Source source, HtmlEntry htmlEntry, DataDescriptor descriptor) {
    if (identical(descriptor, HtmlEntry.PARSED_UNIT)) {
      ht.HtmlUnit unit = htmlEntry.anyParsedUnit;
      if (unit != null) {
        return htmlEntry;
      }
    }
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = htmlEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      try {
        if (htmlEntry.getState(SourceEntry.CONTENT) != CacheState.VALID) {
          htmlEntry = new GetContentTask(this, source).perform(_resultRecorder) as HtmlEntry;
        }
        htmlEntry = new ParseHtmlTask(this, source, htmlEntry.modificationTime, htmlEntry.getValue(SourceEntry.CONTENT)).perform(_resultRecorder) as HtmlEntry;
      } on AnalysisException catch (exception) {
        throw exception;
      } on JavaException catch (exception) {
        throw new AnalysisException.con3(exception);
      }
      state = htmlEntry.getState(descriptor);
    }
    return htmlEntry;
  }

  /**
   * Given a source for an HTML file, return a cache entry in which the state of the data
   * represented by the given descriptor is either [CacheState#VALID] or
   * [CacheState#ERROR]. This method assumes that the data can be produced by resolving the
   * source if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the HTML file
   * @param dartEntry the cache entry associated with the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  HtmlEntry _cacheHtmlResolutionData(Source source, HtmlEntry htmlEntry, DataDescriptor descriptor) {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = htmlEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      htmlEntry = _cacheHtmlParseData(source, htmlEntry, HtmlEntry.PARSED_UNIT);
      htmlEntry = new ResolveHtmlTask(this, source, htmlEntry.modificationTime, htmlEntry.getValue(HtmlEntry.PARSED_UNIT)).perform(_resultRecorder) as HtmlEntry;
      state = htmlEntry.getState(descriptor);
    }
    return htmlEntry;
  }

  /**
   * Given the encoded form of a source, use the source factory to reconstitute the original source.
   *
   * @param encoding the encoded form of a source
   * @return the source represented by the encoding
   */
  Source _computeSourceFromEncoding(String encoding) => _sourceFactory.fromEncoding(encoding);

  /**
   * Return `true` if the given array of sources contains the given source.
   *
   * @param sources the sources being searched
   * @param targetSource the source being searched for
   * @return `true` if the given source is in the array
   */
  bool _contains(List<Source> sources, Source targetSource) {
    for (Source source in sources) {
      if (source == targetSource) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return `true` if the given array of sources contains any of the given target sources.
   *
   * @param sources the sources being searched
   * @param targetSources the sources being searched for
   * @return `true` if any of the given target sources are in the array
   */
  bool _containsAny(List<Source> sources, List<Source> targetSources) {
    for (Source targetSource in targetSources) {
      if (_contains(sources, targetSource)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Create a [GenerateDartErrorsTask] for the given source, marking the verification errors
   * as being in-process. The compilation unit and the library can be the same if the compilation
   * unit is the defining compilation unit of the library.
   *
   * @param unitSource the source for the compilation unit to be verified
   * @param unitEntry the entry for the compilation unit
   * @param librarySource the source for the library containing the compilation unit
   * @param libraryEntry the entry for the library
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createGenerateDartErrorsTask(Source unitSource, DartEntry unitEntry, Source librarySource, DartEntry libraryEntry) {
    if (unitEntry.getStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource) != CacheState.VALID || libraryEntry.getState(DartEntry.ELEMENT) != CacheState.VALID) {
      return _createResolveDartLibraryTask(librarySource, libraryEntry);
    }
    CompilationUnit unit = unitEntry.getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
    LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
    DartEntryImpl dartCopy = unitEntry.writableCopy;
    dartCopy.setStateInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource, CacheState.IN_PROCESS);
    _cache.put(unitSource, dartCopy);
    return new AnalysisContextImpl_TaskData(new GenerateDartErrorsTask(this, unitSource, dartCopy.modificationTime, unit, libraryElement), false);
  }

  /**
   * Create a [GenerateDartHintsTask] for the given source, marking the hints as being
   * in-process.
   *
   * @param source the source whose content is to be verified
   * @param dartEntry the entry for the source
   * @param librarySource the source for the library containing the source
   * @param libraryEntry the entry for the library
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createGenerateDartHintsTask(Source source, DartEntry dartEntry, Source librarySource, DartEntry libraryEntry) {
    if (libraryEntry.getState(DartEntry.ELEMENT) != CacheState.VALID) {
      return _createResolveDartLibraryTask(librarySource, libraryEntry);
    }
    LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
    CompilationUnitElement definingUnit = libraryElement.definingCompilationUnit;
    List<CompilationUnitElement> parts = libraryElement.parts;
    List<TimestampedData<CompilationUnit>> units = new List<TimestampedData>(parts.length + 1);
    units[0] = _getResolvedUnit(definingUnit, librarySource);
    if (units[0] == null) {
      // TODO(brianwilkerson) We should return a ResolveDartUnitTask (unless there are multiple ASTs
      // that need to be resolved.
      return _createResolveDartLibraryTask(librarySource, libraryEntry);
    }
    for (int i = 0; i < parts.length; i++) {
      units[i + 1] = _getResolvedUnit(parts[i], librarySource);
      if (units[i + 1] == null) {
        // TODO(brianwilkerson) We should return a ResolveDartUnitTask (unless there are multiple
        // ASTs that need to be resolved.
        return _createResolveDartLibraryTask(librarySource, libraryEntry);
      }
    }
    DartEntryImpl dartCopy = dartEntry.writableCopy;
    dartCopy.setStateInLibrary(DartEntry.HINTS, librarySource, CacheState.IN_PROCESS);
    _cache.put(source, dartCopy);
    return new AnalysisContextImpl_TaskData(new GenerateDartHintsTask(this, units, libraryElement), false);
  }

  /**
   * Create a [GetContentTask] for the given source, marking the content as being in-process.
   *
   * @param source the source whose content is to be accessed
   * @param sourceEntry the entry for the source
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createGetContentTask(Source source, SourceEntry sourceEntry) {
    SourceEntryImpl sourceCopy = sourceEntry.writableCopy;
    sourceCopy.setState(SourceEntry.CONTENT, CacheState.IN_PROCESS);
    _cache.put(source, sourceCopy);
    return new AnalysisContextImpl_TaskData(new GetContentTask(this, source), false);
  }

  /**
   * Create a [ParseDartTask] for the given source, marking the parse errors as being
   * in-process.
   *
   * @param source the source whose content is to be parsed
   * @param dartEntry the entry for the source
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createParseDartTask(Source source, DartEntry dartEntry) {
    if (dartEntry.getState(DartEntry.TOKEN_STREAM) != CacheState.VALID || dartEntry.getState(SourceEntry.LINE_INFO) != CacheState.VALID) {
      return _createScanDartTask(source, dartEntry);
    }
    Token tokenStream = dartEntry.getValue(DartEntry.TOKEN_STREAM);
    DartEntryImpl dartCopy = dartEntry.writableCopy;
    dartCopy.setState(DartEntry.TOKEN_STREAM, CacheState.FLUSHED);
    dartCopy.setState(DartEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
    _cache.put(source, dartCopy);
    return new AnalysisContextImpl_TaskData(new ParseDartTask(this, source, dartCopy.modificationTime, tokenStream, dartEntry.getValue(SourceEntry.LINE_INFO)), false);
  }

  /**
   * Create a [ParseHtmlTask] for the given source, marking the parse errors as being
   * in-process.
   *
   * @param source the source whose content is to be parsed
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createParseHtmlTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(SourceEntry.CONTENT) != CacheState.VALID) {
      return _createGetContentTask(source, htmlEntry);
    }
    String content = htmlEntry.getValue(SourceEntry.CONTENT);
    HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
    htmlCopy.setState(SourceEntry.CONTENT, CacheState.FLUSHED);
    htmlCopy.setState(HtmlEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
    _cache.put(source, htmlCopy);
    return new AnalysisContextImpl_TaskData(new ParseHtmlTask(this, source, htmlCopy.modificationTime, content), false);
  }

  /**
   * Create a [ResolveAngularComponentTemplateTask] for the given source, marking the angular
   * errors as being in-process.
   *
   * @param source the source whose content is to be resolved
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createResolveAngularComponentTemplateTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(HtmlEntry.RESOLVED_UNIT) != CacheState.VALID) {
      return _createResolveHtmlTask(source, htmlEntry);
    }
    AngularApplication application = htmlEntry.getValue(HtmlEntry.ANGULAR_APPLICATION);
    AngularComponentElement component = htmlEntry.getValue(HtmlEntry.ANGULAR_COMPONENT);
    HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
    htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.IN_PROCESS);
    _cache.put(source, htmlCopy);
    return new AnalysisContextImpl_TaskData(new ResolveAngularComponentTemplateTask(this, source, htmlCopy.modificationTime, htmlCopy.getValue(HtmlEntry.RESOLVED_UNIT), component, application), false);
  }

  /**
   * Create a [ResolveAngularEntryHtmlTask] for the given source, marking the angular entry as
   * being in-process.
   *
   * @param source the source whose content is to be resolved
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createResolveAngularEntryHtmlTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(HtmlEntry.RESOLVED_UNIT) != CacheState.VALID) {
      return _createResolveHtmlTask(source, htmlEntry);
    }
    HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
    htmlCopy.setState(HtmlEntry.ANGULAR_ENTRY, CacheState.IN_PROCESS);
    _cache.put(source, htmlCopy);
    return new AnalysisContextImpl_TaskData(new ResolveAngularEntryHtmlTask(this, source, htmlCopy.modificationTime, htmlCopy.getValue(HtmlEntry.RESOLVED_UNIT)), false);
  }

  /**
   * Create a [ResolveDartLibraryTask] for the given source, marking the element model as
   * being in-process.
   *
   * @param source the source whose content is to be resolved
   * @param dartEntry the entry for the source
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createResolveDartLibraryTask(Source source, DartEntry dartEntry) {
    try {
      AnalysisContextImpl_CycleBuilder builder = new AnalysisContextImpl_CycleBuilder(this);
      builder.computeCycleContaining(source);
      AnalysisContextImpl_TaskData taskData = builder.taskData;
      if (taskData != null) {
        return taskData;
      }
      return new AnalysisContextImpl_TaskData(new ResolveDartLibraryCycleTask(this, source, source, builder.librariesInCycle), false);
    } on AnalysisException catch (exception) {
      AnalysisEngine.instance.logger.logError2("Internal error trying to compute the next analysis task", exception);
    }
    return new AnalysisContextImpl_TaskData(null, false);
  }

  /**
   * Create a [ResolveHtmlTask] for the given source, marking the resolved unit as being
   * in-process.
   *
   * @param source the source whose content is to be resolved
   * @param htmlEntry the entry for the source
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createResolveHtmlTask(Source source, HtmlEntry htmlEntry) {
    if (htmlEntry.getState(HtmlEntry.PARSED_UNIT) != CacheState.VALID) {
      return _createParseHtmlTask(source, htmlEntry);
    }
    HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
    htmlCopy.setState(HtmlEntry.RESOLVED_UNIT, CacheState.IN_PROCESS);
    _cache.put(source, htmlCopy);
    return new AnalysisContextImpl_TaskData(new ResolveHtmlTask(this, source, htmlCopy.modificationTime, htmlCopy.getValue(HtmlEntry.PARSED_UNIT)), false);
  }

  /**
   * Create a [ScanDartTask] for the given source, marking the scan errors as being
   * in-process.
   *
   * @param source the source whose content is to be scanned
   * @param dartEntry the entry for the source
   * @return task data representing the created task
   */
  AnalysisContextImpl_TaskData _createScanDartTask(Source source, DartEntry dartEntry) {
    if (dartEntry.getState(SourceEntry.CONTENT) != CacheState.VALID) {
      return _createGetContentTask(source, dartEntry);
    }
    String content = dartEntry.getValue(SourceEntry.CONTENT);
    DartEntryImpl dartCopy = dartEntry.writableCopy;
    dartCopy.setState(SourceEntry.CONTENT, CacheState.FLUSHED);
    dartCopy.setState(DartEntry.SCAN_ERRORS, CacheState.IN_PROCESS);
    _cache.put(source, dartCopy);
    return new AnalysisContextImpl_TaskData(new ScanDartTask(this, source, dartCopy.modificationTime, content), false);
  }

  /**
   * Create a source information object suitable for the given source. Return the source information
   * object that was created, or `null` if the source should not be tracked by this context.
   *
   * @param source the source for which an information object is being created
   * @param explicitlyAdded `true` if the source was explicitly added to the context
   * @return the source information object that was created
   */
  SourceEntry _createSourceEntry(Source source, bool explicitlyAdded) {
    String name = source.shortName;
    if (AnalysisEngine.isHtmlFileName(name)) {
      HtmlEntryImpl htmlEntry = new HtmlEntryImpl();
      htmlEntry.modificationTime = getModificationStamp(source);
      htmlEntry.explicitlyAdded = explicitlyAdded;
      _cache.put(source, htmlEntry);
      return htmlEntry;
    } else {
      DartEntryImpl dartEntry = new DartEntryImpl();
      dartEntry.modificationTime = getModificationStamp(source);
      dartEntry.explicitlyAdded = explicitlyAdded;
      _cache.put(source, dartEntry);
      return dartEntry;
    }
  }

  /**
   * Return a string with debugging information about the given source (the full name and
   * modification stamp of the source).
   *
   * @param source the source for which a debugging string is to be produced
   * @return debugging information about the given source
   */
  String _debuggingString(Source source) => "'${source.fullName}' [${getModificationStamp(source)}]";

  /**
   * Return an array containing all of the change notices that are waiting to be returned. If there
   * are no notices, then return either `null` or an empty array, depending on the value of
   * the argument.
   *
   * @param nullIfEmpty `true` if `null` should be returned when there are no notices
   * @return the change notices that are waiting to be returned
   */
  List<ChangeNotice> _getChangeNotices(bool nullIfEmpty) {
    if (_pendingNotices.isEmpty) {
      if (nullIfEmpty) {
        return null;
      }
      return ChangeNoticeImpl.EMPTY_ARRAY;
    }
    List<ChangeNotice> notices = new List.from(_pendingNotices.values);
    _pendingNotices.clear();
    return notices;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by generating hints for the library if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the entry representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  Object _getDartHintData(Source unitSource, Source librarySource, DartEntry dartEntry, DataDescriptor descriptor) {
    dartEntry = _cacheDartHintData(unitSource, librarySource, dartEntry, descriptor);
    if (identical(descriptor, DartEntry.ELEMENT)) {
      return dartEntry.getValue(descriptor);
    }
    return dartEntry.getValueInLibrary(descriptor, librarySource);
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source. This method assumes that the data can be produced by parsing the
   * source if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  Object _getDartParseData(Source source, DartEntry dartEntry, DataDescriptor descriptor) {
    dartEntry = _cacheDartParseData(source, dartEntry, descriptor);
    if (identical(descriptor, DartEntry.PARSED_UNIT)) {
      _accessedAst(source);
      return dartEntry.anyParsedCompilationUnit;
    }
    return dartEntry.getValue(descriptor);
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not a Dart file. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  Object _getDartParseData2(Source source, DataDescriptor descriptor, Object defaultValue) {
    DartEntry dartEntry = _getReadableDartEntry(source);
    if (dartEntry == null) {
      return defaultValue;
    }
    try {
      return _getDartParseData(source, dartEntry, descriptor);
    } on ObsoleteSourceAnalysisException catch (exception) {
      AnalysisEngine.instance.logger.logInformation2("Could not compute ${descriptor.toString()}", exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by resolving the source in the context of the library if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the entry representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  Object _getDartResolutionData(Source unitSource, Source librarySource, DartEntry dartEntry, DataDescriptor descriptor) {
    dartEntry = _cacheDartResolutionData(unitSource, librarySource, dartEntry, descriptor);
    if (identical(descriptor, DartEntry.ELEMENT)) {
      return dartEntry.getValue(descriptor);
    } else if (identical(descriptor, DartEntry.RESOLVED_UNIT)) {
      _accessedAst(unitSource);
    }
    return dartEntry.getValueInLibrary(descriptor, librarySource);
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source, or the given default value if the
   * source is not a Dart file. This method assumes that the data can be produced by resolving the
   * source in the context of the library if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  Object _getDartResolutionData2(Source unitSource, Source librarySource, DataDescriptor descriptor, Object defaultValue) {
    DartEntry dartEntry = _getReadableDartEntry(unitSource);
    if (dartEntry == null) {
      return defaultValue;
    }
    try {
      return _getDartResolutionData(unitSource, librarySource, dartEntry, descriptor);
    } on ObsoleteSourceAnalysisException catch (exception) {
      AnalysisEngine.instance.logger.logInformation2("Could not compute ${descriptor.toString()}", exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source. This method assumes that the data can be produced by scanning the
   * source if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be scanned
   */
  Object _getDartScanData(Source source, DartEntry dartEntry, DataDescriptor descriptor) {
    dartEntry = _cacheDartScanData(source, dartEntry, descriptor);
    return dartEntry.getValue(descriptor);
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not a Dart file. This
   * method assumes that the data can be produced by scanning the source if it is not already
   * cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be scanned
   */
  Object _getDartScanData2(Source source, DataDescriptor descriptor, Object defaultValue) {
    DartEntry dartEntry = _getReadableDartEntry(source);
    if (dartEntry == null) {
      return defaultValue;
    }
    try {
      return _getDartScanData(source, dartEntry, descriptor);
    } on ObsoleteSourceAnalysisException catch (exception) {
      AnalysisEngine.instance.logger.logInformation2("Could not compute ${descriptor.toString()}", exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by verifying the source within the given library if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the entry representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  Object _getDartVerificationData(Source unitSource, Source librarySource, DartEntry dartEntry, DataDescriptor descriptor) {
    dartEntry = _cacheDartVerificationData(unitSource, librarySource, dartEntry, descriptor);
    return dartEntry.getValueInLibrary(descriptor, librarySource);
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not an HTML file. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not an HTML file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  Object _getHtmlParseData(Source source, DataDescriptor descriptor, Object defaultValue) {
    HtmlEntry htmlEntry = _getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      return defaultValue;
    }
    htmlEntry = _cacheHtmlParseData(source, htmlEntry, descriptor);
    if (identical(descriptor, HtmlEntry.PARSED_UNIT)) {
      _accessedAst(source);
      return htmlEntry.anyParsedUnit;
    }
    return htmlEntry.getValue(descriptor);
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not an HTML file. This
   * method assumes that the data can be produced by resolving the source if it is not already
   * cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not an HTML file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  Object _getHtmlResolutionData(Source source, DataDescriptor descriptor, Object defaultValue) {
    HtmlEntry htmlEntry = _getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      return defaultValue;
    }
    try {
      return _getHtmlResolutionData2(source, htmlEntry, descriptor);
    } on ObsoleteSourceAnalysisException catch (exception) {
      AnalysisEngine.instance.logger.logInformation2("Could not compute ${descriptor.toString()}", exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source. This method assumes that the data can be produced by resolving the
   * source if it is not already cached.
   *
   * <b>Note:</b> This method cannot be used in an async environment.
   *
   * @param source the source representing the HTML file
   * @param htmlEntry the entry representing the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  Object _getHtmlResolutionData2(Source source, HtmlEntry htmlEntry, DataDescriptor descriptor) {
    htmlEntry = _cacheHtmlResolutionData(source, htmlEntry, descriptor);
    if (identical(descriptor, HtmlEntry.RESOLVED_UNIT)) {
      _accessedAst(source);
    }
    return htmlEntry.getValue(descriptor);
  }

  /**
   * Look through the cache for a task that needs to be performed. Return the task that was found,
   * or `null` if there is no more work to be done.
   *
   * @return the next task that needs to be performed
   */
  AnalysisTask get nextAnalysisTask {
    bool hintsEnabled = _options.hint;
    bool hasBlockedTask = false;
    //
    // Look for incremental analysis
    //
    if (_incrementalAnalysisCache != null && _incrementalAnalysisCache.hasWork) {
      AnalysisTask task = new IncrementalAnalysisTask(this, _incrementalAnalysisCache);
      _incrementalAnalysisCache = null;
      return task;
    }
    //
    // Look for a priority source that needs to be analyzed.
    //
    int priorityCount = _priorityOrder.length;
    for (int i = 0; i < priorityCount; i++) {
      Source source = _priorityOrder[i];
      AnalysisContextImpl_TaskData taskData = _getNextAnalysisTaskForSource(source, _cache.get(source), true, hintsEnabled);
      AnalysisTask task = taskData.task;
      if (task != null) {
        return task;
      } else if (taskData.isBlocked) {
        hasBlockedTask = true;
      }
    }
    if (_neededForResolution != null) {
      List<Source> sourcesToRemove = new List<Source>();
      for (Source source in _neededForResolution) {
        SourceEntry sourceEntry = _cache.get(source);
        if (sourceEntry is DartEntry) {
          DartEntry dartEntry = sourceEntry;
          if (!dartEntry.hasResolvableCompilationUnit) {
            if (dartEntry.getState(DartEntry.PARSED_UNIT) == CacheState.ERROR) {
              sourcesToRemove.add(source);
            } else {
              AnalysisContextImpl_TaskData taskData = _createParseDartTask(source, dartEntry);
              AnalysisTask task = taskData.task;
              if (task != null) {
                return task;
              } else if (taskData.isBlocked) {
                hasBlockedTask = true;
              }
            }
          }
        }
      }
      int count = sourcesToRemove.length;
      for (int i = 0; i < count; i++) {
        _neededForResolution.remove(sourcesToRemove[i]);
      }
    }
    //
    // Look for a non-priority source that needs to be analyzed.
    //
    List<Source> sourcesToRemove = new List<Source>();
    WorkManager_WorkIterator sources = _workManager.iterator();
    while (sources.hasNext) {
      Source source = sources.next();
      AnalysisContextImpl_TaskData taskData = _getNextAnalysisTaskForSource(source, _cache.get(source), false, hintsEnabled);
      AnalysisTask task = taskData.task;
      if (task != null) {
        int count = sourcesToRemove.length;
        for (int i = 0; i < count; i++) {
          _workManager.remove(sourcesToRemove[i]);
        }
        return task;
      } else if (taskData.isBlocked) {
        hasBlockedTask = true;
      } else {
        sourcesToRemove.add(source);
      }
    }
    int count = sourcesToRemove.length;
    for (int i = 0; i < count; i++) {
      _workManager.remove(sourcesToRemove[i]);
    }
    //      //
    //      // Look for a non-priority source that needs to be analyzed and was missed by the loop above.
    //      //
    //      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
    //        source = entry.getKey();
    //        TaskData taskData = getNextAnalysisTaskForSource(source, entry.getValue(), false, hintsEnabled);
    //        AnalysisTask task = taskData.getTask();
    //        if (task != null) {
    //          System.out.println("Failed to analyze " + source.getFullName());
    //          return task;
    //        }
    //      }
    if (hasBlockedTask) {
      // All of the analysis work is blocked waiting for an asynchronous task to complete.
      return WaitForAsyncTask.instance;
    }
    return null;
  }

  /**
   * Look at the given source to see whether a task needs to be performed related to it. Return the
   * task that should be performed, or `null` if there is no more work to be done for the
   * source.
   *
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * @param source the source to be checked
   * @param sourceEntry the cache entry associated with the source
   * @param isPriority `true` if the source is a priority source
   * @param hintsEnabled `true` if hints are currently enabled
   * @return the next task that needs to be performed for the given source
   */
  AnalysisContextImpl_TaskData _getNextAnalysisTaskForSource(Source source, SourceEntry sourceEntry, bool isPriority, bool hintsEnabled) {
    if (sourceEntry == null) {
      return new AnalysisContextImpl_TaskData(null, false);
    }
    CacheState contentState = sourceEntry.getState(SourceEntry.CONTENT);
    if (contentState == CacheState.INVALID) {
      return _createGetContentTask(source, sourceEntry);
    } else if (contentState == CacheState.IN_PROCESS) {
      // We are already in the process of getting the content. There's nothing else we can do with
      // this source until that's complete.
      return new AnalysisContextImpl_TaskData(null, true);
    } else if (contentState == CacheState.ERROR) {
      // We have done all of the analysis we can for this source because we cannot get its content.
      return new AnalysisContextImpl_TaskData(null, false);
    }
    if (sourceEntry is DartEntry) {
      DartEntry dartEntry = sourceEntry;
      CacheState scanErrorsState = dartEntry.getState(DartEntry.SCAN_ERRORS);
      if (scanErrorsState == CacheState.INVALID || (isPriority && scanErrorsState == CacheState.FLUSHED)) {
        return _createScanDartTask(source, dartEntry);
      }
      CacheState parseErrorsState = dartEntry.getState(DartEntry.PARSE_ERRORS);
      if (parseErrorsState == CacheState.INVALID || (isPriority && parseErrorsState == CacheState.FLUSHED)) {
        return _createParseDartTask(source, dartEntry);
      }
      if (isPriority && parseErrorsState != CacheState.ERROR) {
        if (!dartEntry.hasResolvableCompilationUnit) {
          return _createParseDartTask(source, dartEntry);
        }
      }
      SourceKind kind = dartEntry.getValue(DartEntry.SOURCE_KIND);
      if (kind == SourceKind.UNKNOWN) {
        return _createParseDartTask(source, dartEntry);
      } else if (kind == SourceKind.LIBRARY) {
        CacheState elementState = dartEntry.getState(DartEntry.ELEMENT);
        if (elementState == CacheState.INVALID) {
          return _createResolveDartLibraryTask(source, dartEntry);
        }
      }
      List<Source> librariesContaining = dartEntry.getValue(DartEntry.CONTAINING_LIBRARIES);
      for (Source librarySource in librariesContaining) {
        SourceEntry librarySourceEntry = _cache.get(librarySource);
        if (librarySourceEntry is DartEntry) {
          DartEntry libraryEntry = librarySourceEntry;
          CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
          if (elementState == CacheState.INVALID || (isPriority && elementState == CacheState.FLUSHED)) {
            //return createResolveDartLibraryTask(librarySource, (DartEntry) libraryEntry);
            DartEntryImpl libraryCopy = libraryEntry.writableCopy;
            libraryCopy.setState(DartEntry.ELEMENT, CacheState.IN_PROCESS);
            _cache.put(librarySource, libraryCopy);
            return new AnalysisContextImpl_TaskData(new ResolveDartLibraryTask(this, source, librarySource), false);
          }
          CacheState resolvedUnitState = dartEntry.getStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
          if (resolvedUnitState == CacheState.INVALID || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
            //
            // The commented out lines below are an optimization that doesn't quite work yet. The
            // problem is that if the source was not resolved because it wasn't part of any library,
            // then there won't be any elements in the element model that we can use to resolve it.
            //
            //LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
            //if (libraryElement != null) {
            //  return new ResolveDartUnitTask(this, source, libraryElement);
            //}
            // Possibly replace with: return createResolveDartLibraryTask(librarySource, (DartEntry) libraryEntry);
            DartEntryImpl dartCopy = dartEntry.writableCopy;
            dartCopy.setStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource, CacheState.IN_PROCESS);
            _cache.put(source, dartCopy);
            return new AnalysisContextImpl_TaskData(new ResolveDartLibraryTask(this, source, librarySource), false);
          }
          if (_generateSdkErrors || !source.isInSystemLibrary) {
            CacheState verificationErrorsState = dartEntry.getStateInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource);
            if (verificationErrorsState == CacheState.INVALID || (isPriority && verificationErrorsState == CacheState.FLUSHED)) {
              return _createGenerateDartErrorsTask(source, dartEntry, librarySource, libraryEntry);
            }
            if (hintsEnabled) {
              CacheState hintsState = dartEntry.getStateInLibrary(DartEntry.HINTS, librarySource);
              if (hintsState == CacheState.INVALID || (isPriority && hintsState == CacheState.FLUSHED)) {
                return _createGenerateDartHintsTask(source, dartEntry, librarySource, libraryEntry);
              }
            }
          }
        }
      }
    } else if (sourceEntry is HtmlEntry) {
      HtmlEntry htmlEntry = sourceEntry;
      CacheState parseErrorsState = htmlEntry.getState(HtmlEntry.PARSE_ERRORS);
      if (parseErrorsState == CacheState.INVALID || (isPriority && parseErrorsState == CacheState.FLUSHED)) {
        return _createParseHtmlTask(source, htmlEntry);
      }
      if (isPriority && parseErrorsState != CacheState.ERROR) {
        ht.HtmlUnit parsedUnit = htmlEntry.anyParsedUnit;
        if (parsedUnit == null) {
          return _createParseHtmlTask(source, htmlEntry);
        }
      }
      CacheState resolvedUnitState = htmlEntry.getState(HtmlEntry.RESOLVED_UNIT);
      if (resolvedUnitState == CacheState.INVALID || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
        return _createResolveHtmlTask(source, htmlEntry);
      }
      //
      // Angular support
      //
      if (_options.analyzeAngular) {
        // Try to resolve the HTML as an Angular entry point.
        CacheState angularEntryState = htmlEntry.getState(HtmlEntry.ANGULAR_ENTRY);
        if (angularEntryState == CacheState.INVALID || (isPriority && angularEntryState == CacheState.FLUSHED)) {
          return _createResolveAngularEntryHtmlTask(source, htmlEntry);
        }
        // Try to resolve the HTML as an Angular application part.
        CacheState angularErrorsState = htmlEntry.getState(HtmlEntry.ANGULAR_ERRORS);
        if (angularErrorsState == CacheState.INVALID || (isPriority && angularErrorsState == CacheState.FLUSHED)) {
          return _createResolveAngularComponentTemplateTask(source, htmlEntry);
        }
      }
    }
    return new AnalysisContextImpl_TaskData(null, false);
  }

  /**
   * Return a change notice for the given source, creating one if one does not already exist.
   *
   * @param source the source for which changes are being reported
   * @return a change notice for the given source
   */
  ChangeNoticeImpl _getNotice(Source source) {
    ChangeNoticeImpl notice = _pendingNotices[source];
    if (notice == null) {
      notice = new ChangeNoticeImpl(source);
      _pendingNotices[source] = notice;
    }
    return notice;
  }

  /**
   * Return the cache entry associated with the given source, or `null` if the source is not a
   * Dart file.
   *
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  DartEntry _getReadableDartEntry(Source source) {
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      sourceEntry = _createSourceEntry(source, false);
    }
    if (sourceEntry is DartEntry) {
      return sourceEntry as DartEntry;
    }
    return null;
  }

  /**
   * Return the cache entry associated with the given source, or `null` if the source is not
   * an HTML file.
   *
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  HtmlEntry _getReadableHtmlEntry(Source source) {
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      sourceEntry = _createSourceEntry(source, false);
    }
    if (sourceEntry is HtmlEntry) {
      return sourceEntry as HtmlEntry;
    }
    return null;
  }

  /**
   * Return the cache entry associated with the given source, creating it if necessary.
   *
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  SourceEntry _getReadableSourceEntry(Source source) {
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      sourceEntry = _createSourceEntry(source, false);
    }
    return sourceEntry;
  }

  /**
   * Return the cache entry associated with the given source, or `null` if there is no entry
   * associated with the source.
   *
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  SourceEntry _getReadableSourceEntryOrNull(Source source) => _cache.get(source);

  /**
   * Return a resolved compilation unit corresponding to the given element in the given library, or
   * `null` if the information is not cached.
   *
   * @param element the element representing the compilation unit
   * @param librarySource the source representing the library containing the unit
   * @return the specified resolved compilation unit
   */
  TimestampedData<CompilationUnit> _getResolvedUnit(CompilationUnitElement element, Source librarySource) {
    SourceEntry sourceEntry = _cache.get(element.source);
    if (sourceEntry is DartEntry) {
      DartEntry dartEntry = sourceEntry;
      if (dartEntry.getStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource) == CacheState.VALID) {
        return new TimestampedData<CompilationUnit>(dartEntry.modificationTime, dartEntry.getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource));
      }
    }
    return null;
  }

  /**
   * Return an array containing all of the sources known to this context that have the given kind.
   *
   * @param kind the kind of sources to be returned
   * @return all of the sources known to this context that have the given kind
   */
  List<Source> _getSources(SourceKind kind) {
    List<Source> sources = new List<Source>();
    for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
      if (entry.getValue().kind == kind) {
        sources.add(entry.getKey());
      }
    }
    return new List.from(sources);
  }

  /**
   * Look at the given source to see whether a task needs to be performed related to it. If so, add
   * the source to the set of sources that need to be processed. This method duplicates, and must
   * therefore be kept in sync with,
   * [getNextAnalysisTask]. This method is intended to
   * be used for testing purposes only.
   *
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * @param source the source to be checked
   * @param sourceEntry the cache entry associated with the source
   * @param isPriority `true` if the source is a priority source
   * @param hintsEnabled `true` if hints are currently enabled
   * @param sources the set to which sources should be added
   */
  void _getSourcesNeedingProcessing(Source source, SourceEntry sourceEntry, bool isPriority, bool hintsEnabled, Set<Source> sources) {
    if (sourceEntry is DartEntry) {
      DartEntry dartEntry = sourceEntry;
      CacheState scanErrorsState = dartEntry.getState(DartEntry.SCAN_ERRORS);
      if (scanErrorsState == CacheState.INVALID || (isPriority && scanErrorsState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      CacheState parseErrorsState = dartEntry.getState(DartEntry.PARSE_ERRORS);
      if (parseErrorsState == CacheState.INVALID || (isPriority && parseErrorsState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      if (isPriority) {
        if (!dartEntry.hasResolvableCompilationUnit) {
          sources.add(source);
          return;
        }
      }
      for (Source librarySource in getLibrariesContaining(source)) {
        SourceEntry libraryEntry = _cache.get(librarySource);
        if (libraryEntry is DartEntry) {
          CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
          if (elementState == CacheState.INVALID || (isPriority && elementState == CacheState.FLUSHED)) {
            sources.add(source);
            return;
          }
          CacheState resolvedUnitState = dartEntry.getStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
          if (resolvedUnitState == CacheState.INVALID || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
            LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
            if (libraryElement != null) {
              sources.add(source);
              return;
            }
          }
          CacheState verificationErrorsState = dartEntry.getStateInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource);
          if (verificationErrorsState == CacheState.INVALID || (isPriority && verificationErrorsState == CacheState.FLUSHED)) {
            LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
            if (libraryElement != null) {
              sources.add(source);
              return;
            }
          }
          if (hintsEnabled) {
            CacheState hintsState = dartEntry.getStateInLibrary(DartEntry.HINTS, librarySource);
            if (hintsState == CacheState.INVALID || (isPriority && hintsState == CacheState.FLUSHED)) {
              LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
              if (libraryElement != null) {
                sources.add(source);
                return;
              }
            }
          }
        }
      }
    } else if (sourceEntry is HtmlEntry) {
      HtmlEntry htmlEntry = sourceEntry;
      CacheState parsedUnitState = htmlEntry.getState(HtmlEntry.PARSED_UNIT);
      if (parsedUnitState == CacheState.INVALID || (isPriority && parsedUnitState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      CacheState resolvedUnitState = htmlEntry.getState(HtmlEntry.RESOLVED_UNIT);
      if (resolvedUnitState == CacheState.INVALID || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      // Angular
      if (_options.analyzeAngular) {
        CacheState angularErrorsState = htmlEntry.getState(HtmlEntry.ANGULAR_ERRORS);
        if (angularErrorsState == CacheState.INVALID || (isPriority && angularErrorsState == CacheState.FLUSHED)) {
          AngularApplication entryInfo = htmlEntry.getValue(HtmlEntry.ANGULAR_ENTRY);
          if (entryInfo != null) {
            sources.add(source);
            return;
          }
          AngularApplication applicationInfo = htmlEntry.getValue(HtmlEntry.ANGULAR_APPLICATION);
          if (applicationInfo != null) {
            AngularComponentElement component = htmlEntry.getValue(HtmlEntry.ANGULAR_COMPONENT);
            if (component != null) {
              sources.add(source);
              return;
            }
          }
        }
      }
    }
  }

  /**
   * Invalidate all of the resolution results computed by this context.
   *
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   */
  void _invalidateAllResolutionInformation() {
    Map<Source, List<Source>> oldPartMap = new Map<Source, List<Source>>();
    for (MapEntry<Source, SourceEntry> mapEntry in _cache.entrySet()) {
      Source source = mapEntry.getKey();
      SourceEntry sourceEntry = mapEntry.getValue();
      if (sourceEntry is HtmlEntry) {
        HtmlEntryImpl htmlCopy = sourceEntry.writableCopy;
        htmlCopy.invalidateAllResolutionInformation();
        mapEntry.setValue(htmlCopy);
      } else if (sourceEntry is DartEntry) {
        DartEntry dartEntry = sourceEntry;
        oldPartMap[source] = dartEntry.getValue(DartEntry.INCLUDED_PARTS);
        DartEntryImpl dartCopy = dartEntry.writableCopy;
        dartCopy.invalidateAllResolutionInformation();
        mapEntry.setValue(dartCopy);
        _workManager.add(source, SourcePriority.UNKNOWN);
      }
    }
    _removeFromPartsUsingMap(oldPartMap);
  }

  /**
   * In response to a change to Angular entry point [HtmlElement], invalidate any results that
   * depend on it.
   *
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * <b>Note:</b> Any cache entries that were accessed before this method was invoked must be
   * re-accessed after this method returns.
   *
   * @param entryCopy the [HtmlEntryImpl] of the (maybe) Angular entry point being invalidated
   */
  void _invalidateAngularResolution(HtmlEntryImpl entryCopy) {
    AngularApplication application = entryCopy.getValue(HtmlEntry.ANGULAR_ENTRY);
    if (application == null) {
      return;
    }
    _angularApplications.remove(application);
    // invalidate Entry
    entryCopy.setState(HtmlEntry.ANGULAR_ENTRY, CacheState.INVALID);
    // reset HTML sources
    List<AngularElement> oldAngularElements = application.elements;
    for (AngularElement angularElement in oldAngularElements) {
      if (angularElement is AngularHasTemplateElement) {
        AngularHasTemplateElement hasTemplate = angularElement;
        Source templateSource = hasTemplate.templateSource;
        if (templateSource != null) {
          HtmlEntry htmlEntry = _getReadableHtmlEntry(templateSource);
          HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
          htmlCopy.setValue(HtmlEntry.ANGULAR_APPLICATION, null);
          htmlCopy.setValue(HtmlEntry.ANGULAR_COMPONENT, null);
          htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
          _cache.put(templateSource, htmlCopy);
          _workManager.add(templateSource, SourcePriority.HTML);
        }
      }
    }
    // reset Dart sources
    List<Source> oldElementSources = application.elementSources;
    for (Source elementSource in oldElementSources) {
      DartEntry dartEntry = _getReadableDartEntry(elementSource);
      DartEntryImpl dartCopy = dartEntry.writableCopy;
      dartCopy.setValue(DartEntry.ANGULAR_ERRORS, AnalysisError.NO_ERRORS);
      _cache.put(elementSource, dartCopy);
      // notify about (disappeared) Angular errors
      ChangeNoticeImpl notice = _getNotice(elementSource);
      notice.setErrors(dartCopy.allErrors, dartEntry.getValue(SourceEntry.LINE_INFO));
    }
  }

  /**
   * In response to a change to at least one of the compilation units in the given library,
   * invalidate any results that are dependent on the result of resolving that library.
   *
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * <b>Note:</b> Any cache entries that were accessed before this method was invoked must be
   * re-accessed after this method returns.
   *
   * @param librarySource the source of the library being invalidated
   */
  void _invalidateLibraryResolution(Source librarySource) {
    // TODO(brianwilkerson) This could be optimized. There's no need to flush all of these entries
    // if the public namespace hasn't changed, which will be a fairly common case. The question is
    // whether we can afford the time to compute the namespace to look for differences.
    DartEntry libraryEntry = _getReadableDartEntry(librarySource);
    if (libraryEntry != null) {
      List<Source> includedParts = libraryEntry.getValue(DartEntry.INCLUDED_PARTS);
      DartEntryImpl libraryCopy = libraryEntry.writableCopy;
      libraryCopy.invalidateAllResolutionInformation();
      _cache.put(librarySource, libraryCopy);
      _workManager.add(librarySource, SourcePriority.LIBRARY);
      for (Source partSource in includedParts) {
        SourceEntry partEntry = _cache.get(partSource);
        if (partEntry is DartEntry) {
          DartEntryImpl partCopy = partEntry.writableCopy;
          partCopy.invalidateAllResolutionInformation();
          _cache.put(partSource, partCopy);
        }
      }
    }
    // invalidate Angular applications
    List<AngularApplication> angularApplicationsCopy = [];
    for (AngularApplication application in angularApplicationsCopy) {
      if (application.dependsOn(librarySource)) {
        Source entryPointSource = application.entryPoint;
        HtmlEntry entry = _getReadableHtmlEntry(entryPointSource);
        HtmlEntryImpl entryCopy = entry.writableCopy;
        _invalidateAngularResolution(entryCopy);
        _cache.put(entryPointSource, entryCopy);
        _workManager.add(entryPointSource, SourcePriority.HTML);
      }
    }
  }

  /**
   * Return `true` if this library is, or depends on, dart:html.
   *
   * @param library the library being tested
   * @param visitedLibraries a collection of the libraries that have been visited, used to prevent
   *          infinite recursion
   * @return `true` if this library is, or depends on, dart:html
   */
  bool _isClient(LibraryElement library, Source htmlSource, Set<LibraryElement> visitedLibraries) {
    if (visitedLibraries.contains(library)) {
      return false;
    }
    if (library.source == htmlSource) {
      return true;
    }
    visitedLibraries.add(library);
    for (LibraryElement imported in library.importedLibraries) {
      if (_isClient(imported, htmlSource, visitedLibraries)) {
        return true;
      }
    }
    for (LibraryElement exported in library.exportedLibraries) {
      if (_isClient(exported, htmlSource, visitedLibraries)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Log the given debugging information.
   *
   * @param message the message to be added to the log
   */
  void _logInformation(String message) {
    AnalysisEngine.instance.logger.logInformation(message);
  }

  /**
   * Log the given debugging information.
   *
   * @param message the message to be added to the log
   * @param exception the exception to be included in the log entry
   */
  void _logInformation2(String message, Exception exception) {
    if (exception == null) {
      AnalysisEngine.instance.logger.logInformation(message);
    } else {
      AnalysisEngine.instance.logger.logInformation2(message, exception);
    }
  }

  /**
   * Updates [HtmlEntry]s that correspond to the previously known and new Angular application
   * information.
   */
  void _recordAngularEntryPoint(HtmlEntryImpl entry, ResolveAngularEntryHtmlTask task) {
    AngularApplication application = task.application;
    if (application != null) {
      _angularApplications.add(application);
      // if this is an entry point, then we already resolved it
      entry.setValue(HtmlEntry.ANGULAR_ERRORS, task.entryErrors);
      // schedule HTML templates analysis
      List<AngularElement> newAngularElements = application.elements;
      for (AngularElement angularElement in newAngularElements) {
        if (angularElement is AngularHasTemplateElement) {
          AngularHasTemplateElement hasTemplate = angularElement;
          Source templateSource = hasTemplate.templateSource;
          if (templateSource != null) {
            HtmlEntry htmlEntry = _getReadableHtmlEntry(templateSource);
            HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
            htmlCopy.setValue(HtmlEntry.ANGULAR_APPLICATION, application);
            if (hasTemplate is AngularComponentElement) {
              AngularComponentElement component = hasTemplate;
              htmlCopy.setValue(HtmlEntry.ANGULAR_COMPONENT, component);
            }
            htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
            _cache.put(templateSource, htmlCopy);
            _workManager.add(templateSource, SourcePriority.HTML);
          }
        }
      }
      // update Dart sources errors
      List<Source> newElementSources = application.elementSources;
      for (Source elementSource in newElementSources) {
        DartEntry dartEntry = _getReadableDartEntry(elementSource);
        DartEntryImpl dartCopy = dartEntry.writableCopy;
        dartCopy.setValue(DartEntry.ANGULAR_ERRORS, task.getErrors(elementSource));
        _cache.put(elementSource, dartCopy);
        // notify about Dart errors
        ChangeNoticeImpl notice = _getNotice(elementSource);
        notice.setErrors(dartCopy.allErrors, computeLineInfo(elementSource));
      }
    }
    // remember Angular entry point
    entry.setValue(HtmlEntry.ANGULAR_ENTRY, application);
  }

  /**
   * Given a cache entry and a library element, record the library element and other information
   * gleaned from the element in the cache entry.
   *
   * @param dartEntry the original cache entry from which the copy was made
   * @param dartCopy the cache entry in which data is to be recorded
   * @param library the library element used to record information
   * @param librarySource the source for the library used to record information
   * @param htmlSource the source for the HTML library
   */
  void _recordElementData(DartEntry dartEntry, DartEntryImpl dartCopy, LibraryElement library, Source librarySource, Source htmlSource) {
    dartCopy.setValue(DartEntry.ELEMENT, library);
    dartCopy.setValue(DartEntry.IS_LAUNCHABLE, library.entryPoint != null);
    dartCopy.setValue(DartEntry.IS_CLIENT, _isClient(library, htmlSource, new Set<LibraryElement>()));
  }

  /**
   * Record the results produced by performing a [GenerateDartErrorsTask]. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  DartEntry _recordGenerateDartErrorsTask(GenerateDartErrorsTask task) {
    Source source = task.source;
    Source librarySource = task.libraryElement.source;
    AnalysisException thrownException = task.exception;
    DartEntry dartEntry = null;
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      throw new ObsoleteSourceAnalysisException(source);
    } else if (sourceEntry is! DartEntry) {
      // This shouldn't be possible because we should never have performed the task if the source
      // didn't represent a Dart file, but check to be safe.
      throw new AnalysisException.con1("Internal error: attempting to verify non-Dart file as a Dart file: ${source.fullName}");
    }
    dartEntry = sourceEntry as DartEntry;
    int sourceTime = getModificationStamp(source);
    int resultTime = task.modificationTime;
    if (sourceTime == resultTime) {
      if (dartEntry.modificationTime != sourceTime) {
        // The source has changed without the context being notified. Simulate notification.
        _sourceChanged(source);
        dartEntry = _getReadableDartEntry(source);
        if (dartEntry == null) {
          throw new AnalysisException.con1("A Dart file became a non-Dart file: ${source.fullName}");
        }
      }
      DartEntryImpl dartCopy = dartEntry.writableCopy;
      if (thrownException == null) {
        dartCopy.setValueInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource, task.errors);
        ChangeNoticeImpl notice = _getNotice(source);
        notice.setErrors(dartCopy.allErrors, dartCopy.getValue(SourceEntry.LINE_INFO));
      } else {
        dartCopy.setStateInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource, CacheState.ERROR);
      }
      dartCopy.exception = thrownException;
      _cache.put(source, dartCopy);
      dartEntry = dartCopy;
    } else {
      _logInformation2("Generated errors discarded for ${_debuggingString(source)}; sourceTime = ${sourceTime}, resultTime = ${resultTime}, cacheTime = ${dartEntry.modificationTime}", thrownException);
      DartEntryImpl dartCopy = dartEntry.writableCopy;
      if (thrownException == null || resultTime >= 0) {
        //
        // The analysis was performed on out-of-date sources. Mark the cache so that the source
        // will be re-verified using the up-to-date sources.
        //
        //          dartCopy.setState(DartEntry.VERIFICATION_ERRORS, librarySource, CacheState.INVALID);
        _removeFromParts(source, dartEntry);
        dartCopy.invalidateAllInformation();
        dartCopy.modificationTime = sourceTime;
        _cache.removedAst(source);
        _workManager.add(source, SourcePriority.UNKNOWN);
      } else {
        //
        // We could not determine whether the sources were up-to-date or out-of-date. Mark the
        // cache so that we won't attempt to re-verify the source until there's a good chance
        // that we'll be able to do so without error.
        //
        dartCopy.setStateInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource, CacheState.ERROR);
      }
      dartCopy.exception = thrownException;
      _cache.put(source, dartCopy);
      dartEntry = dartCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Record the results produced by performing a [GenerateDartHintsTask]. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  DartEntry _recordGenerateDartHintsTask(GenerateDartHintsTask task) {
    Source librarySource = task.libraryElement.source;
    AnalysisException thrownException = task.exception;
    DartEntry libraryEntry = null;
    Map<Source, TimestampedData<List<AnalysisError>>> hintMap = task.hintMap;
    if (hintMap == null) {
      // We don't have any information about which sources to mark as invalid other than the library
      // source.
      SourceEntry sourceEntry = _cache.get(librarySource);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(librarySource);
      } else if (sourceEntry is! DartEntry) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException.con1("Internal error: attempting to generate hints for non-Dart file as a Dart file: ${librarySource.fullName}");
      }
      if (thrownException == null) {
        thrownException = new AnalysisException.con1("GenerateDartHintsTask returned a null hint map without throwing an exception: ${librarySource.fullName}");
      }
      DartEntryImpl dartCopy = (sourceEntry as DartEntry).writableCopy;
      dartCopy.setStateInLibrary(DartEntry.HINTS, librarySource, CacheState.ERROR);
      dartCopy.exception = thrownException;
      _cache.put(librarySource, dartCopy);
      throw thrownException;
    }
    for (MapEntry<Source, TimestampedData<List<AnalysisError>>> entry in getMapEntrySet(hintMap)) {
      Source unitSource = entry.getKey();
      TimestampedData<List<AnalysisError>> results = entry.getValue();
      SourceEntry sourceEntry = _cache.get(unitSource);
      if (sourceEntry is! DartEntry) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException.con1("Internal error: attempting to parse non-Dart file as a Dart file: ${unitSource.fullName}");
      }
      DartEntry dartEntry = sourceEntry as DartEntry;
      if (unitSource == librarySource) {
        libraryEntry = dartEntry;
      }
      int sourceTime = getModificationStamp(unitSource);
      int resultTime = results.modificationTime;
      if (sourceTime == resultTime) {
        if (dartEntry.modificationTime != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          _sourceChanged(unitSource);
          dartEntry = _getReadableDartEntry(unitSource);
          if (dartEntry == null) {
            throw new AnalysisException.con1("A Dart file became a non-Dart file: ${unitSource.fullName}");
          }
        }
        DartEntryImpl dartCopy = dartEntry.writableCopy;
        if (thrownException == null) {
          dartCopy.setValueInLibrary(DartEntry.HINTS, librarySource, results.data);
          ChangeNoticeImpl notice = _getNotice(unitSource);
          notice.setErrors(dartCopy.allErrors, dartCopy.getValue(SourceEntry.LINE_INFO));
        } else {
          dartCopy.setStateInLibrary(DartEntry.HINTS, librarySource, CacheState.ERROR);
        }
        dartCopy.exception = thrownException;
        _cache.put(unitSource, dartCopy);
        dartEntry = dartCopy;
      } else {
        _logInformation2("Generated hints discarded for ${_debuggingString(unitSource)}; sourceTime = ${sourceTime}, resultTime = ${resultTime}, cacheTime = ${dartEntry.modificationTime}", thrownException);
        if (dartEntry.getStateInLibrary(DartEntry.HINTS, librarySource) == CacheState.IN_PROCESS) {
          DartEntryImpl dartCopy = dartEntry.writableCopy;
          if (thrownException == null || resultTime >= 0) {
            //
            // The analysis was performed on out-of-date sources. Mark the cache so that the sources
            // will be re-analyzed using the up-to-date sources.
            //
            //              dartCopy.setState(DartEntry.HINTS, librarySource, CacheState.INVALID);
            _removeFromParts(unitSource, dartEntry);
            dartCopy.invalidateAllInformation();
            dartCopy.modificationTime = sourceTime;
            _cache.removedAst(unitSource);
            _workManager.add(unitSource, SourcePriority.UNKNOWN);
          } else {
            //
            // We could not determine whether the sources were up-to-date or out-of-date. Mark the
            // cache so that we won't attempt to re-analyze the sources until there's a good chance
            // that we'll be able to do so without error.
            //
            dartCopy.setStateInLibrary(DartEntry.HINTS, librarySource, CacheState.ERROR);
          }
          dartCopy.exception = thrownException;
          _cache.put(unitSource, dartCopy);
          dartEntry = dartCopy;
        }
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return libraryEntry;
  }

  /**
   * Record the results produced by performing a [GetContentTask].
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  SourceEntry _recordGetContentsTask(GetContentTask task) {
    if (!task.isComplete) {
      return null;
    }
    Source source = task.source;
    AnalysisException thrownException = task.exception;
    SourceEntry sourceEntry = null;
    sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      throw new ObsoleteSourceAnalysisException(source);
    }
    SourceEntryImpl sourceCopy = sourceEntry.writableCopy;
    if (thrownException == null) {
      sourceCopy.modificationTime = task.modificationTime;
      sourceCopy.setValue(SourceEntry.CONTENT, task.content);
    } else {
      sourceCopy.exception = thrownException;
      sourceCopy.recordContentError();
      _workManager.remove(source);
    }
    _cache.put(source, sourceCopy);
    sourceEntry = sourceCopy;
    if (thrownException != null) {
      throw thrownException;
    }
    return sourceEntry;
  }

  /**
   * Record the results produced by performing a [IncrementalAnalysisTask].
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  DartEntry _recordIncrementalAnalysisTaskResults(IncrementalAnalysisTask task) {
    CompilationUnit unit = task.compilationUnit;
    if (unit != null) {
      ChangeNoticeImpl notice = _getNotice(task.source);
      notice.compilationUnit = unit;
      _incrementalAnalysisCache = IncrementalAnalysisCache.cacheResult(task.cache, unit);
    }
    return null;
  }

  /**
   * Record the results produced by performing a [ParseDartTask]. If the results were computed
   * from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  DartEntry _recordParseDartTaskResults(ParseDartTask task) {
    Source source = task.source;
    AnalysisException thrownException = task.exception;
    DartEntry dartEntry = null;
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      throw new ObsoleteSourceAnalysisException(source);
    } else if (sourceEntry is! DartEntry) {
      // This shouldn't be possible because we should never have performed the task if the source
      // didn't represent a Dart file, but check to be safe.
      throw new AnalysisException.con1("Internal error: attempting to parse non-Dart file as a Dart file: ${source.fullName}");
    }
    dartEntry = sourceEntry as DartEntry;
    int sourceTime = getModificationStamp(source);
    int resultTime = task.modificationTime;
    if (sourceTime == resultTime) {
      if (dartEntry.modificationTime != sourceTime) {
        // The source has changed without the context being notified. Simulate notification.
        _sourceChanged(source);
        dartEntry = _getReadableDartEntry(source);
        if (dartEntry == null) {
          throw new AnalysisException.con1("A Dart file became a non-Dart file: ${source.fullName}");
        }
      }
      _removeFromParts(source, dartEntry);
      DartEntryImpl dartCopy = dartEntry.writableCopy;
      if (thrownException == null) {
        if (task.hasNonPartOfDirective) {
          dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.LIBRARY);
          dartCopy.containingLibrary = source;
          _workManager.add(source, SourcePriority.LIBRARY);
        } else if (task.hasPartOfDirective) {
          dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.PART);
          dartCopy.removeContainingLibrary(source);
          _workManager.add(source, SourcePriority.NORMAL_PART);
        } else {
          // The file contains no directives.
          List<Source> containingLibraries = dartCopy.containingLibraries;
          if (containingLibraries.length > 1 || (containingLibraries.length == 1 && containingLibraries[0] != source)) {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.PART);
            dartCopy.removeContainingLibrary(source);
            _workManager.add(source, SourcePriority.NORMAL_PART);
          } else {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.LIBRARY);
            dartCopy.containingLibrary = source;
            _workManager.add(source, SourcePriority.LIBRARY);
          }
        }
        List<Source> newParts = task.includedSources;
        for (int i = 0; i < newParts.length; i++) {
          Source partSource = newParts[i];
          DartEntry partEntry = _getReadableDartEntry(partSource);
          if (partEntry != null && !identical(partEntry, dartEntry)) {
            DartEntryImpl partCopy = partEntry.writableCopy;
            // TODO(brianwilkerson) Change the kind of the "part" if it was marked as a library
            // and it has no directives.
            partCopy.addContainingLibrary(source);
            _cache.put(partSource, partCopy);
          }
        }
        dartCopy.setValue(DartEntry.PARSED_UNIT, task.compilationUnit);
        dartCopy.setValue(DartEntry.PARSE_ERRORS, task.errors);
        dartCopy.setValue(DartEntry.EXPORTED_LIBRARIES, task.exportedSources);
        dartCopy.setValue(DartEntry.IMPORTED_LIBRARIES, task.importedSources);
        dartCopy.setValue(DartEntry.INCLUDED_PARTS, newParts);
        _cache.storedAst(source);
        ChangeNoticeImpl notice = _getNotice(source);
        notice.setErrors(dartCopy.allErrors, task.lineInfo);
        // Verify that the incrementally parsed and resolved unit in the incremental cache
        // is structurally equivalent to the fully parsed unit
        _incrementalAnalysisCache = IncrementalAnalysisCache.verifyStructure(_incrementalAnalysisCache, source, task.compilationUnit);
      } else {
        _removeFromParts(source, dartEntry);
        dartCopy.recordParseError();
        _cache.removedAst(source);
      }
      dartCopy.exception = thrownException;
      _cache.put(source, dartCopy);
      dartEntry = dartCopy;
    } else {
      _logInformation2("Parse results discarded for ${_debuggingString(source)}; sourceTime = ${sourceTime}, resultTime = ${resultTime}, cacheTime = ${dartEntry.modificationTime}", thrownException);
      DartEntryImpl dartCopy = dartEntry.writableCopy;
      if (thrownException == null || resultTime >= 0) {
        //
        // The analysis was performed on out-of-date sources. Mark the cache so that the sources
        // will be re-analyzed using the up-to-date sources.
        //
        //          dartCopy.recordParseNotInProcess();
        _removeFromParts(source, dartEntry);
        dartCopy.invalidateAllInformation();
        dartCopy.modificationTime = sourceTime;
        _cache.removedAst(source);
        _workManager.add(source, SourcePriority.UNKNOWN);
      } else {
        //
        // We could not determine whether the sources were up-to-date or out-of-date. Mark the
        // cache so that we won't attempt to re-analyze the sources until there's a good chance
        // that we'll be able to do so without error.
        //
        dartCopy.recordParseError();
      }
      dartCopy.exception = thrownException;
      _cache.put(source, dartCopy);
      dartEntry = dartCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Record the results produced by performing a [ParseHtmlTask]. If the results were computed
   * from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  HtmlEntry _recordParseHtmlTaskResults(ParseHtmlTask task) {
    Source source = task.source;
    AnalysisException thrownException = task.exception;
    HtmlEntry htmlEntry = null;
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      throw new ObsoleteSourceAnalysisException(source);
    } else if (sourceEntry is! HtmlEntry) {
      // This shouldn't be possible because we should never have performed the task if the source
      // didn't represent an HTML file, but check to be safe.
      throw new AnalysisException.con1("Internal error: attempting to parse non-HTML file as a HTML file: ${source.fullName}");
    }
    htmlEntry = sourceEntry as HtmlEntry;
    int sourceTime = getModificationStamp(source);
    int resultTime = task.modificationTime;
    if (sourceTime == resultTime) {
      if (htmlEntry.modificationTime != sourceTime) {
        // The source has changed without the context being notified. Simulate notification.
        _sourceChanged(source);
        htmlEntry = _getReadableHtmlEntry(source);
        if (htmlEntry == null) {
          throw new AnalysisException.con1("An HTML file became a non-HTML file: ${source.fullName}");
        }
      }
      HtmlEntryImpl htmlCopy = (sourceEntry as HtmlEntry).writableCopy;
      if (thrownException == null) {
        LineInfo lineInfo = task.lineInfo;
        ht.HtmlUnit unit = task.htmlUnit;
        htmlCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
        htmlCopy.setValue(HtmlEntry.PARSED_UNIT, unit);
        htmlCopy.setValue(HtmlEntry.PARSE_ERRORS, task.errors);
        htmlCopy.setValue(HtmlEntry.REFERENCED_LIBRARIES, task.referencedLibraries);
        _cache.storedAst(source);
        ChangeNoticeImpl notice = _getNotice(source);
        notice.setErrors(htmlCopy.allErrors, lineInfo);
      } else {
        htmlCopy.recordParseError();
        _cache.removedAst(source);
      }
      htmlCopy.exception = thrownException;
      _cache.put(source, htmlCopy);
      htmlEntry = htmlCopy;
    } else {
      _logInformation2("Parse results discarded for ${_debuggingString(source)}; sourceTime = ${sourceTime}, resultTime = ${resultTime}, cacheTime = ${htmlEntry.modificationTime}", thrownException);
      HtmlEntryImpl htmlCopy = (sourceEntry as HtmlEntry).writableCopy;
      if (thrownException == null || resultTime >= 0) {
        //
        // The analysis was performed on out-of-date sources. Mark the cache so that the sources
        // will be re-analyzed using the up-to-date sources.
        //
        //          if (htmlCopy.getState(SourceEntry.LINE_INFO) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(SourceEntry.LINE_INFO, CacheState.INVALID);
        //          }
        //          if (htmlCopy.getState(HtmlEntry.PARSED_UNIT) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.INVALID);
        //          }
        //          if (htmlCopy.getState(HtmlEntry.REFERENCED_LIBRARIES) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.INVALID);
        //          }
        htmlCopy.invalidateAllInformation();
        htmlCopy.modificationTime = sourceTime;
        _cache.removedAst(source);
      } else {
        //
        // We could not determine whether the sources were up-to-date or out-of-date. Mark the
        // cache so that we won't attempt to re-analyze the sources until there's a good chance
        // that we'll be able to do so without error.
        //
        htmlCopy.setState(SourceEntry.LINE_INFO, CacheState.ERROR);
        htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.ERROR);
        htmlCopy.setState(HtmlEntry.RESOLVED_UNIT, CacheState.ERROR);
        htmlCopy.setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.ERROR);
      }
      htmlCopy.exception = thrownException;
      _cache.put(source, htmlCopy);
      htmlEntry = htmlCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a [ResolveAngularComponentTemplateTask]. If the
   * results were computed from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @throws AnalysisException if the results could not be recorded
   */
  HtmlEntry _recordResolveAngularComponentTemplateTaskResults(ResolveAngularComponentTemplateTask task) {
    Source source = task.source;
    AnalysisException thrownException = task.exception;
    HtmlEntry htmlEntry = null;
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      throw new ObsoleteSourceAnalysisException(source);
    } else if (sourceEntry is! HtmlEntry) {
      // This shouldn't be possible because we should never have performed the task if the source
      // didn't represent an HTML file, but check to be safe.
      throw new AnalysisException.con1("Internal error: attempting to resolve non-HTML file as an HTML file: ${source.fullName}");
    }
    htmlEntry = sourceEntry as HtmlEntry;
    int sourceTime = getModificationStamp(source);
    int resultTime = task.modificationTime;
    if (sourceTime == resultTime) {
      if (htmlEntry.modificationTime != sourceTime) {
        // The source has changed without the context being notified. Simulate notification.
        _sourceChanged(source);
        htmlEntry = _getReadableHtmlEntry(source);
        if (htmlEntry == null) {
          throw new AnalysisException.con1("An HTML file became a non-HTML file: ${source.fullName}");
        }
      }
      HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
      if (thrownException == null) {
        htmlCopy.setValue(HtmlEntry.ANGULAR_ERRORS, task.resolutionErrors);
        // notify about errors
        ChangeNoticeImpl notice = _getNotice(source);
        notice.htmlUnit = task.resolvedUnit;
        notice.setErrors(htmlCopy.allErrors, htmlCopy.getValue(SourceEntry.LINE_INFO));
      } else {
        htmlCopy.recordResolutionError();
      }
      htmlCopy.exception = thrownException;
      _cache.put(source, htmlCopy);
      htmlEntry = htmlCopy;
    } else {
      HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
      if (thrownException == null || resultTime >= 0) {
        //
        // The analysis was performed on out-of-date sources. Mark the cache so that the sources
        // will be re-analyzed using the up-to-date sources.
        //
        //          if (htmlCopy.getState(HtmlEntry.ANGULAR_ERRORS) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
        //          }
        //          if (htmlCopy.getState(HtmlEntry.ELEMENT) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.INVALID);
        //          }
        //          if (htmlCopy.getState(HtmlEntry.RESOLUTION_ERRORS) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.INVALID);
        //          }
        htmlCopy.invalidateAllInformation();
        htmlCopy.modificationTime = sourceTime;
        _cache.removedAst(source);
      } else {
        //
        // We could not determine whether the sources were up-to-date or out-of-date. Mark the
        // cache so that we won't attempt to re-analyze the sources until there's a good chance
        // that we'll be able to do so without error.
        //
        htmlCopy.recordResolutionError();
      }
      htmlCopy.exception = thrownException;
      _cache.put(source, htmlCopy);
      htmlEntry = htmlCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a [ResolveAngularEntryHtmlTask]. If the results
   * were computed from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @throws AnalysisException if the results could not be recorded
   */
  HtmlEntry _recordResolveAngularEntryHtmlTaskResults(ResolveAngularEntryHtmlTask task) {
    Source source = task.source;
    AnalysisException thrownException = task.exception;
    HtmlEntry htmlEntry = null;
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      throw new ObsoleteSourceAnalysisException(source);
    } else if (sourceEntry is! HtmlEntry) {
      // This shouldn't be possible because we should never have performed the task if the source
      // didn't represent an HTML file, but check to be safe.
      throw new AnalysisException.con1("Internal error: attempting to resolve non-HTML file as an HTML file: ${source.fullName}");
    }
    htmlEntry = sourceEntry as HtmlEntry;
    int sourceTime = getModificationStamp(source);
    int resultTime = task.modificationTime;
    if (sourceTime == resultTime) {
      if (htmlEntry.modificationTime != sourceTime) {
        // The source has changed without the context being notified. Simulate notification.
        _sourceChanged(source);
        htmlEntry = _getReadableHtmlEntry(source);
        if (htmlEntry == null) {
          throw new AnalysisException.con1("An HTML file became a non-HTML file: ${source.fullName}");
        }
      }
      HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
      if (thrownException == null) {
        htmlCopy.setValue(HtmlEntry.RESOLVED_UNIT, task.resolvedUnit);
        _recordAngularEntryPoint(htmlCopy, task);
        _cache.storedAst(source);
        ChangeNoticeImpl notice = _getNotice(source);
        notice.htmlUnit = task.resolvedUnit;
        notice.setErrors(htmlCopy.allErrors, htmlCopy.getValue(SourceEntry.LINE_INFO));
      } else {
        htmlCopy.recordResolutionError();
      }
      htmlCopy.exception = thrownException;
      _cache.put(source, htmlCopy);
      htmlEntry = htmlCopy;
    } else {
      HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
      if (thrownException == null || resultTime >= 0) {
        //
        // The analysis was performed on out-of-date sources. Mark the cache so that the sources
        // will be re-analyzed using the up-to-date sources.
        //
        //          if (htmlCopy.getState(HtmlEntry.ANGULAR_ERRORS) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
        //          }
        //          if (htmlCopy.getState(HtmlEntry.ELEMENT) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.INVALID);
        //          }
        //          if (htmlCopy.getState(HtmlEntry.RESOLUTION_ERRORS) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.INVALID);
        //          }
        htmlCopy.invalidateAllInformation();
        htmlCopy.modificationTime = sourceTime;
        _cache.removedAst(source);
      } else {
        //
        // We could not determine whether the sources were up-to-date or out-of-date. Mark the
        // cache so that we won't attempt to re-analyze the sources until there's a good chance
        // that we'll be able to do so without error.
        //
        htmlCopy.recordResolutionError();
      }
      htmlCopy.exception = thrownException;
      _cache.put(source, htmlCopy);
      htmlEntry = htmlCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a [ResolveDartUnitTask]. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  DartEntry _recordResolveDartUnitTaskResults(ResolveDartUnitTask task) {
    Source unitSource = task.source;
    Source librarySource = task.librarySource;
    AnalysisException thrownException = task.exception;
    DartEntry dartEntry = null;
    SourceEntry sourceEntry = _cache.get(unitSource);
    if (sourceEntry == null) {
      throw new ObsoleteSourceAnalysisException(unitSource);
    } else if (sourceEntry is! DartEntry) {
      // This shouldn't be possible because we should never have performed the task if the source
      // didn't represent a Dart file, but check to be safe.
      throw new AnalysisException.con1("Internal error: attempting to resolve non-Dart file as a Dart file: ${unitSource.fullName}");
    }
    dartEntry = sourceEntry as DartEntry;
    int sourceTime = getModificationStamp(unitSource);
    int resultTime = task.modificationTime;
    if (sourceTime == resultTime) {
      if (dartEntry.modificationTime != sourceTime) {
        // The source has changed without the context being notified. Simulate notification.
        _sourceChanged(unitSource);
        dartEntry = _getReadableDartEntry(unitSource);
        if (dartEntry == null) {
          throw new AnalysisException.con1("A Dart file became a non-Dart file: ${unitSource.fullName}");
        }
      }
      DartEntryImpl dartCopy = dartEntry.writableCopy;
      if (thrownException == null) {
        dartCopy.setValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource, task.resolvedUnit);
        _cache.storedAst(unitSource);
      } else {
        dartCopy.setStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource, CacheState.ERROR);
        _cache.removedAst(unitSource);
      }
      dartCopy.exception = thrownException;
      _cache.put(unitSource, dartCopy);
      dartEntry = dartCopy;
    } else {
      _logInformation2("Resolution results discarded for ${_debuggingString(unitSource)}; sourceTime = ${sourceTime}, resultTime = ${resultTime}, cacheTime = ${dartEntry.modificationTime}", thrownException);
      DartEntryImpl dartCopy = dartEntry.writableCopy;
      if (thrownException == null || resultTime >= 0) {
        //
        // The analysis was performed on out-of-date sources. Mark the cache so that the sources
        // will be re-analyzed using the up-to-date sources.
        //
        //          if (dartCopy.getState(DartEntry.RESOLVED_UNIT) == CacheState.IN_PROCESS) {
        //            dartCopy.setState(DartEntry.RESOLVED_UNIT, librarySource, CacheState.INVALID);
        //          }
        _removeFromParts(unitSource, dartEntry);
        dartCopy.invalidateAllInformation();
        dartCopy.modificationTime = sourceTime;
        _cache.removedAst(unitSource);
        _workManager.add(unitSource, SourcePriority.UNKNOWN);
      } else {
        //
        // We could not determine whether the sources were up-to-date or out-of-date. Mark the
        // cache so that we won't attempt to re-analyze the sources until there's a good chance
        // that we'll be able to do so without error.
        //
        dartCopy.setStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource, CacheState.ERROR);
      }
      dartCopy.exception = thrownException;
      _cache.put(unitSource, dartCopy);
      dartEntry = dartCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Record the results produced by performing a [ResolveHtmlTask]. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  HtmlEntry _recordResolveHtmlTaskResults(ResolveHtmlTask task) {
    Source source = task.source;
    AnalysisException thrownException = task.exception;
    HtmlEntry htmlEntry = null;
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      throw new ObsoleteSourceAnalysisException(source);
    } else if (sourceEntry is! HtmlEntry) {
      // This shouldn't be possible because we should never have performed the task if the source
      // didn't represent an HTML file, but check to be safe.
      throw new AnalysisException.con1("Internal error: attempting to resolve non-HTML file as an HTML file: ${source.fullName}");
    }
    htmlEntry = sourceEntry as HtmlEntry;
    int sourceTime = getModificationStamp(source);
    int resultTime = task.modificationTime;
    if (sourceTime == resultTime) {
      if (htmlEntry.modificationTime != sourceTime) {
        // The source has changed without the context being notified. Simulate notification.
        _sourceChanged(source);
        htmlEntry = _getReadableHtmlEntry(source);
        if (htmlEntry == null) {
          throw new AnalysisException.con1("An HTML file became a non-HTML file: ${source.fullName}");
        }
      }
      HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
      if (thrownException == null) {
        htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.FLUSHED);
        htmlCopy.setValue(HtmlEntry.RESOLVED_UNIT, task.resolvedUnit);
        htmlCopy.setValue(HtmlEntry.ELEMENT, task.element);
        htmlCopy.setValue(HtmlEntry.RESOLUTION_ERRORS, task.resolutionErrors);
        _cache.storedAst(source);
        ChangeNoticeImpl notice = _getNotice(source);
        notice.htmlUnit = task.resolvedUnit;
        notice.setErrors(htmlCopy.allErrors, htmlCopy.getValue(SourceEntry.LINE_INFO));
      } else {
        htmlCopy.recordResolutionError();
        _cache.removedAst(source);
      }
      htmlCopy.exception = thrownException;
      _cache.put(source, htmlCopy);
      htmlEntry = htmlCopy;
    } else {
      _logInformation2("Resolution results discarded for ${_debuggingString(source)}; sourceTime = ${sourceTime}, resultTime = ${resultTime}, cacheTime = ${htmlEntry.modificationTime}", thrownException);
      HtmlEntryImpl htmlCopy = htmlEntry.writableCopy;
      if (thrownException == null || resultTime >= 0) {
        //
        // The analysis was performed on out-of-date sources. Mark the cache so that the sources
        // will be re-analyzed using the up-to-date sources.
        //
        //          if (htmlCopy.getState(HtmlEntry.ELEMENT) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.INVALID);
        //          }
        //          if (htmlCopy.getState(HtmlEntry.RESOLUTION_ERRORS) == CacheState.IN_PROCESS) {
        //            htmlCopy.setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.INVALID);
        //          }
        htmlCopy.invalidateAllInformation();
        htmlCopy.modificationTime = sourceTime;
        _cache.removedAst(source);
      } else {
        //
        // We could not determine whether the sources were up-to-date or out-of-date. Mark the
        // cache so that we won't attempt to re-analyze the sources until there's a good chance
        // that we'll be able to do so without error.
        //
        htmlCopy.recordResolutionError();
      }
      htmlCopy.exception = thrownException;
      _cache.put(source, htmlCopy);
      htmlEntry = htmlCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a [ScanDartTask]. If the results were computed
   * from data that is now out-of-date, then the results will not be recorded.
   *
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  DartEntry _recordScanDartTaskResults(ScanDartTask task) {
    Source source = task.source;
    AnalysisException thrownException = task.exception;
    DartEntry dartEntry = null;
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      throw new ObsoleteSourceAnalysisException(source);
    } else if (sourceEntry is! DartEntry) {
      // This shouldn't be possible because we should never have performed the task if the source
      // didn't represent a Dart file, but check to be safe.
      throw new AnalysisException.con1("Internal error: attempting to parse non-Dart file as a Dart file: ${source.fullName}");
    }
    dartEntry = sourceEntry as DartEntry;
    int sourceTime = getModificationStamp(source);
    int resultTime = task.modificationTime;
    if (sourceTime == resultTime) {
      if (dartEntry.modificationTime != sourceTime) {
        // The source has changed without the context being notified. Simulate notification.
        _sourceChanged(source);
        dartEntry = _getReadableDartEntry(source);
        if (dartEntry == null) {
          throw new AnalysisException.con1("A Dart file became a non-Dart file: ${source.fullName}");
        }
      }
      DartEntryImpl dartCopy = dartEntry.writableCopy;
      if (thrownException == null) {
        LineInfo lineInfo = task.lineInfo;
        dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
        dartCopy.setValue(DartEntry.TOKEN_STREAM, task.tokenStream);
        dartCopy.setValue(DartEntry.SCAN_ERRORS, task.errors);
        _cache.storedAst(source);
        _workManager.add(source, SourcePriority.NORMAL_PART);
        ChangeNoticeImpl notice = _getNotice(source);
        notice.setErrors(dartEntry.allErrors, lineInfo);
      } else {
        _removeFromParts(source, dartEntry);
        dartCopy.recordScanError();
        _cache.removedAst(source);
      }
      dartCopy.exception = thrownException;
      _cache.put(source, dartCopy);
      dartEntry = dartCopy;
    } else {
      _logInformation2("Scan results discarded for ${_debuggingString(source)}; sourceTime = ${sourceTime}, resultTime = ${resultTime}, cacheTime = ${dartEntry.modificationTime}", thrownException);
      DartEntryImpl dartCopy = dartEntry.writableCopy;
      if (thrownException == null || resultTime >= 0) {
        //
        // The analysis was performed on out-of-date sources. Mark the cache so that the sources
        // will be re-analyzed using the up-to-date sources.
        //
        //          dartCopy.recordScanNotInProcess();
        _removeFromParts(source, dartEntry);
        dartCopy.invalidateAllInformation();
        dartCopy.modificationTime = sourceTime;
        _cache.removedAst(source);
        _workManager.add(source, SourcePriority.UNKNOWN);
      } else {
        //
        // We could not determine whether the sources were up-to-date or out-of-date. Mark the
        // cache so that we won't attempt to re-analyze the sources until there's a good chance
        // that we'll be able to do so without error.
        //
        dartCopy.recordScanError();
      }
      dartCopy.exception = thrownException;
      _cache.put(source, dartCopy);
      dartEntry = dartCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Remove the given library from the list of containing libraries for all of the parts referenced
   * by the given entry.
   *
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * @param librarySource the library to be removed
   * @param dartEntry the entry containing the list of included parts
   */
  void _removeFromParts(Source librarySource, DartEntry dartEntry) {
    List<Source> oldParts = dartEntry.getValue(DartEntry.INCLUDED_PARTS);
    for (int i = 0; i < oldParts.length; i++) {
      Source partSource = oldParts[i];
      DartEntry partEntry = _getReadableDartEntry(partSource);
      if (partEntry != null && !identical(partEntry, dartEntry)) {
        DartEntryImpl partCopy = partEntry.writableCopy;
        partCopy.removeContainingLibrary(librarySource);
        if (partCopy.containingLibraries.length == 0 && !exists(partSource)) {
          _cache.remove(partSource);
        } else {
          _cache.put(partSource, partCopy);
        }
      }
    }
  }

  /**
   * Remove the given libraries that are keys in the given map from the list of containing libraries
   * for each of the parts in the corresponding value.
   *
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * @param oldPartMap the table containing the parts associated with each library
   */
  void _removeFromPartsUsingMap(Map<Source, List<Source>> oldPartMap) {
    for (MapEntry<Source, List<Source>> entry in getMapEntrySet(oldPartMap)) {
      Source librarySource = entry.getKey();
      List<Source> oldParts = entry.getValue();
      for (int i = 0; i < oldParts.length; i++) {
        Source partSource = oldParts[i];
        if (partSource != librarySource) {
          DartEntry partEntry = _getReadableDartEntry(partSource);
          if (partEntry != null) {
            DartEntryImpl partCopy = partEntry.writableCopy;
            partCopy.removeContainingLibrary(librarySource);
            if (partCopy.containingLibraries.length == 0 && !exists(partSource)) {
              _cache.remove(partSource);
            } else {
              _cache.put(partSource, partCopy);
            }
          }
        }
      }
    }
  }

  /**
   * Remove the given source from the priority order if it is in the list.
   *
   * @param source the source to be removed
   */
  void _removeFromPriorityOrder(Source source) {
    int count = _priorityOrder.length;
    List<Source> newOrder = new List<Source>();
    for (int i = 0; i < count; i++) {
      if (_priorityOrder[i] != source) {
        newOrder.add(_priorityOrder[i]);
      }
    }
    if (newOrder.length < count) {
      analysisPriorityOrder = newOrder;
    }
  }

  /**
   * Create an entry for the newly added source. Return `true` if the new source is a Dart
   * file.
   *
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * @param source the source that has been added
   * @return `true` if the new source is a Dart file
   */
  bool _sourceAvailable(Source source) {
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null) {
      sourceEntry = _createSourceEntry(source, true);
    } else {
      SourceEntryImpl sourceCopy = sourceEntry.writableCopy;
      int newTime = getModificationStamp(source);
      sourceCopy.modificationTime = newTime;
      sourceCopy.explicitlyAdded = true;
      // TODO(brianwilkerson) Understand why we're not invalidating the cached data.
      _cache.put(source, sourceCopy);
    }
    if (sourceEntry is HtmlEntry) {
      _workManager.add(source, SourcePriority.HTML);
    } else {
      _workManager.add(source, SourcePriority.UNKNOWN);
    }
    return sourceEntry is DartEntry;
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * @param source the source that has been changed
   */
  void _sourceChanged(Source source) {
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry == null || sourceEntry.modificationTime == getModificationStamp(source)) {
      // Either we have removed this source, in which case we don't care that it is changed, or we
      // have already invalidated the cache and don't need to invalidate it again.
      return;
    }
    if (sourceEntry is HtmlEntry) {
      HtmlEntryImpl htmlCopy = sourceEntry.writableCopy;
      htmlCopy.modificationTime = getModificationStamp(source);
      _invalidateAngularResolution(htmlCopy);
      htmlCopy.invalidateAllInformation();
      _cache.put(source, htmlCopy);
      _cache.removedAst(source);
      _workManager.add(source, SourcePriority.HTML);
    } else if (sourceEntry is DartEntry) {
      List<Source> containingLibraries = getLibrariesContaining(source);
      Set<Source> librariesToInvalidate = new Set<Source>();
      for (Source containingLibrary in containingLibraries) {
        librariesToInvalidate.add(containingLibrary);
        for (Source dependentLibrary in getLibrariesDependingOn(containingLibrary)) {
          librariesToInvalidate.add(dependentLibrary);
        }
      }
      for (Source library in librariesToInvalidate) {
        //    for (Source library : containingLibraries) {
        _invalidateLibraryResolution(library);
      }
      _removeFromParts(source, _cache.get(source) as DartEntry);
      DartEntryImpl dartCopy = (_cache.get(source) as DartEntry).writableCopy;
      dartCopy.modificationTime = getModificationStamp(source);
      dartCopy.invalidateAllInformation();
      _cache.put(source, dartCopy);
      _cache.removedAst(source);
      _workManager.add(source, SourcePriority.UNKNOWN);
    }
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * @param source the source that has been deleted
   */
  void _sourceRemoved(Source source) {
    SourceEntry sourceEntry = _cache.get(source);
    if (sourceEntry is HtmlEntry) {
      HtmlEntryImpl htmlCopy = sourceEntry.writableCopy;
      _invalidateAngularResolution(htmlCopy);
    } else if (sourceEntry is DartEntry) {
      Set<Source> libraries = new Set<Source>();
      for (Source librarySource in getLibrariesContaining(source)) {
        libraries.add(librarySource);
        for (Source dependentLibrary in getLibrariesDependingOn(librarySource)) {
          libraries.add(dependentLibrary);
        }
      }
      for (Source librarySource in libraries) {
        _invalidateLibraryResolution(librarySource);
      }
    }
    _cache.remove(source);
    _workManager.remove(source);
    _removeFromPriorityOrder(source);
  }

  /**
   * Check the cache for any invalid entries (entries whose modification time does not match the
   * modification time of the source associated with the entry). Invalid entries will be marked as
   * invalid so that the source will be re-analyzed.
   *
   * <b>Note:</b> This method must only be invoked while we are synchronized on [cacheLock].
   *
   * @return `true` if at least one entry was invalid
   */
  bool _validateCacheConsistency() {
    int consistencyCheckStart = JavaSystem.nanoTime();
    List<Source> changedSources = new List<Source>();
    List<Source> missingSources = new List<Source>();
    for (MapEntry<Source, SourceEntry> entry in _cache.entrySet()) {
      Source source = entry.getKey();
      SourceEntry sourceEntry = entry.getValue();
      int sourceTime = getModificationStamp(source);
      if (sourceTime != sourceEntry.modificationTime) {
        changedSources.add(source);
      }
      if (sourceEntry.exception != null) {
        if (!exists(source)) {
          missingSources.add(source);
        }
      }
    }
    int count = changedSources.length;
    for (int i = 0; i < count; i++) {
      _sourceChanged(changedSources[i]);
    }
    int consistencyCheckEnd = JavaSystem.nanoTime();
    if (changedSources.length > 0 || missingSources.length > 0) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Consistency check took ");
      writer.print((consistencyCheckEnd - consistencyCheckStart) / 1000000.0);
      writer.println(" ms and found");
      writer.print("  ");
      writer.print(changedSources.length);
      writer.println(" inconsistent entries");
      writer.print("  ");
      writer.print(missingSources.length);
      writer.println(" missing sources");
      for (Source source in missingSources) {
        writer.print("    ");
        writer.println(source.fullName);
      }
      _logInformation(writer.toString());
    }
    return changedSources.length > 0;
  }
}

/**
 * Instances of the class `AnalysisTaskResultRecorder` are used by an analysis context to
 * record the results of a task.
 */
class AnalysisContextImpl_AnalysisTaskResultRecorder implements AnalysisTaskVisitor<SourceEntry> {
  final AnalysisContextImpl AnalysisContextImpl_this;

  AnalysisContextImpl_AnalysisTaskResultRecorder(this.AnalysisContextImpl_this);

  @override
  DartEntry visitGenerateDartErrorsTask(GenerateDartErrorsTask task) => AnalysisContextImpl_this._recordGenerateDartErrorsTask(task);

  @override
  DartEntry visitGenerateDartHintsTask(GenerateDartHintsTask task) => AnalysisContextImpl_this._recordGenerateDartHintsTask(task);

  @override
  SourceEntry visitGetContentTask(GetContentTask task) => AnalysisContextImpl_this._recordGetContentsTask(task);

  @override
  DartEntry visitIncrementalAnalysisTask(IncrementalAnalysisTask task) => AnalysisContextImpl_this._recordIncrementalAnalysisTaskResults(task);

  @override
  DartEntry visitParseDartTask(ParseDartTask task) => AnalysisContextImpl_this._recordParseDartTaskResults(task);

  @override
  HtmlEntry visitParseHtmlTask(ParseHtmlTask task) => AnalysisContextImpl_this._recordParseHtmlTaskResults(task);

  @override
  HtmlEntry visitResolveAngularComponentTemplateTask(ResolveAngularComponentTemplateTask task) => AnalysisContextImpl_this._recordResolveAngularComponentTemplateTaskResults(task);

  @override
  HtmlEntry visitResolveAngularEntryHtmlTask(ResolveAngularEntryHtmlTask task) => AnalysisContextImpl_this._recordResolveAngularEntryHtmlTaskResults(task);

  @override
  DartEntry visitResolveDartLibraryCycleTask(ResolveDartLibraryCycleTask task) => AnalysisContextImpl_this.recordResolveDartLibraryCycleTaskResults(task);

  @override
  DartEntry visitResolveDartLibraryTask(ResolveDartLibraryTask task) => AnalysisContextImpl_this.recordResolveDartLibraryTaskResults(task);

  @override
  DartEntry visitResolveDartUnitTask(ResolveDartUnitTask task) => AnalysisContextImpl_this._recordResolveDartUnitTaskResults(task);

  @override
  HtmlEntry visitResolveHtmlTask(ResolveHtmlTask task) => AnalysisContextImpl_this._recordResolveHtmlTaskResults(task);

  @override
  DartEntry visitScanDartTask(ScanDartTask task) => AnalysisContextImpl_this._recordScanDartTaskResults(task);
}

class AnalysisContextImpl_ContextRetentionPolicy implements CacheRetentionPolicy {
  final AnalysisContextImpl AnalysisContextImpl_this;

  AnalysisContextImpl_ContextRetentionPolicy(this.AnalysisContextImpl_this);

  @override
  RetentionPriority getAstPriority(Source source, SourceEntry sourceEntry) {
    int priorityCount = AnalysisContextImpl_this._priorityOrder.length;
    for (int i = 0; i < priorityCount; i++) {
      if (source == AnalysisContextImpl_this._priorityOrder[i]) {
        return RetentionPriority.HIGH;
      }
    }
    if (AnalysisContextImpl_this._neededForResolution != null && AnalysisContextImpl_this._neededForResolution.contains(source)) {
      return RetentionPriority.HIGH;
    }
    if (sourceEntry is DartEntry) {
      DartEntry dartEntry = sourceEntry;
      if (_astIsNeeded(dartEntry)) {
        return RetentionPriority.MEDIUM;
      }
    }
    return RetentionPriority.LOW;
  }

  bool _astIsNeeded(DartEntry dartEntry) => dartEntry.hasInvalidData(DartEntry.HINTS) || dartEntry.hasInvalidData(DartEntry.VERIFICATION_ERRORS) || dartEntry.hasInvalidData(DartEntry.RESOLUTION_ERRORS);
}

/**
 * Instances of the class `LibraryPair` hold a library and a list of the (source, entry)
 * pairs for compilation units in the library.
 */
class CycleBuilder_LibraryPair {
  /**
   * The library containing the compilation units.
   */
  ResolvableLibrary library;

  /**
   * The (source, entry) pairs representing the compilation units in the library.
   */
  List<CycleBuilder_SourceEntryPair> entryPairs;

  /**
   * Initialize a newly created pair.
   *
   * @param library the library containing the compilation units
   * @param entryPairs the (source, entry) pairs representing the compilation units in the
   *          library
   */
  CycleBuilder_LibraryPair(ResolvableLibrary library, List<CycleBuilder_SourceEntryPair> entryPairs) {
    this.library = library;
    this.entryPairs = entryPairs;
  }
}

/**
 * Instances of the class `SourceEntryPair` hold a source and the cache entry associated
 * with that source. They are used to reduce the number of times an entry must be looked up in
 * the [cache].
 */
class CycleBuilder_SourceEntryPair {
  /**
   * The source associated with the entry.
   */
  Source source;

  /**
   * The entry associated with the source.
   */
  DartEntry entry;

  /**
   * Initialize a newly created pair.
   *
   * @param source the source associated with the entry
   * @param entry the entry associated with the source
   */
  CycleBuilder_SourceEntryPair(Source source, DartEntry entry) {
    this.source = source;
    this.entry = entry;
  }
}

/**
 * Instances of the class `CycleBuilder` are used to construct a list of the libraries that
 * must be resolved together in order to resolve any one of the libraries.
 */
class AnalysisContextImpl_CycleBuilder {
  final AnalysisContextImpl AnalysisContextImpl_this;

  /**
   * A table mapping the sources of the defining compilation units of libraries to the
   * representation of the library that has the information needed to resolve the library.
   */
  Map<Source, ResolvableLibrary> _libraryMap = new Map<Source, ResolvableLibrary>();

  /**
   * The dependency graph used to compute the libraries in the cycle.
   */
  DirectedGraph<ResolvableLibrary> _dependencyGraph;

  /**
   * A list containing the libraries that are ready to be resolved.
   */
  List<ResolvableLibrary> _librariesInCycle;

  /**
   * The analysis task that needs to be performed before the cycle of libraries can be resolved,
   * or `null` if the libraries are ready to be resolved.
   */
  AnalysisContextImpl_TaskData _taskData;

  /**
   * Initialize a newly created cycle builder.
   */
  AnalysisContextImpl_CycleBuilder(this.AnalysisContextImpl_this) : super();

  /**
   * Compute a list of the libraries that need to be resolved together in order to resolve the
   * given library.
   *
   * @param librarySource the source of the library to be resolved
   * @throws AnalysisException if the core library cannot be found
   */
  void computeCycleContaining(Source librarySource) {
    //
    // Create the object representing the library being resolved.
    //
    ResolvableLibrary targetLibrary = _createLibrary(librarySource);
    //
    // Compute the set of libraries that need to be resolved together.
    //
    _dependencyGraph = new DirectedGraph<ResolvableLibrary>();
    _computeLibraryDependencies(targetLibrary);
    if (_taskData != null) {
      return;
    }
    _librariesInCycle = _dependencyGraph.findCycleContaining(targetLibrary);
    //
    // Ensure that all of the data needed to resolve them has been computed.
    //
    _ensureImportsAndExports();
    if (_taskData != null) {
      // At least one imported library needs to be resolved before the target library.
      AnalysisTask task = _taskData.task;
      if (task is ResolveDartLibraryTask) {
        AnalysisContextImpl_this._workManager.addFirst(task.librarySource, SourcePriority.LIBRARY);
      }
      return;
    }
    _computePartsInCycle(librarySource);
    if (_taskData != null) {
      // At least one part needs to be parsed.
      return;
    }
    // All of the AST's necessary to perform a resolution of the library cycle have been
    // gathered, so it is no longer necessary to retain them in the cache.
    AnalysisContextImpl_this._neededForResolution = null;
  }

  /**
   * Return a list containing the libraries that are ready to be resolved (assuming that
   * [getTaskData] returns `null`).
   *
   * @return the libraries that are ready to be resolved
   */
  List<ResolvableLibrary> get librariesInCycle => _librariesInCycle;

  /**
   * Return a representation of an analysis task that needs to be performed before the cycle of
   * libraries can be resolved, or `null` if the libraries are ready to be resolved.
   *
   * @return the analysis task that needs to be performed before the cycle of libraries can be
   *         resolved
   */
  AnalysisContextImpl_TaskData get taskData => _taskData;

  /**
   * Recursively traverse the libraries reachable from the given library, creating instances of
   * the class [Library] to represent them, and record the references in the library
   * objects.
   *
   * @param library the library to be processed to find libraries that have not yet been traversed
   * @throws AnalysisException if some portion of the library graph could not be traversed
   */
  void _computeLibraryDependencies(ResolvableLibrary library) {
    Source librarySource = library.librarySource;
    DartEntry dartEntry = AnalysisContextImpl_this._getReadableDartEntry(librarySource);
    List<Source> importedSources = _getSources(librarySource, dartEntry, DartEntry.IMPORTED_LIBRARIES);
    if (_taskData != null) {
      return;
    }
    List<Source> exportedSources = _getSources(librarySource, dartEntry, DartEntry.EXPORTED_LIBRARIES);
    if (_taskData != null) {
      return;
    }
    _computeLibraryDependenciesFromDirectives(library, importedSources, exportedSources);
  }

  /**
   * Recursively traverse the libraries reachable from the given library, creating instances of
   * the class [Library] to represent them, and record the references in the library
   * objects.
   *
   * @param library the library to be processed to find libraries that have not yet been traversed
   * @param importedSources an array containing the sources that are imported into the given
   *          library
   * @param exportedSources an array containing the sources that are exported from the given
   *          library
   */
  void _computeLibraryDependenciesFromDirectives(ResolvableLibrary library, List<Source> importedSources, List<Source> exportedSources) {
    int importCount = importedSources.length;
    if (importCount > 0) {
      List<ResolvableLibrary> importedLibraries = new List<ResolvableLibrary>();
      bool explicitlyImportsCore = false;
      for (int i = 0; i < importCount; i++) {
        Source importedSource = importedSources[i];
        if (importedSource == AnalysisContextImpl_this._coreLibrarySource) {
          explicitlyImportsCore = true;
        }
        ResolvableLibrary importedLibrary = _libraryMap[importedSource];
        if (importedLibrary == null) {
          importedLibrary = _createLibraryOrNull(importedSource);
          if (importedLibrary != null) {
            _computeLibraryDependencies(importedLibrary);
            if (_taskData != null) {
              return;
            }
          }
        }
        if (importedLibrary != null) {
          importedLibraries.add(importedLibrary);
          _dependencyGraph.addEdge(library, importedLibrary);
        }
      }
      library.explicitlyImportsCore = explicitlyImportsCore;
      if (!explicitlyImportsCore && AnalysisContextImpl_this._coreLibrarySource != library.librarySource) {
        ResolvableLibrary importedLibrary = _libraryMap[AnalysisContextImpl_this._coreLibrarySource];
        if (importedLibrary == null) {
          importedLibrary = _createLibraryOrNull(AnalysisContextImpl_this._coreLibrarySource);
          if (importedLibrary != null) {
            _computeLibraryDependencies(importedLibrary);
            if (_taskData != null) {
              return;
            }
          }
        }
        if (importedLibrary != null) {
          importedLibraries.add(importedLibrary);
          _dependencyGraph.addEdge(library, importedLibrary);
        }
      }
      library.importedLibraries = new List.from(importedLibraries);
    } else {
      library.explicitlyImportsCore = false;
      ResolvableLibrary importedLibrary = _libraryMap[AnalysisContextImpl_this._coreLibrarySource];
      if (importedLibrary == null) {
        importedLibrary = _createLibraryOrNull(AnalysisContextImpl_this._coreLibrarySource);
        if (importedLibrary != null) {
          _computeLibraryDependencies(importedLibrary);
          if (_taskData != null) {
            return;
          }
        }
      }
      if (importedLibrary != null) {
        _dependencyGraph.addEdge(library, importedLibrary);
        library.importedLibraries = <ResolvableLibrary> [importedLibrary];
      }
    }
    int exportCount = exportedSources.length;
    if (exportCount > 0) {
      List<ResolvableLibrary> exportedLibraries = new List<ResolvableLibrary>();
      for (int i = 0; i < exportCount; i++) {
        Source exportedSource = exportedSources[i];
        ResolvableLibrary exportedLibrary = _libraryMap[exportedSource];
        if (exportedLibrary == null) {
          exportedLibrary = _createLibraryOrNull(exportedSource);
          if (exportedLibrary != null) {
            _computeLibraryDependencies(exportedLibrary);
            if (_taskData != null) {
              return;
            }
          }
        }
        if (exportedLibrary != null) {
          exportedLibraries.add(exportedLibrary);
          _dependencyGraph.addEdge(library, exportedLibrary);
        }
      }
      library.exportedLibraries = new List.from(exportedLibraries);
    }
  }

  /**
   * Gather the resolvable AST structures for each of the compilation units in each of the
   * libraries in the cycle. This is done in two phases: first we ensure that we have cached an
   * AST structure for each compilation unit, then we gather them. We split the work this way
   * because getting the AST structures can change the state of the cache in such a way that we
   * would have more work to do if any compilation unit didn't have a resolvable AST structure.
   */
  void _computePartsInCycle(Source librarySource) {
    int count = _librariesInCycle.length;
    List<CycleBuilder_LibraryPair> libraryData = new List<CycleBuilder_LibraryPair>();
    for (int i = 0; i < count; i++) {
      ResolvableLibrary library = _librariesInCycle[i];
      libraryData.add(new CycleBuilder_LibraryPair(library, _ensurePartsInLibrary(library)));
    }
    AnalysisContextImpl_this._neededForResolution = _gatherSources(libraryData);
    if (AnalysisContextImpl._TRACE_PERFORM_TASK) {
      print("  preserve resolution data for ${AnalysisContextImpl_this._neededForResolution.length} sources while resolving ${librarySource.fullName}");
    }
    if (_taskData != null) {
      return;
    }
    for (int i = 0; i < count; i++) {
      _computePartsInLibrary(libraryData[i]);
    }
  }

  /**
   * Gather the resolvable compilation units for each of the compilation units in the specified
   * library.
   *
   * @param libraryPair a holder containing both the library and a list of (source, entry) pairs
   *          for all of the compilation units in the library
   */
  void _computePartsInLibrary(CycleBuilder_LibraryPair libraryPair) {
    ResolvableLibrary library = libraryPair.library;
    List<CycleBuilder_SourceEntryPair> entryPairs = libraryPair.entryPairs;
    int count = entryPairs.length;
    List<ResolvableCompilationUnit> units = new List<ResolvableCompilationUnit>(count);
    for (int i = 0; i < count; i++) {
      CycleBuilder_SourceEntryPair entryPair = entryPairs[i];
      Source source = entryPair.source;
      DartEntryImpl dartCopy = entryPair.entry.writableCopy;
      units[i] = new ResolvableCompilationUnit.con2(dartCopy.modificationTime, dartCopy.resolvableCompilationUnit, source);
      AnalysisContextImpl_this._cache.put(source, dartCopy);
    }
    library.resolvableCompilationUnits = units;
  }

  /**
   * Create an object to represent the information about the library defined by the compilation
   * unit with the given source.
   *
   * @param librarySource the source of the library's defining compilation unit
   * @return the library object that was created
   */
  ResolvableLibrary _createLibrary(Source librarySource) {
    ResolvableLibrary library = new ResolvableLibrary(librarySource);
    SourceEntry sourceEntry = AnalysisContextImpl_this._cache.get(librarySource);
    if (sourceEntry is DartEntry) {
      LibraryElementImpl libraryElement = sourceEntry.getValue(DartEntry.ELEMENT) as LibraryElementImpl;
      if (libraryElement != null) {
        library.libraryElement = libraryElement;
      }
    }
    _libraryMap[librarySource] = library;
    return library;
  }

  /**
   * Create an object to represent the information about the library defined by the compilation
   * unit with the given source.
   *
   * @param librarySource the source of the library's defining compilation unit
   * @return the library object that was created
   */
  ResolvableLibrary _createLibraryOrNull(Source librarySource) {
    if (!AnalysisContextImpl_this.exists(librarySource)) {
      return null;
    }
    ResolvableLibrary library = new ResolvableLibrary(librarySource);
    SourceEntry sourceEntry = AnalysisContextImpl_this._cache.get(librarySource);
    if (sourceEntry is DartEntry) {
      LibraryElementImpl libraryElement = sourceEntry.getValue(DartEntry.ELEMENT) as LibraryElementImpl;
      if (libraryElement != null) {
        library.libraryElement = libraryElement;
      }
    }
    _libraryMap[librarySource] = library;
    return library;
  }

  /**
   * Ensure that all of the libraries that are exported by the given library (but are not
   * themselves in the cycle) have element models built for them.
   *
   * @param library the library being tested
   */
  void _ensureExports(ResolvableLibrary library, Set<Source> visitedLibraries) {
    List<ResolvableLibrary> dependencies = library.exports;
    int dependencyCount = dependencies.length;
    for (int i = 0; i < dependencyCount; i++) {
      ResolvableLibrary dependency = dependencies[i];
      if (!_librariesInCycle.contains(dependency) && visitedLibraries.add(dependency.librarySource)) {
        if (dependency.libraryElement == null) {
          Source dependencySource = dependency.librarySource;
          AnalysisContextImpl_this._workManager.addFirst(dependencySource, SourcePriority.LIBRARY);
          if (_taskData == null) {
            _taskData = AnalysisContextImpl_this._createResolveDartLibraryTask(dependencySource, AnalysisContextImpl_this._getReadableDartEntry(dependencySource));
            return;
          }
        } else {
          _ensureExports(dependency, visitedLibraries);
          if (_taskData != null) {
            return;
          }
        }
      }
    }
  }

  /**
   * Ensure that all of the libraries that are exported by the given library (but are not
   * themselves in the cycle) have element models built for them.
   *
   * @param library the library being tested
   * @throws MissingDataException if there is at least one library being depended on that does not
   *           have an element model built for it
   */
  void _ensureImports(ResolvableLibrary library) {
    List<ResolvableLibrary> dependencies = library.imports;
    int dependencyCount = dependencies.length;
    for (int i = 0; i < dependencyCount; i++) {
      ResolvableLibrary dependency = dependencies[i];
      if (!_librariesInCycle.contains(dependency) && dependency.libraryElement == null) {
        Source dependencySource = dependency.librarySource;
        AnalysisContextImpl_this._workManager.addFirst(dependencySource, SourcePriority.LIBRARY);
        if (_taskData == null) {
          _taskData = AnalysisContextImpl_this._createResolveDartLibraryTask(dependencySource, AnalysisContextImpl_this._getReadableDartEntry(dependencySource));
          return;
        }
      }
    }
  }

  /**
   * Ensure that all of the libraries that are either imported or exported by libraries in the
   * cycle (but are not themselves in the cycle) have element models built for them.
   */
  void _ensureImportsAndExports() {
    Set<Source> visitedLibraries = new Set<Source>();
    int libraryCount = _librariesInCycle.length;
    for (int i = 0; i < libraryCount; i++) {
      ResolvableLibrary library = _librariesInCycle[i];
      _ensureImports(library);
      if (_taskData != null) {
        return;
      }
      _ensureExports(library, visitedLibraries);
      if (_taskData != null) {
        return;
      }
    }
  }

  /**
   * Ensure that there is a resolvable compilation unit available for all of the compilation units
   * in the given library.
   *
   * @param library the library for which resolvable compilation units must be available
   * @return a list of (source, entry) pairs for all of the compilation units in the library
   */
  List<CycleBuilder_SourceEntryPair> _ensurePartsInLibrary(ResolvableLibrary library) {
    List<CycleBuilder_SourceEntryPair> pairs = new List<CycleBuilder_SourceEntryPair>();
    Source librarySource = library.librarySource;
    DartEntry libraryEntry = AnalysisContextImpl_this._getReadableDartEntry(librarySource);
    _ensureResolvableCompilationUnit(librarySource, libraryEntry);
    pairs.add(new CycleBuilder_SourceEntryPair(librarySource, libraryEntry));
    List<Source> partSources = _getSources(librarySource, libraryEntry, DartEntry.INCLUDED_PARTS);
    int count = partSources.length;
    for (int i = 0; i < count; i++) {
      Source partSource = partSources[i];
      DartEntry partEntry = AnalysisContextImpl_this._getReadableDartEntry(partSource);
      if (partEntry != null && partEntry.getState(DartEntry.PARSED_UNIT) != CacheState.ERROR) {
        _ensureResolvableCompilationUnit(partSource, partEntry);
        pairs.add(new CycleBuilder_SourceEntryPair(partSource, partEntry));
      }
    }
    return pairs;
  }

  /**
   * Ensure that there is a resolvable compilation unit available for the given source.
   *
   * @param source the source for which a resolvable compilation unit must be available
   * @param dartEntry the entry associated with the source
   */
  void _ensureResolvableCompilationUnit(Source source, DartEntry dartEntry) {
    if (!dartEntry.hasResolvableCompilationUnit) {
      if (_taskData == null) {
        _taskData = AnalysisContextImpl_this._createParseDartTask(source, dartEntry);
      }
    }
  }

  Set<Source> _gatherSources(List<CycleBuilder_LibraryPair> libraryData) {
    int libraryCount = libraryData.length;
    Set<Source> sources = new Set<Source>();
    for (int i = 0; i < libraryCount; i++) {
      List<CycleBuilder_SourceEntryPair> entryPairs = libraryData[i].entryPairs;
      int entryCount = entryPairs.length;
      for (int j = 0; j < entryCount; j++) {
        sources.add(entryPairs[j].source);
      }
    }
    return sources;
  }

  /**
   * Return the sources described by the given descriptor.
   *
   * @param source the source with which the sources are associated
   * @param dartEntry the entry corresponding to the source
   * @param descriptor the descriptor indicating which sources are to be returned
   * @return the sources described by the given descriptor
   */
  List<Source> _getSources(Source source, DartEntry dartEntry, DataDescriptor<List<Source>> descriptor) {
    if (dartEntry == null) {
      return Source.EMPTY_ARRAY;
    }
    CacheState exportState = dartEntry.getState(descriptor);
    if (exportState == CacheState.ERROR) {
      return Source.EMPTY_ARRAY;
    } else if (exportState != CacheState.VALID) {
      if (_taskData == null) {
        _taskData = AnalysisContextImpl_this._createParseDartTask(source, dartEntry);
      }
      return Source.EMPTY_ARRAY;
    }
    return dartEntry.getValue(descriptor);
  }
}

/**
 * Instances of the class `TaskData` represent information about the next task to be
 * performed. Each data has an implicit associated source: the source that might need to be
 * analyzed. There are essentially three states that can be represented:
 * * If [getTask] returns a non-`null` value, then that is the task that should
 * be executed to further analyze the associated source.
 * * Otherwise, if [isBlocked] returns `true`, then there is no work that can be
 * done, but analysis for the associated source is not complete.
 * * Otherwise, [getDependentSource] should return a source that needs to be analyzed
 * before the analysis of the associated source can be completed.
 */
class AnalysisContextImpl_TaskData {
  /**
   * The task that is to be performed.
   */
  final AnalysisTask task;

  /**
   * A flag indicating whether the associated source is blocked waiting for its contents to be
   * loaded.
   */
  final bool _blocked;

  /**
   * Initialize a newly created data holder.
   *
   * @param task the task that is to be performed
   * @param blocked `true` if the associated source is blocked waiting for its contents to
   *          be loaded
   */
  AnalysisContextImpl_TaskData(this.task, this._blocked);

  /**
   * Return `true` if the associated source is blocked waiting for its contents to be
   * loaded.
   *
   * @return `true` if the associated source is blocked waiting for its contents to be
   *         loaded
   */
  bool get isBlocked => _blocked;

  @override
  String toString() {
    if (task == null) {
      return "blocked: ${_blocked}";
    }
    return task.toString();
  }
}

/**
 * Instances of the class `AnalysisErrorInfoImpl` represent the analysis errors and line info
 * associated with a source.
 */
class AnalysisErrorInfoImpl implements AnalysisErrorInfo {
  /**
   * The analysis errors associated with a source, or `null` if there are no errors.
   */
  final List<AnalysisError> errors;

  /**
   * The line information associated with the errors, or `null` if there are no errors.
   */
  final LineInfo lineInfo;

  /**
   * Initialize an newly created error info with the errors and line information
   *
   * @param errors the errors as a result of analysis
   * @param lineinfo the line info for the errors
   */
  AnalysisErrorInfoImpl(this.errors, this.lineInfo);
}

/**
 * Instances of the class `AnalysisOptions` represent a set of analysis options used to
 * control the behavior of an analysis context.
 */
class AnalysisOptionsImpl implements AnalysisOptions {
  /**
   * The maximum number of sources for which data should be kept in the cache.
   */
  static int DEFAULT_CACHE_SIZE = 64;

  /**
   * The maximum number of sources for which AST structures should be kept in the cache.
   */
  int cacheSize = DEFAULT_CACHE_SIZE;

  /**
   * A flag indicating whether analysis is to parse and analyze function bodies.
   */
  bool analyzeFunctionBodies = true;

  /**
   * A flag indicating whether analysis is to generate dart2js related hint results.
   */
  bool dart2jsHint = true;

  /**
   * A flag indicating whether errors, warnings and hints should be generated for sources in the
   * SDK.
   */
  bool _generateSdkErrors = false;

  /**
   * A flag indicating whether analysis is to generate hint results (e.g. type inference based
   * information and pub best practices).
   */
  bool hint = true;

  /**
   * A flag indicating whether incremental analysis should be used.
   */
  bool incremental = false;

  /**
   * A flag indicating whether analysis is to parse comments.
   */
  bool preserveComments = true;

  /**
   * A flag indicating whether analysis is to analyze Angular.
   */
  bool analyzeAngular = true;

  /**
   * Initialize a newly created set of analysis options to have their default values.
   */
  AnalysisOptionsImpl();

  /**
   * Initialize a newly created set of analysis options to have the same values as those in the
   * given set of analysis options.
   *
   * @param options the analysis options whose values are being copied
   */
  AnalysisOptionsImpl.con1(AnalysisOptions options) {
    cacheSize = options.cacheSize;
    dart2jsHint = options.dart2jsHint;
    hint = options.hint;
    incremental = options.incremental;
  }

  @override
  bool get generateSdkErrors => _generateSdkErrors;

  /**
   * Set whether errors, warnings and hints should be generated for sources in the SDK to match the
   * given value.
   *
   * @param generate `true` if errors, warnings and hints should be generated for sources in
   *          the SDK
   */
  void set generateSdkErrors(bool generate) {
    _generateSdkErrors = generate;
  }
}

/**
 * Instances of the class `ChangeNoticeImpl` represent a change to the analysis results
 * associated with a given source.
 */
class ChangeNoticeImpl implements ChangeNotice {
  /**
   * The source for which the result is being reported.
   */
  final Source source;

  /**
   * The fully resolved AST that changed as a result of the analysis, or `null` if the AST was
   * not changed.
   */
  CompilationUnit compilationUnit;

  /**
   * The fully resolved HTML that changed as a result of the analysis, or `null` if the HTML
   * was not changed.
   */
  ht.HtmlUnit htmlUnit;

  /**
   * The errors that changed as a result of the analysis, or `null` if errors were not
   * changed.
   */
  List<AnalysisError> _errors;

  /**
   * The line information associated with the source, or `null` if errors were not changed.
   */
  LineInfo _lineInfo;

  /**
   * An empty array of change notices.
   */
  static List<ChangeNoticeImpl> EMPTY_ARRAY = new List<ChangeNoticeImpl>(0);

  /**
   * Initialize a newly created notice associated with the given source.
   *
   * @param source the source for which the change is being reported
   */
  ChangeNoticeImpl(this.source);

  @override
  List<AnalysisError> get errors => _errors;

  @override
  LineInfo get lineInfo => _lineInfo;

  /**
   * Set the errors that changed as a result of the analysis to the given errors and set the line
   * information to the given line information.
   *
   * @param errors the errors that changed as a result of the analysis
   * @param lineInfo the line information associated with the source
   */
  void setErrors(List<AnalysisError> errors, LineInfo lineInfo) {
    this._errors = errors;
    this._lineInfo = lineInfo;
    if (lineInfo == null) {
      AnalysisEngine.instance.logger.logInformation2("No line info: ${source}", new JavaException());
    }
  }

  @override
  String toString() => "Changes for ${source.fullName}";
}

/**
 * Instances of the class `DelegatingAnalysisContextImpl` extend [AnalysisContextImpl
 ] to delegate sources to the appropriate analysis context. For instance, if the
 * source is in a system library then the analysis context from the [DartSdk] is used.
 */
class DelegatingAnalysisContextImpl extends AnalysisContextImpl {
  /**
   * This references the [InternalAnalysisContext] held onto by the [DartSdk] which is
   * used (instead of this [AnalysisContext]) for SDK sources. This field is set when
   * #setSourceFactory(SourceFactory) is called, and references the analysis context in the
   * [DartUriResolver] in the [SourceFactory], this analysis context assumes that there
   * will be such a resolver.
   */
  InternalAnalysisContext _sdkAnalysisContext;

  @override
  void addSourceInfo(Source source, SourceEntry info) {
    if (source.isInSystemLibrary) {
      _sdkAnalysisContext.addSourceInfo(source, info);
    } else {
      super.addSourceInfo(source, info);
    }
  }

  @override
  List<AnalysisError> computeErrors(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.computeErrors(source);
    } else {
      return super.computeErrors(source);
    }
  }

  @override
  List<Source> computeExportedLibraries(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.computeExportedLibraries(source);
    } else {
      return super.computeExportedLibraries(source);
    }
  }

  @override
  HtmlElement computeHtmlElement(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.computeHtmlElement(source);
    } else {
      return super.computeHtmlElement(source);
    }
  }

  @override
  List<Source> computeImportedLibraries(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.computeImportedLibraries(source);
    } else {
      return super.computeImportedLibraries(source);
    }
  }

  @override
  SourceKind computeKindOf(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.computeKindOf(source);
    } else {
      return super.computeKindOf(source);
    }
  }

  @override
  LibraryElement computeLibraryElement(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.computeLibraryElement(source);
    } else {
      return super.computeLibraryElement(source);
    }
  }

  @override
  LineInfo computeLineInfo(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.computeLineInfo(source);
    } else {
      return super.computeLineInfo(source);
    }
  }

  @override
  ResolvableCompilationUnit computeResolvableCompilationUnit(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.computeResolvableCompilationUnit(source);
    } else {
      return super.computeResolvableCompilationUnit(source);
    }
  }

  @override
  AnalysisErrorInfo getErrors(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.getErrors(source);
    } else {
      return super.getErrors(source);
    }
  }

  @override
  HtmlElement getHtmlElement(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.getHtmlElement(source);
    } else {
      return super.getHtmlElement(source);
    }
  }

  @override
  List<Source> getHtmlFilesReferencing(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.getHtmlFilesReferencing(source);
    } else {
      return super.getHtmlFilesReferencing(source);
    }
  }

  @override
  SourceKind getKindOf(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.getKindOf(source);
    } else {
      return super.getKindOf(source);
    }
  }

  @override
  List<Source> getLibrariesContaining(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.getLibrariesContaining(source);
    } else {
      return super.getLibrariesContaining(source);
    }
  }

  @override
  List<Source> getLibrariesDependingOn(Source librarySource) {
    if (librarySource.isInSystemLibrary) {
      return _sdkAnalysisContext.getLibrariesDependingOn(librarySource);
    } else {
      return super.getLibrariesDependingOn(librarySource);
    }
  }

  @override
  LibraryElement getLibraryElement(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.getLibraryElement(source);
    } else {
      return super.getLibraryElement(source);
    }
  }

  @override
  List<Source> get librarySources => ArrayUtils.addAll(super.librarySources, _sdkAnalysisContext.librarySources);

  @override
  LineInfo getLineInfo(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.getLineInfo(source);
    } else {
      return super.getLineInfo(source);
    }
  }

  @override
  Namespace getPublicNamespace(LibraryElement library) {
    Source source = library.source;
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.getPublicNamespace(library);
    } else {
      return super.getPublicNamespace(library);
    }
  }

  @override
  CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    if (unitSource.isInSystemLibrary) {
      return _sdkAnalysisContext.getResolvedCompilationUnit(unitSource, library);
    } else {
      return super.getResolvedCompilationUnit(unitSource, library);
    }
  }

  @override
  CompilationUnit getResolvedCompilationUnit2(Source unitSource, Source librarySource) {
    if (unitSource.isInSystemLibrary) {
      return _sdkAnalysisContext.getResolvedCompilationUnit2(unitSource, librarySource);
    } else {
      return super.getResolvedCompilationUnit2(unitSource, librarySource);
    }
  }

  @override
  bool isClientLibrary(Source librarySource) {
    if (librarySource.isInSystemLibrary) {
      return _sdkAnalysisContext.isClientLibrary(librarySource);
    } else {
      return super.isClientLibrary(librarySource);
    }
  }

  @override
  bool isServerLibrary(Source librarySource) {
    if (librarySource.isInSystemLibrary) {
      return _sdkAnalysisContext.isServerLibrary(librarySource);
    } else {
      return super.isServerLibrary(librarySource);
    }
  }

  @override
  CompilationUnit parseCompilationUnit(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.parseCompilationUnit(source);
    } else {
      return super.parseCompilationUnit(source);
    }
  }

  @override
  ht.HtmlUnit parseHtmlUnit(Source source) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.parseHtmlUnit(source);
    } else {
      return super.parseHtmlUnit(source);
    }
  }

  @override
  void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    if (elementMap.isEmpty) {
      return;
    }
    // TODO(jwren) we are making the assumption here that the elementMap will have sources from only
    // one library, while this is true with our use of the Analysis Engine, it is not required by
    // the API, revisit to fix cases where the elementMap can have sources both in the sdk and other
    // libraries
    Source source = new JavaIterator(elementMap.keys.toSet()).next();
    if (source.isInSystemLibrary) {
      _sdkAnalysisContext.recordLibraryElements(elementMap);
    } else {
      super.recordLibraryElements(elementMap);
    }
  }

  @override
  CompilationUnit resolveCompilationUnit(Source source, LibraryElement library) {
    if (source.isInSystemLibrary) {
      return _sdkAnalysisContext.resolveCompilationUnit(source, library);
    } else {
      return super.resolveCompilationUnit(source, library);
    }
  }

  @override
  CompilationUnit resolveCompilationUnit2(Source unitSource, Source librarySource) {
    if (unitSource.isInSystemLibrary) {
      return _sdkAnalysisContext.resolveCompilationUnit2(unitSource, librarySource);
    } else {
      return super.resolveCompilationUnit2(unitSource, librarySource);
    }
  }

  @override
  ht.HtmlUnit resolveHtmlUnit(Source unitSource) {
    if (unitSource.isInSystemLibrary) {
      return _sdkAnalysisContext.resolveHtmlUnit(unitSource);
    } else {
      return super.resolveHtmlUnit(unitSource);
    }
  }

  @override
  void setChangedContents(Source source, String contents, int offset, int oldLength, int newLength) {
    if (source.isInSystemLibrary) {
      _sdkAnalysisContext.setChangedContents(source, contents, offset, oldLength, newLength);
    } else {
      super.setChangedContents(source, contents, offset, oldLength, newLength);
    }
  }

  @override
  void setContents(Source source, String contents) {
    if (source.isInSystemLibrary) {
      _sdkAnalysisContext.setContents(source, contents);
    } else {
      super.setContents(source, contents);
    }
  }

  @override
  void set sourceFactory(SourceFactory factory) {
    super.sourceFactory = factory;
    DartSdk sdk = factory.dartSdk;
    if (sdk != null) {
      _sdkAnalysisContext = sdk.context as InternalAnalysisContext;
      if (_sdkAnalysisContext is DelegatingAnalysisContextImpl) {
        _sdkAnalysisContext = null;
        throw new IllegalStateException("The context provided by an SDK cannot itself be a delegating analysis context");
      }
    } else {
      throw new IllegalStateException("SourceFactorys provided to DelegatingAnalysisContextImpls must have a DartSdk associated with the provided SourceFactory.");
    }
  }
}

/**
 * Instances of the class `IncrementalAnalysisCache` hold information used to perform
 * incremental analysis.
 *
 * @see AnalysisContextImpl#setChangedContents(Source, String, int, int, int)
 */
class IncrementalAnalysisCache {
  /**
   * Determine if the incremental analysis result can be cached for the next incremental analysis.
   *
   * @param cache the prior incremental analysis cache
   * @param unit the incrementally updated compilation unit
   * @return the cache used for incremental analysis or `null` if incremental analysis results
   *         cannot be cached for the next incremental analysis
   */
  static IncrementalAnalysisCache cacheResult(IncrementalAnalysisCache cache, CompilationUnit unit) {
    if (cache != null && unit != null) {
      return new IncrementalAnalysisCache(cache.librarySource, cache.source, unit, cache._newContents, cache._newContents, 0, 0, 0);
    }
    return null;
  }

  /**
   * Determine if the cache should be cleared.
   *
   * @param cache the prior cache or `null` if none
   * @param source the source being updated (not `null`)
   * @return the cache used for incremental analysis or `null` if incremental analysis cannot
   *         be performed
   */
  static IncrementalAnalysisCache clear(IncrementalAnalysisCache cache, Source source) {
    if (cache == null || cache.source == source) {
      return null;
    }
    return cache;
  }

  /**
   * Determine if incremental analysis can be performed from the given information.
   *
   * @param cache the prior cache or `null` if none
   * @param source the source being updated (not `null`)
   * @param oldContents the original source contents prior to this update (may be `null`)
   * @param newContents the new contents after this incremental change (not `null`)
   * @param offset the offset at which the change occurred
   * @param oldLength the length of the text being replaced
   * @param newLength the length of the replacement text
   * @param sourceEntry the cached entry for the given source or `null` if none
   * @return the cache used for incremental analysis or `null` if incremental analysis cannot
   *         be performed
   */
  static IncrementalAnalysisCache update(IncrementalAnalysisCache cache, Source source, String oldContents, String newContents, int offset, int oldLength, int newLength, SourceEntry sourceEntry) {
    // Determine the cache resolved unit
    Source librarySource = null;
    CompilationUnit unit = null;
    if (sourceEntry is DartEntryImpl) {
      DartEntryImpl dartEntry = sourceEntry;
      List<Source> librarySources = dartEntry.librariesContaining;
      if (librarySources.length == 1) {
        librarySource = librarySources[0];
        if (librarySource != null) {
          unit = dartEntry.getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
        }
      }
    }
    // Create a new cache if there is not an existing cache or the source is different
    // or a new resolved compilation unit is available
    if (cache == null || cache.source != source || unit != null) {
      if (unit == null) {
        return null;
      }
      if (oldContents == null) {
        if (oldLength != 0) {
          return null;
        }
        oldContents = "${newContents.substring(0, offset)}${newContents.substring(offset + newLength)}";
      }
      return new IncrementalAnalysisCache(librarySource, source, unit, oldContents, newContents, offset, oldLength, newLength);
    }
    // Update the existing cache if the change is contiguous
    if (cache._oldLength == 0 && cache._newLength == 0) {
      cache._offset = offset;
      cache._oldLength = oldLength;
      cache._newLength = newLength;
    } else {
      if (cache._offset > offset || offset > cache._offset + cache._newLength) {
        return null;
      }
      cache._newLength += newLength - oldLength;
    }
    cache._newContents = newContents;
    return cache;
  }

  /**
   * Verify that the incrementally parsed and resolved unit in the incremental cache is structurally
   * equivalent to the fully parsed unit.
   *
   * @param cache the prior cache or `null` if none
   * @param source the source of the compilation unit that was parsed (not `null`)
   * @param unit the compilation unit that was just parsed
   * @return the cache used for incremental analysis or `null` if incremental analysis results
   *         cannot be cached for the next incremental analysis
   */
  static IncrementalAnalysisCache verifyStructure(IncrementalAnalysisCache cache, Source source, CompilationUnit unit) {
    if (cache != null && unit != null && cache.source == source) {
      if (!AstComparator.equalUnits(cache.resolvedUnit, unit)) {
        return null;
      }
    }
    return cache;
  }

  final Source librarySource;

  final Source source;

  final String oldContents;

  final CompilationUnit resolvedUnit;

  String _newContents;

  int _offset = 0;

  int _oldLength = 0;

  int _newLength = 0;

  IncrementalAnalysisCache(this.librarySource, this.source, this.resolvedUnit, this.oldContents, String newContents, int offset, int oldLength, int newLength) {
    this._newContents = newContents;
    this._offset = offset;
    this._oldLength = oldLength;
    this._newLength = newLength;
  }

  /**
   * Return the current contents for the receiver's source.
   *
   * @return the contents (not `null`)
   */
  String get newContents => _newContents;

  /**
   * Return the number of characters in the replacement text.
   *
   * @return the replacement length (zero or greater)
   */
  int get newLength => _newLength;

  /**
   * Return the character position of the first changed character.
   *
   * @return the offset (zero or greater)
   */
  int get offset => _offset;

  /**
   * Return the number of characters that were replaced.
   *
   * @return the replaced length (zero or greater)
   */
  int get oldLength => _oldLength;

  /**
   * Determine if the cache contains source changes that need to be analyzed
   *
   * @return `true` if the cache contains changes to be analyzed, else `false`
   */
  bool get hasWork => _oldLength > 0 || _newLength > 0;
}

/**
 * Instances of the class `InstrumentedAnalysisContextImpl` implement an
 * [AnalysisContext] by recording instrumentation data and delegating to
 * another analysis context to do the non-instrumentation work.
 */
class InstrumentedAnalysisContextImpl implements InternalAnalysisContext {
  /**
   * If the current thread is the UI thread, then note this in the specified instrumentation and
   * append this information to the log.
   *
   * @param instrumentation the instrumentation, not `null`
   */
  static void _checkThread(InstrumentationBuilder instrumentation) {
  }

  /**
   * Record an exception that was thrown during analysis.
   *
   * @param instrumentation the instrumentation builder being used to record the exception
   * @param exception the exception being reported
   */
  static void _recordAnalysisException(InstrumentationBuilder instrumentation, AnalysisException exception) {
    instrumentation.record(exception);
  }

  /**
   * The unique identifier used to identify this analysis context in the instrumentation data.
   */
  String _contextId = UUID.randomUUID().toString();

  /**
   * The analysis context to which all of the non-instrumentation work is delegated.
   */
  InternalAnalysisContext _basis;

  /**
   * Create a new [InstrumentedAnalysisContextImpl] which wraps a new
   * [AnalysisContextImpl] as the basis context.
   */
  InstrumentedAnalysisContextImpl() : this.con1(new DelegatingAnalysisContextImpl());

  /**
   * Create a new [InstrumentedAnalysisContextImpl] with a specified basis context, aka the
   * context to wrap and instrument.
   *
   * @param context some [InstrumentedAnalysisContext] to wrap and instrument
   */
  InstrumentedAnalysisContextImpl.con1(InternalAnalysisContext context) {
    _basis = context;
  }

  @override
  void addSourceInfo(Source source, SourceEntry info) {
    _basis.addSourceInfo(source, info);
  }

  @override
  void applyChanges(ChangeSet changeSet) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-applyChanges");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      _basis.applyChanges(changeSet);
    } finally {
      instrumentation.log();
    }
  }

  @override
  String computeDocumentationComment(Element element) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-computeDocumentationComment");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.computeDocumentationComment(element);
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<AnalysisError> computeErrors(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-computeErrors");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      List<AnalysisError> errors = _basis.computeErrors(source);
      instrumentation.metric2("Errors-count", errors.length);
      return errors;
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<Source> computeExportedLibraries(Source source) => _basis.computeExportedLibraries(source);

  @override
  HtmlElement computeHtmlElement(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-computeHtmlElement");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.computeHtmlElement(source);
    } on AnalysisException catch (e) {
      _recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<Source> computeImportedLibraries(Source source) => _basis.computeImportedLibraries(source);

  @override
  SourceKind computeKindOf(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-computeKindOf");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.computeKindOf(source);
    } finally {
      instrumentation.log();
    }
  }

  @override
  LibraryElement computeLibraryElement(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-computeLibraryElement");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.computeLibraryElement(source);
    } on AnalysisException catch (e) {
      _recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @override
  LineInfo computeLineInfo(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-computeLineInfo");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.computeLineInfo(source);
    } on AnalysisException catch (e) {
      _recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @override
  ResolvableCompilationUnit computeResolvableCompilationUnit(Source source) => _basis.computeResolvableCompilationUnit(source);

  @override
  void dispose() {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-dispose");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      _basis.dispose();
    } finally {
      instrumentation.log();
    }
  }

  @override
  bool exists(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-exists");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.exists(source);
    } finally {
      instrumentation.log();
    }
  }

  @override
  AnalysisContext extractContext(SourceContainer container) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-extractContext");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      InstrumentedAnalysisContextImpl newContext = new InstrumentedAnalysisContextImpl();
      _basis.extractContextInto(container, newContext._basis);
      return newContext;
    } finally {
      instrumentation.log();
    }
  }

  @override
  InternalAnalysisContext extractContextInto(SourceContainer container, InternalAnalysisContext newContext) => _basis.extractContextInto(container, newContext);

  @override
  AnalysisOptions get analysisOptions {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getAnalysisOptions");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.analysisOptions;
    } finally {
      instrumentation.log();
    }
  }

  @override
  AngularApplication getAngularApplicationWithHtml(Source htmlSource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getAngularApplication");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getAngularApplicationWithHtml(htmlSource);
    } finally {
      instrumentation.log();
    }
  }

  /**
   * @return the underlying [AnalysisContext].
   */
  AnalysisContext get basis => _basis;

  @override
  CompilationUnitElement getCompilationUnitElement(Source unitSource, Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getCompilationUnitElement");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getCompilationUnitElement(unitSource, librarySource);
    } finally {
      instrumentation.log();
    }
  }

  @override
  TimestampedData<String> getContents(Source source) => _basis.getContents(source);

  @override
  Element getElement(ElementLocation location) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getElement");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getElement(location);
    } finally {
      instrumentation.log();
    }
  }

  @override
  AnalysisErrorInfo getErrors(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getErrors");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      AnalysisErrorInfo ret = _basis.getErrors(source);
      if (ret != null) {
        instrumentation.metric2("Errors-count", ret.errors.length);
      }
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @override
  HtmlElement getHtmlElement(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getHtmlElement");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getHtmlElement(source);
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<Source> getHtmlFilesReferencing(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getHtmlFilesReferencing");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      List<Source> ret = _basis.getHtmlFilesReferencing(source);
      if (ret != null) {
        instrumentation.metric2("Source-count", ret.length);
      }
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<Source> get htmlSources {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getHtmlSources");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      List<Source> ret = _basis.htmlSources;
      if (ret != null) {
        instrumentation.metric2("Source-count", ret.length);
      }
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @override
  SourceKind getKindOf(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getKindOf");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getKindOf(source);
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<Source> get launchableClientLibrarySources {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getLaunchableClientLibrarySources");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      List<Source> ret = _basis.launchableClientLibrarySources;
      if (ret != null) {
        instrumentation.metric2("Source-count", ret.length);
      }
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<Source> get launchableServerLibrarySources {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getLaunchableServerLibrarySources");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      List<Source> ret = _basis.launchableServerLibrarySources;
      if (ret != null) {
        instrumentation.metric2("Source-count", ret.length);
      }
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<Source> getLibrariesContaining(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getLibrariesContaining");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      List<Source> ret = _basis.getLibrariesContaining(source);
      if (ret != null) {
        instrumentation.metric2("Source-count", ret.length);
      }
      return ret;
    } finally {
      instrumentation.log2(2);
    }
  }

  @override
  List<Source> getLibrariesDependingOn(Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getLibrariesDependingOn");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      List<Source> ret = _basis.getLibrariesDependingOn(librarySource);
      if (ret != null) {
        instrumentation.metric2("Source-count", ret.length);
      }
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<Source> getLibrariesReferencedFromHtml(Source htmlSource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getLibrariesReferencedFromHtml");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getLibrariesReferencedFromHtml(htmlSource);
    } finally {
      instrumentation.log();
    }
  }

  @override
  LibraryElement getLibraryElement(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getLibraryElement");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getLibraryElement(source);
    } finally {
      instrumentation.log();
    }
  }

  @override
  List<Source> get librarySources {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getLibrarySources");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      List<Source> ret = _basis.librarySources;
      if (ret != null) {
        instrumentation.metric2("Source-count", ret.length);
      }
      return ret;
    } finally {
      instrumentation.log();
    }
  }

  @override
  LineInfo getLineInfo(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getLineInfo");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getLineInfo(source);
    } finally {
      instrumentation.log();
    }
  }

  @override
  int getModificationStamp(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getModificationStamp");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getModificationStamp(source);
    } finally {
      instrumentation.log();
    }
  }

  @override
  Namespace getPublicNamespace(LibraryElement library) => _basis.getPublicNamespace(library);

  @override
  List<Source> get refactoringUnsafeSources {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getRefactoringUnsafeSources");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.refactoringUnsafeSources;
    } finally {
      instrumentation.log();
    }
  }

  @override
  CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getResolvedCompilationUnit");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getResolvedCompilationUnit(unitSource, library);
    } finally {
      instrumentation.log();
    }
  }

  @override
  CompilationUnit getResolvedCompilationUnit2(Source unitSource, Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getResolvedCompilationUnit");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getResolvedCompilationUnit2(unitSource, librarySource);
    } finally {
      instrumentation.log2(2);
    }
  }

  @override
  ht.HtmlUnit getResolvedHtmlUnit(Source htmlSource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getResolvedHtmlUnit");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.getResolvedHtmlUnit(htmlSource);
    } finally {
      instrumentation.log2(2);
    }
  }

  @override
  SourceFactory get sourceFactory {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-getSourceFactory");
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.sourceFactory;
    } finally {
      instrumentation.log2(2);
    }
  }

  @override
  AnalysisContentStatistics get statistics => _basis.statistics;

  @override
  TypeProvider get typeProvider => _basis.typeProvider;

  @override
  bool isClientLibrary(Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-isClientLibrary");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.isClientLibrary(librarySource);
    } finally {
      instrumentation.log();
    }
  }

  @override
  bool get isDisposed {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-isDisposed");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.isDisposed;
    } finally {
      instrumentation.log();
    }
  }

  @override
  bool isServerLibrary(Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-isServerLibrary");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.isServerLibrary(librarySource);
    } finally {
      instrumentation.log();
    }
  }

  @override
  void mergeContext(AnalysisContext context) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-mergeContext");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      if (context is InstrumentedAnalysisContextImpl) {
        context = (context as InstrumentedAnalysisContextImpl)._basis;
      }
      _basis.mergeContext(context);
    } finally {
      instrumentation.log();
    }
  }

  @override
  CompilationUnit parseCompilationUnit(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-parseCompilationUnit");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.parseCompilationUnit(source);
    } on AnalysisException catch (e) {
      _recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @override
  ht.HtmlUnit parseHtmlUnit(Source source) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-parseHtmlUnit");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.parseHtmlUnit(source);
    } on AnalysisException catch (e) {
      _recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @override
  AnalysisResult performAnalysisTask() {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-performAnalysisTask");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      AnalysisResult result = _basis.performAnalysisTask();
      if (result.changeNotices != null) {
        instrumentation.metric2("ChangeNotice-count", result.changeNotices.length);
      }
      return result;
    } finally {
      instrumentation.log2(2);
    }
  }

  @override
  void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    _basis.recordLibraryElements(elementMap);
  }

  @override
  CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-resolveCompilationUnit");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.resolveCompilationUnit(unitSource, library);
    } on AnalysisException catch (e) {
      _recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @override
  CompilationUnit resolveCompilationUnit2(Source unitSource, Source librarySource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-resolveCompilationUnit");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.resolveCompilationUnit2(unitSource, librarySource);
    } on AnalysisException catch (e) {
      _recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @override
  ht.HtmlUnit resolveHtmlUnit(Source htmlSource) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-resolveHtmlUnit");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      return _basis.resolveHtmlUnit(htmlSource);
    } on AnalysisException catch (e) {
      _recordAnalysisException(instrumentation, e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  @override
  void set analysisOptions(AnalysisOptions options) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-setAnalysisOptions");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      _basis.analysisOptions = options;
    } finally {
      instrumentation.log();
    }
  }

  @override
  void set analysisPriorityOrder(List<Source> sources) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-setAnalysisPriorityOrder");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      _basis.analysisPriorityOrder = sources;
    } finally {
      instrumentation.log();
    }
  }

  @override
  void setChangedContents(Source source, String contents, int offset, int oldLength, int newLength) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-setChangedContents");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      _basis.setChangedContents(source, contents, offset, oldLength, newLength);
    } finally {
      instrumentation.log();
    }
  }

  @override
  void setContents(Source source, String contents) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-setContents");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      _basis.setContents(source, contents);
    } finally {
      instrumentation.log();
    }
  }

  @override
  void set sourceFactory(SourceFactory factory) {
    InstrumentationBuilder instrumentation = Instrumentation.builder2("Analysis-setSourceFactory");
    _checkThread(instrumentation);
    try {
      instrumentation.metric3("contextId", _contextId);
      _basis.sourceFactory = factory;
    } finally {
      instrumentation.log();
    }
  }
}

/**
 * The interface `InternalAnalysisContext` defines additional behavior for an analysis context
 * that is required by internal users of the context.
 */
abstract class InternalAnalysisContext implements AnalysisContext {
  /**
   * Add the given source with the given information to this context.
   *
   * @param source the source to be added
   * @param info the information about the source
   */
  void addSourceInfo(Source source, SourceEntry info);

  /**
   * Return an array containing the sources of the libraries that are exported by the library with
   * the given source. The array will be empty if the given source is invalid, if the given source
   * does not represent a library, or if the library does not export any other libraries.
   *
   * @param source the source representing the library whose exports are to be returned
   * @return the sources of the libraries that are exported by the given library
   * @throws AnalysisException if the exported libraries could not be computed
   */
  List<Source> computeExportedLibraries(Source source);

  /**
   * Return an array containing the sources of the libraries that are imported by the library with
   * the given source. The array will be empty if the given source is invalid, if the given source
   * does not represent a library, or if the library does not import any other libraries.
   *
   * @param source the source representing the library whose imports are to be returned
   * @return the sources of the libraries that are imported by the given library
   * @throws AnalysisException if the imported libraries could not be computed
   */
  List<Source> computeImportedLibraries(Source source);

  /**
   * Return an AST structure corresponding to the given source, but ensure that the structure has
   * not already been resolved and will not be resolved by any other threads or in any other
   * library.
   *
   * <b>Note:</b> This method cannot be used in an async environment
   *
   * @param source the compilation unit for which an AST structure should be returned
   * @return the AST structure representing the content of the source
   * @throws AnalysisException if the analysis could not be performed
   */
  ResolvableCompilationUnit computeResolvableCompilationUnit(Source source);

  /**
   * Initialize the specified context by removing the specified sources from the receiver and adding
   * them to the specified context.
   *
   * @param container the container containing sources that should be removed from this context and
   *          added to the returned context
   * @param newContext the context to be initialized
   * @return the analysis context that was initialized
   */
  InternalAnalysisContext extractContextInto(SourceContainer container, InternalAnalysisContext newContext);

  /**
   * Return a namespace containing mappings for all of the public names defined by the given
   * library.
   *
   * @param library the library whose public namespace is to be returned
   * @return the public namespace of the given library
   */
  Namespace getPublicNamespace(LibraryElement library);

  /**
   * Returns a statistics about this context.
   */
  AnalysisContentStatistics get statistics;

  /**
   * Returns a type provider for this context or throws an exception if dart:core cannot be
   * resolved.
   *
   * @return the type provider (not `null`)
   * @throws AnalysisException if dart:core cannot be resolved
   */
  TypeProvider get typeProvider;

  /**
   * Given a table mapping the source for the libraries represented by the corresponding elements to
   * the elements representing the libraries, record those mappings.
   *
   * @param elementMap a table mapping the source for the libraries represented by the elements to
   *          the elements representing the libraries
   */
  void recordLibraryElements(Map<Source, LibraryElement> elementMap);
}

/**
 * Container with global [AnalysisContext] performance statistics.
 */
class PerformanceStatistics {
  /**
   * The [TimeCounter] for time spent in Angular analysis.
   */
  static TimeCounter angular = new TimeCounter();

  /**
   * The [TimeCounter] for time spent in reading files.
   */
  static TimeCounter io = new TimeCounter();

  /**
   * The [TimeCounter] for time spent in scanning.
   */
  static TimeCounter scan = new TimeCounter();

  /**
   * The [TimeCounter] for time spent in parsing.
   */
  static TimeCounter parse = new TimeCounter();

  /**
   * The [TimeCounter] for time spent in resolving.
   */
  static TimeCounter resolve = new TimeCounter();

  /**
   * The [TimeCounter] for time spent in error verifier.
   */
  static TimeCounter errors = new TimeCounter();

  /**
   * The [TimeCounter] for time spent in hints generator.
   */
  static TimeCounter hints = new TimeCounter();
}

/**
 * Instances of the class `RecordingErrorListener` implement an error listener that will
 * record the errors that are reported to it in a way that is appropriate for caching those errors
 * within an analysis context.
 */
class RecordingErrorListener implements AnalysisErrorListener {
  /**
   * A HashMap of lists containing the errors that were collected, keyed by each [Source].
   */
  Map<Source, Set<AnalysisError>> _errors = new Map<Source, Set<AnalysisError>>();

  /**
   * Add all of the errors recorded by the given listener to this listener.
   *
   * @param listener the listener that has recorded the errors to be added
   */
  void addAll(RecordingErrorListener listener) {
    for (AnalysisError error in listener.errors) {
      onError(error);
    }
  }

  /**
   * Answer the errors collected by the listener.
   *
   * @return an array of errors (not `null`, contains no `null`s)
   */
  List<AnalysisError> get errors {
    Iterable<MapEntry<Source, Set<AnalysisError>>> entrySet = getMapEntrySet(_errors);
    int numEntries = entrySet.length;
    if (numEntries == 0) {
      return AnalysisError.NO_ERRORS;
    }
    List<AnalysisError> resultList = new List<AnalysisError>();
    for (MapEntry<Source, Set<AnalysisError>> entry in entrySet) {
      resultList.addAll(entry.getValue());
    }
    return new List.from(resultList);
  }

  /**
   * Answer the errors collected by the listener for some passed [Source].
   *
   * @param source some [Source] for which the caller wants the set of [AnalysisError]s
   *          collected by this listener
   * @return the errors collected by the listener for the passed [Source]
   */
  List<AnalysisError> getErrorsForSource(Source source) {
    Set<AnalysisError> errorsForSource = _errors[source];
    if (errorsForSource == null) {
      return AnalysisError.NO_ERRORS;
    } else {
      return new List.from(errorsForSource);
    }
  }

  @override
  void onError(AnalysisError error) {
    Source source = error.source;
    Set<AnalysisError> errorsForSource = _errors[source];
    if (_errors[source] == null) {
      errorsForSource = new Set<AnalysisError>();
      _errors[source] = errorsForSource;
    }
    errorsForSource.add(error);
  }
}

/**
 * Instances of the class `ResolutionEraser` remove any resolution information from an AST
 * structure when used to visit that structure.
 */
class ResolutionEraser extends GeneralizingAstVisitor<Object> {
  @override
  Object visitAssignmentExpression(AssignmentExpression node) {
    node.staticElement = null;
    node.propagatedElement = null;
    return super.visitAssignmentExpression(node);
  }

  @override
  Object visitBinaryExpression(BinaryExpression node) {
    node.staticElement = null;
    node.propagatedElement = null;
    return super.visitBinaryExpression(node);
  }

  @override
  Object visitCompilationUnit(CompilationUnit node) {
    node.element = null;
    return super.visitCompilationUnit(node);
  }

  @override
  Object visitConstructorDeclaration(ConstructorDeclaration node) {
    node.element = null;
    return super.visitConstructorDeclaration(node);
  }

  @override
  Object visitConstructorName(ConstructorName node) {
    node.staticElement = null;
    return super.visitConstructorName(node);
  }

  @override
  Object visitDirective(Directive node) {
    node.element = null;
    return super.visitDirective(node);
  }

  @override
  Object visitExpression(Expression node) {
    node.staticType = null;
    node.propagatedType = null;
    return super.visitExpression(node);
  }

  @override
  Object visitFunctionExpression(FunctionExpression node) {
    node.element = null;
    return super.visitFunctionExpression(node);
  }

  @override
  Object visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    node.staticElement = null;
    node.propagatedElement = null;
    return super.visitFunctionExpressionInvocation(node);
  }

  @override
  Object visitIndexExpression(IndexExpression node) {
    node.staticElement = null;
    node.propagatedElement = null;
    return super.visitIndexExpression(node);
  }

  @override
  Object visitInstanceCreationExpression(InstanceCreationExpression node) {
    node.staticElement = null;
    return super.visitInstanceCreationExpression(node);
  }

  @override
  Object visitPostfixExpression(PostfixExpression node) {
    node.staticElement = null;
    node.propagatedElement = null;
    return super.visitPostfixExpression(node);
  }

  @override
  Object visitPrefixExpression(PrefixExpression node) {
    node.staticElement = null;
    node.propagatedElement = null;
    return super.visitPrefixExpression(node);
  }

  @override
  Object visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    node.staticElement = null;
    return super.visitRedirectingConstructorInvocation(node);
  }

  @override
  Object visitSimpleIdentifier(SimpleIdentifier node) {
    node.staticElement = null;
    node.propagatedElement = null;
    return super.visitSimpleIdentifier(node);
  }

  @override
  Object visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    node.staticElement = null;
    return super.visitSuperConstructorInvocation(node);
  }
}

/**
 * Instances of the class `ResolvableCompilationUnit` represent a compilation unit that is not
 * referenced by any other objects and for which we have modification stamp information. It is used
 * by the [LibraryResolver] to resolve a library.
 */
class ResolvableCompilationUnit extends TimestampedData<CompilationUnit> {
  /**
   * The source of the compilation unit.
   */
  final Source source;

  /**
   * Initialize a newly created holder to hold the given values.
   *
   * @param modificationTime the modification time of the source from which the AST was created
   * @param unit the AST that was created from the source
   */
  ResolvableCompilationUnit.con1(int modificationTime, CompilationUnit unit) : this.con2(modificationTime, unit, null);

  /**
   * Initialize a newly created holder to hold the given values.
   *
   * @param modificationTime the modification time of the source from which the AST was created
   * @param unit the AST that was created from the source
   * @param source the source of the compilation unit
   */
  ResolvableCompilationUnit.con2(int modificationTime, CompilationUnit unit, this.source) : super(modificationTime, unit);

  /**
   * Return the AST that was created from the source.
   *
   * @return the AST that was created from the source
   */
  CompilationUnit get compilationUnit => data;
}

/**
 * Instances of the class `ResolvableHtmlUnit` represent an HTML unit that is not referenced
 * by any other objects and for which we have modification stamp information. It is used by the
 * [ResolveHtmlTask] to resolve an HTML source.
 */
class ResolvableHtmlUnit extends TimestampedData<ht.HtmlUnit> {
  /**
   * Initialize a newly created holder to hold the given values.
   *
   * @param modificationTime the modification time of the source from which the AST was created
   * @param unit the AST that was created from the source
   */
  ResolvableHtmlUnit(int modificationTime, ht.HtmlUnit unit) : super(modificationTime, unit);

  /**
   * Return the AST that was created from the source.
   *
   * @return the AST that was created from the source
   */
  ht.HtmlUnit get compilationUnit => data;
}

/**
 * The enumerated type `Priority` defines the priority levels used to return sources in an
 * optimal order. A smaller ordinal value equates to a higher priority.
 */
class SourcePriority extends Enum<SourcePriority> {
  /**
   * Used for a Dart source that is known to be a part contained in a library that was recently
   * resolved. These parts are given a higher priority because there is a high probability that
   * their AST structure is still in the cache and therefore would not need to be re-created.
   */
  static const SourcePriority PRIORITY_PART = const SourcePriority('PRIORITY_PART', 0);

  /**
   * Used for a Dart source that is known to be a library.
   */
  static const SourcePriority LIBRARY = const SourcePriority('LIBRARY', 1);

  /**
   * Used for a Dart source whose kind is unknown.
   */
  static const SourcePriority UNKNOWN = const SourcePriority('UNKNOWN', 2);

  /**
   * Used for a Dart source that is known to be a part but whose library has not yet been resolved.
   */
  static const SourcePriority NORMAL_PART = const SourcePriority('NORMAL_PART', 3);

  /**
   * Used for an HTML source.
   */
  static const SourcePriority HTML = const SourcePriority('HTML', 4);

  static const List<SourcePriority> values = const [PRIORITY_PART, LIBRARY, UNKNOWN, NORMAL_PART, HTML];

  const SourcePriority(String name, int ordinal) : super(name, ordinal);
}

/**
 * Instances of the class `TimestampedData` represent analysis data for which we have a
 * modification time.
 */
class TimestampedData<E> {
  /**
   * The modification time of the source from which the data was created.
   */
  final int modificationTime;

  /**
   * The data that was created from the source.
   */
  final E data;

  /**
   * Initialize a newly created holder to hold the given values.
   *
   * @param modificationTime the modification time of the source from which the data was created
   * @param unit the data that was created from the source
   */
  TimestampedData(this.modificationTime, this.data);
}

/**
 * Instances of the class `WorkManager` manage a list of sources that need to have analysis
 * work performed on them.
 */
class WorkManager {
  /**
   * An array containing the various queues is priority order.
   */
  List<List<Source>> _workQueues;

  /**
   * Initialize a newly created manager to have no work queued up.
   */
  WorkManager() {
    int queueCount = SourcePriority.values.length;
    _workQueues = new List<List>(queueCount);
    for (int i = 0; i < queueCount; i++) {
      _workQueues[i] = new List<Source>();
    }
  }

  /**
   * Record that the given source needs to be analyzed. The priority level is used to control when
   * the source will be analyzed with respect to other sources. If the source was previously added
   * then it's priority is updated. If it was previously added with the same priority then it's
   * position in the queue is unchanged.
   *
   * @param source the source that needs to be analyzed
   * @param priority the priority level of the source
   */
  void add(Source source, SourcePriority priority) {
    int queueCount = _workQueues.length;
    int ordinal = priority.ordinal;
    for (int i = 0; i < queueCount; i++) {
      List<Source> queue = _workQueues[i];
      if (i == ordinal) {
        if (!queue.contains(source)) {
          queue.add(source);
        }
      } else {
        queue.remove(source);
      }
    }
  }

  /**
   * Record that the given source needs to be analyzed. The priority level is used to control when
   * the source will be analyzed with respect to other sources. If the source was previously added
   * then it's priority is updated. In either case, it will be analyzed before other sources of the
   * same priority.
   *
   * @param source the source that needs to be analyzed
   * @param priority the priority level of the source
   */
  void addFirst(Source source, SourcePriority priority) {
    int queueCount = _workQueues.length;
    int ordinal = priority.ordinal;
    for (int i = 0; i < queueCount; i++) {
      List<Source> queue = _workQueues[i];
      if (i == ordinal) {
        queue.remove(source);
        queue.insert(0, source);
      } else {
        queue.remove(source);
      }
    }
  }

  /**
   * Return an iterator that can be used to access the sources to be analyzed in the order in which
   * they should be analyzed.
   *
   * <b>Note:</b> As with other iterators, no sources can be added or removed from this work manager
   * while the iterator is being used. Unlike some implementations, however, the iterator will not
   * detect when this requirement has been violated; it might work correctly, it might return the
   * wrong source, or it might throw an exception.
   *
   * @return an iterator that can be used to access the next source to be analyzed
   */
  WorkManager_WorkIterator iterator() => new WorkManager_WorkIterator(this);

  /**
   * Record that the given source is fully analyzed.
   *
   * @param source the source that is fully analyzed
   */
  void remove(Source source) {
    int queueCount = _workQueues.length;
    for (int i = 0; i < queueCount; i++) {
      _workQueues[i].remove(source);
    }
  }

  @override
  String toString() {
    JavaStringBuilder builder = new JavaStringBuilder();
    List<SourcePriority> priorities = SourcePriority.values;
    bool needsSeparator = false;
    int queueCount = _workQueues.length;
    for (int i = 0; i < queueCount; i++) {
      List<Source> queue = _workQueues[i];
      if (!queue.isEmpty) {
        if (needsSeparator) {
          builder.append("; ");
        }
        builder.append(priorities[i]);
        builder.append(": ");
        int queueSize = queue.length;
        for (int j = 0; j < queueSize; j++) {
          if (j > 0) {
            builder.append(", ");
          }
          builder.append(queue[j].fullName);
        }
        needsSeparator = true;
      }
    }
    return builder.toString();
  }
}

/**
 * Instances of the class `WorkIterator` implement an iterator that returns the sources in a
 * work manager in the order in which they are to be analyzed.
 */
class WorkManager_WorkIterator {
  final WorkManager WorkManager_this;

  /**
   * The index of the work queue through which we are currently iterating.
   */
  int _queueIndex = 0;

  /**
   * The index of the next element of the work queue to be returned.
   */
  int _index = -1;

  /**
   * Initialize a newly created iterator to be ready to return the first element in the iteration.
   */
  WorkManager_WorkIterator(this.WorkManager_this) {
    _advance();
  }

  /**
   * Return `true` if there is another [Source] available for processing.
   *
   * @return `true` if there is another [Source] available for processing
   */
  bool get hasNext => _queueIndex < WorkManager_this._workQueues.length;

  /**
   * Return the next [Source] available for processing and advance so that the returned
   * source will not be returned again.
   *
   * @return the next [Source] available for processing
   */
  Source next() {
    if (!hasNext) {
      throw new NoSuchElementException();
    }
    Source source = WorkManager_this._workQueues[_queueIndex][_index];
    _advance();
    return source;
  }

  /**
   * Increment the [index] and [queueIndex] so that they are either indicating the
   * next source to be returned or are indicating that there are no more sources to be returned.
   */
  void _advance() {
    _index++;
    if (_index >= WorkManager_this._workQueues[_queueIndex].length) {
      _index = 0;
      _queueIndex++;
      while (_queueIndex < WorkManager_this._workQueues.length && WorkManager_this._workQueues[_queueIndex].isEmpty) {
        _queueIndex++;
      }
    }
  }
}

/**
 * An [Expression] with optional [AngularFilterNode]s.
 */
class AngularExpression {
  /**
   * The [Expression] to apply filters to.
   */
  final Expression expression;

  /**
   * The filters to apply.
   */
  final List<AngularFilterNode> filters;

  AngularExpression(this.expression, this.filters);

  /**
   * Return the offset of the character immediately following the last character of this node's
   * source range. This is equivalent to `node.getOffset() + node.getLength()`.
   *
   * @return the offset of the character just past the node's source range
   */
  int get end {
    if (filters.isEmpty) {
      return expression.end;
    }
    AngularFilterNode lastFilter = filters[filters.length - 1];
    List<AngularFilterArgument> filterArguments = lastFilter.arguments;
    if (filterArguments.isEmpty) {
      return lastFilter.name.end;
    }
    return filterArguments[filterArguments.length - 1].expression.end;
  }

  /**
   * Return Dart [Expression]s this Angular expression consists of.
   */
  List<Expression> get expressions {
    List<Expression> expressions = [];
    expressions.add(expression);
    for (AngularFilterNode filter in filters) {
      expressions.add(filter.name);
      for (AngularFilterArgument filterArgument in filter.arguments) {
        expressions.addAll(filterArgument.subExpressions);
        expressions.add(filterArgument.expression);
      }
    }
    return expressions;
  }

  /**
   * Return the number of characters in the expression's source range.
   */
  int get length => end - offset;

  /**
   * Return the offset of the first character in the expression's source range.
   */
  int get offset => expression.offset;
}

/**
 * Angular filter argument.
 */
class AngularFilterArgument {
  /**
   * The [TokenType#COLON] token.
   */
  final Token token;

  /**
   * The argument expression.
   */
  final Expression expression;

  /**
   * The optional sub-[Expression]s.
   */
  List<Expression> subExpressions = Expression.EMPTY_ARRAY;

  AngularFilterArgument(this.token, this.expression);
}

/**
 * Angular filter node.
 */
class AngularFilterNode {
  /**
   * The [TokenType#BAR] token.
   */
  final Token token;

  /**
   * The name of the filter.
   */
  final SimpleIdentifier name;

  /**
   * The arguments for this filter.
   */
  final List<AngularFilterArgument> arguments;

  AngularFilterNode(this.token, this.name, this.arguments);
}

/**
 * Instances of the class [AngularHtmlUnitResolver] resolve Angular specific expressions.
 */
class AngularHtmlUnitResolver extends ht.RecursiveXmlVisitor<Object> {
  static String _NG_APP = "ng-app";

  /**
   * Checks if given [Element] is an artificial local variable and returns corresponding
   * [AngularElement], or `null` otherwise.
   */
  static AngularElement getAngularElement(Element element) {
    // may be artificial local variable, replace with AngularElement
    if (element is LocalVariableElement) {
      LocalVariableElement local = element;
      List<ToolkitObjectElement> toolkitObjects = local.toolkitObjects;
      if (toolkitObjects.length == 1 && toolkitObjects[0] is AngularElement) {
        return toolkitObjects[0] as AngularElement;
      }
    }
    // not a special Element
    return null;
  }

  /**
   * @return `true` if the given [HtmlUnit] has <code>ng-app</code> annotation.
   */
  static bool hasAngularAnnotation(ht.HtmlUnit htmlUnit) {
    try {
      htmlUnit.accept(new RecursiveXmlVisitor_AngularHtmlUnitResolver_hasAngularAnnotation());
    } on AngularHtmlUnitResolver_FoundAppError catch (e) {
      return true;
    }
    return false;
  }

  static SimpleIdentifier _createIdentifier(String name, int offset) {
    StringToken token = _createStringToken(name, offset);
    return new SimpleIdentifier(token);
  }

  /**
   * Adds [AngularElement] declared by the given top-level [Element].
   *
   * @param angularElements the list to fill with top-level [AngularElement]s
   * @param classElement the [ClassElement] to get [AngularElement]s from
   */
  static void _addAngularElementsFromClass(Set<AngularElement> angularElements, ClassElement classElement) {
    for (ToolkitObjectElement toolkitObject in classElement.toolkitObjects) {
      if (toolkitObject is AngularElement) {
        angularElements.add(toolkitObject);
      }
    }
  }

  /**
   * Returns the array of all top-level Angular elements that could be used in this library.
   *
   * @param libraryElement the [LibraryElement] to analyze
   * @return the array of all top-level Angular elements that could be used in this library
   */
  static void _addAngularElementsFromLibrary(Set<AngularElement> angularElements, LibraryElement library, Set<LibraryElement> visited) {
    if (library == null) {
      return;
    }
    if (!visited.add(library)) {
      return;
    }
    // add Angular elements from current library
    for (CompilationUnitElement unit in library.units) {
      angularElements.addAll(unit.angularViews);
      for (ClassElement type in unit.types) {
        _addAngularElementsFromClass(angularElements, type);
      }
    }
    // handle imports
    for (ImportElement importElement in library.imports) {
      LibraryElement importedLibrary = importElement.importedLibrary;
      _addAngularElementsFromLibrary(angularElements, importedLibrary, visited);
    }
  }

  static StringToken _createStringToken(String name, int offset) => new StringToken(TokenType.IDENTIFIER, name, offset);

  /**
   * Returns the array of all top-level Angular elements that could be used in this library.
   *
   * @param libraryElement the [LibraryElement] to analyze
   * @return the array of all top-level Angular elements that could be used in this library
   */
  static List<AngularElement> _getAngularElements(Set<LibraryElement> libraries, LibraryElement libraryElement) {
    Set<AngularElement> angularElements = new Set();
    _addAngularElementsFromLibrary(angularElements, libraryElement, libraries);
    return new List.from(angularElements);
  }

  /**
   * Returns the external Dart [CompilationUnit] referenced by the given [HtmlUnit].
   */
  static CompilationUnit _getDartUnit(AnalysisContext context, ht.HtmlUnit unit) {
    for (HtmlScriptElement script in unit.element.scripts) {
      if (script is ExternalHtmlScriptElement) {
        Source scriptSource = script.scriptSource;
        if (scriptSource != null) {
          return context.resolveCompilationUnit2(scriptSource, scriptSource);
        }
      }
    }
    return null;
  }

  static Set<Source> _getLibrarySources(Set<LibraryElement> libraries) {
    Set<Source> sources = new Set();
    for (LibraryElement library in libraries) {
      sources.add(library.source);
    }
    return sources;
  }

  final InternalAnalysisContext _context;

  TypeProvider _typeProvider;

  AngularHtmlUnitResolver_FilteringAnalysisErrorListener _errorListener;

  final Source _source;

  final LineInfo _lineInfo;

  final ht.HtmlUnit _unit;

  List<AngularElement> _angularElements;

  List<NgProcessor> _processors = [];

  LibraryElementImpl _libraryElement;

  CompilationUnitElementImpl _unitElement;

  FunctionElementImpl _functionElement;

  ResolverVisitor _resolver;

  bool _isAngular = false;

  List<LocalVariableElementImpl> _definedVariables = [];

  Set<LibraryElement> _injectedLibraries = new Set();

  Scope _topNameScope;

  Scope _nameScope;

  AngularHtmlUnitResolver(this._context, AnalysisErrorListener errorListener, this._source, this._lineInfo, this._unit) {
    this._typeProvider = _context.typeProvider;
    this._errorListener = new AngularHtmlUnitResolver_FilteringAnalysisErrorListener(errorListener);
  }

  /**
   * The [AngularApplication] for the Web application with this entry point, may be
   * `null` if not an entry point.
   */
  AngularApplication calculateAngularApplication() {
    // check if Angular at all
    if (!hasAngularAnnotation(_unit)) {
      return null;
    }
    // prepare resolved Dart unit
    CompilationUnit dartUnit = _getDartUnit(_context, _unit);
    if (dartUnit == null) {
      return null;
    }
    // prepare accessible Angular elements
    LibraryElement libraryElement = dartUnit.element.library;
    Set<LibraryElement> libraries = new Set();
    List<AngularElement> angularElements = _getAngularElements(libraries, libraryElement);
    // resolve AngularComponentElement template URIs
    // TODO(scheglov) resolve to HtmlElement to allow F3 ?
    Set<Source> angularElementsSources = new Set();
    for (AngularElement angularElement in angularElements) {
      if (angularElement is AngularHasTemplateElement) {
        AngularHasTemplateElement hasTemplate = angularElement;
        angularElementsSources.add(angularElement.source);
        String templateUri = hasTemplate.templateUri;
        if (templateUri == null) {
          continue;
        }
        try {
          Source templateSource = _source.resolveRelative(parseUriWithException(templateUri));
          if (!_context.exists(templateSource)) {
            templateSource = _context.sourceFactory.resolveUri(_source, "package:${templateUri}");
            if (!_context.exists(templateSource)) {
              _errorListener.onError(new AnalysisError.con2(angularElement.source, hasTemplate.templateUriOffset, templateUri.length, AngularCode.URI_DOES_NOT_EXIST, [templateUri]));
              continue;
            }
          }
          if (!AnalysisEngine.isHtmlFileName(templateUri)) {
            continue;
          }
          if (hasTemplate is AngularComponentElementImpl) {
            hasTemplate.templateSource = templateSource;
          }
          if (hasTemplate is AngularViewElementImpl) {
            hasTemplate.templateSource = templateSource;
          }
        } on URISyntaxException catch (exception) {
          _errorListener.onError(new AnalysisError.con2(angularElement.source, hasTemplate.templateUriOffset, templateUri.length, AngularCode.INVALID_URI, [templateUri]));
        }
      }
    }
    // create AngularApplication
    AngularApplication application = new AngularApplication(_source, _getLibrarySources(libraries), angularElements, new List.from(angularElementsSources));
    // set AngularApplication for each AngularElement
    for (AngularElement angularElement in angularElements) {
      (angularElement as AngularElementImpl).application = application;
    }
    // done
    return application;
  }

  /**
   * Resolves [source] as an [AngularComponentElement] template file.
   *
   * @param application the Angular application we are resolving for
   * @param component the [AngularComponentElement] to resolve template for, not `null`
   */
  void resolveComponentTemplate(AngularApplication application, AngularComponentElement component) {
    _isAngular = true;
    _resolveInternal(application.elements, component);
  }

  /**
   * Resolves [source] as an Angular application entry point.
   */
  void resolveEntryPoint(AngularApplication application) {
    _resolveInternal(application.elements, null);
  }

  @override
  Object visitXmlAttributeNode(ht.XmlAttributeNode node) {
    _parseEmbeddedExpressionsInAttribute(node);
    _resolveExpressions(node.expressions);
    return super.visitXmlAttributeNode(node);
  }

  @override
  Object visitXmlTagNode(ht.XmlTagNode node) {
    bool wasAngular = _isAngular;
    try {
      // new Angular context
      if (node.getAttribute(_NG_APP) != null) {
        _isAngular = true;
        _visitModelDirectives(node);
      }
      // not Angular
      if (!_isAngular) {
        return super.visitXmlTagNode(node);
      }
      // process node in separate name scope
      _pushNameScope();
      try {
        _parseEmbeddedExpressionsInTag(node);
        // apply processors
        for (NgProcessor processor in _processors) {
          if (processor.canApply(node)) {
            processor.apply(this, node);
          }
        }
        // resolve expressions
        _resolveExpressions(node.expressions);
        // process children
        return super.visitXmlTagNode(node);
      } finally {
        _popNameScope();
      }
    } finally {
      _isAngular = wasAngular;
    }
  }

  /**
   * Creates new [LocalVariableElementImpl] with given type and identifier.
   *
   * @param type the [Type] of the variable
   * @param identifier the identifier to create variable for
   * @return the new [LocalVariableElementImpl]
   */
  LocalVariableElementImpl _createLocalVariableFromIdentifier(DartType type, SimpleIdentifier identifier) {
    LocalVariableElementImpl variable = new LocalVariableElementImpl(identifier);
    _definedVariables.add(variable);
    variable.type = type;
    return variable;
  }

  /**
   * Creates new [LocalVariableElementImpl] with given name and type.
   *
   * @param type the [Type] of the variable
   * @param name the name of the variable
   * @return the new [LocalVariableElementImpl]
   */
  LocalVariableElementImpl _createLocalVariableWithName(DartType type, String name) {
    SimpleIdentifier identifier = _createIdentifier(name, 0);
    return _createLocalVariableFromIdentifier(type, identifier);
  }

  /**
   * Declares the given [LocalVariableElementImpl] in the [topNameScope].
   */
  void _defineTopVariable(LocalVariableElementImpl variable) {
    _recordDefinedVariable(variable);
    _topNameScope.define(variable);
    _recordTypeLibraryInjected(variable);
  }

  /**
   * Declares the given [LocalVariableElementImpl] in the current [nameScope].
   */
  void _defineVariable(LocalVariableElementImpl variable) {
    _recordDefinedVariable(variable);
    _nameScope.define(variable);
    _recordTypeLibraryInjected(variable);
  }

  /**
   * @return the [AngularElement] with the given name, maybe `null`.
   */
  AngularElement _findAngularElement(String name) {
    for (AngularElement element in _angularElements) {
      if (name == element.name) {
        return element;
      }
    }
    return null;
  }

  /**
   * @return the [TypeProvider] of the [AnalysisContext].
   */
  TypeProvider get typeProvider => _typeProvider;

  /**
   * Parses given [String] as an [AngularExpression] at the given offset.
   */
  AngularExpression _parseAngularExpression(String contents, int startIndex, int endIndex, int offset) {
    Token token = _scanDart(contents, startIndex, endIndex, offset);
    return _parseAngularExpressionInToken(token);
  }

  AngularExpression _parseAngularExpressionInToken(Token token) {
    List<Token> tokens = _splitAtBar(token);
    Expression mainExpression = _parseDartExpressionInToken(tokens[0]);
    // parse filters
    List<AngularFilterNode> filters = [];
    for (int i = 1; i < tokens.length; i++) {
      Token filterToken = tokens[i];
      Token barToken = filterToken;
      filterToken = filterToken.next;
      // TODO(scheglov) report missing identifier
      SimpleIdentifier name = _parseDartExpressionInToken(filterToken) as SimpleIdentifier;
      filterToken = name.endToken.next;
      // parse arguments
      List<AngularFilterArgument> arguments = [];
      while (filterToken.type != TokenType.EOF) {
        // skip ":"
        Token colonToken = filterToken;
        if (colonToken.type == TokenType.COLON) {
          filterToken = filterToken.next;
        } else {
          _reportErrorForToken(AngularCode.MISSING_FILTER_COLON, colonToken, []);
        }
        // parse argument
        Expression argument = _parseDartExpressionInToken(filterToken);
        arguments.add(new AngularFilterArgument(colonToken, argument));
        // next token
        filterToken = argument.endToken.next;
      }
      filters.add(new AngularFilterNode(barToken, name, arguments));
    }
    // done
    return new AngularExpression(mainExpression, filters);
  }

  /**
   * Parses given [String] as an [Expression] at the given offset.
   */
  Expression _parseDartExpression(String contents, int startIndex, int endIndex, int offset) {
    Token token = _scanDart(contents, startIndex, endIndex, offset);
    return _parseDartExpressionInToken(token);
  }

  Expression _parseDartExpressionInToken(Token token) {
    Parser parser = new Parser(_source, _errorListener);
    return parser.parseExpression(token);
  }

  void _popNameScope() {
    _nameScope = _resolver.popNameScope();
  }

  void _pushNameScope() {
    _nameScope = _resolver.pushNameScope();
  }

  /**
   * Reports given [ErrorCode] at the given [AstNode].
   */
  void _reportErrorForNode(ErrorCode errorCode, AstNode node, List<Object> arguments) {
    _reportErrorForOffset(errorCode, node.offset, node.length, arguments);
  }

  /**
   * Reports given [ErrorCode] at the given position.
   */
  void _reportErrorForOffset(ErrorCode errorCode, int offset, int length, List<Object> arguments) {
    _errorListener.onError(new AnalysisError.con2(_source, offset, length, errorCode, arguments));
  }

  /**
   * Reports given [ErrorCode] at the given [Token].
   */
  void _reportErrorForToken(ErrorCode errorCode, Token token, List<Object> arguments) {
    _reportErrorForOffset(errorCode, token.offset, token.length, arguments);
  }

  void _resolveExpression(AngularExpression angularExpression) {
    List<Expression> dartExpressions = angularExpression.expressions;
    for (Expression dartExpression in dartExpressions) {
      _resolveNode(dartExpression);
    }
  }

  /**
   * Resolves given [AstNode] using [resolver].
   */
  void _resolveNode(AstNode node) {
    node.accept(_resolver);
  }

  Token _scanDart(String contents, int startIndex, int endIndex, int offset) => ht.HtmlParser.scanDartSource(_source, _lineInfo, contents.substring(startIndex, endIndex), offset + startIndex, _errorListener);

  /**
   * Puts into [libraryElement] an artificial [LibraryElementImpl] for this HTML
   * [Source].
   */
  void _createLibraryElement() {
    // create CompilationUnitElementImpl
    String unitName = _source.shortName;
    _unitElement = new CompilationUnitElementImpl(unitName);
    _unitElement.source = _source;
    // create LibraryElementImpl
    _libraryElement = new LibraryElementImpl(_context, null);
    _libraryElement.definingCompilationUnit = _unitElement;
    _libraryElement.angularHtml = true;
    _injectedLibraries.add(_libraryElement);
    // create FunctionElementImpl
    _functionElement = new FunctionElementImpl.con2(0);
    _unitElement.functions = <FunctionElement> [_functionElement];
  }

  /**
   * Creates new [NgProcessor] for the given [AngularElement], maybe `null` if not
   * supported.
   */
  NgProcessor _createProcessor(AngularElement element) {
    if (element is AngularComponentElement) {
      AngularComponentElement component = element;
      return new NgComponentElementProcessor(component);
    }
    if (element is AngularControllerElement) {
      AngularControllerElement controller = element;
      return new NgControllerElementProcessor(controller);
    }
    if (element is AngularDirectiveElement) {
      AngularDirectiveElement directive = element;
      return new NgDirectiveElementProcessor(directive);
    }
    return null;
  }

  /**
   * Puts into [resolver] an [ResolverVisitor] to resolve [Expression]s in
   * [source].
   */
  void _createResolver() {
    InheritanceManager inheritanceManager = new InheritanceManager(_libraryElement);
    _resolver = new ResolverVisitor.con2(_libraryElement, _source, _typeProvider, inheritanceManager, _errorListener);
    _topNameScope = _resolver.pushNameScope();
    // add Scope variables - no type, no location, just to avoid warnings
    {
      DartType type = _typeProvider.dynamicType;
      _topNameScope.define(_createLocalVariableWithName(type, "\$id"));
      _topNameScope.define(_createLocalVariableWithName(type, "\$parent"));
      _topNameScope.define(_createLocalVariableWithName(type, "\$root"));
    }
  }

  /**
   * Defines variable for the given [AngularElement] with type of the enclosing
   * [ClassElement].
   */
  void _defineTopVariable_forClassElement(AngularElement element) {
    ClassElement classElement = element.enclosingElement as ClassElement;
    InterfaceType type = classElement.type;
    LocalVariableElementImpl variable = _createLocalVariableWithName(type, element.name);
    _defineTopVariable(variable);
    variable.toolkitObjects = <AngularElement> [element];
  }

  /**
   * Defines variable for the given [AngularScopePropertyElement].
   */
  void _defineTopVariable_forScopeProperty(AngularScopePropertyElement element) {
    DartType type = element.type;
    LocalVariableElementImpl variable = _createLocalVariableWithName(type, element.name);
    _defineTopVariable(variable);
    variable.toolkitObjects = <AngularElement> [element];
  }

  /**
   * Parse the value of the given token for embedded expressions, and add any embedded expressions
   * that are found to the given list of expressions.
   *
   * @param expressions the list to which embedded expressions are to be added
   * @param token the token whose value is to be parsed
   */
  void _parseEmbeddedExpressions(List<AngularMoustacheXmlExpression> expressions, ht.Token token) {
    // prepare Token information
    String lexeme = token.lexeme;
    int offset = token.offset;
    // find expressions between {{ and }}
    int startIndex = StringUtilities.indexOf2(lexeme, 0, AngularMoustacheXmlExpression.OPENING_DELIMITER_CHAR, AngularMoustacheXmlExpression.OPENING_DELIMITER_CHAR);
    while (startIndex >= 0) {
      int endIndex = StringUtilities.indexOf2(lexeme, startIndex + AngularMoustacheXmlExpression.OPENING_DELIMITER_LENGTH, AngularMoustacheXmlExpression.CLOSING_DELIMITER_CHAR, AngularMoustacheXmlExpression.CLOSING_DELIMITER_CHAR);
      if (endIndex < 0) {
        // TODO(brianwilkerson) Should we report this error or will it be reported by something else?
        return;
      } else if (startIndex + AngularMoustacheXmlExpression.OPENING_DELIMITER_LENGTH < endIndex) {
        startIndex += AngularMoustacheXmlExpression.OPENING_DELIMITER_LENGTH;
        AngularExpression expression = _parseAngularExpression(lexeme, startIndex, endIndex, offset);
        expressions.add(new AngularMoustacheXmlExpression(startIndex, endIndex, expression));
      }
      startIndex = StringUtilities.indexOf2(lexeme, endIndex + AngularMoustacheXmlExpression.CLOSING_DELIMITER_LENGTH, AngularMoustacheXmlExpression.OPENING_DELIMITER_CHAR, AngularMoustacheXmlExpression.OPENING_DELIMITER_CHAR);
    }
  }

  void _parseEmbeddedExpressionsInAttribute(ht.XmlAttributeNode node) {
    List<AngularMoustacheXmlExpression> expressions = [];
    _parseEmbeddedExpressions(expressions, node.valueToken);
    if (!expressions.isEmpty) {
      node.expressions = new List.from(expressions);
    }
  }

  void _parseEmbeddedExpressionsInTag(ht.XmlTagNode node) {
    List<AngularMoustacheXmlExpression> expressions = [];
    ht.Token token = node.attributeEnd;
    ht.Token endToken = node.endToken;
    bool inChild = false;
    while (!identical(token, endToken)) {
      for (ht.XmlTagNode child in node.tagNodes) {
        if (identical(token, child.beginToken)) {
          inChild = true;
          break;
        }
        if (identical(token, child.endToken)) {
          inChild = false;
          break;
        }
      }
      if (!inChild && token.type == ht.TokenType.TEXT) {
        _parseEmbeddedExpressions(expressions, token);
      }
      token = token.next;
    }
    node.expressions = new List.from(expressions);
  }

  void _recordDefinedVariable(LocalVariableElementImpl variable) {
    _definedVariables.add(variable);
    _functionElement.localVariables = new List.from(_definedVariables);
  }

  /**
   * When we inject variable, we give access to the library of its type.
   */
  void _recordTypeLibraryInjected(LocalVariableElementImpl variable) {
    LibraryElement typeLibrary = variable.type.element.library;
    _injectedLibraries.add(typeLibrary);
  }

  void _resolveExpressions(List<ht.XmlExpression> expressions) {
    for (ht.XmlExpression xmlExpression in expressions) {
      if (xmlExpression is AngularXmlExpression) {
        AngularXmlExpression angularXmlExpression = xmlExpression;
        _resolveXmlExpression(angularXmlExpression);
      }
    }
  }

  /**
   * Resolves Angular specific expressions and elements in the [source].
   *
   * @param angularElements the [AngularElement]s accessible in the component's library, not
   *          `null`
   * @param component the [AngularComponentElement] to resolve template for, maybe
   *          `null` if not a component template
   */
  void _resolveInternal(List<AngularElement> angularElements, AngularComponentElement component) {
    this._angularElements = angularElements;
    // add built-in processors
    _processors.add(NgModelProcessor.INSTANCE);
    // _processors.add(NgRepeatProcessor.INSTANCE);
    // add element's libraries
    for (AngularElement angularElement in angularElements) {
      _injectedLibraries.add(angularElement.library);
    }
    // prepare Dart library
    _createLibraryElement();
    (_unit.element as HtmlElementImpl).angularCompilationUnit = _unitElement;
    // prepare Dart resolver
    _createResolver();
    // maybe resolving component template
    if (component != null) {
      _defineTopVariable_forClassElement(component);
      for (AngularScopePropertyElement scopeProperty in component.scopeProperties) {
        _defineTopVariable_forScopeProperty(scopeProperty);
      }
    }
    // add processors
    for (AngularElement angularElement in angularElements) {
      NgProcessor processor = _createProcessor(angularElement);
      if (processor != null) {
        _processors.add(processor);
      }
    }
    // define filters
    for (AngularElement angularElement in angularElements) {
      if (angularElement is AngularFilterElement) {
        _defineTopVariable_forClassElement(angularElement);
      }
    }
    // run this HTML visitor
    _unit.accept(this);
    // simulate imports for injects
    {
      List<ImportElement> imports = [];
      for (LibraryElement injectedLibrary in _injectedLibraries) {
        ImportElementImpl importElement = new ImportElementImpl(-1);
        importElement.importedLibrary = injectedLibrary;
        imports.add(importElement);
      }
      _libraryElement.imports = new List.from(imports);
    }
    // push conditional errors
    for (ProxyConditionalAnalysisError conditionalCode in _resolver.proxyConditionalAnalysisErrors) {
      _resolver.reportError(conditionalCode.analysisError);
    }
  }

  void _resolveXmlExpression(AngularXmlExpression angularXmlExpression) {
    AngularExpression angularExpression = angularXmlExpression.expression;
    _resolveExpression(angularExpression);
  }

  List<Token> _splitAtBar(Token token) {
    List<Token> tokens = [];
    tokens.add(token);
    while (token.type != TokenType.EOF) {
      if (token.type == TokenType.BAR) {
        tokens.add(token);
        Token eofToken = new Token(TokenType.EOF, 0);
        token.previous.setNext(eofToken);
      }
      token = token.next;
    }
    return tokens;
  }

  /**
   * The "ng-model" directive is special, it contributes to the top-level name scope. These models
   * can be used before actual "ng-model" attribute in HTML. So, we need to define them once we
   * found [NG_APP] context.
   */
  void _visitModelDirectives(ht.XmlTagNode appNode) {
    appNode.accept(new RecursiveXmlVisitor_AngularHtmlUnitResolver_visitModelDirectives(this));
  }
}

class AngularHtmlUnitResolver_FilteringAnalysisErrorListener implements AnalysisErrorListener {
  final AnalysisErrorListener _listener;

  AngularHtmlUnitResolver_FilteringAnalysisErrorListener(this._listener);

  @override
  void onError(AnalysisError error) {
    ErrorCode errorCode = error.errorCode;
    if (identical(errorCode, StaticWarningCode.UNDEFINED_GETTER) || identical(errorCode, StaticWarningCode.UNDEFINED_IDENTIFIER) || identical(errorCode, StaticTypeWarningCode.UNDEFINED_GETTER)) {
      return;
    }
    _listener.onError(error);
  }
}

class AngularHtmlUnitResolver_FoundAppError extends Error {
}

class RecursiveXmlVisitor_AngularHtmlUnitResolver_hasAngularAnnotation extends ht.RecursiveXmlVisitor<Object> {
  @override
  Object visitXmlTagNode(ht.XmlTagNode node) {
    if (node.getAttribute(AngularHtmlUnitResolver._NG_APP) != null) {
      throw new AngularHtmlUnitResolver_FoundAppError();
    }
    return super.visitXmlTagNode(node);
  }
}

class RecursiveXmlVisitor_AngularHtmlUnitResolver_visitModelDirectives extends ht.RecursiveXmlVisitor<Object> {
  final AngularHtmlUnitResolver AngularHtmlUnitResolver_this;

  RecursiveXmlVisitor_AngularHtmlUnitResolver_visitModelDirectives(this.AngularHtmlUnitResolver_this) : super();

  @override
  Object visitXmlTagNode(ht.XmlTagNode node) {
    NgModelProcessor directive = NgModelProcessor.INSTANCE;
    if (directive.canApply(node)) {
      directive._applyTopDeclarations(AngularHtmlUnitResolver_this, node);
    }
    return super.visitXmlTagNode(node);
  }
}

/**
 * Implementation of [AngularXmlExpression] for an [AngularExpression] enclosed between
 * <code>{{</code> and <code>}}</code>.
 */
class AngularMoustacheXmlExpression extends AngularXmlExpression {
  static int OPENING_DELIMITER_CHAR = 0x7B;

  static int CLOSING_DELIMITER_CHAR = 0x7D;

  static String OPENING_DELIMITER = "{{";

  static String CLOSING_DELIMITER = "}}";

  static int OPENING_DELIMITER_LENGTH = OPENING_DELIMITER.length;

  static int CLOSING_DELIMITER_LENGTH = CLOSING_DELIMITER.length;

  /**
   * The offset of the first character of the opening delimiter.
   */
  final int _openingOffset;

  /**
   * The offset of the first character of the closing delimiter.
   */
  final int _closingOffset;

  AngularMoustacheXmlExpression(this._openingOffset, this._closingOffset, AngularExpression expression) : super(expression);

  @override
  int get end => _closingOffset + CLOSING_DELIMITER_LENGTH;

  @override
  int get length => _closingOffset + CLOSING_DELIMITER_LENGTH - _openingOffset;

  @override
  int get offset => _openingOffset;
}

/**
 * Implementation of [AngularXmlExpression] for an [AngularExpression] embedded without
 * any wrapping characters.
 */
class AngularRawXmlExpression extends AngularXmlExpression {
  AngularRawXmlExpression(AngularExpression expression) : super(expression);

  @override
  int get end => expression.end;

  @override
  int get length => expression.length;

  @override
  int get offset => expression.offset;
}

/**
 * Abstract Angular specific [XmlExpression].
 */
abstract class AngularXmlExpression extends ht.XmlExpression {
  /**
   * The expression that is enclosed between the delimiters.
   */
  final AngularExpression expression;

  AngularXmlExpression(this.expression);

  @override
  ht.XmlExpression_Reference getReference(int offset) {
    // main expression
    ht.XmlExpression_Reference reference = _getReferenceAtNode(expression.expression, offset);
    if (reference != null) {
      return reference;
    }
    // filters
    for (AngularFilterNode filter in expression.filters) {
      // filter name
      reference = _getReferenceAtNode(filter.name, offset);
      if (reference != null) {
        return reference;
      }
      // filter arguments
      for (AngularFilterArgument filterArgument in filter.arguments) {
        reference = _getReferenceAtNode(filterArgument.expression, offset);
        if (reference != null) {
          return reference;
        }
      }
    }
    return null;
  }

  /**
   * If the given [AstNode] has an [Element] at the given offset, then returns
   * [Reference] with this [Element].
   */
  ht.XmlExpression_Reference _getReferenceAtNode(AstNode root, int offset) {
    AstNode node = new NodeLocator.con1(offset).searchWithin(root);
    if (node != null) {
      Element element = ElementLocator.locate(node);
      return new ht.XmlExpression_Reference(element, node.offset, node.length);
    }
    return null;
  }
}

/**
 * Recursively visits [HtmlUnit] and every embedded [Expression].
 */
abstract class ExpressionVisitor extends ht.RecursiveXmlVisitor<Object> {
  /**
   * Visits the given [Expression]s embedded into tag or attribute.
   *
   * @param expression the [Expression] to visit, not `null`
   */
  void visitExpression(Expression expression);

  @override
  Object visitXmlAttributeNode(ht.XmlAttributeNode node) {
    _visitExpressions(node.expressions);
    return super.visitXmlAttributeNode(node);
  }

  @override
  Object visitXmlTagNode(ht.XmlTagNode node) {
    _visitExpressions(node.expressions);
    return super.visitXmlTagNode(node);
  }

  /**
   * Visits [Expression]s of the given [XmlExpression]s.
   */
  void _visitExpressions(List<ht.XmlExpression> expressions) {
    for (ht.XmlExpression xmlExpression in expressions) {
      if (xmlExpression is AngularXmlExpression) {
        AngularXmlExpression angularXmlExpression = xmlExpression;
        List<Expression> dartExpressions = angularXmlExpression.expression.expressions;
        for (Expression dartExpression in dartExpressions) {
          visitExpression(dartExpression);
        }
      }
      if (xmlExpression is ht.RawXmlExpression) {
        ht.RawXmlExpression rawXmlExpression = xmlExpression;
        visitExpression(rawXmlExpression.expression);
      }
    }
  }
}

/**
 * [NgComponentElementProcessor] applies [AngularComponentElement] by parsing mapped
 * attributes as expressions.
 */
class NgComponentElementProcessor extends NgDirectiveProcessor {
  final AngularComponentElement _element;

  NgComponentElementProcessor(this._element);

  @override
  void apply(AngularHtmlUnitResolver resolver, ht.XmlTagNode node) {
    node.element = _element.selector;
    for (AngularPropertyElement property in _element.properties) {
      String name = property.name;
      ht.XmlAttributeNode attribute = node.getAttribute(name);
      if (attribute != null) {
        attribute.element = property;
        // resolve if binding
        if (property.propertyKind != AngularPropertyKind.ATTR) {
          AngularExpression expression = parseAngularExpression(resolver, attribute);
          resolver._resolveExpression(expression);
          setAngularExpression(attribute, expression);
        }
      }
    }
  }

  @override
  bool canApply(ht.XmlTagNode node) => _element.selector.apply(node);
}

/**
 * [NgControllerElementProcessor] applies [AngularControllerElement].
 */
class NgControllerElementProcessor extends NgProcessor {
  final AngularControllerElement _element;

  NgControllerElementProcessor(this._element);

  @override
  void apply(AngularHtmlUnitResolver resolver, ht.XmlTagNode node) {
    InterfaceType type = (_element.enclosingElement as ClassElement).type;
    String name = _element.name;
    LocalVariableElementImpl variable = resolver._createLocalVariableWithName(type, name);
    resolver._defineVariable(variable);
    variable.toolkitObjects = <AngularElement> [_element];
  }

  @override
  bool canApply(ht.XmlTagNode node) => _element.selector.apply(node);
}

/**
 * [NgDirectiveElementProcessor] applies [AngularDirectiveElement] by parsing mapped
 * attributes as expressions.
 */
class NgDirectiveElementProcessor extends NgDirectiveProcessor {
  final AngularDirectiveElement _element;

  NgDirectiveElementProcessor(this._element);

  @override
  void apply(AngularHtmlUnitResolver resolver, ht.XmlTagNode node) {
    String selectorAttributeName = null;
    {
      AngularSelectorElement selector = _element.selector;
      if (selector is HasAttributeSelectorElementImpl) {
        selectorAttributeName = selector.name;
        // resolve attribute expression
        ht.XmlAttributeNode attribute = node.getAttribute(selectorAttributeName);
        if (attribute != null) {
          attribute.element = selector;
        }
      }
    }
    //
    for (AngularPropertyElement property in _element.properties) {
      // prepare attribute name
      String name = property.name;
      if (name == ".") {
        name = selectorAttributeName;
      }
      // prepare attribute
      ht.XmlAttributeNode attribute = node.getAttribute(name);
      if (attribute == null) {
        continue;
      }
      // if not resolved as the selector, resolve as a property
      if (name != selectorAttributeName) {
        attribute.element = property;
      }
      // skip if attribute has no value
      if (!NgDirectiveProcessor.hasValue(attribute)) {
        continue;
      }
      // resolve if binding
      if (property.propertyKind != AngularPropertyKind.ATTR) {
        resolver._pushNameScope();
        try {
          _onNgEventDirective(resolver);
          AngularExpression expression = parseAngularExpression(resolver, attribute);
          resolver._resolveExpression(expression);
          setAngularExpression(attribute, expression);
        } finally {
          resolver._popNameScope();
        }
      }
    }
  }

  @override
  bool canApply(ht.XmlTagNode node) => _element.selector.apply(node);

  /**
   * Support for <code>$event</code> variable in <code>NgEventDirective</code>.
   */
  void _onNgEventDirective(AngularHtmlUnitResolver resolver) {
    if (_element.isClass("NgEventDirective")) {
      DartType dynamicType = resolver.typeProvider.dynamicType;
      resolver._defineVariable(resolver._createLocalVariableWithName(dynamicType, "\$event"));
    }
  }
}

/**
 * [NgDirectiveProcessor] describes any <code>NgDirective</code> annotation instance.
 */
abstract class NgDirectiveProcessor extends NgProcessor {
  static bool hasValue(ht.XmlAttributeNode attribute) {
    ht.Token valueToken = attribute.valueToken;
    return valueToken != null && !valueToken.isSynthetic;
  }

  static AngularRawXmlExpression newAngularRawXmlExpression(AngularExpression e) => new AngularRawXmlExpression(e);

  static ht.RawXmlExpression newRawXmlExpression(Expression e) => new ht.RawXmlExpression(e);

  AngularExpression parseAngularExpression(AngularHtmlUnitResolver resolver, ht.XmlAttributeNode attribute) {
    Token token = _scanAttribute(resolver, attribute);
    return resolver._parseAngularExpressionInToken(token);
  }

  Expression parseDartExpression(AngularHtmlUnitResolver resolver, ht.XmlAttributeNode attribute) {
    Token token = _scanAttribute(resolver, attribute);
    return resolver._parseDartExpressionInToken(token);
  }

  /**
   * Sets single [AngularExpression] for [XmlAttributeNode].
   */
  void setAngularExpression(ht.XmlAttributeNode attribute, AngularExpression expression) {
    _setExpression(attribute, newAngularRawXmlExpression(expression));
  }

  /**
   * Sets single [Expression] for [XmlAttributeNode].
   */
  void setExpression(ht.XmlAttributeNode attribute, Expression expression) {
    _setExpression(attribute, newRawXmlExpression(expression));
  }

  void setExpressions(ht.XmlAttributeNode attribute, List<ht.XmlExpression> xmlExpressions) {
    attribute.expressions = new List.from(xmlExpressions);
  }

  Token _scanAttribute(AngularHtmlUnitResolver resolver, ht.XmlAttributeNode attribute) {
    int offset = attribute.valueToken.offset + 1;
    String value = attribute.text;
    return resolver._scanDart(value, 0, value.length, offset);
  }

  void _setExpression(ht.XmlAttributeNode attribute, ht.XmlExpression xmlExpression) {
    attribute.expressions = <ht.XmlExpression> [xmlExpression];
  }
}

/**
 * [NgModelProcessor] describes built-in <code>NgModel</code> directive.
 */
class NgModelProcessor extends NgDirectiveProcessor {
  static String _NG_MODEL = "ng-model";

  static NgModelProcessor INSTANCE = new NgModelProcessor();

  @override
  void apply(AngularHtmlUnitResolver resolver, ht.XmlTagNode node) {
    ht.XmlAttributeNode attribute = node.getAttribute(_NG_MODEL);
    Expression expression = parseDartExpression(resolver, attribute);
    // identifiers have been already handled by "apply top"
    if (expression is SimpleIdentifier) {
      return;
    }
    // resolve
    resolver._resolveNode(expression);
    // remember expression
    setExpression(attribute, expression);
  }

  @override
  bool canApply(ht.XmlTagNode node) => node.getAttribute(_NG_MODEL) != null;

  /**
   * This method is used to define top-level [VariableElement]s for each "ng-model" with
   * simple identifier model.
   */
  void _applyTopDeclarations(AngularHtmlUnitResolver resolver, ht.XmlTagNode node) {
    ht.XmlAttributeNode attribute = node.getAttribute(_NG_MODEL);
    Expression expression = parseDartExpression(resolver, attribute);
    // if not identifier, then not a top-level model, delay until "apply"
    if (expression is! SimpleIdentifier) {
      return;
    }
    SimpleIdentifier identifier = expression as SimpleIdentifier;
    // define variable Element
    InterfaceType type = resolver.typeProvider.stringType;
    LocalVariableElementImpl element = resolver._createLocalVariableFromIdentifier(type, identifier);
    resolver._defineTopVariable(element);
    // remember expression
    identifier.staticElement = element;
    identifier.staticType = type;
    setExpression(attribute, identifier);
  }
}

/**
 * [NgProcessor] is used to apply an Angular feature.
 */
abstract class NgProcessor {
  /**
   * Applies this [NgProcessor] to the resolver.
   *
   * @param resolver the [AngularHtmlUnitResolver] to apply to, not `null`
   * @param node the [XmlTagNode] to apply within, not `null`
   */
  void apply(AngularHtmlUnitResolver resolver, ht.XmlTagNode node);

  /**
   * Checks if this processor can be applied to the given [XmlTagNode].
   *
   * @param node the [XmlTagNode] to check
   * @return `true` if this processor can be applied, or `false` otherwise
   */
  bool canApply(ht.XmlTagNode node);
}

/**
 * The abstract class `AnalysisTask` defines the behavior of objects used to perform an
 * analysis task.
 */
abstract class AnalysisTask {
  /**
   * The context in which the task is to be performed.
   */
  final InternalAnalysisContext context;

  /**
   * The exception that was thrown while performing this task, or `null` if the task completed
   * successfully.
   */
  AnalysisException _thrownException;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   */
  AnalysisTask(this.context);

  /**
   * Use the given visitor to visit this task.
   *
   * @param visitor the visitor that should be used to visit this task
   * @return the value returned by the visitor
   * @throws AnalysisException if the visitor throws the exception
   */
  accept(AnalysisTaskVisitor visitor);

  /**
   * Return the exception that was thrown while performing this task, or `null` if the task
   * completed successfully.
   *
   * @return the exception that was thrown while performing this task
   */
  AnalysisException get exception => _thrownException;

  /**
   * Perform this analysis task and use the given visitor to visit this task after it has completed.
   *
   * @param visitor the visitor used to visit this task after it has completed
   * @return the value returned by the visitor
   * @throws AnalysisException if the visitor throws the exception
   */
  Object perform(AnalysisTaskVisitor visitor) {
    try {
      _safelyPerform();
    } on AnalysisException catch (exception) {
      _thrownException = exception;
      AnalysisEngine.instance.logger.logInformation2("Task failed: ${taskDescription}", exception);
    }
    return accept(visitor);
  }

  @override
  String toString() => taskDescription;

  /**
   * Return a textual description of this task.
   *
   * @return a textual description of this task
   */
  String get taskDescription;

  /**
   * Perform this analysis task, protected by an exception handler.
   *
   * @throws AnalysisException if an exception occurs while performing the task
   */
  void internalPerform();

  /**
   * Perform this analysis task, ensuring that all exceptions are wrapped in an
   * [AnalysisException].
   *
   * @throws AnalysisException if any exception occurs while performing the task
   */
  void _safelyPerform() {
    try {
      internalPerform();
    } on AnalysisException catch (exception) {
      throw exception;
    } on JavaException catch (exception) {
      throw new AnalysisException.con3(exception);
    }
  }
}

/**
 * The interface `AnalysisTaskVisitor` defines the behavior of objects that can visit tasks.
 * While tasks are not structured in any interesting way, this class provides the ability to
 * dispatch to an appropriate method.
 */
abstract class AnalysisTaskVisitor<E> {
  /**
   * Visit a [GenerateDartErrorsTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitGenerateDartErrorsTask(GenerateDartErrorsTask task);

  /**
   * Visit a [GenerateDartHintsTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitGenerateDartHintsTask(GenerateDartHintsTask task);

  /**
   * Visit a [GetContentTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitGetContentTask(GetContentTask task);

  /**
   * Visit an [IncrementalAnalysisTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitIncrementalAnalysisTask(IncrementalAnalysisTask incrementalAnalysisTask);

  /**
   * Visit a [ParseDartTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitParseDartTask(ParseDartTask task);

  /**
   * Visit a [ParseHtmlTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitParseHtmlTask(ParseHtmlTask task);

  /**
   * Visit a [ResolveAngularComponentTemplateTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitResolveAngularComponentTemplateTask(ResolveAngularComponentTemplateTask task);

  /**
   * Visit a [ResolveAngularEntryHtmlTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitResolveAngularEntryHtmlTask(ResolveAngularEntryHtmlTask task);

  /**
   * Visit a [ResolveDartLibraryCycleTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitResolveDartLibraryCycleTask(ResolveDartLibraryCycleTask task);

  /**
   * Visit a [ResolveDartLibraryTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitResolveDartLibraryTask(ResolveDartLibraryTask task);

  /**
   * Visit a [ResolveDartUnitTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitResolveDartUnitTask(ResolveDartUnitTask task);

  /**
   * Visit a [ResolveHtmlTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitResolveHtmlTask(ResolveHtmlTask task);

  /**
   * Visit a [ScanDartTask].
   *
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  E visitScanDartTask(ScanDartTask task);
}

/**
 * Instances of the class `GenerateDartErrorsTask` generate errors and warnings for a single
 * Dart source.
 */
class GenerateDartErrorsTask extends AnalysisTask {
  /**
   * The source for which errors and warnings are to be produced.
   */
  final Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  final int modificationTime;

  /**
   * The compilation unit used to resolve the dependencies.
   */
  final CompilationUnit _unit;

  /**
   * The element model for the library containing the source.
   */
  final LibraryElement libraryElement;

  /**
   * The errors that were generated for the source.
   */
  List<AnalysisError> _errors;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param source the source for which errors and warnings are to be produced
   * @param modificationTime the time at which the contents of the source were last modified
   * @param unit the compilation unit used to resolve the dependencies
   * @param libraryElement the element model for the library containing the source
   */
  GenerateDartErrorsTask(InternalAnalysisContext context, this.source, this.modificationTime, this._unit, this.libraryElement) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitGenerateDartErrorsTask(this);

  /**
   * Return the errors that were generated for the source.
   *
   * @return the errors that were generated for the source
   */
  List<AnalysisError> get errors => _errors;

  @override
  String get taskDescription => "generate errors and warnings for ${source.fullName}";

  @override
  void internalPerform() {
    TimeCounter_TimeCounterHandle timeCounter = PerformanceStatistics.errors.start();
    try {
      RecordingErrorListener errorListener = new RecordingErrorListener();
      ErrorReporter errorReporter = new ErrorReporter(errorListener, source);
      TypeProvider typeProvider = context.typeProvider;
      //
      // Use the ConstantVerifier to verify the use of constants. This needs to happen before using
      // the ErrorVerifier because some error codes need the computed constant values.
      //
      ConstantVerifier constantVerifier = new ConstantVerifier(errorReporter, typeProvider);
      _unit.accept(constantVerifier);
      //
      // Use the ErrorVerifier to compute the rest of the errors.
      //
      ErrorVerifier errorVerifier = new ErrorVerifier(errorReporter, libraryElement, typeProvider, new InheritanceManager(libraryElement));
      _unit.accept(errorVerifier);
      _errors = errorListener.getErrorsForSource(source);
    } finally {
      timeCounter.stop();
    }
  }
}

/**
 * Instances of the class `GenerateDartHintsTask` generate hints for a single Dart library.
 */
class GenerateDartHintsTask extends AnalysisTask {
  /**
   * The compilation units that comprise the library, with the defining compilation unit appearing
   * first in the array.
   */
  final List<TimestampedData<CompilationUnit>> _units;

  /**
   * The element model for the library being analyzed.
   */
  final LibraryElement libraryElement;

  /**
   * A table mapping the sources that were analyzed to the hints that were generated for the
   * sources.
   */
  Map<Source, TimestampedData<List<AnalysisError>>> _hintMap;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param units the compilation units that comprise the library, with the defining compilation
   *          unit appearing first in the array
   * @param libraryElement the element model for the library being analyzed
   */
  GenerateDartHintsTask(InternalAnalysisContext context, this._units, this.libraryElement) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitGenerateDartHintsTask(this);

  /**
   * Return a table mapping the sources that were analyzed to the hints that were generated for the
   * sources, or `null` if the task has not been performed or if the analysis did not complete
   * normally.
   *
   * @return a table mapping the sources that were analyzed to the hints that were generated for the
   *         sources
   */
  Map<Source, TimestampedData<List<AnalysisError>>> get hintMap => _hintMap;

  @override
  String get taskDescription {
    Source librarySource = libraryElement.source;
    if (librarySource == null) {
      return "generate Dart hints for library without source";
    }
    return "generate Dart hints for ${librarySource.fullName}";
  }

  @override
  void internalPerform() {
    //
    // Gather the compilation units.
    //
    int unitCount = _units.length;
    List<CompilationUnit> compilationUnits = new List<CompilationUnit>(unitCount);
    for (int i = 0; i < unitCount; i++) {
      compilationUnits[i] = _units[i].data;
    }
    //
    // Analyze all of the units.
    //
    RecordingErrorListener errorListener = new RecordingErrorListener();
    HintGenerator hintGenerator = new HintGenerator(compilationUnits, context, errorListener);
    hintGenerator.generateForLibrary();
    //
    // Store the results.
    //
    _hintMap = new Map<Source, TimestampedData<List<AnalysisError>>>();
    for (int i = 0; i < unitCount; i++) {
      int modificationTime = _units[i].modificationTime;
      Source source = _units[i].data.element.source;
      List<AnalysisError> errors = errorListener.getErrorsForSource(source);
      _hintMap[source] = new TimestampedData<List<AnalysisError>>(modificationTime, errors);
    }
  }
}

/**
 * Instances of the class `GetContentTask` get the contents of a source.
 */
class GetContentTask extends AnalysisTask {
  /**
   * The source to be read.
   */
  final Source source;

  /**
   * A flag indicating whether this task is complete.
   */
  bool _complete = false;

  /**
   * The contents of the source.
   */
  String _content;

  /**
   * The time at which the contents of the source were last modified.
   */
  int _modificationTime = -1;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param contentData the time-stamped contents of the source
   */
  GetContentTask(InternalAnalysisContext context, this.source) : super(context) {
    if (source == null) {
      throw new IllegalArgumentException("Cannot get contents of null source");
    }
  }

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitGetContentTask(this);

  /**
   * Return the contents of the source, or `null` if the task has not completed or if there
   * was an exception while getting the contents.
   *
   * @return the contents of the source
   */
  String get content => _content;

  /**
   * Return the time at which the contents of the source that was parsed were last modified, or a
   * negative value if the task has not yet been performed or if an exception occurred.
   *
   * @return the time at which the contents of the source that was parsed were last modified
   */
  int get modificationTime => _modificationTime;

  /**
   * Return `true` if this task is complete. Unlike most tasks, this task is allowed to be
   * visited more than once in order to support asynchronous IO. If the task is not complete when it
   * is visited synchronously as part of the [AnalysisTask#perform]
   * method, it will be visited again, using the same visitor, when the IO operation has been
   * performed.
   *
   * @return `true` if this task is complete
   */
  bool get isComplete => _complete;

  @override
  String get taskDescription => "get contents of ${source.fullName}";

  @override
  void internalPerform() {
    _complete = true;
    try {
      TimestampedData<String> data = context.getContents(source);
      _content = data.data;
      _modificationTime = data.modificationTime;
    } on JavaException catch (exception) {
      throw new AnalysisException.con2("Could not get contents of ${source}", exception);
    }
  }
}

/**
 * Instances of the class `IncrementalAnalysisTask` incrementally update existing analysis.
 */
class IncrementalAnalysisTask extends AnalysisTask {
  /**
   * The information used to perform incremental analysis.
   */
  final IncrementalAnalysisCache cache;

  /**
   * The compilation unit that was produced by incrementally updating the existing unit.
   */
  CompilationUnit _updatedUnit;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param cache the incremental analysis cache used to perform the analysis
   */
  IncrementalAnalysisTask(InternalAnalysisContext context, this.cache) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitIncrementalAnalysisTask(this);

  /**
   * Return the compilation unit that was produced by incrementally updating the existing
   * compilation unit, or `null` if the task has not yet been performed, could not be
   * performed, or if an exception occurred.
   *
   * @return the compilation unit
   */
  CompilationUnit get compilationUnit => _updatedUnit;

  /**
   * Return the source that is to be incrementally analyzed.
   *
   * @return the source
   */
  Source get source => cache != null ? cache.source : null;

  @override
  String get taskDescription => "incremental analysis ${(cache != null ? cache.source : "null")}";

  @override
  void internalPerform() {
    if (cache == null) {
      return;
    }
    // Only handle small changes
    if (cache.oldLength > 0 || cache.newLength > 30) {
      return;
    }
    // Produce an updated token stream
    CharacterReader reader = new CharSequenceReader(cache.newContents);
    BooleanErrorListener errorListener = new BooleanErrorListener();
    IncrementalScanner scanner = new IncrementalScanner(cache.source, reader, errorListener);
    scanner.rescan(cache.resolvedUnit.beginToken, cache.offset, cache.oldLength, cache.newLength);
    if (errorListener.errorReported) {
      return;
    }
    // Produce an updated AST
    IncrementalParser parser = new IncrementalParser(cache.source, scanner.tokenMap, AnalysisErrorListener.NULL_LISTENER);
    _updatedUnit = parser.reparse(cache.resolvedUnit, scanner.leftToken, scanner.rightToken, cache.offset, cache.offset + cache.oldLength);
    // Update the resolution
    TypeProvider typeProvider = this.typeProvider;
    if (_updatedUnit != null && typeProvider != null) {
      CompilationUnitElement element = _updatedUnit.element;
      if (element != null) {
        LibraryElement library = element.library;
        if (library != null) {
          IncrementalResolver resolver = new IncrementalResolver(library, cache.source, typeProvider, errorListener);
          resolver.resolve(parser.updatedNode);
        }
      }
    }
  }

  /**
   * Return the type provider used for incremental resolution.
   *
   * @return the type provider (or `null` if an exception occurs)
   */
  TypeProvider get typeProvider {
    try {
      return context.typeProvider;
    } on AnalysisException catch (exception) {
      return null;
    }
  }
}

/**
 * Instances of the class `ParseDartTask` parse a specific source as a Dart file.
 */
class ParseDartTask extends AnalysisTask {
  /**
   * Return the result of resolving the URI of the given URI-based directive against the URI of the
   * given library, or `null` if the URI is not valid.
   *
   * @param context the context in which the resolution is to be performed
   * @param librarySource the source representing the library containing the directive
   * @param directive the directive which URI should be resolved
   * @param errorListener the error listener to which errors should be reported
   * @return the result of resolving the URI against the URI of the library
   */
  static Source resolveSource(AnalysisContext analysisContext, Source librarySource, UriBasedDirective directive, AnalysisErrorListener errorListener) {
    StringLiteral uriLiteral = directive.uri;
    if (uriLiteral is StringInterpolation) {
      errorListener.onError(new AnalysisError.con2(librarySource, uriLiteral.offset, uriLiteral.length, CompileTimeErrorCode.URI_WITH_INTERPOLATION, []));
      return null;
    }
    String uriContent = uriLiteral.stringValue.trim();
    directive.uriContent = uriContent;
    if (directive is ImportDirective && uriContent.startsWith(_DART_EXT_SCHEME)) {
      return null;
    }
    try {
      String encodedUriContent = Uri.encodeFull(uriContent);
      parseUriWithException(encodedUriContent);
      Source source = analysisContext.sourceFactory.resolveUri(librarySource, encodedUriContent);
      if (!analysisContext.exists(source)) {
        errorListener.onError(new AnalysisError.con2(librarySource, uriLiteral.offset, uriLiteral.length, CompileTimeErrorCode.URI_DOES_NOT_EXIST, [uriContent]));
      }
      directive.source = source;
      return source;
    } on URISyntaxException catch (exception) {
      errorListener.onError(new AnalysisError.con2(librarySource, uriLiteral.offset, uriLiteral.length, CompileTimeErrorCode.INVALID_URI, [uriContent]));
    }
    return null;
  }

  /**
   * The source to be parsed.
   */
  final Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  final int modificationTime;

  /**
   * The head of the token stream used for parsing.
   */
  final Token _tokenStream;

  /**
   * The line information associated with the source.
   */
  final LineInfo lineInfo;

  /**
   * The compilation unit that was produced by parsing the source.
   */
  CompilationUnit _unit;

  /**
   * A flag indicating whether the source contains a 'part of' directive.
   */
  bool _containsPartOfDirective = false;

  /**
   * A flag indicating whether the source contains any directive other than a 'part of' directive.
   */
  bool _containsNonPartOfDirective = false;

  /**
   * A set containing the sources referenced by 'export' directives.
   */
  Set<Source> _exportedSources = new Set<Source>();

  /**
   * A set containing the sources referenced by 'import' directives.
   */
  Set<Source> _importedSources = new Set<Source>();

  /**
   * A set containing the sources referenced by 'part' directives.
   */
  Set<Source> _includedSources = new Set<Source>();

  /**
   * The errors that were produced by scanning and parsing the source.
   */
  List<AnalysisError> _errors = AnalysisError.NO_ERRORS;

  /**
   * The prefix of a URI using the `dart-ext` scheme to reference a native code library.
   */
  static String _DART_EXT_SCHEME = "dart-ext:";

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param modificationTime the time at which the contents of the source were last modified
   * @param tokenStream the head of the token stream used for parsing
   * @param lineInfo the line information associated with the source
   */
  ParseDartTask(InternalAnalysisContext context, this.source, this.modificationTime, this._tokenStream, this.lineInfo) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitParseDartTask(this);

  /**
   * Return the compilation unit that was produced by parsing the source, or `null` if the
   * task has not yet been performed or if an exception occurred.
   *
   * @return the compilation unit that was produced by parsing the source
   */
  CompilationUnit get compilationUnit => _unit;

  /**
   * Return the errors that were produced by scanning and parsing the source, or an empty array if
   * the task has not yet been performed or if an exception occurred.
   *
   * @return the errors that were produced by scanning and parsing the source
   */
  List<AnalysisError> get errors => _errors;

  /**
   * Return an array containing the sources referenced by 'export' directives, or an empty array if
   * the task has not yet been performed or if an exception occurred.
   *
   * @return an array containing the sources referenced by 'export' directives
   */
  List<Source> get exportedSources => _toArray(_exportedSources);

  /**
   * Return an array containing the sources referenced by 'import' directives, or an empty array if
   * the task has not yet been performed or if an exception occurred.
   *
   * @return an array containing the sources referenced by 'import' directives
   */
  List<Source> get importedSources => _toArray(_importedSources);

  /**
   * Return an array containing the sources referenced by 'part' directives, or an empty array if
   * the task has not yet been performed or if an exception occurred.
   *
   * @return an array containing the sources referenced by 'part' directives
   */
  List<Source> get includedSources => _toArray(_includedSources);

  /**
   * Return `true` if the source contains any directive other than a 'part of' directive, or
   * `false` if the task has not yet been performed or if an exception occurred.
   *
   * @return `true` if the source contains any directive other than a 'part of' directive
   */
  bool get hasNonPartOfDirective => _containsNonPartOfDirective;

  /**
   * Return `true` if the source contains a 'part of' directive, or `false` if the task
   * has not yet been performed or if an exception occurred.
   *
   * @return `true` if the source contains a 'part of' directive
   */
  bool get hasPartOfDirective => _containsPartOfDirective;

  @override
  String get taskDescription {
    if (source == null) {
      return "parse as dart null source";
    }
    return "parse as dart ${source.fullName}";
  }

  @override
  void internalPerform() {
    //
    // Then parse the token stream.
    //
    TimeCounter_TimeCounterHandle timeCounterParse = PerformanceStatistics.parse.start();
    try {
      RecordingErrorListener errorListener = new RecordingErrorListener();
      Parser parser = new Parser(source, errorListener);
      parser.parseFunctionBodies = context.analysisOptions.analyzeFunctionBodies;
      _unit = parser.parseCompilationUnit(_tokenStream);
      _unit.lineInfo = lineInfo;
      AnalysisContext analysisContext = context;
      for (Directive directive in _unit.directives) {
        if (directive is PartOfDirective) {
          _containsPartOfDirective = true;
        } else {
          _containsNonPartOfDirective = true;
          if (directive is ExportDirective) {
            Source exportSource = resolveSource(analysisContext, source, directive, errorListener);
            if (exportSource != null) {
              _exportedSources.add(exportSource);
            }
          } else if (directive is ImportDirective) {
            Source importSource = resolveSource(analysisContext, source, directive, errorListener);
            if (importSource != null) {
              _importedSources.add(importSource);
            }
          } else if (directive is PartDirective) {
            Source partSource = resolveSource(analysisContext, source, directive, errorListener);
            if (partSource != null && partSource != source) {
              _includedSources.add(partSource);
            }
          }
        }
      }
      _errors = errorListener.getErrorsForSource(source);
    } finally {
      timeCounterParse.stop();
    }
  }

  /**
   * Efficiently convert the given set of sources to an array.
   *
   * @param sources the set to be converted
   * @return an array containing all of the sources in the given set
   */
  List<Source> _toArray(Set<Source> sources) {
    int size = sources.length;
    if (size == 0) {
      return Source.EMPTY_ARRAY;
    }
    return new List.from(sources);
  }
}

/**
 * Instances of the class `ParseHtmlTask` parse a specific source as an HTML file.
 */
class ParseHtmlTask extends AnalysisTask {
  /**
   * The source to be parsed.
   */
  final Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  final int modificationTime;

  /**
   * The contents of the source.
   */
  final String _content;

  /**
   * The line information that was produced.
   */
  LineInfo _lineInfo;

  /**
   * The HTML unit that was produced by parsing the source.
   */
  ht.HtmlUnit _unit;

  /**
   * The errors that were produced by scanning and parsing the source.
   */
  List<AnalysisError> _errors = AnalysisError.NO_ERRORS;

  /**
   * An array containing the sources of the libraries that are referenced within the HTML.
   */
  List<Source> _referencedLibraries = Source.EMPTY_ARRAY;

  /**
   * The name of the 'src' attribute in a HTML tag.
   */
  static String _ATTRIBUTE_SRC = "src";

  /**
   * The name of the 'script' tag in an HTML file.
   */
  static String _TAG_SCRIPT = "script";

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param modificationTime the time at which the contents of the source were last modified
   * @param content the contents of the source
   */
  ParseHtmlTask(InternalAnalysisContext context, this.source, this.modificationTime, this._content) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitParseHtmlTask(this);

  /**
   * Return the errors that were produced by scanning and parsing the source, or `null` if the
   * task has not yet been performed or if an exception occurred.
   *
   * @return the errors that were produced by scanning and parsing the source
   */
  List<AnalysisError> get errors => _errors;

  /**
   * Return the HTML unit that was produced by parsing the source.
   *
   * @return the HTML unit that was produced by parsing the source
   */
  ht.HtmlUnit get htmlUnit => _unit;

  /**
   * Return the line information that was produced, or `null` if the task has not yet been
   * performed or if an exception occurred.
   *
   * @return the line information that was produced
   */
  LineInfo get lineInfo => _lineInfo;

  /**
   * Return an array containing the sources of the libraries that are referenced within the HTML.
   *
   * @return the sources of the libraries that are referenced within the HTML
   */
  List<Source> get referencedLibraries => _referencedLibraries;

  @override
  String get taskDescription {
    if (source == null) {
      return "parse as html null source";
    }
    return "parse as html ${source.fullName}";
  }

  @override
  void internalPerform() {
    try {
      ht.AbstractScanner scanner = new ht.StringScanner(source, _content);
      scanner.passThroughElements = <String> [_TAG_SCRIPT];
      ht.Token token = scanner.tokenize();
      _lineInfo = new LineInfo(scanner.lineStarts);
      RecordingErrorListener errorListener = new RecordingErrorListener();
      _unit = new ht.HtmlParser(source, errorListener).parse(token, _lineInfo);
      _unit.accept(new RecursiveXmlVisitor_ParseHtmlTask_internalPerform(this, errorListener));
      _errors = errorListener.getErrorsForSource(source);
      _referencedLibraries = librarySources;
    } on JavaException catch (exception) {
      throw new AnalysisException.con3(exception);
    }
  }

  /**
   * Return the sources of libraries that are referenced in the specified HTML file.
   *
   * @return the sources of libraries that are referenced in the HTML file
   */
  List<Source> get librarySources {
    List<Source> libraries = new List<Source>();
    _unit.accept(new RecursiveXmlVisitor_ParseHtmlTask_getLibrarySources(this, libraries));
    if (libraries.isEmpty) {
      return Source.EMPTY_ARRAY;
    }
    return new List.from(libraries);
  }

  /**
   * Resolves directives in the given [CompilationUnit].
   */
  void _resolveScriptDirectives(CompilationUnit script, AnalysisErrorListener errorListener) {
    if (script == null) {
      return;
    }
    AnalysisContext analysisContext = context;
    for (Directive directive in script.directives) {
      if (directive is ExportDirective) {
        ParseDartTask.resolveSource(analysisContext, source, directive, errorListener);
      } else if (directive is ImportDirective) {
        ParseDartTask.resolveSource(analysisContext, source, directive, errorListener);
      } else if (directive is PartDirective) {
        ParseDartTask.resolveSource(analysisContext, source, directive, errorListener);
      }
    }
  }
}

class RecursiveXmlVisitor_ParseHtmlTask_internalPerform extends ht.RecursiveXmlVisitor<Object> {
  final ParseHtmlTask ParseHtmlTask_this;

  RecordingErrorListener errorListener;

  RecursiveXmlVisitor_ParseHtmlTask_internalPerform(this.ParseHtmlTask_this, this.errorListener) : super();

  @override
  Object visitHtmlScriptTagNode(ht.HtmlScriptTagNode node) {
    ParseHtmlTask_this._resolveScriptDirectives(node.script, errorListener);
    return null;
  }
}

class RecursiveXmlVisitor_ParseHtmlTask_getLibrarySources extends ht.RecursiveXmlVisitor<Object> {
  final ParseHtmlTask ParseHtmlTask_this;

  List<Source> libraries;

  RecursiveXmlVisitor_ParseHtmlTask_getLibrarySources(this.ParseHtmlTask_this, this.libraries) : super();

  @override
  Object visitHtmlScriptTagNode(ht.HtmlScriptTagNode node) {
    ht.XmlAttributeNode scriptAttribute = null;
    for (ht.XmlAttributeNode attribute in node.attributes) {
      if (javaStringEqualsIgnoreCase(attribute.name, ParseHtmlTask._ATTRIBUTE_SRC)) {
        scriptAttribute = attribute;
      }
    }
    if (scriptAttribute != null) {
      try {
        Uri uri = new Uri(path: scriptAttribute.text);
        String fileName = uri.path;
        Source librarySource = ParseHtmlTask_this.context.sourceFactory.resolveUri(ParseHtmlTask_this.source, fileName);
        if (ParseHtmlTask_this.context.exists(librarySource)) {
          libraries.add(librarySource);
        }
      } on URISyntaxException catch (e) {
      }
    }
    return super.visitHtmlScriptTagNode(node);
  }
}

/**
 * Instances of the class `ResolveAngularComponentTemplateTask` resolve HTML template
 * referenced by [AngularComponentElement].
 */
class ResolveAngularComponentTemplateTask extends AnalysisTask {
  /**
   * The source to be resolved.
   */
  final Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  final int modificationTime;

  /**
   * The HTML unit to be resolved.
   */
  final ht.HtmlUnit _unit;

  /**
   * The [AngularComponentElement] to resolve template for.
   */
  final AngularComponentElement _component;

  /**
   * The Angular application to resolve in context of.
   */
  final AngularApplication _application;

  /**
   * The [HtmlUnit] that was resolved by this task.
   */
  ht.HtmlUnit _resolvedUnit;

  /**
   * The resolution errors that were discovered while resolving the source.
   */
  List<AnalysisError> _resolutionErrors = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param source the source to be resolved
   * @param modificationTime the time at which the contents of the source were last modified
   * @param unit the HTML unit to be resolved
   * @param component the component that uses this HTML template, not `null`
   * @param application the Angular application to resolve in context of
   */
  ResolveAngularComponentTemplateTask(InternalAnalysisContext context, this.source, this.modificationTime, this._unit, this._component, this._application) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitResolveAngularComponentTemplateTask(this);

  List<AnalysisError> get resolutionErrors => _resolutionErrors;

  /**
   * Return the [HtmlUnit] that was resolved by this task.
   *
   * @return the [HtmlUnit] that was resolved by this task
   */
  ht.HtmlUnit get resolvedUnit => _resolvedUnit;

  @override
  String get taskDescription => "resolving Angular template ${source}";

  @override
  void internalPerform() {
    //
    // Prepare for resolution.
    //
    RecordingErrorListener errorListener = new RecordingErrorListener();
    LineInfo lineInfo = context.getLineInfo(source);
    //
    // Perform resolution.
    //
    if (_application != null) {
      AngularHtmlUnitResolver resolver = new AngularHtmlUnitResolver(context, errorListener, source, lineInfo, _unit);
      resolver.resolveComponentTemplate(_application, _component);
      _resolvedUnit = _unit;
    }
    //
    // Remember the errors.
    //
    _resolutionErrors = errorListener.getErrorsForSource(source);
  }
}

/**
 * Instances of the class `ResolveAngularEntryHtmlTask` resolve a specific HTML file as an
 * Angular entry point.
 */
class ResolveAngularEntryHtmlTask extends AnalysisTask {
  /**
   * The source to be resolved.
   */
  final Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  final int modificationTime;

  /**
   * The HTML unit to be resolved.
   */
  final ht.HtmlUnit _unit;

  /**
   * The listener to record errors.
   */
  RecordingErrorListener _errorListener = new RecordingErrorListener();

  /**
   * The [HtmlUnit] that was resolved by this task.
   */
  ht.HtmlUnit _resolvedUnit;

  /**
   * The element produced by resolving the source.
   */
  HtmlElement _element = null;

  /**
   * The Angular application to resolve in context of.
   */
  AngularApplication _application;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param source the source to be resolved
   * @param modificationTime the time at which the contents of the source were last modified
   * @param unit the HTML unit to be resolved
   */
  ResolveAngularEntryHtmlTask(InternalAnalysisContext context, this.source, this.modificationTime, this._unit) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitResolveAngularEntryHtmlTask(this);

  /**
   * Returns the [AngularApplication] for the Web application with this Angular entry point,
   * maybe `null` if not an Angular entry point.
   */
  AngularApplication get application => _application;

  HtmlElement get element => _element;

  /**
   * The resolution errors that were discovered while resolving the source.
   */
  List<AnalysisError> get entryErrors => _errorListener.getErrorsForSource(source);

  /**
   * Returns [AnalysisError]s recorded for the given [Source].
   */
  List<AnalysisError> getErrors(Source source) => _errorListener.getErrorsForSource(source);

  /**
   * Return the [HtmlUnit] that was resolved by this task.
   *
   * @return the [HtmlUnit] that was resolved by this task
   */
  ht.HtmlUnit get resolvedUnit => _resolvedUnit;

  @override
  String get taskDescription {
    if (source == null) {
      return "resolve as Angular entry point null source";
    }
    return "resolve as Angular entry point ${source.fullName}";
  }

  @override
  void internalPerform() {
    //
    // Prepare for resolution.
    //
    LineInfo lineInfo = context.getLineInfo(source);
    //
    // Try to resolve as an Angular entry point.
    //
    _application = new AngularHtmlUnitResolver(context, _errorListener, source, lineInfo, _unit).calculateAngularApplication();
    //
    // Perform resolution.
    //
    if (_application != null) {
      new AngularHtmlUnitResolver(context, _errorListener, source, lineInfo, _unit).resolveEntryPoint(_application);
    }
    //
    // Remember the resolved unit.
    //
    _resolvedUnit = _unit;
  }
}

/**
 * Instances of the class `ResolveDartLibraryTask` resolve a specific Dart library.
 */
class ResolveDartLibraryCycleTask extends AnalysisTask {
  /**
   * The source representing the file whose compilation unit is to be returned. TODO(brianwilkerson)
   * This should probably be removed, but is being left in for now to ease the transition.
   */
  final Source unitSource;

  /**
   * The source representing the library to be resolved.
   */
  final Source librarySource;

  /**
   * The libraries that are part of the cycle containing the library to be resolved.
   */
  final List<ResolvableLibrary> _librariesInCycle;

  /**
   * The library resolver holding information about the libraries that were resolved.
   */
  LibraryResolver2 _resolver;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param unitSource the source representing the file whose compilation unit is to be returned
   * @param librarySource the source representing the library to be resolved
   * @param librariesInCycle the libraries that are part of the cycle containing the library to be
   *          resolved
   */
  ResolveDartLibraryCycleTask(InternalAnalysisContext context, this.unitSource, this.librarySource, this._librariesInCycle) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitResolveDartLibraryCycleTask(this);

  /**
   * Return the library resolver holding information about the libraries that were resolved.
   *
   * @return the library resolver holding information about the libraries that were resolved
   */
  LibraryResolver2 get libraryResolver => _resolver;

  @override
  String get taskDescription {
    if (librarySource == null) {
      return "resolve library null source";
    }
    return "resolve library ${librarySource.fullName}";
  }

  @override
  void internalPerform() {
    _resolver = new LibraryResolver2(context);
    _resolver.resolveLibrary(librarySource, _librariesInCycle);
  }
}

/**
 * Instances of the class `ResolveDartLibraryTask` resolve a specific Dart library.
 */
class ResolveDartLibraryTask extends AnalysisTask {
  /**
   * The source representing the file whose compilation unit is to be returned.
   */
  final Source unitSource;

  /**
   * The source representing the library to be resolved.
   */
  final Source librarySource;

  /**
   * The library resolver holding information about the libraries that were resolved.
   */
  LibraryResolver _resolver;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param unitSource the source representing the file whose compilation unit is to be returned
   * @param librarySource the source representing the library to be resolved
   */
  ResolveDartLibraryTask(InternalAnalysisContext context, this.unitSource, this.librarySource) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitResolveDartLibraryTask(this);

  /**
   * Return the library resolver holding information about the libraries that were resolved.
   *
   * @return the library resolver holding information about the libraries that were resolved
   */
  LibraryResolver get libraryResolver => _resolver;

  @override
  String get taskDescription {
    if (librarySource == null) {
      return "resolve library null source";
    }
    return "resolve library ${librarySource.fullName}";
  }

  @override
  void internalPerform() {
    _resolver = new LibraryResolver(context);
    _resolver.resolveLibrary(librarySource, true);
  }
}

/**
 * Instances of the class `ResolveDartUnitTask` resolve a single Dart file based on a existing
 * element model.
 */
class ResolveDartUnitTask extends AnalysisTask {
  /**
   * The source that is to be resolved.
   */
  final Source source;

  /**
   * The element model for the library containing the source.
   */
  final LibraryElement _libraryElement;

  /**
   * The time at which the contents of the source were last modified.
   */
  int _modificationTime = -1;

  /**
   * The compilation unit that was resolved by this task.
   */
  CompilationUnit _resolvedUnit;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param libraryElement the element model for the library containing the source
   */
  ResolveDartUnitTask(InternalAnalysisContext context, this.source, this._libraryElement) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitResolveDartUnitTask(this);

  /**
   * Return the source for the library containing the source that is to be resolved.
   *
   * @return the source for the library containing the source that is to be resolved
   */
  Source get librarySource => _libraryElement.source;

  /**
   * Return the time at which the contents of the source that was parsed were last modified, or a
   * negative value if the task has not yet been performed or if an exception occurred.
   *
   * @return the time at which the contents of the source that was parsed were last modified
   */
  int get modificationTime => _modificationTime;

  /**
   * Return the compilation unit that was resolved by this task.
   *
   * @return the compilation unit that was resolved by this task
   */
  CompilationUnit get resolvedUnit => _resolvedUnit;

  @override
  String get taskDescription {
    Source librarySource = _libraryElement.source;
    if (librarySource == null) {
      return "resolve unit null source";
    }
    return "resolve unit ${librarySource.fullName}";
  }

  @override
  void internalPerform() {
    TypeProvider typeProvider = (_libraryElement.context as InternalAnalysisContext).typeProvider;
    ResolvableCompilationUnit resolvableUnit = context.computeResolvableCompilationUnit(source);
    _modificationTime = resolvableUnit.modificationTime;
    CompilationUnit unit = resolvableUnit.compilationUnit;
    if (unit == null) {
      throw new AnalysisException.con1("Internal error: computeResolvableCompilationUnit returned a value without a parsed Dart unit");
    }
    //
    // Resolve names in declarations.
    //
    new DeclarationResolver().resolve(unit, _find(_libraryElement, source));
    //
    // Resolve the type names.
    //
    RecordingErrorListener errorListener = new RecordingErrorListener();
    TypeResolverVisitor typeResolverVisitor = new TypeResolverVisitor.con2(_libraryElement, source, typeProvider, errorListener);
    unit.accept(typeResolverVisitor);
    //
    // Resolve the rest of the structure
    //
    InheritanceManager inheritanceManager = new InheritanceManager(_libraryElement);
    ResolverVisitor resolverVisitor = new ResolverVisitor.con2(_libraryElement, source, typeProvider, inheritanceManager, errorListener);
    unit.accept(resolverVisitor);
    // TODO (jwren) Move this logic/ loop into the ResolverVisitor and then make the reportError protected again.
    for (ProxyConditionalAnalysisError conditionalCode in resolverVisitor.proxyConditionalAnalysisErrors) {
      if (conditionalCode.shouldIncludeErrorCode()) {
        resolverVisitor.reportError(conditionalCode.analysisError);
      }
    }
    //
    // Perform additional error checking.
    //
    TimeCounter_TimeCounterHandle counterHandleErrors = PerformanceStatistics.errors.start();
    try {
      ErrorReporter errorReporter = new ErrorReporter(errorListener, source);
      ErrorVerifier errorVerifier = new ErrorVerifier(errorReporter, _libraryElement, typeProvider, inheritanceManager);
      unit.accept(errorVerifier);
      ConstantVerifier constantVerifier = new ConstantVerifier(errorReporter, typeProvider);
      unit.accept(constantVerifier);
    } finally {
      counterHandleErrors.stop();
    }
    //
    // Capture the results.
    //
    _resolvedUnit = unit;
  }

  /**
   * Search the compilation units that are part of the given library and return the element
   * representing the compilation unit with the given source. Return `null` if there is no
   * such compilation unit.
   *
   * @param libraryElement the element representing the library being searched through
   * @param unitSource the source for the compilation unit whose element is to be returned
   * @return the element representing the compilation unit
   */
  CompilationUnitElement _find(LibraryElement libraryElement, Source unitSource) {
    CompilationUnitElement element = libraryElement.definingCompilationUnit;
    if (element.source == unitSource) {
      return element;
    }
    for (CompilationUnitElement partElement in libraryElement.parts) {
      if (partElement.source == unitSource) {
        return partElement;
      }
    }
    return null;
  }
}

/**
 * Instances of the class `ResolveHtmlTask` resolve a specific source as an HTML file.
 */
class ResolveHtmlTask extends AnalysisTask {
  /**
   * The source to be resolved.
   */
  final Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  final int modificationTime;

  /**
   * The HTML unit to be resolved.
   */
  final ht.HtmlUnit _unit;

  /**
   * The [HtmlUnit] that was resolved by this task.
   */
  ht.HtmlUnit _resolvedUnit;

  /**
   * The element produced by resolving the source.
   */
  HtmlElement _element = null;

  /**
   * The resolution errors that were discovered while resolving the source.
   */
  List<AnalysisError> _resolutionErrors = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param source the source to be resolved
   * @param modificationTime the time at which the contents of the source were last modified
   * @param unit the HTML unit to be resolved
   */
  ResolveHtmlTask(InternalAnalysisContext context, this.source, this.modificationTime, this._unit) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitResolveHtmlTask(this);

  HtmlElement get element => _element;

  List<AnalysisError> get resolutionErrors => _resolutionErrors;

  /**
   * Return the [HtmlUnit] that was resolved by this task.
   *
   * @return the [HtmlUnit] that was resolved by this task
   */
  ht.HtmlUnit get resolvedUnit => _resolvedUnit;

  @override
  String get taskDescription {
    if (source == null) {
      return "resolve as html null source";
    }
    return "resolve as html ${source.fullName}";
  }

  @override
  void internalPerform() {
    //
    // Build the standard HTML element.
    //
    HtmlUnitBuilder builder = new HtmlUnitBuilder(context);
    _element = builder.buildHtmlElement(source, modificationTime, _unit);
    RecordingErrorListener errorListener = builder.errorListener;
    //
    // Record all resolution errors.
    //
    _resolutionErrors = errorListener.getErrorsForSource(source);
    //
    // Remember the resolved unit.
    //
    _resolvedUnit = _unit;
  }
}

/**
 * Instances of the class `ScanDartTask` scan a specific source as a Dart file.
 */
class ScanDartTask extends AnalysisTask {
  /**
   * The source to be scanned.
   */
  final Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  final int modificationTime;

  /**
   * The contents of the source.
   */
  final String _content;

  /**
   * The token stream that was produced by scanning the source.
   */
  Token _tokenStream;

  /**
   * The line information that was produced.
   */
  LineInfo _lineInfo;

  /**
   * The errors that were produced by scanning the source.
   */
  List<AnalysisError> _errors = AnalysisError.NO_ERRORS;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   *
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param modificationTime the time at which the contents of the source were last modified
   * @param content the contents of the source
   */
  ScanDartTask(InternalAnalysisContext context, this.source, this.modificationTime, this._content) : super(context);

  @override
  accept(AnalysisTaskVisitor visitor) => visitor.visitScanDartTask(this);

  /**
   * Return the errors that were produced by scanning the source, or `null` if the task has
   * not yet been performed or if an exception occurred.
   *
   * @return the errors that were produced by scanning the source
   */
  List<AnalysisError> get errors => _errors;

  /**
   * Return the line information that was produced, or `null` if the task has not yet been
   * performed or if an exception occurred.
   *
   * @return the line information that was produced
   */
  LineInfo get lineInfo => _lineInfo;

  /**
   * Return the token stream that was produced by scanning the source, or `null` if the task
   * has not yet been performed or if an exception occurred.
   *
   * @return the token stream that was produced by scanning the source
   */
  Token get tokenStream => _tokenStream;

  @override
  String get taskDescription {
    if (source == null) {
      return "scan as dart null source";
    }
    return "scan as dart ${source.fullName}";
  }

  @override
  void internalPerform() {
    RecordingErrorListener errorListener = new RecordingErrorListener();
    TimeCounter_TimeCounterHandle timeCounterScan = PerformanceStatistics.scan.start();
    try {
      Scanner scanner = new Scanner(source, new CharSequenceReader(_content), errorListener);
      scanner.preserveComments = context.analysisOptions.preserveComments;
      _tokenStream = scanner.tokenize();
      _lineInfo = new LineInfo(scanner.lineStarts);
      _errors = errorListener.getErrorsForSource(source);
    } on JavaException catch (exception) {
      throw new AnalysisException.con3(exception);
    } finally {
      timeCounterScan.stop();
    }
  }
}

/**
 * The unique instances of the class `WaitForAsyncTask` represents a state in which there is
 * no analysis work that can be done until some asynchronous task (such as IO) has completed, but
 * where analysis is not yet complete.
 */
class WaitForAsyncTask extends AnalysisTask {
  /**
   * The unique instance of this class.
   */
  static WaitForAsyncTask _UniqueInstance = new WaitForAsyncTask();

  /**
   * Return the unique instance of this class.
   *
   * @return the unique instance of this class
   */
  static WaitForAsyncTask get instance => _UniqueInstance;

  /**
   * Prevent the creation of instances of this class.
   */
  WaitForAsyncTask() : super(null);

  @override
  accept(AnalysisTaskVisitor visitor) => null;

  @override
  String get taskDescription => "Waiting for async analysis";

  @override
  void internalPerform() {
  }
}

/**
 * The interface `Logger` defines the behavior of objects that can be used to receive
 * information about errors within the analysis engine. Implementations usually write this
 * information to a file, but can also record the information for later use (such as during testing)
 * or even ignore the information.
 */
abstract class Logger {
  static final Logger NULL = new Logger_NullLogger();

  /**
   * Log the given message as an error.
   *
   * @param message an explanation of why the error occurred or what it means
   */
  void logError(String message);

  /**
   * Log the given exception as one representing an error.
   *
   * @param message an explanation of why the error occurred or what it means
   * @param exception the exception being logged
   */
  void logError2(String message, Exception exception);

  /**
   * Log the given informational message.
   *
   * @param message an explanation of why the error occurred or what it means
   * @param exception the exception being logged
   */
  void logInformation(String message);

  /**
   * Log the given exception as one representing an informational message.
   *
   * @param message an explanation of why the error occurred or what it means
   * @param exception the exception being logged
   */
  void logInformation2(String message, Exception exception);
}

/**
 * Implementation of [Logger] that does nothing.
 */
class Logger_NullLogger implements Logger {
  @override
  void logError(String message) {
  }

  @override
  void logError2(String message, Exception exception) {
  }

  @override
  void logInformation(String message) {
  }

  @override
  void logInformation2(String message, Exception exception) {
  }
}
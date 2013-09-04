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
package com.google.dart.engine.internal.context;

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.AnnotatedNode;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisContentStatistics;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.html.parser.HtmlParseResult;
import com.google.dart.engine.html.parser.HtmlParser;
import com.google.dart.engine.html.scanner.HtmlScanResult;
import com.google.dart.engine.html.scanner.HtmlScanner;
import com.google.dart.engine.internal.builder.HtmlUnitBuilder;
import com.google.dart.engine.internal.cache.CacheState;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DartEntryImpl;
import com.google.dart.engine.internal.cache.DataDescriptor;
import com.google.dart.engine.internal.cache.HtmlEntry;
import com.google.dart.engine.internal.cache.HtmlEntryImpl;
import com.google.dart.engine.internal.cache.SourceEntry;
import com.google.dart.engine.internal.cache.SourceEntryImpl;
import com.google.dart.engine.internal.element.ElementImpl;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.DeclarationResolver;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.internal.resolver.Library;
import com.google.dart.engine.internal.resolver.LibraryResolver;
import com.google.dart.engine.internal.resolver.ProxyConditionalAnalysisError;
import com.google.dart.engine.internal.resolver.ResolverVisitor;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.resolver.TypeProviderImpl;
import com.google.dart.engine.internal.resolver.TypeResolverVisitor;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.verifier.ConstantVerifier;
import com.google.dart.engine.internal.verifier.ErrorVerifier;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharBufferScanner;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.collection.ListUtilities;
import com.google.dart.engine.utilities.io.UriUtilities;
import com.google.dart.engine.utilities.os.OSUtilities;
import com.google.dart.engine.utilities.source.LineInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instances of the class {@code AnalysisContextImpl} implement an {@link AnalysisContext analysis
 * context}.
 * 
 * @coverage dart.engine
 */
public class AnalysisContextImpl implements InternalAnalysisContext {
  /**
   * The interface {@code AnalysisTask} defines the behavior of objects used to perform an analysis
   * task.
   */
  private interface AnalysisTask {
    /**
     * Perform a single analysis task. Implementors should assume that the cache is not locked.
     */
    public void perform();
  }

  /**
   * Instances of the class {@code ParseDartTask} parse a specific source as a Dart file.
   */
  private class ParseDartTask implements AnalysisTask {
    /**
     * The source to be parsed.
     */
    private Source source;

    /**
     * Initialize a newly created task to parse the given source as a Dart file.
     * 
     * @param source the source to be resolved
     */
    public ParseDartTask(Source source) {
      this.source = source;
    }

    @Override
    public void perform() {
      try {
        internalParseDart(source);
      } catch (AnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not parse " + source.getFullName(),
            exception);
      }
    }
  }

  /**
   * Instances of the class {@code ParseHtmlTask} parse a specific source as an HTML file.
   */
  private class ParseHtmlTask implements AnalysisTask {
    /**
     * The source to be parsed.
     */
    private Source source;

    /**
     * Initialize a newly created task to parse the given source as an HTML file.
     * 
     * @param source the source to be resolved
     */
    public ParseHtmlTask(Source source) {
      this.source = source;
    }

    @Override
    public void perform() {
      try {
        internalParseHtml(source);
      } catch (AnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not parse " + source.getFullName(),
            exception);
      }
    }
  }

  /**
   * Instances of the class {@code ResolveDartLibraryTask} resolve a specific source as a Dart
   * library.
   */
  private class ResolveDartLibraryTask implements AnalysisTask {
    /**
     * The source to be resolved.
     */
    private Source source;

    /**
     * Initialize a newly created task to resolve the given source as a Dart file.
     * 
     * @param source the source to be resolved
     */
    public ResolveDartLibraryTask(Source source) {
      this.source = source;
    }

    @Override
    public void perform() {
      try {
        computeLibraryElement(source);
      } catch (AnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not resolve " + source.getFullName(),
            exception);
      }
    }
  }

  /**
   * Instances of the class {@code ResolveDartUnitTask} resolve a specific source as a Dart file
   * within a library.
   */
  private class ResolveDartUnitTask implements AnalysisTask {
    /**
     * The source to be resolved.
     */
    private Source unitSource;

    /**
     * The source of the library in which the source is to be resolved.
     */
    private Source librarySource;

    /**
     * Initialize a newly created task to resolve the given source as a Dart file.
     * 
     * @param unitSource the source to be resolved
     * @param librarySource the source of the library in which the source is to be resolved
     */
    public ResolveDartUnitTask(Source unitSource, Source librarySource) {
      this.unitSource = unitSource;
      this.librarySource = librarySource;
    }

    @Override
    public void perform() {
      try {
        resolveCompilationUnit(unitSource, librarySource);
      } catch (AnalysisException exception) {
        DartEntryImpl dartCopy = getReadableDartEntry(unitSource).getWritableCopy();
        dartCopy.recordResolutionError();
        sourceMap.put(unitSource, dartCopy);
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not resolve " + unitSource.getFullName() + " in " + librarySource.getFullName(),
            exception);
      }
    }
  }

  /**
   * Instances of the class {@code ResolveHtmlTask} resolve a specific source as an HTML file.
   */
  private class ResolveHtmlTask implements AnalysisTask {
    /**
     * The source to be resolved.
     */
    private Source source;

    /**
     * Initialize a newly created task to resolve the given source as an HTML file.
     * 
     * @param source the source to be resolved
     */
    public ResolveHtmlTask(Source source) {
      this.source = source;
    }

    @Override
    public void perform() {
      try {
        computeHtmlElement(source);
      } catch (AnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not resolve " + source.getFullName(),
            exception);
      }
    }
  }

  /**
   * Instances of the class {@code ScanResult} represent the results of scanning a source.
   */
  private static class ScanResult {
    /**
     * The time at which the contents of the source were last modified.
     */
    private long modificationTime;

    /**
     * The first token in the token stream.
     */
    private Token token;

    /**
     * The line start information that was produced.
     */
    private int[] lineStarts;

    /**
     * Initialize a newly created result object to be empty.
     */
    private ScanResult() {
      super();
    }

    /**
     * Return the time at which the contents of the source were last modified.
     * 
     * @return the time at which the contents of the source were last modified
     */
    public long getModificationTime() {
      return modificationTime;
    }
  }

  /**
   * Helper for {@link #getStatistics()}, puts the library-specific state into the given statistics
   * object.
   */
  private static void putStatCacheItem(AnalysisContentStatisticsImpl statistics,
      DartEntry dartEntry, Source librarySource, DataDescriptor<?> key) {
    statistics.putCacheItem(key, dartEntry.getState(key, librarySource));
  }

  /**
   * Helper for {@link #getStatistics()}, puts the library independent state into the given
   * statistics object.
   */
  private static void putStatCacheItem(AnalysisContentStatisticsImpl statistics, SourceEntry entry,
      DataDescriptor<?> key) {
    statistics.putCacheItem(key, entry.getState(key));
  }

  /**
   * The set of analysis options controlling the behavior of this context.
   */
  private AnalysisOptions options = new AnalysisOptionsImpl();

  /**
   * The source factory used to create the sources that can be analyzed in this context.
   */
  private SourceFactory sourceFactory;

  /**
   * A table mapping the sources known to the context to the information known about the source.
   */
  private final HashMap<Source, SourceEntry> sourceMap = new HashMap<Source, SourceEntry>();

  /**
   * An array containing the order in which sources will be analyzed by the method
   * {@link #performAnalysisTask()}.
   */
  private Source[] priorityOrder = Source.EMPTY_ARRAY;

  /**
   * A table mapping sources to the change notices that are waiting to be returned related to that
   * source.
   */
  private HashMap<Source, ChangeNoticeImpl> pendingNotices = new HashMap<Source, ChangeNoticeImpl>();

  /**
   * A list containing the most recently accessed sources with the most recently used at the end of
   * the list. When more sources are added than the maximum allowed then the least recently used
   * source will be removed and will have it's cached AST structure flushed.
   */
  private ArrayList<Source> recentlyUsed = new ArrayList<Source>(MAX_CACHE_SIZE);

  /**
   * The number of times that the flushing of information from the cache has been disabled without
   * being re-enabled.
   */
  private int cacheRemovalCount = 0;

  /**
   * The object used to synchronize access to all of the caches.
   */
  private Object cacheLock = new Object();

  /**
   * The maximum number of sources for which data should be kept in the cache.
   */
  private static final int MAX_CACHE_SIZE = 64;

  /**
   * The maximum number of sources that can be on the priority list. This <b>must</b> be less than
   * the {@link #MAX_CACHE_SIZE} in order to prevent an infinite loop in performAnalysisTask().
   * 
   * @see #setAnalysisPriorityOrder(List)
   */
  private static final int MAX_PRIORITY_LIST_SIZE = MAX_CACHE_SIZE - 4;

  /**
   * The name of the 'src' attribute in a HTML tag.
   */
  private static final String ATTRIBUTE_SRC = "src";

  /**
   * The name of the 'type' attribute in a HTML tag.
   */
  private static final String ATTRIBUTE_TYPE = "type";

  /**
   * The name of the 'script' tag in an HTML file.
   */
  private static final String TAG_SCRIPT = "script";

  /**
   * The value of the 'type' attribute of a 'script' tag that indicates that the script is written
   * in Dart.
   */
  private static final String TYPE_DART = "application/dart";

  /**
   * Initialize a newly created analysis context.
   */
  public AnalysisContextImpl() {
    super();
    if (AnalysisEngine.getInstance().getUseExperimentalContext()) {
      throw new RuntimeException("Should not be creating an instance of AnalysisContextImpl");
    }
  }

  @Override
  public void addSourceInfo(Source source, SourceEntry info) {
    // This implementation assumes that the access to sourceMap does not need to be synchronized
    // because no other object can have access to this context while this method is being invoked.
    sourceMap.put(source, info);
  }

  @Override
  public void applyChanges(ChangeSet changeSet) {
    if (changeSet.isEmpty()) {
      return;
    }
    synchronized (cacheLock) {
      //
      // First, compute the list of sources that have been removed.
      //
      ArrayList<Source> removedSources = new ArrayList<Source>(changeSet.getRemoved());
      for (SourceContainer container : changeSet.getRemovedContainers()) {
        addSourcesInContainer(removedSources, container);
      }
      //
      // Then determine which cached results are no longer valid.
      //
      boolean addedDartSource = false;
      for (Source source : changeSet.getAdded()) {
        if (sourceAvailable(source)) {
          addedDartSource = true;
        }
      }
      for (Source source : changeSet.getChanged()) {
        sourceChanged(source);
      }
      for (Source source : removedSources) {
        sourceRemoved(source);
      }
      if (addedDartSource) {
        // TODO(brianwilkerson) This is hugely inefficient, but we need to re-analyze any libraries
        // that might have been referencing the not-yet-existing source that was just added. Longer
        // term we need to keep track of which libraries are referencing non-existing sources and
        // only re-analyze those libraries.
        for (Map.Entry<Source, SourceEntry> mapEntry : sourceMap.entrySet()) {
          if (!mapEntry.getKey().isInSystemLibrary() && mapEntry.getValue() instanceof DartEntry) {
            DartEntryImpl dartCopy = ((DartEntry) mapEntry.getValue()).getWritableCopy();
            dartCopy.invalidateAllResolutionInformation();
            mapEntry.setValue(dartCopy);
          }
        }
      }
    }
  }

  @Override
  public String computeDocumentationComment(Element element) throws AnalysisException {
    if (element == null) {
      return null;
    }
    Source source = element.getSource();
    if (source == null) {
      return null;
    }
    CompilationUnit unit = parseCompilationUnit(source);
    if (unit == null) {
      return null;
    }
    NodeLocator locator = new NodeLocator(element.getNameOffset());
    ASTNode nameNode = locator.searchWithin(unit);
    while (nameNode != null) {
      if (nameNode instanceof AnnotatedNode) {
        Comment comment = ((AnnotatedNode) nameNode).getDocumentationComment();
        if (comment == null) {
          return null;
        }
        StringBuilder builder = new StringBuilder();
        Token[] tokens = comment.getTokens();
        for (int i = 0; i < tokens.length; i++) {
          if (i > 0) {
            builder.append(OSUtilities.LINE_SEPARATOR);
          }
          builder.append(tokens[i].getLexeme());
        }
        return builder.toString();
      }
      nameNode = nameNode.getParent();
    }
    return null;
  }

  @Override
  public AnalysisError[] computeErrors(Source source) throws AnalysisException {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry instanceof DartEntry) {
      ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
      DartEntry dartEntry = (DartEntry) sourceEntry;
      ListUtilities.addAll(
          errors,
          internalGetDartParseData(source, dartEntry, DartEntry.PARSE_ERRORS));
      if (dartEntry.getValue(DartEntry.SOURCE_KIND) == SourceKind.LIBRARY) {
        ListUtilities.addAll(
            errors,
            internalGetDartResolutionData(source, source, dartEntry, DartEntry.RESOLUTION_ERRORS));
      } else {
        Source[] libraries = getLibrariesContaining(source);
        for (Source librarySource : libraries) {
          ListUtilities.addAll(
              errors,
              internalGetDartResolutionData(
                  source,
                  librarySource,
                  dartEntry,
                  DartEntry.RESOLUTION_ERRORS));
        }
      }
      // TODO(brianwilkerson) Gather other kinds of errors when their generation is moved out of
      // resolution into separate phases.
      //addAll(errors, internalGetDartHintData(source, dartEntry, DartEntry.HINTS));
      if (errors.isEmpty()) {
        return AnalysisError.NO_ERRORS;
      }
      return errors.toArray(new AnalysisError[errors.size()]);
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      return internalGetHtmlResolutionData(
          source,
          htmlEntry,
          HtmlEntry.RESOLUTION_ERRORS,
          AnalysisError.NO_ERRORS);
    }
    return AnalysisError.NO_ERRORS;
  }

  @Override
  public Source[] computeExportedLibraries(Source source) throws AnalysisException {
    return internalGetDartParseData(source, DartEntry.EXPORTED_LIBRARIES, Source.EMPTY_ARRAY);
  }

  @Override
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      return null;
    }
    CacheState elementState = htmlEntry.getState(HtmlEntry.ELEMENT);
    if (elementState != CacheState.ERROR && elementState != CacheState.VALID) {
      htmlEntry = internalResolveHtml(source);
    }
    return htmlEntry.getValue(HtmlEntry.ELEMENT);
  }

  @Override
  public Source[] computeImportedLibraries(Source source) throws AnalysisException {
    return internalGetDartParseData(source, DartEntry.IMPORTED_LIBRARIES, Source.EMPTY_ARRAY);
  }

  @Override
  public SourceKind computeKindOf(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry == null) {
      return SourceKind.UNKNOWN;
    } else if (sourceEntry instanceof DartEntry) {
      try {
        return internalGetDartParseData(source, (DartEntry) sourceEntry, DartEntry.SOURCE_KIND);
      } catch (AnalysisException exception) {
        return SourceKind.UNKNOWN;
      }
    }
    return sourceEntry.getKind();
  }

  @Override
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
    synchronized (cacheLock) {
      DartEntry dartEntry = getDartEntry(source);
      if (dartEntry == null) {
        return null;
      }
      LibraryElement element = dartEntry.getValue(DartEntry.ELEMENT);
      if (element == null) {
        LibraryResolver resolver = new LibraryResolver(this);
        try {
          element = resolver.resolveLibrary(source, true);
          recordResolutionResults(resolver);
        } catch (AnalysisException exception) {
          DartEntryImpl dartCopy = getDartEntry(source).getWritableCopy();
          dartCopy.recordResolutionError();
          sourceMap.put(source, dartCopy);
          AnalysisEngine.getInstance().getLogger().logError(
              "Could not resolve the library " + source.getFullName(),
              exception);
        }
      }
      return element;
    }
  }

  @Override
  public LineInfo computeLineInfo(Source source) throws AnalysisException {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry instanceof HtmlEntry) {
      return internalGetHtmlParseData(source, SourceEntry.LINE_INFO, null);
    } else if (sourceEntry instanceof DartEntry) {
      return internalGetDartParseData(source, SourceEntry.LINE_INFO, null);
    }
    return null;
  }

  @Override
  public ResolvableCompilationUnit computeResolvableCompilationUnit(Source source)
      throws AnalysisException {
    while (true) {
      synchronized (cacheLock) {
        SourceEntry sourceEntry = getSourceEntry(source);
        if (!(sourceEntry instanceof DartEntry)) {
          throw new AnalysisException("computeResolvableCompilationUnit for non-Dart: "
              + source.getFullName());
        }
        DartEntry dartEntry = (DartEntry) sourceEntry;
        if (dartEntry.getState(DartEntry.PARSED_UNIT) == CacheState.ERROR) {
          throw new AnalysisException(
              "Internal error: computeResolvableCompilationUnit could not parse "
                  + source.getFullName());
        }
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        CompilationUnit unit = dartCopy.getResolvableCompilationUnit();
        if (unit != null) {
          sourceMap.put(source, dartCopy);
          return new ResolvableCompilationUnit(dartCopy.getModificationTime(), unit);
        }
      }
      internalParseDart(source);
    }
  }

  @Override
  public ResolvableHtmlUnit computeResolvableHtmlUnit(Source source) throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      throw new AnalysisException("computeResolvableHtmlUnit invoked for non-HTML file: "
          + source.getFullName());
    }
    htmlEntry = internalCacheHtmlParseData(source, htmlEntry, HtmlEntry.PARSED_UNIT);
    HtmlUnit unit = htmlEntry.getValue(HtmlEntry.PARSED_UNIT);
    if (unit == null) {
      throw new AnalysisException("Internal error: computeResolvableHtmlUnit could not parse "
          + source.getFullName());
    }
    // If the unit is ever modified by resolution then we will need to create a copy of it.
    return new ResolvableHtmlUnit(htmlEntry.getModificationTime(), unit);
  }

  @Override
  public AnalysisContext extractContext(SourceContainer container) {
    return extractContextInto(
        container,
        (InternalAnalysisContext) AnalysisEngine.getInstance().createAnalysisContext());
  }

  @Override
  public InternalAnalysisContext extractContextInto(SourceContainer container,
      InternalAnalysisContext newContext) {
    ArrayList<Source> sourcesToRemove = new ArrayList<Source>();
    synchronized (cacheLock) {
      // Move sources in the specified directory to the new context
      for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
        Source source = entry.getKey();
        if (container.contains(source)) {
          sourcesToRemove.add(source);
          newContext.addSourceInfo(source, entry.getValue().getWritableCopy());
        }
      }

      // TODO (danrubel): Either remove sources or adjust contract described in AnalysisContext.
      // Currently, callers assume that sources have been removed from this context

//      for (Source source : sourcesToRemove) {
//        // TODO(brianwilkerson) Determine whether the source should be removed (that is, whether
//        // there are no additional dependencies on the source), and if so remove all information
//        // about the source.
//        sourceMap.remove(source);
//        parseCache.remove(source);
//        publicNamespaceCache.remove(source);
//        libraryElementCache.remove(source);
//      }
    }

    return newContext;
  }

  @Override
  public AnalysisOptions getAnalysisOptions() {
    return options;
  }

  @Override
  public Element getElement(ElementLocation location) {
    // TODO(brianwilkerson) This should not be a "get" method.
    try {
      String[] components = ((ElementLocationImpl) location).getComponents();
      Source librarySource = computeSourceFromEncoding(components[0]);
      ElementImpl element = (ElementImpl) computeLibraryElement(librarySource);
      for (int i = 1; i < components.length; i++) {
        if (element == null) {
          return null;
        }
        element = element.getChild(components[i]);
      }
      return element;
    } catch (AnalysisException exception) {
      return null;
    }
  }

  @Override
  public AnalysisErrorInfo getErrors(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      return new AnalysisErrorInfoImpl(
          dartEntry.getAllErrors(),
          dartEntry.getValue(SourceEntry.LINE_INFO));
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      return new AnalysisErrorInfoImpl(
          htmlEntry.getAllErrors(),
          htmlEntry.getValue(SourceEntry.LINE_INFO));
    }
    return new AnalysisErrorInfoImpl(AnalysisError.NO_ERRORS, null);
  }

  @Override
  public HtmlElement getHtmlElement(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry instanceof HtmlEntry) {
      return ((HtmlEntry) sourceEntry).getValue(HtmlEntry.ELEMENT);
    }
    return null;
  }

  @Override
  public Source[] getHtmlFilesReferencing(Source source) {
    SourceKind sourceKind = getKindOf(source);
    if (sourceKind == null) {
      return Source.EMPTY_ARRAY;
    }
    synchronized (cacheLock) {
      ArrayList<Source> htmlSources = new ArrayList<Source>();
      switch (sourceKind) {
        case LIBRARY:
        default:
          for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
            SourceEntry sourceEntry = entry.getValue();
            if (sourceEntry.getKind() == SourceKind.HTML) {
              Source[] referencedLibraries = ((HtmlEntry) sourceEntry).getValue(HtmlEntry.REFERENCED_LIBRARIES);
              if (contains(referencedLibraries, source)) {
                htmlSources.add(entry.getKey());
              }
            }
          }
          break;
        case PART:
          Source[] librarySources = getLibrariesContaining(source);
          for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
            SourceEntry sourceEntry = entry.getValue();
            if (sourceEntry.getKind() == SourceKind.HTML) {
              Source[] referencedLibraries = ((HtmlEntry) sourceEntry).getValue(HtmlEntry.REFERENCED_LIBRARIES);
              if (containsAny(referencedLibraries, librarySources)) {
                htmlSources.add(entry.getKey());
              }
            }
          }
          break;
      }
      if (htmlSources.isEmpty()) {
        return Source.EMPTY_ARRAY;
      }
      return htmlSources.toArray(new Source[htmlSources.size()]);
    }
  }

  @Override
  public Source[] getHtmlSources() {
    return getSources(SourceKind.HTML);
  }

  @Override
  public SourceKind getKindOf(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry == null) {
      return SourceKind.UNKNOWN;
    }
    return sourceEntry.getKind();
  }

  @Override
  public Source[] getLaunchableClientLibrarySources() {
    // TODO(brianwilkerson) This needs to filter out libraries that do not reference dart:html,
    // either directly or indirectly.
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
        Source source = entry.getKey();
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry.getKind() == SourceKind.LIBRARY && !source.isInSystemLibrary()) {
//          DartEntry dartEntry = (DartEntry) sourceEntry;
//          if (dartEntry.getValue(DartEntry.IS_LAUNCHABLE) && dartEntry.getValue(DartEntry.IS_CLIENT)) {
          sources.add(source);
//          }
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public Source[] getLaunchableServerLibrarySources() {
    // TODO(brianwilkerson) This needs to filter out libraries that reference dart:html, either
    // directly or indirectly.
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
        Source source = entry.getKey();
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry.getKind() == SourceKind.LIBRARY && !source.isInSystemLibrary()) {
//          DartEntry dartEntry = (DartEntry) sourceEntry;
//          if (dartEntry.getValue(DartEntry.IS_LAUNCHABLE) && !dartEntry.getValue(DartEntry.IS_CLIENT)) {
          sources.add(source);
//          }
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
  }

  @Override
  public Source[] getLibrariesContaining(Source source) {
    synchronized (cacheLock) {
      SourceEntry sourceEntry = sourceMap.get(source);
      if (sourceEntry != null && sourceEntry.getKind() == SourceKind.LIBRARY) {
        return new Source[] {source};
      }
      ArrayList<Source> librarySources = new ArrayList<Source>();
      for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
        sourceEntry = entry.getValue();
        if (sourceEntry.getKind() == SourceKind.LIBRARY) {
          if (contains(((DartEntry) sourceEntry).getValue(DartEntry.INCLUDED_PARTS), source)) {
            librarySources.add(entry.getKey());
          }
        }
      }
      if (librarySources.isEmpty()) {
        return Source.EMPTY_ARRAY;
      }
      return librarySources.toArray(new Source[librarySources.size()]);
    }
  }

  @Override
  public Source[] getLibrariesDependingOn(Source librarySource) {
    synchronized (cacheLock) {
      ArrayList<Source> dependentLibraries = new ArrayList<Source>();
      for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry.getKind() == SourceKind.LIBRARY) {
          if (contains(
              ((DartEntry) sourceEntry).getValue(DartEntry.EXPORTED_LIBRARIES),
              librarySource)) {
            dependentLibraries.add(entry.getKey());
          }
          if (contains(
              ((DartEntry) sourceEntry).getValue(DartEntry.IMPORTED_LIBRARIES),
              librarySource)) {
            dependentLibraries.add(entry.getKey());
          }
        }
      }
      if (dependentLibraries.isEmpty()) {
        return Source.EMPTY_ARRAY;
      }
      return dependentLibraries.toArray(new Source[dependentLibraries.size()]);
    }
  }

  @Override
  public LibraryElement getLibraryElement(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry instanceof DartEntry) {
      return ((DartEntry) sourceEntry).getValue(DartEntry.ELEMENT);
    }
    return null;
  }

  @Override
  public Source[] getLibrarySources() {
    return getSources(SourceKind.LIBRARY);
  }

  @Override
  public LineInfo getLineInfo(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry != null) {
      return sourceEntry.getValue(SourceEntry.LINE_INFO);
    }
    return null;
  }

  @Override
  public Namespace getPublicNamespace(LibraryElement library) {
    // TODO(brianwilkerson) Rename this to not start with 'get'. Note that this is not part of the
    // API of the interface.
    Source source = library.getDefiningCompilationUnit().getSource();
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return null;
    }
    synchronized (cacheLock) {
      Namespace namespace = dartEntry.getValue(DartEntry.PUBLIC_NAMESPACE);
      if (namespace == null) {
        NamespaceBuilder builder = new NamespaceBuilder();
        namespace = builder.createPublicNamespace(library);
        DartEntryImpl dartCopy = getDartEntry(source).getWritableCopy();
        dartCopy.setValue(DartEntry.PUBLIC_NAMESPACE, namespace);
        sourceMap.put(source, dartCopy);
      }
      return namespace;
    }
  }

  @Override
  public Namespace getPublicNamespace(Source source) throws AnalysisException {
    // TODO(brianwilkerson) Rename this to not start with 'get'. Note that this is not part of the
    // API of the interface.
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return null;
    }
    synchronized (cacheLock) {
      Namespace namespace = dartEntry.getValue(DartEntry.PUBLIC_NAMESPACE);
      if (namespace == null) {
        LibraryElement library = computeLibraryElement(source);
        if (library == null) {
          return null;
        }
        NamespaceBuilder builder = new NamespaceBuilder();
        namespace = builder.createPublicNamespace(library);
        DartEntryImpl dartCopy = getDartEntry(source).getWritableCopy();
        dartCopy.setValue(DartEntry.PUBLIC_NAMESPACE, namespace);
        sourceMap.put(source, dartCopy);
      }
      return namespace;
    }
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, LibraryElement library) {
    if (library == null) {
      return null;
    }
    return getResolvedCompilationUnit(unitSource, library.getSource());
  }

  @Override
  public CompilationUnit getResolvedCompilationUnit(Source unitSource, Source librarySource) {
    SourceEntry sourceEntry = getReadableSourceEntry(unitSource);
    if (sourceEntry instanceof DartEntry) {
      return ((DartEntry) sourceEntry).getValue(DartEntry.RESOLVED_UNIT, librarySource);
    }
    return null;
  }

  @Override
  public SourceFactory getSourceFactory() {
    return sourceFactory;
  }

  /**
   * Return a list of the sources that would be processed by {@link #performAnalysisTask()}. This
   * method is intended to be used for testing purposes only.
   * 
   * @return a list of the sources that would be processed by {@link #performAnalysisTask()}
   */
  @VisibleForTesting
  public List<Source> getSourcesNeedingProcessing() {
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry instanceof DartEntry) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          CacheState parsedUnitState = dartEntry.getState(DartEntry.PARSED_UNIT);
          CacheState elementState = dartEntry.getState(DartEntry.ELEMENT);
          if (parsedUnitState == CacheState.INVALID || elementState == CacheState.INVALID) {
            sources.add(entry.getKey());
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          CacheState parsedUnitState = htmlEntry.getState(HtmlEntry.PARSED_UNIT);
          CacheState elementState = htmlEntry.getState(HtmlEntry.ELEMENT);
          if (parsedUnitState == CacheState.INVALID || elementState == CacheState.INVALID) {
            sources.add(entry.getKey());
          }
        }
      }
    }
    return sources;
  }

  @Override
  public AnalysisContentStatistics getStatistics() {
    AnalysisContentStatisticsImpl statistics = new AnalysisContentStatisticsImpl();
    synchronized (cacheLock) {
      for (Entry<Source, SourceEntry> mapEntry : sourceMap.entrySet()) {
        SourceEntry entry = mapEntry.getValue();
        if (entry instanceof DartEntry) {
          Source source = mapEntry.getKey();
          DartEntry dartEntry = (DartEntry) entry;
          SourceKind kind = dartEntry.getValue(DartEntry.SOURCE_KIND);
          // get library independent values
          putStatCacheItem(statistics, dartEntry, DartEntry.PARSE_ERRORS);
          putStatCacheItem(statistics, dartEntry, DartEntry.PARSED_UNIT);
          putStatCacheItem(statistics, dartEntry, DartEntry.SOURCE_KIND);
          putStatCacheItem(statistics, dartEntry, DartEntry.LINE_INFO);
          if (kind == SourceKind.LIBRARY) {
            putStatCacheItem(statistics, dartEntry, DartEntry.ELEMENT);
            putStatCacheItem(statistics, dartEntry, DartEntry.EXPORTED_LIBRARIES);
            putStatCacheItem(statistics, dartEntry, DartEntry.IMPORTED_LIBRARIES);
            putStatCacheItem(statistics, dartEntry, DartEntry.INCLUDED_PARTS);
            putStatCacheItem(statistics, dartEntry, DartEntry.IS_CLIENT);
            putStatCacheItem(statistics, dartEntry, DartEntry.IS_LAUNCHABLE);
            // The public namespace isn't computed by performAnalysisTask().
            //putStatCacheItem(statistics, dartEntry, DartEntry.PUBLIC_NAMESPACE);
          }
          // get library-specific values
          Source[] librarySources = getLibrariesContaining(source);
          for (Source librarySource : librarySources) {
            // TODO(brianwilkerson) Restore the line below when hints are being computed separately.
//            putStatCacheItem(statistics, dartEntry, librarySource, DartEntry.HINTS);
            putStatCacheItem(statistics, dartEntry, librarySource, DartEntry.RESOLUTION_ERRORS);
            putStatCacheItem(statistics, dartEntry, librarySource, DartEntry.RESOLVED_UNIT);
          }
//        } else if (entry instanceof HtmlEntry) {
//          HtmlEntry htmlEntry = (HtmlEntry) entry;
        }
      }
    }
    return statistics;
  }

  @Override
  public boolean isClientLibrary(Source librarySource) {
    SourceEntry sourceEntry = getReadableSourceEntry(librarySource);
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      return dartEntry.getValue(DartEntry.IS_CLIENT) && dartEntry.getValue(DartEntry.IS_LAUNCHABLE);
    }
    return false;
  }

  @Override
  public boolean isServerLibrary(Source librarySource) {
    SourceEntry sourceEntry = getReadableSourceEntry(librarySource);
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      return !dartEntry.getValue(DartEntry.IS_CLIENT)
          && dartEntry.getValue(DartEntry.IS_LAUNCHABLE);
    }
    return false;
  }

  @Override
  public void mergeContext(AnalysisContext context) {
    if (context instanceof InstrumentedAnalysisContextImpl) {
      context = ((InstrumentedAnalysisContextImpl) context).getBasis();
    }
    if (!(context instanceof AnalysisContextImpl)) {
      return;
    }
    synchronized (cacheLock) {
      // TODO(brianwilkerson) This does not lock against the other context's cacheLock.
      for (Map.Entry<Source, SourceEntry> entry : ((AnalysisContextImpl) context).sourceMap.entrySet()) {
        Source newSource = entry.getKey();
        SourceEntry existingEntry = getSourceEntry(newSource);
        if (existingEntry == null) {
          // TODO(brianwilkerson) Decide whether we really need to copy the info.
          sourceMap.put(newSource, entry.getValue().getWritableCopy());
        } else {
          // TODO(brianwilkerson) Decide whether/how to merge the entries.
        }
      }
    }
  }

  @Override
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return null;
    }
    CompilationUnit unit = dartEntry.getAnyParsedCompilationUnit();
    if (unit == null) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      CacheState state = dartEntry.getState(DartEntry.PARSED_UNIT);
      while (state != CacheState.VALID && state != CacheState.ERROR) {
        dartEntry = internalParseDart(source);
        state = dartEntry.getState(DartEntry.PARSED_UNIT);
      }
      unit = dartEntry.getAnyParsedCompilationUnit();
    }
    return unit;
  }

  @Override
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
    return internalGetHtmlParseData(source, HtmlEntry.PARSED_UNIT, null);
  }

  @Override
  public ChangeNotice[] performAnalysisTask() {
    // TODO(brianwilkerson) Replace the body of this method with the commented-out code.
//    AnalysisTask task = getNextTaskAnalysisTask();
//    if (task == null) {
//      synchronized (cacheLock) {
//        if (!pendingNotices.isEmpty()) {
//          ChangeNotice[] notices = pendingNotices.values().toArray(
//              new ChangeNotice[pendingNotices.size()]);
//          pendingNotices.clear();
//          return notices;
//        }
//      }
//      return null;
//    }
//    task.perform();
//    synchronized (cacheLock) {
//      if (pendingNotices.isEmpty()) {
//        return ChangeNoticeImpl.EMPTY_ARRAY;
//      }
//      ChangeNotice[] notices = pendingNotices.values().toArray(
//          new ChangeNotice[pendingNotices.size()]);
//      pendingNotices.clear();
//      return notices;
//    }
    synchronized (cacheLock) {
      if (!performSingleAnalysisTask() && pendingNotices.isEmpty()) {
        return null;
      }
      if (pendingNotices.isEmpty()) {
        return ChangeNoticeImpl.EMPTY_ARRAY;
      }
      ChangeNotice[] notices = pendingNotices.values().toArray(
          new ChangeNotice[pendingNotices.size()]);
      pendingNotices.clear();
      return notices;
    }
  }

  @Override
  public void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    synchronized (cacheLock) {
      Source htmlSource = sourceFactory.forUri(DartSdk.DART_HTML);
      for (Map.Entry<Source, LibraryElement> entry : elementMap.entrySet()) {
        Source librarySource = entry.getKey();
        LibraryElement library = entry.getValue();
        //
        // Cache the element in the library's info.
        //
        DartEntry dartEntry = getDartEntry(librarySource);
        if (dartEntry != null) {
          DartEntryImpl dartCopy = dartEntry.getWritableCopy();
          recordElementData(dartCopy, library, htmlSource);
          sourceMap.put(librarySource, dartCopy);
        }
      }
    }
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source source, LibraryElement library)
      throws AnalysisException {
    if (library == null) {
      return null;
    }
    return resolveCompilationUnit(source, library.getSource());
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, Source librarySource)
      throws AnalysisException {
    synchronized (cacheLock) {
      accessed(unitSource);
      DartEntry dartEntry = getDartEntry(unitSource);
      if (dartEntry == null) {
        return null;
      }
      CompilationUnit unit = dartEntry.getValue(DartEntry.RESOLVED_UNIT, librarySource);
      if (unit == null) {
        disableCacheRemoval();
        try {
          LibraryElement libraryElement = computeLibraryElement(librarySource);
          unit = dartEntry.getValue(DartEntry.RESOLVED_UNIT, librarySource);
          if (unit == null && libraryElement != null) {
            Source coreLibrarySource = libraryElement.getContext().getSourceFactory().forUri(
                DartSdk.DART_CORE);
            LibraryElement coreElement = computeLibraryElement(coreLibrarySource);
            TypeProvider typeProvider = new TypeProviderImpl(coreElement);
            ResolvableCompilationUnit resolvableUnit = computeResolvableCompilationUnit(unitSource);
            CompilationUnit unitAST = resolvableUnit.getCompilationUnit();
            //
            // Resolve names in declarations.
            //
            new DeclarationResolver().resolve(unitAST, find(libraryElement, unitSource));
            //
            // Resolve the type names.
            //
            RecordingErrorListener errorListener = new RecordingErrorListener();
            TypeResolverVisitor typeResolverVisitor = new TypeResolverVisitor(
                libraryElement,
                unitSource,
                typeProvider,
                errorListener);
            unitAST.accept(typeResolverVisitor);
            //
            // Resolve the rest of the structure
            //
            InheritanceManager inheritanceManager = new InheritanceManager(libraryElement);
            ResolverVisitor resolverVisitor = new ResolverVisitor(
                libraryElement,
                unitSource,
                typeProvider,
                inheritanceManager,
                errorListener);
            unitAST.accept(resolverVisitor);
            // TODO (jwren) Move this logic/ loop into the ResolverVisitor and then make the reportError protected again.
            for (ProxyConditionalAnalysisError conditionalCode : resolverVisitor.getProxyConditionalAnalysisErrors()) {
              if (conditionalCode.shouldIncludeErrorCode()) {
                resolverVisitor.reportError(conditionalCode.getAnalysisError());
              }
            }
            //
            // Perform additional error checking.
            //
            ErrorReporter errorReporter = new ErrorReporter(errorListener, unitSource);
            ErrorVerifier errorVerifier = new ErrorVerifier(
                errorReporter,
                libraryElement,
                typeProvider,
                inheritanceManager);
            unitAST.accept(errorVerifier);

            ConstantVerifier constantVerifier = new ConstantVerifier(errorReporter, typeProvider);
            unitAST.accept(constantVerifier);
            //
            // Capture the results.
            //
            unitAST.setResolutionErrors(errorListener.getErrors());
            DartEntryImpl dartCopy = getDartEntry(unitSource).getWritableCopy();
            dartCopy.setValue(DartEntry.RESOLVED_UNIT, librarySource, unitAST);
            sourceMap.put(unitSource, dartCopy);
            unit = unitAST;
          }
        } finally {
          enableCacheRemoval();
        }
      }
      return unit;
    }
  }

  @Override
  public HtmlUnit resolveHtmlUnit(Source source) throws AnalysisException {
    // There is currently no difference between the parsed and resolved forms of an HTML
    // unit. This code needs to change if resolution ever modifies the AST.
    return parseHtmlUnit(source);
  }

  @Override
  public void setAnalysisOptions(AnalysisOptions options) {
    synchronized (cacheLock) {
      this.options = options;
      invalidateAllResults();
    }
  }

  @Override
  public void setAnalysisPriorityOrder(List<Source> sources) {
    synchronized (cacheLock) {
      if (sources == null || sources.isEmpty()) {
        priorityOrder = Source.EMPTY_ARRAY;
      } else {
        while (sources.remove(null)) {
          // Nothing else to do.
        }
        //
        // Cap the size of the priority list to being less than the cache size. Failure to do so can
        // result in an infinite loop in performAnalysisTask() because re-caching one AST structure
        // can cause another priority source's AST structure to be flushed.
        //
        int count = Math.min(sources.size(), MAX_PRIORITY_LIST_SIZE);
        priorityOrder = new Source[count];
        for (int i = 0; i < count; i++) {
          priorityOrder[i] = sources.get(i);
        }
      }
    }
  }

  @Override
  public void setContents(Source source, String contents) {
    synchronized (cacheLock) {
      if (sourceFactory.setContents(source, contents)) {
        sourceChanged(source);
      }
    }
  }

  @Override
  public void setSourceFactory(SourceFactory factory) {
    synchronized (cacheLock) {
      if (sourceFactory == factory) {
        return;
      } else if (factory.getContext() != null) {
        throw new IllegalStateException("Source factories cannot be shared between contexts");
      }
      if (sourceFactory != null) {
        sourceFactory.setContext(null);
      }
      factory.setContext(this);
      sourceFactory = factory;
      invalidateAllResults();
    }
  }

  @Override
  public Iterable<Source> sourcesToResolve(Source[] changedSources) {
    List<Source> librarySources = new ArrayList<Source>();
    for (Source source : changedSources) {
      if (computeKindOf(source) == SourceKind.LIBRARY) {
        librarySources.add(source);
      }
    }
    return librarySources;
  }

  /**
   * Record that the given source was just accessed for some unspecified purpose.
   * <p>
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that was accessed
   */
  private void accessed(Source source) {
    if (recentlyUsed.remove(source)) {
      recentlyUsed.add(source);
      return;
    }
    if (cacheRemovalCount == 0 && recentlyUsed.size() >= MAX_CACHE_SIZE) {
      flushAstFromCache();
    }
    recentlyUsed.add(source);
  }

  /**
   * Add all of the sources contained in the given source container to the given list of sources.
   * <p>
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param sources the list to which sources are to be added
   * @param container the source container containing the sources to be added to the list
   */
  private void addSourcesInContainer(ArrayList<Source> sources, SourceContainer container) {
    for (Source source : sourceMap.keySet()) {
      if (container.contains(source)) {
        sources.add(source);
      }
    }
  }

  /**
   * Given the encoded form of a source, use the source factory to reconstitute the original source.
   * 
   * @param encoding the encoded form of a source
   * @return the source represented by the encoding
   */
  private Source computeSourceFromEncoding(String encoding) {
    synchronized (cacheLock) {
      return sourceFactory.fromEncoding(encoding);
    }
  }

  /**
   * Return {@code true} if the given array of sources contains the given source.
   * 
   * @param sources the sources being searched
   * @param targetSource the source being searched for
   * @return {@code true} if the given source is in the array
   */
  private boolean contains(Source[] sources, Source targetSource) {
    for (Source source : sources) {
      if (source.equals(targetSource)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return {@code true} if the given array of sources contains any of the given target sources.
   * 
   * @param sources the sources being searched
   * @param targetSources the sources being searched for
   * @return {@code true} if any of the given target sources are in the array
   */
  private boolean containsAny(Source[] sources, Source[] targetSources) {
    for (Source targetSource : targetSources) {
      if (contains(sources, targetSource)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Create a source information object suitable for the given source. Return the source information
   * object that was created, or {@code null} if the source should not be tracked by this context.
   * 
   * @param source the source for which an information object is being created
   * @return the source information object that was created
   */
  private SourceEntry createSourceEntry(Source source) {
    String name = source.getShortName();
    if (AnalysisEngine.isHtmlFileName(name)) {
      HtmlEntryImpl htmlEntry = new HtmlEntryImpl();
      htmlEntry.setModificationTime(source.getModificationStamp());
      sourceMap.put(source, htmlEntry);
      return htmlEntry;
    } else {
      DartEntryImpl dartEntry = new DartEntryImpl();
      dartEntry.setModificationTime(source.getModificationStamp());
      sourceMap.put(source, dartEntry);
      return dartEntry;
    }
  }

  /**
   * Disable flushing information from the cache until {@link #enableCacheRemoval()} has been
   * called.
   */
  private void disableCacheRemoval() {
    cacheRemovalCount++;
  }

  /**
   * Re-enable flushing information from the cache.
   */
  private void enableCacheRemoval() {
    if (cacheRemovalCount > 0) {
      cacheRemovalCount--;
    }
    if (cacheRemovalCount == 0) {
      while (recentlyUsed.size() >= MAX_CACHE_SIZE) {
        flushAstFromCache();
      }
    }
  }

  /**
   * Search the compilation units that are part of the given library and return the element
   * representing the compilation unit with the given source. Return {@code null} if there is no
   * such compilation unit.
   * 
   * @param libraryElement the element representing the library being searched through
   * @param unitSource the source for the compilation unit whose element is to be returned
   * @return the element representing the compilation unit
   */
  private CompilationUnitElement find(LibraryElement libraryElement, Source unitSource) {
    CompilationUnitElement element = libraryElement.getDefiningCompilationUnit();
    if (element.getSource().equals(unitSource)) {
      return element;
    }
    for (CompilationUnitElement partElement : libraryElement.getParts()) {
      if (partElement.getSource().equals(unitSource)) {
        return partElement;
      }
    }
    return null;
  }

  /**
   * Flush one AST structure from the cache.
   * <p>
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   */
  private void flushAstFromCache() {
    Source removedSource = removeAstToFlush();
    SourceEntry sourceEntry = sourceMap.get(removedSource);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.FLUSHED);
      sourceMap.put(removedSource, htmlCopy);
    } else if (sourceEntry instanceof DartEntry) {
      DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
      dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.FLUSHED);
      for (Source librarySource : getLibrariesContaining(removedSource)) {
        dartCopy.setState(DartEntry.RESOLVED_UNIT, librarySource, CacheState.FLUSHED);
      }
      sourceMap.put(removedSource, dartCopy);
    }
  }

  /**
   * Return the compilation unit information associated with the given source, or {@code null} if
   * the source is not known to this context. This method should be used to access the compilation
   * unit information rather than accessing the compilation unit map directly because sources in the
   * SDK are implicitly part of every analysis context and are therefore only added to the map when
   * first accessed.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source for which information is being sought
   * @return the compilation unit information associated with the given source
   */
  private DartEntry getDartEntry(Source source) {
    SourceEntry sourceEntry = getSourceEntry(source);
    if (sourceEntry == null) {
      sourceEntry = new DartEntryImpl();
      sourceMap.put(source, sourceEntry);
      return (DartEntry) sourceEntry;
    } else if (sourceEntry instanceof DartEntry) {
      return (DartEntry) sourceEntry;
    }
    return null;
  }

  /**
   * Return the sources of libraries that are referenced in the specified HTML file.
   * 
   * @param htmlSource the source of the HTML file being analyzed
   * @param htmlUnit the AST for the HTML file being analyzed
   * @return the sources of libraries that are referenced in the HTML file
   */
  private Source[] getLibrarySources(final Source htmlSource, HtmlUnit htmlUnit) {
    final ArrayList<Source> libraries = new ArrayList<Source>();
    htmlUnit.accept(new RecursiveXmlVisitor<Void>() {
      @Override
      public Void visitXmlTagNode(XmlTagNode node) {
        if (node.getTag().getLexeme().equalsIgnoreCase(TAG_SCRIPT)) {
          boolean isDartScript = false;
          XmlAttributeNode scriptAttribute = null;
          for (XmlAttributeNode attribute : node.getAttributes()) {
            if (attribute.getName().getLexeme().equalsIgnoreCase(ATTRIBUTE_SRC)) {
              scriptAttribute = attribute;
            } else if (attribute.getName().getLexeme().equalsIgnoreCase(ATTRIBUTE_TYPE)) {
              String text = attribute.getText();
              if (text != null && text.equalsIgnoreCase(TYPE_DART)) {
                isDartScript = true;
              }
            }
          }
          if (isDartScript && scriptAttribute != null) {
            String text = scriptAttribute.getText();
            if (text != null) {
              try {
                URI uri = new URI(null, null, text, null);
                String fileName = uri.getPath();
                Source librarySource = sourceFactory.resolveUri(htmlSource, fileName);
                if (librarySource != null && librarySource.exists()) {
                  libraries.add(librarySource);
                }
              } catch (Exception exception) {
                AnalysisEngine.getInstance().getLogger().logInformation(
                    "Invalid URI ('" + text + "') in script tag in '" + htmlSource.getFullName()
                        + "'",
                    exception);
              }
            }
          }
        }
        return super.visitXmlTagNode(node);
      }
    });
    if (libraries.isEmpty()) {
      return Source.EMPTY_ARRAY;
    }
    return libraries.toArray(new Source[libraries.size()]);
  }

  /**
   * Look through the cache for a task that needs to be performed. Return the task that was found,
   * or {@code null} if there is no more work to be done.
   * 
   * @return the next task that needs to be performed
   */
  private AnalysisTask getNextTaskAnalysisTask() {
    synchronized (cacheLock) {
      //
      // Look for a priority source that needs to be analyzed.
      //
      for (Source source : priorityOrder) {
        SourceEntry sourceEntry = sourceMap.get(source);
        if (sourceEntry instanceof DartEntry) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          CacheState parseErrorsState = dartEntry.getState(DartEntry.PARSE_ERRORS);
          if (parseErrorsState == CacheState.INVALID || parseErrorsState == CacheState.FLUSHED) {
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setState(DartEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
            sourceMap.put(source, dartCopy);
            return new ParseDartTask(source);
          }
          CacheState parseUnitState = dartEntry.getState(DartEntry.PARSED_UNIT);
          if (parseUnitState == CacheState.INVALID || parseUnitState == CacheState.FLUSHED) {
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.IN_PROCESS);
            sourceMap.put(source, dartCopy);
            return new ParseDartTask(source);
          }
          for (Source librarySource : getLibrariesContaining(source)) {
            SourceEntry libraryEntry = sourceMap.get(librarySource);
            if (libraryEntry instanceof DartEntry) {
              CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
              if (elementState == CacheState.INVALID || elementState == CacheState.FLUSHED) {
                DartEntryImpl libraryCopy = ((DartEntry) libraryEntry).getWritableCopy();
                libraryCopy.setState(DartEntry.ELEMENT, CacheState.IN_PROCESS);
                sourceMap.put(librarySource, libraryCopy);
                return new ResolveDartLibraryTask(librarySource);
              }
            }
            CacheState resolvedUnitState = dartEntry.getState(
                DartEntry.RESOLVED_UNIT,
                librarySource);
            if (resolvedUnitState == CacheState.INVALID || resolvedUnitState == CacheState.FLUSHED) {
              DartEntryImpl dartCopy = dartEntry.getWritableCopy();
              dartCopy.setState(DartEntry.RESOLVED_UNIT, librarySource, CacheState.IN_PROCESS);
              sourceMap.put(source, dartCopy);
              return new ResolveDartUnitTask(source, librarySource);
            }
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          CacheState parsedUnitState = htmlEntry.getState(HtmlEntry.PARSED_UNIT);
          if (parsedUnitState == CacheState.INVALID || parsedUnitState == CacheState.FLUSHED) {
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.IN_PROCESS);
            sourceMap.put(source, htmlCopy);
            return new ParseHtmlTask(source);
          }
          CacheState elementState = htmlEntry.getState(HtmlEntry.ELEMENT);
          if (elementState == CacheState.INVALID || elementState == CacheState.FLUSHED) {
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.IN_PROCESS);
            sourceMap.put(source, htmlCopy);
            return new ResolveHtmlTask(source);
          }
        }
      }
      //
      // Look for a non-priority source that needs to be parsed.
      //
      for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry instanceof DartEntry) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          if (dartEntry.getState(DartEntry.PARSED_UNIT) == CacheState.INVALID) {
            Source source = entry.getKey();
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setState(DartEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
            sourceMap.put(source, dartCopy);
            return new ParseDartTask(source);
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          if (htmlEntry.getState(HtmlEntry.PARSED_UNIT) == CacheState.INVALID) {
            Source source = entry.getKey();
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.IN_PROCESS);
            sourceMap.put(source, htmlCopy);
            return new ParseHtmlTask(source);
          }
        }
      }
      //
      // Look for a non-priority source that needs to be resolved.
      //
      for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry instanceof DartEntry && sourceEntry.getKind() == SourceKind.LIBRARY) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          if (dartEntry.getState(DartEntry.ELEMENT) == CacheState.INVALID) {
            Source source = entry.getKey();
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setState(DartEntry.ELEMENT, CacheState.IN_PROCESS);
            sourceMap.put(source, dartCopy);
            return new ResolveDartLibraryTask(source);
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          if (htmlEntry.getState(HtmlEntry.ELEMENT) == CacheState.INVALID) {
            Source source = entry.getKey();
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.IN_PROCESS);
            sourceMap.put(source, htmlCopy);
            return new ResolveHtmlTask(source);
          }
        }
      }
      return null;
    }
  }

  /**
   * Return a change notice for the given source, creating one if one does not already exist.
   * 
   * @param source the source for which changes are being reported
   * @return a change notice for the given source
   */
  private ChangeNoticeImpl getNotice(Source source) {
    ChangeNoticeImpl notice = pendingNotices.get(source);
    if (notice == null) {
      notice = new ChangeNoticeImpl(source);
      pendingNotices.put(source, notice);
    }
    return notice;
  }

  /**
   * Return the cache entry associated with the given source, or {@code null} if the source is not a
   * Dart file.
   * 
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  private DartEntry getReadableDartEntry(Source source) {
    synchronized (cacheLock) {
      SourceEntry sourceEntry = sourceMap.get(source);
      if (sourceEntry == null) {
        sourceEntry = createSourceEntry(source);
      }
      if (sourceEntry instanceof DartEntry) {
        accessed(source);
        return (DartEntry) sourceEntry;
      }
      return null;
    }
  }

  /**
   * Return the cache entry associated with the given source, or {@code null} if the source is not
   * an HTML file.
   * 
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  private HtmlEntry getReadableHtmlEntry(Source source) {
    synchronized (cacheLock) {
      SourceEntry sourceEntry = sourceMap.get(source);
      if (sourceEntry == null) {
        sourceEntry = createSourceEntry(source);
      }
      if (sourceEntry instanceof HtmlEntry) {
        accessed(source);
        return (HtmlEntry) sourceEntry;
      }
      return null;
    }
  }

  /**
   * Return the cache entry associated with the given source, or {@code null} if there is no entry
   * associated with the source.
   * 
   * @param source the source for which a cache entry is being sought
   * @return the source cache entry associated with the given source
   */
  private SourceEntry getReadableSourceEntry(Source source) {
    synchronized (cacheLock) {
      SourceEntry sourceEntry = sourceMap.get(source);
      if (sourceEntry == null) {
        sourceEntry = createSourceEntry(source);
      }
      if (sourceEntry != null) {
        accessed(source);
      }
      return sourceEntry;
    }
  }

  /**
   * Return the source information associated with the given source, or {@code null} if the source
   * is not known to this context. This method should be used to access the source information
   * rather than accessing the source map directly because sources in the SDK are implicitly part of
   * every analysis context and are therefore only added to the map when first accessed.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source for which information is being sought
   * @return the source information associated with the given source
   */
  private SourceEntry getSourceEntry(Source source) {
    SourceEntry sourceEntry = sourceMap.get(source);
    if (sourceEntry == null) {
      sourceEntry = createSourceEntry(source);
    }
    return sourceEntry;
  }

  /**
   * Return an array containing all of the sources known to this context that have the given kind.
   * 
   * @param kind the kind of sources to be returned
   * @return all of the sources known to this context that have the given kind
   */
  private Source[] getSources(SourceKind kind) {
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
        if (entry.getValue().getKind() == kind) {
          sources.add(entry.getKey());
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
  }

  /**
   * Given a source for an HTML file, return a cache entry in which all of the data represented by
   * the given descriptors is available. This method assumes that the data can be produced by
   * parsing the source if it is not already cached.
   * 
   * @param htmlEntry the cache entry associated with the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   */
  private boolean hasHtmlParseDataCached(HtmlEntry htmlEntry, DataDescriptor<?>... descriptors) {
    for (DataDescriptor<?> descriptor : descriptors) {
      CacheState state = htmlEntry.getState(descriptor);
      if (state != CacheState.VALID && state != CacheState.ERROR) {
        return false;
      }
    }
    return true;
  }

  /**
   * Given a source for a Dart file, return a cache entry in which the data represented by the given
   * descriptor is available. This method assumes that the data can be produced by parsing the
   * source if it is not already cached.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private DartEntry internalCacheDartParseData(Source source, DartEntry dartEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      dartEntry = internalParseDart(source);
      state = dartEntry.getState(descriptor);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * all of the data represented by the given descriptors is available. This method assumes that the
   * data can be produced by resolving the source in the context of the library if it is not already
   * cached.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private DartEntry internalCacheDartResolutionData(Source unitSource, Source librarySource,
      DartEntry dartEntry, DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getState(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      dartEntry = internalResolveDart(unitSource, librarySource);
      state = dartEntry.getState(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for an HTML file, return a cache entry in which all of the data represented by
   * the given descriptors is available. This method assumes that the data can be produced by
   * parsing the source if it is not already cached.
   * 
   * @param source the source representing the HTML file
   * @param htmlEntry the cache entry associated with the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private HtmlEntry internalCacheHtmlParseData(Source source, HtmlEntry htmlEntry,
      DataDescriptor<?>... descriptors) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    while (!hasHtmlParseDataCached(htmlEntry, descriptors)) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      htmlEntry = internalParseHtml(source);
    }
    return htmlEntry;
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source. This method assumes that the data can be produced by parsing the
   * source if it is not already cached.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private <E> E internalGetDartParseData(Source source, DartEntry dartEntry,
      DataDescriptor<E> descriptor) throws AnalysisException {
    dartEntry = internalCacheDartParseData(source, dartEntry, descriptor);
    return dartEntry.getValue(descriptor);
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not a Dart file. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   * 
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private <E> E internalGetDartParseData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return defaultValue;
    }
    return internalGetDartParseData(source, dartEntry, descriptor);
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by resolving the source in the context of the library if it is not already cached.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the entry representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private <E> E internalGetDartResolutionData(Source unitSource, Source librarySource,
      DartEntry dartEntry, DataDescriptor<E> descriptor) throws AnalysisException {
    dartEntry = internalCacheDartResolutionData(unitSource, librarySource, dartEntry, descriptor);
    return dartEntry.getValue(descriptor, librarySource);
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by resolving the source in the context of the library if it is not already cached.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the file is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private <E> E internalGetDartResolutionData(Source unitSource, Source librarySource,
      DataDescriptor<E> descriptor, E defaultValue) throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(unitSource);
    if (dartEntry == null) {
      return defaultValue;
    }
    dartEntry = internalCacheDartResolutionData(unitSource, librarySource, dartEntry, descriptor);
    return dartEntry.getValue(descriptor);
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not an HTML file. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   * 
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private <E> E internalGetHtmlParseData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      return defaultValue;
    }
    htmlEntry = internalCacheHtmlParseData(source, htmlEntry, descriptor);
    return htmlEntry.getValue(descriptor);
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not an HTML file. This
   * method assumes that the data can be produced by resolving the source if it is not already
   * cached.
   * 
   * @param source the source representing the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not an HTML file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E internalGetHtmlResolutionData(Source source, HtmlEntry htmlEntry,
      DataDescriptor<E> descriptor, E defaultValue) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    if (htmlEntry == null) {
      return defaultValue;
    }
    CacheState state = htmlEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      htmlEntry = internalResolveHtml(source);
      state = htmlEntry.getState(descriptor);
    }
    return htmlEntry.getValue(descriptor);
  }

  @Deprecated
  private CompilationUnit internalParseCompilationUnit(DartEntryImpl dartCopy, Source source)
      throws AnalysisException {
    try {
      accessed(source);
      RecordingErrorListener errorListener = new RecordingErrorListener();
      ScanResult scanResult = internalScan(source, errorListener);
      Parser parser = new Parser(source, errorListener);
      CompilationUnit unit = parser.parseCompilationUnit(scanResult.token);
      LineInfo lineInfo = new LineInfo(scanResult.lineStarts);
      AnalysisError[] errors = errorListener.getErrors(source);
      boolean hasPartOfDirective = false;
      boolean hasLibraryDirective = false;
      HashSet<Source> exportedSources = new HashSet<Source>();
      HashSet<Source> importedSources = new HashSet<Source>();
      HashSet<Source> includedSources = new HashSet<Source>();
      for (Directive directive : unit.getDirectives()) {
        if (directive instanceof ExportDirective) {
          Source exportSource = resolveSource(source, (ExportDirective) directive);
          if (exportSource != null) {
            exportedSources.add(exportSource);
          }
        } else if (directive instanceof ImportDirective) {
          Source importSource = resolveSource(source, (ImportDirective) directive);
          if (importSource != null) {
            importedSources.add(importSource);
          }
        } else if (directive instanceof LibraryDirective) {
          hasLibraryDirective = true;
        } else if (directive instanceof PartDirective) {
          Source partSource = resolveSource(source, (PartDirective) directive);
          if (partSource != null) {
            includedSources.add(partSource);
          }
        } else if (directive instanceof PartOfDirective) {
          hasPartOfDirective = true;
        }
      }
      unit.setParsingErrors(errors);
      unit.setLineInfo(lineInfo);
      if (dartCopy.getState(DartEntry.SOURCE_KIND) == CacheState.INVALID) {
        if (hasPartOfDirective && !hasLibraryDirective) {
          dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.PART);
        } else {
          dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.LIBRARY);
        }
      }
      dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
      dartCopy.setValue(DartEntry.PARSED_UNIT, unit);
      dartCopy.setValue(DartEntry.PARSE_ERRORS, errors);
      dartCopy.setValue(DartEntry.EXPORTED_LIBRARIES, toArray(exportedSources));
      dartCopy.setValue(DartEntry.IMPORTED_LIBRARIES, toArray(importedSources));
      dartCopy.setValue(DartEntry.INCLUDED_PARTS, toArray(includedSources));
      // TODO(brianwilkerson) Find out whether clients want notification when part of the errors are
      // available.
//      ChangeNoticeImpl notice = getNotice(source);
//      if (notice.getErrors() == null) {
//        notice.setErrors(errors, lineInfo);
//      }
      // Access the unit through the entry so that the entry records that the unit has been accessed.
      return dartCopy.getValue(DartEntry.PARSED_UNIT);
    } catch (AnalysisException exception) {
      dartCopy.setState(SourceEntry.LINE_INFO, CacheState.ERROR);
      dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.ERROR);
      dartCopy.setState(DartEntry.PARSE_ERRORS, CacheState.ERROR);
      dartCopy.setState(DartEntry.EXPORTED_LIBRARIES, CacheState.ERROR);
      dartCopy.setState(DartEntry.IMPORTED_LIBRARIES, CacheState.ERROR);
      dartCopy.setState(DartEntry.INCLUDED_PARTS, CacheState.ERROR);
      throw exception;
    }
  }

  /**
   * Scan and parse the given Dart file, updating the cache as appropriate, and return the updated
   * cache entry associated with the source.
   * 
   * @param source the source representing the compilation unit to be parsed
   * @return the updated cache entry associated with the source
   * @throws AnalysisException if the source does not represent a Dart compilation unit or if the
   *           compilation unit cannot be parsed for some reason
   */
  private DartEntry internalParseDart(Source source) throws AnalysisException {
    ScanResult scanResult = null;
    LineInfo lineInfo = null;
    CompilationUnit unit = null;
    AnalysisError[] errors = null;
    boolean hasPartOfDirective = false;
    boolean hasLibraryDirective = false;
    HashSet<Source> exportedSources = new HashSet<Source>();
    HashSet<Source> importedSources = new HashSet<Source>();
    HashSet<Source> includedSources = new HashSet<Source>();
    AnalysisException thrownException = null;
    try {
      RecordingErrorListener errorListener = new RecordingErrorListener();
      scanResult = internalScan(source, errorListener);
      Parser parser = new Parser(source, errorListener);
      unit = parser.parseCompilationUnit(scanResult.token);
      lineInfo = new LineInfo(scanResult.lineStarts);
      errors = errorListener.getErrors(source);
      for (Directive directive : unit.getDirectives()) {
        if (directive instanceof ExportDirective) {
          Source exportSource = resolveSource(source, (ExportDirective) directive);
          if (exportSource != null) {
            exportedSources.add(exportSource);
          }
        } else if (directive instanceof ImportDirective) {
          Source importSource = resolveSource(source, (ImportDirective) directive);
          if (importSource != null) {
            importedSources.add(importSource);
          }
        } else if (directive instanceof LibraryDirective) {
          hasLibraryDirective = true;
        } else if (directive instanceof PartDirective) {
          Source partSource = resolveSource(source, (PartDirective) directive);
          if (partSource != null) {
            includedSources.add(partSource);
          }
        } else if (directive instanceof PartOfDirective) {
          hasPartOfDirective = true;
        }
      }
      unit.setParsingErrors(errors);
      unit.setLineInfo(lineInfo);
    } catch (AnalysisException exception) {
      thrownException = exception;
    }
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = sourceMap.get(source);
      if (!(sourceEntry instanceof DartEntry)) {
        throw new AnalysisException(
            "Internal error: attempting to parse non-Dart file as a Dart file: "
                + source.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      accessed(source);
      long sourceTime = source.getModificationStamp();
      long resultTime = scanResult == null ? sourceTime : scanResult.getModificationTime();
      if (sourceTime == resultTime) {
        if (dartEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          dartEntry = getReadableDartEntry(source);
        }
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null) {
          dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
          if (hasPartOfDirective && !hasLibraryDirective) {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.PART);
          } else {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.LIBRARY);
          }
          dartCopy.setValue(DartEntry.PARSED_UNIT, unit);
          dartCopy.setValue(DartEntry.PARSE_ERRORS, errors);
          dartCopy.setValue(DartEntry.EXPORTED_LIBRARIES, toArray(exportedSources));
          dartCopy.setValue(DartEntry.IMPORTED_LIBRARIES, toArray(importedSources));
          dartCopy.setValue(DartEntry.INCLUDED_PARTS, toArray(includedSources));
        } else {
          dartCopy.recordParseError();
        }
        sourceMap.put(source, dartCopy);
        dartEntry = dartCopy;
      }
    }
    if (thrownException != null) {
      if (!(thrownException.getCause() instanceof IOException)) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not parse " + source.getFullName(),
            thrownException);
      }
      throw thrownException;
    }
//    ChangeNoticeImpl notice = getNotice(source);
//    notice.setErrors(dartCopy.getAllErrors(), lineInfo);
    return dartEntry;
  }

  /**
   * Scan and parse the given HTML file, updating the cache as appropriate, and return the updated
   * cache entry associated with the source.
   * 
   * @param source the source representing the HTML file to be parsed
   * @return the updated cache entry associated with the source
   * @throws AnalysisException if the source does not represent an HTML file or if the file cannot
   *           be parsed for some reason
   */
  private HtmlEntry internalParseHtml(Source source) throws AnalysisException {
    HtmlParseResult result = null;
    LineInfo lineInfo = null;
    AnalysisException thrownException = null;
    try {
      result = new HtmlParser(source).parse(scanHtml(source));
      lineInfo = new LineInfo(result.getLineStarts());
    } catch (AnalysisException exception) {
      thrownException = exception;
    }
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = sourceMap.get(source);
      if (!(sourceEntry instanceof HtmlEntry)) {
        throw new AnalysisException(
            "Internal error: attempting to parse non-HTML file as a HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      accessed(source);
      long sourceTime = source.getModificationStamp();
      long resultTime = result == null ? sourceTime : result.getModificationTime();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
        }
        HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
        if (thrownException == null) {
          HtmlUnit unit = result.getHtmlUnit();
          htmlCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
          htmlCopy.setValue(HtmlEntry.PARSED_UNIT, unit);
          htmlCopy.setValue(HtmlEntry.REFERENCED_LIBRARIES, getLibrarySources(source, unit));
        } else {
          htmlCopy.setState(SourceEntry.LINE_INFO, CacheState.ERROR);
          htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.ERROR);
          htmlCopy.setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.ERROR);
        }
        sourceMap.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not parse " + source.getFullName(),
          thrownException);
      throw thrownException;
    }
    ChangeNoticeImpl notice = getNotice(source);
    notice.setErrors(htmlEntry.getAllErrors(), lineInfo);
    return htmlEntry;
  }

  private DartEntry internalResolveDart(Source unitSource, Source librarySource)
      throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(unitSource);
    if (dartEntry == null) {
      throw new AnalysisException(
          "Internal error: attempting to parse non-Dart file as a Dart file: "
              + unitSource.getFullName());
    }
    LibraryResolver resolver = null;
    AnalysisException thrownException = null;
    try {
      resolver = new LibraryResolver(this);
      resolver.resolveLibrary(librarySource, true);
    } catch (AnalysisException exception) {
      thrownException = exception;
    }
    if (thrownException == null) {
      synchronized (cacheLock) {
        accessed(unitSource);
      }
      recordResolutionResults(resolver);
      dartEntry = getReadableDartEntry(unitSource);
    } else {
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not resolve " + unitSource.getFullName(),
          thrownException);
      // TODO(brianwilkerson) This is incomplete. We ought to mark all of the compilation units in
      // the library as being unresolvable.
      boolean unitIsLibrary = unitSource.equals(librarySource);
      DartEntryImpl dartCopy = dartEntry.getWritableCopy();
      dartCopy.setState(DartEntry.RESOLUTION_ERRORS, librarySource, CacheState.ERROR);
      if (unitIsLibrary) {
        dartCopy.setState(DartEntry.ELEMENT, CacheState.ERROR);
      }
      sourceMap.put(unitSource, dartCopy);
      if (!unitIsLibrary) {
        DartEntry libraryEntry = getReadableDartEntry(librarySource);
        if (libraryEntry != null) {
          DartEntryImpl libraryCopy = dartEntry.getWritableCopy();
          libraryCopy.setState(DartEntry.RESOLUTION_ERRORS, librarySource, CacheState.ERROR);
          libraryCopy.setState(DartEntry.ELEMENT, CacheState.ERROR);
          sourceMap.put(librarySource, libraryCopy);
        }
      }
      throw thrownException;
    }
    ChangeNoticeImpl notice = getNotice(unitSource);
    notice.setErrors(dartEntry.getAllErrors(), dartEntry.getValue(SourceEntry.LINE_INFO));
    return dartEntry;
  }

  /**
   * Scan and parse the given HTML file, updating the cache as appropriate, and return the updated
   * cache entry associated with the source.
   * 
   * @param source the source representing the HTML file to be parsed
   * @return the updated cache entry associated with the source
   * @throws AnalysisException if the source does not represent an HTML file or if the file cannot
   *           be parsed for some reason
   */
  private HtmlEntry internalResolveHtml(Source source) throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      throw new AnalysisException(
          "Internal error: attempting to parse non-HTML file as a HTML file: "
              + source.getFullName());
    }
    long resultTime = 0L;
    HtmlElement element = null;
    AnalysisError[] resolutionErrors = null;
    AnalysisException thrownException = null;
    try {
      htmlEntry = internalCacheHtmlParseData(source, htmlEntry, HtmlEntry.PARSED_UNIT);
      HtmlUnit unit = htmlEntry.getValue(HtmlEntry.PARSED_UNIT);
      if (unit == null) {
        throw new AnalysisException(
            "Internal error: internalCacheHtmlParseData returned an entry without a parsed HTML unit");
      }
      resultTime = htmlEntry.getModificationTime();
      HtmlUnitBuilder builder = new HtmlUnitBuilder(this);
      element = builder.buildHtmlElement(source, resultTime, unit);
      resolutionErrors = builder.getErrorListener().getErrors(source);
    } catch (AnalysisException exception) {
      thrownException = exception;
    }
    synchronized (cacheLock) {
      SourceEntry sourceEntry = sourceMap.get(source);
      if (!(sourceEntry instanceof HtmlEntry)) {
        throw new AnalysisException(
            "Internal error: attempting to resolve non-HTML file as a HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      accessed(source);
      long sourceTime = source.getModificationStamp();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
        }
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null) {
          htmlCopy.setValue(HtmlEntry.RESOLUTION_ERRORS, resolutionErrors);
          htmlCopy.setValue(HtmlEntry.ELEMENT, element);
        } else {
          htmlCopy.setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.ERROR);
          htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.ERROR);
        }
        sourceMap.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not resolve " + source.getFullName(),
          thrownException);
      throw thrownException;
    }
    ChangeNoticeImpl notice = getNotice(source);
    notice.setErrors(htmlEntry.getAllErrors(), htmlEntry.getValue(SourceEntry.LINE_INFO));
    return htmlEntry;
  }

  private ScanResult internalScan(final Source source, final AnalysisErrorListener errorListener)
      throws AnalysisException {
    final ScanResult result = new ScanResult();
    Source.ContentReceiver receiver = new Source.ContentReceiver() {
      @Override
      public void accept(CharBuffer contents, long modificationTime) {
        CharBufferScanner scanner = new CharBufferScanner(source, contents, errorListener);
        result.modificationTime = modificationTime;
        result.token = scanner.tokenize();
        result.lineStarts = scanner.getLineStarts();
      }

      @Override
      public void accept(String contents, long modificationTime) {
        StringScanner scanner = new StringScanner(source, contents, errorListener);
        result.modificationTime = modificationTime;
        result.token = scanner.tokenize();
        result.lineStarts = scanner.getLineStarts();
      }
    };
    try {
      source.getContents(receiver);
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    }
    return result;
  }

  /**
   * Invalidate all of the results computed by this context.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   */
  private void invalidateAllResults() {
    for (Map.Entry<Source, SourceEntry> mapEntry : sourceMap.entrySet()) {
      SourceEntry sourceEntry = mapEntry.getValue();
      if (sourceEntry instanceof HtmlEntry) {
        HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
        htmlCopy.invalidateAllResolutionInformation();
        mapEntry.setValue(htmlCopy);
      } else if (sourceEntry instanceof DartEntry) {
        DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
        dartCopy.invalidateAllResolutionInformation();
        mapEntry.setValue(dartCopy);
      }
    }
  }

  /**
   * In response to a change to at least one of the compilation units in the given library,
   * invalidate any results that are dependent on the result of resolving that library.
   * 
   * @param librarySource the source of the library being invalidated
   */
  private void invalidateLibraryResolution(Source librarySource) {
    // TODO(brianwilkerson) This could be optimized. There's no need to flush all of these caches if
    // the public namespace hasn't changed, which will be a fairly common case.
    DartEntry libraryEntry = getDartEntry(librarySource);
    if (libraryEntry != null) {
      Source[] includedParts = libraryEntry.getValue(DartEntry.INCLUDED_PARTS);
      DartEntryImpl libraryCopy = libraryEntry.getWritableCopy();
      libraryCopy.invalidateAllResolutionInformation();
      libraryCopy.setState(DartEntry.INCLUDED_PARTS, CacheState.INVALID);
      sourceMap.put(librarySource, libraryCopy);
      for (Source unitSource : includedParts) {
        DartEntry partEntry = getDartEntry(unitSource);
        if (partEntry != null) {
          DartEntryImpl dartCopy = partEntry.getWritableCopy();
          dartCopy.invalidateAllResolutionInformation();
          sourceMap.put(unitSource, dartCopy);
        }
      }
    }
  }

  /**
   * Return {@code true} if this library is, or depends on, dart:html.
   * 
   * @param library the library being tested
   * @param visitedLibraries a collection of the libraries that have been visited, used to prevent
   *          infinite recursion
   * @return {@code true} if this library is, or depends on, dart:html
   */
  private boolean isClient(LibraryElement library, Source htmlSource,
      HashSet<LibraryElement> visitedLibraries) {
    if (visitedLibraries.contains(library)) {
      return false;
    }
    if (library.getSource().equals(htmlSource)) {
      return true;
    }
    visitedLibraries.add(library);
    for (LibraryElement imported : library.getImportedLibraries()) {
      if (isClient(imported, htmlSource, visitedLibraries)) {
        return true;
      }
    }
    for (LibraryElement exported : library.getExportedLibraries()) {
      if (isClient(exported, htmlSource, visitedLibraries)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return {@code true} if the given source is in the array of priority sources.
   * <p>
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   */
  private boolean isPrioritySource(Source source) {
    for (Source prioritySource : priorityOrder) {
      if (source.equals(prioritySource)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Perform a single analysis task.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @return {@code true} if work was done, implying that there might be more work to be done
   */
  private boolean performSingleAnalysisTask() {
    //
    // Look for a priority source that needs to be analyzed.
    //
    for (Source source : priorityOrder) {
      SourceEntry sourceEntry = sourceMap.get(source);
      if (sourceEntry instanceof DartEntry) {
        DartEntry dartEntry = (DartEntry) sourceEntry;
        CacheState parsedUnitState = dartEntry.getState(DartEntry.PARSED_UNIT);
        if (parsedUnitState == CacheState.INVALID || parsedUnitState == CacheState.FLUSHED) {
          safelyParseCompilationUnit(source, dartEntry);
          return true;
        }
        for (Source librarySource : getLibrariesContaining(source)) {
          SourceEntry libraryEntry = sourceMap.get(librarySource);
          if (libraryEntry instanceof DartEntry) {
            CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
            if (elementState == CacheState.INVALID || elementState == CacheState.FLUSHED) {
              safelyResolveCompilationUnit(librarySource);
              return true;
            }
          }
          if (dartEntry.getState(DartEntry.RESOLVED_UNIT, librarySource) == CacheState.FLUSHED) {
            safelyResolveCompilationUnit(source, librarySource);
            return true;
          }
        }
      } else if (sourceEntry instanceof HtmlEntry) {
        HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
        CacheState parsedUnitState = htmlEntry.getState(HtmlEntry.PARSED_UNIT);
        if (parsedUnitState == CacheState.INVALID || parsedUnitState == CacheState.FLUSHED) {
          safelyParseHtmlUnit(source);
          return true;
        }
        CacheState elementState = htmlEntry.getState(HtmlEntry.ELEMENT);
        if (elementState == CacheState.INVALID || elementState == CacheState.FLUSHED) {
          safelyResolveHtmlUnit(source);
          return true;
        }
      }
    }
    //
    // Look for a non-priority source that needs to be parsed.
    //
    for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
      SourceEntry sourceEntry = entry.getValue();
      if (sourceEntry instanceof DartEntry) {
        DartEntry dartEntry = (DartEntry) sourceEntry;
        if (dartEntry.getState(DartEntry.PARSED_UNIT) == CacheState.INVALID) {
          safelyParseCompilationUnit(entry.getKey(), dartEntry);
          return true;
        }
      } else if (sourceEntry instanceof HtmlEntry) {
        HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
        if (htmlEntry.getState(HtmlEntry.PARSED_UNIT) == CacheState.INVALID) {
          safelyParseHtmlUnit(entry.getKey());
          return true;
        }
      }
    }
    //
    // Look for a non-priority source that needs to be resolved.
    //
    for (Map.Entry<Source, SourceEntry> entry : sourceMap.entrySet()) {
      SourceEntry sourceEntry = entry.getValue();
      if (sourceEntry instanceof DartEntry && sourceEntry.getKind() == SourceKind.LIBRARY) {
        DartEntry dartEntry = (DartEntry) sourceEntry;
        if (dartEntry.getState(DartEntry.ELEMENT) == CacheState.INVALID) {
          safelyResolveCompilationUnit(entry.getKey());
          return true;
        }
      } else if (sourceEntry instanceof HtmlEntry) {
        HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
        if (htmlEntry.getState(HtmlEntry.ELEMENT) == CacheState.INVALID) {
          safelyResolveHtmlUnit(entry.getKey());
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Given a cache entry and a library element, record the library element and other information
   * gleaned from the element in the cache entry.
   * 
   * @param dartCopy the cache entry in which data is to be recorded
   * @param library the library element used to record information
   * @param htmlSource the source for the HTML library
   */
  private void recordElementData(DartEntryImpl dartCopy, LibraryElement library, Source htmlSource) {
    dartCopy.setValue(DartEntry.ELEMENT, library);
    dartCopy.setValue(DartEntry.IS_LAUNCHABLE, library.getEntryPoint() != null);
    dartCopy.setValue(
        DartEntry.IS_CLIENT,
        isClient(library, htmlSource, new HashSet<LibraryElement>()));
    ArrayList<Source> unitSources = new ArrayList<Source>();
    unitSources.add(library.getDefiningCompilationUnit().getSource());
    for (CompilationUnitElement part : library.getParts()) {
      Source partSource = part.getSource();
      unitSources.add(partSource);
    }
    dartCopy.setValue(DartEntry.INCLUDED_PARTS, unitSources.toArray(new Source[unitSources.size()]));
  }

  /**
   * Record the result of using the given resolver to resolve one or more libraries.
   * 
   * @param resolver the resolver that has the needed results
   * @throws AnalysisException if the results cannot be retrieved for some reason
   */
  private void recordResolutionResults(LibraryResolver resolver) throws AnalysisException {
    Source htmlSource = sourceFactory.forUri(DartSdk.DART_HTML);
    RecordingErrorListener errorListener = resolver.getErrorListener();
    for (Library library : resolver.getResolvedLibraries()) {
      Source librarySource = library.getLibrarySource();
      for (Source source : library.getCompilationUnitSources()) {
        CompilationUnit unit = library.getAST(source);
        AnalysisError[] errors = errorListener.getErrors(source);
        unit.setResolutionErrors(errors);
        LineInfo lineInfo = unit.getLineInfo();
        synchronized (cacheLock) {
          DartEntry dartEntry = getDartEntry(source);
          if (dartEntry != null) {
            long sourceTime = source.getModificationStamp();
            long resultTime = dartEntry.getModificationTime();
            if (sourceTime == resultTime) {
              DartEntryImpl dartCopy = dartEntry.getWritableCopy();
              dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
              dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.FLUSHED);
              dartCopy.setValue(DartEntry.RESOLVED_UNIT, librarySource, unit);
              dartCopy.setValue(DartEntry.RESOLUTION_ERRORS, librarySource, errors);
              if (source == librarySource) {
                recordElementData(dartCopy, library.getLibraryElement(), htmlSource);
              }
              sourceMap.put(source, dartCopy);

              ChangeNoticeImpl notice = getNotice(source);
              notice.setCompilationUnit(unit);
              notice.setErrors(dartCopy.getAllErrors(), lineInfo);
            } else {
              // The source has changed without the context being notified. Simulate notification.
              sourceChanged(source);
            }
          }
        }
      }
    }
  }

  /**
   * Remove and return one source from the list of recently used sources whose AST structure can be
   * flushed from the cache. The source that will be returned will be the source that has been
   * unreferenced for the longest period of time but that is not a priority for analysis.
   * 
   * @return the source that was removed
   *         <p>
   *         Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   */
  private Source removeAstToFlush() {
    for (int i = 0; i < recentlyUsed.size(); i++) {
      Source source = recentlyUsed.get(i);
      if (!isPrioritySource(source)) {
        return recentlyUsed.remove(i);
      }
    }
    AnalysisEngine.getInstance().getLogger().logError(
        "Internal error: The number of priority sources is greater than the maximum cache size",
        new Exception());
    return recentlyUsed.remove(0);
  }

  /**
   * Return the result of resolving the URI of the given URI-based directive against the URI of the
   * given library, or {@code null} if the URI is not valid.
   * 
   * @param librarySource the source representing the library containing the directive
   * @param directive the directive which URI should be resolved
   * @return the result of resolving the URI against the URI of the library
   */
  private Source resolveSource(Source librarySource, UriBasedDirective directive) {
    StringLiteral uriLiteral = directive.getUri();
    if (uriLiteral instanceof StringInterpolation) {
      return null;
    }
    String uriContent = uriLiteral.getStringValue().trim();
    if (uriContent == null || uriContent.isEmpty()) {
      return null;
    }
    uriContent = UriUtilities.encode(uriContent);
    try {
      new URI(uriContent);
      return sourceFactory.resolveUri(librarySource, uriContent);
    } catch (URISyntaxException exception) {
      return null;
    }
  }

  /**
   * Parse the given source and update the cache.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source to be parsed
   * @param dartEntry the cache entry associated with the source
   */
  private void safelyParseCompilationUnit(Source source, DartEntry dartEntry) {
    DartEntryImpl dartCopy = dartEntry.getWritableCopy();
    try {
      internalParseCompilationUnit(dartCopy, source);
    } catch (AnalysisException exception) {
      if (!(exception.getCause() instanceof IOException)) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not parse " + source.getFullName(),
            exception);
      }
    }
    sourceMap.put(source, dartCopy);
  }

  /**
   * Parse the given source and update the cache.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source to be parsed
   */
  private void safelyParseHtmlUnit(Source source) {
    try {
      parseHtmlUnit(source);
    } catch (AnalysisException exception) {
      if (!(exception.getCause() instanceof IOException)) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not parse " + source.getFullName(),
            exception);
      }
    }
  }

  /**
   * Resolve the given source and update the cache.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source to be resolved
   */
  private void safelyResolveCompilationUnit(Source source) {
    try {
      computeLibraryElement(source);
    } catch (AnalysisException exception) {
      // Ignored
    }
  }

  /**
   * Resolve the given source within the given library and update the cache.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param unitSource the source to be resolved
   * @param librarySource the source
   */
  private void safelyResolveCompilationUnit(Source unitSource, Source librarySource) {
    try {
      resolveCompilationUnit(unitSource, librarySource);
    } catch (AnalysisException exception) {
      DartEntryImpl dartCopy = getReadableDartEntry(unitSource).getWritableCopy();
      dartCopy.recordResolutionError();
      sourceMap.put(unitSource, dartCopy);
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not resolve " + unitSource.getFullName() + " in " + librarySource.getFullName(),
          exception);
    }
  }

  /**
   * Resolve the given source and update the cache.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source to be resolved
   */
  private void safelyResolveHtmlUnit(Source source) {
    try {
      computeHtmlElement(source);
    } catch (AnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not resolve " + source.getFullName(),
          exception);
    }
  }

  private HtmlScanResult scanHtml(Source source) throws AnalysisException {
    HtmlScanner scanner = new HtmlScanner(source);
    try {
      source.getContents(scanner);
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    }
    return scanner.getResult();
  }

  /**
   * Create an entry for the newly added source. Return {@code true} if the new source is a Dart
   * file.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been added
   * @return {@code true} if the new source is a Dart file
   */
  private boolean sourceAvailable(Source source) {
    SourceEntry sourceEntry = sourceMap.get(source);
    if (sourceEntry == null) {
      sourceEntry = createSourceEntry(source);
    } else {
      SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
      sourceCopy.setModificationTime(source.getModificationStamp());
      sourceMap.put(source, sourceCopy);
    }
    return sourceEntry instanceof DartEntry;
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been changed
   */
  private void sourceChanged(Source source) {
    SourceEntry sourceEntry = sourceMap.get(source);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      htmlCopy.setModificationTime(source.getModificationStamp());
      htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.INVALID);
      htmlCopy.setState(SourceEntry.LINE_INFO, CacheState.INVALID);
      htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.INVALID);
      htmlCopy.setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.INVALID);
      htmlCopy.setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.INVALID);
      sourceMap.put(source, htmlCopy);
    } else if (sourceEntry instanceof DartEntry) {
//      HashSet<Source> librariesToInvalidate = new HashSet<Source>();
      Source[] containingLibraries = getLibrariesContaining(source);
//      for (Source containingLibrary : containingLibraries) {
//        librariesToInvalidate.add(containingLibrary);
//        for (Source dependentLibrary : getLibrariesDependingOn(containingLibrary)) {
//          librariesToInvalidate.add(dependentLibrary);
//        }
//      }

      DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
      dartCopy.setModificationTime(source.getModificationStamp());
      dartCopy.setState(DartEntry.ELEMENT, CacheState.INVALID);
      dartCopy.setState(SourceEntry.LINE_INFO, CacheState.INVALID);
      dartCopy.setState(DartEntry.PARSE_ERRORS, CacheState.INVALID);
      dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.INVALID);
      dartCopy.setState(DartEntry.SOURCE_KIND, CacheState.INVALID);
      sourceMap.put(source, dartCopy);

//      for (Source library : librariesToInvalidate) {
      for (Source library : containingLibraries) {
        invalidateLibraryResolution(library);
      }
    }
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been deleted
   */
  private void sourceRemoved(Source source) {
    // TODO(brianwilkerson) Determine whether the source should be removed (that is, whether
    // there are no additional dependencies on the source), and if so remove all information
    // about the source.
    DartEntry dartEntry = getDartEntry(source);
    if (dartEntry != null) {
      HashSet<Source> libraries = new HashSet<Source>();
      for (Source librarySource : getLibrariesContaining(source)) {
        libraries.add(librarySource);
        for (Source dependentLibrary : getLibrariesDependingOn(librarySource)) {
          libraries.add(dependentLibrary);
        }
      }
      for (Source librarySource : libraries) {
        invalidateLibraryResolution(librarySource);
      }
    }
    sourceMap.remove(source);
  }

  /**
   * Efficiently convert the given set of sources to an array.
   * 
   * @param sources the set to be converted
   * @return an array containing all of the sources in the given set
   */
  private Source[] toArray(HashSet<Source> sources) {
    int size = sources.size();
    if (size == 0) {
      return Source.EMPTY_ARRAY;
    }
    return sources.toArray(new Source[size]);
  }
}

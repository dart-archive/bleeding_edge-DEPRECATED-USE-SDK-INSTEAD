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
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.cache.AnalysisCache;
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
import com.google.dart.engine.internal.resolver.Library;
import com.google.dart.engine.internal.resolver.LibraryResolver;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.task.AnalysisTask;
import com.google.dart.engine.internal.task.AnalysisTaskVisitor;
import com.google.dart.engine.internal.task.GenerateDartHintsTask;
import com.google.dart.engine.internal.task.ParseDartTask;
import com.google.dart.engine.internal.task.ParseHtmlTask;
import com.google.dart.engine.internal.task.ResolveDartLibraryTask;
import com.google.dart.engine.internal.task.ResolveDartUnitTask;
import com.google.dart.engine.internal.task.ResolveHtmlTask;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.collection.ListUtilities;
import com.google.dart.engine.utilities.os.OSUtilities;
import com.google.dart.engine.utilities.source.LineInfo;

import java.io.IOException;
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
   * Instances of the class {@code AnalysisTaskResultRecorder} are used by an analysis context to
   * record the results of a task.
   */
  private class AnalysisTaskResultRecorder implements AnalysisTaskVisitor<SourceEntry> {
    @Override
    public SourceEntry visitGenerateDartHintsTask(GenerateDartHintsTask task)
        throws AnalysisException {
      return recordGenerateDartHintsTask(task);
    }

    @Override
    public DartEntry visitParseDartTask(ParseDartTask task) throws AnalysisException {
      return recordParseDartTaskResults(task);
    }

    @Override
    public HtmlEntry visitParseHtmlTask(ParseHtmlTask task) throws AnalysisException {
      return recordParseHtmlTaskResults(task);
    }

    @Override
    public DartEntry visitResolveDartLibraryTask(ResolveDartLibraryTask task)
        throws AnalysisException {
      return recordResolveDartLibraryTaskResults(task);
    }

    @Override
    public SourceEntry visitResolveDartUnitTask(ResolveDartUnitTask task) throws AnalysisException {
      return recordResolveDartUnitTaskResults(task);
    }

    @Override
    public SourceEntry visitResolveHtmlTask(ResolveHtmlTask task) throws AnalysisException {
      return recordResolveHtmlTaskResults(task);
    }
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
  private final AnalysisCache cache = new AnalysisCache(MAX_CACHE_SIZE);

  /**
   * A table mapping sources to the change notices that are waiting to be returned related to that
   * source.
   */
  private HashMap<Source, ChangeNoticeImpl> pendingNotices = new HashMap<Source, ChangeNoticeImpl>();

  /**
   * The object used to synchronize access to all of the caches. The rules related to the use of
   * this lock object are
   * <ul>
   * <li>no analysis work is done while holding the lock, and</li>
   * <li>no analysis results can be recorded unless we have obtained the lock and validated that the
   * results are for the same version (modification time) of the source as our current cache
   * content.</li>
   * </ul>
   */
  private Object cacheLock = new Object();

  /**
   * The object used to record the results of performing an analysis task.
   */
  private AnalysisTaskResultRecorder resultRecorder;

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
   * Initialize a newly created analysis context.
   */
  public AnalysisContextImpl() {
    super();
    resultRecorder = new AnalysisTaskResultRecorder();
  }

  @Override
  public void addSourceInfo(Source source, SourceEntry info) {
    // This implementation assumes that the access to the cache does not need to be synchronized
    // because no other object can have access to this context while this method is being invoked.
    cache.put(source, info);
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
        for (Map.Entry<Source, SourceEntry> mapEntry : cache.entrySet()) {
          SourceEntry sourceEntry = mapEntry.getValue();
          if (!mapEntry.getKey().isInSystemLibrary() && sourceEntry instanceof DartEntry) {
            DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
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
      ListUtilities.addAll(errors, getDartParseData(source, dartEntry, DartEntry.PARSE_ERRORS));
      dartEntry = getReadableDartEntry(source);
      if (dartEntry.getValue(DartEntry.SOURCE_KIND) == SourceKind.LIBRARY) {
        ListUtilities.addAll(
            errors,
            getDartResolutionData(source, source, dartEntry, DartEntry.RESOLUTION_ERRORS));
      } else {
        Source[] libraries = getLibrariesContaining(source);
        for (Source librarySource : libraries) {
          ListUtilities.addAll(
              errors,
              getDartResolutionData(source, librarySource, dartEntry, DartEntry.RESOLUTION_ERRORS));
        }
      }
      // TODO(brianwilkerson) Gather other kinds of errors when their generation is moved out of
      // resolution into separate phases.
      //addAll(errors, getDartHintData(source, librarySource, dartEntry, DartEntry.HINTS));
      if (errors.isEmpty()) {
        return AnalysisError.NO_ERRORS;
      }
      return errors.toArray(new AnalysisError[errors.size()]);
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      return getHtmlResolutionData(source, htmlEntry, HtmlEntry.RESOLUTION_ERRORS);
    }
    return AnalysisError.NO_ERRORS;
  }

  @Override
  public Source[] computeExportedLibraries(Source source) throws AnalysisException {
    return getDartParseData(source, DartEntry.EXPORTED_LIBRARIES, Source.EMPTY_ARRAY);
  }

  @Override
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
    return getHtmlResolutionData(source, HtmlEntry.ELEMENT, null);
  }

  @Override
  public Source[] computeImportedLibraries(Source source) throws AnalysisException {
    return getDartParseData(source, DartEntry.IMPORTED_LIBRARIES, Source.EMPTY_ARRAY);
  }

  @Override
  public SourceKind computeKindOf(Source source) {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry == null) {
      return SourceKind.UNKNOWN;
    } else if (sourceEntry instanceof DartEntry) {
      try {
        return getDartParseData(source, (DartEntry) sourceEntry, DartEntry.SOURCE_KIND);
      } catch (AnalysisException exception) {
        return SourceKind.UNKNOWN;
      }
    }
    return sourceEntry.getKind();
  }

  @Override
  public LibraryElement computeLibraryElement(Source source) throws AnalysisException {
    return getDartResolutionData(source, source, DartEntry.ELEMENT, null);
  }

  @Override
  public LineInfo computeLineInfo(Source source) throws AnalysisException {
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry instanceof HtmlEntry) {
      return getHtmlParseData(source, SourceEntry.LINE_INFO, null);
    } else if (sourceEntry instanceof DartEntry) {
      return getDartParseData(source, SourceEntry.LINE_INFO, null);
    }
    return null;
  }

  @Override
  public ResolvableCompilationUnit computeResolvableCompilationUnit(Source source)
      throws AnalysisException {
    while (true) {
      synchronized (cacheLock) {
        DartEntry dartEntry = getReadableDartEntry(source);
        if (dartEntry == null) {
          throw new AnalysisException("computeResolvableCompilationUnit for non-Dart: "
              + source.getFullName());
        }
        if (dartEntry.getState(DartEntry.PARSED_UNIT) == CacheState.ERROR) {
          throw new AnalysisException(
              "Internal error: computeResolvableCompilationUnit could not parse "
                  + source.getFullName());
        }
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        CompilationUnit unit = dartCopy.getResolvableCompilationUnit();
        if (unit != null) {
          cache.put(source, dartCopy);
          return new ResolvableCompilationUnit(dartCopy.getModificationTime(), unit);
        }
      }
      cacheDartParseData(source, getReadableDartEntry(source), DartEntry.PARSED_UNIT);
    }
  }

  @Override
  public ResolvableHtmlUnit computeResolvableHtmlUnit(Source source) throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      throw new AnalysisException("computeResolvableHtmlUnit invoked for non-HTML file: "
          + source.getFullName());
    }
    htmlEntry = cacheHtmlParseData(source, htmlEntry, HtmlEntry.PARSED_UNIT);
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
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
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
          for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
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
          for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
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
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
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
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
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
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null || sourceEntry.getKind() != SourceKind.PART) {
        return new Source[] {source};
      }
      ArrayList<Source> librarySources = new ArrayList<Source>();
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
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
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
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
    Namespace namespace = null;
    if (dartEntry.getValue(DartEntry.ELEMENT) == library) {
      namespace = dartEntry.getValue(DartEntry.PUBLIC_NAMESPACE);
    }
    if (namespace == null) {
      NamespaceBuilder builder = new NamespaceBuilder();
      namespace = builder.createPublicNamespace(library);
      synchronized (cacheLock) {
        dartEntry = getReadableDartEntry(source);
        if (dartEntry == null) {
          AnalysisEngine.getInstance().getLogger().logError(
              new AnalysisException("A Dart file became a non-Dart file: " + source.getFullName()));
          return null;
        }
        if (dartEntry.getValue(DartEntry.ELEMENT) == library) {
          DartEntryImpl dartCopy = getReadableDartEntry(source).getWritableCopy();
          dartCopy.setValue(DartEntry.PUBLIC_NAMESPACE, namespace);
          cache.put(source, dartCopy);
        }
      }
    }
    return namespace;
  }

  @Override
  public Namespace getPublicNamespace(Source source) throws AnalysisException {
    // TODO(brianwilkerson) Rename this to not start with 'get'. Note that this is not part of the
    // API of the interface.
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return null;
    }
    Namespace namespace = dartEntry.getValue(DartEntry.PUBLIC_NAMESPACE);
    if (namespace == null) {
      LibraryElement library = computeLibraryElement(source);
      if (library == null) {
        return null;
      }
      NamespaceBuilder builder = new NamespaceBuilder();
      namespace = builder.createPublicNamespace(library);
      synchronized (cacheLock) {
        dartEntry = getReadableDartEntry(source);
        if (dartEntry == null) {
          throw new AnalysisException("A Dart file became a non-Dart file: " + source.getFullName());
        }
        if (dartEntry.getValue(DartEntry.ELEMENT) == library) {
          DartEntryImpl dartCopy = getReadableDartEntry(source).getWritableCopy();
          dartCopy.setValue(DartEntry.PUBLIC_NAMESPACE, namespace);
          cache.put(source, dartCopy);
        }
      }
    }
    return namespace;
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
   * method duplicates, and must therefore be kept in sync with, {@link #getNextTaskAnalysisTask()}.
   * This method is intended to be used for testing purposes only.
   * 
   * @return a list of the sources that would be processed by {@link #performAnalysisTask()}
   */
  @VisibleForTesting
  public List<Source> getSourcesNeedingProcessing() {
    HashSet<Source> sources = new HashSet<Source>();
    synchronized (cacheLock) {
      //
      // Look for a priority source that needs to be analyzed.
      //
      for (Source source : cache.getPriorityOrder()) {
        SourceEntry sourceEntry = cache.get(source);
        if (sourceEntry instanceof DartEntry) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          CacheState parseErrorsState = dartEntry.getState(DartEntry.PARSE_ERRORS);
          if (parseErrorsState == CacheState.INVALID || parseErrorsState == CacheState.FLUSHED) {
            sources.add(source);
          }
          CacheState parseUnitState = dartEntry.getState(DartEntry.PARSED_UNIT);
          if (parseUnitState == CacheState.INVALID || parseUnitState == CacheState.FLUSHED) {
            sources.add(source);
          }
          for (Source librarySource : getLibrariesContaining(source)) {
            SourceEntry libraryEntry = cache.get(librarySource);
            if (libraryEntry instanceof DartEntry) {
              CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
              if (elementState == CacheState.INVALID || elementState == CacheState.FLUSHED) {
                sources.add(source);
              }
              CacheState resolvedUnitState = dartEntry.getState(
                  DartEntry.RESOLVED_UNIT,
                  librarySource);
              if (resolvedUnitState == CacheState.INVALID
                  || resolvedUnitState == CacheState.FLUSHED) {
                LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
                if (libraryElement != null) {
                  sources.add(source);
                }
              }
            }
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          CacheState parsedUnitState = htmlEntry.getState(HtmlEntry.PARSED_UNIT);
          if (parsedUnitState == CacheState.INVALID || parsedUnitState == CacheState.FLUSHED) {
            sources.add(source);
          }
          CacheState elementState = htmlEntry.getState(HtmlEntry.ELEMENT);
          if (elementState == CacheState.INVALID || elementState == CacheState.FLUSHED) {
            sources.add(source);
          }
        }
      }
      //
      // Look for a non-priority source that needs to be parsed.
      //
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry instanceof DartEntry) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          if (dartEntry.getState(DartEntry.PARSED_UNIT) == CacheState.INVALID) {
            sources.add(entry.getKey());
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          if (htmlEntry.getState(HtmlEntry.PARSED_UNIT) == CacheState.INVALID) {
            sources.add(entry.getKey());
          }
        }
      }
      //
      // Look for a non-priority source that needs to be resolved.
      //
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry instanceof DartEntry && sourceEntry.getKind() == SourceKind.LIBRARY) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          if (dartEntry.getState(DartEntry.ELEMENT) == CacheState.INVALID) {
            sources.add(entry.getKey());
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          if (htmlEntry.getState(HtmlEntry.ELEMENT) == CacheState.INVALID) {
            sources.add(entry.getKey());
          }
        }
      }
    }
    return new ArrayList<Source>(sources);
  }

  @Override
  public AnalysisContentStatistics getStatistics() {
    AnalysisContentStatisticsImpl statistics = new AnalysisContentStatisticsImpl();
    synchronized (cacheLock) {
      for (Entry<Source, SourceEntry> mapEntry : cache.entrySet()) {
        SourceEntry entry = mapEntry.getValue();
        if (entry instanceof DartEntry) {
          Source source = mapEntry.getKey();
          DartEntry dartEntry = (DartEntry) entry;
          SourceKind kind = dartEntry.getValue(DartEntry.SOURCE_KIND);
          // get library independent values
          statistics.putCacheItem(dartEntry, DartEntry.PARSE_ERRORS);
          statistics.putCacheItem(dartEntry, DartEntry.PARSED_UNIT);
          statistics.putCacheItem(dartEntry, DartEntry.SOURCE_KIND);
          statistics.putCacheItem(dartEntry, DartEntry.LINE_INFO);
          if (kind == SourceKind.LIBRARY) {
            statistics.putCacheItem(dartEntry, DartEntry.ELEMENT);
            statistics.putCacheItem(dartEntry, DartEntry.EXPORTED_LIBRARIES);
            statistics.putCacheItem(dartEntry, DartEntry.IMPORTED_LIBRARIES);
            statistics.putCacheItem(dartEntry, DartEntry.INCLUDED_PARTS);
            statistics.putCacheItem(dartEntry, DartEntry.IS_CLIENT);
            statistics.putCacheItem(dartEntry, DartEntry.IS_LAUNCHABLE);
            // The public namespace isn't computed by performAnalysisTask() and therefore isn't
            // interesting.
            //statistics.putCacheItem(dartEntry, DartEntry.PUBLIC_NAMESPACE);
          }
          // get library-specific values
          Source[] librarySources = getLibrariesContaining(source);
          for (Source librarySource : librarySources) {
            // TODO(brianwilkerson) Restore the line below when hints are being computed separately.
//            statistics.putCacheItem(dartEntry, librarySource, DartEntry.HINTS);
            statistics.putCacheItem(dartEntry, librarySource, DartEntry.RESOLUTION_ERRORS);
            statistics.putCacheItem(dartEntry, librarySource, DartEntry.RESOLVED_UNIT);
          }
//        } else if (entry instanceof HtmlEntry) {
//          HtmlEntry htmlEntry = (HtmlEntry) entry;
        }
      }
    }
    return statistics;
  }

  @Override
  public TimestampedData<CompilationUnit> internalResolveCompilationUnit(Source unitSource,
      LibraryElement libraryElement) throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(unitSource);
    if (dartEntry == null) {
      throw new AnalysisException("internalResolveCompilationUnit invoked for non-Dart file: "
          + unitSource.getFullName());
    }
    Source librarySource = libraryElement.getSource();
    dartEntry = cacheDartResolutionData(
        unitSource,
        librarySource,
        dartEntry,
        DartEntry.RESOLVED_UNIT);
    return new TimestampedData<CompilationUnit>(
        dartEntry.getModificationTime(),
        dartEntry.getValue(DartEntry.RESOLVED_UNIT, librarySource));
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
      for (Map.Entry<Source, SourceEntry> entry : ((AnalysisContextImpl) context).cache.entrySet()) {
        Source newSource = entry.getKey();
        SourceEntry existingEntry = getReadableSourceEntry(newSource);
        if (existingEntry == null) {
          // TODO(brianwilkerson) Decide whether we really need to copy the info.
          cache.put(newSource, entry.getValue().getWritableCopy());
        } else {
          // TODO(brianwilkerson) Decide whether/how to merge the entries.
        }
      }
    }
  }

  @Override
  public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
    return getDartParseData(source, DartEntry.PARSED_UNIT, null);
  }

  @Override
  public HtmlUnit parseHtmlUnit(Source source) throws AnalysisException {
    return getHtmlParseData(source, HtmlEntry.PARSED_UNIT, null);
  }

  @Override
  public ChangeNotice[] performAnalysisTask() {
    AnalysisTask task = getNextTaskAnalysisTask();
    if (task == null) {
      return getChangeNotices(true);
    }
    try {
      task.perform(resultRecorder);
    } catch (AnalysisException exception) {
      if (!(exception.getCause() instanceof IOException)) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Internal error while performing the task: " + task,
            exception);
      }
    }
    return getChangeNotices(false);
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
        DartEntry dartEntry = getReadableDartEntry(librarySource);
        if (dartEntry != null) {
          DartEntryImpl dartCopy = dartEntry.getWritableCopy();
          recordElementData(dartCopy, library, htmlSource);
          cache.put(librarySource, dartCopy);
        }
      }
    }
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, LibraryElement library)
      throws AnalysisException {
    if (library == null) {
      return null;
    }
    return resolveCompilationUnit(unitSource, library.getSource());
  }

  @Override
  public CompilationUnit resolveCompilationUnit(Source unitSource, Source librarySource)
      throws AnalysisException {
    return getDartResolutionData(unitSource, librarySource, DartEntry.RESOLVED_UNIT, null);
  }

  @Override
  public HtmlUnit resolveHtmlUnit(Source htmlSource) throws AnalysisException {
    // There is currently no difference between the parsed and resolved forms of an HTML
    // unit. This code needs to change if resolution ever modifies the AST.
    return parseHtmlUnit(htmlSource);
  }

  @Override
  public void setAnalysisOptions(AnalysisOptions options) {
    synchronized (cacheLock) {
      this.options = options;
      invalidateAllResolutionInformation();
    }
  }

  @Override
  public void setAnalysisPriorityOrder(List<Source> sources) {
    synchronized (cacheLock) {
      if (sources == null || sources.isEmpty()) {
        cache.setPriorityOrder(Source.EMPTY_ARRAY);
      } else {
        while (sources.remove(null)) {
          // Nothing else to do.
        }
        if (sources.isEmpty()) {
          cache.setPriorityOrder(Source.EMPTY_ARRAY);
        }
        //
        // Cap the size of the priority list to being less than the cache size. Failure to do so can
        // result in an infinite loop in performAnalysisTask() because re-caching one AST structure
        // can cause another priority source's AST structure to be flushed.
        //
        int count = Math.min(sources.size(), MAX_PRIORITY_LIST_SIZE);
        Source[] priorityOrder = new Source[count];
        for (int i = 0; i < count; i++) {
          priorityOrder[i] = sources.get(i);
        }
        cache.setPriorityOrder(priorityOrder);
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
      invalidateAllResolutionInformation();
    }
  }

  @Override
  @Deprecated
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
   * Add all of the sources contained in the given source container to the given list of sources.
   * <p>
   * Note: This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param sources the list to which sources are to be added
   * @param container the source container containing the sources to be added to the list
   */
  private void addSourcesInContainer(ArrayList<Source> sources, SourceContainer container) {
    for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
      Source source = entry.getKey();
      if (container.contains(source)) {
        sources.add(source);
      }
    }
  }

  /**
   * Return {@code true} if the modification times of the sources used by the given library resolver
   * to resolve one or more libraries are consistent with the modification times in the cache.
   * 
   * @param resolver the library resolver used to resolve one or more libraries
   * @return {@code true} if we should record the results of the resolution
   * @throws AnalysisException if any of the modification times could not be determined (this should
   *           not happen)
   */
  private boolean allModificationTimesMatch(LibraryResolver resolver) throws AnalysisException {
    boolean allTimesMatch = true;
    for (Library library : resolver.getResolvedLibraries()) {
      for (Source source : library.getCompilationUnitSources()) {
        DartEntry dartEntry = getReadableDartEntry(source);
        if (dartEntry == null) {
          // This shouldn't be possible because we should never have performed the task if the
          // source didn't represent a Dart file, but check to be safe.
          throw new AnalysisException(
              "Internal error: attempting to reolve non-Dart file as a Dart file: "
                  + source.getFullName());
        }
        long sourceTime = source.getModificationStamp();
        long resultTime = library.getModificationTime(source);
        if (sourceTime != resultTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          allTimesMatch = false;
        }
      }
    }
    return allTimesMatch;
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
  private DartEntry cacheDartParseData(Source source, DartEntry dartEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    if (descriptor == DartEntry.PARSED_UNIT) {
      CompilationUnit unit = dartEntry.getAnyParsedCompilationUnit();
      if (unit != null) {
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
      dartEntry = (DartEntry) new ParseDartTask(this, source).perform(resultRecorder);
      state = dartEntry.getState(descriptor);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * the data represented by the given descriptor is available. This method assumes that the data
   * can be produced by resolving the source in the context of the library if it is not already
   * cached.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private DartEntry cacheDartResolutionData(Source unitSource, Source librarySource,
      DartEntry dartEntry, DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = (descriptor == DartEntry.ELEMENT) ? dartEntry.getState(descriptor)
        : dartEntry.getState(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      // TODO(brianwilkerson) As an optimization, if we already have the element model for the
      // library we can use ResolveDartUnitTask to produce the resolved AST structure much faster.
      dartEntry = (DartEntry) new ResolveDartLibraryTask(this, unitSource, librarySource).perform(resultRecorder);
      state = (descriptor == DartEntry.ELEMENT) ? dartEntry.getState(descriptor)
          : dartEntry.getState(descriptor, librarySource);
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
  private HtmlEntry cacheHtmlParseData(Source source, HtmlEntry htmlEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = htmlEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      htmlEntry = (HtmlEntry) new ParseHtmlTask(this, source).perform(resultRecorder);
      state = htmlEntry.getState(descriptor);
    }
    return htmlEntry;
  }

  /**
   * Given a source for an HTML file, return a cache entry in which the the data represented by the
   * given descriptor is available. This method assumes that the data can be produced by resolving
   * the source if it is not already cached.
   * 
   * @param source the source representing the HTML file
   * @param dartEntry the cache entry associated with the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private HtmlEntry cacheHtmlResolutionData(Source source, HtmlEntry htmlEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = htmlEntry.getState(descriptor);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      htmlEntry = (HtmlEntry) new ResolveHtmlTask(this, source).perform(resultRecorder);
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
      cache.put(source, htmlEntry);
      return htmlEntry;
    } else {
      DartEntryImpl dartEntry = new DartEntryImpl();
      dartEntry.setModificationTime(source.getModificationStamp());
      cache.put(source, dartEntry);
      return dartEntry;
    }
  }

  /**
   * Return an array containing all of the change notices that are waiting to be returned. If there
   * are no notices, then return either {@code null} or an empty array, depending on the value of
   * the argument.
   * 
   * @param nullIfEmpty {@code true} if {@code null} should be returned when there are no notices
   * @return the change notices that are waiting to be returned
   */
  private ChangeNotice[] getChangeNotices(boolean nullIfEmpty) {
    synchronized (cacheLock) {
      if (pendingNotices.isEmpty()) {
        if (nullIfEmpty) {
          return null;
        }
        return ChangeNoticeImpl.EMPTY_ARRAY;
      }
      ChangeNotice[] notices = pendingNotices.values().toArray(
          new ChangeNotice[pendingNotices.size()]);
      pendingNotices.clear();
      return notices;
    }
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
  @SuppressWarnings("unchecked")
  private <E> E getDartParseData(Source source, DartEntry dartEntry, DataDescriptor<E> descriptor)
      throws AnalysisException {
    dartEntry = cacheDartParseData(source, dartEntry, descriptor);
    if (descriptor == DartEntry.PARSED_UNIT) {
      return (E) dartEntry.getAnyParsedCompilationUnit();
    }
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
  private <E> E getDartParseData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return defaultValue;
    }
    return getDartParseData(source, dartEntry, descriptor);
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
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getDartResolutionData(Source unitSource, Source librarySource, DartEntry dartEntry,
      DataDescriptor<E> descriptor) throws AnalysisException {
    dartEntry = cacheDartResolutionData(unitSource, librarySource, dartEntry, descriptor);
    if (descriptor == DartEntry.ELEMENT) {
      return dartEntry.getValue(descriptor);
    }
    return dartEntry.getValue(descriptor, librarySource);
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source, or the given default value if the
   * source is not a Dart file. This method assumes that the data can be produced by resolving the
   * source in the context of the library if it is not already cached.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getDartResolutionData(Source unitSource, Source librarySource,
      DataDescriptor<E> descriptor, E defaultValue) throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(unitSource);
    if (dartEntry == null) {
      return defaultValue;
    }
    return getDartResolutionData(unitSource, librarySource, dartEntry, descriptor);
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not an HTML file. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   * 
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not an HTML file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private <E> E getHtmlParseData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      return defaultValue;
    }
    htmlEntry = cacheHtmlParseData(source, htmlEntry, descriptor);
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
  private <E> E getHtmlResolutionData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      return defaultValue;
    }
    return getHtmlResolutionData(source, htmlEntry, descriptor);
  }

  /**
   * Given a source for an HTML file, return the data represented by the given descriptor that is
   * associated with that source. This method assumes that the data can be produced by resolving the
   * source if it is not already cached.
   * 
   * @param source the source representing the HTML file
   * @param htmlEntry the entry representing the HTML file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getHtmlResolutionData(Source source, HtmlEntry htmlEntry,
      DataDescriptor<E> descriptor) throws AnalysisException {
    htmlEntry = cacheHtmlResolutionData(source, htmlEntry, descriptor);
    return htmlEntry.getValue(descriptor);
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
      for (Source source : cache.getPriorityOrder()) {
        SourceEntry sourceEntry = cache.get(source);
        if (sourceEntry instanceof DartEntry) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          CacheState parseErrorsState = dartEntry.getState(DartEntry.PARSE_ERRORS);
          if (parseErrorsState == CacheState.INVALID || parseErrorsState == CacheState.FLUSHED) {
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setState(DartEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
            cache.put(source, dartCopy);
            return new ParseDartTask(this, source);
          }
          CacheState parseUnitState = dartEntry.getState(DartEntry.PARSED_UNIT);
          if (parseUnitState == CacheState.INVALID || parseUnitState == CacheState.FLUSHED) {
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.IN_PROCESS);
            cache.put(source, dartCopy);
            return new ParseDartTask(this, source);
          }
          for (Source librarySource : getLibrariesContaining(source)) {
            SourceEntry libraryEntry = cache.get(librarySource);
            if (libraryEntry instanceof DartEntry) {
              CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
              if (elementState == CacheState.INVALID || elementState == CacheState.FLUSHED) {
                DartEntryImpl libraryCopy = ((DartEntry) libraryEntry).getWritableCopy();
                libraryCopy.setState(DartEntry.ELEMENT, CacheState.IN_PROCESS);
                cache.put(librarySource, libraryCopy);
                return new ResolveDartLibraryTask(this, source, librarySource);
              }
              CacheState resolvedUnitState = dartEntry.getState(
                  DartEntry.RESOLVED_UNIT,
                  librarySource);
              if (resolvedUnitState == CacheState.INVALID
                  || resolvedUnitState == CacheState.FLUSHED) {
                LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
                if (libraryElement != null) {
                  DartEntryImpl dartCopy = dartEntry.getWritableCopy();
                  dartCopy.setState(DartEntry.RESOLVED_UNIT, librarySource, CacheState.IN_PROCESS);
                  cache.put(source, dartCopy);
                  return new ResolveDartUnitTask(this, source, libraryElement);
                }
              }
//              CacheState hintsState = dartEntry.getState(DartEntry.HINTS, librarySource);
//              if (hintsState == CacheState.INVALID || hintsState == CacheState.FLUSHED) {
//                LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
//                if (libraryElement != null) {
//                  DartEntryImpl dartCopy = dartEntry.getWritableCopy();
//                  dartCopy.setState(DartEntry.HINTS, librarySource, CacheState.IN_PROCESS);
//                  cache.put(source, dartCopy);
//                  return new GenerateDartHintsTask(this, libraryElement);
//                }
//              }
            }
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          CacheState parsedUnitState = htmlEntry.getState(HtmlEntry.PARSED_UNIT);
          if (parsedUnitState == CacheState.INVALID || parsedUnitState == CacheState.FLUSHED) {
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.IN_PROCESS);
            cache.put(source, htmlCopy);
            return new ParseHtmlTask(this, source);
          }
          CacheState elementState = htmlEntry.getState(HtmlEntry.ELEMENT);
          if (elementState == CacheState.INVALID || elementState == CacheState.FLUSHED) {
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.IN_PROCESS);
            cache.put(source, htmlCopy);
            return new ResolveHtmlTask(this, source);
          }
        }
      }
      //
      // Look for a non-priority source that needs to be parsed.
      //
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry instanceof DartEntry) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          if (dartEntry.getState(DartEntry.PARSED_UNIT) == CacheState.INVALID) {
            Source source = entry.getKey();
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setState(DartEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
            cache.put(source, dartCopy);
            return new ParseDartTask(this, source);
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          if (htmlEntry.getState(HtmlEntry.PARSED_UNIT) == CacheState.INVALID) {
            Source source = entry.getKey();
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.IN_PROCESS);
            cache.put(source, htmlCopy);
            return new ParseHtmlTask(this, source);
          }
        }
      }
      //
      // Look for a non-priority source that needs to be resolved.
      //
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry instanceof DartEntry && sourceEntry.getKind() == SourceKind.LIBRARY) {
          DartEntry dartEntry = (DartEntry) sourceEntry;
          if (dartEntry.getState(DartEntry.ELEMENT) == CacheState.INVALID) {
            Source source = entry.getKey();
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setState(DartEntry.ELEMENT, CacheState.IN_PROCESS);
            cache.put(source, dartCopy);
            return new ResolveDartLibraryTask(this, source, source);
          }
        } else if (sourceEntry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
          if (htmlEntry.getState(HtmlEntry.ELEMENT) == CacheState.INVALID) {
            Source source = entry.getKey();
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.IN_PROCESS);
            cache.put(source, htmlCopy);
            return new ResolveHtmlTask(this, source);
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
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        sourceEntry = createSourceEntry(source);
      }
      if (sourceEntry instanceof DartEntry) {
        cache.accessed(source);
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
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        sourceEntry = createSourceEntry(source);
      }
      if (sourceEntry instanceof HtmlEntry) {
        cache.accessed(source);
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
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        sourceEntry = createSourceEntry(source);
      }
      if (sourceEntry != null) {
        cache.accessed(source);
      }
      return sourceEntry;
    }
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
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
        if (entry.getValue().getKind() == kind) {
          sources.add(entry.getKey());
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
  }

  /**
   * Invalidate all of the resolution results computed by this context.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   */
  private void invalidateAllResolutionInformation() {
    for (Map.Entry<Source, SourceEntry> mapEntry : cache.entrySet()) {
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
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param librarySource the source of the library being invalidated
   */
  private void invalidateLibraryResolution(Source librarySource) {
    // TODO(brianwilkerson) This could be optimized. There's no need to flush all of these caches if
    // the public namespace hasn't changed, which will be a fairly common case. The question is
    // whether we can afford the time to compute the namespace to look for differences.
    DartEntry libraryEntry = getReadableDartEntry(librarySource);
    if (libraryEntry != null) {
      Source[] includedParts = libraryEntry.getValue(DartEntry.INCLUDED_PARTS);
      DartEntryImpl libraryCopy = libraryEntry.getWritableCopy();
      libraryCopy.invalidateAllResolutionInformation();
      libraryCopy.setState(DartEntry.INCLUDED_PARTS, CacheState.INVALID);
      cache.put(librarySource, libraryCopy);
      for (Source partSource : includedParts) {
        SourceEntry partEntry = cache.get(partSource);
        if (partEntry instanceof DartEntry) {
          DartEntryImpl partCopy = ((DartEntry) partEntry).getWritableCopy();
          partCopy.invalidateAllResolutionInformation();
          cache.put(partSource, partCopy);
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
   * Record the results produced by performing a {@link GenerateDartHintsTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordGenerateDartHintsTask(GenerateDartHintsTask task)
      throws AnalysisException {
    Source librarySource = task.getLibraryElement().getSource();
    AnalysisException thrownException = task.getException();
    DartEntry libraryEntry = null;
    for (Map.Entry<Source, TimestampedData<AnalysisError[]>> entry : task.getHintMap().entrySet()) {
      Source unitSource = entry.getKey();
      TimestampedData<AnalysisError[]> results = entry.getValue();
      synchronized (cacheLock) {
        SourceEntry sourceEntry = cache.get(unitSource);
        if (!(sourceEntry instanceof DartEntry)) {
          // This shouldn't be possible because we should never have performed the task if the source
          // didn't represent a Dart file, but check to be safe.
          throw new AnalysisException(
              "Internal error: attempting to parse non-Dart file as a Dart file: "
                  + unitSource.getFullName());
        }
        DartEntry dartEntry = (DartEntry) sourceEntry;
        if (unitSource.equals(librarySource)) {
          libraryEntry = dartEntry;
        }
        cache.accessed(unitSource);
        long sourceTime = unitSource.getModificationStamp();
        long resultTime = results.getModificationTime();
        if (sourceTime == resultTime) {
          if (dartEntry.getModificationTime() != sourceTime) {
            // The source has changed without the context being notified. Simulate notification.
            sourceChanged(unitSource);
            dartEntry = getReadableDartEntry(unitSource);
            if (dartEntry == null) {
              throw new AnalysisException("A Dart file became a non-Dart file: "
                  + unitSource.getFullName());
            }
          }
          DartEntryImpl dartCopy = dartEntry.getWritableCopy();
          if (thrownException == null) {
            dartCopy.setValue(DartEntry.HINTS, results.getData());
            ChangeNoticeImpl notice = getNotice(unitSource);
            notice.setErrors(dartEntry.getAllErrors(), dartCopy.getValue(SourceEntry.LINE_INFO));
          } else {
            dartCopy.setState(DartEntry.HINTS, CacheState.ERROR);
          }
          dartCopy.setException(thrownException);
          cache.put(unitSource, dartCopy);
          dartEntry = dartCopy;
        } else {
          if (dartEntry.getState(DartEntry.HINTS) == CacheState.IN_PROCESS) {
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            if (thrownException == null || resultTime >= 0L) {
              //
              // The analysis was performed on out-of-date sources. Mark the cache so that the sources
              // will be re-analyzed using the up-to-date sources.
              //
              dartCopy.setState(DartEntry.HINTS, CacheState.INVALID);
            } else {
              //
              // We could not determine whether the sources were up-to-date or out-of-date. Mark the
              // cache so that we won't attempt to re-analyze the sources until there's a good chance
              // that we'll be able to do so without error.
              //
              dartCopy.setState(DartEntry.HINTS, CacheState.ERROR);
            }
            dartCopy.setException(thrownException);
            cache.put(unitSource, dartCopy);
            dartEntry = dartCopy;
          }
        }
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return libraryEntry;
  }

  /**
   * Record the results produced by performing a {@link ParseDartTask}. If the results were computed
   * from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordParseDartTaskResults(ParseDartTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to parse non-Dart file as a Dart file: "
                + source.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      cache.accessed(source);
      long sourceTime = source.getModificationStamp();
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (dartEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          dartEntry = getReadableDartEntry(source);
          if (dartEntry == null) {
            throw new AnalysisException("A Dart file became a non-Dart file: "
                + source.getFullName());
          }
        }
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null) {
          LineInfo lineInfo = task.getLineInfo();
          dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
          if (task.hasPartOfDirective() && !task.hasLibraryDirective()) {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.PART);
          } else {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.LIBRARY);
          }
          dartCopy.setValue(DartEntry.PARSED_UNIT, task.getCompilationUnit());
          dartCopy.setValue(DartEntry.PARSE_ERRORS, task.getErrors());
          dartCopy.setValue(DartEntry.EXPORTED_LIBRARIES, task.getExportedSources());
          dartCopy.setValue(DartEntry.IMPORTED_LIBRARIES, task.getImportedSources());
          dartCopy.setValue(DartEntry.INCLUDED_PARTS, task.getIncludedSources());

//        ChangeNoticeImpl notice = getNotice(source);
//        notice.setErrors(dartEntry.getAllErrors(), task.getLineInfo());
        } else {
          dartCopy.recordParseError();
        }
        dartCopy.setException(thrownException);
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      } else {
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
          dartCopy.recordParseNotInProcess();
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.recordParseError();
        }
        dartCopy.setException(thrownException);
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Record the results produced by performing a {@link ParseHtmlTask}. If the results were computed
   * from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private HtmlEntry recordParseHtmlTaskResults(ParseHtmlTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to parse non-HTML file as a HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      cache.accessed(source);
      long sourceTime = source.getModificationStamp();
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
          if (htmlEntry == null) {
            throw new AnalysisException("An HTML file became a non-HTML file: "
                + source.getFullName());
          }
        }
        HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
        if (thrownException == null) {
          LineInfo lineInfo = task.getLineInfo();
          HtmlUnit unit = task.getHtmlUnit();
          htmlCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
          htmlCopy.setValue(HtmlEntry.PARSED_UNIT, unit);
          htmlCopy.setValue(HtmlEntry.REFERENCED_LIBRARIES, task.getReferencedLibraries());

          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(htmlEntry.getAllErrors(), lineInfo);
        } else {
          htmlCopy.setState(SourceEntry.LINE_INFO, CacheState.ERROR);
          htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.ERROR);
          htmlCopy.setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.ERROR);
        }
        htmlCopy.setException(thrownException);
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
          if (htmlCopy.getState(SourceEntry.LINE_INFO) == CacheState.IN_PROCESS) {
            htmlCopy.setState(SourceEntry.LINE_INFO, CacheState.INVALID);
          }
          if (htmlCopy.getState(HtmlEntry.PARSED_UNIT) == CacheState.IN_PROCESS) {
            htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.INVALID);
          }
          if (htmlCopy.getState(HtmlEntry.REFERENCED_LIBRARIES) == CacheState.IN_PROCESS) {
            htmlCopy.setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.INVALID);
          }
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          htmlCopy.setState(SourceEntry.LINE_INFO, CacheState.ERROR);
          htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.ERROR);
          htmlCopy.setState(HtmlEntry.REFERENCED_LIBRARIES, CacheState.ERROR);
        }
        htmlCopy.setException(thrownException);
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
  }

  /**
   * Record the results produced by performing a {@link ResolveDartLibraryTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordResolveDartLibraryTaskResults(ResolveDartLibraryTask task)
      throws AnalysisException {
    LibraryResolver resolver = task.getLibraryResolver();
    AnalysisException thrownException = task.getException();
    DartEntry unitEntry = null;
    if (resolver != null) {
      //
      // The resolver should only be null if an exception was thrown before (or while) it was
      // being created.
      //
      synchronized (cacheLock) {
        if (allModificationTimesMatch(resolver)) {
          Source htmlSource = getSourceFactory().forUri(DartSdk.DART_HTML);
          Source unitSource = task.getUnitSource();
          RecordingErrorListener errorListener = resolver.getErrorListener();
          for (Library library : resolver.getResolvedLibraries()) {
            Source librarySource = library.getLibrarySource();
            for (Source source : library.getCompilationUnitSources()) {
              CompilationUnit unit = library.getAST(source);
              AnalysisError[] errors = errorListener.getErrors(source);
              unit.setResolutionErrors(errors);
              LineInfo lineInfo = unit.getLineInfo();
              DartEntry dartEntry = (DartEntry) cache.get(source);
              long sourceTime = source.getModificationStamp();
              if (dartEntry.getModificationTime() != sourceTime) {
                // The source has changed without the context being notified. Simulate notification.
                sourceChanged(source);
                dartEntry = getReadableDartEntry(source);
                if (dartEntry == null) {
                  throw new AnalysisException("A Dart file became a non-Dart file: "
                      + source.getFullName());
                }
              }
              DartEntryImpl dartCopy = dartEntry.getWritableCopy();
              if (thrownException == null) {
                dartCopy.setValue(SourceEntry.LINE_INFO, lineInfo);
                dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.FLUSHED);
                dartCopy.setValue(DartEntry.RESOLVED_UNIT, librarySource, unit);
                dartCopy.setValue(DartEntry.RESOLUTION_ERRORS, librarySource, errors);
                if (source == librarySource) {
                  recordElementData(dartCopy, library.getLibraryElement(), htmlSource);
                }
              } else {
                dartCopy.recordResolutionError();
              }
              dartCopy.setException(thrownException);
              cache.put(source, dartCopy);
              if (source.equals(unitSource)) {
                unitEntry = dartCopy;
              }

              ChangeNoticeImpl notice = getNotice(source);
              notice.setCompilationUnit(unit);
              notice.setErrors(dartCopy.getAllErrors(), lineInfo);
            }
          }
        } else {
          for (Library library : resolver.getResolvedLibraries()) {
            for (Source source : library.getCompilationUnitSources()) {
              DartEntry dartEntry = getReadableDartEntry(source);
              if (dartEntry != null) {
                long resultTime = library.getModificationTime(source);
                DartEntryImpl dartCopy = dartEntry.getWritableCopy();
                if (thrownException == null || resultTime >= 0L) {
                  //
                  // The analysis was performed on out-of-date sources. Mark the cache so that the sources
                  // will be re-analyzed using the up-to-date sources.
                  //
                  dartCopy.recordResolutionNotInProcess();
                } else {
                  //
                  // We could not determine whether the sources were up-to-date or out-of-date. Mark the
                  // cache so that we won't attempt to re-analyze the sources until there's a good chance
                  // that we'll be able to do so without error.
                  //
                  dartCopy.recordResolutionError();
                }
                dartCopy.setException(thrownException);
                cache.put(source, dartCopy);
              }
            }
          }
        }
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return unitEntry;
  }

  /**
   * Record the results produced by performing a {@link ResolveDartUnitTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private SourceEntry recordResolveDartUnitTaskResults(ResolveDartUnitTask task)
      throws AnalysisException {
    Source unitSource = task.getSource();
    Source librarySource = task.getLibrarySource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(unitSource);
      if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to reolve non-Dart file as a Dart file: "
                + unitSource.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      cache.accessed(unitSource);
      long sourceTime = unitSource.getModificationStamp();
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (dartEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(unitSource);
          dartEntry = getReadableDartEntry(unitSource);
          if (dartEntry == null) {
            throw new AnalysisException("A Dart file became a non-Dart file: "
                + unitSource.getFullName());
          }
        }
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null) {
          dartCopy.setValue(DartEntry.RESOLVED_UNIT, librarySource, task.getResolvedUnit());
        } else {
          dartCopy.setState(DartEntry.RESOLVED_UNIT, librarySource, CacheState.ERROR);
        }
        dartCopy.setException(thrownException);
        cache.put(unitSource, dartCopy);
        dartEntry = dartCopy;
      } else {
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
          if (dartCopy.getState(DartEntry.RESOLVED_UNIT) == CacheState.IN_PROCESS) {
            dartCopy.setState(DartEntry.RESOLVED_UNIT, librarySource, CacheState.INVALID);
          }
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.setState(DartEntry.RESOLVED_UNIT, librarySource, CacheState.ERROR);
        }
        dartCopy.setException(thrownException);
        cache.put(unitSource, dartCopy);
        dartEntry = dartCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return dartEntry;
  }

  /**
   * Record the results produced by performing a {@link ResolveHtmlTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private SourceEntry recordResolveHtmlTaskResults(ResolveHtmlTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to reolve non-HTML file as an HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      cache.accessed(source);
      long sourceTime = source.getModificationStamp();
      long resultTime = task.getModificationTime();
      if (sourceTime == resultTime) {
        if (htmlEntry.getModificationTime() != sourceTime) {
          // The source has changed without the context being notified. Simulate notification.
          sourceChanged(source);
          htmlEntry = getReadableHtmlEntry(source);
          if (htmlEntry == null) {
            throw new AnalysisException("An HTML file became a non-HTML file: "
                + source.getFullName());
          }
        }
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null) {
          htmlCopy.setValue(HtmlEntry.ELEMENT, task.getElement());
          htmlCopy.setValue(HtmlEntry.RESOLUTION_ERRORS, task.getResolutionErrors());
        } else {
          htmlCopy.recordResolutionError();
        }
        htmlCopy.setException(thrownException);
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
          if (htmlCopy.getState(HtmlEntry.ELEMENT) == CacheState.IN_PROCESS) {
            htmlCopy.setState(HtmlEntry.ELEMENT, CacheState.INVALID);
          }
          if (htmlCopy.getState(HtmlEntry.RESOLUTION_ERRORS) == CacheState.IN_PROCESS) {
            htmlCopy.setState(HtmlEntry.RESOLUTION_ERRORS, CacheState.INVALID);
          }
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          htmlCopy.recordResolutionError();
        }
        htmlCopy.setException(thrownException);
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return htmlEntry;
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
    SourceEntry sourceEntry = cache.get(source);
    if (sourceEntry == null) {
      sourceEntry = createSourceEntry(source);
    } else {
      SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
      sourceCopy.setModificationTime(source.getModificationStamp());
      cache.put(source, sourceCopy);
    }
    return sourceEntry instanceof DartEntry;
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been changed
   */
  private void sourceChanged(Source source) {
    SourceEntry sourceEntry = cache.get(source);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      htmlCopy.setModificationTime(source.getModificationStamp());
      htmlCopy.invalidateAllInformation();
      cache.put(source, htmlCopy);
    } else if (sourceEntry instanceof DartEntry) {
//      HashSet<Source> librariesToInvalidate = new HashSet<Source>();
      Source[] containingLibraries = getLibrariesContaining(source);
//      for (Source containingLibrary : containingLibraries) {
//        librariesToInvalidate.add(containingLibrary);
//        for (Source dependentLibrary : getLibrariesDependingOn(containingLibrary)) {
//          librariesToInvalidate.add(dependentLibrary);
//        }
//      }

//    for (Source library : librariesToInvalidate) {
      for (Source library : containingLibraries) {
        invalidateLibraryResolution(library);
      }

      DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
      dartCopy.setModificationTime(source.getModificationStamp());
      dartCopy.invalidateAllInformation();
      cache.put(source, dartCopy);
    }
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been deleted
   */
  private void sourceRemoved(Source source) {
    SourceEntry sourceEntry = cache.get(source);
    if (sourceEntry instanceof DartEntry) {
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
    cache.remove(source);
  }
}

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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AnnotatedNode;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisContentStatistics;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.context.ObsoleteSourceAnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularHasTemplateElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.cache.AnalysisCache;
import com.google.dart.engine.internal.cache.CacheRetentionPolicy;
import com.google.dart.engine.internal.cache.CacheState;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DartEntryImpl;
import com.google.dart.engine.internal.cache.DataDescriptor;
import com.google.dart.engine.internal.cache.HtmlEntry;
import com.google.dart.engine.internal.cache.HtmlEntryImpl;
import com.google.dart.engine.internal.cache.RetentionPriority;
import com.google.dart.engine.internal.cache.SourceEntry;
import com.google.dart.engine.internal.cache.SourceEntryImpl;
import com.google.dart.engine.internal.element.ElementImpl;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.internal.resolver.Library;
import com.google.dart.engine.internal.resolver.LibraryResolver;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.resolver.TypeProviderImpl;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.task.AnalysisTask;
import com.google.dart.engine.internal.task.AnalysisTaskVisitor;
import com.google.dart.engine.internal.task.GenerateDartErrorsTask;
import com.google.dart.engine.internal.task.GenerateDartHintsTask;
import com.google.dart.engine.internal.task.GetContentTask;
import com.google.dart.engine.internal.task.IncrementalAnalysisTask;
import com.google.dart.engine.internal.task.ParseDartTask;
import com.google.dart.engine.internal.task.ParseHtmlTask;
import com.google.dart.engine.internal.task.ResolveAngularComponentTemplateTask;
import com.google.dart.engine.internal.task.ResolveAngularEntryHtmlTask;
import com.google.dart.engine.internal.task.ResolveDartDependenciesTask;
import com.google.dart.engine.internal.task.ResolveDartLibraryTask;
import com.google.dart.engine.internal.task.ResolveDartUnitTask;
import com.google.dart.engine.internal.task.ResolveHtmlTask;
import com.google.dart.engine.internal.task.ScanDartTask;
import com.google.dart.engine.internal.task.WaitForAsyncTask;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.Source.ContentReceiver;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.utilities.collection.ListUtilities;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.engine.utilities.os.OSUtilities;
import com.google.dart.engine.utilities.source.LineInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
    public DartEntry visitGenerateDartErrorsTask(GenerateDartErrorsTask task)
        throws AnalysisException {
      return recordGenerateDartErrorsTask(task);
    }

    @Override
    public DartEntry visitGenerateDartHintsTask(GenerateDartHintsTask task)
        throws AnalysisException {
      return recordGenerateDartHintsTask(task);
    }

    @Override
    public SourceEntry visitGetContentTask(GetContentTask task) throws AnalysisException {
      return recordGetContentsTask(task);
    }

    @Override
    public DartEntry visitIncrementalAnalysisTask(IncrementalAnalysisTask task)
        throws AnalysisException {
      return recordIncrementalAnalysisTaskResults(task);
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
    public HtmlEntry visitResolveAngularComponentTemplateTask(
        ResolveAngularComponentTemplateTask task) throws AnalysisException {
      return recordResolveAngularComponentTemplateTaskResults(task);
    }

    @Override
    public HtmlEntry visitResolveAngularEntryHtmlTask(ResolveAngularEntryHtmlTask task)
        throws AnalysisException {
      return recordResolveAngularEntryHtmlTaskResults(task);
    }

    @Override
    public DartEntry visitResolveDartDependenciesTask(ResolveDartDependenciesTask task)
        throws AnalysisException {
      return recordResolveDartDependenciesTaskResults(task);
    }

    @Override
    public DartEntry visitResolveDartLibraryTask(ResolveDartLibraryTask task)
        throws AnalysisException {
      return recordResolveDartLibraryTaskResults(task);
    }

    @Override
    public DartEntry visitResolveDartUnitTask(ResolveDartUnitTask task) throws AnalysisException {
      return recordResolveDartUnitTaskResults(task);
    }

    @Override
    public HtmlEntry visitResolveHtmlTask(ResolveHtmlTask task) throws AnalysisException {
      return recordResolveHtmlTaskResults(task);
    }

    @Override
    public DartEntry visitScanDartTask(ScanDartTask task) throws AnalysisException {
      return recordScanDartTaskResults(task);
    }
  }

  private class ContextRetentionPolicy implements CacheRetentionPolicy {
    @Override
    public RetentionPriority getAstPriority(Source source, SourceEntry sourceEntry) {
      for (Source prioritySource : priorityOrder) {
        if (source.equals(prioritySource)) {
          return RetentionPriority.HIGH;
        }
      }
      if (sourceEntry instanceof DartEntry) {
        DartEntry dartEntry = (DartEntry) sourceEntry;
        if (astIsNeeded(dartEntry)) {
          return RetentionPriority.MEDIUM;
        }
      }
      return RetentionPriority.LOW;
    }

    private boolean astIsNeeded(DartEntry dartEntry) {
      return dartEntry.hasInvalidData(DartEntry.HINTS)
          || dartEntry.hasInvalidData(DartEntry.VERIFICATION_ERRORS)
          || dartEntry.hasInvalidData(DartEntry.RESOLUTION_ERRORS);
    }
  }

  /**
   * Instances of the class {@code TaskData} represent information about the next task to be
   * performed. Each data has an implicit associated source: the source that might need to be
   * analyzed. There are essentially three states that can be represented:
   * <ul>
   * <li>If {@link #getTask()} returns a non-{@code null} value, then that is the task that should
   * be executed to further analyze the associated source.
   * <li>Otherwise, if {@link #isBlocked()} returns {@code true}, then there is no work that can be
   * done, but analysis for the associated source is not complete.
   * <li>Otherwise, {@link #getDependentSource()} should return a source that needs to be analyzed
   * before the analysis of the associated source can be completed.
   * </ul>
   */
  private static class TaskData {
    /**
     * The task that is to be performed.
     */
    private AnalysisTask task;

    /**
     * A flag indicating whether the associated source is blocked waiting for its contents to be
     * loaded.
     */
    private boolean blocked;

    /**
     * The source that needs to be analyzed before further progress can be made on the associated
     * source.
     */
    private Source dependentSource;

    /**
     * Initialize a newly created data holder.
     * 
     * @param task the task that is to be performed
     * @param blocked {@code true} if the associated source is blocked waiting for its contents to
     *          be loaded
     * @param dependentSource t
     */
    public TaskData(AnalysisTask task, boolean blocked, Source dependentSource) {
      this.task = task;
      this.blocked = blocked;
      this.dependentSource = dependentSource;
    }

    /**
     * Return the source that needs to be analyzed before further progress can be made on the
     * associated source.
     * 
     * @return the source that needs to be analyzed before further progress can be made
     */
    public Source getDependentSource() {
      return dependentSource;
    }

    /**
     * Return the task that is to be performed, or {@code null} if there is no task associated with
     * the source.
     * 
     * @return the task that is to be performed
     */
    public AnalysisTask getTask() {
      return task;
    }

    /**
     * Return {@code true} if the associated source is blocked waiting for its contents to be
     * loaded.
     * 
     * @return {@code true} if the associated source is blocked waiting for its contents to be
     *         loaded
     */
    public boolean isBlocked() {
      return blocked;
    }
  }

  /**
   * The difference between the maximum cache size and the maximum priority order size. The priority
   * list must be capped so that it is less than the cache size. Failure to do so can result in an
   * infinite loop in performAnalysisTask() because re-caching one AST structure can cause another
   * priority source's AST structure to be flushed.
   */
  private static final int PRIORITY_ORDER_SIZE_DELTA = 4;

  /**
   * The set of analysis options controlling the behavior of this context.
   */
  private AnalysisOptionsImpl options = new AnalysisOptionsImpl();

  /**
   * A cache of content used to override the default content of a source.
   */
  private ContentCache contentCache = new ContentCache();

  /**
   * The source factory used to create the sources that can be analyzed in this context.
   */
  private SourceFactory sourceFactory;

  /**
   * A table mapping the sources known to the context to the information known about the source.
   */
  private final AnalysisCache cache;

  /**
   * An array containing sources for which data should not be flushed.
   */
  private Source[] priorityOrder = Source.EMPTY_ARRAY;

  /**
   * A table mapping sources to the change notices that are waiting to be returned related to that
   * source.
   */
  private HashMap<Source, ChangeNoticeImpl> pendingNotices = new HashMap<Source, ChangeNoticeImpl>();

  /**
   * A set containing information about the tasks that have been performed since the last change
   * notification. Used to detect infinite loops in {@link #performAnalysisTask()}.
   */
  private HashSet<String> recentTasks = new HashSet<String>();

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
   * Cached information used in incremental analysis or {@code null} if none. Synchronize against
   * {@link #cacheLock} before accessing this field.
   */
  private IncrementalAnalysisCache incrementalAnalysisCache;

  /**
   * The object used to manage the list of sources that need to be analyzed.
   */
  private WorkManager workManager = new WorkManager();

  /**
   * The set of {@link AngularApplication} in this context.
   */
  private final Set<AngularApplication> angularApplications = Sets.newHashSet();

  /**
   * Initialize a newly created analysis context.
   */
  public AnalysisContextImpl() {
    super();
    resultRecorder = new AnalysisTaskResultRecorder();
    cache = new AnalysisCache(AnalysisOptionsImpl.DEFAULT_CACHE_SIZE, new ContextRetentionPolicy());
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
      recentTasks.clear();
      //
      // First, compute the list of sources that have been removed.
      //
      ArrayList<Source> removedSources = new ArrayList<Source>(changeSet.getRemovedSources());
      for (SourceContainer container : changeSet.getRemovedContainers()) {
        addSourcesInContainer(removedSources, container);
      }
      //
      // Then determine which cached results are no longer valid.
      //
      boolean addedDartSource = false;
      for (Source source : changeSet.getAddedSources()) {
        if (sourceAvailable(source)) {
          addedDartSource = true;
        }
      }
      for (Source source : changeSet.getChangedSources()) {
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
        logInformation("Added Dart sources, invalidating all resolution information");
        for (Map.Entry<Source, SourceEntry> mapEntry : cache.entrySet()) {
          Source source = mapEntry.getKey();
          SourceEntry sourceEntry = mapEntry.getValue();
          if (!source.isInSystemLibrary() && sourceEntry instanceof DartEntry) {
            DartEntry dartEntry = (DartEntry) sourceEntry;
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            removeFromParts(source, dartEntry);
            dartCopy.invalidateAllResolutionInformation();
            mapEntry.setValue(dartCopy);
            SourcePriority priority = SourcePriority.UNKNOWN;
            SourceKind kind = dartCopy.getKind();
            if (kind == SourceKind.LIBRARY) {
              priority = SourcePriority.LIBRARY;
            } else if (kind == SourceKind.PART) {
              priority = SourcePriority.NORMAL_PART;
            }
            workManager.add(source, priority);
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
    AstNode nameNode = locator.searchWithin(unit);
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
    boolean enableHints = options.getHint();
    SourceEntry sourceEntry = getReadableSourceEntry(source);
    if (sourceEntry instanceof DartEntry) {
      ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();
      try {
        DartEntry dartEntry = (DartEntry) sourceEntry;
        ListUtilities.addAll(errors, getDartScanData(source, dartEntry, DartEntry.SCAN_ERRORS));
        dartEntry = getReadableDartEntry(source);
        ListUtilities.addAll(errors, getDartParseData(source, dartEntry, DartEntry.PARSE_ERRORS));
        dartEntry = getReadableDartEntry(source);
        if (dartEntry.getValue(DartEntry.SOURCE_KIND) == SourceKind.LIBRARY) {
          ListUtilities.addAll(
              errors,
              getDartResolutionData(source, source, dartEntry, DartEntry.RESOLUTION_ERRORS));
          dartEntry = getReadableDartEntry(source);
          ListUtilities.addAll(
              errors,
              getDartVerificationData(source, source, dartEntry, DartEntry.VERIFICATION_ERRORS));
          if (enableHints) {
            dartEntry = getReadableDartEntry(source);
            ListUtilities.addAll(
                errors,
                getDartHintData(source, source, dartEntry, DartEntry.HINTS));
          }
        } else {
          Source[] libraries = getLibrariesContaining(source);
          for (Source librarySource : libraries) {
            ListUtilities.addAll(
                errors,
                getDartResolutionData(source, librarySource, dartEntry, DartEntry.RESOLUTION_ERRORS));
            dartEntry = getReadableDartEntry(source);
            ListUtilities.addAll(
                errors,
                getDartVerificationData(
                    source,
                    librarySource,
                    dartEntry,
                    DartEntry.VERIFICATION_ERRORS));
            if (enableHints) {
              dartEntry = getReadableDartEntry(source);
              ListUtilities.addAll(
                  errors,
                  getDartHintData(source, librarySource, dartEntry, DartEntry.HINTS));
            }
          }
        }
      } catch (ObsoleteSourceAnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logInformation(
            "Could not compute errors",
            exception);
      }
      if (errors.isEmpty()) {
        return AnalysisError.NO_ERRORS;
      }
      return errors.toArray(new AnalysisError[errors.size()]);
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      try {
        return getHtmlResolutionData(source, htmlEntry, HtmlEntry.RESOLUTION_ERRORS);
      } catch (ObsoleteSourceAnalysisException exception) {
        AnalysisEngine.getInstance().getLogger().logInformation(
            "Could not compute errors",
            exception);
      }
    }
    return AnalysisError.NO_ERRORS;
  }

  @Override
  public Source[] computeExportedLibraries(Source source) throws AnalysisException {
    return getDartDependencyData(source, DartEntry.EXPORTED_LIBRARIES, Source.EMPTY_ARRAY);
  }

  @Override
  public HtmlElement computeHtmlElement(Source source) throws AnalysisException {
    return getHtmlResolutionData(source, HtmlEntry.ELEMENT, null);
  }

  @Override
  public Source[] computeImportedLibraries(Source source) throws AnalysisException {
    return getDartDependencyData(source, DartEntry.IMPORTED_LIBRARIES, Source.EMPTY_ARRAY);
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
    try {
      if (sourceEntry instanceof HtmlEntry) {
        return getHtmlParseData(source, SourceEntry.LINE_INFO, null);
      } else if (sourceEntry instanceof DartEntry) {
        return getDartScanData(source, SourceEntry.LINE_INFO, null);
      }
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + SourceEntry.LINE_INFO.toString(),
          exception);
    }
    return null;
  }

  @Override
  public ResolvableHtmlUnit computeResolvableAngularComponentHtmlUnit(Source source)
      throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      throw new AnalysisException(
          "computeResolvableAngularComponentHtmlUnit invoked for non-HTML file: "
              + source.getFullName());
    }
    htmlEntry = cacheHtmlResolutionData(source, htmlEntry, HtmlEntry.RESOLVED_UNIT);
    HtmlUnit unit = htmlEntry.getValue(HtmlEntry.RESOLVED_UNIT);
    if (unit == null) {
      AnalysisException cause = htmlEntry.getException();
      throw new AnalysisException(
          "Internal error: computeResolvableAngularComponentHtmlUnit could not resolve "
              + source.getFullName(),
          cause);
    }
    // If the unit is ever modified by resolution then we will need to create a copy of it.
    return new ResolvableHtmlUnit(htmlEntry.getModificationTime(), unit);
  }

  @Override
  public ResolvableCompilationUnit computeResolvableCompilationUnit(Source source)
      throws AnalysisException {
    synchronized (cacheLock) {
      DartEntry dartEntry = getReadableDartEntry(source);
      if (dartEntry == null) {
        throw new AnalysisException("computeResolvableCompilationUnit for non-Dart: "
            + source.getFullName());
      }
      dartEntry = cacheDartParseData(source, dartEntry, DartEntry.PARSED_UNIT);
      DartEntryImpl dartCopy = dartEntry.getWritableCopy();
      CompilationUnit unit = dartCopy.getResolvableCompilationUnit();
      if (unit == null) {
        throw new AnalysisException(
            "Internal error: computeResolvableCompilationUnit could not parse "
                + source.getFullName(),
            dartEntry.getException());
      }
      cache.put(source, dartCopy);
      return new ResolvableCompilationUnit(dartCopy.getModificationTime(), unit);
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
  public boolean exists(Source source) {
    if (source == null) {
      return false;
    }
    synchronized (cacheLock) {
      if (contentCache.getContents(source) != null) {
        return true;
      }
    }
    return source.exists();
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
  public CompilationUnitElement getCompilationUnitElement(Source unitSource, Source librarySource) {
    LibraryElement libraryElement = getLibraryElement(librarySource);
    if (libraryElement != null) {
      // try defining unit
      CompilationUnitElement definingUnit = libraryElement.getDefiningCompilationUnit();
      if (definingUnit.getSource().equals(unitSource)) {
        return definingUnit;
      }
      // try parts
      for (CompilationUnitElement partUnit : libraryElement.getParts()) {
        if (partUnit.getSource().equals(unitSource)) {
          return partUnit;
        }
      }
    }
    return null;
  }

  @Override
  public TimestampedData<CharSequence> getContents(Source source) throws Exception {
    synchronized (cacheLock) {
      String contents = contentCache.getContents(source);
      if (contents != null) {
        return new TimestampedData<CharSequence>(
            contentCache.getModificationStamp(source),
            contents);
      }
    }
    return source.getContents();
  }

  @Override
  @SuppressWarnings("deprecation")
  public void getContentsToReceiver(Source source, ContentReceiver receiver) throws Exception {
    synchronized (cacheLock) {
      String contents = contentCache.getContents(source);
      if (contents != null) {
        receiver.accept(contents, contentCache.getModificationStamp(source));
        return;
      }
    }
    source.getContentsToReceiver(receiver);
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
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return Source.EMPTY_ARRAY;
    }
    return dartEntry.getValue(DartEntry.CONTAINING_LIBRARIES);
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
  public long getModificationStamp(Source source) {
    synchronized (cacheLock) {
      Long stamp = contentCache.getModificationStamp(source);
      if (stamp != null) {
        return stamp.longValue();
      }
    }
    return source.getModificationStamp();
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
      namespace = builder.createPublicNamespaceForLibrary(library);
      synchronized (cacheLock) {
        dartEntry = getReadableDartEntry(source);
        if (dartEntry == null) {
          AnalysisEngine.getInstance().getLogger().logError(
              "Could not compute the public namespace for " + library.getSource().getFullName(),
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
      namespace = builder.createPublicNamespaceForLibrary(library);
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
  public Source[] getRefactoringUnsafeSources() {
    ArrayList<Source> sources = new ArrayList<Source>();
    synchronized (cacheLock) {
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
        SourceEntry sourceEntry = entry.getValue();
        if (sourceEntry instanceof DartEntry) {
          if (!((DartEntry) sourceEntry).isRefactoringSafe()) {
            sources.add(entry.getKey());
          }
        }
      }
    }
    return sources.toArray(new Source[sources.size()]);
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
      return ((DartEntry) sourceEntry).getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
    }
    return null;
  }

  @Override
  public HtmlUnit getResolvedHtmlUnit(Source htmlSource) {
    SourceEntry sourceEntry = getReadableSourceEntry(htmlSource);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      return htmlEntry.getValue(HtmlEntry.RESOLVED_UNIT);
    }
    return null;
  }

  @Override
  public SourceFactory getSourceFactory() {
    return sourceFactory;
  }

  /**
   * Return a list of the sources that would be processed by {@link #performAnalysisTask()}. This
   * method duplicates, and must therefore be kept in sync with, {@link #getNextAnalysisTask()}.
   * This method is intended to be used for testing purposes only.
   * 
   * @return a list of the sources that would be processed by {@link #performAnalysisTask()}
   */
  @VisibleForTesting
  public List<Source> getSourcesNeedingProcessing() {
    HashSet<Source> sources = new HashSet<Source>();
    synchronized (cacheLock) {
      boolean hintsEnabled = options.getHint();
      //
      // Look for priority sources that need to be analyzed.
      //
      for (Source source : priorityOrder) {
        getSourcesNeedingProcessing(source, cache.get(source), true, hintsEnabled, sources);
      }
      //
      // Look for non-priority sources that need to be analyzed.
      //
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
        getSourcesNeedingProcessing(entry.getKey(), entry.getValue(), false, hintsEnabled, sources);
      }
    }
    return new ArrayList<Source>(sources);
  }

  @Override
  public AnalysisContentStatistics getStatistics() {
    AnalysisContentStatisticsImpl statistics = new AnalysisContentStatisticsImpl();
    synchronized (cacheLock) {
      for (Entry<Source, SourceEntry> mapEntry : cache.entrySet()) {
        statistics.addSource(mapEntry.getKey());
        SourceEntry entry = mapEntry.getValue();
        if (entry instanceof DartEntry) {
          Source source = mapEntry.getKey();
          DartEntry dartEntry = (DartEntry) entry;
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
            // The public namespace isn't computed by performAnalysisTask() and therefore isn't
            // interesting.
            //statistics.putCacheItem(dartEntry, DartEntry.PUBLIC_NAMESPACE);
          }
          // get library-specific values
          Source[] librarySources = getLibrariesContaining(source);
          for (Source librarySource : librarySources) {
            statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.HINTS);
            statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.RESOLUTION_ERRORS);
            statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.RESOLVED_UNIT);
            statistics.putCacheItemInLibrary(dartEntry, librarySource, DartEntry.VERIFICATION_ERRORS);
          }
        } else if (entry instanceof HtmlEntry) {
          HtmlEntry htmlEntry = (HtmlEntry) entry;
          statistics.putCacheItem(htmlEntry, SourceEntry.LINE_INFO);
          statistics.putCacheItem(htmlEntry, HtmlEntry.PARSE_ERRORS);
          statistics.putCacheItem(htmlEntry, HtmlEntry.PARSED_UNIT);
          statistics.putCacheItem(htmlEntry, HtmlEntry.RESOLUTION_ERRORS);
          statistics.putCacheItem(htmlEntry, HtmlEntry.RESOLVED_UNIT);
          // We are not currently recording any hints related to HTML.
          // statistics.putCacheItem(htmlEntry, HtmlEntry.HINTS);
        }
      }
    }
    return statistics;
  }

  @Override
  public TypeProvider getTypeProvider() throws AnalysisException {
    Source coreSource = getSourceFactory().forUri(DartSdk.DART_CORE);
    return new TypeProviderImpl(computeLibraryElement(coreSource));
  }

  @Override
  public TimestampedData<CompilationUnit> internalParseCompilationUnit(Source source)
      throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      throw new AnalysisException("internalParseCompilationUnit invoked for non-Dart file: "
          + source.getFullName());
    }
    dartEntry = cacheDartParseData(source, dartEntry, DartEntry.PARSED_UNIT);
    CompilationUnit unit = dartEntry.getAnyParsedCompilationUnit();
    if (unit == null) {
      throw new AnalysisException("internalParseCompilationUnit could not cache a parsed unit: "
          + source.getFullName(), dartEntry.getException());
    }
    return new TimestampedData<CompilationUnit>(dartEntry.getModificationTime(), unit);
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
        dartEntry.getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource));
  }

  @Override
  public TimestampedData<Token> internalScanTokenStream(Source source) throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      throw new AnalysisException("internalScanTokenStream invoked for non-Dart file: "
          + source.getFullName());
    }
    dartEntry = cacheDartScanData(source, dartEntry, DartEntry.TOKEN_STREAM);
    return new TimestampedData<Token>(
        dartEntry.getModificationTime(),
        dartEntry.getValue(DartEntry.TOKEN_STREAM));
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
  public AnalysisResult performAnalysisTask() {
    long getStart = System.currentTimeMillis();
    AnalysisTask task = getNextAnalysisTask();
    long getEnd = System.currentTimeMillis();
    if (task == null && validateCacheConsistency()) {
      task = getNextAnalysisTask();
    }
    if (task == null) {
      return new AnalysisResult(getChangeNotices(true), getEnd - getStart, null, -1L);
    }
    String taskDescriptor = task.toString();
    if (recentTasks.add(taskDescriptor)) {
      logInformation("Performing task: " + taskDescriptor);
    } else {
      logInformation("*** Performing repeated task: " + taskDescriptor);
    }
    long performStart = System.currentTimeMillis();
    try {
      task.perform(resultRecorder);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not perform analysis task: " + taskDescriptor,
          exception);
    } catch (AnalysisException exception) {
      if (!(exception.getCause() instanceof IOException)) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Internal error while performing the task: " + task,
            exception);
      }
    }
    long performEnd = System.currentTimeMillis();
    return new AnalysisResult(
        getChangeNotices(false),
        getEnd - getStart,
        task.getClass().getName(),
        performEnd - performStart);
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
          recordElementData(dartEntry, dartCopy, library, library.getSource(), htmlSource);
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
    computeHtmlElement(htmlSource);
    return parseHtmlUnit(htmlSource);
  }

  @Override
  public void setAnalysisOptions(AnalysisOptions options) {
    synchronized (cacheLock) {
      boolean needsRecompute = this.options.getAnalyzeFunctionBodies() != options.getAnalyzeFunctionBodies()
          || this.options.getDart2jsHint() != options.getDart2jsHint()
          || (this.options.getHint() && !options.getHint())
          || this.options.getPreserveComments() != options.getPreserveComments();

      int cacheSize = options.getCacheSize();
      if (this.options.getCacheSize() != cacheSize) {
        this.options.setCacheSize(cacheSize);
        cache.setMaxCacheSize(cacheSize);
        //
        // Cap the size of the priority list to being less than the cache size. Failure to do so can
        // result in an infinite loop in performAnalysisTask() because re-caching one AST structure
        // can cause another priority source's AST structure to be flushed.
        //
        int maxPriorityOrderSize = cacheSize - PRIORITY_ORDER_SIZE_DELTA;
        if (priorityOrder.length > maxPriorityOrderSize) {
          Source[] newPriorityOrder = new Source[maxPriorityOrderSize];
          System.arraycopy(priorityOrder, 0, newPriorityOrder, 0, maxPriorityOrderSize);
          priorityOrder = newPriorityOrder;
        }
      }
      this.options.setAnalyzeFunctionBodies(options.getAnalyzeFunctionBodies());
      this.options.setDart2jsHint(options.getDart2jsHint());
      this.options.setHint(options.getHint());
      this.options.setIncremental(options.getIncremental());
      this.options.setPreserveComments(options.getPreserveComments());

      if (needsRecompute) {
        invalidateAllResolutionInformation();
      }
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
        if (sources.isEmpty()) {
          priorityOrder = Source.EMPTY_ARRAY;
        }
        //
        // Cap the size of the priority list to being less than the cache size. Failure to do so can
        // result in an infinite loop in performAnalysisTask() because re-caching one AST structure
        // can cause another priority source's AST structure to be flushed.
        //
        int count = Math.min(sources.size(), options.getCacheSize() - PRIORITY_ORDER_SIZE_DELTA);
        priorityOrder = new Source[count];
        for (int i = 0; i < count; i++) {
          priorityOrder[i] = sources.get(i);
        }
      }
    }
  }

  @Override
  public void setChangedContents(Source source, String contents, int offset, int oldLength,
      int newLength) {
    synchronized (cacheLock) {
      recentTasks.clear();
      String originalContents = contentCache.setContents(source, contents);
      if (contents != null) {
        if (!contents.equals(originalContents)) {
          if (options.getIncremental()) {
            incrementalAnalysisCache = IncrementalAnalysisCache.update(
                incrementalAnalysisCache,
                source,
                originalContents,
                contents,
                offset,
                oldLength,
                newLength,
                getReadableSourceEntry(source));
          }
          sourceChanged(source);
          SourceEntry sourceEntry = cache.get(source);
          if (sourceEntry != null) {
            SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
            sourceCopy.setModificationTime(contentCache.getModificationStamp(source));
            sourceCopy.setValue(SourceEntry.CONTENT, contents);
            cache.put(source, sourceCopy);
          }
        }
      } else if (originalContents != null) {
        incrementalAnalysisCache = IncrementalAnalysisCache.clear(incrementalAnalysisCache, source);
        sourceChanged(source);
      }
    }
  }

  @Override
  public void setContents(Source source, String contents) {
    synchronized (cacheLock) {
      recentTasks.clear();
      String originalContents = contentCache.setContents(source, contents);
      if (contents != null) {
        if (!contents.equals(originalContents)) {
          incrementalAnalysisCache = IncrementalAnalysisCache.clear(
              incrementalAnalysisCache,
              source);
          sourceChanged(source);
          SourceEntry sourceEntry = cache.get(source);
          if (sourceEntry != null) {
            SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
            sourceCopy.setModificationTime(contentCache.getModificationStamp(source));
            sourceCopy.setValue(SourceEntry.CONTENT, contents);
            cache.put(source, sourceCopy);
          }
        }
      } else if (originalContents != null) {
        incrementalAnalysisCache = IncrementalAnalysisCache.clear(incrementalAnalysisCache, source);
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

  /**
   * Record the results produced by performing a {@link ResolveDartLibraryTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  protected DartEntry recordResolveDartLibraryTaskResults(ResolveDartLibraryTask task)
      throws AnalysisException {
    LibraryResolver resolver = task.getLibraryResolver();
    AnalysisException thrownException = task.getException();
    DartEntry unitEntry = null;
    Source unitSource = task.getUnitSource();
    if (resolver != null) {
      //
      // The resolver should only be null if an exception was thrown before (or while) it was
      // being created.
      //
      Set<Library> resolvedLibraries = resolver.getResolvedLibraries();
      if (resolvedLibraries == null) {
        //
        // The resolved libraries should only be null if an exception was thrown during resolution.
        //
        unitEntry = getReadableDartEntry(unitSource);
        if (unitEntry == null) {
          throw new AnalysisException("A Dart file became a non-Dart file: "
              + unitSource.getFullName());
        }
        DartEntryImpl dartCopy = unitEntry.getWritableCopy();
        dartCopy.recordResolutionError();
        dartCopy.setException(thrownException);
        cache.put(unitSource, dartCopy);
        cache.remove(unitSource);
        if (thrownException != null) {
          throw thrownException;
        }
        return dartCopy;
      }
      synchronized (cacheLock) {
        if (allModificationTimesMatch(resolvedLibraries)) {
          Source htmlSource = getSourceFactory().forUri(DartSdk.DART_HTML);
          RecordingErrorListener errorListener = resolver.getErrorListener();
          for (Library library : resolvedLibraries) {
            Source librarySource = library.getLibrarySource();
            for (Source source : library.getCompilationUnitSources()) {
              CompilationUnit unit = library.getAST(source);
              AnalysisError[] errors = errorListener.getErrors(source);
              LineInfo lineInfo = getLineInfo(source);
              DartEntry dartEntry = (DartEntry) cache.get(source);
              long sourceTime = getModificationStamp(source);
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
                dartCopy.setValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource, unit);
                dartCopy.setValueInLibrary(DartEntry.RESOLUTION_ERRORS, librarySource, errors);
                if (source.equals(librarySource)) {
                  recordElementData(
                      dartEntry,
                      dartCopy,
                      library.getLibraryElement(),
                      librarySource,
                      htmlSource);
                }
                cache.storedAst(source);
              } else {
                dartCopy.recordResolutionError();
                cache.remove(source);
              }
              dartCopy.setException(thrownException);
              cache.put(source, dartCopy);
              if (!source.equals(librarySource)) {
                workManager.add(source, SourcePriority.PRIORITY_PART);
              }
              if (source.equals(unitSource)) {
                unitEntry = dartCopy;
              }

              ChangeNoticeImpl notice = getNotice(source);
              notice.setCompilationUnit(unit);
              notice.setErrors(dartCopy.getAllErrors(), lineInfo);
            }
          }
        } else {
          @SuppressWarnings("resource")
          PrintStringWriter writer = new PrintStringWriter();
          writer.println("Library resolution results discarded for");
          for (Library library : resolvedLibraries) {
            for (Source source : library.getCompilationUnitSources()) {
              DartEntry dartEntry = getReadableDartEntry(source);
              if (dartEntry != null) {
                long resultTime = library.getModificationTime(source);
                writer.println("  " + debuggingString(source) + "; sourceTime = "
                    + getModificationStamp(source) + ", resultTime = " + resultTime
                    + ", cacheTime = " + dartEntry.getModificationTime());
                DartEntryImpl dartCopy = dartEntry.getWritableCopy();
                if (thrownException == null || resultTime >= 0L) {
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
                  cache.remove(source);
                }
                dartCopy.setException(thrownException);
                cache.put(source, dartCopy);
                if (source.equals(unitSource)) {
                  unitEntry = dartCopy;
                }
              } else {
                writer.println("  " + debuggingString(source) + "; sourceTime = "
                    + getModificationStamp(source) + ", no entry");
              }
            }
          }
          logInformation(writer.toString());
        }
      }
    }
    if (thrownException != null) {
      throw thrownException;
    }
    if (unitEntry == null) {
      unitEntry = getReadableDartEntry(unitSource);
      if (unitEntry == null) {
        throw new AnalysisException("A Dart file became a non-Dart file: "
            + unitSource.getFullName());
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
  private void accessedAst(Source source) {
    synchronized (cacheLock) {
      cache.accessedAst(source);
    }
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
  private boolean allModificationTimesMatch(Set<Library> resolvedLibraries)
      throws AnalysisException {
    boolean allTimesMatch = true;
    for (Library library : resolvedLibraries) {
      for (Source source : library.getCompilationUnitSources()) {
        DartEntry dartEntry = getReadableDartEntry(source);
        if (dartEntry == null) {
          // This shouldn't be possible because we should never have performed the task if the
          // source didn't represent a Dart file, but check to be safe.
          throw new AnalysisException(
              "Internal error: attempting to resolve non-Dart file as a Dart file: "
                  + source.getFullName());
        }
        long sourceTime = getModificationStamp(source);
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
   * Given a source for a Dart file, return a cache entry in which the state of the data represented
   * by the given descriptor is either {@link CacheState#VALID} or {@link CacheState#ERROR}. This
   * method assumes that the data can be produced by resolving the directives in the source if they
   * are not already cached.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private DartEntry cacheDartDependencyData(Source source, DartEntry dartEntry,
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
      dartEntry = (DartEntry) new ResolveDartDependenciesTask(this, source).perform(resultRecorder);
      state = dartEntry.getState(descriptor);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * the state of the data represented by the given descriptor is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by generating hints
   * for the library if the data is not already cached.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private DartEntry cacheDartHintData(Source unitSource, Source librarySource, DartEntry dartEntry,
      DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getStateInLibrary(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      dartEntry = (DartEntry) new GenerateDartHintsTask(this, getLibraryElement(librarySource)).perform(resultRecorder);
      state = dartEntry.getStateInLibrary(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file, return a cache entry in which the state of the data represented
   * by the given descriptor is either {@link CacheState#VALID} or {@link CacheState#ERROR}. This
   * method assumes that the data can be produced by parsing the source if it is not already cached.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
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
   * the state of the data represented by the given descriptor is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by resolving the
   * source in the context of the library if it is not already cached.
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
        : dartEntry.getStateInLibrary(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      // TODO(brianwilkerson) As an optimization, if we already have the element model for the
      // library we can use ResolveDartUnitTask to produce the resolved AST structure much faster.
      dartEntry = (DartEntry) new ResolveDartLibraryTask(this, unitSource, librarySource).perform(resultRecorder);
      state = (descriptor == DartEntry.ELEMENT) ? dartEntry.getState(descriptor)
          : dartEntry.getStateInLibrary(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file, return a cache entry in which the state of the data represented
   * by the given descriptor is either {@link CacheState#VALID} or {@link CacheState#ERROR}. This
   * method assumes that the data can be produced by scanning the source if it is not already
   * cached.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be scanned
   */
  private DartEntry cacheDartScanData(Source source, DartEntry dartEntry,
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
      // TODO(brianwilkerson) Convert this to get the contents from the cache. (I'm not sure how
      // that would work in an asynchronous environment.)
      try {
        dartEntry = (DartEntry) new ScanDartTask(this, source, getContents(source)).perform(resultRecorder);
      } catch (AnalysisException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new AnalysisException(exception);
      }
      state = dartEntry.getState(descriptor);
    }
    return dartEntry;
  }

  /**
   * Given a source for a Dart file and the library that contains it, return a cache entry in which
   * the state of the data represented by the given descriptor is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by verifying the
   * source in the given library if the data is not already cached.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return a cache entry containing the required data
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private DartEntry cacheDartVerificationData(Source unitSource, Source librarySource,
      DartEntry dartEntry, DataDescriptor<?> descriptor) throws AnalysisException {
    //
    // Check to see whether we already have the information being requested.
    //
    CacheState state = dartEntry.getStateInLibrary(descriptor, librarySource);
    while (state != CacheState.ERROR && state != CacheState.VALID) {
      //
      // If not, compute the information. Unless the modification date of the source continues to
      // change, this loop will eventually terminate.
      //
      dartEntry = (DartEntry) new GenerateDartErrorsTask(
          this,
          unitSource,
          getLibraryElement(librarySource)).perform(resultRecorder);
      state = dartEntry.getStateInLibrary(descriptor, librarySource);
    }
    return dartEntry;
  }

  /**
   * Given a source for an HTML file, return a cache entry in which all of the data represented by
   * the state of the given descriptors is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by parsing the
   * source if it is not already cached.
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
    if (descriptor == HtmlEntry.PARSED_UNIT) {
      HtmlUnit unit = htmlEntry.getAnyParsedUnit();
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
      // TODO(brianwilkerson) Convert this to get the contents from the cache. (I'm not sure how
      // that would work in an asynchronous environment.)
      try {
        htmlEntry = (HtmlEntry) new ParseHtmlTask(this, source, getContents(source)).perform(resultRecorder);
      } catch (AnalysisException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new AnalysisException(exception);
      }
      state = htmlEntry.getState(descriptor);
    }
    return htmlEntry;
  }

  /**
   * Given a source for an HTML file, return a cache entry in which the state of the data
   * represented by the given descriptor is either {@link CacheState#VALID} or
   * {@link CacheState#ERROR}. This method assumes that the data can be produced by resolving the
   * source if it is not already cached.
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
      htmlEntry.setModificationTime(getModificationStamp(source));
      cache.put(source, htmlEntry);
      return htmlEntry;
    } else {
      DartEntryImpl dartEntry = new DartEntryImpl();
      dartEntry.setModificationTime(getModificationStamp(source));
      cache.put(source, dartEntry);
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
  private String debuggingString(Source source) {
    return "'" + source.getFullName() + "' [" + getModificationStamp(source) + "]";
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
   * associated with that source. This method assumes that the data can be produced by resolving the
   * directives in the source if they are not already cached.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private <E> E getDartDependencyData(Source source, DartEntry dartEntry,
      DataDescriptor<E> descriptor) throws AnalysisException {
    dartEntry = cacheDartDependencyData(source, dartEntry, descriptor);
    return dartEntry.getValue(descriptor);
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not a Dart file. This
   * method assumes that the data can be produced by resolving the directives in the source if they
   * are not already cached.
   * 
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be parsed
   */
  private <E> E getDartDependencyData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return defaultValue;
    }
    try {
      return getDartDependencyData(source, dartEntry, descriptor);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + descriptor.toString(),
          exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by generating hints for the library if it is not already cached.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the entry representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getDartHintData(Source unitSource, Source librarySource, DartEntry dartEntry,
      DataDescriptor<E> descriptor) throws AnalysisException {
    dartEntry = cacheDartHintData(unitSource, librarySource, dartEntry, descriptor);
    if (descriptor == DartEntry.ELEMENT) {
      return dartEntry.getValue(descriptor);
    }
    return dartEntry.getValueInLibrary(descriptor, librarySource);
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
      accessedAst(source);
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
    try {
      return getDartParseData(source, dartEntry, descriptor);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + descriptor.toString(),
          exception);
      return defaultValue;
    }
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
    } else if (descriptor == DartEntry.RESOLVED_UNIT) {
      accessedAst(unitSource);
    }
    return dartEntry.getValueInLibrary(descriptor, librarySource);
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
    try {
      return getDartResolutionData(unitSource, librarySource, dartEntry, descriptor);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + descriptor.toString(),
          exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source. This method assumes that the data can be produced by scanning the
   * source if it is not already cached.
   * 
   * @param source the source representing the Dart file
   * @param dartEntry the cache entry associated with the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be scanned
   */
  private <E> E getDartScanData(Source source, DartEntry dartEntry, DataDescriptor<E> descriptor)
      throws AnalysisException {
    dartEntry = cacheDartScanData(source, dartEntry, descriptor);
    return dartEntry.getValue(descriptor);
  }

  /**
   * Given a source for a Dart file, return the data represented by the given descriptor that is
   * associated with that source, or the given default value if the source is not a Dart file. This
   * method assumes that the data can be produced by scanning the source if it is not already
   * cached.
   * 
   * @param source the source representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @param defaultValue the value to be returned if the source is not a Dart file
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be scanned
   */
  private <E> E getDartScanData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    DartEntry dartEntry = getReadableDartEntry(source);
    if (dartEntry == null) {
      return defaultValue;
    }
    try {
      return getDartScanData(source, dartEntry, descriptor);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + descriptor.toString(),
          exception);
      return defaultValue;
    }
  }

  /**
   * Given a source for a Dart file and the library that contains it, return the data represented by
   * the given descriptor that is associated with that source. This method assumes that the data can
   * be produced by verifying the source within the given library if it is not already cached.
   * 
   * @param unitSource the source representing the Dart file
   * @param librarySource the source representing the library containing the Dart file
   * @param dartEntry the entry representing the Dart file
   * @param descriptor the descriptor representing the data to be returned
   * @return the requested data about the given source
   * @throws AnalysisException if data could not be returned because the source could not be
   *           resolved
   */
  private <E> E getDartVerificationData(Source unitSource, Source librarySource,
      DartEntry dartEntry, DataDescriptor<E> descriptor) throws AnalysisException {
    dartEntry = cacheDartVerificationData(unitSource, librarySource, dartEntry, descriptor);
    return dartEntry.getValueInLibrary(descriptor, librarySource);
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
  @SuppressWarnings("unchecked")
  private <E> E getHtmlParseData(Source source, DataDescriptor<E> descriptor, E defaultValue)
      throws AnalysisException {
    HtmlEntry htmlEntry = getReadableHtmlEntry(source);
    if (htmlEntry == null) {
      return defaultValue;
    }
    htmlEntry = cacheHtmlParseData(source, htmlEntry, descriptor);
    if (descriptor == HtmlEntry.PARSED_UNIT) {
      accessedAst(source);
      return (E) htmlEntry.getAnyParsedUnit();
    }
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
    try {
      return getHtmlResolutionData(source, htmlEntry, descriptor);
    } catch (ObsoleteSourceAnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Could not compute " + descriptor.toString(),
          exception);
      return defaultValue;
    }
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
    if (descriptor == HtmlEntry.RESOLVED_UNIT) {
      accessedAst(source);
    }
    return htmlEntry.getValue(descriptor);
  }

  /**
   * Look through the cache for a task that needs to be performed. Return the task that was found,
   * or {@code null} if there is no more work to be done.
   * 
   * @return the next task that needs to be performed
   */
  private AnalysisTask getNextAnalysisTask() {
    synchronized (cacheLock) {
      boolean hintsEnabled = options.getHint();
      boolean sdkErrorsEnabled = options.getGenerateSdkErrors();
      boolean hasBlockedTask = false;
      //
      // Look for incremental analysis
      //
      if (incrementalAnalysisCache != null && incrementalAnalysisCache.hasWork()) {
        AnalysisTask task = new IncrementalAnalysisTask(this, incrementalAnalysisCache);
        incrementalAnalysisCache = null;
        return task;
      }
      //
      // Look for a priority source that needs to be analyzed.
      //
      for (Source source : priorityOrder) {
        TaskData taskData = getNextNondependentAnalysisTask(
            source,
            true,
            hintsEnabled,
            sdkErrorsEnabled);
        AnalysisTask task = taskData.getTask();
        if (task != null) {
          return task;
        } else if (taskData.isBlocked()) {
          hasBlockedTask = true;
        }
      }
      //
      // Look for a non-priority source that needs to be analyzed.
      //
      Source source = workManager.getNextSource();
      while (source != null) {
        TaskData taskData = getNextNondependentAnalysisTask(
            source,
            false,
            hintsEnabled,
            sdkErrorsEnabled);
        AnalysisTask task = taskData.getTask();
        if (task != null) {
          return task;
        } else if (taskData.isBlocked()) {
          hasBlockedTask = true;
        } else {
          workManager.remove(source);
        }
        source = workManager.getNextSource();
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
        return WaitForAsyncTask.getInstance();
      }
      return null;
    }
  }

  /**
   * Look at the given source to see whether a task needs to be performed related to it. Return the
   * task that should be performed, or {@code null} if there is no more work to be done for the
   * source.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source to be checked
   * @param sourceEntry the cache entry associated with the source
   * @param isPriority {@code true} if the source is a priority source
   * @param hintsEnabled {@code true} if hints are currently enabled
   * @param sdkErrorsEnabled {@code true} if errors, warnings and hints should be generated for
   *          sources in the SDK
   * @return the next task that needs to be performed for the given source
   */
  private TaskData getNextAnalysisTaskForSource(Source source, SourceEntry sourceEntry,
      boolean isPriority, boolean hintsEnabled, boolean sdkErrorsEnabled) {
    if (sourceEntry == null) {
      return new TaskData(null, false, null);
    }
    CacheState contentState = sourceEntry.getState(SourceEntry.CONTENT);
    if (contentState == CacheState.INVALID) {
      SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
      sourceCopy.setState(SourceEntry.CONTENT, CacheState.IN_PROCESS);
      cache.put(source, sourceCopy);
      return new TaskData(new GetContentTask(this, source), false, null);
    } else if (contentState == CacheState.IN_PROCESS) {
      // We are in the process of getting the content. There's nothing else we can do with this
      // source until that's complete.
      return new TaskData(null, true, null);
    }
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      CacheState scanErrorsState = dartEntry.getState(DartEntry.SCAN_ERRORS);
      if (scanErrorsState == CacheState.INVALID
          || (isPriority && scanErrorsState == CacheState.FLUSHED)) {
        // TODO(brianwilkerson) Convert this to get the contents from the cache or to asynchronously
        // request the contents if they are not in the cache.
        try {
          DartEntryImpl dartCopy = dartEntry.getWritableCopy();
          dartCopy.setState(DartEntry.SCAN_ERRORS, CacheState.IN_PROCESS);
          TimestampedData<CharSequence> contentData;
          if (contentState == CacheState.VALID) {
            contentData = new TimestampedData<CharSequence>(
                dartCopy.getModificationTime(),
                dartCopy.getValue(SourceEntry.CONTENT));
            dartCopy.setState(SourceEntry.CONTENT, CacheState.FLUSHED);
          } else {
            contentData = getContents(source);
          }
          cache.put(source, dartCopy);
          return new TaskData(new ScanDartTask(this, source, contentData), false, null);
        } catch (Exception exception) {
          DartEntryImpl dartCopy = dartEntry.getWritableCopy();
          dartCopy.recordScanError();
          dartCopy.setException(new AnalysisException(exception));
          cache.put(source, dartCopy);
        }
      }
      CacheState parseErrorsState = dartEntry.getState(DartEntry.PARSE_ERRORS);
      if (parseErrorsState == CacheState.INVALID
          || (isPriority && parseErrorsState == CacheState.FLUSHED)) {
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        dartCopy.setState(DartEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
        cache.put(source, dartCopy);
        return new TaskData(new ParseDartTask(this, source), false, null);
      }
      if (isPriority && parseErrorsState != CacheState.ERROR) {
        CompilationUnit parseUnit = dartEntry.getAnyParsedCompilationUnit();
        if (parseUnit == null) {
          DartEntryImpl dartCopy = dartEntry.getWritableCopy();
          dartCopy.setState(DartEntry.PARSED_UNIT, CacheState.IN_PROCESS);
          cache.put(source, dartCopy);
          return new TaskData(new ParseDartTask(this, source), false, null);
        }
      }
      CacheState exportState = dartEntry.getState(DartEntry.EXPORTED_LIBRARIES);
      if (exportState == CacheState.INVALID || (isPriority && exportState == CacheState.FLUSHED)) {
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        dartCopy.setState(DartEntry.EXPORTED_LIBRARIES, CacheState.IN_PROCESS);
        cache.put(source, dartCopy);
        return new TaskData(new ResolveDartDependenciesTask(this, source), false, null);
      }
      Source[] librariesContaining = dartEntry.getValue(DartEntry.CONTAINING_LIBRARIES);
      for (Source librarySource : librariesContaining) {
        SourceEntry libraryEntry = cache.get(librarySource);
        if (libraryEntry instanceof DartEntry) {
          CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
          if (elementState == CacheState.INVALID
              || (isPriority && elementState == CacheState.FLUSHED)) {
            DartEntryImpl libraryCopy = ((DartEntry) libraryEntry).getWritableCopy();
            libraryCopy.setState(DartEntry.ELEMENT, CacheState.IN_PROCESS);
            cache.put(librarySource, libraryCopy);
            return new TaskData(
                new ResolveDartLibraryTask(this, source, librarySource),
                false,
                null);
          }
          CacheState resolvedUnitState = dartEntry.getStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
          if (resolvedUnitState == CacheState.INVALID
              || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
            //
            // The commented out lines below are an optimization that doesn't quite work yet. The
            // problem is that if the source was not resolved because it wasn't part of any library,
            // then there won't be any elements in the element model that we can use to resolve it.
            //
            //LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
            //if (libraryElement != null) {
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            dartCopy.setStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource, CacheState.IN_PROCESS);
            cache.put(source, dartCopy);
            //return new ResolveDartUnitTask(this, source, libraryElement);
            return new TaskData(
                new ResolveDartLibraryTask(this, source, librarySource),
                false,
                null);
            //}
          }
          if (sdkErrorsEnabled || !source.isInSystemLibrary()) {
            CacheState verificationErrorsState = dartEntry.getStateInLibrary(
                DartEntry.VERIFICATION_ERRORS,
                librarySource);
            if (verificationErrorsState == CacheState.INVALID
                || (isPriority && verificationErrorsState == CacheState.FLUSHED)) {
              LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
              if (libraryElement != null) {
                DartEntryImpl dartCopy = dartEntry.getWritableCopy();
                dartCopy.setStateInLibrary(
                    DartEntry.VERIFICATION_ERRORS,
                    librarySource,
                    CacheState.IN_PROCESS);
                cache.put(source, dartCopy);
                return new TaskData(
                    new GenerateDartErrorsTask(this, source, libraryElement),
                    false,
                    null);
              }
            }
            if (hintsEnabled) {
              CacheState hintsState = dartEntry.getStateInLibrary(DartEntry.HINTS, librarySource);
              if (hintsState == CacheState.INVALID
                  || (isPriority && hintsState == CacheState.FLUSHED)) {
                LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
                if (libraryElement != null) {
                  DartEntryImpl dartCopy = dartEntry.getWritableCopy();
                  dartCopy.setStateInLibrary(DartEntry.HINTS, librarySource, CacheState.IN_PROCESS);
                  cache.put(source, dartCopy);
                  return new TaskData(new GenerateDartHintsTask(this, libraryElement), false, null);
                }
              }
            }
          }
        }
      }
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      CacheState parseErrorsState = htmlEntry.getState(HtmlEntry.PARSE_ERRORS);
      if (parseErrorsState == CacheState.INVALID
          || (isPriority && parseErrorsState == CacheState.FLUSHED)) {
        try {
          HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
          htmlCopy.setState(HtmlEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
          TimestampedData<CharSequence> contentData;
          if (contentState == CacheState.VALID) {
            contentData = new TimestampedData<CharSequence>(
                htmlCopy.getModificationTime(),
                htmlCopy.getValue(SourceEntry.CONTENT));
            htmlCopy.setState(SourceEntry.CONTENT, CacheState.FLUSHED);
          } else {
            contentData = getContents(source);
          }
          cache.put(source, htmlCopy);
          return new TaskData(new ParseHtmlTask(this, source, contentData), false, null);
        } catch (Exception exception) {
          HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
          htmlCopy.recordParseError();
          htmlCopy.setException(new AnalysisException(exception));
          cache.put(source, htmlCopy);
        }
      }
      if (isPriority && parseErrorsState != CacheState.ERROR) {
        HtmlUnit parsedUnit = htmlEntry.getAnyParsedUnit();
        if (parsedUnit == null) {
          try {
            HtmlEntryImpl dartCopy = htmlEntry.getWritableCopy();
            dartCopy.setState(HtmlEntry.PARSE_ERRORS, CacheState.IN_PROCESS);
            TimestampedData<CharSequence> contentData;
            if (contentState == CacheState.VALID) {
              contentData = new TimestampedData<CharSequence>(
                  dartCopy.getModificationTime(),
                  dartCopy.getValue(SourceEntry.CONTENT));
              dartCopy.setState(SourceEntry.CONTENT, CacheState.FLUSHED);
            } else {
              contentData = getContents(source);
            }
            cache.put(source, dartCopy);
            return new TaskData(new ParseHtmlTask(this, source, contentData), false, null);
          } catch (Exception exception) {
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.recordParseError();
            htmlCopy.setException(new AnalysisException(exception));
            cache.put(source, htmlCopy);
          }
        }
      }
      CacheState resolvedUnitState = htmlEntry.getState(HtmlEntry.RESOLVED_UNIT);
      if (resolvedUnitState == CacheState.INVALID
          || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        htmlCopy.setState(HtmlEntry.RESOLVED_UNIT, CacheState.IN_PROCESS);
        cache.put(source, htmlCopy);
        return new TaskData(new ResolveHtmlTask(this, source), false, null);
      }
      // Angular support
      if (options.getAnalyzeAngular()) {
        // try to resolve as an Angular entry point
        CacheState angularEntryState = htmlEntry.getState(HtmlEntry.ANGULAR_ENTRY);
        if (angularEntryState == CacheState.INVALID) {
          HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
          htmlCopy.setState(HtmlEntry.ANGULAR_ENTRY, CacheState.IN_PROCESS);
          cache.put(source, htmlCopy);
          return new TaskData(new ResolveAngularEntryHtmlTask(this, source), false, null);
        }
        // try to resolve as an Angular application part
        CacheState angularErrorsState = htmlEntry.getState(HtmlEntry.ANGULAR_ERRORS);
        if (angularErrorsState == CacheState.INVALID) {
          AngularApplication application = htmlEntry.getValue(HtmlEntry.ANGULAR_APPLICATION);
          // try to resolve as an Angular template
          AngularComponentElement component = htmlEntry.getValue(HtmlEntry.ANGULAR_COMPONENT);
          HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
          htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.IN_PROCESS);
          cache.put(source, htmlCopy);
          return new TaskData(new ResolveAngularComponentTemplateTask(
              this,
              source,
              component,
              application), false, null);
        }
      }
    }
    return new TaskData(null, false, null);
  }

  private TaskData getNextNondependentAnalysisTask(Source source, boolean isPriority,
      boolean hintsEnabled, boolean sdkErrorsEnabled) {
    TaskData taskData = getNextAnalysisTaskForSource(
        source,
        cache.get(source),
        isPriority,
        hintsEnabled,
        sdkErrorsEnabled);
    if (taskData.getTask() != null || taskData.isBlocked()) {
      return taskData;
    }
    while (taskData.getDependentSource() != null) {
      taskData = getNextAnalysisTaskForSource(
          source,
          cache.get(source),
          isPriority,
          hintsEnabled,
          sdkErrorsEnabled);
      if (taskData.getTask() != null || taskData.isBlocked()) {
        return taskData;
      }
    }
    return new TaskData(null, false, null);
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
   * Look at the given source to see whether a task needs to be performed related to it. If so, add
   * the source to the set of sources that need to be processed. This method duplicates, and must
   * therefore be kept in sync with,
   * {@link #getNextAnalysisTask(Source, SourceEntry, boolean, boolean)}. This method is intended to
   * be used for testing purposes only.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source to be checked
   * @param sourceEntry the cache entry associated with the source
   * @param isPriority {@code true} if the source is a priority source
   * @param hintsEnabled {@code true} if hints are currently enabled
   * @param sources the set to which sources should be added
   */
  private void getSourcesNeedingProcessing(Source source, SourceEntry sourceEntry,
      boolean isPriority, boolean hintsEnabled, HashSet<Source> sources) {
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      CacheState scanErrorsState = dartEntry.getState(DartEntry.SCAN_ERRORS);
      if (scanErrorsState == CacheState.INVALID
          || (isPriority && scanErrorsState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      CacheState parseErrorsState = dartEntry.getState(DartEntry.PARSE_ERRORS);
      if (parseErrorsState == CacheState.INVALID
          || (isPriority && parseErrorsState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      if (isPriority) {
        CompilationUnit parseUnit = dartEntry.getAnyParsedCompilationUnit();
        if (parseUnit == null) {
          sources.add(source);
          return;
        }
      }
      for (Source librarySource : getLibrariesContaining(source)) {
        SourceEntry libraryEntry = cache.get(librarySource);
        if (libraryEntry instanceof DartEntry) {
          CacheState elementState = libraryEntry.getState(DartEntry.ELEMENT);
          if (elementState == CacheState.INVALID
              || (isPriority && elementState == CacheState.FLUSHED)) {
            sources.add(source);
            return;
          }
          CacheState resolvedUnitState = dartEntry.getStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource);
          if (resolvedUnitState == CacheState.INVALID
              || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
            LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
            if (libraryElement != null) {
              sources.add(source);
              return;
            }
          }
          CacheState verificationErrorsState = dartEntry.getStateInLibrary(
              DartEntry.VERIFICATION_ERRORS,
              librarySource);
          if (verificationErrorsState == CacheState.INVALID
              || (isPriority && verificationErrorsState == CacheState.FLUSHED)) {
            LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
            if (libraryElement != null) {
              sources.add(source);
              return;
            }
          }
          if (hintsEnabled) {
            CacheState hintsState = dartEntry.getStateInLibrary(DartEntry.HINTS, librarySource);
            if (hintsState == CacheState.INVALID
                || (isPriority && hintsState == CacheState.FLUSHED)) {
              LibraryElement libraryElement = libraryEntry.getValue(DartEntry.ELEMENT);
              if (libraryElement != null) {
                sources.add(source);
                return;
              }
            }
          }
        }
      }
    } else if (sourceEntry instanceof HtmlEntry) {
      HtmlEntry htmlEntry = (HtmlEntry) sourceEntry;
      CacheState parsedUnitState = htmlEntry.getState(HtmlEntry.PARSED_UNIT);
      if (parsedUnitState == CacheState.INVALID
          || (isPriority && parsedUnitState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      CacheState resolvedUnitState = htmlEntry.getState(HtmlEntry.RESOLVED_UNIT);
      if (resolvedUnitState == CacheState.INVALID
          || (isPriority && resolvedUnitState == CacheState.FLUSHED)) {
        sources.add(source);
        return;
      }
      // Angular
      if (options.getAnalyzeAngular()) {
        CacheState angularErrorsState = htmlEntry.getState(HtmlEntry.ANGULAR_ERRORS);
        if (angularErrorsState == CacheState.INVALID) {
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
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   */
  private void invalidateAllResolutionInformation() {
    for (Map.Entry<Source, SourceEntry> mapEntry : cache.entrySet()) {
      Source source = mapEntry.getKey();
      SourceEntry sourceEntry = mapEntry.getValue();
      if (sourceEntry instanceof HtmlEntry) {
        HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
        htmlCopy.invalidateAllResolutionInformation();
        mapEntry.setValue(htmlCopy);
      } else if (sourceEntry instanceof DartEntry) {
        DartEntry dartEntry = (DartEntry) sourceEntry;
        removeFromParts(source, dartEntry);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        dartCopy.invalidateAllResolutionInformation();
        mapEntry.setValue(dartCopy);
        workManager.add(source, SourcePriority.UNKNOWN);
      }
    }
  }

  /**
   * In response to a change to Angular entry point {@link HtmlElement}, invalidate any results that
   * depend on it.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * <p>
   * <b>Note:</b> Any cache entries that were accessed before this method was invoked must be
   * re-accessed after this method returns.
   * 
   * @param entryCopy the {@link HtmlEntryImpl} of the (maybe) Angular entry point being invalidated
   */
  private void invalidateAngularResolution(HtmlEntryImpl entryCopy) {
    AngularApplication application = entryCopy.getValue(HtmlEntry.ANGULAR_ENTRY);
    if (application == null) {
      return;
    }
    angularApplications.remove(application);
    // invalidate Entry
    entryCopy.setState(HtmlEntry.ANGULAR_ENTRY, CacheState.INVALID);
    // reset HTML sources
    AngularElement[] oldAngularElements = application.getElements();
    for (AngularElement angularElement : oldAngularElements) {
      if (angularElement instanceof AngularHasTemplateElement) {
        AngularHasTemplateElement hasTemplate = (AngularHasTemplateElement) angularElement;
        Source templateSource = hasTemplate.getTemplateSource();
        if (templateSource != null) {
          HtmlEntry htmlEntry = getReadableHtmlEntry(templateSource);
          HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
          htmlCopy.setValue(HtmlEntry.ANGULAR_APPLICATION, null);
          htmlCopy.setValue(HtmlEntry.ANGULAR_COMPONENT, null);
          htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
          cache.put(templateSource, htmlCopy);
          workManager.add(templateSource, SourcePriority.HTML);
        }
      }
    }
    // reset Dart sources
    Source[] oldElementSources = application.getElementSources();
    for (Source elementSource : oldElementSources) {
      DartEntry dartEntry = getReadableDartEntry(elementSource);
      DartEntryImpl dartCopy = dartEntry.getWritableCopy();
      dartCopy.setValue(DartEntry.ANGULAR_ERRORS, AnalysisError.NO_ERRORS);
      cache.put(elementSource, dartCopy);
      // notify about (disappeared) Angular errors
      ChangeNoticeImpl notice = getNotice(elementSource);
      notice.setErrors(dartCopy.getAllErrors(), dartEntry.getValue(SourceEntry.LINE_INFO));
    }
  }

  /**
   * In response to a change to at least one of the compilation units in the given library,
   * invalidate any results that are dependent on the result of resolving that library.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * <p>
   * <b>Note:</b> Any cache entries that were accessed before this method was invoked must be
   * re-accessed after this method returns.
   * 
   * @param librarySource the source of the library being invalidated
   * @param writer the writer to which debugging information should be written
   */
  private void invalidateLibraryResolution(Source librarySource, PrintStringWriter writer) {
    // TODO(brianwilkerson) This could be optimized. There's no need to flush all of these caches if
    // the public namespace hasn't changed, which will be a fairly common case. The question is
    // whether we can afford the time to compute the namespace to look for differences.
    DartEntry libraryEntry = getReadableDartEntry(librarySource);
    if (libraryEntry != null) {
      Source[] includedParts = libraryEntry.getValue(DartEntry.INCLUDED_PARTS);
      DartEntryImpl libraryCopy = libraryEntry.getWritableCopy();
      long oldTime = libraryCopy.getModificationTime();
      libraryCopy.invalidateAllResolutionInformation();
      cache.put(librarySource, libraryCopy);
      workManager.add(librarySource, SourcePriority.LIBRARY);
      if (writer != null) {
        writer.println("  Invalidated library source: " + debuggingString(librarySource)
            + " (previously modified at " + oldTime + ")");
      }
      for (Source partSource : includedParts) {
        SourceEntry partEntry = cache.get(partSource);
        if (partEntry instanceof DartEntry) {
          DartEntryImpl partCopy = ((DartEntry) partEntry).getWritableCopy();
          oldTime = partCopy.getModificationTime();
          if (partEntry != libraryCopy) {
            partCopy.removeContainingLibrary(librarySource);
            workManager.add(librarySource, SourcePriority.NORMAL_PART);
          }
          partCopy.invalidateAllResolutionInformation();
          cache.put(partSource, partCopy);
          if (writer != null) {
            writer.println("  Invalidated part source: " + debuggingString(partSource)
                + " (previously modified at " + oldTime + ")");
          }
        }
      }
    }
    // invalidate Angular applications
    List<AngularApplication> angularApplicationsCopy = Lists.newArrayList(angularApplications);
    for (AngularApplication application : angularApplicationsCopy) {
      if (application.dependsOn(librarySource)) {
        Source entryPointSource = application.getEntryPoint();
        HtmlEntry entry = getReadableHtmlEntry(entryPointSource);
        HtmlEntryImpl entryCopy = entry.getWritableCopy();
        invalidateAngularResolution(entryCopy);
        cache.put(entryPointSource, entryCopy);
        workManager.add(entryPointSource, SourcePriority.HTML);
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
   * Log the given debugging information.
   * 
   * @param message the message to be added to the log
   */
  private void logInformation(String message) {
    AnalysisEngine.getInstance().getLogger().logInformation(message);
  }

  /**
   * Log the given debugging information.
   * 
   * @param message the message to be added to the log
   * @param exception the exception to be included in the log entry
   */
  private void logInformation(String message, Throwable exception) {
    if (exception == null) {
      AnalysisEngine.getInstance().getLogger().logInformation(message);
    } else {
      AnalysisEngine.getInstance().getLogger().logInformation(message, exception);
    }
  }

  /**
   * Updates {@link HtmlEntry}s that correspond to the previously known and new Angular application
   * information.
   */
  private void recordAngularEntryPoint(HtmlEntryImpl entry, ResolveAngularEntryHtmlTask task)
      throws AnalysisException {
    AngularApplication application = task.getApplication();
    if (application != null) {
      angularApplications.add(application);
      // if this is an entry point, then we already resolved it
      entry.setValue(HtmlEntry.ANGULAR_ERRORS, task.getEntryErrors());
      // schedule HTML templates analysis
      AngularElement[] newAngularElements = application.getElements();
      for (AngularElement angularElement : newAngularElements) {
        if (angularElement instanceof AngularHasTemplateElement) {
          AngularHasTemplateElement hasTemplate = (AngularHasTemplateElement) angularElement;
          Source templateSource = hasTemplate.getTemplateSource();
          if (templateSource != null) {
            HtmlEntry htmlEntry = getReadableHtmlEntry(templateSource);
            HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
            htmlCopy.setValue(HtmlEntry.ANGULAR_APPLICATION, application);
            if (hasTemplate instanceof AngularComponentElement) {
              AngularComponentElement component = (AngularComponentElement) hasTemplate;
              htmlCopy.setValue(HtmlEntry.ANGULAR_COMPONENT, component);
            }
            htmlCopy.setState(HtmlEntry.ANGULAR_ERRORS, CacheState.INVALID);
            cache.put(templateSource, htmlCopy);
            workManager.add(templateSource, SourcePriority.HTML);
          }
        }
      }
      // update Dart sources errors
      Source[] newElementSources = application.getElementSources();
      for (Source elementSource : newElementSources) {
        DartEntry dartEntry = getReadableDartEntry(elementSource);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        dartCopy.setValue(DartEntry.ANGULAR_ERRORS, task.getErrors(elementSource));
        cache.put(elementSource, dartCopy);
        // notify about Dart errors
        ChangeNoticeImpl notice = getNotice(elementSource);
        notice.setErrors(dartCopy.getAllErrors(), computeLineInfo(elementSource));
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
  private void recordElementData(DartEntry dartEntry, DartEntryImpl dartCopy,
      LibraryElement library, Source librarySource, Source htmlSource) {
    dartCopy.setValue(DartEntry.ELEMENT, library);
    dartCopy.setValue(DartEntry.IS_LAUNCHABLE, library.getEntryPoint() != null);
    dartCopy.setValue(
        DartEntry.IS_CLIENT,
        isClient(library, htmlSource, new HashSet<LibraryElement>()));
    // TODO(brianwilkerson) Understand why we're doing this both here and in
    // ResolveDartDependenciesTask and whether we should also be capturing the imported and exported
    // sources here.
    removeFromParts(librarySource, dartEntry);
    CompilationUnitElement[] parts = library.getParts();
    int count = parts.length;
    Source[] unitSources = new Source[count + 1];
    unitSources[0] = library.getDefiningCompilationUnit().getSource();
    for (int i = 0; i < count; i++) {
      Source unitSource = parts[i].getSource();
      unitSources[i + 1] = unitSource;
      DartEntry unitEntry = getReadableDartEntry(unitSource);
      if (unitSource != null) {
        DartEntryImpl unitCopy = unitEntry.getWritableCopy();
        unitCopy.addContainingLibrary(librarySource);
        cache.put(unitSource, unitCopy);
      }
    }
    dartCopy.setValue(DartEntry.INCLUDED_PARTS, unitSources);
  }

  /**
   * Record the results produced by performing a {@link GenerateDartErrorsTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordGenerateDartErrorsTask(GenerateDartErrorsTask task)
      throws AnalysisException {
    Source source = task.getSource();
    Source librarySource = task.getLibraryElement().getSource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to verify non-Dart file as a Dart file: "
                + source.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
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
          dartCopy.setValueInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource, task.getErrors());
          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(dartCopy.getAllErrors(), dartCopy.getValue(SourceEntry.LINE_INFO));
        } else {
          dartCopy.setStateInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource, CacheState.ERROR);
        }
        dartCopy.setException(thrownException);
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      } else {
        logInformation("Generated errors discarded for " + debuggingString(source)
            + "; sourceTime = " + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
            + dartEntry.getModificationTime(), thrownException);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the source
          // will be re-verified using the up-to-date sources.
          //
//          dartCopy.setState(DartEntry.VERIFICATION_ERRORS, librarySource, CacheState.INVALID);
          removeFromParts(source, dartEntry);
          dartCopy.invalidateAllInformation();
          dartCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
          workManager.add(source, SourcePriority.UNKNOWN);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-verify the source until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.setStateInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource, CacheState.ERROR);
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
    HashMap<Source, TimestampedData<AnalysisError[]>> hintMap = task.getHintMap();
    if (hintMap == null) {
      synchronized (cacheLock) {
        // We don't have any information about which sources to mark as invalid other than the library
        // source.
        SourceEntry sourceEntry = cache.get(librarySource);
        if (sourceEntry == null) {
          throw new ObsoleteSourceAnalysisException(librarySource);
        } else if (!(sourceEntry instanceof DartEntry)) {
          // This shouldn't be possible because we should never have performed the task if the source
          // didn't represent a Dart file, but check to be safe.
          throw new AnalysisException(
              "Internal error: attempting to generate hints for non-Dart file as a Dart file: "
                  + librarySource.getFullName());
        }
        if (thrownException == null) {
          thrownException = new AnalysisException(
              "GenerateDartHintsTask returned a null hint map without throwing an exception: "
                  + librarySource.getFullName());
        }
        DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
        dartCopy.setStateInLibrary(DartEntry.HINTS, librarySource, CacheState.ERROR);
        dartCopy.setException(thrownException);
        cache.put(librarySource, dartCopy);
      }
      throw thrownException;
    }
    for (Map.Entry<Source, TimestampedData<AnalysisError[]>> entry : hintMap.entrySet()) {
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
        long sourceTime = getModificationStamp(unitSource);
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
            dartCopy.setValueInLibrary(DartEntry.HINTS, librarySource, results.getData());
            ChangeNoticeImpl notice = getNotice(unitSource);
            notice.setErrors(dartCopy.getAllErrors(), dartCopy.getValue(SourceEntry.LINE_INFO));
          } else {
            dartCopy.setStateInLibrary(DartEntry.HINTS, librarySource, CacheState.ERROR);
          }
          dartCopy.setException(thrownException);
          cache.put(unitSource, dartCopy);
          dartEntry = dartCopy;
        } else {
          logInformation("Generated hints discarded for " + debuggingString(unitSource)
              + "; sourceTime = " + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
              + dartEntry.getModificationTime(), thrownException);
          if (dartEntry.getStateInLibrary(DartEntry.HINTS, librarySource) == CacheState.IN_PROCESS) {
            DartEntryImpl dartCopy = dartEntry.getWritableCopy();
            if (thrownException == null || resultTime >= 0L) {
              //
              // The analysis was performed on out-of-date sources. Mark the cache so that the sources
              // will be re-analyzed using the up-to-date sources.
              //
//              dartCopy.setState(DartEntry.HINTS, librarySource, CacheState.INVALID);
              removeFromParts(unitSource, dartEntry);
              dartCopy.invalidateAllInformation();
              dartCopy.setModificationTime(sourceTime);
              cache.removedAst(unitSource);
              workManager.add(unitSource, SourcePriority.UNKNOWN);
            } else {
              //
              // We could not determine whether the sources were up-to-date or out-of-date. Mark the
              // cache so that we won't attempt to re-analyze the sources until there's a good chance
              // that we'll be able to do so without error.
              //
              dartCopy.setStateInLibrary(DartEntry.HINTS, librarySource, CacheState.ERROR);
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
   * Record the results produced by performing a {@link GetContentTask}.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private SourceEntry recordGetContentsTask(GetContentTask task) throws AnalysisException {
    if (!task.isComplete()) {
      return null;
    }
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    SourceEntry sourceEntry = null;
    synchronized (cacheLock) {
      sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      }
      SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
      if (thrownException == null) {
        sourceCopy.setModificationTime(task.getModificationTime());
        sourceCopy.setValue(SourceEntry.CONTENT, task.getContent());
      } else {
        sourceCopy.setException(thrownException);
        sourceCopy.recordContentError();
        workManager.remove(source);
      }
      cache.put(source, sourceCopy);
      sourceEntry = sourceCopy;
    }
    if (thrownException != null) {
      throw thrownException;
    }
    return sourceEntry;
  }

  /**
   * Record the results produced by performing a {@link IncrementalAnalysisTask}.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordIncrementalAnalysisTaskResults(IncrementalAnalysisTask task)
      throws AnalysisException {
    synchronized (cacheLock) {
      CompilationUnit unit = task.getCompilationUnit();
      if (unit != null) {
        ChangeNoticeImpl notice = getNotice(task.getSource());
        notice.setCompilationUnit(unit);
        incrementalAnalysisCache = IncrementalAnalysisCache.cacheResult(task.getCache(), unit);
      }
    }
    return null;
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
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to parse non-Dart file as a Dart file: "
                + source.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
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
          if (task.hasPartOfDirective() && !task.hasLibraryDirective()) {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.PART);
            dartCopy.removeContainingLibrary(source);
            workManager.add(source, SourcePriority.NORMAL_PART);
          } else {
            dartCopy.setValue(DartEntry.SOURCE_KIND, SourceKind.LIBRARY);
            dartCopy.setContainingLibrary(source);
            workManager.add(source, SourcePriority.LIBRARY);
          }
          dartCopy.setValue(DartEntry.PARSED_UNIT, task.getCompilationUnit());
          dartCopy.setValue(DartEntry.PARSE_ERRORS, task.getErrors());
          cache.storedAst(source);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(dartCopy.getAllErrors(), dartCopy.getValue(SourceEntry.LINE_INFO));

          // Verify that the incrementally parsed and resolved unit in the incremental cache
          // is structurally equivalent to the fully parsed unit
          incrementalAnalysisCache = IncrementalAnalysisCache.verifyStructure(
              incrementalAnalysisCache,
              source,
              task.getCompilationUnit());
        } else {
          removeFromParts(source, dartEntry);
          dartCopy.recordParseError();
          cache.removedAst(source);
        }
        dartCopy.setException(thrownException);
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      } else {
        logInformation(
            "Parse results discarded for " + debuggingString(source) + "; sourceTime = "
                + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
                + dartEntry.getModificationTime(),
            thrownException);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          dartCopy.recordParseNotInProcess();
          removeFromParts(source, dartEntry);
          dartCopy.invalidateAllInformation();
          dartCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
          workManager.add(source, SourcePriority.UNKNOWN);
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
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to parse non-HTML file as a HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
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
          htmlCopy.setValue(HtmlEntry.PARSE_ERRORS, task.getErrors());
          htmlCopy.setValue(HtmlEntry.REFERENCED_LIBRARIES, task.getReferencedLibraries());
          cache.storedAst(source);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(htmlCopy.getAllErrors(), lineInfo);
        } else {
          htmlCopy.recordParseError();
          cache.removedAst(source);
        }
        htmlCopy.setException(thrownException);
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        logInformation(
            "Parse results discarded for " + debuggingString(source) + "; sourceTime = "
                + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
                + htmlEntry.getModificationTime(),
            thrownException);
        HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
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
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
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
   * Record the results produced by performing a {@link ResolveAngularComponentTemplateTask}. If the
   * results were computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @throws AnalysisException if the results could not be recorded
   */
  private HtmlEntry recordResolveAngularComponentTemplateTaskResults(
      ResolveAngularComponentTemplateTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-HTML file as an HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
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
          htmlCopy.setValue(HtmlEntry.ANGULAR_ERRORS, task.getResolutionErrors());
          // notify about errors
          ChangeNoticeImpl notice = getNotice(source);
          notice.setHtmlUnit(task.getResolvedUnit());
          notice.setErrors(htmlCopy.getAllErrors(), htmlCopy.getValue(SourceEntry.LINE_INFO));
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
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
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
   * Record the results produced by performing a {@link ResolveAngularEntryHtmlTask}. If the results
   * were computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @throws AnalysisException if the results could not be recorded
   */
  private HtmlEntry recordResolveAngularEntryHtmlTaskResults(ResolveAngularEntryHtmlTask task)
      throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-HTML file as an HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
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
          htmlCopy.setValue(HtmlEntry.RESOLVED_UNIT, task.getResolvedUnit());
          recordAngularEntryPoint(htmlCopy, task);
          cache.storedAst(source);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setHtmlUnit(task.getResolvedUnit());
          notice.setErrors(htmlCopy.getAllErrors(), htmlCopy.getValue(SourceEntry.LINE_INFO));
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
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
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
   * Record the results produced by performing a {@link ResolveDartDependenciesTask}. If the results
   * were computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordResolveDartDependenciesTaskResults(ResolveDartDependenciesTask task)
      throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve Dart dependencies in a non-Dart file: "
                + source.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
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
        removeFromParts(source, dartEntry);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null) {
          Source[] newParts = task.getIncludedSources();
          for (int i = 0; i < newParts.length; i++) {
            Source partSource = newParts[i];
            DartEntry partEntry = getReadableDartEntry(partSource);
            if (partEntry != null && partEntry != dartEntry) {
              DartEntryImpl partCopy = partEntry.getWritableCopy();
              partCopy.addContainingLibrary(source);
              cache.put(partSource, partCopy);
            }
          }
          dartCopy.setValue(DartEntry.EXPORTED_LIBRARIES, task.getExportedSources());
          dartCopy.setValue(DartEntry.IMPORTED_LIBRARIES, task.getImportedSources());
          dartCopy.setValue(DartEntry.INCLUDED_PARTS, newParts);
        } else {
          dartCopy.recordDependencyError();
        }
        dartCopy.setException(thrownException);
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      } else {
        logInformation("Dependency resolution results discarded for " + debuggingString(source)
            + "; sourceTime = " + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
            + dartEntry.getModificationTime(), thrownException);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          dartCopy.recordDependencyNotInProcess();
          removeFromParts(source, dartEntry);
          dartCopy.invalidateAllInformation();
          dartCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
          workManager.add(source, SourcePriority.UNKNOWN);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.setState(DartEntry.EXPORTED_LIBRARIES, CacheState.ERROR);
          dartCopy.setState(DartEntry.IMPORTED_LIBRARIES, CacheState.ERROR);
          dartCopy.setState(DartEntry.INCLUDED_PARTS, CacheState.ERROR);
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
   * Record the results produced by performing a {@link ResolveDartUnitTask}. If the results were
   * computed from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordResolveDartUnitTaskResults(ResolveDartUnitTask task)
      throws AnalysisException {
    Source unitSource = task.getSource();
    Source librarySource = task.getLibrarySource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(unitSource);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(unitSource);
      } else if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-Dart file as a Dart file: "
                + unitSource.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      long sourceTime = getModificationStamp(unitSource);
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
          dartCopy.setValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource, task.getResolvedUnit());
          cache.storedAst(unitSource);
        } else {
          dartCopy.setStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource, CacheState.ERROR);
          cache.removedAst(unitSource);
        }
        dartCopy.setException(thrownException);

        cache.put(unitSource, dartCopy);
        dartEntry = dartCopy;
      } else {
        logInformation("Resolution results discarded for " + debuggingString(unitSource)
            + "; sourceTime = " + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
            + dartEntry.getModificationTime(), thrownException);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          if (dartCopy.getState(DartEntry.RESOLVED_UNIT) == CacheState.IN_PROCESS) {
//            dartCopy.setState(DartEntry.RESOLVED_UNIT, librarySource, CacheState.INVALID);
//          }
          removeFromParts(unitSource, dartEntry);
          dartCopy.invalidateAllInformation();
          dartCopy.setModificationTime(sourceTime);
          cache.removedAst(unitSource);
          workManager.add(unitSource, SourcePriority.UNKNOWN);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.setStateInLibrary(DartEntry.RESOLVED_UNIT, librarySource, CacheState.ERROR);
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
  private HtmlEntry recordResolveHtmlTaskResults(ResolveHtmlTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    HtmlEntry htmlEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof HtmlEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent an HTML file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to resolve non-HTML file as an HTML file: "
                + source.getFullName());
      }
      htmlEntry = (HtmlEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
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
          htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.FLUSHED);
          htmlCopy.setValue(HtmlEntry.RESOLVED_UNIT, task.getResolvedUnit());
          htmlCopy.setValue(HtmlEntry.ELEMENT, task.getElement());
          htmlCopy.setValue(HtmlEntry.RESOLUTION_ERRORS, task.getResolutionErrors());
          cache.storedAst(source);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setHtmlUnit(task.getResolvedUnit());
          notice.setErrors(htmlCopy.getAllErrors(), htmlCopy.getValue(SourceEntry.LINE_INFO));
        } else {
          htmlCopy.recordResolutionError();
          cache.removedAst(source);
        }
        htmlCopy.setException(thrownException);
        cache.put(source, htmlCopy);
        htmlEntry = htmlCopy;
      } else {
        logInformation("Resolution results discarded for " + debuggingString(source)
            + "; sourceTime = " + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
            + htmlEntry.getModificationTime(), thrownException);
        HtmlEntryImpl htmlCopy = htmlEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
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
          htmlCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
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
   * Record the results produced by performing a {@link ScanDartTask}. If the results were computed
   * from data that is now out-of-date, then the results will not be recorded.
   * 
   * @param task the task that was performed
   * @return an entry containing the computed results
   * @throws AnalysisException if the results could not be recorded
   */
  private DartEntry recordScanDartTaskResults(ScanDartTask task) throws AnalysisException {
    Source source = task.getSource();
    AnalysisException thrownException = task.getException();
    DartEntry dartEntry = null;
    synchronized (cacheLock) {
      SourceEntry sourceEntry = cache.get(source);
      if (sourceEntry == null) {
        throw new ObsoleteSourceAnalysisException(source);
      } else if (!(sourceEntry instanceof DartEntry)) {
        // This shouldn't be possible because we should never have performed the task if the source
        // didn't represent a Dart file, but check to be safe.
        throw new AnalysisException(
            "Internal error: attempting to parse non-Dart file as a Dart file: "
                + source.getFullName());
      }
      dartEntry = (DartEntry) sourceEntry;
      long sourceTime = getModificationStamp(source);
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
          dartCopy.setValue(DartEntry.TOKEN_STREAM, task.getTokenStream());
          dartCopy.setValue(DartEntry.SCAN_ERRORS, task.getErrors());
          cache.storedAst(source);
          workManager.add(source, SourcePriority.NORMAL_PART);

          ChangeNoticeImpl notice = getNotice(source);
          notice.setErrors(dartEntry.getAllErrors(), lineInfo);
        } else {
          removeFromParts(source, dartEntry);
          dartCopy.recordScanError();
          cache.removedAst(source);
        }
        dartCopy.setException(thrownException);
        cache.put(source, dartCopy);
        dartEntry = dartCopy;
      } else {
        logInformation(
            "Scan results discarded for " + debuggingString(source) + "; sourceTime = "
                + sourceTime + ", resultTime = " + resultTime + ", cacheTime = "
                + dartEntry.getModificationTime(),
            thrownException);
        DartEntryImpl dartCopy = dartEntry.getWritableCopy();
        if (thrownException == null || resultTime >= 0L) {
          //
          // The analysis was performed on out-of-date sources. Mark the cache so that the sources
          // will be re-analyzed using the up-to-date sources.
          //
//          dartCopy.recordScanNotInProcess();
          removeFromParts(source, dartEntry);
          dartCopy.invalidateAllInformation();
          dartCopy.setModificationTime(sourceTime);
          cache.removedAst(source);
          workManager.add(source, SourcePriority.UNKNOWN);
        } else {
          //
          // We could not determine whether the sources were up-to-date or out-of-date. Mark the
          // cache so that we won't attempt to re-analyze the sources until there's a good chance
          // that we'll be able to do so without error.
          //
          dartCopy.recordScanError();
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
   * Remove the given library from the list of containing libraries for all of the parts referenced
   * by the given entry.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param librarySource the library to be removed
   * @param dartEntry the entry containing the list of included parts
   */
  private void removeFromParts(Source librarySource, DartEntry dartEntry) {
    Source[] oldParts = dartEntry.getValue(DartEntry.INCLUDED_PARTS);
    for (int i = 0; i < oldParts.length; i++) {
      Source partSource = oldParts[i];
      DartEntry partEntry = getReadableDartEntry(partSource);
      if (partEntry != null && partEntry != dartEntry) {
        DartEntryImpl partCopy = partEntry.getWritableCopy();
        partCopy.removeContainingLibrary(librarySource);
        if (partCopy.getLibrariesContaining().length == 0 && !exists(partSource)) {
          cache.remove(partSource);
        } else {
          cache.put(partSource, partCopy);
        }
      }
    }
  }

  /**
   * Remove the given source from the priority order if it is in the list.
   * 
   * @param source the source to be removed
   */
  private void removeFromPriorityOrder(Source source) {
    int count = priorityOrder.length;
    ArrayList<Source> newOrder = new ArrayList<Source>(count);
    for (int i = 0; i < count; i++) {
      if (!priorityOrder[i].equals(source)) {
        newOrder.add(priorityOrder[i]);
      }
    }
    if (newOrder.size() < count) {
      setAnalysisPriorityOrder(newOrder);
    }
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
      logInformation("Added new source: " + debuggingString(source));
    } else {
      SourceEntryImpl sourceCopy = sourceEntry.getWritableCopy();
      long oldTime = sourceCopy.getModificationTime();
      sourceCopy.setModificationTime(getModificationStamp(source));
      // TODO(brianwilkerson) Understand why we're not invalidating the cache.
      cache.put(source, sourceCopy);
      logInformation("Added new source: " + debuggingString(source) + " (previously modified at "
          + oldTime + ")");
    }
    if (sourceEntry instanceof HtmlEntry) {
      workManager.add(source, SourcePriority.HTML);
    } else {
      workManager.add(source, SourcePriority.UNKNOWN);
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
    if (sourceEntry == null || sourceEntry.getModificationTime() == getModificationStamp(source)) {
      // Either we have removed this source, in which case we don't care that it is changed, or we
      // have already invalidated the cache and don't need to invalidate it again.
      if (sourceEntry == null) {
        logInformation("Modified source, but there is no entry: " + debuggingString(source));
      } else {
        logInformation("Modified source, but modification time matches: " + debuggingString(source));
      }
      return;
    }
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      long oldTime = htmlCopy.getModificationTime();
      htmlCopy.setModificationTime(getModificationStamp(source));
      invalidateAngularResolution(htmlCopy);
      htmlCopy.invalidateAllInformation();
      cache.put(source, htmlCopy);
      cache.removedAst(source);
      workManager.add(source, SourcePriority.HTML);
      logInformation("Modified HTML source: " + debuggingString(source)
          + " (previously modified at " + oldTime + ")");
    } else if (sourceEntry instanceof DartEntry) {
      Source[] containingLibraries = getLibrariesContaining(source);
      HashSet<Source> librariesToInvalidate = new HashSet<Source>();
      for (Source containingLibrary : containingLibraries) {
        librariesToInvalidate.add(containingLibrary);
        for (Source dependentLibrary : getLibrariesDependingOn(containingLibrary)) {
          librariesToInvalidate.add(dependentLibrary);
        }
      }

      PrintStringWriter writer = new PrintStringWriter();
      long oldTime = sourceEntry.getModificationTime();
      writer.println("Modified Dart source: " + debuggingString(source)
          + " (previously modified at " + oldTime + ")");

      for (Source library : librariesToInvalidate) {
//    for (Source library : containingLibraries) {
        invalidateLibraryResolution(library, writer);
      }

      removeFromParts(source, ((DartEntry) cache.get(source)));
      DartEntryImpl dartCopy = ((DartEntry) cache.get(source)).getWritableCopy();
      dartCopy.setModificationTime(getModificationStamp(source));
      dartCopy.invalidateAllInformation();
      cache.put(source, dartCopy);
      cache.removedAst(source);
      workManager.add(source, SourcePriority.UNKNOWN);

      logInformation(writer.toString());
    }
  }

  /**
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @param source the source that has been deleted
   */
  private void sourceRemoved(Source source) {
    PrintStringWriter writer = new PrintStringWriter();
    writer.println("Removed source: " + debuggingString(source));

    SourceEntry sourceEntry = cache.get(source);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      invalidateAngularResolution(htmlCopy);
    } else if (sourceEntry instanceof DartEntry) {
      HashSet<Source> libraries = new HashSet<Source>();
      for (Source librarySource : getLibrariesContaining(source)) {
        libraries.add(librarySource);
        for (Source dependentLibrary : getLibrariesDependingOn(librarySource)) {
          libraries.add(dependentLibrary);
        }
      }
      for (Source librarySource : libraries) {
        invalidateLibraryResolution(librarySource, writer);
      }
    }
    cache.remove(source);
    workManager.remove(source);
    removeFromPriorityOrder(source);
    logInformation(writer.toString());
  }

  /**
   * Check the cache for any invalid entries (entries whose modification time does not match the
   * modification time of the source associated with the entry). Invalid entries will be marked as
   * invalid so that the source will be re-analyzed.
   * <p>
   * <b>Note:</b> This method must only be invoked while we are synchronized on {@link #cacheLock}.
   * 
   * @return {@code true} if at least one entry was invalid
   */
  private boolean validateCacheConsistency() {
    long consistencyCheckStart = System.nanoTime();
    ArrayList<Source> missingSources = new ArrayList<Source>();
    int inconsistentCount = 0;
    synchronized (cacheLock) {
      for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
        Source source = entry.getKey();
        SourceEntry sourceEntry = entry.getValue();
        long sourceTime = getModificationStamp(source);
        if (sourceTime != sourceEntry.getModificationTime()) {
          sourceChanged(source);
          inconsistentCount++;
        }
        if (sourceEntry.getException() != null) {
          if (!exists(source)) {
            missingSources.add(source);
          }
        }
      }
    }
    long consistencyCheckEnd = System.nanoTime();
    @SuppressWarnings("resource")
    PrintStringWriter writer = new PrintStringWriter();
    writer.print("Consistency check took ");
    writer.print((consistencyCheckEnd - consistencyCheckStart) / 1000000.0);
    writer.println(" ms and found");
    writer.print("  ");
    writer.print(inconsistentCount);
    writer.println(" inconsistent entries");
    writer.print("  ");
    writer.print(missingSources.size());
    writer.println(" missing sources");
    for (Source source : missingSources) {
      writer.print("    ");
      writer.println(source.getFullName());
    }
    logInformation(writer.toString());
    return inconsistentCount > 0;
  }
}

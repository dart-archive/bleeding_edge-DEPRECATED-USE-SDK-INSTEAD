/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisDelta;
import com.google.dart.engine.context.AnalysisDelta.AnalysisLevel;
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.internal.context.ChangeNoticeImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerError;
import com.google.dart.server.AnalysisServerErrorCode;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.Element;
import com.google.dart.server.FixableErrorCodesConsumer;
import com.google.dart.server.FixesConsumer;
import com.google.dart.server.InternalAnalysisServer;
import com.google.dart.server.MinorRefactoringsConsumer;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.SearchResult;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.SourceSet;
import com.google.dart.server.TypeHierarchyConsumer;
import com.google.dart.server.TypeHierarchyItem;
import com.google.dart.server.internal.local.computer.DartUnitFixesComputer;
import com.google.dart.server.internal.local.computer.DartUnitHighlightsComputer;
import com.google.dart.server.internal.local.computer.DartUnitMinorRefactoringsComputer;
import com.google.dart.server.internal.local.computer.DartUnitNavigationComputer;
import com.google.dart.server.internal.local.computer.DartUnitOutlineComputer;
import com.google.dart.server.internal.local.computer.ElementReferencesComputer;
import com.google.dart.server.internal.local.computer.TypeHierarchyComputer;
import com.google.dart.server.internal.local.operation.ApplyAnalysisDeltaOperation;
import com.google.dart.server.internal.local.operation.ApplyChangesOperation;
import com.google.dart.server.internal.local.operation.ComputeFixesOperation;
import com.google.dart.server.internal.local.operation.ComputeMinorRefactoringsOperation;
import com.google.dart.server.internal.local.operation.ComputeTypeHierarchyOperation;
import com.google.dart.server.internal.local.operation.CreateContextOperation;
import com.google.dart.server.internal.local.operation.DeleteContextOperation;
import com.google.dart.server.internal.local.operation.GetContextOperation;
import com.google.dart.server.internal.local.operation.GetFixableErrorCodesOperation;
import com.google.dart.server.internal.local.operation.NotificationOperation;
import com.google.dart.server.internal.local.operation.PerformAnalysisOperation;
import com.google.dart.server.internal.local.operation.SearchReferencesOperation;
import com.google.dart.server.internal.local.operation.ServerOperation;
import com.google.dart.server.internal.local.operation.ServerOperationQueue;
import com.google.dart.server.internal.local.operation.SetOptionsOperation;
import com.google.dart.server.internal.local.operation.SetPrioritySourcesOperation;
import com.google.dart.server.internal.local.operation.ShutdownOperation;
import com.google.dart.server.internal.local.operation.SubscribeOperation;
import com.google.dart.server.internal.local.source.FileResource;
import com.google.dart.server.internal.local.source.PackageMapUriResolver;
import com.google.dart.server.internal.local.source.Resource;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process implementation of {@link AnalysisServer}.
 * 
 * @coverage dart.server.local
 */
public class LocalAnalysisServerImpl implements AnalysisServer, InternalAnalysisServer {
  /**
   * The thread that runs {@link #index}.
   */
  private class LocalAnalysisServerIndexThread extends Thread {
    public LocalAnalysisServerIndexThread() {
      setName("LocalAnalysisServerIndexThread");
      setDaemon(true);
    }

    @Override
    public void run() {
      index.run();
    }
  }

  /**
   * The thread that executes {@link ServerOperation}.
   */
  private class LocalAnalysisServerOperationThread extends Thread {
    public LocalAnalysisServerOperationThread() {
      setName("LocalAnalysisServerOperationThread");
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        ServerOperation operation = operationQueue.take(0);
        while (test_paused) {
          Thread.yield();
        }
        if (operation == ShutdownOperation.INSTANCE) {
          break;
        }
        try {
          operation.performOperation(LocalAnalysisServerImpl.this);
        } catch (AnalysisServerErrorException serverException) {
          onServerError(serverException.error);
        } catch (Throwable e) {
          onServerError(AnalysisServerErrorCode.EXCEPTION, e.getMessage());
        }
        operationQueue.markLastOperationCompleted();
      }
    }
  }

  private static final String VERSION = "0.0.1";

  /**
   * A table mapping SDK directories to the corresponding {@link DartSdk} instances.
   */
  private static final Map<String, DartSdk> sdkMap = Maps.newHashMap();

  /**
   * The queue of {@link ServerOperation}s to execute.
   */
  private final ServerOperationQueue operationQueue = new ServerOperationQueue();

  /**
   * The {@link Index} instance for this server.
   */
  private final Index index = IndexFactory.newIndex(IndexFactory.newMemoryIndexStore());

  /**
   * The {@link SearchEngine} instance for this server.
   */
  private final SearchEngine searchEngine = SearchEngineFactory.createSearchEngine(index);

  /**
   * This is used only for testing purposes and allows tests to control the order of operations on
   * multiple threads.
   */
  private boolean test_paused;

  /**
   * This is used only for testing purposes and allows tests to enable logging.
   */
  private boolean test_log;

  /**
   * This is used only for testing purposes and allows tests to check the order of operations.
   */
  private List<String> test_analyzedContexts;

  /**
   * The unique ID for the next context.
   */
  private final AtomicInteger nextId = new AtomicInteger();

  /**
   * A table mapping context id's to the analysis contexts associated with them.
   */
  private final Map<String, AnalysisContext> contextMap = Maps.newHashMap();

  /**
   * A table mapping context id's to the sources known in the associated contexts.
   */
  private final Map<String, Set<Source>> contextKnownSourcesMap = Maps.newHashMap();

  /**
   * A table mapping context id's to the sources explicitly added to the associated contexts.
   */
  private final Map<String, Set<Source>> contextAddedSourcesMap = Maps.newHashMap();

  /**
   * A set of context id's with priority sources.
   */
  private final Set<String> priorityContexts = Sets.newHashSet();

  /**
   * A table mapping context id's to the subscriptions for notifications associated with them.
   */
  private final Map<String, Map<NotificationKind, SourceSetBasedProvider>> notificationMap = Maps.newHashMap();

  /**
   * The listener that will receive notification when new analysis results become available.
   */
  private final BroadcastAnalysisServerListener listener = new BroadcastAnalysisServerListener();

  public LocalAnalysisServerImpl() {
    new LocalAnalysisServerIndexThread().start();
    new LocalAnalysisServerOperationThread().start();
  }

  @Override
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.addListener(listener);
  }

  @Override
  public void applyAnalysisDelta(String contextId, AnalysisDelta delta) {
    operationQueue.add(new ApplyAnalysisDeltaOperation(contextId, delta));
  }

  @Override
  public void applyChanges(String contextId, ChangeSet changeSet) {
    operationQueue.add(new ApplyChangesOperation(contextId, changeSet));
  }

  @Override
  public void computeFixes(String contextId, AnalysisError[] errors, FixesConsumer consumer) {
    operationQueue.add(new ComputeFixesOperation(contextId, errors, consumer));
  }

  @Override
  public void computeMinorRefactorings(String contextId, Source source, int offset, int length,
      MinorRefactoringsConsumer consumer) {
    operationQueue.add(new ComputeMinorRefactoringsOperation(
        contextId,
        source,
        offset,
        length,
        consumer));
  }

  @Override
  public void computeTypeHierarchy(String contextId, Element element, TypeHierarchyConsumer consumer) {
    operationQueue.add(new ComputeTypeHierarchyOperation(contextId, element, consumer));
  }

  @Override
  public String createContext(String name, String sdkDirectory, Map<String, String> packageMap) {
    String contextId = name + "-" + nextId.getAndIncrement();
    operationQueue.add(new CreateContextOperation(contextId, sdkDirectory, packageMap));
    return contextId;
  }

  @Override
  public void deleteContext(String contextId) {
    operationQueue.add(new DeleteContextOperation(contextId));
  }

  @Override
  public AnalysisContext getContext(String contextId) {
    GetContextOperation operation = new GetContextOperation(contextId);
    operationQueue.add(operation);
    return operation.getContext();
  }

  @Override
  public Map<String, AnalysisContext> getContextMap() {
    return contextMap;
  }

  @Override
  public void getFixableErrorCodes(String contextId, FixableErrorCodesConsumer consumer) {
    operationQueue.add(new GetFixableErrorCodesOperation(contextId, consumer));
  }

  @Override
  public Index getIndex() {
    return index;
  }

  /**
   * Implementation for {@link #applyAnalysisDelta(String, AnalysisDelta)}.
   */
  public void internalApplyAnalysisDelta(String contextId, AnalysisDelta delta) {
    AnalysisContext context = getAnalysisContext(contextId);
    Set<Source> sourcesMap = getSourcesMap(contextId, contextAddedSourcesMap);
    for (Entry<Source, AnalysisLevel> entry : delta.getAnalysisLevels().entrySet()) {
      Source source = entry.getKey();
      if (entry.getValue() == AnalysisLevel.NONE) {
        sourcesMap.remove(source);
      } else {
        sourcesMap.add(source);
      }
    }
    context.applyAnalysisDelta(delta);
    schedulePerformAnalysisOperation(contextId, false);
  }

  /**
   * Implementation for {@link #applyChanges(String, ChangeSet)}.
   */
  public void internalApplyChanges(String contextId, ChangeSet changeSet) throws Exception {
    AnalysisContext context = getAnalysisContext(contextId);
    getSourcesMap(contextId, contextAddedSourcesMap).addAll(changeSet.getAddedSources());
    context.applyChanges(changeSet);
    schedulePerformAnalysisOperation(contextId, false);
  }

  /**
   * Implementation for {@link #computeFixes(String, AnalysisError[], FixesConsumer)}.
   */
  public void internalComputeFixes(String contextId, AnalysisError[] errors, FixesConsumer consumer)
      throws Exception {
    AnalysisContext analysisContext = getAnalysisContext(contextId);
    for (AnalysisError error : errors) {
      Source source = error.getSource();
      Source[] librarySources = analysisContext.getLibrariesContaining(source);
      if (librarySources.length != 0) {
        Source librarySource = librarySources[0];
        CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
        if (unit != null) {
          new DartUnitFixesComputer(searchEngine, contextId, analysisContext, unit, error, consumer).compute();
        }
      }
    }
    // send "done" notification
    consumer.computedFixes(Maps.<AnalysisError, CorrectionProposal[]> newHashMap(), true);
  }

  /**
   * Implementation for
   * {@link #computeMinorRefactorings(String, Source, int, MinorRefactoringsConsumer)}.
   */
  public void internalComputeMinorRefactorings(String contextId, Source source, int offset,
      int length, MinorRefactoringsConsumer consumer) throws Exception {
    AnalysisContext analysisContext = getAnalysisContext(contextId);
    Source[] librarySources = analysisContext.getLibrariesContaining(source);
    if (librarySources.length != 0) {
      Source librarySource = librarySources[0];
      CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
      if (unit != null) {
        new DartUnitMinorRefactoringsComputer(
            searchEngine,
            contextId,
            analysisContext,
            source,
            unit,
            offset,
            length,
            consumer).compute();
      }
    }
    consumer.computedProposals(CorrectionProposal.EMPTY_ARRAY, true);
  }

  /**
   * Implementation for {@link #computeTypeHierarchy(String, Element, TypeHierarchyConsumer)}.
   */
  public void internalComputeTypeHierarchy(String contextId, Element element,
      TypeHierarchyConsumer consumer) throws Exception {
    TypeHierarchyItem result = null;
    // prepare context
    AnalysisContext analysisContext = getAnalysisContext(contextId);
    Source source = element.getSource();
    Source[] librarySources = analysisContext.getLibrariesContaining(source);
    // compute
    if (librarySources.length != 0) {
      Source librarySource = librarySources[0];
      CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
      if (unit != null) {
        CompilationUnitElement unitElement = unit.getElement();
        result = new TypeHierarchyComputer(searchEngine, contextId, unitElement, element).compute();
      }
    }
    // done
    consumer.computedHierarchy(result);
  }

  /**
   * Implementation for {@link #createContext(String, String, Map)}.
   */
  public void internalCreateContext(String contextId, String sdkDirectory,
      Map<String, String> packageMap) throws Exception {
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    DartSdk sdk = getSdk(contextId, sdkDirectory);
    // prepare package map
    Map<String, Resource> packageResourceMap = Maps.newHashMap();
    for (Entry<String, String> entry : packageMap.entrySet()) {
      String packageName = entry.getKey();
      String packageDirName = entry.getValue();
      File packageDir = new File(packageDirName);
      FileResource packageResource = new FileResource(packageDir);
      packageResourceMap.put(packageName, packageResource);
    }
    // set source factory
    SourceFactory sourceFactory = new SourceFactory(
        new DartUriResolver(sdk),
        new FileUriResolver(),
        new PackageMapUriResolver(packageResourceMap));
    context.setSourceFactory(sourceFactory);
    // add context
    contextMap.put(contextId, context);
    schedulePerformAnalysisOperation(contextId, false);
  }

  /**
   * Implementation for {@link #deleteContext(String)}.
   */
  public void internalDeleteContext(String contextId) throws Exception {
    // stop processing this context
    operationQueue.removeWithContextId(contextId);
    // remove associated information
    contextKnownSourcesMap.remove(contextId);
    contextAddedSourcesMap.remove(contextId);
    notificationMap.remove(contextId);
    // prepare context
    AnalysisContext context = contextMap.remove(contextId);
    if (context == null) {
      throw new AnalysisServerErrorException(AnalysisServerErrorCode.INVALID_CONTEXT_ID, contextId);
    }
    // remove from index
    index.removeContext(context);
  }

  public void internalGetFixableErrorCodes(String contextId, FixableErrorCodesConsumer consumer) {
    ErrorCode[] fixableErrorCodes = DartUnitFixesComputer.getFixableErrorCodes();
    consumer.computed(fixableErrorCodes);
  }

  /**
   * Sends one of the {@link NotificationKind}s.
   */
  public void internalNotification(String contextId, ChangeNotice changeNotice,
      NotificationKind kind) throws Exception {
    Source source = changeNotice.getSource();
    log("internalNotification: %s with %s", kind, changeNotice);
    switch (kind) {
      case ERRORS:
        listener.computedErrors(contextId, source, changeNotice.getErrors());
        break;
      case HIGHLIGHTS: {
        CompilationUnit dartUnit = changeNotice.getCompilationUnit();
        log("\tHIGHLIGHTS %s", dartUnit);
        if (dartUnit != null) {
          listener.computedHighlights(
              contextId,
              source,
              new DartUnitHighlightsComputer(dartUnit).compute());
        }
        break;
      }
      case NAVIGATION: {
        CompilationUnit dartUnit = changeNotice.getCompilationUnit();
        if (dartUnit != null) {
          listener.computedNavigation(contextId, source, new DartUnitNavigationComputer(
              contextId,
              dartUnit).compute());
        }
        break;
      }
      case OUTLINE:
        CompilationUnit dartUnit = changeNotice.getCompilationUnit();
        if (dartUnit != null) {
          listener.computedOutline(contextId, source, new DartUnitOutlineComputer(
              contextId,
              source,
              dartUnit).compute());
        }
        break;
    }
  }

  /**
   * Performs analysis in the given {@link AnalysisContext}.
   */
  public void internalPerformAnalysis(String contextId) throws Exception {
    if (test_analyzedContexts != null) {
      test_analyzedContexts.add(contextId);
    }
    AnalysisContext context = getAnalysisContext(contextId);
    Set<Source> knownSources = getSourcesMap(contextId, contextKnownSourcesMap);
    // prepare results
    AnalysisResult result = context.performAnalysisTask();
    ChangeNotice[] notices = result.getChangeNotices();
    log("internalPerformAnalysis: %s", result.getTaskClassName());
    if (notices == null) {
      return;
    }
    // remember known sources
    for (ChangeNotice changeNotice : notices) {
      Source source = changeNotice.getSource();
      log("\tsource: %s", source);
      knownSources.add(source);
    }
    // index units
    for (ChangeNotice changeNotice : notices) {
      CompilationUnit dartUnit = changeNotice.getCompilationUnit();
      if (dartUnit != null) {
        index.indexUnit(context, dartUnit);
      }
    }
    // schedule analysis again
    schedulePerformAnalysisOperation(contextId, true);
    // schedule notifications
    Map<NotificationKind, SourceSetBasedProvider> notifications = notificationMap.get(contextId);
    log("\tnotifications: %s", notifications);
    if (notifications != null) {
      for (Entry<NotificationKind, SourceSetBasedProvider> entry : notifications.entrySet()) {
        NotificationKind notificationKind = entry.getKey();
        SourceSetBasedProvider sourceProvider = entry.getValue();
        for (ChangeNotice changeNotice : notices) {
          Source source = changeNotice.getSource();
          if (sourceProvider.apply(source)) {
            log("\tadd NotificationOperation: %s with %s", notificationKind, changeNotice);
            operationQueue.add(new NotificationOperation(contextId, changeNotice, notificationKind));
          }
        }
      }
    }
  }

  /**
   * Implementation for {@link #searchElementReferences(Element, SearchResultsConsumer)}.
   */
  public void internalSearchElementReferences(String contextId, Element element,
      SearchResultsConsumer consumer) throws Exception {
    AnalysisContext context = getAnalysisContext(contextId);
    new ElementReferencesComputer(searchEngine, context, element, consumer).compute();
    consumer.computed(SearchResult.EMPTY_ARRAY, true);
  }

  /**
   * Implementation for {@link #setOptions(String, AnalysisOptions)}.
   */
  public void internalSetOptions(String contextId, AnalysisOptions options) throws Exception {
    AnalysisContext context = getAnalysisContext(contextId);
    context.setAnalysisOptions(options);
    schedulePerformAnalysisOperation(contextId, false);
  }

  /**
   * Implementation for {@link #setPrioritySources(String, Source[])}.
   */
  public void internalSetPrioritySources(String contextId, Source[] sources) throws Exception {
    AnalysisContext context = getAnalysisContext(contextId);
    context.setAnalysisPriorityOrder(Lists.newArrayList(sources));
    schedulePerformAnalysisOperation(contextId, false);
  }

  /**
   * Implementation for {@link #subscribe(String, Map)}.
   */
  public void internalSubscribe(String contextId, Map<NotificationKind, SourceSet> subscriptions)
      throws Exception {
    log("internalSubscribe: %s", subscriptions);
    AnalysisContext analysisContext = getAnalysisContext(contextId);
    Set<Source> knownSources = getSourcesMap(contextId, contextKnownSourcesMap);
    Set<Source> addedSources = getSourcesMap(contextId, contextAddedSourcesMap);
    Map<NotificationKind, SourceSetBasedProvider> notifications = notificationMap.get(contextId);
    if (notifications == null) {
      notifications = Maps.newHashMap();
      notificationMap.put(contextId, notifications);
    }
    // prepare new sources to send notifications for
    for (Entry<NotificationKind, SourceSet> entry : subscriptions.entrySet()) {
      NotificationKind kind = entry.getKey();
      SourceSet sourceSet = entry.getValue();
      SourceSetBasedProvider oldProvider = notifications.get(kind);
      SourceSetBasedProvider newProvider = new SourceSetBasedProvider(
          sourceSet,
          knownSources,
          addedSources);
      // schedule notification operations for new sources
      Set<Source> newSources = newProvider.computeNewSources(oldProvider);
      log("\tnewSources: %s", newSources);
      for (Source unitSource : newSources) {
        log("\tunitSource: %s", unitSource);
        Source[] librarySources = analysisContext.getLibrariesContaining(unitSource);
        log("\t\tlibrarySources: %s", librarySources.length);
        if (librarySources.length != 0) {
          Source librarySource = librarySources[0];
          log("\t\tlibrarySource: %s", librarySource);
          CompilationUnit unit = analysisContext.resolveCompilationUnit(unitSource, librarySource);
          log("\t\tunit: %s", unit);
          if (unit != null) {
            ChangeNoticeImpl changeNotice = new ChangeNoticeImpl(unitSource);
            changeNotice.setCompilationUnit(unit);
            log("\t\tadd NotificationOperation: %s with %s", kind, changeNotice);
            operationQueue.add(new NotificationOperation(contextId, changeNotice, kind));
          }
        }
      }
      // put new provider
      notifications.put(kind, newProvider);
    }
  }

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.removeListener(listener);
  }

  @Override
  public void searchClassMemberDeclarations(String name, SearchResultsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void searchClassMemberReferences(String name, SearchResultsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void searchElementReferences(Element element, boolean withPotential,
      SearchResultsConsumer consumer) {
    // TODO(scheglov) support for "withPotential"
    operationQueue.add(new SearchReferencesOperation(element, consumer));
  }

  @Override
  public void searchTopLevelDeclarations(String contextId, String pattern,
      SearchResultsConsumer consumer) {
    // TODO(scheglov) implement
  }

  @Override
  public void setOptions(String contextId, AnalysisOptions options) {
    operationQueue.add(new SetOptionsOperation(contextId, options));
  }

  @Override
  public void setPrioritySources(String contextId, Source[] sources) {
    operationQueue.add(new SetPrioritySourcesOperation(contextId, sources));
    if (sources.length != 0) {
      priorityContexts.add(contextId);
    } else {
      priorityContexts.remove(contextId);
    }
  }

  @Override
  public void shutdown() {
    operationQueue.add(ShutdownOperation.INSTANCE);
    index.stop();
  }

  @Override
  public void subscribe(String contextId, Map<NotificationKind, SourceSet> subscriptions) {
    operationQueue.add(new SubscribeOperation(contextId, subscriptions));
  }

  @VisibleForTesting
  public void test_addOperation(ServerOperation operation) {
    operationQueue.add(operation);
  }

  @VisibleForTesting
  public void test_pingListeners() {
    listener.computedErrors(null, null, null);
  }

  /**
   * Sets the {@link List} to record analyzed contexts into.
   */
  public void test_setAnalyzedContexts(List<String> analyzedContexts) {
    test_analyzedContexts = analyzedContexts;
  }

  @VisibleForTesting
  public void test_setLog(boolean log) {
    this.test_log = log;
  }

  @VisibleForTesting
  public void test_setPaused(boolean paused) {
    this.test_paused = paused;
  }

  @VisibleForTesting
  public void test_waitForWorkerComplete() {
    while (!operationQueue.isEmpty()) {
      Thread.yield();
    }
  }

  @Override
  public String version() {
    return VERSION;
  }

  /**
   * Returns the {@link AnalysisContext} for the given identifier, maybe {@code null}.
   */
  private AnalysisContext getAnalysisContext(String contextId) {
    AnalysisContext context = contextMap.get(contextId);
    if (context == null) {
      throw new AnalysisServerErrorException(AnalysisServerErrorCode.INVALID_CONTEXT_ID, contextId);
    }
    return context;
  }

  private DartSdk getSdk(String contextId, String directory) {
    DartSdk sdk = sdkMap.get(directory);
    if (sdk == null) {
      File directoryFile = new File(directory);
      sdk = new DirectoryBasedDartSdk(directoryFile);
      sdkMap.put(directory, sdk);
      AnalysisContext context = sdk.getContext();
      SourceFactory factory = context.getSourceFactory();
      AnalysisDelta delta = new AnalysisDelta();
      for (String uri : sdk.getUris()) {
        delta.setAnalysisLevel(factory.forUri(uri), AnalysisLevel.RESOLVED);
      }
      context.applyAnalysisDelta(delta);
      String sdkContextId = "dart-sdk-internal-" + nextId.getAndIncrement();
      contextMap.put(sdkContextId, context);
      schedulePerformAnalysisOperation(sdkContextId, false);
    }
    return sdk;
  }

  /**
   * Returns an existing or just added {@link Source} set associated with the given context.
   */
  private Set<Source> getSourcesMap(String contextId, Map<String, Set<Source>> contextSourcesMap) {
    Set<Source> sources = contextSourcesMap.get(contextId);
    if (sources == null) {
      sources = Sets.newHashSet();
      contextSourcesMap.put(contextId, sources);
    }
    return sources;
  }

  private void log(String msg, Object... arguments) {
    if (test_log) {
      String message = String.format(msg, arguments);
      System.out.println(message);
    }
  }

  /**
   * Reports an error to the {@link #listener}.
   * 
   * @param error the error to report
   */
  private void onServerError(AnalysisServerError error) {
    listener.onServerError(error);
  }

  /**
   * Reports an error to the {@link #listener}.
   * 
   * @param errorCode the error code to be associated with this error
   * @param arguments the arguments used to build the error message
   */
  private void onServerError(AnalysisServerErrorCode errorCode, Object... arguments) {
    AnalysisServerError error = new AnalysisServerError(errorCode, arguments);
    onServerError(error);
  }

  /**
   * Schedules analysis for the given context.
   * 
   * @param isContinue is {@code true} if the new operation is continuation of analysis of the same
   *          contexts which was analyzed before.
   */
  private void schedulePerformAnalysisOperation(String contextId, boolean isContinue) {
    boolean isPriority = priorityContexts.contains(contextId);
    operationQueue.add(new PerformAnalysisOperation(contextId, isPriority, isContinue));
  }
}

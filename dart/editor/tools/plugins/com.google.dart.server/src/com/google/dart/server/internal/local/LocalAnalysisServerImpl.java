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
import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.AnalysisResult;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.internal.context.ChangeNoticeImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerError;
import com.google.dart.server.AnalysisServerErrorCode;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.SourceSet;
import com.google.dart.server.internal.local.computer.DartUnitHighlightsComputer;
import com.google.dart.server.internal.local.computer.DartUnitNavigationComputer;
import com.google.dart.server.internal.local.computer.DartUnitOutlineComputer;
import com.google.dart.server.internal.local.computer.DartUnitReferencesComputer;
import com.google.dart.server.internal.local.operation.ApplyChangesOperation;
import com.google.dart.server.internal.local.operation.CreateContextOperation;
import com.google.dart.server.internal.local.operation.DeleteContextOperation;
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
import java.net.URI;
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
public class LocalAnalysisServerImpl implements AnalysisServer {
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
   * This is used only for testing purposes and allows tests to disable SDK analysis for speed.
   */
  private boolean test_disableForcedSdkAnalysis;

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
  private final Map<String, Map<NotificationKind, SourceSetBaseProvider>> notificationMap = Maps.newHashMap();

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
  public void applyChanges(String contextId, ChangeSet changeSet) {
    operationQueue.add(new ApplyChangesOperation(contextId, changeSet));
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
    AnalysisContext context = contextMap.remove(contextId);
    contextKnownSourcesMap.remove(contextId);
    contextAddedSourcesMap.remove(contextId);
    notificationMap.remove(contextId);
    if (context == null) {
      onServerError(AnalysisServerErrorCode.INVALID_CONTEXT_ID, contextId);
      return;
    }
    operationQueue.removeWithContextId(contextId);
  }

  /**
   * Sends one of the {@link NotificationKind}s.
   */
  public void internalNotification(String contextId, ChangeNotice changeNotice,
      NotificationKind kind) throws Exception {
    Source source = changeNotice.getSource();
    switch (kind) {
      case ERRORS:
        listener.computedErrors(contextId, source, changeNotice.getErrors());
        break;
      case HIGHLIGHTS: {
        CompilationUnit dartUnit = changeNotice.getCompilationUnit();
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
          listener.computedNavigation(
              contextId,
              source,
              new DartUnitNavigationComputer(dartUnit).compute());
        }
        break;
      }
      case OUTLINE:
        CompilationUnit dartUnit = changeNotice.getCompilationUnit();
        if (dartUnit != null) {
          listener.computedOutline(
              contextId,
              source,
              new DartUnitOutlineComputer(dartUnit).compute());
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
    if (notices == null) {
      return;
    }
    // remember known sources
    for (ChangeNotice changeNotice : notices) {
      Source source = changeNotice.getSource();
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
    Map<NotificationKind, SourceSetBaseProvider> notifications = notificationMap.get(contextId);
    if (notifications != null) {
      for (Entry<NotificationKind, SourceSetBaseProvider> entry : notifications.entrySet()) {
        NotificationKind notificationKind = entry.getKey();
        SourceSetBaseProvider sourceProvider = entry.getValue();
        for (ChangeNotice changeNotice : notices) {
          Source source = changeNotice.getSource();
          if (sourceProvider.apply(source)) {
            operationQueue.add(new NotificationOperation(contextId, changeNotice, notificationKind));
          }
        }
      }
    }
  }

  /**
   * Implementation for {@link #searchReferences(String, Source, int, SearchResultsConsumer)}.
   */
  public void internalSearchReferences(String contextId, Source source, int offset,
      SearchResultsConsumer consumer) throws Exception {
    AnalysisContext analysisContext = getAnalysisContext(contextId);
    Source[] librarySources = analysisContext.getLibrariesContaining(source);
    // TODO(scheglov) references from multiple libraries
    if (librarySources.length != 0) {
      Source librarySource = librarySources[0];
      CompilationUnit unit = analysisContext.resolveCompilationUnit(source, librarySource);
      if (unit != null) {
        new DartUnitReferencesComputer(searchEngine, contextId, source, unit, offset, consumer).compute();
      }
    }
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
    AnalysisContext analysisContext = getAnalysisContext(contextId);
    Set<Source> knownSources = getSourcesMap(contextId, contextKnownSourcesMap);
    Set<Source> addedSources = getSourcesMap(contextId, contextAddedSourcesMap);
    Map<NotificationKind, SourceSetBaseProvider> notifications = notificationMap.get(contextId);
    if (notifications == null) {
      notifications = Maps.newHashMap();
      notificationMap.put(contextId, notifications);
    }
    // prepare new sources to send notifications for
    for (Entry<NotificationKind, SourceSet> entry : subscriptions.entrySet()) {
      NotificationKind kind = entry.getKey();
      SourceSet sourceSet = entry.getValue();
      SourceSetBaseProvider oldProvider = notifications.get(kind);
      SourceSetBaseProvider newProvider = new SourceSetBaseProvider(
          sourceSet,
          knownSources,
          addedSources);
      // schedule notification operations for new sources
      Set<Source> newSources = newProvider.computeNewSources(oldProvider);
      for (Source unitSource : newSources) {
        Source[] librarySources = analysisContext.getLibrariesContaining(unitSource);
        if (librarySources.length != 0) {
          Source librarySource = librarySources[0];
          CompilationUnit unit = analysisContext.resolveCompilationUnit(unitSource, librarySource);
          if (unit != null) {
            ChangeNoticeImpl changeNotice = new ChangeNoticeImpl(unitSource);
            changeNotice.setCompilationUnit(unit);
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
  public void searchReferences(String contextId, Source source, int offset,
      SearchResultsConsumer consumer) {
    operationQueue.add(new SearchReferencesOperation(contextId, source, offset, consumer));
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
  public void test_disableForcedSdkAnalysis() {
    this.test_disableForcedSdkAnalysis = true;
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
      // schedule SDK libraries analysis
      DartUriResolver dartUriResolver = new DartUriResolver(sdk);
      ChangeSet changeSet = new ChangeSet();
      for (String uri : sdk.getUris()) {
        if (!test_disableForcedSdkAnalysis || uri.equals(DartSdk.DART_CORE)) {
          changeSet.addedSource(dartUriResolver.resolveAbsolute(URI.create(uri)));
        }
      }
      applyChanges(contextId, changeSet);
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

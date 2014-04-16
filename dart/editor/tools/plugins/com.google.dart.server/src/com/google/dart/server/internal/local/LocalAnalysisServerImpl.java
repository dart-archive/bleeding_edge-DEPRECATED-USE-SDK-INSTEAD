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
import com.google.common.collect.ImmutableSet;
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
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerError;
import com.google.dart.server.AnalysisServerErrorCode;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.SourceSet;
import com.google.dart.server.internal.local.computer.DartUnitNavigationComputer;

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
public class LocalAnalysisServerImpl implements AnalysisServer {
  /**
   * The local analysis server worker.
   */
  private class LocalAnalysisServerThread extends Thread {
    public LocalAnalysisServerThread() {
      setName("LocalAnalysisServerThread");
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

  private final ServerOperationQueue operationQueue = new ServerOperationQueue();

  /**
   * The worker thread.
   */
  private final LocalAnalysisServerThread thread;

  /**
   * This is used only for testing purposes and allows tests to control the order of operations on
   * multiple threads.
   */
  private boolean test_paused;

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
   * A set of context id's with priority sources.
   */
  private final Set<String> priorityContexts = Sets.newHashSet();

  /**
   * A table mapping context id's to the subscriptions for notifications associated with them.
   */
  private final Map<String, Map<NotificationKind, Set<Source>>> notificationMap = Maps.newHashMap();

  /**
   * The listener that will receive notification when new analysis results become available.
   */
  private final BroadcastAnalysisServerListener listener = new BroadcastAnalysisServerListener();

  public LocalAnalysisServerImpl() {
    thread = new LocalAnalysisServerThread();
    thread.start();
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
    context.applyChanges(changeSet);
    schedulePerformAnalysisOperation(contextId, false);
  }

  /**
   * Implementation for {@link #createContext(String, String, Map)}.
   */
  public void internalCreateContext(String contextId, String sdkDirectory,
      Map<String, String> packageMap) throws Exception {
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    DartSdk sdk = getSdk(sdkDirectory);
    // TODO(scheglov) PackageUriResolver
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    context.setSourceFactory(sourceFactory);
    // add context
    contextMap.put(contextId, context);
    schedulePerformAnalysisOperation(contextId, false);
  }

  /**
   * Sends one of the {@link NotificationKind}s.
   */
  public void internalDartUnitNotification(String contextId, Source source, NotificationKind kind,
      CompilationUnit unit) throws Exception {
    switch (kind) {
      case ERRORS:
        // TODO(scheglov)
        break;
      case HIGHLIGHT:
        // TODO(scheglov)
        break;
      case NAVIGATION:
        listener.computedNavigation(
            contextId,
            source,
            new DartUnitNavigationComputer(unit).compute());
        break;
      case OUTLINE:
        // TODO(scheglov)
        break;
    }
  }

  /**
   * Implementation for {@link #deleteContext(String)}.
   */
  public void internalDeleteContext(String contextId) throws Exception {
    AnalysisContext context = contextMap.remove(contextId);
    notificationMap.remove(contextId);
    if (context == null) {
      onServerError(AnalysisServerErrorCode.INVALID_CONTEXT_ID, contextId);
      return;
    }
    operationQueue.removeWithContextId(contextId);
  }

  /**
   * Performs analysis in the given {@link AnalysisContext}.
   */
  public void internalPerformAnalysis(String contextId) throws Exception {
    if (test_analyzedContexts != null) {
      test_analyzedContexts.add(contextId);
    }
    AnalysisContext context = getAnalysisContext(contextId);
    AnalysisResult result = context.performAnalysisTask();
    ChangeNotice[] notices = result.getChangeNotices();
    if (notices == null) {
      return;
    }
    // schedule analysis again
    schedulePerformAnalysisOperation(contextId, true);
    // send notifications
    Map<NotificationKind, Set<Source>> notifications = notificationMap.get(contextId);
    for (ChangeNotice changeNotice : notices) {
      Source source = changeNotice.getSource();
      // notify about errors
      listener.computedErrors(contextId, source, changeNotice.getErrors());
      // schedule computable notifications
      if (notifications != null) {
        for (Entry<NotificationKind, Set<Source>> entry : notifications.entrySet()) {
          NotificationKind notificationKind = entry.getKey();
          Set<Source> notificationSources = entry.getValue();
          if (notificationSources.contains(source)) {
            CompilationUnit dartUnit = changeNotice.getCompilationUnit();
            if (dartUnit != null) {
              operationQueue.add(new DartUnitNotificationOperation(
                  contextId,
                  source,
                  notificationKind,
                  dartUnit));
            }
          }
        }
      }
    }
  }

  /**
   * Implementation for {@link #setNotificationSources(String, NotificationKind, Source[])}.
   */
  public void internalSetNotificationSources(String contextId, NotificationKind kind,
      Source[] sources) throws Exception {
    AnalysisContext analysisContext = getAnalysisContext(contextId);
    Map<NotificationKind, Set<Source>> notifications = notificationMap.get(contextId);
    if (notifications == null) {
      notifications = Maps.newHashMap();
      notificationMap.put(contextId, notifications);
    }
    // prepare new sources to send notifications for
    Set<Source> oldSourceSet = notifications.get(kind);
    Set<Source> newSourceSet = ImmutableSet.copyOf(sources);
    Set<Source> newSources;
    if (oldSourceSet == null) {
      newSources = newSourceSet;
    } else {
      newSources = Sets.difference(newSourceSet, oldSourceSet);
    }
    // ...schedule notification operations for these new sources
    for (Source unitSource : newSources) {
      Source[] librarySources = analysisContext.getLibrariesContaining(unitSource);
      if (librarySources.length != 0) {
        Source librarySource = librarySources[0];
        CompilationUnit unit = analysisContext.resolveCompilationUnit(unitSource, librarySource);
        if (unit != null) {
          operationQueue.add(new DartUnitNotificationOperation(contextId, unitSource, kind, unit));
        }
      }
    }
    notifications.put(kind, newSourceSet);
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

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.removeListener(listener);
  }

  @Override
  public void setNotificationSources(String contextId, NotificationKind kind, Source[] sources) {
    operationQueue.add(new SetNotificationSourcesOperation(contextId, kind, sources));
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
  }

  @Override
  public void subscribe(String contextId, Map<NotificationKind, SourceSet> subscriptions) {
    // TODO(scheglov) implement it
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

  private DartSdk getSdk(String directory) {
    DartSdk sdk = sdkMap.get(directory);
    if (sdk == null) {
      sdk = new DirectoryBasedDartSdk(new File(directory));
      sdkMap.put(directory, sdk);
    }
    return sdk;
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

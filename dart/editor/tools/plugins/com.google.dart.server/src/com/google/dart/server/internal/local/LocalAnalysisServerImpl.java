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
import com.google.dart.engine.AnalysisEngine;
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

import java.io.File;
import java.util.Map;
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
        if (test_paused) {
          Thread.yield();
          continue;
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
        test_queueIsEmptyAfterPerformOperation = operationQueue.isEmpty();
      }
    }
  }

  private static final String VERSION = "0.0.1";

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
   * This is used only for testing purposes and allows tests to wait until all operations are
   * executed.
   */
  private boolean test_queueIsEmptyAfterPerformOperation;

  /**
   * The unique ID for the next context.
   */
  private final AtomicInteger nextId = new AtomicInteger();

  /**
   * A table mapping context id's to the analysis contexts associated with them.
   */
  private final Map<String, AnalysisContext> contextMap = Maps.newHashMap();

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
  public void internalApplyChanges(String contextId, ChangeSet changeSet) {
    AnalysisContext context = getAnalysisContext(contextId);
    context.applyChanges(changeSet);
    schedulePerformAnalysisOperation(contextId);
  }

  /**
   * Implementation for {@link #createContext(String, String, Map)}.
   */
  public void internalCreateContext(String contextId, String sdkDirectory,
      Map<String, String> packageMap) {
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    DartSdk sdk = new DirectoryBasedDartSdk(new File(sdkDirectory));
    // TODO(scheglov) PackageUriResolver
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    context.setSourceFactory(sourceFactory);
    // add context
    contextMap.put(contextId, context);
    schedulePerformAnalysisOperation(contextId);
  }

  /**
   * Implementation for {@link #deleteContext(String)}.
   */
  public void internalDeleteContext(String contextId) {
    AnalysisContext context = contextMap.remove(contextId);
    if (context == null) {
      onServerError(AnalysisServerErrorCode.INVALID_CONTEXT_ID, contextId);
      return;
    }
    operationQueue.removeWithContextId(contextId);
  }

  /**
   * Performs analysis in the given {@link AnalysisContext}.
   */
  public void internalPerformAnalysis(String contextId) {
    while (true) {
      AnalysisContext context = getAnalysisContext(contextId);
      AnalysisResult result = context.performAnalysisTask();
      ChangeNotice[] notices = result.getChangeNotices();
      if (notices == null) {
        return;
      }
      // schedule analysis again
      schedulePerformAnalysisOperation(contextId);
      // notify about errors
      for (ChangeNotice changeNotice : notices) {
        listener.computedErrors(contextId, changeNotice.getSource(), changeNotice.getErrors());
      }
    }
  }

  /**
   * Implementation for {@link #setContents(String, Source, String)}.
   */
  public void internalSetContents(String contextId, Source source, String contents) {
    AnalysisContext context = getAnalysisContext(contextId);
    context.setContents(source, contents);
    schedulePerformAnalysisOperation(contextId);
  }

  /**
   * Implementation for {@link #setOptions(String, AnalysisOptions)}.
   */
  public void internalSetOptions(String contextId, AnalysisOptions options) {
    AnalysisContext context = getAnalysisContext(contextId);
    context.setAnalysisOptions(options);
    schedulePerformAnalysisOperation(contextId);
  }

  /**
   * Implementation for {@link #setPrioritySources(String, Source[])}.
   */
  public void internalSetPrioritySources(String contextId, Source[] sources) {
    AnalysisContext context = getAnalysisContext(contextId);
    context.setAnalysisPriorityOrder(Lists.newArrayList(sources));
    schedulePerformAnalysisOperation(contextId);
  }

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    this.listener.removeListener(listener);
  }

  @Override
  public void setContents(String contextId, Source source, String contents) {
    operationQueue.add(new SetContentsOperation(contextId, source, contents));
  }

  @Override
  public void setOptions(String contextId, AnalysisOptions options) {
    operationQueue.add(new SetOptionsOperation(contextId, options));
  }

  @Override
  public void setPrioritySources(String contextId, Source[] sources) {
    operationQueue.add(new SetPrioritySourcesOperation(contextId, sources));
  }

  @Override
  public void shutdown() {
    operationQueue.add(ShutdownOperation.INSTANCE);
  }

  @VisibleForTesting
  public void test_addOperation(ServerOperation operation) {
    operationQueue.add(operation);
  }

  @VisibleForTesting
  public void test_pingListeners() {
    listener.computedErrors(null, null, null);
  }

  @VisibleForTesting
  public void test_setPaused(boolean paused) {
    this.test_paused = paused;
  }

  @VisibleForTesting
  public void test_waitForWorkerComplete() {
    while (!operationQueue.isEmpty() || !test_queueIsEmptyAfterPerformOperation) {
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
   */
  private void schedulePerformAnalysisOperation(String contextId) {
    operationQueue.add(new PerformAnalysisOperation(contextId));
  }
}

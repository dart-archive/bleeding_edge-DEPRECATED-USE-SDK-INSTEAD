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
import com.google.common.util.concurrent.Uninterruptibles;
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
import com.google.dart.server.AnalysisServerListener;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process implementation of {@link AnalysisServer}.
 * 
 * @coverage dart.server.local
 */
public class LocalAnalysisServerImpl implements AnalysisServer {
  /**
   * Information about the {@link AnalysisContext} and its ID.
   */
  private static class AnalysisContextInfo {
    final String id;
    final AnalysisContext context;

    public AnalysisContextInfo(String id, AnalysisContext context) {
      this.id = id;
      this.context = context;
    }
  }

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
        // prepare context
        AnalysisContextInfo contextInfo;
        synchronized (contextWorkQueue) {
          while (contextWorkQueue.isEmpty()) {
            try {
              contextWorkQueue.wait();
            } catch (InterruptedException e) {
            }
          }
          contextInfo = contextWorkQueue.getFirst();
        }
        // maybe paused
        if (test_paused) {
          Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);
          continue;
        }
        // stop signal
        if (contextInfo == SHUTDOWN_OBJECT) {
          break;
        }
        // perform analysis
        performAnalysis(contextInfo);
        synchronized (contextWorkQueue) {
          contextWorkQueue.removeFirst();
          contextWorkQueue.notifyAll();
        }
      }
    }

    /**
     * Performs analysis in the given {@link AnalysisContext}.
     */
    private void performAnalysis(AnalysisContextInfo contextInfo) {
      while (true) {
        AnalysisResult result = contextInfo.context.performAnalysisTask();
        ChangeNotice[] notices = result.getChangeNotices();
        if (notices == null) {
          return;
        }
        // notify about errors
        List<AnalysisServerListener> listenersCopy = Lists.newArrayList(listeners);
        for (ChangeNotice changeNotice : notices) {
          for (AnalysisServerListener listener : listenersCopy) {
            listener.computedErrors(
                contextInfo.id,
                changeNotice.getSource(),
                changeNotice.getErrors());
          }
        }
      }
    }
  }

  private static final String VERSION = "0.0.1";

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
   * This object is used as a signal that the worker thread should stop.
   */
  private static final AnalysisContextInfo SHUTDOWN_OBJECT = new AnalysisContextInfo(null, null);

  /**
   * The unique ID for the next context.
   */
  private final AtomicInteger nextId = new AtomicInteger();

  /**
   * A table mapping context id's to the analysis contexts associated with them.
   */
  private final Map<String, AnalysisContext> contextMap = Maps.newHashMap();

  /**
   * A queue of the analysis contexts for which analysis work needs to be performed.
   */
  private final LinkedList<AnalysisContextInfo> contextWorkQueue = Lists.newLinkedList();

  /**
   * The listeners that will receive notification when new analysis results become available.
   */
  private final List<AnalysisServerListener> listeners = Lists.newArrayList();

  public LocalAnalysisServerImpl() {
    thread = new LocalAnalysisServerThread();
    thread.start();
  }

  @Override
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    if (listeners.contains(listener)) {
      return;
    }
    listeners.add(listener);
  }

  @Override
  public void applyChanges(String contextId, ChangeSet changeSet) {
    AnalysisContext context = getAnalysisContext(contextId);
    if (context != null) {
      context.applyChanges(changeSet);
      addContextToWorkQueue(contextId, context);
    }
  }

  @Override
  public String createContext(String name, String sdkDirectory, Map<String, String> packageMap) {
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    DartSdk sdk = new DirectoryBasedDartSdk(new File(sdkDirectory));
    // TODO(scheglov) PackageUriResolver
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    context.setSourceFactory(sourceFactory);
    // add context
    String contextId = name + "-" + nextId.getAndIncrement();
    synchronized (contextMap) {
      contextMap.put(contextId, context);
    }
    addContextToWorkQueue(contextId, context);
    return contextId;
  }

  @Override
  public void deleteContext(String contextId) {
    // prepare context
    AnalysisContext context;
    synchronized (contextMap) {
      context = contextMap.remove(contextId);
    }
    if (context == null) {
      return;
    }
    // remove the context from the work queue
    synchronized (contextWorkQueue) {
      for (Iterator<AnalysisContextInfo> iter = contextWorkQueue.iterator(); iter.hasNext();) {
        AnalysisContextInfo contextInfo = iter.next();
        if (contextInfo.id.equals(contextId)) {
          iter.remove();
        }
      }
    }
  }

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void setContents(String contextId, Source source, String contents) {
    AnalysisContext context = getAnalysisContext(contextId);
    if (context != null) {
      context.setContents(source, contents);
      addContextToWorkQueue(contextId, context);
    }
  }

  @Override
  public void setOptions(String contextId, AnalysisOptions options) {
    AnalysisContext context = getAnalysisContext(contextId);
    if (context != null) {
      context.setAnalysisOptions(options);
      addContextToWorkQueue(contextId, context);
    }
  }

  @Override
  public void setPrioritySources(String contextId, Source[] sources) {
    AnalysisContext context = getAnalysisContext(contextId);
    if (context != null) {
      context.setAnalysisPriorityOrder(Lists.newArrayList(sources));
      addContextToWorkQueue(contextId, context);
    }
  }

  @Override
  public void shutdown() {
    synchronized (contextWorkQueue) {
      contextWorkQueue.addFirst(SHUTDOWN_OBJECT);
      contextWorkQueue.notify();
    }
  }

  @VisibleForTesting
  public void test_pingListeners() {
    for (AnalysisServerListener listener : listeners) {
      listener.computedErrors(null, null, null);
    }
  }

  @VisibleForTesting
  public void test_setPaused(boolean paused) {
    this.test_paused = paused;
  }

  @VisibleForTesting
  public void test_waitForWorkerComplete() {
    synchronized (contextWorkQueue) {
      while (!contextWorkQueue.isEmpty()) {
        try {
          contextWorkQueue.wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  @Override
  public String version() {
    return VERSION;
  }

  /**
   * Add the given {@link AnalysisContext} to the {@link #contextWorkQueue}.
   */
  private void addContextToWorkQueue(String contextId, AnalysisContext context) {
    AnalysisContextInfo contextInfo = new AnalysisContextInfo(contextId, context);
    synchronized (contextWorkQueue) {
      contextWorkQueue.addLast(contextInfo);
      contextWorkQueue.notify();
    }
  }

  /**
   * Returns the {@link AnalysisContext} for the given identifier, maybe {@code null}.
   */
  private AnalysisContext getAnalysisContext(String contextId) {
    synchronized (contextMap) {
      return contextMap.get(contextId);
    }
  }
}

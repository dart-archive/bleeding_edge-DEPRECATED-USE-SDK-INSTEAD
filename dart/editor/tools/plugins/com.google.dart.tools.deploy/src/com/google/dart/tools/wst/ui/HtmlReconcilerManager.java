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
package com.google.dart.tools.wst.ui;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.builder.AnalysisManager;

import org.eclipse.jface.text.IDocument;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HtmlReconcilerManager {
  private static class UpdateWorker {
    private final Project project;
    private final AnalysisContext context;
    private final Source source;
    private String content;
    private long contentTime;

    public UpdateWorker(Project project, AnalysisContext context, Source source, String content) {
      this.project = project;
      this.context = context;
      this.source = source;
      setContent(content);
    }

    public void setContent(String content) {
      this.content = content;
      this.contentTime = System.currentTimeMillis();
    }

    boolean isUpdatedMoreThan(long msAgo) {
      return System.currentTimeMillis() - contentTime > msAgo;
    }

    void perform() {
      context.setContents(source, content);
      AnalysisManager.getInstance().performAnalysisInBackground(project, context);
    }
  }

  private static class UpdateWorkerThread extends Thread {
    public UpdateWorkerThread() {
      setName("UpdateWorkerThread");
    }

    @Override
    public void run() {
      while (true) {
        Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        // prepare worker
        UpdateWorker worker = null;
        synchronized (updateWorkerQueue) {
          if (!updateWorkerQueue.isEmpty()) {
            if (updateWorkerQueue.getFirst().isUpdatedMoreThan(250)) {
              worker = updateWorkerQueue.removeFirst();
            }
          }
        }
        // run worker
        if (worker != null) {
          worker.perform();
        }
      }
    }
  }

  private static final HtmlReconcilerManager INSTANCE = new HtmlReconcilerManager();
  private static final LinkedList<UpdateWorker> updateWorkerQueue = Lists.newLinkedList();

  /**
   * Retrieve the reconciler cache manager.
   * 
   * @return The singleton DartReconcilerManager
   */
  public static HtmlReconcilerManager getInstance() {
    return INSTANCE;
  }

  /**
   * Schedules update of the given {@link Source} in background and subsequent analysis.
   */
  public static void performUpdateInBackground(Project project, AnalysisContext context,
      Source source, String content) {
    synchronized (updateWorkerQueue) {
      // if the same Source, update worker
      if (!updateWorkerQueue.isEmpty()) {
        UpdateWorker lastWorker = updateWorkerQueue.getLast();
        if (lastWorker.project == project && lastWorker.context == context
            && Objects.equal(lastWorker.source, source)) {
          lastWorker.setContent(content);
          return;
        }
      }
      // new Source - new worker
      updateWorkerQueue.add(new UpdateWorker(project, context, source, content));
    }
  }

  private final Map<IDocument, HtmlReconcilerHook> reconcilers = Maps.newHashMap();

  private HtmlReconcilerManager() {
    // This is a singleton.
    new UpdateWorkerThread().start();
  }

  /**
   * Retrieve the reconciler used for the given <code>document</code>.
   * 
   * @param document The IDocument being edited
   * @return The HtmlReconcilerHook used for reconciling
   */
  public HtmlReconcilerHook reconcilerFor(IDocument document) {
    synchronized (reconcilers) {
      HtmlReconcilerHook rec = reconcilers.get(document);
      if (rec == null) {
        rec = new HtmlReconcilerHook();
        rec.connect(document);
        reconcilers.put(document, rec);
      }
      return rec;
    }
  }

  /**
   * Set the reconciler to be used for the given <code>document</code> to <code>reconciler</code>.
   * 
   * @param document The IDocument to reconcile
   * @param reconciler The EmbeddedDartReconcilerHook that does the reconciling
   */
  public void reconcileWith(IDocument document, HtmlReconcilerHook reconciler) {
    synchronized (reconcilers) {
      if (reconciler != null) {
        HtmlReconcilerHook rec = reconcilers.get(document);
        if (rec == reconciler) {
          return;
        }
        if (rec != null) {
          rec.disconnect(document);
        }
      }
      reconcilers.put(document, reconciler);
    }
  }
}

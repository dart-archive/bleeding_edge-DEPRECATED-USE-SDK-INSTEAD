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

package com.google.dart.tools.ui.internal.text.dart;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Helper for updating the order in which sources are analyzed in contexts associated with editors.
 * This is called once per instantiated editor on startup and then once for each editor as it
 * becomes active. For example, if there are 2 of 7 editors visible on startup, then this will be
 * called for the 2 visible editors.
 * 
 * @coverage dart.editor.ui.text
 */
public class DartPrioritySourcesHelper {
  private static class PriorityOrderRequest {
    final AnalysisContext context;
    final List<Source> sources;

    public PriorityOrderRequest(AnalysisContext context, List<Source> sources) {
      this.context = context;
      this.sources = sources;
    }

    public void perform() {
      context.setAnalysisPriorityOrder(sources);
    }
  }

  private class PriorityOrderThread extends Thread {
    public PriorityOrderThread() {
      setName("DartPrioritySourcesHelper-PriorityOrderThread");
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        try {
          PriorityOrderRequest request = requestQueue.take();
          if (request == SHUTDOWN_REQUEST) {
            return;
          }
          request.perform();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  private static final PriorityOrderRequest SHUTDOWN_REQUEST = new PriorityOrderRequest(null, null);

  private final IWorkbench workbench;
  private final DartIgnoreManager ignoreManager;
  private final BlockingQueue<PriorityOrderRequest> requestQueue = new LinkedBlockingQueue<PriorityOrderRequest>();

  public DartPrioritySourcesHelper(IWorkbench workbench) {
    this(workbench, DartCore.getIgnoreManager());
  }

  public DartPrioritySourcesHelper(IWorkbench workbench, DartIgnoreManager ignoreManager) {
    this.workbench = workbench;
    this.ignoreManager = ignoreManager;
  }

  /**
   * Schedules helper start, once {@link IWorkbenchPage} is created.
   */
  public void start() {
    workbench.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window != null) {
          IWorkbenchPage page = window.getActivePage();
          if (page != null) {
            internalStart(page);
          }
        }
      }
    });
    new PriorityOrderThread().start();
  }

  public void stop() {
    requestQueue.offer(SHUTDOWN_REQUEST);
  }

  public void test_waitForQueueEmpty() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    requestQueue.add(new PriorityOrderRequest(null, null) {
      @Override
      public void perform() {
        latch.countDown();
      }
    });
    latch.await();
  }

  /**
   * @return the {@link DartPriorityFileEditor} that corresponds to the given {@link IWorkbenchPart}
   *         , maybe {@code null}.
   */
  private DartPriorityFileEditor getPrioritySourceEditor(IWorkbenchPart part) {
    if (part != null) {
      Object maybeEditor = part.getAdapter(DartPriorityFileEditor.class);
      if (maybeEditor instanceof DartPriorityFileEditor) {
        return (DartPriorityFileEditor) maybeEditor;
      }
    }
    return null;
  }

  /**
   * Answer the visible {@link DartPriorityFileEditor}s.
   * 
   * @param context the context (not {@code null})
   * @return a list of sources (not {@code null}, contains no {@code null}s)
   */
  private List<DartPriorityFileEditor> getVisibleEditors() {
    List<DartPriorityFileEditor> editors = Lists.newArrayList();;
    for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
      for (IWorkbenchPage page : window.getPages()) {
        for (IEditorReference editorRef : page.getEditorReferences()) {
          IEditorPart part = editorRef.getEditor(false);
          DartPriorityFileEditor editor = getPrioritySourceEditor(part);
          if (editor != null) {
            if (editor.isVisible()) {
              editors.add(editor);
            }
          }
        }
      }
    }
    return editors;
  }

  /**
   * Answer the visible editors displaying source for the given context. This must be called on the
   * UI thread because it accesses windows, pages, and editors.
   * 
   * @param context the context (not {@code null})
   * @return a list of sources (not {@code null}, contains no {@code null}s)
   */
  private List<Source> getVisibleSourcesForContext(AnalysisContext context) {
    List<Source> sources = Lists.newArrayList();
    List<DartPriorityFileEditor> editors = getVisibleEditors();
    for (DartPriorityFileEditor editor : editors) {
      if (editor.getInputAnalysisContext() == context) {
        Source source = editor.getInputSource();
        if (source != null && ignoreManager.isAnalyzed(source.getFullName())) {
          sources.add(source);
        }
      }
    }
    return sources;
  }

  private void handlePartActivated(IWorkbenchPart part) {
    DartPriorityFileEditor editor = getPrioritySourceEditor(part);
    if (editor != null) {
      updateAnalysisPriorityOrderOnUiThread(editor, true);
    }
  }

  private void handlePartDeactivated(IWorkbenchPart part) {
    DartPriorityFileEditor editor = getPrioritySourceEditor(part);
    if (editor != null) {
      updateAnalysisPriorityOrderOnUiThread(editor, false);
    }
  }

  /**
   * Starts listening for {@link IWorkbenchPage} and adding/removing sources of the visible editors.
   */
  private void internalStart(IWorkbenchPage activePage) {
    // make source of the currently visible editors a priority ones
    // but exclude those sources that are marked as do-not-analyze
    {
      Map<AnalysisContext, List<Source>> contextMap = Maps.newHashMap();
      List<DartPriorityFileEditor> editors = getVisibleEditors();
      for (DartPriorityFileEditor editor : editors) {
        AnalysisContext context = editor.getInputAnalysisContext();
        if (context != null && !contextMap.containsKey(context)) {
          List<Source> sources = getVisibleSourcesForContext(context);
          contextMap.put(context, sources);
        }
      }
      // schedule priority sources setting
      for (Entry<AnalysisContext, List<Source>> entry : contextMap.entrySet()) {
        AnalysisContext context = entry.getKey();
        List<Source> prioritySources = entry.getValue();
        updateAnalysisPriorityOrderInBackground(context, prioritySources);
      }
    }
    // track visible editors
    activePage.addPartListener(new IPartListener2() {
      @Override
      public void partActivated(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partBroughtToTop(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partClosed(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partDeactivated(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partHidden(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part != null) {
          handlePartDeactivated(part);
        }
      }

      @Override
      public void partInputChanged(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partOpened(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partVisible(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part != null) {
          handlePartActivated(part);
        }
      }
    });
  }

  /**
   * Schedules {@link AnalysisContext#setAnalysisPriorityOrder(List)} execution in background.
   */
  private void updateAnalysisPriorityOrderInBackground(AnalysisContext context, List<Source> sources) {
    try {
      requestQueue.add(new PriorityOrderRequest(context, sources));
    } catch (IllegalStateException e) {
      // Should never happen, "requestQueue" has a very high capacity.
    }
  }

  /**
   * Update the order in which sources are analyzed in the context associated with the editor. This
   * is called once per instantiated editor on startup and then once for each editor as it becomes
   * active. For example, if there are 2 of 7 editors visible on startup, then this will be called
   * for the 2 visible editors.
   * <p>
   * MUST be called on the UI thread.
   * 
   * @param isOpen {@code true} if the editor is open and the source should be the first source
   *          analyzed or {@code false} if the editor is closed and the source should be removed
   *          from the priority list.
   */
  private void updateAnalysisPriorityOrderOnUiThread(DartPriorityFileEditor editor, boolean isOpen) {
    AnalysisContext context = editor.getInputAnalysisContext();
    Source source = editor.getInputSource();
    if (context != null && source != null) {
      List<Source> sources = getVisibleSourcesForContext(context);
      sources.remove(source);
      if (isOpen) {
        if (ignoreManager.isAnalyzed(source.getFullName())) {
          sources.add(0, source);
        }
      }
      updateAnalysisPriorityOrderInBackground(context, sources);
    }
  }
}

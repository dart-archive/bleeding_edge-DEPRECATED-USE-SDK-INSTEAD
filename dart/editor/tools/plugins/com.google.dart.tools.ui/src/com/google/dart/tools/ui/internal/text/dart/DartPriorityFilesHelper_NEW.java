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
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.generated.types.AnalysisService;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import java.util.concurrent.TimeUnit;

/**
 * Helper for updating the order in which files are analyzed in contexts associated with editors.
 * This is called once per instantiated editor on startup and then once for each editor as it
 * becomes active. For example, if there are 2 of 7 editors visible on startup, then this will be
 * called for the 2 visible editors.
 * 
 * @coverage dart.editor.ui.text
 */
public class DartPriorityFilesHelper_NEW {
  private final IWorkbench workbench;
  private final AnalysisServer analysisServer;
  private final Object lock = new Object();
  private final Map<String, List<String>> subscriptions = Maps.newHashMap();
  private final List<String> visibleFiles = Lists.newArrayList();
  private List<String> visibleFilesSent = Lists.newArrayList();
  private String activeFile = null;

  private final Job sendToServerJob = new Job("Send visible files subscriptions") {
    @Override
    protected IStatus run(IProgressMonitor monitor) {
      synchronized (lock) {
        // send priority files, if changed
        if (!visibleFiles.equals(visibleFilesSent)) {
          analysisServer.analysis_setPriorityFiles(visibleFiles);
          visibleFilesSent = Lists.newArrayList(visibleFiles);
        }
        // update active file subscriptions
        {
          List<String> activeFileList = Lists.newArrayList();
          if (activeFile != null) {
            activeFileList.add(activeFile);
          }
          // update subscriptions
          subscriptions.put(AnalysisService.NAVIGATION, activeFileList);
          subscriptions.put(AnalysisService.OCCURRENCES, activeFileList);
          subscriptions.put(AnalysisService.OUTLINE, activeFileList);
        }
        // update visible file subscriptions
        subscriptions.put(AnalysisService.HIGHLIGHTS, visibleFiles);
        subscriptions.put(AnalysisService.OVERRIDES, visibleFiles);
        analysisServer.analysis_setSubscriptions(subscriptions);
        // done
        test_hasPendingJob = false;
      }
      return Status.OK_STATUS;
    }
  };

  private boolean test_hasPendingJob = false;

  public DartPriorityFilesHelper_NEW(IWorkbench workbench, AnalysisServer analysisServer) {
    this.workbench = workbench;
    this.analysisServer = analysisServer;
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
  }

  /**
   * Waits until a scheduled background job for sending information to the server finishes, so that
   * test can check correctness of the information.
   */
  public void test_waitWhileHasPendingJob() {
    while (true) {
      synchronized (lock) {
        if (!test_hasPendingJob) {
          return;
        }
      }
      Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * @return the {@link DartPriorityFileEditor} that corresponds to the given {@link IWorkbenchPart}
   *         , maybe {@code null}.
   */
  private DartPriorityFileEditor getPriorityFileEditor(IWorkbenchPart part) {
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
   */
  private List<DartPriorityFileEditor> getVisibleEditors() {
    List<DartPriorityFileEditor> editors = Lists.newArrayList();;
    for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
      for (IWorkbenchPage page : window.getPages()) {
        for (IEditorReference editorRef : page.getEditorReferences()) {
          IEditorPart part = editorRef.getEditor(false);
          DartPriorityFileEditor editor = getPriorityFileEditor(part);
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

  private void handlePartActivated(IWorkbenchPart part, boolean scheduleJob) {
    DartPriorityFileEditor editor = getPriorityFileEditor(part);
    String file = editor != null ? editor.getInputFilePath() : null;
    synchronized (lock) {
      activeFile = file;
      if (scheduleJob) {
        test_hasPendingJob = true;
        sendToServerJob.schedule();
      }
    }
  }

  /**
   * Starts listening for {@link IWorkbenchPage} and adding/removing files of the visible editors.
   */
  private void internalStart(IWorkbenchPage activePage) {
    // subscribe for currently active part
    {
      IWorkbenchPart activePart = activePage.getActivePart();
      handlePartActivated(activePart, false);
    }
    // make files of the currently visible editors a priority ones
    prepareVisibleFiles();
    // schedule job to send an initial state
    test_hasPendingJob = true;
    sendToServerJob.schedule();
    // track visible editors
    activePage.addPartListener(new IPartListener2() {
      @Override
      public void partActivated(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        handlePartActivated(part, true);
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
        if (isDartPriorityFileEditor(partRef)) {
          scheduleSubscriptionsUpdate();
        }
      }

      @Override
      public void partInputChanged(IWorkbenchPartReference partRef) {
        if (isDartPriorityFileEditor(partRef)) {
          scheduleSubscriptionsUpdate();
        }
      }

      @Override
      public void partOpened(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partVisible(IWorkbenchPartReference partRef) {
        if (isDartPriorityFileEditor(partRef)) {
          scheduleSubscriptionsUpdate();
        }
      }
    });
  }

  private boolean isDartPriorityFileEditor(IWorkbenchPartReference partRef) {
    if (partRef == null) {
      return false;
    }
    // get part
    IWorkbenchPart part = partRef.getPart(false);
    if (part == null) {
      return false;
    }
    // get editor
    DartPriorityFileEditor editor = getPriorityFileEditor(part);
    return editor != null;
  }

  /**
   * Fills {@link #visibleFiles} with visible files.
   */
  private void prepareVisibleFiles() {
    visibleFiles.clear();
    List<DartPriorityFileEditor> editors = getVisibleEditors();
    for (DartPriorityFileEditor editor : editors) {
      String file = editor.getInputFilePath();
      if (file != null) {
        visibleFiles.add(file);
      }
    }
  }

  /**
   * Prepares the list of visible files and a schedules subscription update.
   */
  private void scheduleSubscriptionsUpdate() {
    synchronized (lock) {
      prepareVisibleFiles();
      test_hasPendingJob = true;
      sendToServerJob.schedule(25);
    }
  }
}

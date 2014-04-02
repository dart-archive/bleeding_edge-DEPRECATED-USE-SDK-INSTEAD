/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.instrumentation;

import com.google.dart.engine.utilities.instrumentation.HealthUtils;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.ui.feedback.FeedbackUtils;
import com.google.dart.tools.ui.feedback.FeedbackUtils.Stats;
import com.google.dart.tools.ui.instrumentation.util.Base64;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * {@code Heartbeat} provides utility methods that an external instrumentation plugin can call to
 * get periodic information about the health of the development environment.
 */
public class Heartbeat {

  private static final Heartbeat INSTANCE = new Heartbeat();

  public static Heartbeat getInstance() {
    return INSTANCE;
  }

  /**
   * Log the information that is also reported in the feedback dialog
   */
  private static void logFeedbackInfo(InstrumentationBuilder instrumentation) {

    instrumentation.metric("FB-VersionDetails", FeedbackUtils.getEditorVersionDetails());
    instrumentation.metric("FB-OSName", FeedbackUtils.getOSName());

    Stats stats = FeedbackUtils.getStats();
    instrumentation.metric("FB-Stats-autoRunPubEnabled", stats.autoRunPubEnabled);
    instrumentation.metric("FB-Stats-numEditors", stats.numEditors);
    instrumentation.metric("FB-Stats-numProjects", stats.numProjects);
    instrumentation.metric("FB-Stats-numThreads", stats.numThreads);
    instrumentation.metric("FB-Stats-indexStats", "index: " + stats.indexStats);

  }

  /**
   * This method logs information about the health of the development environment. It is imperative
   * that this method execute quickly as this is typically called once per minute.
   * 
   * @param instrumentation the instrumentation used to log information (not {@code null})
   */
  public void heartbeat(InstrumentationBuilder instrumentation) {

    logFeedbackInfo(instrumentation);

    HealthUtils.logMemory(instrumentation);
    HealthUtils.logThreads(instrumentation);

    logWindowsPagesAndTabs(instrumentation);
  }

  private void logWindowsPagesAndTabs(InstrumentationBuilder instrumentation) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
    IWorkbenchWindow[] allWindows = workbench.getWorkbenchWindows();
    instrumentation.metric("OpenWindowsCount", allWindows.length);

    for (int windowIndex = 0; windowIndex < allWindows.length; windowIndex++) {
      IWorkbenchWindow window = allWindows[windowIndex];
      if (window == activeWindow) {
        instrumentation.metric("ActiveWindow", windowIndex);
      }
      String windowKey = "Window-" + windowIndex;

      IWorkbenchPage activePage = window.getActivePage();
      IWorkbenchPage[] allPages = window.getPages();
      instrumentation.metric(windowKey + "-OpenPageCount", allPages.length);

      for (int pageIndex = 0; pageIndex < allPages.length; pageIndex++) {
        IWorkbenchPage page = allPages[pageIndex];
        if (page == activePage) {
          instrumentation.metric(windowKey + "-ActivePage", pageIndex);
        }
        String pageKey = windowKey + "-Page-" + pageIndex;

        IWorkbenchPart activePart = page.getActivePart();
        IViewReference[] allViews = page.getViewReferences();
        IEditorReference[] allEditors = page.getEditorReferences();
        instrumentation.metric(pageKey + "-OpenViewCount", allViews.length);

        for (int viewIndex = 0; viewIndex < allViews.length; viewIndex++) {
          IViewReference view = allViews[viewIndex];
          if (view == activePart) {
            instrumentation.metric(pageKey + "-ActiveView", viewIndex);
          }
          String viewKey = pageKey + "-View-" + viewIndex;

          instrumentation.metric(viewKey + "-Id", view.getId());
        }

        for (int editorIndex = 0; editorIndex < allEditors.length; editorIndex++) {
          IEditorReference editor = allEditors[editorIndex];
          if (editor == activePart) {
            instrumentation.metric(pageKey + "-ActiveEditorTab", editorIndex);
          }
          String editorKey = pageKey + "-Editor-" + editorIndex;

          instrumentation.metric(editorKey + "-Id", editor.getId());
          instrumentation.metric(editorKey + "-Dirty", editor.isDirty());
          instrumentation.data(editorKey + "-Name", editor.getTitle());

          InstrumentationBuilder srcInstr = Instrumentation.builder("Editor-src-HB");
          try {

            IEditorPart part = editor.getEditor(false);

            srcInstr.metric(editorKey + "-Id", editor.getId());
            srcInstr.metric(editorKey + "-Dirty", editor.isDirty());
            srcInstr.data(editorKey + "-Name", editor.getTitle());

            if (part instanceof ITextEditor) {
              ITextEditor textEditor = (ITextEditor) part;
              IDocumentProvider provider = textEditor.getDocumentProvider();
              if (provider != null) {
                IDocument document = provider.getDocument(textEditor.getEditorInput());
                if (document != null) {
                  String docSrc = document.get();

                  //TODO(lukechurch): Add a Java+Python compatible compressor here
                  String docSrcb64 = Base64.encodeBytes(docSrc.getBytes());

                  srcInstr.data(editorKey + "-src", docSrcb64);

                }
              }
            }

          } catch (Exception e) {
            srcInstr.record(e);

          } finally {
            srcInstr.log();
          }
        }
      }
    }
  }
}

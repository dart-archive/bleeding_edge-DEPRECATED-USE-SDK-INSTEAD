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
package com.google.dart.tools.internal.search.ui;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.Element;
import com.google.dart.server.SearchIdConsumer;
import com.google.dart.server.SearchResult;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.SearchResultsListener;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction;
import com.google.dart.tools.ui.actions.OpenAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Finds references of the selected entity in the workspace.
 * 
 * @coverage dart.editor.ui.search
 */
public class FindReferencesAction_NEW extends AbstractDartSelectionAction {
  /**
   * Shows "Search" view with references to non-local elements with given name.
   */
  public static void searchNameUses(final String name) {
    try {
      SearchView view = (SearchView) DartToolsPlugin.showView(SearchView.ID);
      view.showPage(new SearchResultPage_NEW(view, "Searching for references...") {
        @Override
        protected boolean canUseFilterPotential() {
          return false;
        }

        @Override
        protected IProject getCurrentProject() {
          return findCurrentProject();
        }

        @Override
        protected String getQueryElementName() {
          return name;
        }

        @Override
        protected String getQueryKindName() {
          return "references";
        }

        @Override
        protected List<SearchResult> runQuery() {
          final List<SearchResult> allResults = Lists.newArrayList();
          final CountDownLatch latch = new CountDownLatch(1);
          DartCore.getAnalysisServer().searchClassMemberReferences(name, new SearchIdConsumer() {
            @Override
            public void computedSearchId(String searchId) {
              DartCore.getAnalysisServerData().addSearchResultsListener(
                  searchId,
                  new SearchResultsListener() {
                    @Override
                    public void computedSearchResults(SearchResult[] results, boolean last) {
                      Collections.addAll(allResults, results);
                      if (last) {
                        latch.countDown();
                      }
                    }
                  });
            }
          });
          Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.MINUTES);
          return allResults;
        }
      });
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Find references", "Exception during search.");
    }
  }

  /**
   * Finds the "current" project. That is the project of the active editor.
   */
  static IProject findCurrentProject() {
    IEditorPart editor = DartToolsPlugin.getActiveEditor();
    if (editor != null) {
      IEditorInput input = editor.getEditorInput();
      if (input instanceof IFileEditorInput) {
        IFileEditorInput fileInput = (IFileEditorInput) input;
        IFile file = fileInput.getFile();
        if (file != null) {
          return file.getProject();
        }
      }
    }
    return null;
  }

  public FindReferencesAction_NEW(DartEditor editor) {
    super(editor);
  }

  public FindReferencesAction_NEW(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    Element[] elements = OpenAction.getNavigationTargets(selection);
    setEnabled(elements.length != 0);
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    // TODO(scheglov) Analysis Server: implement search for an Element in outline view
//    Element element = getSelectionElement(selection);
//    setEnabled(element != null);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    // prepare context
    DartEditor editor = selection.getEditor();
    final String filePath = editor.getInputFilePath();
    final int offset = selection.getOffset();
    // open Search view
    SearchView view = (SearchView) DartToolsPlugin.showView(SearchView.ID);
    if (view == null) {
      return;
    }
    // do search
    final Element[] elements = OpenAction.getNavigationTargets(selection);
    final Element element = elements != null ? elements[0] : null;
    view.showPage(new SearchResultPage_NEW(view, "Searching for references...") {

      @Override
      protected IProject getCurrentProject() {
        return findCurrentProject();
      }

      @Override
      protected String getQueryElementName() {
        if (element == null) {
          return "<unknown>";
        }
        return element.getName();
      }

      @Override
      protected String getQueryKindName() {
        return "references";
      }

      @Override
      protected List<SearchResult> runQuery() {
        final List<SearchResult> allResults = Lists.newArrayList();
        if (element != null) {
          final CountDownLatch latch = new CountDownLatch(1);
          DartCore.getAnalysisServer().searchElementReferences(
              filePath,
              offset,
              true,
              new SearchIdConsumer() {
                @Override
                public void computedSearchId(String searchId) {
                  DartCore.getAnalysisServerData().addSearchResultsListener(
                      searchId,
                      new SearchResultsListener() {
                        @Override
                        public void computedSearchResults(SearchResult[] results, boolean last) {
                          Collections.addAll(allResults, results);
                          if (last) {
                            latch.countDown();
                          }
                        }
                      });
                }
              });
          Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.MINUTES);
        }
        return allResults;
      }
    });
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    // TODO(scheglov) Analysis Server: implement search for an Element in outline view
//    Element element = getSelectionElement(selection);
//    doSearch(element, null);
  }

  @Override
  protected void init() {
    setText(SearchMessages.Search_FindReferencesAction_label);
    setToolTipText(SearchMessages.Search_FindReferencesAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.FIND_REFERENCES_IN_WORKSPACE_ACTION);
  }
}

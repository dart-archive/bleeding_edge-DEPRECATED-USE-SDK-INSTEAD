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
import com.google.dart.server.FindElementReferencesConsumer;
import com.google.dart.server.FindMemberReferencesConsumer;
import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.server.generated.types.NavigationTarget;
import com.google.dart.server.generated.types.SearchResult;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisServerNavigationListener;
import com.google.dart.tools.core.analysis.model.SearchResultsListener;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction_NEW;
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Finds references of the selected entity in the workspace.
 * 
 * @coverage dart.editor.ui.search
 */
public class FindReferencesAction_NEW extends AbstractDartSelectionAction_NEW implements
    AnalysisServerNavigationListener {
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
          DartCore.getAnalysisServer().search_findMemberReferences(
              name,
              new FindMemberReferencesConsumer() {
                @Override
                public void computedSearchId(String searchId) {
                  DartCore.getAnalysisServerData().addSearchResultsListener(
                      searchId,
                      new SearchResultsListener() {
                        @Override
                        public void computedSearchResults(List<SearchResult> results, boolean last) {
                          allResults.addAll(results);
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
    DartCore.getAnalysisServerData().addNavigationListener(file, this);
  }

  @Override
  public void computedNavigation(String file, NavigationRegion[] regions) {
    updateSelectedElement();
  }

  @Override
  public void dispose() {
    DartCore.getAnalysisServerData().removeNavigationListener(file, this);
    super.dispose();
  }

  @Override
  public void run() {
    SearchView view = (SearchView) DartToolsPlugin.showView(SearchView.ID);
    if (view == null) {
      return;
    }
    // do search
    final int offset = selectionOffset;
    view.showPage(new SearchResultPage_NEW(view, "Searching for references...") {
      private Element element;

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
        final CountDownLatch latch = new CountDownLatch(1);
        DartCore.getAnalysisServer().search_findElementReferences(
            file,
            offset,
            true,
            new FindElementReferencesConsumer() {
              @Override
              public void computedElementReferences(String searchId, Element _element) {
                // no element at the offset
                if (searchId == null) {
                  latch.countDown();
                  return;
                }
                // wait for search results
                element = _element;
                DartCore.getAnalysisServerData().addSearchResultsListener(
                    searchId,
                    new SearchResultsListener() {
                      @Override
                      public void computedSearchResults(List<SearchResult> results, boolean last) {
                        allResults.addAll(results);
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
  }

  @Override
  public void selectionChanged(ISelection selection) {
    super.selectionChanged(selection);
    updateSelectedElement();
  }

  @Override
  protected void init() {
    setText(SearchMessages.Search_FindReferencesAction_label);
    setToolTipText(SearchMessages.Search_FindReferencesAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.FIND_REFERENCES_IN_WORKSPACE_ACTION);
  }

  private void updateSelectedElement() {
    NavigationTarget[] targets = NewSelectionConverter.getNavigationTargets(file, selectionOffset);
    setEnabled(targets.length != 0);
  }
}

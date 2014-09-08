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
import com.google.dart.server.FindMemberDeclarationsConsumer;
import com.google.dart.server.generated.types.SearchResult;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.SearchResultsListener;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction_NEW;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.functions.DartWordFinder;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.PlatformUI;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Finds declarations similar to the selected in the workspace.
 * 
 * @coverage dart.editor.ui.search
 */
public class FindDeclarationsAction_NEW extends AbstractDartSelectionAction_NEW {
  /**
   * Asks {@link SearchView} to execute query and display results.
   */
  public static void doSearch(final String name) {
    try {
      SearchView view = (SearchView) DartToolsPlugin.showView(SearchView.ID);
      if (view == null) {
        return;
      }
      String taskName = "Searching for declarations of '" + name + "'...";
      view.showPage(new SearchResultPage_NEW(view, taskName) {
        @Override
        protected boolean canUseFilterPotential() {
          return false;
        }

        @Override
        protected IProject getCurrentProject() {
          return FindReferencesAction.findCurrentProject();
        }

        @Override
        protected String getQueryElementName() {
          return name;
        }

        @Override
        protected String getQueryKindName() {
          return "declarations";
        }

        @Override
        protected List<SearchResult> runQuery() {
          final List<SearchResult> allResults = Lists.newArrayList();
          final CountDownLatch latch = new CountDownLatch(1);
          DartCore.getAnalysisServer().search_findMemberDeclarations(
              name,
              new FindMemberDeclarationsConsumer() {
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
      ExceptionHandler.handle(e, "Find Declarations", "Exception during search.");
    }
  }

  private String selectionName;

  public FindDeclarationsAction_NEW(DartEditor editor) {
    super(editor);
  }

  @Override
  public void run() {
    super.run();
    if (selectionName != null) {
      doSearch(selectionName);
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    super.selectionChanged(event);
    updateSelectionName();
    setEnabled(selectionName != null);
  }

  @Override
  protected void init() {
    setText(SearchMessages.Search_FindDeclarationsAction_label);
    setToolTipText(SearchMessages.Search_FindDeclarationsAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.FIND_DECLARATIONS_IN_WORKSPACE_ACTION);
  }

  private void updateSelectionName() {
    selectionName = null;
    try {
      IDocument document = editor.getViewer().getDocument();
      IRegion nameRegion = DartWordFinder.findWord(document, selectionOffset);
      if (nameRegion == null) {
        return;
      }
      if (nameRegion.getLength() == 0) {
        return;
      }
      selectionName = document.get(nameRegion.getOffset(), nameRegion.getLength());
    } catch (Throwable e) {
    }
  }
}

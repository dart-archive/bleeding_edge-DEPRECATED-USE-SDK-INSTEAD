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
import com.google.dart.server.SearchResult;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction_OLD;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.text.functions.DartWordFinder;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Finds declarations similar to the selected in the workspace.
 * 
 * @coverage dart.editor.ui.search
 */
public class FindDeclarationsAction_NEW extends AbstractDartSelectionAction_OLD {
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
          // TODO (jwren) re-implement after functionality has been updated in server
//          DartCore.getAnalysisServer().searchClassMemberDeclarations(name, new SearchIdConsumer() {
//            @Override
//            public void computedSearchId(String searchId) {
//              DartCore.getAnalysisServerData().addSearchResultsListener(
//                  searchId,
//                  new SearchResultsListener() {
//                    @Override
//                    public void computedSearchResults(SearchResult[] results, boolean last) {
//                      Collections.addAll(allResults, results);
//                      if (last) {
//                        latch.countDown();
//                      }
//                    }
//                  });
//            }
//          });
          Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.MINUTES);
          return allResults;
        }
      });
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Find Declarations", "Exception during search.");
    }
  }

  public FindDeclarationsAction_NEW(DartEditor editor) {
    super(editor);
  }

  public FindDeclarationsAction_NEW(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    String name = findName(selection);
    setEnabled(name != null);
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
//    Element element = getSelectionElement(selection);
//    setEnabled(element != null);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    String name = findName(selection);
    if (name != null) {
      doSearch(name);
    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
//    Element element = getSelectionElement(selection);
//    if (element != null) {
//      String name = element.getDisplayName();
//      doSearch(name);
//    }
  }

  @Override
  protected void init() {
    setText(SearchMessages.Search_FindDeclarationsAction_label);
    setToolTipText(SearchMessages.Search_FindDeclarationsAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.FIND_DECLARATIONS_IN_WORKSPACE_ACTION);
  }

  private String findName(DartSelection selection) {
    // prepare context
    DartEditor editor = selection.getEditor();
    final int offset = selection.getOffset();
    // prepare name
    try {
      IDocument document = editor.getViewer().getDocument();
      IRegion nameRegion = DartWordFinder.findWord(document, offset);
      if (nameRegion == null) {
        return null;
      }
      if (nameRegion.getLength() == 0) {
        return null;
      }
      return document.get(nameRegion.getOffset(), nameRegion.getLength());
    } catch (Throwable e) {
      return null;
    }
  }
}

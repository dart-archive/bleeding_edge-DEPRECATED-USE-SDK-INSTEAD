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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

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
    // TODO(scheglov) restore or remove for the new API
//    try {
//      SearchView view = (SearchView) DartToolsPlugin.showView(SearchView.ID);
//      view.showPage(new SearchResultPage_NEW(view, "Searching for references...", null) {
//        @Override
//        protected boolean canUseFilterPotential() {
//          return false;
//        }
//
//        @Override
//        protected IProject getCurrentProject() {
//          return findCurrentProject();
//        }
//
//        @Override
//        protected String getQueryElementName() {
//          return name;
//        }
//
//        @Override
//        protected String getQueryKindName() {
//          return "references";
//        }
//
//        @Override
//        protected List<SearchResult> runQuery() {
//          final List<SearchResult> allResults = Lists.newArrayList();
//          final CountDownLatch latch = new CountDownLatch(1);
//          DartCore.getAnalysisServer().searchClassMemberReferences(
//              name,
//              new SearchResultsConsumer() {
//                @Override
//                public void computed(SearchResult[] searchResults, boolean isLastResult) {
//                  Collections.addAll(allResults, searchResults);
//                  if (isLastResult) {
//                    latch.countDown();
//                  }
//                }
//              });
//          Uninterruptibles.awaitUninterruptibly(latch, 15, TimeUnit.MINUTES);
//          return allResults;
//        }
//      });
//    } catch (Throwable e) {
//      ExceptionHandler.handle(e, "Find references", "Exception during search.");
//    }
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
    // TODO(scheglov) Analysis Server: implement for new API
    setEnabled(false);
//    Element[] elements = OpenAction.getNavigationTargets(selection);
//    setEnabled(elements.length != 0);
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
    // TODO(scheglov) Analysis Server: implement for new API
//    // prepare context
//    DartEditor editor = selection.getEditor();
//    final String contextId = editor.getInputAnalysisContextId();
//    final Source source = editor.getInputSource();
//    if (contextId == null || source == null) {
//      return;
//    }
//    // open Search view
//    SearchView view = (SearchView) DartToolsPlugin.showView(SearchView.ID);
//    if (view == null) {
//      return;
//    }
//    // do search
//    final Element[] elements = OpenAction.getNavigationTargets(selection);
//    final Element element = elements != null ? elements[0] : null;
//    view.showPage(new SearchResultPage_NEW(view, "Searching for references...", contextId) {
//
//      @Override
//      protected IProject getCurrentProject() {
//        return findCurrentProject();
//      }
//
//      @Override
//      protected String getQueryElementName() {
//        if (element == null) {
//          return "<unknown>";
//        }
//        return element.getName();
//      }
//
//      @Override
//      protected String getQueryKindName() {
//        return "references";
//      }
//
//      @Override
//      protected List<SearchResult> runQuery() {
//        final List<SearchResult> allResults = Lists.newArrayList();
//        // TODO(scheglov) restore or remove for the new API
////        if (element != null) {
////          final CountDownLatch latch = new CountDownLatch(1);
////          DartCore.getAnalysisServer().searchElementReferences(
////              element,
////              true,
////              new SearchResultsConsumer() {
////                @Override
////                public void computed(SearchResult[] searchResults, boolean isLastResult) {
////                  Collections.addAll(allResults, searchResults);
////                  if (isLastResult) {
////                    latch.countDown();
////                  }
////                }
////              });
////          Uninterruptibles.awaitUninterruptibly(latch, 15, TimeUnit.MINUTES);
////        }
//        return allResults;
//      }
//    });
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

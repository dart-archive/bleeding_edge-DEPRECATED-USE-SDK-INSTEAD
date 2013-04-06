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
package com.google.dart.tools.search.internal.ui;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.internal.corext.refactoring.util.DartElementUtil;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import static com.google.dart.tools.search.internal.ui.FindDeclarationsAction.isInvocationNameOfPropertyAccessSelected;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.util.List;

/**
 * Finds references of the selected {@link Element} in the workspace.
 */
public class FindReferencesAction extends AbstractDartSelectionAction {
  public FindReferencesAction(DartEditor editor) {
    super(editor);
  }

  public FindReferencesAction(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    Element element = ActionUtil.getActionElement(selection);
    setEnabled(element != null || isInvocationNameOfPropertyAccessSelected(selection));
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    Element element = getSelectionElement(selection);
    setEnabled(element != null);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = ActionUtil.getActionElement(selection);
    ASTNode node = getSelectionNode(selection);
    doSearch(element, node);
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    doSearch(element, null);
  }

  @Override
  protected void init() {
    setText(SearchMessages.Search_FindReferencesAction_label);
    setToolTipText(SearchMessages.Search_FindReferencesAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.FIND_REFERENCES_IN_WORKSPACE_ACTION);
  }

  /**
   * Asks {@link SearchView} to execute query and display results.
   */
  private void doSearch(Element element, ASTNode node) {
    // tweak
    element = DartElementUtil.getVariableIfSyntheticAccessor(element);
    if (element instanceof ImportElement) {
      element = ((ImportElement) element).getImportedLibrary();
    }
    // prepare name
    String name = null;
    if (node instanceof SimpleIdentifier) {
      name = ((SimpleIdentifier) node).getName();
    }
    // show search results
    try {
      final SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();
      final Element searchElement = element;
      final String searchName = name;
      SearchView view = (SearchView) DartToolsPlugin.getActivePage().showView(SearchView.ID);
      view.showPage(new SearchMatchPage(view, "Searching for references...") {
        @Override
        protected List<SearchMatch> runQuery() {
          List<SearchMatch> allMatches = Lists.newArrayList();
          // add Element references
          if (searchElement != null) {
            allMatches.addAll(searchEngine.searchReferences(searchElement, null, null));
          }
          // add Name references
          List<SearchMatch> nameReferences = searchEngine.searchQualifiedMemberReferences(
              searchName,
              null,
              null);
          for (SearchMatch match : nameReferences) {
            if (searchElement == null || match.getKind() == MatchKind.NAME_REFERENCE_UNRESOLVED) {
              allMatches.add(match);
            }
          }
          return allMatches;
        }
      });
    } catch (Throwable e) {
      ExceptionHandler.handle(e, getText(), "Exception during search.");
    }
  }
}

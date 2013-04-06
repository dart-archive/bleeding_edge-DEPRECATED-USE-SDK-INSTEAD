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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.util.List;

/**
 * Finds declarations of the {@link Element}s similar to selected in the workspace.
 */
public class FindDeclarationsAction extends AbstractDartSelectionAction {
  static boolean isInvocationNameOfPropertyAccessSelected(DartSelection selection) {
    ASTNode node = getSelectionNode(selection);
    if (!(node instanceof SimpleIdentifier)) {
      return false;
    }
    SimpleIdentifier name = (SimpleIdentifier) node;
    ASTNode parent = name.getParent();
    // method name
    if (parent instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) parent;
      return invocation.getMethodName() == name && invocation.getRealTarget() != null;
    }
    // property name
    if (parent instanceof PropertyAccess) {
      PropertyAccess access = (PropertyAccess) parent;
      return access.getPropertyName() == name && access.getRealTarget() != null;
    }
    // property name (cannot be distinguished from prefixed)
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      return prefixed.getIdentifier() == name;
    }
    // we don't know this node
    return false;
  }

  private static boolean isResolvedClassMemberSelected(DartSelection selection) {
    Element element = getSelectionElement(selection);
    return element instanceof MethodElement || element instanceof FieldElement
        || element instanceof PropertyAccessorElement;
  }

  public FindDeclarationsAction(DartEditor editor) {
    super(editor);
  }

  public FindDeclarationsAction(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    setEnabled(isResolvedClassMemberSelected(selection)
        || isInvocationNameOfPropertyAccessSelected(selection));
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    Element element = getSelectionElement(selection);
    setEnabled(element != null);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    ASTNode node = getSelectionNode(selection);
    if (node instanceof SimpleIdentifier) {
      String name = ((SimpleIdentifier) node).getName();
      doSearch(name);
    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    if (element != null) {
      String name = element.getName();
      doSearch(name);
    }
  }

  @Override
  protected void init() {
    setText(SearchMessages.Search_FindDeclarationsAction_label);
    setToolTipText(SearchMessages.Search_FindDeclarationsAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.FIND_DECLARATIONS_IN_WORKSPACE_ACTION);
  }

  /**
   * Asks {@link SearchView} to execute query and display results.
   */
  private void doSearch(final String name) {
    try {
      final SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();
      SearchView view = (SearchView) DartToolsPlugin.getActivePage().showView(SearchView.ID);
      view.showPage(new SearchMatchPage(view, "Searching for declarations...") {
        @Override
        protected List<SearchMatch> runQuery() {
          return searchEngine.searchDeclarations(name, null, null);
        }
      });
    } catch (Throwable e) {
      ExceptionHandler.handle(e, getText(), "Exception during search.");
    }
  }
}

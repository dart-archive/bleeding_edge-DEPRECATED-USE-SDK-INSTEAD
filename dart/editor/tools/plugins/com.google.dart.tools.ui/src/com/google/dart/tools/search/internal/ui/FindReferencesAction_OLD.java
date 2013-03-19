/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;
import com.google.dart.tools.ui.search.ElementQuerySpecification;
import com.google.dart.tools.ui.search.QuerySpecification;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Finds references of the selected element in the workspace. The action is applicable to selections
 * representing a Dart element.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class FindReferencesAction_OLD extends FindAction_OLD {

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Dart editor
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public FindReferencesAction_OLD(DartEditor editor) {
    super(editor);
  }

  /**
   * Creates a new <code>FindReferencesAction</code>. The action requires that the selection
   * provided by the site's selection provider is of type
   * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param site the site providing context information for this action
   */
  public FindReferencesAction_OLD(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartTextSelection selection) {
    // Update text of menu item to include name of element.
//    String text = ActionUtil.constructMenuText(
//        SearchMessages.Search_FindReferencesAction_label,
//        true,
//        selection);
//    setText(text.toString());
  }

  @Override
  QuerySpecification createQuery(DartElement element) throws DartModelException,
      InterruptedException {
    SearchScope scope = SearchScopeFactory.createWorkspaceScope();
    return new ElementQuerySpecification(element, getLimitTo(), scope, "workspace"); //$NON-NLS-1$
  }

  //TODO (pquitslund): update to use SearchFilter
  @Override
  int getLimitTo() {
    return QuerySpecification.LIMIT_REFERENCES;
  }

  @Override
  Class<?>[] getValidTypes() {
    return new Class[] {CompilationUnitElement.class};
  }

  @Override
  void init() {
    setText(SearchMessages.Search_FindReferencesAction_label);
    setToolTipText(SearchMessages.Search_FindReferencesAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.FIND_REFERENCES_IN_WORKSPACE_ACTION);
  }
}

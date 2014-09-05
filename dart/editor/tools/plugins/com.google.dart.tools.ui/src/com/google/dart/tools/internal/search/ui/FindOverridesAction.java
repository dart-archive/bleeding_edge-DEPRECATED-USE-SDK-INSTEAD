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
package com.google.dart.tools.internal.search.ui;

import com.google.dart.tools.ui.actions.AbstractDartSelectionAction_OLD;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.ui.IWorkbenchSite;

/**
 * Finds declarations of the selected method in the hierarchy. The action is applicable to
 * selections representing a Dart method only. TODO(pquitslund): unused (implement or remove)
 */
public class FindOverridesAction extends AbstractDartSelectionAction_OLD /* FindAction_OLD */{

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Dart editor
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public FindOverridesAction(DartEditor editor) {
    super(editor);
  }

  /**
   * Creates a new <code>FindOverridesAction</code>. The action requires that the selection provided
   * by the site's selection provider is of type
   * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param site the site providing context information for this action
   */
  public FindOverridesAction(IWorkbenchSite site) {
    super(site);
  }

  @Override
  protected void init() {
    // TODO Auto-generated method stub
  }

//  @Override
//  QuerySpecification createQuery(DartElement element) throws DartModelException,
//      InterruptedException {
//    SearchScope scope = SearchScopeFactory.createWorkspaceScope();
//    return new ElementQuerySpecification(element, getLimitTo(), scope, "workspace"); //$NON-NLS-1$
//  }
//
//  @Override
//  int getLimitTo() {
//    return QuerySpecification.LIMIT_OVERRIDES;
//  }
//
//  @Override
//  Class<?>[] getValidTypes() {
//    return new Class[] {CompilationUnitElement.class};
//  }
//
//  @Override
//  void init() {
//    setText(SearchMessages.Search_FindOverridesAction_label);
//    setToolTipText(SearchMessages.Search_FindOverridesAction_tooltip);
//    PlatformUI.getWorkbench().getHelpSystem().setHelp(
//        this,
//        DartHelpContextIds.FIND_DECLARATIONS_IN_HIERARCHY_ACTION);
//  }

}

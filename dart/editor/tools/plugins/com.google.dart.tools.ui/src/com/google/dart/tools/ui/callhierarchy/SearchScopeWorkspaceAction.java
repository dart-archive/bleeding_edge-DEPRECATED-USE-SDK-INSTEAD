/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
class SearchScopeWorkspaceAction extends SearchScopeAction {

  public SearchScopeWorkspaceAction(SearchScopeActionGroup group) {
    super(group, CallHierarchyMessages.SearchScopeActionGroup_workspace_text);
    setToolTipText(CallHierarchyMessages.SearchScopeActionGroup_workspace_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_SEARCH_SCOPE_ACTION);
  }

  @Override
  public String getFullDescription(int includeMask) {
    return SearchMessages.WorkspaceScope;
  }

  @Override
  public SearchScope getSearchScope(int includeMask) {
    return SearchScopeFactory.createWorkspaceScope();
  }

  @Override
  public int getSearchScopeType() {
    return SearchScopeActionGroup.SEARCH_SCOPE_TYPE_WORKSPACE;
  }
}

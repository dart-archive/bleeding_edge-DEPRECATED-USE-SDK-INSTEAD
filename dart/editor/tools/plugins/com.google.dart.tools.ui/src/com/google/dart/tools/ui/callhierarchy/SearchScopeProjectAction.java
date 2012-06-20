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
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.ui.PlatformUI;

class SearchScopeProjectAction extends SearchScopeAction {
//  private final SearchScopeActionGroup fGroup;

  public SearchScopeProjectAction(SearchScopeActionGroup group) {
    super(group, CallHierarchyMessages.SearchScopeActionGroup_project_text);
//    this.fGroup = group;
    setToolTipText(CallHierarchyMessages.SearchScopeActionGroup_project_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_SEARCH_SCOPE_ACTION);
  }

  @Override
  public String getFullDescription(int includeMask) {
//    TypeMember[] members = fGroup.getView().getInputElements();
//    if (members != null) {
//      HashSet<String> projectNames = new HashSet<String>();
//      for (int i = 0; i < members.length; i++) {
//        projectNames.add(members[i].getDartProject().getElementName());
//      }
//      SearchScopeFactory factory = SearchScopeFactory.getInstance();
//      return factory.getProjectScopeDescription(
//          projectNames.toArray(new String[projectNames.size()]), includeMask);
//    }
    return ""; //$NON-NLS-1$
  }

  @Override
  public SearchScope getSearchScope(int includeMask) {
    return SearchScopeFactory.createWorkspaceScope();
//    TypeMember[] members = fGroup.getView().getInputElements();
//    if (members == null) {
//      return null;
//    }
//    HashSet<DartProject> projects = new HashSet<DartProject>();
//    for (int i = 0; i < members.length; i++) {
//      projects.add(members[i].getDartProject());
//    }
//    return SearchEngine.createJavaSearchScope(projects.toArray(new DartProject[projects.size()]),
//        includeMask);
  }

  @Override
  public int getSearchScopeType() {
    return SearchScopeActionGroup.SEARCH_SCOPE_TYPE_PROJECT;
  }
}

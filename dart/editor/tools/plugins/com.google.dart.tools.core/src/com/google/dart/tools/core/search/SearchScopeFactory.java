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
package com.google.dart.tools.core.search;

import com.google.dart.tools.core.internal.search.scope.ProjectSearchScope;
import com.google.dart.tools.core.internal.search.scope.WorkspaceSearchScope;
import com.google.dart.tools.core.model.DartProject;

/**
 * The class <code>SearchScopeFactory</code> defines utility methods that can be used to create
 * search scopes.
 * 
 * @coverage dart.tools.core.search
 */
public final class SearchScopeFactory {
  /**
   * A search scope that encompasses everything in the workspace. Because it does not hold any state
   * there is no reason not to share a single instance.
   */
  private static final SearchScope WORKSPACE_SCOPE = new WorkspaceSearchScope();

  /**
   * Create a search scope that encompasses everything in the types included in the given project.
   * 
   * @param project the project defining which types are included in the scope
   * @return the search scope that was created
   */
  public static SearchScope createProjectScope(DartProject project) {
    return new ProjectSearchScope(project);
  }

  /**
   * Create a search scope that encompasses everything in the workspace.
   * 
   * @return the search scope that was created
   */
  public static SearchScope createWorkspaceScope() {
    return WORKSPACE_SCOPE;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private SearchScopeFactory() {
    super();
  }
}

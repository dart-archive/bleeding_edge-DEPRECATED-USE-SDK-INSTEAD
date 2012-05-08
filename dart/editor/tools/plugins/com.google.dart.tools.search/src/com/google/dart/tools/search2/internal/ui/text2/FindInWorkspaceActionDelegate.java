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
package com.google.dart.tools.search2.internal.ui.text2;

import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.text.TextSearchQueryProvider;
import com.google.dart.tools.search2.internal.ui.SearchMessages;

import org.eclipse.core.runtime.CoreException;

/**
 * @author markus.schorn@windriver.com
 */
public class FindInWorkspaceActionDelegate extends FindInRecentScopeActionDelegate {

  public FindInWorkspaceActionDelegate() {
    super(SearchMessages.FindInWorkspaceActionDelegate_text);
    setActionDefinitionId("com.google.dart.tools.search.ui.performTextSearchWorkspace"); //$NON-NLS-1$
  }

  protected ISearchQuery createQuery(TextSearchQueryProvider provider, String searchForString)
      throws CoreException {
    return provider.createQuery(searchForString);
  }
}

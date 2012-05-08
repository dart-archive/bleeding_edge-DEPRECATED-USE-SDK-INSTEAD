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

import com.google.dart.tools.search.internal.ui.text.FileSearchQuery;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.text.FileTextSearchScope;
import com.google.dart.tools.search.ui.text.TextSearchQueryProvider;

import org.eclipse.core.resources.IResource;

import org.eclipse.ui.IWorkingSet;

public class DefaultTextSearchQueryProvider extends TextSearchQueryProvider {

  public ISearchQuery createQuery(TextSearchInput input) {
    FileTextSearchScope scope = input.getScope();
    String text = input.getSearchText();
    boolean regEx = input.isRegExSearch();
    boolean caseSensitive = input.isCaseSensitiveSearch();
    return new FileSearchQuery(text, regEx, caseSensitive, scope);
  }

  public ISearchQuery createQuery(String searchForString) {
    FileTextSearchScope scope = FileTextSearchScope.newWorkspaceScope(
        getPreviousFileNamePatterns(),
        false);
    return new FileSearchQuery(searchForString, false, true, scope);
  }

  public ISearchQuery createQuery(String selectedText, IResource[] resources) {
    FileTextSearchScope scope = FileTextSearchScope.newSearchScope(
        resources,
        getPreviousFileNamePatterns(),
        false);
    return new FileSearchQuery(selectedText, false, true, scope);
  }

  public ISearchQuery createQuery(String selectedText, IWorkingSet[] ws) {
    FileTextSearchScope scope = FileTextSearchScope.newSearchScope(
        ws,
        getPreviousFileNamePatterns(),
        false);
    return new FileSearchQuery(selectedText, false, true, scope);
  }

  private String[] getPreviousFileNamePatterns() {
    return new String[] {"*"}; //$NON-NLS-1$
  }

}

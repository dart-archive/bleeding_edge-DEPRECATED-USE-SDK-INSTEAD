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
package com.google.dart.tools.search.ui.actions;

import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.search.internal.ui.text.FileSearchQuery;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.text.FileTextSearchScope;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Triggers a text search.
 */
public class TextSearchAction extends Action {

  private static final String[] DEFAULT_FILE_EXTENSIONS = {"*"}; //$NON-NLS-1$

  private final Shell shell;
  private final String searchText;
  private final String[] fileExtensions;
  private final boolean isCaseSensitive;
  private final boolean isRegEx;

  public TextSearchAction(Shell shell, String searchText) {
    this(shell, searchText, DEFAULT_FILE_EXTENSIONS, false, false);
  }

  public TextSearchAction(Shell shell, String searchText, String[] fileExtensions, boolean isRegEx,
      boolean isCaseSensitive) {
    this.shell = shell;
    this.searchText = searchText;
    this.fileExtensions = fileExtensions;
    this.isRegEx = isRegEx;
    this.isCaseSensitive = isCaseSensitive;
  }

  @Override
  public void run() {
    try {
      NewSearchUI.runQueryInBackground(newQuery());
    } catch (CoreException e) {
      ErrorDialog.openError(shell, SearchMessages.TextSearchPage_replace_searchproblems_title,
          SearchMessages.TextSearchPage_replace_searchproblems_message, e.getStatus());
    }
  }

  private FileTextSearchScope createTextSearchScope() {
    return FileTextSearchScope.newWorkspaceScope(fileExtensions, false /* derived */);
  }

  private ISearchQuery newQuery() throws CoreException {
    return new FileSearchQuery(searchText, isRegEx, isCaseSensitive, createTextSearchScope());
  }

}

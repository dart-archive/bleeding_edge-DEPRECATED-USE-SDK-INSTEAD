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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;

/**
 * Triggers a text search.
 * 
 * @coverage dart.editor.ui.search
 */
public class TextSearchAction extends Action {
  private final String searchText;

  public TextSearchAction(Shell shell, String searchText) {
    this.searchText = searchText;
  }

  @Override
  public void run() {
    try {
      SearchView view = (SearchView) DartToolsPlugin.getActivePage().showView(SearchView.ID);
      view.showPage(new TextSearchPage(view, "Searching for text...", searchText));
    } catch (CoreException e) {
      ExceptionHandler.handle(e, getText(), "Exception during search.");
    }
  }
}

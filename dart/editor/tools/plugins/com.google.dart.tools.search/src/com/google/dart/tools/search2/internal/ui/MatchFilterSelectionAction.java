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
package com.google.dart.tools.search2.internal.ui;

import com.google.dart.tools.search.internal.ui.SearchPluginImages;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;
import com.google.dart.tools.search.ui.text.MatchFilter;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;

public class MatchFilterSelectionAction extends Action {

  public static final String ACTION_ID = "MatchFilterSelectionAction"; //$NON-NLS-1$

  private AbstractTextSearchViewPage fPage;

  public MatchFilterSelectionAction(AbstractTextSearchViewPage page) {
    super(SearchMessages.MatchFilterSelectionAction_label);
    setId(ACTION_ID);
    SearchPluginImages.setImageDescriptors(
        this,
        SearchPluginImages.T_LCL,
        SearchPluginImages.IMG_LCL_SEARCH_FILTER);
    fPage = page;
  }

  public void run() {
    Shell shell = fPage.getSite().getShell();

    AbstractTextSearchResult input = fPage.getInput();
    if (input == null) {
      return;
    }

    MatchFilter[] allFilters = input.getAllMatchFilters();
    MatchFilter[] checkedFilters = input.getActiveMatchFilters();
    Integer limit = fPage.getElementLimit();

    boolean enableMatchFilterConfiguration = checkedFilters != null;
    boolean enableLimitConfiguration = limit != null;
    int elementLimit = limit != null ? limit.intValue() : -1;

    MatchFilterSelectionDialog dialog = new MatchFilterSelectionDialog(
        shell,
        enableMatchFilterConfiguration,
        allFilters,
        checkedFilters,
        enableLimitConfiguration,
        elementLimit);
    if (dialog.open() == Window.OK) {
      if (enableMatchFilterConfiguration) {
        input.setActiveMatchFilters(dialog.getMatchFilters());
      }
      if (enableLimitConfiguration) {
        fPage.setElementLimit(new Integer(dialog.getLimit()));
      }
    }
  }

}

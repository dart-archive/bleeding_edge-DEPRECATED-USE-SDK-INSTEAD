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
import com.google.dart.tools.search.ui.ISearchResult;

import org.eclipse.jface.action.Action;

public class CancelSearchAction extends Action {

  private SearchView fView;

  public CancelSearchAction(SearchView view) {
    super(SearchMessages.CancelSearchAction_label);
    fView = view;
    setToolTipText(SearchMessages.CancelSearchAction_tooltip);
    SearchPluginImages.setImageDescriptors(
        this,
        SearchPluginImages.T_LCL,
        SearchPluginImages.IMG_LCL_SEARCH_CANCEL);
  }

  public void run() {
    ISearchResult result = fView.getCurrentSearchResult();
    if (result != null) {
      InternalSearchUI.getInstance().cancelSearch(result.getQuery());
    }
  }
}

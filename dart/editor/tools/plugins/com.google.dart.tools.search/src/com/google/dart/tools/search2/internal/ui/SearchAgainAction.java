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
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.NewSearchUI;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;

class SearchAgainAction extends Action {
  private SearchView fView;

  public SearchAgainAction(SearchView view) {
    setText(SearchMessages.SearchAgainAction_label);
    setToolTipText(SearchMessages.SearchAgainAction_tooltip);
    SearchPluginImages.setImageDescriptors(
        this,
        SearchPluginImages.T_LCL,
        SearchPluginImages.IMG_LCL_REFRESH);
    fView = view;
  }

  public void run() {
    final ISearchResult search = fView.getCurrentSearchResult();
    if (search != null) {
      ISearchQuery query = search.getQuery();
      NewSearchUI.cancelQuery(query);
      if (query.canRerun()) {
        if (query.canRunInBackground())
          NewSearchUI.runQueryInBackground(query, fView);
        else {
          Shell shell = fView.getSite().getShell();
          ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
          IStatus status = NewSearchUI.runQueryInForeground(pmd, query, fView);
          if (!status.isOK() && status.getSeverity() != IStatus.CANCEL) {
            ErrorDialog.openError(
                shell,
                SearchMessages.SearchAgainAction_Error_title,
                SearchMessages.SearchAgainAction_Error_message,
                status);
          }
        }
      }
    }
  }
}

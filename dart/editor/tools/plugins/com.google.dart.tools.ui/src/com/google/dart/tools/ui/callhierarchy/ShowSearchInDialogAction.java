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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;

/**
 * Action class to create and open the search in dialog.
 */
public class ShowSearchInDialogAction extends Action {

  private CallHierarchyViewPart chvPart;
  private SearchInDialog searchInDialog;

  /**
   * Action to show the <code>SearchInDialog</code>.
   * 
   * @param part the call hierarchy view part
   * @param viewer the call hierarchy viewer
   */
  public ShowSearchInDialogAction(CallHierarchyViewPart part, CallHierarchyViewer viewer) {
    Assert.isNotNull(part);
    Assert.isNotNull(viewer);
    chvPart = part;
    searchInDialog = new SearchInDialog(chvPart.getViewSite().getShell());
    setText(CallHierarchyMessages.ShowSearchInDialogAction_text);
  }

  /**
   * Returns the <code>SearchInDialog</code>.
   * 
   * @return the <code>searchInDialog</code>
   */
  public SearchInDialog getSearchInDialog() {
    return searchInDialog;
  }

  @Override
  public void run() {
    SearchInDialog dialog = getSearchInDialog();
    if (dialog.open() == Window.OK && dialog.isIncludeMaskChanged()) {
      chvPart.setInputElements(chvPart.getInputElements());
    }
  }
}

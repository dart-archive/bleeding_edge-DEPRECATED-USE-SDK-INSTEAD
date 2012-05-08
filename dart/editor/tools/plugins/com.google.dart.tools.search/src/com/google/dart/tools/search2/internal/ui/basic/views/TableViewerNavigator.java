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
package com.google.dart.tools.search2.internal.ui.basic.views;

import org.eclipse.jface.viewers.TableViewer;

public class TableViewerNavigator implements INavigate {
  private TableViewer fViewer;

  public TableViewerNavigator(TableViewer viewer) {
    fViewer = viewer;
  }

  public void navigateNext(boolean forward) {
    int itemCount = fViewer.getTable().getItemCount();
    if (itemCount == 0)
      return;
    int[] selection = fViewer.getTable().getSelectionIndices();
    int nextIndex = 0;
    if (selection.length > 0) {
      if (forward) {
        nextIndex = selection[selection.length - 1] + 1;
        if (nextIndex >= itemCount)
          nextIndex = 0;
      } else {
        nextIndex = selection[0] - 1;
        if (nextIndex < 0)
          nextIndex = itemCount - 1;
      }
    }
    fViewer.getTable().setSelection(nextIndex);
    fViewer.getTable().showSelection();
  }
}

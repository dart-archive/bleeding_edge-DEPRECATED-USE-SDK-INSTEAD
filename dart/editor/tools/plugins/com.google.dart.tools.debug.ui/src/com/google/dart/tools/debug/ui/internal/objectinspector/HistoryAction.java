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

package com.google.dart.tools.debug.ui.internal.objectinspector;

import com.google.dart.tools.debug.core.util.HistoryList;
import com.google.dart.tools.debug.core.util.HistoryListListener;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.jface.action.Action;

/**
 * An IAction implementation that implements a browser forward and backwards button.
 */
class HistoryAction<T> extends Action implements HistoryListListener<T> {

  public static <E> HistoryAction<E> createBackAction(HistoryList<E> historyList) {
    return new HistoryAction<E>(historyList, true);
  }

  public static <E> HistoryAction<E> createForwardAction(HistoryList<E> historyList) {
    return new HistoryAction<E>(historyList, false);
  }

  private HistoryList<T> historyList;
  private boolean isForwardAction;

  private HistoryAction(HistoryList<T> historyList, boolean isForwardAction) {
    super(isForwardAction ? "Forward" : "Back", isForwardAction
        ? DartDebugUIPlugin.getImageDescriptor("obj16/forward_nav.gif")
        : DartDebugUIPlugin.getImageDescriptor("obj16/backward_nav.gif"));

    this.historyList = historyList;
    this.isForwardAction = isForwardAction;

    updateEnablement();

    historyList.addListener(this);
  }

  @Override
  public void historyAboutToChange(T current) {

  }

  @Override
  public void historyChanged(T current) {
    updateEnablement();
  }

  @Override
  public void run() {
    if (isForwardAction) {
      historyList.navigateNext();
    } else {
      historyList.navigatePrevious();
    }
  }

  private void updateEnablement() {
    if (isForwardAction) {
      setEnabled(historyList.hasNext());
    } else {
      setEnabled(historyList.hasPrevious());
    }
  }

}

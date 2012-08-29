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

package com.google.dart.tools.debug.ui.internal.view;

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * An action to show the Expressions view.
 */
public class ShowExpressionsAction extends Action {

  public ShowExpressionsAction() {
    super("Show Expressions", DartDebugUIPlugin.getImageDescriptor("obj16/watchlist_view.gif"));
  }

  @Override
  public synchronized void run() {
    try {
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
          DartExpressionView.VIEW_ID);
    } catch (PartInitException e) {
      DartUtil.logError(e);
    }
  }

}

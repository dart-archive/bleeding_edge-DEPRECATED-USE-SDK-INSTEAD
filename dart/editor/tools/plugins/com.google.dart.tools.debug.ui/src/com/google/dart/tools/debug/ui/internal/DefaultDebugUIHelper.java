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

package com.google.dart.tools.debug.ui.internal;

import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.ui.internal.view.DebuggerView;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * A helper to allow non-UI code to interact with the UI.
 */
public class DefaultDebugUIHelper extends DebugUIHelper {

  @Override
  public void showStatusLineMessage(final String message) {
    final Display display = Display.getDefault();

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        if (display.isDisposed()) {
          return;
        }

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        IViewPart part = page.findView(DebuggerView.ID);

        if (part != null) {
          IStatusLineManager manager = part.getViewSite().getActionBars().getStatusLineManager();

          manager.setMessage(message);
        }
      }
    });
  }

}

/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.ui.internal.chrome;

import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import java.util.List;

/**
 * A ChromeTabChooser implementation which allows the user to select from a list of given Chrome
 * tabs.
 */
public class DebugUIHelperImpl implements DebugUIHelper {

  public DebugUIHelperImpl() {

  }

  @Override
  public void displayError(final String title, final String message) {
    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        DebugErrorHandler.errorDialog(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), title, message,
            new Status(IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID, message));
      }
    });
  }

  @Override
  public String getPlatform() {
    return SWT.getPlatform();
  }

  @Override
  public int select(final List<String> availableTabs) {
    final int[] result = new int[1];

    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        result[0] = selectImpl(availableTabs);
      }
    });

    return result[0];
  }

  private int selectImpl(List<String> availableTabs) {
    if (availableTabs.size() == 0) {
      return -1;
    } else if (availableTabs.size() == 1) {
      return 0;
    } else {
      ListDialog dialog = new ListDialog(
          PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());

      dialog.setTitle("Select Browser Tab");
      dialog.setMessage("Select the browser tab to debug:");

      dialog.setLabelProvider(new LabelProvider());
      dialog.setContentProvider(new ArrayContentProvider());
      dialog.setInput(availableTabs);

      if (dialog.open() == Window.OK) {
        Object[] result = dialog.getResult();

        if (result.length > 0) {
          return availableTabs.indexOf(result[0]);
        } else {
          return -1;
        }
      } else {
        return -1;
      }
    }
  }

}

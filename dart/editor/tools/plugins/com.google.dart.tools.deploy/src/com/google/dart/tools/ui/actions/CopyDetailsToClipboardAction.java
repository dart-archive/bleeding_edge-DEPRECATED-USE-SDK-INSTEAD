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
package com.google.dart.tools.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

/**
 * Helper class for adding basic copy/paste support to controls.
 */
public class CopyDetailsToClipboardAction extends Action {

  public static interface DetailsProvider {
    String getDetails();
  }

  //max clipboard access retries
  private final static int MAX_REPEAT_COUNT = 10;

  public static void addCopyDetailsPopup(Control control, final DetailsProvider detailsProvider) {

    final Shell shell = control.getShell();

    Menu popUpMenu = new Menu(shell, SWT.POP_UP);
    MenuItem copyItem = new MenuItem(popUpMenu, SWT.PUSH);
    copyItem.setText("Copy Details");
    copyItem.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        new CopyDetailsToClipboardAction(shell, detailsProvider) {
        }.run();
      }
    });
    control.setMenu(popUpMenu);
  }

  private final Shell shell;

  private final DetailsProvider detailsProvider;

  public CopyDetailsToClipboardAction(Shell shell, DetailsProvider detailsProvider) {
    this.shell = shell;
    this.detailsProvider = detailsProvider;
  }

  @Override
  public void run() {

    Clipboard clipboard = new Clipboard(getShell().getDisplay());
    try {
      copyToClipboard(clipboard, detailsProvider.getDetails(), 0);
    } finally {
      clipboard.dispose();
    }
  }

  private void copyToClipboard(Clipboard clipboard, String text, int repeatCount) {
    try {
      clipboard.setContents(new String[] {text}, new Transfer[] {TextTransfer.getInstance()});
    } catch (SWTError e) {

      if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD || repeatCount >= MAX_REPEAT_COUNT) {
        throw e;
      }

      if (MessageDialog.openQuestion(
          getShell(),
          "Problem Copying to Clipboard",
          "There was a problem when accessing the system clipboard.\nRetry?")) {
        copyToClipboard(clipboard, text, repeatCount + 1);
      }
    }
  }

  private Shell getShell() {
    return shell;
  }

}

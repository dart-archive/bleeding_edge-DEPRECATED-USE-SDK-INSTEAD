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
package com.google.dart.tools.ui.feedback;

import com.google.dart.tools.core.DartCore;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * An action to open the {@link FeedbackDialog}.
 * <p>
 * TODO list
 * <li>Currently the screenshot capture doesn't work on the MacOS- dartbug.com/6458
 * <li>Currently the text and jpeg image are sent as separate requests by
 * {@link FeedbackSubmissionJob}, it would be awesome if we sent them all together so that we could
 * have a single email with attachments, and be smarter about how the information is put together as
 * it is sent out by the AppEngine server
 * 
 * @see FeedbackSubmissionJob
 */
public class OpenFeedbackDialogAction extends Action implements IShellProvider {

  public static boolean SCREEN_CAPTURE_ENABLED = !DartCore.isMac();

  private IShellProvider shellProvider;

  //the dialog shell, cached in case we want to ensure there is only one
  private Shell dialogShell;

  private final String productName;

  public OpenFeedbackDialogAction(IShellProvider shellProvider, String productName) {
    this.productName = productName;
    setShellProvider(shellProvider);
  }

  public OpenFeedbackDialogAction(String productName) {
    this(null, productName);
  }

  /**
   * Get the active dialog shell (if there is one); may be <code>null</code>.
   */
  public Shell getDialogShell() {
    return dialogShell;
  }

  @Override
  public Shell getShell() {
    return shellProvider.getShell();
  }

  @Override
  public void run() {
    Image screenshot = null;
    if (SCREEN_CAPTURE_ENABLED) {
      screenshot = captureScreen();
    }
    new FeedbackDialog(getShell(), productName, screenshot) {

      @Override
      public void create() {
        super.create();
        //cache that the dialog is open
        dialogShell = getShell();
      }

      @Override
      public int open() {
        int result = SWT.CANCEL;
        try {
          result = super.open();
        } finally {
          //cache that the dialog is closed
          dialogShell = null;
        }
        return result;
      }
    }.open();
  }

  /**
   * @param shellProvider the shellProvider to set
   */
  public void setShellProvider(IShellProvider shellProvider) {
    this.shellProvider = shellProvider;
  }

  /**
   * Creates screen shot of entire Dart Editor.
   * 
   * @return the created screen capture
   */
  private Image captureScreen() {
    GC gc = null;
    try {
      Shell shell = getShell();
      shell.redraw();
      shell.update();
      final Rectangle shellBounds = shell.getBounds();
      final Display standardDisplay = getShell().getDisplay();
      standardDisplay.update();
      Image image = new Image(standardDisplay, shellBounds.width, shellBounds.height);
      gc = new GC(standardDisplay);
      gc.copyArea(image, shellBounds.x, shellBounds.y);
      return image;
    } catch (Throwable ex) {
      return null;
    } finally {
      if (gc != null) {
        gc.dispose();
      }
    }
  }

}

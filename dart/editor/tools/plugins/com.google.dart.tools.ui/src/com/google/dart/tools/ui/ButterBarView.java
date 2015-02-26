/*
 * Copyright (c) 2015, the Dart project authors.
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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.util.GridDataFactory;
import com.google.dart.tools.ui.internal.util.GridLayoutFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import java.util.ArrayList;

/**
 * A simple view containing brief information with optional buttons. It appears above the editor
 * area and has a "butter bar" color.
 */
public class ButterBarView extends ViewPart {

  /**
   * The listener used by the view to signal when the user clicks a button and the view closes.
   */
  public static interface Listener {
    /**
     * Called when the user presses a button and/or the view is closed.
     * 
     * @param buttonIndex the index of the button pressed or -1 if the view closed without the user
     *          pressing a button.
     */
    void buttonPressed(int buttonIndex);
  }

  /**
   * Record the selected button and close the view.
   */
  private final class ButtonSelectionListener extends SelectionAdapter {
    private final int buttonIndex;

    public ButtonSelectionListener(int buttonIndex) {
      this.buttonIndex = buttonIndex;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      buttonPressed(buttonIndex);
    }
  }

  public static String VIEW_ID = "com.google.dart.tools.ui.butterBarView"; //$NON-NLS-1$

  /**
   * Open the butter bar view if it is not already open and display the specified message and
   * choices. Trigger the given callback when a selection is made and/or the view is closed. This
   * MUST be called on the UI thread.
   * 
   * @param message the message to be displayed
   * @param buttonText the text for each button
   * @param listener the listener notified when the user presses a button and the view is closed
   */
  public static void show(final String message, final String[] buttonText, Listener listener) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      final ButterBarView view = (ButterBarView) page.showView(VIEW_ID);
      view.updatePartControl(message, buttonText, listener);
    } catch (PartInitException e) {
      DartCore.logError(e);
    }
  }

  /**
   * The composite containing the view controls.
   */
  private Composite parent;

  /**
   * The listener notified when the user presses a button and the view is closed
   */
  private Listener listener;

  private Label messageLabel;

  private ArrayList<Button> buttons = new ArrayList<Button>();

  /**
   * Record the selected button.
   * 
   * @param buttonIndex the index of the selected button.
   */
  public void buttonPressed(int buttonIndex) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    page.hideView(this);
    if (listener != null) {
      listener.buttonPressed(buttonIndex);
    }
  }

  @Override
  public void createPartControl(final Composite parent) {
    this.parent = parent;
    GridLayoutFactory.create(parent).columns(1);
    messageLabel = new Label(parent, SWT.WRAP);
    GridDataFactory.create(messageLabel).fillHorizontal().grab();
    Display display = getViewSite().getShell().getDisplay();
    parent.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
  }

  @Override
  public void setFocus() {
    // ignored
  }

  /**
   * Update the view to show the given message with the specified buttons.
   * 
   * @param message the message to be displayed
   * @param buttonText the text for each button
   * @param listener the listener notified when the user presses a button and the view is closed
   */
  private void updatePartControl(String message, String[] buttonText, Listener listener) {
    int buttonCount = buttonText != null ? buttonText.length : 0;
    GridLayoutFactory.create(parent).columns(buttonCount + 1);
    messageLabel.setText(message != null ? message : "");
    for (int buttonIndex = 0; buttonIndex < buttonCount; buttonIndex++) {
      Button button;
      if (buttonIndex < buttons.size()) {
        button = buttons.get(buttonIndex);
      } else {
        button = new Button(parent, SWT.NONE);
        button.addSelectionListener(new ButtonSelectionListener(buttonIndex + 1));
        buttons.add(button);
      }
      button.setText(buttonText[buttonIndex]);
    }
    while (buttons.size() > buttonCount) {
      buttons.remove(buttons.size() - 1).dispose();
    }
    parent.layout();
    this.listener = listener;
  }
}

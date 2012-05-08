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
package com.google.dart.tools.search.internal.ui.util;

import com.google.dart.tools.search.internal.ui.SearchMessages;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class ExtendedDialogWindow extends TrayDialog implements IRunnableContext {

  private Control fContents;
  private Button fCancelButton;
  private List<Button> fActionButtons;
  // The number of long running operation executed from the dialog.
  private long fActiveRunningOperations;

  // The progress monitor
  private boolean fUseEmbeddedProgressMonitorPart;
  private ProgressMonitorPart fProgressMonitorPart;
  private MessageDialog fWindowClosingDialog;
  private static final String FOCUS_CONTROL = "focusControl"; //$NON-NLS-1$

  public ExtendedDialogWindow(Shell shell) {
    super(shell);
    fActionButtons = new ArrayList<Button>();
  }

  /**
   * The dialog is going to be closed. Check if there is a running operation. If so, post an alert
   * saying that the wizard can't be closed.
   * 
   * @return If false is returned, the dialog should stay open
   */
  public boolean okToClose() {
    if (fActiveRunningOperations > 0) {
      synchronized (this) {
        fWindowClosingDialog = createClosingDialog();
      }
      fWindowClosingDialog.open();
      synchronized (this) {
        fWindowClosingDialog = null;
      }
      return false;
    }
    return true;
  }

  //---- Hooks to reimplement in subclasses -----------------------------------

  @Override
  public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
      throws InvocationTargetException, InterruptedException {
    // The operation can only be canceled if it is executed in a separate thread.
    // Otherwise the UI is blocked anyway.
    Object state = null;
    try {
      fActiveRunningOperations++;
      state = aboutToStart(fork && cancelable);
      if (fUseEmbeddedProgressMonitorPart) {
        ModalContext.run(runnable, fork, fProgressMonitorPart, getShell().getDisplay());
      } else {
        new ProgressMonitorDialog(getShell()).run(fork, cancelable, runnable);
      }
    } finally {
      if (state != null) {
        stopped(state);
      }
      fActiveRunningOperations--;
    }
  }

  /**
   * Set the enable state of the perform action button.
   * 
   * @param state The new state
   */
  public void setPerformActionEnabled(boolean state) {
    for (Iterator<Button> buttons = fActionButtons.iterator(); buttons.hasNext();) {
      Button element = buttons.next();
      element.setEnabled(state);
    }
  }

  /**
   * @param enable Use the embedded progress monitor part
   */
  public void setUseEmbeddedProgressMonitorPart(boolean enable) {
    fUseEmbeddedProgressMonitorPart = enable;
  }

  //---- UI creation ----------------------------------------------------------

  /**
   * About to start a long running operation triggered through the wizard. So show the progress
   * monitor and disable the wizard.
   * 
   * @param enableCancelButton The cancel button enable state
   * @return The saved UI state.
   */
  protected synchronized Object aboutToStart(boolean enableCancelButton) {
    HashMap<Object, Object> savedState = null;
    Shell shell = getShell();
    if (shell != null) {
      Display d = shell.getDisplay();

      // Save focus control
      Control focusControl = d.getFocusControl();
      if (focusControl != null && focusControl.getShell() != shell) {
        focusControl = null;
      }

      // Set the busy cursor to all shells.
      setDisplayCursor(d, d.getSystemCursor(SWT.CURSOR_WAIT));

      // Set the arrow cursor to the cancel component.
      fCancelButton.setCursor(d.getSystemCursor(SWT.CURSOR_ARROW));

      // Deactivate shell
      savedState = saveUIState(enableCancelButton);
      if (focusControl != null) {
        savedState.put(FOCUS_CONTROL, focusControl);
      }

      if (fUseEmbeddedProgressMonitorPart) {
        // Attach the progress monitor part to the cancel button
        fProgressMonitorPart.attachToCancelComponent(fCancelButton);
        fProgressMonitorPart.setVisible(true);
      }
    }

    return savedState;
  }

  @Override
  protected void buttonPressed(int buttonId) {
    switch (buttonId) {
      case IDialogConstants.CANCEL_ID:
        if (fActiveRunningOperations == 0) {
          close();
        }
        break;
      default:
        if (performAction(buttonId)) {
          close();
        }
    }
  }

  protected Button createActionButton(Composite parent, int id, String label, boolean defaultButton) {
    Button actionButton = createButton(parent, id, label, defaultButton);
    fActionButtons.add(actionButton);
    return actionButton;
  }

  /**
   * Add buttons to the dialog's button bar. Subclasses may override.
   * 
   * @param parent the button bar composite
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    fCancelButton = createButton(
        parent,
        IDialogConstants.CANCEL_ID,
        IDialogConstants.CANCEL_LABEL,
        false);
  }

  /**
   * Creates the layout of the extended dialog window.
   * 
   * @param parent The parent composite
   * @return The created control
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite result = (Composite) super.createDialogArea(parent);

    fContents = createPageArea(result);
    fContents.setLayoutData(new GridData(GridData.FILL_BOTH));

    if (fUseEmbeddedProgressMonitorPart) {
      // Insert a progress monitor
      fProgressMonitorPart = new ProgressMonitorPart(result, new GridLayout(), SWT.DEFAULT);
      fProgressMonitorPart.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      fProgressMonitorPart.setVisible(false);
      applyDialogFont(fProgressMonitorPart);
    }

    Label separator = new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    return result;
  }

  //---- Setters and Getters --------------------------------------------------

  /**
   * Create the page area.
   * 
   * @param parent The parent composite
   * @return The created control
   */
  protected abstract Control createPageArea(Composite parent);

  //---- Operation stuff ------------------------------------------------------

  /**
   * @return Returns the cancel component that is to be used to cancel a long running operation.
   */
  protected Control getCancelComponent() {
    return fCancelButton;
  }

  @Override
  protected void handleShellCloseEvent() {
    if (okToClose()) {
      super.handleShellCloseEvent();
    }
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  /**
   * Hook called when the user has pressed the button to perform the dialog's action. If the method
   * returns <code>false</code> the dialog stays open. Otherwise the dialog is going to be closed.
   * 
   * @param buttonId Id of the button activated
   * @return If the method returns <code>false</code> the dialog stays open.
   */
  protected boolean performAction(int buttonId) {
    return true;
  }

  //---- UI state save and restoring ---------------------------------------------

  /**
   * Hook called when the user has pressed the button to cancel the dialog. If the method returns
   * <code>false</code> the dialog stays open. Otherwise the dialog is going to be closed.
   * 
   * @return If the method returns <code>false</code> the dialog stays open.
   */
  protected boolean performCancel() {
    return true;
  }

  /*
   * Restores the enable state of the given control.
   */
  protected void restoreEnableState(Control w, @SuppressWarnings("rawtypes") HashMap h) {
    if (!w.isDisposed()) {
      Boolean b = (Boolean) h.get(w);
      if (b != null) {
        w.setEnabled(b.booleanValue());
      }
    }
  }

  /**
   * A long running operation triggered through the wizard was stopped either by user input or by
   * normal end.
   * 
   * @param savedState The saveState returned by <code>aboutToStart</code>.
   * @see #aboutToStart(boolean)
   */
  protected synchronized void stopped(Object savedState) {
    Assert.isTrue(savedState instanceof HashMap);
    Shell shell = getShell();
    if (shell != null) {
      if (fUseEmbeddedProgressMonitorPart) {
        fProgressMonitorPart.setVisible(false);
        fProgressMonitorPart.removeFromCancelComponent(fCancelButton);
      }

      @SuppressWarnings("rawtypes")
      HashMap state = (HashMap) savedState;
      restoreUIState(state);

      setDisplayCursor(shell.getDisplay(), null);
      fCancelButton.setCursor(null);
      Control focusControl = (Control) state.get(FOCUS_CONTROL);
      if (focusControl != null && !focusControl.isDisposed()) {
        focusControl.setFocus();
      }
    }
  }

  private MessageDialog createClosingDialog() {
    MessageDialog result = new MessageDialog(
        getShell(),
        SearchMessages.SearchDialogClosingDialog_title,
        null,
        SearchMessages.SearchDialogClosingDialog_message,
        MessageDialog.QUESTION,
        new String[] {IDialogConstants.OK_LABEL},
        0);
    return result;
  }

  private void restoreUIState(@SuppressWarnings("rawtypes") HashMap state) {
    restoreEnableState(fCancelButton, state);
    for (Iterator<Button> actionButtons = fActionButtons.iterator(); actionButtons.hasNext();) {
      Button button = actionButtons.next();
      restoreEnableState(button, state);
    }
    ControlEnableState pageState = (ControlEnableState) state.get("tabForm"); //$NON-NLS-1$
    pageState.restore();
  }

  private void saveEnableStateAndSet(Control w, HashMap<Object, Object> h, boolean enabled) {
    if (!w.isDisposed()) {
      h.put(w, new Boolean(w.isEnabled()));
      w.setEnabled(enabled);
    }
  }

  private HashMap<Object, Object> saveUIState(boolean keepCancelEnabled) {
    HashMap<Object, Object> savedState = new HashMap<Object, Object>(10);
    saveEnableStateAndSet(fCancelButton, savedState, keepCancelEnabled);
    for (Iterator<Button> actionButtons = fActionButtons.iterator(); actionButtons.hasNext();) {
      Button button = actionButtons.next();
      saveEnableStateAndSet(button, savedState, false);
    }
    savedState.put("tabForm", ControlEnableState.disable(fContents)); //$NON-NLS-1$

    return savedState;
  }

  private void setDisplayCursor(Display d, Cursor c) {
    Shell[] shells = d.getShells();
    for (int i = 0; i < shells.length; i++) {
      shells[i].setCursor(c);
    }
  }
}

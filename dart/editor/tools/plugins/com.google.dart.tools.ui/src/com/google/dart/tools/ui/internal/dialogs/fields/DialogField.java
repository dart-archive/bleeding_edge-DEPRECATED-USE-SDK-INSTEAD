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
package com.google.dart.tools.ui.internal.dialogs.fields;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * Base class of all dialog fields. Dialog fields manage controls together with the model,
 * independent from the creation time of the widgets. - support for automated layout. - enable /
 * disable, set focus a concept of the base class. DialogField have a label.
 */
public class DialogField {

  /**
   * Creates a spacer control.
   * 
   * @param parent The parent composite
   */
  public static Control createEmptySpace(Composite parent) {
    return createEmptySpace(parent, 1);
  }

  /**
   * Creates a spacer control with the given span. The composite is assumed to have
   * <code>MGridLayout</code> as layout.
   * 
   * @param parent The parent composite
   */
  public static Control createEmptySpace(Composite parent, int span) {
    Label label = new Label(parent, SWT.LEFT);
    GridData gd = new GridData();
    gd.horizontalAlignment = GridData.BEGINNING;
    gd.grabExcessHorizontalSpace = false;
    gd.horizontalSpan = span;
    gd.horizontalIndent = 0;
    gd.widthHint = 0;
    gd.heightHint = 0;
    label.setLayoutData(gd);
    return label;
  }

  protected static GridData gridDataForLabel(int span) {
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = span;
    return gd;
  }

  private Label fLabel;

  protected String fLabelText;

  private IDialogFieldListener fDialogFieldListener;

  private boolean fEnabled;

  public DialogField() {
    fEnabled = true;
    fLabel = null;
    fLabelText = ""; //$NON-NLS-1$
  }

  /**
   * Invocation of a dialog field change.
   */
  public void dialogFieldChanged() {
    if (fDialogFieldListener != null) {
      fDialogFieldListener.dialogFieldChanged(this);
    }
  }

  /**
   * Creates all controls of the dialog field and fills it to a composite. The composite is assumed
   * to have <code>MGridLayout</code> as layout. The dialog field will adjust its controls' spans to
   * the number of columns given. To be reimplemented by dialog field implementors.
   */
  public Control[] doFillIntoGrid(Composite parent, int nColumns) {
    assertEnoughColumns(nColumns);

    Label label = getLabelControl(parent);
    label.setLayoutData(gridDataForLabel(nColumns));

    return new Control[] {label};
  }

  // ------- layout helpers

  /**
   * Creates or returns the created label widget.
   * 
   * @param parent The parent composite or <code>null</code> if the widget has already been created.
   */
  public Label getLabelControl(Composite parent) {
    if (fLabel == null) {
      assertCompositeNotNull(parent);

      fLabel = new Label(parent, SWT.LEFT | SWT.WRAP);
      fLabel.setFont(parent.getFont());
      fLabel.setEnabled(fEnabled);
      if (fLabelText != null && !"".equals(fLabelText)) { //$NON-NLS-1$
        fLabel.setText(fLabelText);
      } else {
        // XXX: to avoid a 16 pixel wide empty label - revisit
        fLabel.setText("."); //$NON-NLS-1$
        fLabel.setVisible(false);
      }
    }
    return fLabel;
  }

  /**
   * Returns the number of columns of the dialog field. To be reimplemented by dialog field
   * implementors.
   */
  public int getNumberOfControls() {
    return 1;
  }

  /**
   * Gets the enable state of the dialog field.
   */
  public final boolean isEnabled() {
    return fEnabled;
  }

  /**
   * Posts <code>setFocus</code> to the display event queue.
   */
  public void postSetFocusOnDialogField(Display display) {
    if (display != null) {
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          setFocus();
        }
      });
    }
  }

  /**
   * Brings the UI in sync with the model. Only needed when model was changed in different thread
   * whil UI was lready created.
   */
  public void refresh() {
    updateEnableState();
  }

  /**
   * Defines the listener for this dialog field.
   */
  public final void setDialogFieldListener(IDialogFieldListener listener) {
    fDialogFieldListener = listener;
  }

  /**
   * Sets the enable state of the dialog field.
   */
  public final void setEnabled(boolean enabled) {
    if (enabled != fEnabled) {
      fEnabled = enabled;
      updateEnableState();
    }
  }

  /**
   * Tries to set the focus to the dialog field. Returns <code>true</code> if the dialog field can
   * take focus. To be reimplemented by dialog field implementors.
   */
  public boolean setFocus() {
    return false;
  }

  /**
   * Sets the label of the dialog field.
   */
  public void setLabelText(String labeltext) {
    fLabelText = labeltext;
    if (isOkToUse(fLabel)) {
      fLabel.setText(labeltext);
    }
  }

  protected final void assertCompositeNotNull(Composite comp) {
    Assert.isNotNull(comp, "uncreated control requested with composite null"); //$NON-NLS-1$
  }

  protected final void assertEnoughColumns(int nColumns) {
    Assert.isTrue(nColumns >= getNumberOfControls(), "given number of columns is too small"); //$NON-NLS-1$
  }

  /**
   * Tests is the control is not <code>null</code> and not disposed.
   */
  protected final boolean isOkToUse(Control control) {
    return (control != null) && (Display.getCurrent() != null) && !control.isDisposed();
  }

  /**
   * Called when the enable state changed. To be extended by dialog field implementors.
   */
  protected void updateEnableState() {
    if (fLabel != null) {
      fLabel.setEnabled(fEnabled);
    }
  }

}

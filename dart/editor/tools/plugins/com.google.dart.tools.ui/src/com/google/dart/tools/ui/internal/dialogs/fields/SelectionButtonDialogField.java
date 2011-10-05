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

import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Dialog Field containing a single button such as a radio or checkbox button.
 */
public class SelectionButtonDialogField extends DialogField {

  private Button fButton;
  private boolean fIsSelected;
  private DialogField[] fAttachedDialogFields;
  private final int fButtonStyle;

  /**
   * Creates a selection button. Allowed button styles: SWT.RADIO, SWT.CHECK, SWT.TOGGLE, SWT.PUSH
   */
  public SelectionButtonDialogField(int buttonStyle) {
    super();
    fIsSelected = false;
    fAttachedDialogFields = null;
    fButtonStyle = buttonStyle;
  }

  /**
   * Attaches a field to the selection state of the selection button. The attached field will be
   * disabled if the selection button is not selected.
   */
  public void attachDialogField(DialogField dialogField) {
    attachDialogFields(new DialogField[] {dialogField});
  }

  /**
   * Attaches fields to the selection state of the selection button. The attached fields will be
   * disabled if the selection button is not selected.
   */
  public void attachDialogFields(DialogField[] dialogFields) {
    fAttachedDialogFields = dialogFields;
    for (int i = 0; i < dialogFields.length; i++) {
      dialogFields[i].setEnabled(fIsSelected);
    }
  }

  /*
   * @see DialogField#doFillIntoGrid
   */
  @Override
  public Control[] doFillIntoGrid(Composite parent, int nColumns) {
    assertEnoughColumns(nColumns);

    Button button = getSelectionButton(parent);
    GridData gd = new GridData();
    gd.horizontalSpan = nColumns;
    gd.horizontalAlignment = GridData.FILL;
    if (fButtonStyle == SWT.PUSH) {
      gd.widthHint = SWTUtil.getButtonWidthHint(button);
    }

    button.setLayoutData(gd);

    return new Control[] {button};
  }

  /*
   * @see DialogField#getNumberOfControls
   */
  @Override
  public int getNumberOfControls() {
    return 1;
  }

  /**
   * Returns the selection button widget. When called the first time, the widget will be created.
   * 
   * @param group The parent composite when called the first time, or <code>null</code> after.
   */
  public Button getSelectionButton(Composite group) {
    if (fButton == null) {
      assertCompositeNotNull(group);

      fButton = new Button(group, fButtonStyle);
      fButton.setFont(group.getFont());
      fButton.setText(fLabelText);
      fButton.setEnabled(isEnabled());
      fButton.setSelection(fIsSelected);
      fButton.addSelectionListener(new SelectionListener() {
        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
          doWidgetSelected(e);
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
          doWidgetSelected(e);
        }
      });
    }
    return fButton;
  }

  /**
   * Returns <code>true</code> is teh gived field is attached to the selection button.
   */
  public boolean isAttached(DialogField editor) {
    if (fAttachedDialogFields != null) {
      for (int i = 0; i < fAttachedDialogFields.length; i++) {
        if (fAttachedDialogFields[i] == editor) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the selection state of the button.
   */
  public boolean isSelected() {
    return fIsSelected;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField#refresh()
   */
  @Override
  public void refresh() {
    super.refresh();
    if (isOkToUse(fButton)) {
      fButton.setSelection(fIsSelected);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField#setLabelText
   * (java.lang.String)
   */
  @Override
  public void setLabelText(String labeltext) {
    fLabelText = labeltext;
    if (isOkToUse(fButton)) {
      fButton.setText(labeltext);
    }
  }

  /**
   * Sets the selection state of the button.
   */
  public void setSelection(boolean selected) {
    changeValue(selected);
    if (isOkToUse(fButton)) {
      fButton.setSelection(selected);
    }
  }

  /*
   * @see DialogField#updateEnableState
   */
  @Override
  protected void updateEnableState() {
    super.updateEnableState();
    if (isOkToUse(fButton)) {
      fButton.setEnabled(isEnabled());
    }
  }

  private void changeValue(boolean newState) {
    if (fIsSelected != newState) {
      fIsSelected = newState;
      if (fAttachedDialogFields != null) {
        boolean focusSet = false;
        for (int i = 0; i < fAttachedDialogFields.length; i++) {
          fAttachedDialogFields[i].setEnabled(fIsSelected);
          if (fIsSelected && !focusSet) {
            focusSet = fAttachedDialogFields[i].setFocus();
          }
        }
      }
      dialogFieldChanged();
    } else if (fButtonStyle == SWT.PUSH) {
      dialogFieldChanged();
    }
  }

  private void doWidgetSelected(SelectionEvent e) {
    if (isOkToUse(fButton)) {
      changeValue(fButton.getSelection());
    }
  }

}

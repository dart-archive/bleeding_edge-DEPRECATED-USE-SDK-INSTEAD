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

import com.google.dart.tools.ui.internal.util.ControlContentAssistHelper;

import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog field containing a label and a text control.
 */
public class StringDialogField extends DialogField {

  protected static GridData gridDataForText(int span) {
    GridData gd = new GridData();
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = false;
    gd.horizontalSpan = span;
    return gd;
  }

  private String fText;
  private Text fTextControl;
  private ModifyListener fModifyListener;

  private IContentAssistProcessor fContentAssistProcessor;

  public StringDialogField() {
    super();
    fText = ""; //$NON-NLS-1$
  }

  /*
   * @see DialogField#doFillIntoGrid
   */
  @Override
  public Control[] doFillIntoGrid(Composite parent, int nColumns) {
    assertEnoughColumns(nColumns);

    Label label = getLabelControl(parent);
    label.setLayoutData(gridDataForLabel(1));
    Text text = getTextControl(parent);
    text.setLayoutData(gridDataForText(nColumns - 1));

    return new Control[] {label, text};
  }

  // ------- layout helpers

  public IContentAssistProcessor getContentAssistProcessor() {
    return fContentAssistProcessor;
  }

  /*
   * @see DialogField#getNumberOfControls
   */
  @Override
  public int getNumberOfControls() {
    return 2;
  }

  /**
   * Gets the text. Can not be <code>null</code>
   */
  public String getText() {
    return fText;
  }

  // ------- focus methods

  /**
   * Creates or returns the created text control.
   *
   * @param parent The parent composite or <code>null</code> when the widget has already been
   *          created.
   */
  public Text getTextControl(Composite parent) {
    if (fTextControl == null) {
      assertCompositeNotNull(parent);
      fModifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
          doModifyText(e);
        }
      };

      fTextControl = new Text(parent, SWT.SINGLE | SWT.BORDER);
      // moved up due to 1GEUNW2
      fTextControl.setText(fText);
      fTextControl.setFont(parent.getFont());
      fTextControl.addModifyListener(fModifyListener);

      fTextControl.setEnabled(isEnabled());
      if (fContentAssistProcessor != null) {
        ControlContentAssistHelper.createTextContentAssistant(fTextControl, fContentAssistProcessor);
      }
    }
    return fTextControl;
  }

  // ------- ui creation

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.wst.jsdt.internal.ui.wizards.dialogfields.DialogField#refresh()
   */
  @Override
  public void refresh() {
    super.refresh();
    if (isOkToUse(fTextControl)) {
      setTextWithoutUpdate(fText);
    }
  }

  public void setContentAssistProcessor(IContentAssistProcessor processor) {
    fContentAssistProcessor = processor;
    if (fContentAssistProcessor != null && isOkToUse(fTextControl)) {
      ControlContentAssistHelper.createTextContentAssistant(fTextControl, fContentAssistProcessor);
    }
  }

  /*
   * @see DialogField#setFocus
   */
  @Override
  public boolean setFocus() {
    if (isOkToUse(fTextControl)) {
      fTextControl.setFocus();
      fTextControl.setSelection(0, fTextControl.getText().length());
    }
    return true;
  }

  // ------ enable / disable management

  /**
   * Sets the text. Triggers a dialog-changed event.
   */
  public void setText(String text) {
    fText = text;
    if (isOkToUse(fTextControl)) {
      fTextControl.setText(text);
    } else {
      dialogFieldChanged();
    }
  }

  // ------ text access

  public void setTextFieldEditable(boolean editable) {
    fTextControl.setEditable(editable);
  }

  /**
   * Sets the text without triggering a dialog-changed event.
   */
  public void setTextWithoutUpdate(String text) {
    fText = text;
    if (isOkToUse(fTextControl)) {
      fTextControl.removeModifyListener(fModifyListener);
      fTextControl.setText(text);
      fTextControl.addModifyListener(fModifyListener);
    }
  }

  /*
   * @see DialogField#updateEnableState
   */
  @Override
  protected void updateEnableState() {
    super.updateEnableState();
    if (isOkToUse(fTextControl)) {
      fTextControl.setEnabled(isEnabled());
    }
  }

  private void doModifyText(ModifyEvent e) {
    if (isOkToUse(fTextControl)) {
      fText = fTextControl.getText();
    }
    dialogFieldChanged();
  }

}

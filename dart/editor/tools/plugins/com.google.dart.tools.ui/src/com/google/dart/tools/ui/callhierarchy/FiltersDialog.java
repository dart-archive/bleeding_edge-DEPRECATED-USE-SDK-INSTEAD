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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.ui.internal.callhierarchy.CallHierarchy;
import com.google.dart.tools.ui.internal.dialogs.StatusInfo;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

class FiltersDialog extends StatusDialog {
  private Label namesHelpText;
  private Button filterOnNames;
  private Text names;
  private Text maxCallDepth;

  protected FiltersDialog(Shell parentShell) {
    super(parentShell);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(CallHierarchyMessages.FiltersDialog_filter);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        newShell,
        DartHelpContextIds.CALL_HIERARCHY_FILTERS_DIALOG);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    createNamesArea(composite);
    new Label(composite, SWT.NONE); // Filler
    createMaxCallDepthArea(composite);

    updateUIFromFilter();

    return composite;
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  /**
   * Updates the filter from the UI state. Must be done here rather than by extending open() because
   * after super.open() is called, the widgetry is disposed.
   */
  @Override
  protected void okPressed() {
    if (!isMaxCallDepthValid()) {
      if (maxCallDepth.forceFocus()) {
        maxCallDepth.setSelection(0, maxCallDepth.getCharCount());
        maxCallDepth.showSelection();
      }
    }

    updateFilterFromUI();
    super.okPressed();
  }

  /**
   * Creates a check box button with the given parent and text.
   * 
   * @param parent the parent composite
   * @param text the text for the check box
   * @param grabRow <code>true</code>to grab the remaining horizontal space, <code>false</code>
   *          otherwise
   * @return the check box button
   */
  private Button createCheckbox(Composite parent, String text, boolean grabRow) {
    Button button = new Button(parent, SWT.CHECK);
    button.setFont(parent.getFont());

    if (grabRow) {
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
      button.setLayoutData(gridData);
    }

    button.setText(text);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        validateInput();
        updateEnabledState();
      }
    });

    return button;
  }

  private void createMaxCallDepthArea(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setFont(parent.getFont());
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    composite.setLayout(layout);

    Label label = new Label(composite, SWT.NONE);
    label.setFont(composite.getFont());
    label.setText(CallHierarchyMessages.FiltersDialog_maxCallDepth);

    maxCallDepth = new Text(composite, SWT.SINGLE | SWT.BORDER);
    maxCallDepth.setFont(composite.getFont());
    maxCallDepth.setTextLimit(6);
    maxCallDepth.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        validateInput();
      }
    });

    GridData gridData = new GridData();
    gridData.widthHint = convertWidthInCharsToPixels(10);
    maxCallDepth.setLayoutData(gridData);
  }

  private void createNamesArea(Composite parent) {
    filterOnNames = createCheckbox(parent, CallHierarchyMessages.FiltersDialog_filterOnNames, true);

    names = new Text(parent, SWT.SINGLE | SWT.BORDER);
    names.setFont(parent.getFont());
    names.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        validateInput();
      }
    });

    GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
    gridData.widthHint = convertWidthInCharsToPixels(60);
    names.setLayoutData(gridData);

    namesHelpText = new Label(parent, SWT.LEFT);
    namesHelpText.setFont(parent.getFont());
    namesHelpText.setText(CallHierarchyMessages.FiltersDialog_filterOnNamesSubCaption);
  }

  private boolean isMaxCallDepthValid() {
    String text = maxCallDepth.getText();
    if (text.length() == 0) {
      return false;
    }

    try {
      int maxCallDepth = Integer.parseInt(text);

      return (maxCallDepth >= 1 && maxCallDepth <= 99);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Updates the enabled state of the widgetry.
   */
  private void updateEnabledState() {
    names.setEnabled(filterOnNames.getSelection());
    namesHelpText.setEnabled(filterOnNames.getSelection());
  }

  /**
   * Updates the given filter from the UI state.
   */
  private void updateFilterFromUI() {
    int maxCallDepth = Integer.parseInt(this.maxCallDepth.getText());

    CallHierarchyUI.getDefault().setMaxCallDepth(maxCallDepth);
    CallHierarchy.getDefault().setFilters(names.getText());
    CallHierarchy.getDefault().setFilterEnabled(filterOnNames.getSelection());
  }

  /**
   * Updates the UI state from the given filter.
   */
  private void updateUIFromFilter() {
    maxCallDepth.setText(String.valueOf(CallHierarchyUI.getDefault().getMaxCallDepth()));
    names.setText(CallHierarchy.getDefault().getFilters());
    filterOnNames.setSelection(CallHierarchy.getDefault().isFilterEnabled());
    updateEnabledState();
  }

  private void validateInput() {
    StatusInfo status = new StatusInfo();
    if (!isMaxCallDepthValid()) {
      status.setError(CallHierarchyMessages.FiltersDialog_messageMaxCallDepthInvalid);
    }
    updateStatus(status);
  }
}

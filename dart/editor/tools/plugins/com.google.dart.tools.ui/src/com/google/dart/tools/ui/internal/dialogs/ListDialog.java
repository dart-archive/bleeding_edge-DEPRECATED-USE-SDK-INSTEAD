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
package com.google.dart.tools.ui.internal.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.SelectionDialog;

public class ListDialog extends SelectionDialog {

  private IStructuredContentProvider fContentProvider;
  private ILabelProvider fLabelProvider;
  private Object fInput;
  private TableViewer fTableViewer;
  private boolean fAddCancelButton;
  private final int fShellStyle;

  public ListDialog(Shell parent, int shellStyle) {
    super(parent);
    fAddCancelButton = false;
    fShellStyle = shellStyle;
  }

  @Override
  public void create() {
    setShellStyle(fShellStyle);
    super.create();
  }

  public TableViewer getTableViewer() {
    return fTableViewer;
  }

  public boolean hasFilters() {
    return fTableViewer.getFilters() != null && fTableViewer.getFilters().length != 0;
  }

  public void setAddCancelButton(boolean addCancelButton) {
    fAddCancelButton = addCancelButton;
  }

  public void setContentProvider(IStructuredContentProvider sp) {
    fContentProvider = sp;
  }

  public void setInput(Object input) {
    fInput = input;
  }

  public void setLabelProvider(ILabelProvider lp) {
    fLabelProvider = lp;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    if (!fAddCancelButton) {
      createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    } else {
      super.createButtonsForButtonBar(parent);
    }
  }

  @Override
  protected Control createDialogArea(Composite container) {
    Composite parent = (Composite) super.createDialogArea(container);
    createMessageArea(parent);
    fTableViewer = new TableViewer(parent, getTableStyle());
    fTableViewer.setContentProvider(fContentProvider);
    Table table = fTableViewer.getTable();
    fTableViewer.setLabelProvider(fLabelProvider);
    fTableViewer.setInput(fInput);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = convertWidthInCharsToPixels(55);
    gd.heightHint = convertHeightInCharsToPixels(15);
    table.setLayoutData(gd);
    applyDialogFont(parent);
    return parent;
  }

  @Override
  protected Label createMessageArea(Composite composite) {
    Label label = new Label(composite, SWT.WRAP);
    label.setText(getMessage());
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = convertWidthInCharsToPixels(55);
    label.setLayoutData(gd);
    applyDialogFont(label);
    return label;
  }

  protected int getTableStyle() {
    return SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
  }
}

/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.web.pubspec;

import com.google.dart.tools.core.generator.DartIdentifierUtil;
import com.google.dart.tools.core.pub.PubPackageManager;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import java.util.Arrays;

/**
 * Selection dialog to add a pub package as a dependency to the pubspec.
 */
public class PackageSelectionDialog extends ElementListSelectionDialog {

  private Object[] elements;

  private static int RELOAD_BUTTON_ID = IDialogConstants.CLIENT_ID + 1;

  public PackageSelectionDialog(Shell shell, Object[] elements) {
    super(shell, new LabelProvider());
    setElements(elements);
    this.elements = elements;
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (RELOAD_BUTTON_ID == buttonId) {
      reloadPressed();
    }
    super.buttonPressed(buttonId);
  }

  @Override
  protected void computeResult() {
    if (getSelectedElements().length > 0) {
      setResult(Arrays.asList(getSelectedElements()));
    } else {
      setResult(Arrays.asList(new String[] {getFilter()}));
    }
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, RELOAD_BUTTON_ID, "Reload", false);
    super.createButtonsForButtonBar(parent);
  }

  @Override
  protected Control createDialogArea(Composite parent) {

    // create a composite with standard margins and spacing
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    applyDialogFont(composite);

    createMessageArea(composite);
    Text filterText = createFilterText(composite);
    filterText.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        Status status = new Status(IStatus.OK, PlatformUI.PLUGIN_ID, IStatus.OK, "", //$NON-NLS-1$
            null);
        if (!getFilter().isEmpty() && !isValidDartIdentifier(getFilter())) {
          status = new Status(
              IStatus.ERROR,
              PlatformUI.PLUGIN_ID,
              IStatus.ERROR,
              "Not a valid identifier",
              null);
        }
        updateStatus(status);
      }
    });

    createFilteredList(composite);

    setListElements(elements);

    setSelection(getInitialElementSelections().toArray());

    return composite;
  }

  /**
   * Validates the current selection and updates the status line accordingly.
   * 
   * @return boolean <code>true</code> if the current selection is valid.
   */
  @Override
  protected boolean validateCurrentSelection() {
    Assert.isNotNull(fFilteredList);

    IStatus status;
    Object[] elements = getSelectedElements();

    if (elements.length > 0) {
      status = new Status(IStatus.OK, PlatformUI.PLUGIN_ID, IStatus.OK, "", //$NON-NLS-1$
          null);
    } else {
      if (fFilteredList.isEmpty()) {
        status = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.ERROR, "", null);
      } else {
        String string = getFilter();
        if (!string.isEmpty()) {
          if (isValidDartIdentifier(string)) {
            status = new Status(
                IStatus.WARNING,
                PlatformUI.PLUGIN_ID,
                IStatus.ERROR,
                "Selection not in the list",
                null);
          } else {
            status = new Status(
                IStatus.ERROR,
                PlatformUI.PLUGIN_ID,
                IStatus.ERROR,
                "Not a valid identifier",
                null);
          }
        } else {
          status = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.OK, "", //$NON-NLS-1$
              null);
        }
      }
    }
    updateStatus(status);

    return status.isOK() || status.matches(Status.WARNING);
  }

  private boolean isValidDartIdentifier(String string) {
    if (DartIdentifierUtil.validateIdentifier(string) == Status.OK_STATUS) {
      return true;
    }
    return false;
  }

  private void reloadPressed() {
    PubPackageManager.getInstance().startPackageListFromPubJob();
  }

}

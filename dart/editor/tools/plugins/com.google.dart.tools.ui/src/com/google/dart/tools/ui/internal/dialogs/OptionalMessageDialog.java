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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUIMessages;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * This is a <code>MessageDialog</code> which allows the user to choose that the dialog isn't shown
 * again the next time.
 */
public class OptionalMessageDialog extends MessageDialog {

  // String constants for widgets
  private static final String CHECKBOX_TEXT = DartUIMessages.OptionalMessageDialog_dontShowAgain;

  // Dialog store id constants
  private static final String STORE_ID = "OptionalMessageDialog.hide."; //$NON-NLS-1$

  public static final int NOT_SHOWN = IDialogConstants.CLIENT_ID + 1;

  /**
   * Clears all remembered information about hidden dialogs
   */
  public static void clearAllRememberedStates() {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettings();
    settings.addNewSection(STORE_ID);
  }

  /**
   * Answers whether the optional dialog is enabled and should be shown.
   */
  public static boolean isDialogEnabled(String key) {
    IDialogSettings settings = getDialogSettings();
    return !settings.getBoolean(key);
  }

  /**
   * Opens the dialog but only if the user hasn't choosen to hide it. Returns <code>NOT_SHOWN</code>
   * if the dialog was not shown.
   */
  public static int open(String id, Shell parent, String title, Image titleImage, String message,
      int dialogType, String[] buttonLabels, int defaultButtonIndex) {
    if (!isDialogEnabled(id)) {
      return OptionalMessageDialog.NOT_SHOWN;
    }

    MessageDialog dialog = new OptionalMessageDialog(id, parent, title, titleImage, message,
        dialogType, buttonLabels, defaultButtonIndex);
    return dialog.open();
  }

  /**
   * Sets whether the optional dialog is enabled and should be shown.
   */
  public static void setDialogEnabled(String key, boolean isEnabled) {
    IDialogSettings settings = getDialogSettings();
    settings.put(key, !isEnabled);
  }

  /**
   * Returns this dialog
   * 
   * @return the settings to be used
   */
  private static IDialogSettings getDialogSettings() {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettings();
    settings = settings.getSection(STORE_ID);
    if (settings == null) {
      settings = DartToolsPlugin.getDefault().getDialogSettings().addNewSection(STORE_ID);
    }
    return settings;
  }

  // --------------- Configuration handling --------------

  private Button fHideDialogCheckBox;

  private String fId;

  protected OptionalMessageDialog(String id, Shell parent, String title, Image titleImage,
      String message, int dialogType, String[] buttonLabels, int defaultButtonIndex) {
    super(parent, title, titleImage, message, dialogType, buttonLabels, defaultButtonIndex);
    fId = id;
  }

  @Override
  protected Control createCustomArea(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    fHideDialogCheckBox = new Button(composite, SWT.CHECK | SWT.LEFT);
    fHideDialogCheckBox.setText(CHECKBOX_TEXT);
    fHideDialogCheckBox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        setDialogEnabled(fId, !((Button) e.widget).getSelection());
      }
    });
    applyDialogFont(fHideDialogCheckBox);
    return fHideDialogCheckBox;
  }
}

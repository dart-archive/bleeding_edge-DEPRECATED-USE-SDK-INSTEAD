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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Class to show the Search In dialog.
 * <p>
 * TODO: remove
 */
class SearchInDialog extends TrayDialog {

  private Button[] includeMasks;
  private IDialogSettings settings;
  private int includeMask;
  private boolean includeMaskChanged = false;

  /**
   * Section ID for the SearchInDialog class.
   */
  private static final String DIALOG_SETTINGS_SECTION = "SearchInDialog"; //$NON-NLS-1$	
  private static final String SEARCH_IN_SOURCES = "SearchInSources"; //$NON-NLS-1$
  private static final String SEARCH_IN_PROJECTS = "SearchInProjects"; //$NON-NLS-1$
  private static final String SEARCH_IN_APPLIBS = "SearchInAppLibs"; //$NON-NLS-1$
  private static final String SEARCH_IN_JRE = "SearchInJRE"; //$NON-NLS-1$
  private String[] fKeys = new String[] {
      SEARCH_IN_SOURCES, SEARCH_IN_PROJECTS, SEARCH_IN_JRE, SEARCH_IN_APPLIBS};

  public SearchInDialog(Shell parentShell) {
    super(parentShell);
    settings = DartToolsPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS_SECTION);
    if (settings == null) {
      settings = DartToolsPlugin.getDefault().getDialogSettings().addNewSection(
          DIALOG_SETTINGS_SECTION);
      settings.put(SEARCH_IN_SOURCES, true);
      settings.put(SEARCH_IN_PROJECTS, true);
      settings.put(SEARCH_IN_JRE, true);
      settings.put(SEARCH_IN_APPLIBS, true);
    }
//    includeMask = getInt(fKeys);
  }

  /**
   * Indicates whether the include mask has changed.
   * 
   * @return the includeMaskChanged <code>true</code> if the include mask has changed,
   *         <code>false</code> otherwise
   */
  public boolean isIncludeMaskChanged() {
    return includeMaskChanged;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(CallHierarchyMessages.SearchInDialog_title);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        newShell,
        DartHelpContextIds.CALL_HIERARCHY_SEARCH_IN_DIALOG);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    Control includeMask = createIncludeMask(composite);
    includeMask.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

    return composite;
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  @Override
  protected void okPressed() {
    int mask = getIncludeMask();
    if (mask != includeMask) {
      includeMask = mask;
      for (int i = 0; i < includeMasks.length; i++) {
        settings.put(fKeys[i], includeMasks[i].getSelection());
      }
      includeMaskChanged = true;
    } else {
      includeMaskChanged = false;
    }
    super.okPressed();
  }

  /**
   * Updates the enablement of OK button.
   */
  protected void updateOKStatus() {
    boolean isValidMask = getIncludeMask() != 0;
    getButton(OK).setEnabled(isValidMask);
  }

  /**
   * Returns the include mask.
   * 
   * @return the include mask
   */
  int getIncludeMask() {
    if (includeMasks == null || includeMasks[0].isDisposed()) {
      return includeMask;
    }
    int mask = 0;
    for (int i = 0; i < includeMasks.length; i++) {
      Button button = includeMasks[i];
      if (button.getSelection()) {
        mask |= getIntData(button);
      }
    }
    return mask;
  }

//  /**
//   * Creates and returns the button.
//   * 
//   * @param parent the parent composite
//   * @param style the style of control to construct
//   * @param text the text for the button
//   * @param data the widget data
//   * @param isSelected the new selection state
//   * @return the button created
//   */
//  private Button createButton(Composite parent, int style, String text, int data, boolean isSelected) {
//    Button button = new Button(parent, style);
//    button.setText(text);
//    button.setData(new Integer(data));
//    button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
//    button.setSelection(isSelected);
//    return button;
//  }

  /**
   * Creates the search in options.
   * 
   * @param parent the parent composite
   * @return the group control
   */
  private Control createIncludeMask(Composite parent) {
    Group result = new Group(parent, SWT.NONE);
//    result.setText(SearchMessages.SearchPage_searchIn_label);
//    result.setLayout(new GridLayout(4, false));
//    fIncludeMasks = new Button[] {
//        createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_sources,
//            JavaSearchScopeFactory.SOURCES, fSettings.getBoolean(SEARCH_IN_SOURCES)),
//        createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_projects,
//            JavaSearchScopeFactory.PROJECTS, fSettings.getBoolean(SEARCH_IN_PROJECTS)),
//        createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_jre,
//            JavaSearchScopeFactory.JRE, fSettings.getBoolean(SEARCH_IN_JRE)),
//        createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_libraries,
//            JavaSearchScopeFactory.LIBS, fSettings.getBoolean(SEARCH_IN_APPLIBS)),};
//
//    SelectionAdapter listener = new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        updateOKStatus();
//      }
//    };
//    for (int i = 0; i < fIncludeMasks.length; i++) {
//      includeMasks[i].addSelectionListener(listener);
//    }

    return result;
  }

//  /**
//   * Returns the integer value of the strings.
//   * 
//   * @param str the array of strings
//   * @return the integer value of the strings
//   */
//  private int getInt(String[] str) {
//    boolean value;
//    int mask = 0;
//    int val = 0;
//    for (int i = 0; i < str.length; i++) {
//      value = settings.getBoolean(str[i]);
//      if (value) {
//        switch (i) {
////          case 0:
////            val = JavaSearchScopeFactory.SOURCES;
////            break;
////          case 1:
////            val = JavaSearchScopeFactory.PROJECTS;
////            break;
////          case 2:
////            val = JavaSearchScopeFactory.JRE;
////            break;
////          case 3:
////            val = JavaSearchScopeFactory.LIBS;
//        }
//        mask |= val;
//      }
//    }
//    return mask;
//  }

  /**
   * Returns the value of the given button.
   * 
   * @param button the button for which to fetch value
   * @return the value of the button
   */
  private int getIntData(Button button) {
    return ((Integer) button.getData()).intValue();
  }

}

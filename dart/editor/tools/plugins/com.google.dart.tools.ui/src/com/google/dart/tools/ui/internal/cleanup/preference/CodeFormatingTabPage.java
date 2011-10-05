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
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.internal.cleanup.AbstractCleanUp;
import com.google.dart.tools.ui.internal.cleanup.CleanUpConstants;
import com.google.dart.tools.ui.internal.cleanup.CodeFormatCleanUp;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public final class CodeFormatingTabPage extends AbstractCleanUpTabPage {

  public static final String ID = "org.eclipse.jdt.ui.cleanup.tabpage.code_formatting"; //$NON-NLS-1$

  private Map<String, String> fValues;
  private CleanUpPreview fPreview;

  public CodeFormatingTabPage() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setWorkingValues(Map<String, String> workingValues) {
    super.setWorkingValues(workingValues);
    fValues = workingValues;
  }

  @Override
  protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
    return new AbstractCleanUp[] {new CodeFormatCleanUp(values)};//, new SortMembersCleanUp(values)};
  }

  @Override
  protected DartPreview doCreateJavaPreview(Composite parent) {
    fPreview = (CleanUpPreview) super.doCreateJavaPreview(parent);
    fPreview.showInvisibleCharacters(true);
    fPreview.setFormat(CleanUpOptions.TRUE.equals(fValues.get(CleanUpConstants.FORMAT_SOURCE_CODE)));
    fPreview.setCorrectIndentation(CleanUpOptions.TRUE.equals(fValues.get(CleanUpConstants.FORMAT_CORRECT_INDENTATION)));
    return fPreview;
  }

  @Override
  protected void doCreatePreferences(Composite composite, int numColumns) {

    Group group = createGroup(numColumns, composite,
        CleanUpMessages.CodeFormatingTabPage_GroupName_Formatter);

    if (!isSaveAction()) {
      final CheckboxPreference format = createCheckboxPref(group, numColumns,
          CleanUpMessages.CodeFormatingTabPage_CheckboxName_FormatSourceCode,
          CleanUpConstants.FORMAT_SOURCE_CODE, CleanUpModifyDialog.FALSE_TRUE);
      registerPreference(format);
      format.addObserver(new Observer() {
        @Override
        public void update(Observable o, Object arg) {
          fPreview.setFormat(format.getChecked());
          fPreview.update();
        }
      });
    }

    final CheckboxPreference whiteSpace = createCheckboxPref(group, numColumns,
        CleanUpMessages.CodeFormatingTabPage_RemoveTrailingWhitespace_checkbox_text,
        CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES, CleanUpModifyDialog.FALSE_TRUE);
    intent(group);
    final RadioPreference allPref = createRadioPref(group, 1,
        CleanUpMessages.CodeFormatingTabPage_RemoveTrailingWhitespace_all_radio,
        CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL, CleanUpModifyDialog.FALSE_TRUE);
    final RadioPreference ignoreEmptyPref = createRadioPref(group, 1,
        CleanUpMessages.CodeFormatingTabPage_RemoveTrailingWhitespace_ignoreEmpty_radio,
        CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY,
        CleanUpModifyDialog.FALSE_TRUE);
    registerSlavePreference(whiteSpace, new RadioPreference[] {allPref, ignoreEmptyPref});

    final CheckboxPreference correctIndentation = createCheckboxPref(group, numColumns,
        CleanUpMessages.CodeFormatingTabPage_correctIndentation_checkbox_text,
        CleanUpConstants.FORMAT_CORRECT_INDENTATION, CleanUpModifyDialog.FALSE_TRUE);
    registerPreference(correctIndentation);
    correctIndentation.addObserver(new Observer() {
      @Override
      public void update(Observable o, Object arg) {
        fPreview.setCorrectIndentation(correctIndentation.getChecked());
        fPreview.update();
      }
    });

//    Group sortMembersGroup = createGroup(numColumns, composite,
//        CleanUpMessages.CodeFormatingTabPage_SortMembers_GroupName);
//
//    final CheckboxPreference sortMembersPref = createCheckboxPref(sortMembersGroup, numColumns,
//        CleanUpMessages.CodeFormatingTabPage_SortMembers_CheckBoxLabel,
//        CleanUpConstants.SORT_MEMBERS, CleanUpModifyDialog.FALSE_TRUE);
//    intent(sortMembersGroup);
//    final RadioPreference sortAllPref = createRadioPref(sortMembersGroup, numColumns - 1,
//        CleanUpMessages.CodeFormatingTabPage_SortMembersFields_CheckBoxLabel,
//        CleanUpConstants.SORT_MEMBERS_ALL, CleanUpModifyDialog.FALSE_TRUE);
//    intent(sortMembersGroup);
//    final Button nullRadio = new Button(sortMembersGroup, SWT.RADIO);
//    nullRadio.setText(CleanUpMessages.CodeFormatingTabPage_SortMembersExclusive_radio0);
//    nullRadio.setLayoutData(createGridData(numColumns - 1, GridData.FILL_HORIZONTAL, SWT.DEFAULT));
//    nullRadio.setFont(composite.getFont());
//    intent(sortMembersGroup);
//    final Label warningImage = new Label(sortMembersGroup, SWT.LEFT | SWT.WRAP);
//    warningImage.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
//    warningImage.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
//    final Label warningLabel = createLabel(numColumns - 2, sortMembersGroup,
//        CleanUpMessages.CodeFormatingTabPage_SortMembersSemanticChange_warning);
//
//    registerSlavePreference(sortMembersPref, new RadioPreference[] {sortAllPref});
//    sortMembersPref.addObserver(new Observer() {
//      @Override
//      public void update(Observable o, Object arg) {
//        nullRadio.setEnabled(sortMembersPref.getChecked());
//
//        boolean warningEnabled = sortMembersPref.getChecked() && sortAllPref.getChecked();
//        warningImage.setEnabled(warningEnabled);
//        warningLabel.setEnabled(warningEnabled);
//      }
//    });
//    sortAllPref.addObserver(new Observer() {
//      @Override
//      public void update(Observable o, Object arg) {
//        boolean warningEnabled = sortMembersPref.getChecked() && sortAllPref.getChecked();
//        warningImage.setEnabled(warningEnabled);
//        warningLabel.setEnabled(warningEnabled);
//      }
//    });
//    nullRadio.setEnabled(sortMembersPref.getChecked());
//    nullRadio.setSelection(CleanUpOptions.FALSE.equals(fValues.get(CleanUpConstants.SORT_MEMBERS_ALL)));
//    boolean warningEnabled = sortMembersPref.getChecked() && sortAllPref.getChecked();
//    warningImage.setEnabled(warningEnabled);
//    warningLabel.setEnabled(warningEnabled);
//
//    createLabel(numColumns, sortMembersGroup,
//        CleanUpMessages.CodeFormatingTabPage_SortMembers_Description);
  }

}

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
package com.google.dart.tools.ui.internal.text.editor.saveparticipant;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.cleanup.CleanUpConstants;
import com.google.dart.tools.ui.internal.cleanup.CleanUpRegistry.CleanUpTabPageDescriptor;
import com.google.dart.tools.ui.internal.cleanup.preference.CleanUpTabPage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.Map;

public class SaveActionSelectionDialog extends CleanUpSelectionDialog {

  private static final String PREFERENCE_KEY = "clean_up_save_particpant_modify_dialog"; //$NON-NLS-1$

  public SaveActionSelectionDialog(Shell parentShell, Map<String, String> settings) {
    super(
        parentShell,
        settings,
        SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_CleanUpSaveParticipantConfiguration_Title);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    GridLayout layout = (GridLayout) parent.getLayout();
    layout.numColumns++;
    layout.makeColumnsEqualWidth = false;
    Label label = new Label(parent, SWT.NONE);
    GridData data = new GridData();
    data.widthHint = layout.horizontalSpacing;
    label.setLayoutData(data);
    super.createButtonsForButtonBar(parent);
  }

  @Override
  protected NamedCleanUpTabPage[] createTabPages(Map<String, String> workingValues) {
    CleanUpTabPageDescriptor[] descriptors = DartToolsPlugin.getDefault().getCleanUpRegistry().getCleanUpTabPageDescriptors(
        CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS);

    NamedCleanUpTabPage[] result = new NamedCleanUpTabPage[descriptors.length];

    for (int i = 0; i < descriptors.length; i++) {
      String name = descriptors[i].getName();
      CleanUpTabPage page = descriptors[i].createTabPage();

      page.setOptionsKind(CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS);
      page.setModifyListener(this);
      page.setWorkingValues(workingValues);

      result[i] = new NamedCleanUpTabPage(name, page);
    }

    return result;
  }

  @Override
  protected String getEmptySelectionMessage() {
    return SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_SelectAnAction_Error;
  }

  @Override
  protected String getPreferenceKeyPrefix() {
    return PREFERENCE_KEY;
  }

  @Override
  protected String getSelectionCountMessage(int selectionCount, int size) {
    return Messages.format(
        SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_XofYSelected_Label,
        new Object[] {new Integer(selectionCount), new Integer(size)});
  }
}

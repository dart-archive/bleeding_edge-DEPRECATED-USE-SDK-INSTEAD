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

import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.dialogs.TypeSelectionExtension;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * A type selection dialog used for opening types.
 */
public class OpenTypeSelectionDialog extends FilteredTypesSelectionDialog {

  private static final String DIALOG_SETTINGS = "com.google.dart.tools.ui.dialogs.OpenTypeSelectionDialog2"; //$NON-NLS-1$

  public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context,
      SearchScope scope, int elementKinds) {
    this(parent, multi, context, scope, elementKinds, null);
  }

  public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context,
      SearchScope scope, int elementKinds, TypeSelectionExtension extension) {
    super(parent, multi, context, scope, elementKinds, extension);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#configureShell(org.eclipse
   * .swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell,
        IJavaHelpContextIds.OPEN_TYPE_DIALOG);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.dart.tools.ui.dialogs.FilteredTypesSelectionDialog#getDialogSettings ()
   */
  @Override
  protected IDialogSettings getDialogSettings() {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettings().getSection(
        DIALOG_SETTINGS);

    if (settings == null) {
      settings = DartToolsPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
    }

    return settings;
  }
}

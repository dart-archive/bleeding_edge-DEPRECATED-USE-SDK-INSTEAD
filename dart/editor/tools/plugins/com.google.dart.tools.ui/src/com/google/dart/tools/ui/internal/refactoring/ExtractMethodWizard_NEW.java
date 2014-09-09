/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.DartToolsPlugin;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ExtractMethodWizard_NEW extends ServerRefactoringWizard {

  static final String DIALOG_SETTING_SECTION = "ExtractMethodWizard"; //$NON-NLS-1$

  public ExtractMethodWizard_NEW(ServerExtractMethodRefactoring ref) {
    super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
    setDefaultPageTitle(RefactoringMessages.ExtractMethodWizard_extract_method);
    setDialogSettings(DartToolsPlugin.getDefault().getDialogSettings());
  }

  @Override
  protected void addUserInputPages() {
    addPage(new ExtractMethodInputPage_NEW());
  }
}

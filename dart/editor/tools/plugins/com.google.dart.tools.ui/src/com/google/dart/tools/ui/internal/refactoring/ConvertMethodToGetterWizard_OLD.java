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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ConvertMethodToGetterWizard_OLD extends RefactoringWizard {

  static final String DIALOG_SETTING_SECTION = "ConvertMethodToGetterWizard"; //$NON-NLS-1$

  public ConvertMethodToGetterWizard_OLD(Refactoring ref) {
    super(ref, DIALOG_BASED_USER_INTERFACE);
    setDefaultPageTitle(RefactoringMessages.ConvertMethodToGetterWizard_page_title);
    setDialogSettings(DartToolsPlugin.getDefault().getDialogSettings());
  }

  @Override
  protected void addUserInputPages() {
    // no input page
  }
}

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
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.actions.ActionMessages;
import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.internal.cleanup.CleanUpConstants;
import com.google.dart.tools.ui.internal.cleanup.ImportsCleanUp;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.util.Hashtable;
import java.util.Map;

/**
 * @coverage dart.editor.ui.code_manipulation
 */
public class MultiOrganizeImportAction extends CleanUpAction {

  public MultiOrganizeImportAction(DartEditor editor) {
    super(editor);

    setText(ActionMessages.OrganizeImportsAction_label);
    setToolTipText(ActionMessages.OrganizeImportsAction_tooltip);
    setDescription(ActionMessages.OrganizeImportsAction_description);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.ORGANIZE_IMPORTS_ACTION);
  }

  public MultiOrganizeImportAction(IWorkbenchSite site) {
    super(site);

    setText(ActionMessages.OrganizeImportsAction_label);
    setToolTipText(ActionMessages.OrganizeImportsAction_tooltip);
    setDescription(ActionMessages.OrganizeImportsAction_description);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.ORGANIZE_IMPORTS_ACTION);
  }

  @Override
  protected String getActionName() {
    return ActionMessages.OrganizeImportsAction_error_title;
  }

  @Override
  protected ICleanUp[] getCleanUps(CompilationUnit[] units) {
    Map<String, String> settings = new Hashtable<String, String>();
    settings.put(CleanUpConstants.ORGANIZE_IMPORTS, CleanUpOptions.TRUE);
    ImportsCleanUp importsCleanUp = new ImportsCleanUp(settings);
    return new ICleanUp[] {importsCleanUp};
  }

  @Override
  protected boolean showWizard() {
    return false;
  }
}

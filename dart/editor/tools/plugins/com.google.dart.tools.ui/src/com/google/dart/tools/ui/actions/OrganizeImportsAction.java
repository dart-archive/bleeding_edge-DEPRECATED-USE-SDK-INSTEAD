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

package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.MultiOrganizeImportAction;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Organizes the import directives in a library compilation unit.
 * 
 * @coverage dart.editor.ui.code_manipulation
 */
public class OrganizeImportsAction extends InstrumentedSelectionDispatchAction {
  public static final String ID = DartToolsPlugin.PLUGIN_ID + ".OrganizeImportsAction"; //$NON-NLS-1$

  public OrganizeImportsAction(IWorkbenchWindow window) {
    super(window);
    setText(ActionMessages.OrganizeImportsAction_label);
    setActionDefinitionId("com.google.dart.tools.ui.edit.text.organize.imports");
  }

  @Override
  public void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    IWorkbenchSite site = getSite();
    new MultiOrganizeImportAction(site).doRun(selection, event, instrumentation);
  }

  @Override
  protected void doRun(ITextSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    IWorkbenchSite site = getSite();
    IEditorPart activeEditor = site.getPage().getActiveEditor();
    if (activeEditor != null && activeEditor.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput fileInput = (IFileEditorInput) activeEditor.getEditorInput();
      doRun(new StructuredSelection(new Object[] {fileInput.getFile()}), event, instrumentation);
    }
  }
}

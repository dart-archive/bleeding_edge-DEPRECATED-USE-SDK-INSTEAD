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
package com.google.dart.tools.ui.web.pubspec.actions;

import com.google.dart.tools.ui.actions.InstrumentedSelectionDispatchAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.web.pubspec.PubspecEditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Action that opens the PubspecEditor
 */
public class OpenInPubspecEditorAction extends InstrumentedSelectionDispatchAction {

  private static final String ACTION_ID = "com.google.dart.tools.ui.actions.openInPubspecEditor"; //$NON-NLS-1$

  public OpenInPubspecEditorAction(IWorkbenchWindow window) {
    super(window);
    setId(ACTION_ID);
    setToolTipText("Open with Pubspec Editor");
    setDescription("Opens the Pubspec Editor");
  }

  @Override
  public void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    ISelection selection = getSelection();

    if (selection == null || !(selection instanceof StructuredSelection)) {
      instrumentation.metric("Problem", "Selection was not a StructuredSelection");
      return;
    }

    IFile file = (IFile) ((StructuredSelection) selection).getFirstElement();
    try {
      getSite().getPage().openEditor(
          new FileEditorInput(file),
          PubspecEditor.ID,
          true,
          IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID);
    } catch (PartInitException e) {

      instrumentation.metric("PartInitException", e.getClass().toString());
      instrumentation.data("PartInitException", e.toString());

    }

  }
}

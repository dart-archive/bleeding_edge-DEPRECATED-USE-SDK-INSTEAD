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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Abstract class selection-based actions.
 */
public abstract class AbstractDartSelectionAction_NEW extends Action implements
    ISelectionChangedListener {
  protected final DartEditor editor;
  protected final String file;
  protected int selectionOffset;
  protected int selectionLength;

  public AbstractDartSelectionAction_NEW(DartEditor editor) {
    this.editor = editor;
    file = editor.getInputFilePath();
    init();
    setEnabled(SelectionConverter.canOperateOn(editor));
  }

  public void dispose() {
  }

  public void selectionChanged(ISelection selection) {
    if (selection instanceof ITextSelection) {
      ITextSelection textSelection = (ITextSelection) selection;
      selectionOffset = textSelection.getOffset();
      selectionLength = textSelection.getLength();
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    ISelection selection = event.getSelection();
    selectionChanged(selection);
  }

  protected Shell getShell() {
    return editor.getSite().getShell();
  }

  protected IWorkbenchWindow getWorkbenchWindow() {
    return editor.getSite().getWorkbenchWindow();
  }

  /**
   * Called once by the constructors to initialize label, tooltip, image. To be overridden by
   * subclasses.
   */
  protected abstract void init();
}

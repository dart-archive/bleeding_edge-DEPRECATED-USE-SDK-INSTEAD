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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * An editor for all files that are non-HTML and non-Dart. We're overriding the default text editor
 * because we want to control the context menu contributions.
 */
public class SimpleTextEditor extends TextEditor {

  public static final String ID = "com.google.dart.tools.ui.text.editor.TextEditor"; //$NON-NLS-1$

  public SimpleTextEditor() {
    setRulerContextMenuId("#DartSimpleTextEditorRulerContext"); //$NON-NLS-1$
  }

  @Override
  protected void doSetInput(IEditorInput input) throws CoreException {

    final IFile file = (IFile) input.getAdapter(IFile.class);

    IEditorDescriptor descriptor = IDE.getEditorDescriptor(file);

    //Re-open input with a more appropriate editor if there's a better fit
    if (!descriptor.getId().equals(ID)) {

      close(true);

      Display.getDefault().asyncExec(new Runnable() {

        @Override
        public void run() {
          try {
            IDE.openEditor(DartToolsPlugin.getActivePage(), file, false);
          } catch (PartInitException e) {
            DartToolsPlugin.log(e);
          }
        }
      });

      return;
    }

    //Else, carry on
    super.doSetInput(input);
  }

  @Override
  protected void editorContextMenuAboutToShow(IMenuManager menu) {
    // Cut/Copy/Paste actions..
    addAction(menu, ITextEditorActionConstants.UNDO);
    addAction(menu, ITextEditorActionConstants.CUT);
    addAction(menu, ITextEditorActionConstants.COPY);
    addAction(menu, ITextEditorActionConstants.PASTE);
  }

  @Override
  protected void initializeKeyBindingScopes() {
    setKeyBindingScopes(new String[] {"com.google.dart.tools.ui.dartViewScope"}); //$NON-NLS-1$
  }

  @Override
  protected void rulerContextMenuAboutToShow(IMenuManager menu) {
    super.rulerContextMenuAboutToShow(menu);

    // Remove the Preferences menu item
    menu.remove(ITextEditorActionConstants.RULER_PREFERENCES);
  }
}

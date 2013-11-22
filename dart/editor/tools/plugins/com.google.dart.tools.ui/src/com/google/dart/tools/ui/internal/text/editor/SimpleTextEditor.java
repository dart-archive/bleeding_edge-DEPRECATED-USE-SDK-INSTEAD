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
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.editor.saveactions.RemoveTrailingWhitespaceAction;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import java.lang.reflect.InvocationTargetException;

/**
 * An editor for all files that are non-HTML and non-Dart. We're overriding the default text editor
 * because we want to control the context menu contributions.
 */
public class SimpleTextEditor extends TextEditor {

  public static final String ID = "com.google.dart.tools.ui.text.editor.TextEditor"; //$NON-NLS-1$
  private RemoveTrailingWhitespaceAction removeTrailingWhitespaceAction;

  public SimpleTextEditor() {
    setRulerContextMenuId("#DartSimpleTextEditorRulerContext"); //$NON-NLS-1$
  }

  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    EditorUtility.addGTKPasteHack(getSourceViewer());
  }

  @Override
  public boolean isEditable() {
    if (getEditorInput() instanceof FileStoreEditorInput) {
      return false;
    }
    return super.isEditable();
  }

  @Override
  protected void createActions() {
    removeTrailingWhitespaceAction = new RemoveTrailingWhitespaceAction(getSourceViewer());
    super.createActions();
  }

  @Override
  protected void doSetInput(IEditorInput input) throws CoreException {

    IEditorDescriptor descriptor = null;
    final IFile file = (IFile) input.getAdapter(IFile.class);

    if (file == null) {
      if (input instanceof FileStoreEditorInput) {
        Path path = new Path(((FileStoreEditorInput) input).getURI().getPath());
        descriptor = IDE.getEditorDescriptor(path.lastSegment());
      }
    } else {
      descriptor = IDE.getEditorDescriptor(file);
    }
    //Re-open input with a more appropriate editor if there's a better fit
    if (descriptor != null && !descriptor.getId().equals(ID)) {

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
  protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {

    performSaveActions();
    super.performSave(overwrite, progressMonitor);
  }

  @Override
  protected void rulerContextMenuAboutToShow(IMenuManager menu) {
    super.rulerContextMenuAboutToShow(menu);

    // Remove the Preferences menu item
    menu.remove(ITextEditorActionConstants.RULER_PREFERENCES);
  }

  private boolean isRemoveTrailingWhitespaceEnabled() {
    return PreferenceConstants.getPreferenceStore().getBoolean(
        PreferenceConstants.EDITOR_REMOVE_TRAILING_WS);
  }

  private void performSaveActions() {
    if (isRemoveTrailingWhitespaceEnabled()) {
      IEditorInput input = getEditorInput();
      if (input instanceof FileEditorInput) {
        FileEditorInput fileInput = (FileEditorInput) input;
        String name = fileInput.getName();
        if (name.endsWith(".md")) {
          // Markdown files do not have a custom model. Since trailing whitespace is significant
          // in markdown do not remove it for a file that appears to be markdown.
          return;
        }
      }
      try {
        removeTrailingWhitespaceAction.run();
      } catch (InvocationTargetException e) {
        DartToolsPlugin.log(e);
      }
    }
  }
}

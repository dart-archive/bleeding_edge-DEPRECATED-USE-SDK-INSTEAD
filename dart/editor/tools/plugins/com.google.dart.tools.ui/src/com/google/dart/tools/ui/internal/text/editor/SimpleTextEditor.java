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
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
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

    // Workaround a bug in 64 bit GTK linux that causes the active editor to steal 
    // paste insertions from the omnibox and Glance find UI (dartbug.com/13693).
    if (Util.isLinux()) {
      final ISourceViewer viewer = getSourceViewer();
      viewer.getTextWidget().addVerifyListener(new VerifyListener() {
        @Override
        public void verifyText(VerifyEvent e) {
          Control focusControl = Display.getDefault().getFocusControl();
          // If the focus control is not our text we have no business handling insertions.
          // Redirect to the rightful target
          if (focusControl != viewer.getTextWidget()) {
            if (focusControl instanceof Text) {
              Text text = (Text) focusControl;
              text.setText(e.text);
              e.doit = false;
            }
          }
        }
      });
    }
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
      try {
        removeTrailingWhitespaceAction.run();
      } catch (InvocationTargetException e) {
        DartToolsPlugin.log(e);
      }
    }
  }
}

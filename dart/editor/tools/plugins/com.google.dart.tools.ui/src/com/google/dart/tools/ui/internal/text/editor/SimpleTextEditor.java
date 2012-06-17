// Copyright (c) 2011, the Dart project authors. All Rights Reserved.

package com.google.dart.tools.ui.internal.text.editor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.editors.text.TextEditor;
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

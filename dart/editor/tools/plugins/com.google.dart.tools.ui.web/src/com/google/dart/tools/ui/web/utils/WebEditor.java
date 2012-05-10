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
package com.google.dart.tools.ui.web.utils;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * The abstract superclass of the html and css editors.
 */
public abstract class WebEditor extends TextEditor {

  public final static String MATCHING_BRACKETS = "matchingBrackets";
  public final static String MATCHING_BRACKETS_COLOR = "matchingBracketsColor";

  public WebEditor() {
    // Enable bracket highlighting in the preference store.
    IPreferenceStore store = getPreferenceStore();

    store.setDefault(MATCHING_BRACKETS, true);
    store.setDefault(MATCHING_BRACKETS_COLOR, "128,128,128");
  }

  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);

    getDocument().addDocumentListener(new IDocumentListener() {
      @Override
      public void documentAboutToBeChanged(DocumentEvent event) {

      }

      @Override
      public void documentChanged(DocumentEvent event) {
        handleDocumentModified();
      }
    });
  }

  public IDocument getDocument() {
    return getSourceViewer().getDocument();
  }

  @Override
  public String getTitleToolTip() {
    if (getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) getEditorInput();

      if (input.getFile().getLocation() != null) {
        return input.getFile().getLocation().toFile().toString();
      }
    }

    return super.getTitleToolTip();
  }

  public void selectAndReveal(Node node) {
    if (node.getEndToken() != null) {
      int length = node.getEndToken().getLocation() - node.getStartToken().getLocation();

      length += node.getEndToken().getValue().length();

      selectAndReveal(node.getStartToken().getLocation(), length);
    } else {
      selectAndReveal(node.getStartToken().getLocation(), 0);
    }
  }

  @Override
  protected void editorContextMenuAboutToShow(IMenuManager menu) {
    // Cut/Copy/Paste actions..
    addAction(menu, ITextEditorActionConstants.UNDO);
    addAction(menu, ITextEditorActionConstants.CUT);
    addAction(menu, ITextEditorActionConstants.COPY);
    addAction(menu, ITextEditorActionConstants.PASTE);
  }

  protected void handleDocumentModified() {

  }

  protected abstract void handleReconcilation(IRegion partition);

  @Override
  protected void initializeKeyBindingScopes() {
    setKeyBindingScopes(new String[] {"com.google.dart.tools.ui.dartViewScope"}); //$NON-NLS-1$
  }

  @Override
  protected boolean isOverviewRulerVisible() {
    return false;
  }

  @Override
  protected void rulerContextMenuAboutToShow(IMenuManager menu) {
    super.rulerContextMenuAboutToShow(menu);

    // Remove the Preferences menu item
    menu.remove(ITextEditorActionConstants.RULER_PREFERENCES);
  }

}

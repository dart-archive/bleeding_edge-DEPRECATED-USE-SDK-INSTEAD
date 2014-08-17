/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * A tool bar action which toggles the presentation model of the connected text editor. The editor
 * shows either the highlight range only or always the whole document.
 */
public class TogglePresentationAction extends TextEditorAction implements IPropertyChangeListener {

  private IPreferenceStore fStore;

  /**
   * Constructs and updates the action.
   */
  public TogglePresentationAction() {
    super(
        DartEditorMessages.getBundleForConstructedKeys(),
        "TogglePresentation.", null, IAction.AS_CHECK_BOX); //$NON-NLS-1$
    DartPluginImages.setToolImageDescriptors(this, "segment_edit.gif"); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.TOGGLE_PRESENTATION_ACTION);
    update();
  }

  /*
   * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().equals(PreferenceConstants.EDITOR_SHOW_SEGMENTS)) {
      synchronizeWithPreference(getTextEditor());
    }
  }

  /*
   * @see IAction#actionPerformed
   */
  @Override
  public void run() {

    ITextEditor editor = getTextEditor();
    if (editor == null) {
      return;
    }

    IRegion remembered = editor.getHighlightRange();
    editor.resetHighlightRange();

    boolean showAll = !editor.showsHighlightRangeOnly();
    setChecked(showAll);

    editor.showHighlightRangeOnly(showAll);
    if (remembered != null) {
      editor.setHighlightRange(remembered.getOffset(), remembered.getLength(), true);
    }

    fStore.removePropertyChangeListener(this);
    fStore.setValue(PreferenceConstants.EDITOR_SHOW_SEGMENTS, showAll);
    fStore.addPropertyChangeListener(this);
  }

  /*
   * @see TextEditorAction#setEditor(ITextEditor)
   */
  @Override
  public void setEditor(ITextEditor editor) {

    super.setEditor(editor);

    if (editor != null) {

      if (fStore == null) {
        fStore = DartToolsPlugin.getDefault().getPreferenceStore();
        fStore.addPropertyChangeListener(this);
      }
      synchronizeWithPreference(editor);

    } else if (fStore != null) {
      fStore.removePropertyChangeListener(this);
      fStore = null;
    }

    update();
  }

  /*
   * @see TextEditorAction#update
   */
  @Override
  public void update() {
    ITextEditor editor = getTextEditor();
    boolean checked = (editor != null && editor.showsHighlightRangeOnly());
    setChecked(checked);
    if (editor instanceof CompilationUnitEditor) {
      setEnabled(false);
    } else {
      setEnabled(editor != null);
    }
  }

  /**
   * Synchronizes the appearance of the editor with what the preference store tells him.
   * 
   * @param editor the text editor
   */
  private void synchronizeWithPreference(ITextEditor editor) {

    if (editor == null) {
      return;
    }

    boolean showSegments = fStore.getBoolean(PreferenceConstants.EDITOR_SHOW_SEGMENTS);
    setChecked(showSegments);

    if (editor.showsHighlightRangeOnly() != showSegments) {
      IRegion remembered = editor.getHighlightRange();
      editor.resetHighlightRange();
      editor.showHighlightRangeOnly(showSegments);
      if (remembered != null) {
        editor.setHighlightRange(remembered.getOffset(), remembered.getLength(), true);
      }
    }
  }
}

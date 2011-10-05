/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.actions.IJavaEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * A toolbar action which toggles the enabling state of the editor's text hover.
 */
public class ToggleTextHoverAction extends TextEditorAction implements IPropertyChangeListener {

  private IPreferenceStore fStore;

  /**
   * Constructs and updates the action.
   */
  public ToggleTextHoverAction() {
    super(DartEditorMessages.getBundleForConstructedKeys(), "ToggleTextHover.", null); //$NON-NLS-1$
    DartPluginImages.setToolImageDescriptors(this, "jdoc_hover_edit.gif"); //$NON-NLS-1$
    setActionDefinitionId(IJavaEditorActionDefinitionIds.TOGGLE_TEXT_HOVER);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IJavaHelpContextIds.TOGGLE_TEXTHOVER_ACTION);
    update();
  }

  /*
   * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().equals(PreferenceConstants.EDITOR_SHOW_HOVER)) {
      update();
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

    boolean showHover = !fStore.getBoolean(PreferenceConstants.EDITOR_SHOW_HOVER);
    setChecked(showHover);

    fStore.removePropertyChangeListener(this);
    fStore.setValue(PreferenceConstants.EDITOR_SHOW_HOVER, showHover);
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
    boolean showHover = fStore != null && fStore.getBoolean(PreferenceConstants.EDITOR_SHOW_HOVER);
    setChecked(showHover);
    setEnabled(getTextEditor() != null);
  }
}

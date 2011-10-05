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
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * A toolbar action which toggles the
 * {@linkplain com.google.dart.tools.ui.PreferenceConstants#EDITOR_MARK_OCCURRENCES mark occurrences
 * preference}.
 */
public class ToggleMarkOccurrencesAction extends TextEditorAction implements
    IPropertyChangeListener {

  private IPreferenceStore fStore;

  /**
   * Constructs and updates the action.
   */
  public ToggleMarkOccurrencesAction() {
    super(DartEditorMessages.getBundleForConstructedKeys(),
        "ToggleMarkOccurrencesAction.", null, IAction.AS_CHECK_BOX); //$NON-NLS-1$
    DartPluginImages.setToolImageDescriptors(this, "mark_occurrences.gif"); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IJavaHelpContextIds.TOGGLE_MARK_OCCURRENCES_ACTION);
    update();
  }

  /*
   * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().equals(PreferenceConstants.EDITOR_MARK_OCCURRENCES)) {
      setChecked(Boolean.valueOf(event.getNewValue().toString()).booleanValue());
    }
  }

  /*
   * @see IAction#actionPerformed
   */
  @Override
  public void run() {
    fStore.setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, isChecked());
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
    ITextEditor editor = getTextEditor();

    boolean checked = false;
    if (editor instanceof DartEditor) {
      checked = ((DartEditor) editor).isMarkingOccurrences();
    }

    setChecked(checked);
    setEnabled(editor != null);
  }
}

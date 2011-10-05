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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;

/**
 * Turns off occurrences highlighting on a java editor until linked mode is left.
 */
public class EditorHighlightingSynchronizer implements ILinkedModeListener {

  private final DartEditor fEditor;
  private final boolean fWasOccurrencesOn;

  /**
   * Creates a new synchronizer.
   * 
   * @param editor the java editor the occurrences markers of which will be synchronized with the
   *          linked mode
   */
  public EditorHighlightingSynchronizer(DartEditor editor) {
    Assert.isLegal(editor != null);
    fEditor = editor;
    fWasOccurrencesOn = fEditor.isMarkingOccurrences();

    if (fWasOccurrencesOn && !isEditorDisposed()) {
      fEditor.uninstallOccurrencesFinder();
    }
  }

  /*
   * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.
   * text.link.LinkedModeModel, int)
   */
  @Override
  public void left(LinkedModeModel environment, int flags) {
    if (fWasOccurrencesOn && !isEditorDisposed()) {
      fEditor.installOccurrencesFinder(true);
    }
  }

  /*
   * @see org.eclipse.jface.text.link.ILinkedModeListener#resume(org.eclipse.jface
   * .text.link.LinkedModeModel, int)
   */
  @Override
  public void resume(LinkedModeModel environment, int flags) {
  }

  /*
   * @see org.eclipse.jface.text.link.ILinkedModeListener#suspend(org.eclipse.jface
   * .text.link.LinkedModeModel)
   */
  @Override
  public void suspend(LinkedModeModel environment) {
  }

  /*
	 * 
	 */
  private boolean isEditorDisposed() {
    return fEditor == null || fEditor.getSelectionProvider() == null;
  }

}

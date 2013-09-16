/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.debug;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;

public class EditBreakpointAction extends BreakpointRulerAction {
  protected IBreakpoint[] breakpoints = null;

  /**
   * @param editor
   * @param rulerInfo
   */
  public EditBreakpointAction(ITextEditor editor, IVerticalRuler rulerInfo) {
    super(editor, rulerInfo);
    setText(SSEUIMessages.EditBreakpointAction_0); //$NON-NLS-1$
  }

  public void run() {
    PropertyDialogAction action = new PropertyDialogAction(getTextEditor().getEditorSite(),
        new ISelectionProvider() {
          public void addSelectionChangedListener(ISelectionChangedListener listener) {
            // do nothing
          }

          public ISelection getSelection() {
            return new StructuredSelection(breakpoints);
          }

          public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            // do nothing
          }

          public void setSelection(ISelection selection) {
            // do nothing
          }
        });
    action.run();
  }

  public void update() {
    boolean enableThisAction = hasMarkers();
    setEnabled(enableThisAction);
    breakpoints = getBreakpoints(getMarkers());
  }
}

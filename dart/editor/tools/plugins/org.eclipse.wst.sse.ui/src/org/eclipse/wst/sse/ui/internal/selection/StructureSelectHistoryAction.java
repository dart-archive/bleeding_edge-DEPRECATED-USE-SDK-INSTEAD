/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.selection;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.w3c.dom.Node;

public class StructureSelectHistoryAction extends StructureSelectAction implements IUpdate {
  public StructureSelectHistoryAction(StructuredTextEditor editor) {
    super(editor);
    setText(SSEUIMessages.StructureSelectHistory_label); //$NON-NLS-1$
    setToolTipText(SSEUIMessages.StructureSelectHistory_tooltip); //$NON-NLS-1$
    setDescription(SSEUIMessages.StructureSelectHistory_description); //$NON-NLS-1$

    update();
  }

  protected IndexedRegion getCursorIndexedRegion() {
    return null;
  }

  protected Region getNewSelectionRegion(Node node, Region region) {
    return null;
  }

  public void run() {
    IRegion old = fHistory.getLast();
    if (old != null) {
      try {
        fHistory.ignoreSelectionChanges();
        fEditor.selectAndReveal(old.getOffset(), old.getLength());
      } finally {
        fHistory.listenToSelectionChanges();
      }
    }
  }

  public void update() {
    setEnabled(!fHistory.isEmpty());
  }
}

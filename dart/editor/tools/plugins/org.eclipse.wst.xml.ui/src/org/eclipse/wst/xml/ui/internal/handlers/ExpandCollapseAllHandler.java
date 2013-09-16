/*******************************************************************************
 * Copyright (c) 2008 Standards for Technology in Automotive Retail and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver - initial API and
 * implementation, bug 212330
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.handlers;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.wst.xml.ui.internal.tabletree.IDesignViewer;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLTableTreeViewer;

public class ExpandCollapseAllHandler extends AbstractHandler implements IElementUpdater {

  protected XMLTableTreeViewer viewer = null;

  /**
   * Command handler for handling Expand and Collapse for Tree Viewer
   */
  public ExpandCollapseAllHandler() {
    super();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
   */
  public Object execute(ExecutionEvent event) throws ExecutionException {
    // Implementors to put their code here.

    return null;
  }

  protected XMLTableTreeViewer getTableTreeViewerForEditorPart(IEditorPart targetEditor) {
    XMLTableTreeViewer result = null;
    Object object = targetEditor.getAdapter(IDesignViewer.class);
    if (object instanceof XMLTableTreeViewer) {
      result = (XMLTableTreeViewer) object;
    }
    return result;
  }

  public void updateElement(UIElement element, Map parameters) {
    // TODO Auto-generated method stub

  }
}

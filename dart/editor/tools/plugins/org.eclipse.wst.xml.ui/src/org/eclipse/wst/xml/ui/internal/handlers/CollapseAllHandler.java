/*******************************************************************************
 * Copyright (c) 2008 Standards for Technology in Automotive Retail and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver - initial API and
 * implementation, bug 212330
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class CollapseAllHandler extends ExpandCollapseAllHandler implements IHandler {

  /**
	 * 
	 */
  public CollapseAllHandler() {
    super();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
   */
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IEditorPart editorPart = HandlerUtil.getActiveEditor(event);

    viewer = getTableTreeViewerForEditorPart(editorPart);

    if (viewer != null) {
      // temporarily set the visibility to false
      // this has a HUGE performance benefit
      boolean isVisible = viewer.getControl().getVisible();
      viewer.getControl().setVisible(false);
      viewer.collapseAll();

      // restore the previous visibility state
      // 
      viewer.getControl().setVisible(isVisible);
    }

    return null;
  }
}

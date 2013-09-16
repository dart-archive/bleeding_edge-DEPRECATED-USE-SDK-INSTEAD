/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.properties;

import org.eclipse.jface.action.Action;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImageHelper;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImages;

public class RemoveAction extends Action {
  private ConfigurablePropertySheetPage fPage;

  public RemoveAction(ConfigurablePropertySheetPage page) {
    super();
    fPage = page;
    setText(getText());
    setToolTipText(getText());
    setImageDescriptor(EditorPluginImageHelper.getInstance().getImageDescriptor(
        EditorPluginImages.IMG_ELCL_DELETE));
    setDisabledImageDescriptor(EditorPluginImageHelper.getInstance().getImageDescriptor(
        EditorPluginImages.IMG_DLCL_DELETE));
  }

  /**
   * @see org.eclipse.jface.action.Action#getText()
   */
  public String getText() {
    return SSEUIMessages.RemoveAction_0; //$NON-NLS-1$
  }

  /**
   * @see org.eclipse.jface.action.IAction#run()
   */
  public void run() {
    fPage.remove();
  }
}

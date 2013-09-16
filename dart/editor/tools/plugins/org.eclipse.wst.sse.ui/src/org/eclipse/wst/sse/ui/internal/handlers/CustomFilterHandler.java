/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.wst.sse.ui.internal.contentoutline.ConfigurableContentOutlinePage;

public class CustomFilterHandler extends AbstractHandler {
  public Object execute(ExecutionEvent event) throws ExecutionException {

    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    Object page = editor.getAdapter(IContentOutlinePage.class);
    if (page instanceof ConfigurableContentOutlinePage) {
      ((ConfigurableContentOutlinePage) page).getOutlineFilterProcessor().openDialog();
    }
    return null;
  }

}

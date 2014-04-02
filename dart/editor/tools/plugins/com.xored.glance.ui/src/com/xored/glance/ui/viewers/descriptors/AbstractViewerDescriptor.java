/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.viewers.descriptors;

import com.xored.glance.ui.sources.ITextSourceDescriptor;
import com.xored.glance.ui.viewers.utils.ViewerUtils;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;

/**
 * @author Yuri Strot
 */
public abstract class AbstractViewerDescriptor implements ITextSourceDescriptor {

  protected ITextViewer getTextViewer(Control control) {
    if (control instanceof StyledText) {
      return getTextViewer((StyledText) control);
    }
    return null;
  }

  protected ITextViewer getTextViewer(StyledText text) {
    return ViewerUtils.getTextViewer(text);
  }

}

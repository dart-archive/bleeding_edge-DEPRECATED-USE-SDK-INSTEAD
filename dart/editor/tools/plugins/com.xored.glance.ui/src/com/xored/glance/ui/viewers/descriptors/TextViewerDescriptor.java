/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.viewers.descriptors;

import com.xored.glance.internal.ui.viewers.TextViewerControl;
import com.xored.glance.ui.sources.ITextSource;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.widgets.Control;

/**
 * @author Yuri Strot
 */
public class TextViewerDescriptor extends AbstractViewerDescriptor {

  @Override
  public ITextSource createSource(Control control) {
    return new TextViewerControl((TextViewer) getTextViewer(control));
  }

  @Override
  public boolean isValid(Control control) {
    ITextViewer viewer = getTextViewer(control);
    return viewer instanceof TextViewer && viewer.getDocument() != null;
  }
}

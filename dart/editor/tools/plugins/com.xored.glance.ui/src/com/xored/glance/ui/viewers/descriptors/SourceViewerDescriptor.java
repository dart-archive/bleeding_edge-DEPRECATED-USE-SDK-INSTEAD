/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.viewers.descriptors;

import com.xored.glance.internal.ui.viewers.SourceViewerControl;
import com.xored.glance.ui.sources.ITextSource;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Control;

/**
 * @author Yuri Strot
 */
public class SourceViewerDescriptor extends AbstractViewerDescriptor {

  @Override
  public ITextSource createSource(Control control) {
    return new SourceViewerControl((SourceViewer) getTextViewer(control));
  }

  @Override
  public boolean isValid(Control control) {
    ITextViewer viewer = getTextViewer(control);
    if (viewer instanceof SourceViewer) {
      SourceViewer sViewer = (SourceViewer) viewer;
      if (sViewer.getAnnotationModel() != null && sViewer.getDocument() != null) {
        return true;
      }
    }
    return false;
  }
}

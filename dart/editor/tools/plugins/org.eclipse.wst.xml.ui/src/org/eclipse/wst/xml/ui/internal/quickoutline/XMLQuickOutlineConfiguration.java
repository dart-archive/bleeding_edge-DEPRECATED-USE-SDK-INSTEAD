/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.quickoutline;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.wst.sse.ui.IContentSelectionProvider;
import org.eclipse.wst.sse.ui.quickoutline.AbstractQuickOutlineConfiguration;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeContentProvider;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeLabelProvider;

public class XMLQuickOutlineConfiguration extends AbstractQuickOutlineConfiguration {

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.IOutlineContentManager#getContentProvider()
   */
  public ITreeContentProvider getContentProvider() {
    return new JFaceNodeContentProvider();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.IOutlineContentManager#getContentSelectionProvider()
   */
  public IContentSelectionProvider getContentSelectionProvider() {
    return new XMLContentSelectionProvider();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.IOutlineContentManager#getLabelProvider()
   */
  public ILabelProvider getLabelProvider() {
    return new JFaceNodeLabelProvider();
  }

}

/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentoutline;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;

/**
 * A (column) label provider backed by JFaceNodeAdapters.
 */
public class JFaceNodeLabelProvider extends ColumnLabelProvider {
  /**
   * JFaceNodeLabelProvider constructor comment.
   */
  public JFaceNodeLabelProvider() {
    super();
  }

  /**
   * Returns the JFace adapter for the specified object.
   * 
   * @param adaptable java.lang.Object The object to get the adapter for
   */
  protected IJFaceNodeAdapter getAdapter(Object adaptable) {
    if (adaptable instanceof INodeNotifier) {
      INodeAdapter adapter = ((INodeNotifier) adaptable).getAdapterFor(IJFaceNodeAdapter.class);
      if (adapter instanceof IJFaceNodeAdapter) {
        return (IJFaceNodeAdapter) adapter;
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
   */
  public Image getImage(Object element) {
    IJFaceNodeAdapter adapter = getAdapter(element);
    if (adapter != null)
      return adapter.getLabelImage(element);
    return super.getImage(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
   */
  public String getText(Object element) {
    IJFaceNodeAdapter adapter = getAdapter(element);
    if (adapter != null)
      return adapter.getLabelText(element);
    return super.getText(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object,
   * java.lang.String)
   */
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }
}

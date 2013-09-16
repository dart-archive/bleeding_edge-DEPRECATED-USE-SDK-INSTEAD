/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.views.contentoutline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;

class JFaceNodeLabelProviderCSS extends LabelProvider {

  /**
   * JFaceNodeLabelProvider constructor comment.
   */
  public JFaceNodeLabelProviderCSS() {
    super();
  }

  /**
   * Returns the JFace adapter for the specified object.
   * 
   * @param adaptable java.lang.Object The object to get the adapter for
   */
  private IJFaceNodeAdapter getAdapter(Object adaptable) {
    IJFaceNodeAdapter adapter = null;
    if (adaptable instanceof ICSSModel) {
      adaptable = ((ICSSModel) adaptable).getDocument();
    }
    if (adaptable instanceof INodeNotifier) {
      INodeAdapter nodeAdapter = ((INodeNotifier) adaptable).getAdapterFor(IJFaceNodeAdapter.class);
      if (nodeAdapter instanceof IJFaceNodeAdapter)
        adapter = (IJFaceNodeAdapter) nodeAdapter;
    }
    return adapter;
  }

  /**
   * Returns the image for the label of the given element, for use in the given viewer.
   * 
   * @param viewer The viewer that displays the element.
   * @param element The element for which to provide the label image. Element can be
   *          <code>null</code> indicating no input object is set to the viewer.
   */
  public Image getImage(Object element) {
    Image image = null;
    IJFaceNodeAdapter adapter = getAdapter(element);
    if (adapter != null)
      image = adapter.getLabelImage(element);
    return image;
  }

  /**
   * Returns the text for the label of the given element, for use in the given viewer.
   * 
   * @param viewer The viewer that displays the element.
   * @param element The element for which to provide the label text. Element can be
   *          <code>null</code> indicating no input object is set to the viewer.
   */
  public String getText(Object element) {
    String text = null;
    IJFaceNodeAdapter adapter = getAdapter(element);
    if (adapter != null) {
      text = adapter.getLabelText(element);
    }
    return text;
  }

  /**
   * Checks whether this label provider is affected by the given domain event.
   * 
   * @deprecated
   */
  public boolean isAffected(Object dummy) {// DomainEvent event) {
    // return event.isModifier(DomainEvent.NON_STRUCTURE_CHANGE);
    return true;

  }

  /**
   * Returns whether the label would be affected by a change to the given property of the given
   * element. This can be used to optimize a non-structural viewer update. If the property mentioned
   * in the update does not affect the label, then the viewer need not update the label.
   * 
   * @param element the element
   * @param property the property
   * @return <code>true</code> if the label would be affected, and <code>false</code> if it would be
   *         unaffected
   */
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }
}

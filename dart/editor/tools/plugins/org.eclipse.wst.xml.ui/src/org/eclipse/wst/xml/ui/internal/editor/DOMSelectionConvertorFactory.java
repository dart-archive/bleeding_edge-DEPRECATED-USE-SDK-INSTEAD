/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.editor;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.editor.SelectionConvertor;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.NamedNodeMap;

/**
 * @author nitin
 */
public class DOMSelectionConvertorFactory implements IAdapterFactory {

  private static final Class[] ADAPTER_LIST = new Class[] {SelectionConvertor.class};

  private static class XMLSelectionConvertor extends SelectionConvertor {
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.wst.sse.ui.internal.editor.SelectionConvertor#getElements(org.eclipse.wst.sse
     * .core.internal.provisional.IStructuredModel, int, int)
     */
    public Object[] getElements(IStructuredModel model, int start, int end) {
      Object[] objects = super.getElements(model, start, end);
      // narrow single selected Elements into Attrs if possible
      if (objects.length == 1) {
        if (objects[0] instanceof IDOMNode) {
          IDOMNode node = (IDOMNode) objects[0];
          NamedNodeMap attributes = node.getAttributes();
          if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
              IDOMAttr attribute = (IDOMAttr) attributes.item(i);
              if (attribute.contains(start) && attribute.contains(end)) {
                objects[0] = attribute;
                break;
              }
            }
          }
        }
      }
      return objects;
    }
  }

  private static final Object selectionConvertor = new XMLSelectionConvertor();

  /**
	 * 
	 */
  public DOMSelectionConvertorFactory() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
   */
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adaptableObject instanceof IDOMModel && SelectionConvertor.class.equals(adapterType))
      return selectionConvertor;
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
   */
  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }

}

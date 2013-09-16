/*******************************************************************************
 * Copyright (c) 2010 Standards for Technology in Automotive Retail and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.views.contentmodel;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.w3c.dom.Element;

class ContentModelWorkbenchAdapter implements IWorkbenchAdapter {
  private final Object[] EMPTY = new Object[0];
  private Object parent = null;

  public Object[] getChildren(Object o) {
    if (o instanceof Element) {
      Element node = (Element) o;
      ModelQuery mq = ModelQueryUtil.getModelQuery(node.getOwnerDocument());
      if (mq != null) {
        CMElementDeclaration decl = mq.getCMElementDeclaration(node);
        CMListWorkbenchAdapter adapter = new CMListWorkbenchAdapter(decl);
        return new Object[] {adapter};
      }
    }
    return EMPTY;
  }

  public ImageDescriptor getImageDescriptor(Object object) {
    return null;
  }

  public String getLabel(Object o) {
    return null;
  }

  public Object getParent(Object o) {
    return parent;
  }
}

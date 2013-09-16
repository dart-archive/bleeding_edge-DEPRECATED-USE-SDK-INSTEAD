/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.eclipse.wst.xml.core.internal.contentmodel.CMAnyElement;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.util.ContentBuilder;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;

public class NamespaceInfoContentBuilder extends ContentBuilder {
  protected int count = 1;
  public List list = new Vector();
  protected Hashtable table = new Hashtable();

  public NamespaceInfoContentBuilder() {
    super();
  }

  public void visitCMElementDeclaration(CMElementDeclaration ed) {
    if (ed.getProperty("http://org.eclipse.wst/cm/properties/definitionInfo") != null) //$NON-NLS-1$
    {
      super.visitCMElementDeclaration(ed);
    }
  }

  protected void createAnyElementNode(CMAnyElement anyElement) {
    String uri = anyElement.getNamespaceURI();
    if ((uri != null) && !uri.startsWith("##")) //$NON-NLS-1$
    {
      if (table.get(uri) == null) {
        NamespaceInfo info = new NamespaceInfo();
        info.uri = uri;
        info.prefix = "p" + count++; //$NON-NLS-1$
        table.put(uri, info);
        list.add(info);
      }
    }
  }
}

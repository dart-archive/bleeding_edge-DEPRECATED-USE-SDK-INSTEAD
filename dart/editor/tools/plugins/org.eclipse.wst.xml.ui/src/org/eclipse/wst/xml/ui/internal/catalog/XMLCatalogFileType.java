/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.ibm.icu.util.StringTokenizer;

import org.eclipse.swt.graphics.Image;

public class XMLCatalogFileType {
  public String description;
  public String id;
  public List extensions = new ArrayList();
  public String iconFileName;
  public Image icon;

  public void addExtensions(String contributedExtensions) {
    List list = parseExtensions(contributedExtensions);
    for (Iterator i = list.iterator(); i.hasNext();) {
      String extension = (String) i.next();
      if (!extensions.contains(extension)) {
        extensions.add(extension);
      }
    }
  }

  protected List parseExtensions(String string) {
    List list = new ArrayList();
    for (StringTokenizer st = new StringTokenizer(string, ", "); st.hasMoreTokens();) //$NON-NLS-1$
    {
      String token = st.nextToken();
      if (token != null) {
        list.add(token);
      }
    }
    return list;
  }
}

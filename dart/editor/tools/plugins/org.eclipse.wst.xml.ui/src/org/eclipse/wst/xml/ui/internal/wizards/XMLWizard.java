/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;

public class XMLWizard {
  protected static XMLWizard instance = new XMLWizard();

  public synchronized static XMLWizard getInstance() {
    return instance;
  }

  public XMLWizard() {
    instance = this;

  }

  public ImageDescriptor getImageDescriptor(String name) {
    try {
      URL url = new URL(Platform.getBundle("org.eclipse.wst.xml.ui").getEntry("/"), name); //$NON-NLS-1$ //$NON-NLS-2$
      return ImageDescriptor.createFromURL(url);
    } catch (MalformedURLException e) {
      return ImageDescriptor.getMissingImageDescriptor();
    }
  }
}

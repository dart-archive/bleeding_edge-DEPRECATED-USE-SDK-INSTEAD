/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

public class CatalogFileTypeRegistryReader {
  private static CatalogFileTypeRegistryReader _instance;

  static final String ATT_DESCRIPTION = "description"; //$NON-NLS-1$

  static final String ATT_EXTENSIONS = "extensions"; //$NON-NLS-1$

  static final String ATT_ICON = "icon"; //$NON-NLS-1$

  static final String ATT_ID = "id"; //$NON-NLS-1$

  static final String EXTENSION_POINT_ID = "catalogFileType"; //$NON-NLS-1$

  static final String TAG_NAME = "fileType"; //$NON-NLS-1$

  private static CatalogFileTypeRegistryReader getInstance() {
    if (_instance == null) {
      _instance = new CatalogFileTypeRegistryReader();
    }
    return _instance;
  }

  public static Collection getXMLCatalogFileTypes() {
    return getInstance().hashMap.values();
  }

  private HashMap hashMap;

  public CatalogFileTypeRegistryReader() {
    this.hashMap = new HashMap();
    readRegistry();
  }

  private void readElement(IConfigurationElement element) {
    if (element.getName().equals(TAG_NAME)) {
      String id = element.getAttribute(ATT_ID);
      if (id != null) {
        XMLCatalogFileType fileType = (XMLCatalogFileType) hashMap.get(id);
        if (fileType == null) {
          fileType = new XMLCatalogFileType();
          hashMap.put(id, fileType);
        }
        fileType.id = id;
        if (fileType.description == null) {
          String description = element.getAttribute(ATT_DESCRIPTION);
          fileType.description = description;
        }

        fileType.addExtensions(element.getAttribute(ATT_EXTENSIONS));
      }
    }
  }

  private void readRegistry() {
    readRegistry(EXTENSION_POINT_ID);
  }

  private void readRegistry(String extensionPointId) {
    IExtensionRegistry pluginRegistry = Platform.getExtensionRegistry();
    IExtensionPoint point = pluginRegistry.getExtensionPoint(XMLUIPlugin.ID, extensionPointId);
    if (point != null) {
      IConfigurationElement[] elements = point.getConfigurationElements();
      for (int i = 0; i < elements.length; i++) {
        readElement(elements[i]);
      }
    }
  }
}

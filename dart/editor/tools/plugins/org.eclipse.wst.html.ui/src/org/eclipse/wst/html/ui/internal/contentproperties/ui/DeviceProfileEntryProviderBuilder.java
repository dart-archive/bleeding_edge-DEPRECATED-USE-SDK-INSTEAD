/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentproperties.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

/**
 * @deprecated Not needed. See BUG118359
 */
public class DeviceProfileEntryProviderBuilder {
  private static final String EXTENSION_POINT_PLUGINID = "org.eclipse.wst.html.ui"; //$NON-NLS-1$
  private static final String EXTENSION_POINT_NAME = "deviceProfileEntryProvider"; //$NON-NLS-1$
  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  public DeviceProfileEntryProviderBuilder() {
    super();
  }

  static public DeviceProfileEntryProvider getEntryProvider() {
    IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(
        EXTENSION_POINT_PLUGINID, EXTENSION_POINT_NAME);
    if (point != null) {
      IExtension extensions[] = point.getExtensions();
      if ((extensions != null) && (extensions.length > 0)) {
        for (int i = 0; i < extensions.length; i++) {
          IConfigurationElement elements[] = extensions[i].getConfigurationElements();
          if ((elements != null) && (elements.length > 0)) {
            for (int j = 0; j < elements.length; j++) {
              IConfigurationElement config = elements[j];
              if ((config != null) && (config.getName().equals(EXTENSION_POINT_NAME) == true)) {
                String className = config.getAttribute(ATTR_CLASS);
                if (className != null) {
                  try {
                    DeviceProfileEntryProvider provider = (DeviceProfileEntryProvider) config.createExecutableExtension(ATTR_CLASS);
                    if (provider != null) {
                      return provider;
                    }
                  } catch (CoreException ignored) {
                  }
                }
              }
            }
          }
        }
      }
    }
    return null;
  }
}

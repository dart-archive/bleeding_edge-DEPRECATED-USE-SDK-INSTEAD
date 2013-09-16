/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.ui.internal.extension.RegistryReader;
import org.osgi.framework.Bundle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FormatProcessorsExtensionReader extends RegistryReader {
  private static FormatProcessorsExtensionReader instance;

  public synchronized static FormatProcessorsExtensionReader getInstance() {
    if (instance == null) {
      instance = new FormatProcessorsExtensionReader();

      IExtensionRegistry registry = Platform.getExtensionRegistry();
      instance.readRegistry(registry, "org.eclipse.wst.sse.core", "formatProcessors"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    return instance;
  }

  private Map map = new HashMap();
//	 TODO: private field never read locally
  String processorClassName;

  public IStructuredFormatProcessor getFormatProcessor(String contentTypeId) {
    if (contentTypeId == null)
      return null;

    IStructuredFormatProcessor formatProcessor = null;
    if (map.containsKey(contentTypeId)) {
      formatProcessor = (IStructuredFormatProcessor) map.get(contentTypeId);
    } else {
      IContentTypeManager manager = Platform.getContentTypeManager();
      IContentType queryContentType = manager.getContentType(contentTypeId);
      boolean found = false;
      for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
        String elementContentTypeId = (String) iter.next();
        IContentType elementContentType = manager.getContentType(elementContentTypeId);
        if (queryContentType.isKindOf(elementContentType)) {
          formatProcessor = (IStructuredFormatProcessor) map.get(elementContentTypeId);
          map.put(contentTypeId, formatProcessor);
          found = true;
          break;
        }
      }

      if (!found)
        map.put(contentTypeId, null);
    }

    return formatProcessor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.extension.RegistryReader#readElement(org.eclipse.core.runtime
   * .IConfigurationElement)
   */
  protected boolean readElement(IConfigurationElement element) {
    if (element.getName().equals("processor")) { //$NON-NLS-1$
      String contentTypeId = element.getAttribute("contentTypeId"); //$NON-NLS-1$
      String processorClassName = element.getAttribute("class"); //$NON-NLS-1$
      String pluginID = element.getDeclaringExtension().getNamespace();
      Bundle bundle = Platform.getBundle(pluginID);

      try {
        IStructuredFormatProcessor processor = (IStructuredFormatProcessor) bundle.loadClass(
            processorClassName).newInstance();
        map.put(contentTypeId, processor);

        return true;
      } catch (InstantiationException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }

    return false;
  }
}

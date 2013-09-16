/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.HashMap;

/**
 * This class just converts what's in the plugins registry into a form more easily useable by
 * others, the ContentTypeRegistry.
 */
class AdapterFactoryRegistryReader {
  protected final static String ATT_CLASS = "class"; //$NON-NLS-1$

  protected final static String ATT_ID = "id"; //$NON-NLS-1$

  private static boolean DEBUG = false;
  protected final static String EXTENSION_POINT_ID = "adapterFactoryDescription"; //$NON-NLS-1$
  //
  protected final static String PLUGIN_ID = "org.eclipse.wst.sse.ui"; //$NON-NLS-1$
  protected final static String TAG_CONTENT_TYPE = "contentType"; //$NON-NLS-1$

  protected final static String TAG_NAME = "adapterFactoryDescription"; //$NON-NLS-1$

  public final static String UNKNOWN_CONTENT_TYPE = "unknown"; //$NON-NLS-1$

  /**
   * adds configuration element to contentTypeId map [contentTypeId -> element2providerMap] | V
   * [element -> provider] NOTE: this doesn't create the provider yet, that must be done on demand
   * and stored in the appropriate element2provider
   * 
   * @param map
   * @param contentTypeId
   * @param element
   */
  private static void addElementForContentType(HashMap map, String contentTypeId,
      IConfigurationElement element) {

    Object o = map.get(contentTypeId);
    if (o == null) {
      HashMap element2provider = new HashMap();
      // don't create the executable extension yet
      element2provider.put(element, null);
      map.put(contentTypeId, element2provider);

      if (DEBUG)
        System.out.println("added " + element.getAttribute(ATT_CLASS) + ", but didn't create exec extension"); //$NON-NLS-1$ //$NON-NLS-2$
    } else {
      // add element to unknown list (not executable ext yet...)
      HashMap element2provider = (HashMap) o;
      element2provider.put(element, null);

      if (DEBUG)
        System.out.println("added " + element.getAttribute(ATT_CLASS) + " to unknown list, but didn't create exec extension"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  /**
   * the map passed in: [contentTypeId -> element2providerMap] | V [element -> provider]
   * 
   * @param element
   * @param map
   * @return
   */
  protected static AdapterFactoryProvider readElement(IConfigurationElement element, HashMap map) {

    AdapterFactoryProvider adapterFactoryProvider = null;
    if (element.getName().equals(TAG_NAME)) {
      try {
        IConfigurationElement[] children = element.getChildren();
        boolean specifiedContentType = false;
        if (children != null && children.length > 0) {
          // content types are specified
          for (int i = 0; i < children.length; i++) {
            if (children[i].getName().equals(TAG_CONTENT_TYPE)) {
              // it's possible to have non-contentType childrent
              specifiedContentType = true;
              String contentType = children[i].getAttribute(ATT_ID);
              addElementForContentType(map, contentType, element);
            }
          }
        }
        if (!specifiedContentType) {
          // no content type association
          addElementForContentType(map, UNKNOWN_CONTENT_TYPE, element);
        }
      } catch (Exception e) {
        // if the provider throws any exception, just log and continue
        Logger.logException(e);
      }
    }
    return adapterFactoryProvider;
  }

  /**
   * We simply require an 'add' method, of what ever it is we are to read into
   */
  static void readRegistry(HashMap map) {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint point = registry.getExtensionPoint(PLUGIN_ID, EXTENSION_POINT_ID);
    if (point != null) {
      IConfigurationElement[] elements = point.getConfigurationElements();
      for (int i = 0; i < elements.length; i++) {
        readElement(elements[i], map);
      }
    }
  }

  protected IConfigurationElement configElement = null;

  //    protected final static String ADAPTER_CLASS = "adapterClass";
  // //$NON-NLS-1$
  //    protected final static String DOC_TYPE_ID = "docTypeId"; //$NON-NLS-1$
  //    protected final static String MIME_TYPE_LIST = "mimeTypeList";
  // //$NON-NLS-1$
  //
  /**
   * ContentTypeRegistryReader constructor comment.
   */
  AdapterFactoryRegistryReader() {
    super();
  }
}

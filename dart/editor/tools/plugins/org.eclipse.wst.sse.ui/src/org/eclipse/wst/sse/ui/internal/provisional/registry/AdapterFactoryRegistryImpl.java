/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.registry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AdapterFactoryRegistryImpl implements AdapterFactoryRegistry,
    AdapterFactoryRegistryExtension {

  protected final static String ATT_CLASS = "class"; //$NON-NLS-1$

  private static AdapterFactoryRegistry instance = null;

  static synchronized public AdapterFactoryRegistry getInstance() {
    if (instance == null) {
      instance = new AdapterFactoryRegistryImpl();
    }
    return instance;
  }

  // this map exists so there is only one provider
  // instance not duplicate providers for different content types
  private HashMap adapterProviders = null;

  private boolean DEBUG = false;
  /**
   * This HashMap contains: [contentTypeId -> element2providerMap] | V [configurationElement ->
   * AdapterFactoryProvider]
   */
  private HashMap hashMap = null;

  // providers with no content type associated
  // just added through the add(...) method
  private HashSet unassociatedProviders = null;

  private AdapterFactoryRegistryImpl() {
    super();
    this.hashMap = new HashMap();
    this.unassociatedProviders = new HashSet();
    this.adapterProviders = new HashMap();

    // doesn't instantiate classes, just stores configuration elements
    AdapterFactoryRegistryReader.readRegistry(hashMap);
  }

  void add(AdapterFactoryProvider adapterFactoryProvider) {
    this.unassociatedProviders.add(adapterFactoryProvider);
  }

  public Iterator getAdapterFactories() {
    if (DEBUG) {
      System.out.println("===================================================================================="); //$NON-NLS-1$
      System.out.println("GETTING ALL ADAPTER FACTORIES"); //$NON-NLS-1$
    }

    List results = new ArrayList();

    // add providers that have no content type specification
    results.addAll(this.unassociatedProviders);
    Iterator it = this.hashMap.keySet().iterator();
    String contentTypeId = null;
    while (it.hasNext()) {
      contentTypeId = (String) it.next();

      if (DEBUG)
        System.out.println(" + for: " + contentTypeId); //$NON-NLS-1$

      results.addAll(getAdapterFactoriesAsList(contentTypeId));
    }

    if (DEBUG) {
      System.out.println("===================================================================================="); //$NON-NLS-1$
    }

    return results.iterator();
  }

  public Iterator getAdapterFactories(String contentTypeID) {
    if (DEBUG) {
      System.out.println("===================================================================================="); //$NON-NLS-1$
      System.out.println("GETTING ADAPTER FACTORIES for: " + contentTypeID); //$NON-NLS-1$
    }

    List results = new ArrayList();

    // add providers that have no content type specification
    results.addAll(unassociatedProviders);

    // add unknown content type providers (for backwards compatability)
    results.addAll(getAdapterFactoriesAsList(AdapterFactoryRegistryReader.UNKNOWN_CONTENT_TYPE));

    // add providers for specific content type
    results.addAll(getAdapterFactoriesAsList(Platform.getContentTypeManager().getContentType(
        contentTypeID)));

    if (DEBUG) {
      System.out.println("===================================================================================="); //$NON-NLS-1$
    }

    return results.iterator();
  }

  public List getAdapterFactoriesAsList(IContentType contentType) {
    IContentType type = contentType;
    List results = new ArrayList();
    while (type != null && !type.getId().equals(IContentTypeManager.CT_TEXT)) {
      results.addAll(getAdapterFactoriesAsList(type.getId()));
      type = type.getBaseType();
    }
    return results;
  }

  /**
   * Using this new API, only AdapterFactoryProviders for a certain content type are instantiated.
   * This will allow for the minimum number of plugins to be loaded rather than all that implement
   * the adapter factory extension point.
   * 
   * @param contentTypeID
   * @return
   */
  public List getAdapterFactoriesAsList(String contentTypeID) {

    List results = new ArrayList();

    // get element2Provider map for specified content type
    Object o = hashMap.get(contentTypeID);
    if (o != null) {
      // instantiate if necessary from
      // element2adapterFactoryProvider
      // map
      Map element2Provider = (Map) o;
      Iterator it = element2Provider.keySet().iterator();
      IConfigurationElement element = null;
      String classname = null;
      Object existing = null;
      AdapterFactoryProvider p = null;
      while (it.hasNext()) {
        element = (IConfigurationElement) it.next();
        o = element2Provider.get(element);
        if (o != null) {
          // this provider has already been created
          if (DEBUG)
            System.out.println("already created: " + element.getAttribute(ATT_CLASS)); //$NON-NLS-1$

          results.add(o);
        } else {
          // need to create the provider
          try {
            classname = element.getAttribute(ATT_CLASS);

            if (DEBUG)
              System.out.println("about to create: " + classname); //$NON-NLS-1$

            // check if we created one already
            existing = this.adapterProviders.get(classname);
            if (existing == null) {
              // this is the only place
              // AdapterFactoryProviders
              // are created
              p = (AdapterFactoryProvider) element.createExecutableExtension(ATT_CLASS); // $NON-NLS-1$
              this.adapterProviders.put(classname, p);
            } else {
              p = (AdapterFactoryProvider) existing;
            }

            // add to element2Provider for this contentType
            element2Provider.put(element, p);
            // add to results to return for this method
            results.add(p);

          } catch (CoreException e) {
            // if the provider throws any exception, just log
            // and
            // continue
            Logger.logException(e);
          }
        }
      }
    }

    return results;
  }
}

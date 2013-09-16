/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.html.ui.internal.preferences.HTMLUIPreferenceNames;
import org.eclipse.wst.html.ui.internal.templates.TemplateContextTypeIdsHTML;
import org.eclipse.wst.sse.ui.internal.provisional.registry.AdapterFactoryRegistry;
import org.eclipse.wst.sse.ui.internal.provisional.registry.AdapterFactoryRegistryImpl;

import java.io.IOException;

/**
 * The main plugin class to be used in the desktop.
 */
public class HTMLUIPlugin extends AbstractUIPlugin {
  public final static String ID = "org.eclipse.wst.html.ui"; //$NON-NLS-1$

  protected static HTMLUIPlugin instance = null;

  /**
   * The template store for the html editor.
   */
  private TemplateStore fTemplateStore;

  /**
   * The template context type registry for the html editor.
   */
  private ContextTypeRegistry fContextTypeRegistry;

  public HTMLUIPlugin() {
    super();
    instance = this;
  }

  public static HTMLUIPlugin getDefault() {
    return instance;
  }

  public synchronized static HTMLUIPlugin getInstance() {
    return instance;
  }

  public AdapterFactoryRegistry getAdapterFactoryRegistry() {
    return AdapterFactoryRegistryImpl.getInstance();

  }

  /**
   * Returns the template store for the html editor templates.
   * 
   * @return the template store for the html editor templates
   */
  public TemplateStore getTemplateStore() {
    if (fTemplateStore == null) {
      fTemplateStore = new ContributionTemplateStore(getTemplateContextRegistry(),
          getPreferenceStore(), HTMLUIPreferenceNames.TEMPLATES_KEY);

      try {
        fTemplateStore.load();
      } catch (IOException e) {
        Logger.logException(e);
      }
    }
    return fTemplateStore;
  }

  /**
   * Returns the template context type registry for the html plugin.
   * 
   * @return the template context type registry for the html plugin
   */
  public ContextTypeRegistry getTemplateContextRegistry() {
    if (fContextTypeRegistry == null) {
      ContributionContextTypeRegistry registry = new ContributionContextTypeRegistry();
      registry.addContextType(TemplateContextTypeIdsHTML.ALL);
      registry.addContextType(TemplateContextTypeIdsHTML.NEW);
      registry.addContextType(TemplateContextTypeIdsHTML.TAG);
      registry.addContextType(TemplateContextTypeIdsHTML.ATTRIBUTE);
      registry.addContextType(TemplateContextTypeIdsHTML.ATTRIBUTE_VALUE);

      fContextTypeRegistry = registry;
    }

    return fContextTypeRegistry;
  }
}

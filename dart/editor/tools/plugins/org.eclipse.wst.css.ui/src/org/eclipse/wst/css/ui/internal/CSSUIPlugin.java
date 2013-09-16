/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.css.ui.internal.preferences.CSSUIPreferenceNames;
import org.eclipse.wst.css.ui.internal.templates.TemplateContextTypeIdsCSS;

import java.io.IOException;

/**
 * The main plugin class to be used in the desktop.
 */
public class CSSUIPlugin extends AbstractUIPlugin {
  public final static String ID = "org.eclipse.wst.css.ui"; //$NON-NLS-1$
  //The shared instance.
  private static CSSUIPlugin plugin;

  /**
   * The template store for the css ui.
   */
  private TemplateStore fTemplateStore;

  /**
   * The template context type registry for css ui.
   */
  private ContextTypeRegistry fContextTypeRegistry;

  /**
   * The constructor.
   */
  public CSSUIPlugin() {
    super();
    plugin = this;
  }

  /**
   * Returns the shared instance.
   */
  public static CSSUIPlugin getDefault() {
    return plugin;
  }

  /**
   * Returns the workspace instance.
   */
  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  /**
   * Returns the template store for the css editor templates.
   * 
   * @return the template store for the css editor templates
   */
  public TemplateStore getTemplateStore() {
    if (fTemplateStore == null) {
      fTemplateStore = new ContributionTemplateStore(getTemplateContextRegistry(),
          getPreferenceStore(), CSSUIPreferenceNames.TEMPLATES_KEY);

      try {
        fTemplateStore.load();
      } catch (IOException e) {
        Logger.logException(e);
      }
    }
    return fTemplateStore;
  }

  /**
   * Returns the template context type registry for the css plugin.
   * 
   * @return the template context type registry for the css plugin
   */
  public ContextTypeRegistry getTemplateContextRegistry() {
    if (fContextTypeRegistry == null) {
      ContributionContextTypeRegistry registry = new ContributionContextTypeRegistry();
      registry.addContextType(TemplateContextTypeIdsCSS.ALL);
      registry.addContextType(TemplateContextTypeIdsCSS.NEW);

      fContextTypeRegistry = registry;
    }

    return fContextTypeRegistry;
  }
}

/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal;

import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
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
   * The template store for the css ui.
   */
  private TemplateStore fTemplateStore;

  /**
   * The template context type registry for css ui.
   */
  private ContextTypeRegistry fContextTypeRegistry;

  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      updatePreferences();
    }
  };

  /**
   * The constructor.
   */
  public CSSUIPlugin() {
    super();
    plugin = this;
    initializePreferenceUpdater();
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

  private void initializePreferenceUpdater() {
    updatePreferences();
    IPreferenceStore toolsPreferences = PreferenceConstants.getPreferenceStore();
    toolsPreferences.addPropertyChangeListener(propertyChangeListener);
  }

  private void updatePreferences() {
    IPreferenceStore toolsPreferences = PreferenceConstants.getPreferenceStore();
    boolean useSpaces = toolsPreferences.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
    int size = toolsPreferences.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
    String ch = useSpaces ? CSSCorePreferenceNames.SPACE : CSSCorePreferenceNames.TAB;
    Preferences preferences = CSSCorePlugin.getDefault().getPluginPreferences();
    preferences.setValue(CSSCorePreferenceNames.INDENTATION_SIZE, size);
    preferences.setValue(CSSCorePreferenceNames.INDENTATION_CHAR, ch);
  }
}

/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal;

import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames;
import org.eclipse.wst.html.ui.internal.preferences.HTMLUIPreferenceNames;
import org.eclipse.wst.html.ui.internal.templates.TemplateContextTypeIdsHTML;
import org.eclipse.wst.sse.core.internal.validate.ValidationMessage;
import org.eclipse.wst.sse.ui.internal.provisional.registry.AdapterFactoryRegistry;
import org.eclipse.wst.sse.ui.internal.provisional.registry.AdapterFactoryRegistryImpl;

import java.io.IOException;

/**
 * The main plugin class to be used in the desktop.
 */
public class HTMLUIPlugin extends AbstractUIPlugin {
  public final static String ID = "org.eclipse.wst.html.ui"; //$NON-NLS-1$

  protected static HTMLUIPlugin instance = null;

  public static HTMLUIPlugin getDefault() {
    return instance;
  }

  public synchronized static HTMLUIPlugin getInstance() {
    return instance;
  }

  /**
   * The template store for the html editor.
   */
  private TemplateStore fTemplateStore;

  /**
   * The template context type registry for the html editor.
   */
  private ContextTypeRegistry fContextTypeRegistry;

  private IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      updatePreferences();
    }
  };

  public HTMLUIPlugin() {
    super();
    instance = this;
    initializePreferenceUpdater();
  }

  public AdapterFactoryRegistry getAdapterFactoryRegistry() {
    return AdapterFactoryRegistryImpl.getInstance();

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

  private void disableValidationWarnings() {
    IEclipsePreferences node = new InstanceScope().getNode(HTMLCorePlugin.getDefault().getBundle().getSymbolicName());
    node.putInt(HTMLCorePreferenceNames.ATTRIBUTE_UNDEFINED_NAME, ValidationMessage.IGNORE);
    node.putInt(HTMLCorePreferenceNames.ATTRIBUTE_UNDEFINED_VALUE, ValidationMessage.IGNORE);
    node.putInt(HTMLCorePreferenceNames.ATTRIBUTE_INVALID_NAME, ValidationMessage.IGNORE);
    node.putInt(HTMLCorePreferenceNames.ATTRIBUTE_VALUE_EQUALS_MISSING, ValidationMessage.IGNORE);
    node.putInt(HTMLCorePreferenceNames.ELEM_INVALID_TEXT, ValidationMessage.IGNORE);
    node.putInt(HTMLCorePreferenceNames.ATTRIBUTE_NAME_MISMATCH, ValidationMessage.IGNORE);
    node.putInt(HTMLCorePreferenceNames.ELEM_UNKNOWN_NAME, ValidationMessage.IGNORE);
    node.putInt(HTMLCorePreferenceNames.ELEM_INVALID_NAME, ValidationMessage.IGNORE);
  }

  private void initializePreferenceUpdater() {
    updatePreferences();
    disableValidationWarnings();
    IPreferenceStore toolsPreferences = PreferenceConstants.getPreferenceStore();
    toolsPreferences.addPropertyChangeListener(propertyChangeListener);
  }

  private void updatePreferences() {
    IPreferenceStore toolsPreferences = PreferenceConstants.getPreferenceStore();
    boolean useSpaces = toolsPreferences.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
    int size = toolsPreferences.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
    String ch = useSpaces ? HTMLCorePreferenceNames.SPACE : HTMLCorePreferenceNames.TAB;
    Preferences preferences = HTMLCorePlugin.getDefault().getPluginPreferences();
    preferences.setValue(HTMLCorePreferenceNames.INDENTATION_SIZE, useSpaces ? size : 1);
    preferences.setValue(HTMLCorePreferenceNames.INDENTATION_CHAR, ch);
  }
}

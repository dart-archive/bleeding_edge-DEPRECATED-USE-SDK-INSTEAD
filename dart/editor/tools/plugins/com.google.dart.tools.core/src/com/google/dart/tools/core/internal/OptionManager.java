/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.internal;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartPreferenceConstants;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages {@link DartCore} options.
 */
public class OptionManager {

  private static class EclipsePreferencesListener implements
      IEclipsePreferences.IPreferenceChangeListener {
    @Override
    public void preferenceChange(IEclipsePreferences.PreferenceChangeEvent event) {
      //TODO (pquitslund): respond to preference updates
    }
  }

  private HashMap<String, String> optionsCache;
  private HashSet<String> optionNames = new HashSet<String>(20);

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];
  private EclipsePreferencesListener instancePreferencesListener = new EclipsePreferencesListener();
  private IEclipsePreferences.IPreferenceChangeListener propertyListener;
  private IEclipsePreferences.IPreferenceChangeListener resourcesPropertyListener;
  
  private static final int PREF_INSTANCE = 0;
  private static final int PREF_DEFAULT = 1;

  /**
   * Listener on eclipse preferences default/instance node changes.
   */
  private IEclipsePreferences.INodeChangeListener instanceNodeListener = new IEclipsePreferences.INodeChangeListener() {
    @Override
    public void added(IEclipsePreferences.NodeChangeEvent event) {
      // do nothing
    }

    @Override
    public void removed(IEclipsePreferences.NodeChangeEvent event) {
      if (event.getChild() == OptionManager.this.preferencesLookup[PREF_INSTANCE]) {
        OptionManager.this.preferencesLookup[PREF_INSTANCE] = getInstanceScope().getNode(
            DartCore.PLUGIN_ID);
        OptionManager.this.preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(new EclipsePreferencesListener());
      }
    }
  };

  private IEclipsePreferences.INodeChangeListener defaultNodeListener = new IEclipsePreferences.INodeChangeListener() {
    @Override
    public void added(IEclipsePreferences.NodeChangeEvent event) {
      // do nothing
    }

    @Override
    public void removed(IEclipsePreferences.NodeChangeEvent event) {
      if (event.getChild() == OptionManager.this.preferencesLookup[PREF_DEFAULT]) {
        OptionManager.this.preferencesLookup[PREF_DEFAULT] = getDefaultScope().getNode(
            DartCore.PLUGIN_ID);
      }
    }
  };

  /**
   * The shared unique instance.
   */
  private static OptionManager instance;

  /**
   * Return the unique Option Manager instance.
   * 
   * @return the unique Option Manager
   */
  public synchronized static OptionManager getInstance() {

    if (instance == null) {
      instance = new OptionManager();
      instance.doStartup();
    }

    return instance;
  }

  public static void shutdown() {
    if (instance != null) {
      instance.doShutdown();
    }
  }

  private OptionManager() {
    initializePreferences();
  }

  /**
   * Utility method for returning one option value only. Equivalent to
   * <code>getOptions().get(optionName)</code> Note that it may answer <code>null</code> if this
   * option does not exist.
   * <p>
   * Helper constants have been defined on DartPreferenceConstants for each of the option IDs
   * (categorized in Code assist option ID, Compiler option ID and Core option ID) and some of their
   * acceptable values (categorized in Option value). Some options accept open value sets beyond the
   * documented constant values.
   * <p>
   * Note: each release may add new options.
   * 
   * @param optionName the name of the option whose value is to be returned
   * @return the value of a given option
   */
  public String getOption(String optionName) {
    DartCore.notYetImplemented();
    // if (DartCore.CORE_ENCODING.equals(optionName)){
    // return DartCore.getEncoding();
    // }
    String propertyName = optionName;
    if (optionNames.contains(propertyName)) {
      IPreferencesService service = Platform.getPreferencesService();
      String value = service.get(optionName, null, preferencesLookup);
      return value == null ? null : value.trim();
    }
    return null;
  }

  /**
   * Return the table of the current options. Initially, all options have their default values, and
   * this method returns a table that includes all known options.
   * <p>
   * Helper constants have been defined on DartPreferenceConstants for each of the option IDs
   * (categorized in Code assist option ID, Compiler option ID and Core option ID) and some of their
   * acceptable values (categorized in Option value). Some options accept open value sets beyond the
   * documented constant values.
   * <p>
   * Note: each release may add new options.
   * <p>
   * Returns a default set of options even if the platform is not running.
   * </p>
   * 
   * @return table of current settings of all options (key type: <code>String</code>; value type:
   *         <code>String
   *         </code>)
   */
  public HashMap<String, String> getOptions() {
    // return cached options if already computed
    HashMap<String, String> cachedOptions; // use a local variable to avoid race
                                           // condition (see
                                           // https://bugs.eclipse.org/bugs/show_bug.cgi?id=256329
                                           // )
    if ((cachedOptions = optionsCache) != null) {
      return new HashMap<String, String>(cachedOptions);
    }

    if (!Platform.isRunning()) {
      optionsCache = getDefaultOptionsNoInitialization();
      return new HashMap<String, String>(optionsCache);
    }
    // init
    HashMap<String, String> options = new HashMap<String, String>(10);
    IPreferencesService service = Platform.getPreferencesService();

    // set options using preferences service lookup
    Iterator<String> iterator = optionNames.iterator();
    while (iterator.hasNext()) {
      String propertyName = iterator.next();
      String propertyValue = service.get(propertyName, null, preferencesLookup);
      if (propertyValue != null) {
        options.put(propertyName, propertyValue);
      }
    }

    optionsCache = new HashMap<String, String>(options);

    // return built map
    return options;

  }

  private void doShutdown() {

    IEclipsePreferences preferences = getInstanceScope().getNode(DartCore.PLUGIN_ID);
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      DartCore.logError("Could not save DartCore preferences", e); //$NON-NLS-1$
    }

    // Stop listening to preferences changes
    preferences.removePreferenceChangeListener(propertyListener);
    ((IEclipsePreferences) preferencesLookup[PREF_DEFAULT].parent()).removeNodeChangeListener(defaultNodeListener);
    preferencesLookup[PREF_DEFAULT] = null;
    ((IEclipsePreferences) preferencesLookup[PREF_INSTANCE].parent()).removeNodeChangeListener(instanceNodeListener);
    preferencesLookup[PREF_INSTANCE].removePreferenceChangeListener(instancePreferencesListener);
    preferencesLookup[PREF_INSTANCE] = null;
    String resourcesPluginId = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
    getInstanceScope().getNode(resourcesPluginId).removePreferenceChangeListener(
        resourcesPropertyListener);

    // wait for the initialization job to finish
    try {
      Job.getJobManager().join(DartCore.PLUGIN_ID, null);
    } catch (InterruptedException e) {
      // ignore
    }

  }

  private void doStartup() {

    try {

      // Initialize eclipse preferences
      initializePreferences();

      // Listen to preference changes
      propertyListener = new IEclipsePreferences.IPreferenceChangeListener() {
        @Override
        public void preferenceChange(PreferenceChangeEvent event) {
          OptionManager.this.optionsCache = null;
        }
      };
      getInstanceScope().getNode(DartCore.PLUGIN_ID).addPreferenceChangeListener(propertyListener);

      // listen for encoding changes (see
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=255501 )
      resourcesPropertyListener = new IEclipsePreferences.IPreferenceChangeListener() {
        @Override
        public void preferenceChange(PreferenceChangeEvent event) {
          if (ResourcesPlugin.PREF_ENCODING.equals(event.getKey())) {
            OptionManager.this.optionsCache = null;
          }
        }
      };
      String resourcesPluginId = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
      getInstanceScope().getNode(resourcesPluginId).addPreferenceChangeListener(
          resourcesPropertyListener);

    } catch (RuntimeException e) {
      shutdown();
      throw e;
    }
  }

  // Do not modify without modifying getDefaultOptions()
  private HashMap<String, String> getDefaultOptionsNoInitialization() {

    DartCore.notYetImplemented();

    // Map defaultOptionsMap = new CompilerOptions().getMap(); // compiler
    // defaults
    Map<String, String> defaultOptionsMap = new HashMap<String, String>();

    // Formatter settings
    defaultOptionsMap.putAll(DefaultCodeFormatterConstants.getEclipseDefaultSettings());

    // CodeAssist settings
    defaultOptionsMap.put(
        DartPreferenceConstants.CODEASSIST_VISIBILITY_CHECK,
        DartPreferenceConstants.DISABLED);
    defaultOptionsMap.put(
        DartPreferenceConstants.CODEASSIST_DEPRECATION_CHECK,
        DartPreferenceConstants.DISABLED);
    defaultOptionsMap.put(
        DartPreferenceConstants.CODEASSIST_IMPLICIT_QUALIFICATION,
        DartPreferenceConstants.DISABLED);
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_FIELD_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_STATIC_FIELD_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_STATIC_FINAL_FIELD_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_LOCAL_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_ARGUMENT_PREFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_FIELD_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_STATIC_FIELD_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_STATIC_FINAL_FIELD_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_LOCAL_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(DartPreferenceConstants.CODEASSIST_ARGUMENT_SUFFIXES, ""); //$NON-NLS-1$
    defaultOptionsMap.put(
        DartPreferenceConstants.CODEASSIST_FORBIDDEN_REFERENCE_CHECK,
        DartPreferenceConstants.ENABLED);
    defaultOptionsMap.put(
        DartPreferenceConstants.CODEASSIST_DISCOURAGED_REFERENCE_CHECK,
        DartPreferenceConstants.DISABLED);
    defaultOptionsMap.put(
        DartPreferenceConstants.CODEASSIST_CAMEL_CASE_MATCH,
        DartPreferenceConstants.ENABLED);

    return new HashMap<String, String>(defaultOptionsMap);
  }

  private IScopeContext getDefaultScope() {
    return DefaultScope.INSTANCE;
  }

  private IScopeContext getInstanceScope() {
    return InstanceScope.INSTANCE;
  }

  private void initializePreferences() {
    // Create lookups
    preferencesLookup[PREF_INSTANCE] = getInstanceScope().getNode(DartCore.PLUGIN_ID);
    preferencesLookup[PREF_DEFAULT] = getDefaultScope().getNode(DartCore.PLUGIN_ID);

    // Listen to instance preferences node removal from parent in order to
    // refresh stored one
    instanceNodeListener = new IEclipsePreferences.INodeChangeListener() {
      @Override
      public void added(IEclipsePreferences.NodeChangeEvent event) {
        // do nothing
      }

      @Override
      public void removed(IEclipsePreferences.NodeChangeEvent event) {
        if (event.getChild() == OptionManager.this.preferencesLookup[PREF_INSTANCE]) {
          OptionManager.this.preferencesLookup[PREF_INSTANCE] = getInstanceScope().getNode(
              DartCore.PLUGIN_ID);
          OptionManager.this.preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(new EclipsePreferencesListener());
        }
      }
    };
    ((IEclipsePreferences) preferencesLookup[PREF_INSTANCE].parent()).addNodeChangeListener(instanceNodeListener);
    preferencesLookup[PREF_INSTANCE].addPreferenceChangeListener(instancePreferencesListener = new EclipsePreferencesListener());

    // Listen to default preferences node removal from parent in order to
    // refresh stored one
    defaultNodeListener = new IEclipsePreferences.INodeChangeListener() {
      @Override
      public void added(IEclipsePreferences.NodeChangeEvent event) {
        // do nothing
      }

      @Override
      public void removed(IEclipsePreferences.NodeChangeEvent event) {
        if (event.getChild() == OptionManager.this.preferencesLookup[PREF_DEFAULT]) {
          OptionManager.this.preferencesLookup[PREF_DEFAULT] = getDefaultScope().getNode(
              DartCore.PLUGIN_ID);
        }
      }
    };
    ((IEclipsePreferences) preferencesLookup[PREF_DEFAULT].parent()).addNodeChangeListener(defaultNodeListener);
  }

}

/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.folding;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.text.folding.IDartFoldingStructureProvider;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class JavaFoldingStructureProviderRegistry {

  private static final String EXTENSION_POINT = "foldingStructureProviders"; //$NON-NLS-1$

  /** The map of descriptors, indexed by their identifiers. */
  private Map fDescriptors;

  /**
   * Creates a new instance.
   */
  public JavaFoldingStructureProviderRegistry() {
  }

  /**
   * Instantiates and returns the provider that is currently configured in the preferences.
   * 
   * @return the current provider according to the preferences
   */
  public IDartFoldingStructureProvider getCurrentFoldingProvider() {
    IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
    String currentProviderId = preferenceStore.getString(PreferenceConstants.EDITOR_FOLDING_PROVIDER);
    JavaFoldingStructureProviderDescriptor desc = getFoldingProviderDescriptor(currentProviderId);

    // Fallback to default if extension has gone
    if (desc == null) {
      String message = Messages.format(
          FoldingMessages.JavaFoldingStructureProviderRegistry_warning_providerNotFound_resetToDefault,
          currentProviderId);
      DartToolsPlugin.log(new Status(IStatus.WARNING, DartToolsPlugin.getPluginId(), IStatus.OK,
          message, null));

      String defaultProviderId = preferenceStore.getDefaultString(PreferenceConstants.EDITOR_FOLDING_PROVIDER);

      desc = getFoldingProviderDescriptor(defaultProviderId);
      Assert.isNotNull(desc);

      preferenceStore.setToDefault(PreferenceConstants.EDITOR_FOLDING_PROVIDER);
    }

    try {
      return desc.createProvider();
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
      return null;
    }
  }

  /**
   * Returns the folding provider descriptor with identifier <code>id</code> or <code>null</code> if
   * no such provider is registered.
   * 
   * @param id the identifier for which a provider is wanted
   * @return the corresponding provider descriptor, or <code>null</code> if none can be found
   */
  public JavaFoldingStructureProviderDescriptor getFoldingProviderDescriptor(String id) {
    synchronized (this) {
      ensureRegistered();
      return (JavaFoldingStructureProviderDescriptor) fDescriptors.get(id);
    }
  }

  /**
   * Returns an array of <code>JavaFoldingStructureProviderDescriptor</code> describing all
   * extension to the <code>foldingProviders</code> extension point.
   * 
   * @return the list of extensions to the <code>quickDiffReferenceProvider</code> extension point
   */
  public JavaFoldingStructureProviderDescriptor[] getFoldingProviderDescriptors() {
    synchronized (this) {
      ensureRegistered();
      return (JavaFoldingStructureProviderDescriptor[]) fDescriptors.values().toArray(
          new JavaFoldingStructureProviderDescriptor[fDescriptors.size()]);
    }
  }

  /**
   * Reads all extensions.
   * <p>
   * This method can be called more than once in order to reload from a changed extension registry.
   * </p>
   */
  public void reloadExtensions() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    Map map = new HashMap();

    IConfigurationElement[] elements = registry.getConfigurationElementsFor(
        DartToolsPlugin.getPluginId(), EXTENSION_POINT);
    for (int i = 0; i < elements.length; i++) {
      JavaFoldingStructureProviderDescriptor desc = new JavaFoldingStructureProviderDescriptor(
          elements[i]);
      map.put(desc.getId(), desc);
    }

    synchronized (this) {
      fDescriptors = Collections.unmodifiableMap(map);
    }
  }

  /**
   * Ensures that the extensions are read and stored in <code>fDescriptors</code>.
   */
  private void ensureRegistered() {
    if (fDescriptors == null) {
      reloadExtensions();
    }
  }

}

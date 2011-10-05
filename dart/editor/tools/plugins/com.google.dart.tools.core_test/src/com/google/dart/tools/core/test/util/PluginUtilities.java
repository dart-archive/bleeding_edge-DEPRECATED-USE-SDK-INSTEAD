/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.test.util;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;

import java.net.URL;

/**
 * The class <code>PluginUtilities</code> defines utility methods for working with plug-ins.
 */
public class PluginUtilities {
  /**
   * Return the unique identifier of the given plug-in.
   * 
   * @return the unique identifier of the given plug-in
   */
  public static String getId(Plugin plugin) {
    return plugin.getBundle().getSymbolicName();
  }

  /**
   * Return an URL representing the given plug-in's installation directory.
   * 
   * @param plugin the plug-in
   * @return the given plug-in's installation directory
   */
  public static URL getInstallUrl(Plugin plugin) {
    if (plugin == null) {
      return null;
    }
    return plugin.getBundle().getEntry("/");
  }

  /**
   * Return an URL representing the installation directory of the plug-in with the given identifier,
   * or <code>null</code> if there is no plug-in with the given identifier.
   * 
   * @param pluginId the identifier of the plug-in
   * @return the specified plug-in's installation directory
   */
  public static URL getInstallUrl(String pluginId) {
    Bundle bundle;

    bundle = Platform.getBundle(pluginId);
    if (bundle == null) {
      return null;
    }
    return bundle.getEntry("/");
  }

  /**
   * Return the name of the given plug-in. If the plug-in does not have a name, return the unique
   * identifier for the plug-in instead.
   * 
   * @return the name of the given plug-in
   */
  public static String getName(Plugin plugin) {
    String label;
    Object bundleName;

    label = null;
    bundleName = plugin.getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
    if (bundleName instanceof String) {
      label = (String) bundleName;
    }
    if (label == null || label.trim().length() == 0) {
      return getId(plugin);
    }
    return label;
  }

  /**
   * Return an URL for the file located within the installation directory of the given plug-in that
   * has the given relative path.
   * 
   * @param pluginId the identifier for the plug-in
   * @param relativePath the relative path of the file within the installation directory
   * @return the URL for the specified file
   */
  public static URL getUrl(Plugin plugin, String relativePath) {
    if (plugin == null || relativePath == null) {
      return null;
    }
    return plugin.getBundle().getEntry(relativePath);
  }

  /**
   * Return an URL for the file located within the installation directory of the plug-in that has
   * the given identifier that has the given relative path.
   * 
   * @param pluginId the identifier for the plug-in
   * @param relativePath the relative path of the file within the installation directory
   * @return the URL for the specified file
   */
  public static URL getUrl(String pluginId, String relativePath) {
    Bundle bundle;

    if (pluginId == null || relativePath == null) {
      return null;
    }
    bundle = Platform.getBundle(pluginId);
    if (bundle != null) {
      return bundle.getEntry(relativePath);
    }
    return null;
  }

  /**
   * Return the version identifier associated with the plug-in with the given identifier, or
   * <code>null</code> if there is no such plug-in.
   * 
   * @param pluginId the identifier of the plug-in
   * @return the version identifier for the specified plug-in
   */
  public static String getVersionString(Plugin plugin) {
    if (plugin == null) {
      return null;
    }
    return (String) plugin.getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
  }

  /**
   * Return the version identifier associated with the plug-in with the given identifier, or
   * <code>null</code> if there is no such plug-in.
   * 
   * @param pluginId the identifier of the plug-in
   * @return the version identifier for the specified plug-in
   */
  public static String getVersionString(String pluginId) {
    Bundle bundle;

    bundle = Platform.getBundle(pluginId);
    if (bundle == null) {
      return null;
    }
    return (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private PluginUtilities() {
  }
}

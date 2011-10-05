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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.SystemLibrary;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.Util;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the collection of libraries declared using the "libraries" extension point
 */
public class BundledSystemLibraryManager {
  public static final String BUNDLED_LIBRARY_PREFIX = "com.google.dart.library.";

  private static final String LIBRARIES_EXTENSION_POINT_ID = DartCore.PLUGIN_ID + ".libraries";

  private static boolean initialized = false;
  private static Collection<String> allLibrarySpecs;
  private static Collection<String> visibleLibrarySpecs;
  private static SystemLibrary[] allLibraries;
  private static Map<URI, URI> longToShortUriMap;

  /**
   * Answer an array of all bundled libraries.
   * 
   * @return an array of libraries (not <code>null</code>, contains no <code>null</code>s)
   */
  public static SystemLibrary[] getAllLibraries() {
    initLibraries();
    return allLibraries;
  }

  /**
   * Answer a collection of all bundled library URL specs (e.g. "dart:dom").
   * 
   * @return a collection of specs (not <code>null</code>, contains no <code>null</code>s)
   */
  public static Collection<String> getAllLibrarySpecs() {
    initLibraries();
    return allLibrarySpecs;
  }

  /**
   * Answer the original "dart:<libname>" URI for the specified resolved URI or <code>null</code> if
   * it does not map to a
   */
  public static URI getShortUri(URI uri) {
    initLibraries();
    return longToShortUriMap.get(uri);
  }

  /**
   * Answer a collection of library URL specs (e.g. "dart:dom") which should be visible to the user
   * for import.
   * 
   * @return a collection of specs (not <code>null</code>, contains no <code>null</code>s)
   */
  public static Collection<String> getVisibleLibrarySpecs() {
    initLibraries();
    return visibleLibrarySpecs;
  }

  /**
   * Initialize the system libraries based upon the libraries declared using the "libraries"
   * extension point. Use {@link #initLibraries()} to set the system libraries once, and this method
   * when testing to reset the system libraries based upon the libraries declared using the
   * "libraries" extension point.
   */
  private static void computeLibraries() {
    ArrayList<SystemLibrary> libraries = new ArrayList<SystemLibrary>();
    allLibrarySpecs = new ArrayList<String>();
    visibleLibrarySpecs = new ArrayList<String>();
    longToShortUriMap = new HashMap<URI, URI>();

    IExtensionRegistry registery = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registery.getExtensionPoint(LIBRARIES_EXTENSION_POINT_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

    for (IConfigurationElement elem : elements) {
      String pluginId = elem.getDeclaringExtension().getNamespaceIdentifier();
      String shortName = elem.getAttribute("name");
      if (shortName == null || shortName.length() == 0) {
        warning(pluginId, "missing name attribute");
        continue;
      }
      String host = elem.getAttribute("host");
      if (host == null || host.length() == 0) {
        warning(pluginId, "missing host attribute");
        continue;
      }
      String pathToLib = elem.getAttribute("path");
      if (pathToLib == null || pathToLib.length() == 0) {
        warning(pluginId, "missing path attribute");
        continue;
      }
      Bundle bundle = Platform.getBundle(pluginId);
      BundledSystemLibrary lib = new BundledSystemLibrary(shortName, host, pathToLib, bundle);
      libraries.add(lib);

      String libSpec = "dart:" + shortName;
      allLibrarySpecs.add(libSpec);
      boolean visible = !("false".equals(elem.getAttribute("visible")));
      if (visible) {
        visibleLibrarySpecs.add(libSpec);
      }

      URI libUri;
      URI expandedUri;
      try {
        libUri = new URI(libSpec);
        expandedUri = new URI("dart://" + host + "/" + pathToLib);
      } catch (URISyntaxException e) {
        throw new AssertionError(e);
      }
      URI resolvedUri = lib.translateUri(expandedUri);
      longToShortUriMap.put(resolvedUri, libUri);
      longToShortUriMap.put(expandedUri, libUri);
    }
    allLibraries = libraries.toArray(new SystemLibrary[libraries.size()]);
  }

  private static synchronized void initLibraries() {
    if (!initialized) {
      initialized = true;
      computeLibraries();
    }
  }

  private static void warning(String pluginId, String message) {
    Util.log(new Status(IStatus.WARNING, DartCore.PLUGIN_ID, message
        + " in libraries declaration in " + pluginId));
  }
}

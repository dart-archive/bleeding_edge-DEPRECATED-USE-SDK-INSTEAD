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
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class <code>SystemLibraryManagerProvider</code> manages the {@link SystemLibraryManager
 * system library managers} used by the tools.
 */
public class SystemLibraryManagerProvider {

  /**
   * Specialized {@link SystemLibraryManager} for accessing libraries bundled with the editor.
   */
  static abstract class EditorLibraryManager extends SystemLibraryManager {
    private List<SystemLibrary> libraries;
    private Map<URI, URI> longToShortUriMap;

    /**
     * Answer a collection of all bundled library URL specs (e.g. "dart:dom").
     * 
     * @return a collection of specs (not <code>null</code>, contains no <code>null</code>s)
     */
    public Collection<String> getAllLibrarySpecs() {
      Collection<String> result = new ArrayList<String>(libraries.size());
      for (SystemLibrary lib : libraries) {
        result.add("dart:" + lib.getShortName());
      }
      return result;
    }

    /**
     * Scan the directory returned by {@link #getLibrariesDir()} looking for libraries of the form
     * libraries/<name>/<name>.dart and libraries/<name>/<name>_impl.dart
     */
    @Override
    protected SystemLibrary[] getDefaultLibraries() {
      File librariesDir = getLibrariesDir();
      libraries = new ArrayList<SystemLibrary>();
      longToShortUriMap = new HashMap<URI, URI>();
      if (librariesDir.isDirectory()) {
        for (String name : librariesDir.list()) {
          File dir = new File(librariesDir, name);
          if (!dir.isDirectory()) {
            if (DartCoreDebug.DARTLIB) {
              DartCore.logInformation("Skipping " + dir);
            }
            continue;
          }
          if (!addLib(name, name, dir)) {

            // Hack for core library because it does not follow the naming convention
            if ("core".equals(name)) {
              if (addLib(name, name, dir, "corelib.dart")) {
                addLib(name, name + "_impl", dir, "corelib_impl.dart");
                continue;
              }
            }

            DartCore.logInformation("Expected " + name + ".dart in " + dir);
            continue;
          }
          if (!addLib(name, name + "_impl", dir)) {
            continue;
          }
        }
      }
      return libraries.toArray(new SystemLibrary[libraries.size()]);
    }

    abstract File getLibrariesDir();

    /**
     * Answer the original "dart:<libname>" URI for the specified resolved URI or <code>null</code>
     * if it does not map to a short URI.
     */
    URI getShortUri(URI uri) {
      return longToShortUriMap.get(uri);
    }

    private boolean addLib(String host, String name, File dir) {
      return addLib(host, name, dir, name + ".dart");
    }

    private boolean addLib(String host, String name, File dir, String libFileName)
        throws AssertionError {
      File libFile = new File(dir, libFileName);
      if (!libFile.isFile()) {
        return false;
      }
      if (DartCoreDebug.DARTLIB) {
        DartCore.logInformation("Found dart:" + name + " in " + dir);
      }
      SystemLibrary lib = new SystemLibrary(name, host, libFileName, dir);
      libraries.add(lib);
      String libSpec = "dart:" + name;
      URI libUri;
      URI expandedUri;
      try {
        libUri = new URI(libSpec);
        expandedUri = new URI("dart://" + host + "/" + libFileName);
      } catch (URISyntaxException e) {
        throw new AssertionError(e);
      }
      URI resolvedUri = lib.translateUri(expandedUri);
      longToShortUriMap.put(resolvedUri, libUri);
      longToShortUriMap.put(expandedUri, libUri);
      return true;
    }
  }

  /**
   * The single system library manager currently being managed.
   */
  private static EditorLibraryManager MANAGER;

  /**
   * Answer a collection of all bundled library URL specs (e.g. "dart:dom").
   * 
   * @return a collection of specs (not <code>null</code>, contains no <code>null</code>s)
   */
  public static Collection<String> getAllLibrarySpecs() {
    initManager();
    return MANAGER.getAllLibrarySpecs();
  }

  /**
   * Answer the original "dart:<libname>" URI for the specified resolved URI or <code>null</code> if
   * it does not map to a short URI.
   */
  public static URI getShortUri(URI uri) {
    initManager();
    return MANAGER.getShortUri(uri);
  }

  /**
   * Return the single system library manager currently being managed.
   * 
   * @return the single system library manager currently being managed
   */
  public static SystemLibraryManager getSystemLibraryManager() {
    initManager();
    return MANAGER;
  }

  /**
   * Locate the libraries directory
   */
  private static File findLibraries() {
    File installDir = new File(Platform.getInstallLocation().getURL().getFile());
    File libDir = new File(installDir, "libraries");
    if (!libDir.exists()) {
      DartCore.logError("Missing libraries directory: " + libDir);
    }
    if (DartCoreDebug.DARTLIB) {
      DartCore.logInformation("Reading bundled libraries from " + libDir);
    }
    return libDir;
  }

  private static void initManager() {
    if (MANAGER == null) {
      MANAGER = new EditorLibraryManager() {

        @Override
        File getLibrariesDir() {
          return findLibraries();
        }

      };
    }
  }
}

/*
 * Copyright 2011 Dart project authors.
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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specialized {@link SystemLibraryManager} for accessing libraries bundled with the editor.
 * 
 * @see SystemLibraryManagerProvider#getSystemLibraryManager()
 * @see SystemLibraryManagerProvider#getDartCLibraryManager()
 * @see SystemLibraryManagerProvider#getVmLibraryManager()
 */
public abstract class EditorLibraryManager extends SystemLibraryManager {
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
   * Answer the original "dart:<libname>" URI for the specified resolved URI or <code>null</code> if
   * it does not map to a short URI.
   */
  public URI getShortUri(URI uri) {
    return longToShortUriMap.get(uri);
  }

  /**
   * Scan the directory returned by {@link #getLibrariesDir()} looking for libraries of the form
   * libraries/<name>/<name>_<platform>.dart and libraries/<name>/<name>.dart where <platform> is
   * the value returned by {@link #getPlatformName()}. This is called by the superclass constructor,
   * thus ensuring that the {@link #libraries} field is always initialized.
   */
  @Override
  protected SystemLibrary[] getDefaultLibraries() {
    final String platformName = getPlatformName();
    final File librariesDir = getLibrariesDir();
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

        // First attempt to locate a platform specific library file <name>_<platform>.dart
        // (e.g. "core_runtime.dart") and if that does not exist then "<name>.dart"

        String platformLibFileName = name + "_" + platformName + ".dart";
        if (addLib(name, name, dir, platformLibFileName)) {
          continue;
        }
        String commonLibFileName = name + ".dart";
        if (addLib(name, name, dir, commonLibFileName)) {
          continue;
        }

        // Hack for core library because it does not follow the naming convention

        if ("core".equals(name) && addLib(name, name, dir, "corelib.dart")) {
          continue;
        }
        if ("coreimpl".equals(name) && addLib(name, name, dir, "corelib_impl.dart")) {
          continue;
        }

        DartCore.logInformation("Expected " + platformLibFileName + " or " + commonLibFileName
            + " in " + dir);
      }
    }
    return libraries.toArray(new SystemLibrary[libraries.size()]);
  }

  /**
   * Answer the directory containing the library folders. Typically this would be the "libraries" or
   * "sdk/lib" directory.
   * 
   * <pre>
   * install-directory/
   *    sdk/
   *       lib/
   *          core/
   *             core_runtime.dart
   *             core_compiler.dart
   *             ... other core files ...
   *          coreimpl/
   *             coreimpl_runtime.dart
   *             ... other coreimpl files ...
   *          dom/
   *             dom.dart
   *             ... other dom files ...
   *          ... other library directories ...
   * </pre>
   */
  abstract File getLibrariesDir();

  /**
   * Answer the platform name (DartC = "compiler", VM = "runtime") used when locating platform
   * specific library files (e.g. "core_runtime.dart" vs "core.dart").
   */
  abstract String getPlatformName();

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

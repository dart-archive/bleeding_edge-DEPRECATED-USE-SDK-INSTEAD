/*
 * Copyright 2012 Dart project authors.
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

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
   * Answer the directory containing the library folders. Typically this would be the "dart-sdk/lib"
   * directory.
   * 
   * <pre>
   * install-directory/
   *    dart-sdk/
   *       lib/
   *          core/
   *             core_runtime.dart
   *             core_frog.dart
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
  public abstract File getLibrariesDir();

  /**
   * Answer the original "dart:<libname>" URI for the specified resolved URI or <code>null</code> if
   * it does not map to a short URI.
   */
  public URI getShortUri(URI uri) {
    return longToShortUriMap.get(uri);
  }

  /**
   * Answer the URI against which {@link #getDefaultLibraries()} resolves relative paths
   * 
   * @return the URI (not <code>null</code>)
   */
  protected abstract URI getBaseUri();

  /**
   * Scan the directory returned by {@link #getLibrariesDir()} looking for libraries of the form
   * libraries/<name>/<name>_<platform>.dart and libraries/<name>/<name>.dart where <platform> is
   * the value returned by {@link #getPlatformName()}. This is called by the superclass constructor,
   * thus ensuring that the {@link #libraries} field is always initialized.
   */
  @Override
  protected SystemLibrary[] getDefaultLibraries() {
    libraries = new ArrayList<SystemLibrary>();
    longToShortUriMap = new HashMap<URI, URI>();

    // Cycle through the import.config, extracting explicit mappings and searching directories

    URI base = getBaseUri();
    Properties importConfig = getImportConfig();
    HashSet<String> explicitShortNames = new HashSet<String>();
    for (Entry<Object, Object> entry : importConfig.entrySet()) {
      String shortName = ((String) entry.getKey()).trim();
      String path = ((String) entry.getValue()).trim();

      File file;
      try {
        file = new File(base.resolve(new URI(null, null, path, null)).normalize());
      } catch (URISyntaxException e) {
        DartCore.logError("Failed to resolve " + path + " against " + base);
        continue;
      }
      if (!file.exists()) {
        DartCore.logError("File for " + shortName + " does not exist - " + file);
        continue;
      }

      // If the shortName ends with ":" then search the associated directory for libraries

      if (shortName.endsWith(":")) {
        if (!file.isDirectory()) {
          DartCore.logError("Expected directory for " + shortName + " - " + file);
          continue;
        }
        if (DartCoreDebug.DARTLIB) {
          DartCore.logInformation("Scanning " + file);
        }
        for (File child : file.listFiles()) {
          String host = child.getName();
          // Do not overwrite explicit shortName to dart file mappings
          if (explicitShortNames.contains(shortName + host)) {
            if (DartCoreDebug.DARTLIB) {
              DartCore.logInformation("Skipping " + shortName + host
                  + " as it is explicitly defined");
            }
            continue;
          }
          if (!child.isDirectory()) {
            if (DartCoreDebug.DARTLIB) {
              DartCore.logInformation("Skipping " + child);
            }
            continue;
          }
          File dartFile = new File(child, child.getName() + ".dart");
          if (!dartFile.isFile()) {
            if (DartCoreDebug.DARTLIB) {
              DartCore.logInformation("Skipping " + child + " because no " + dartFile.getName());
            }
            continue;
          }
          //longToShortUriMap.put(shortUri, dartFile.toURI());
          addLib(shortName, host, host, child, dartFile.getName());
        }
      } else {

        // Otherwise treat the entry as an explicit shortName to dart file mapping

        int index = shortName.indexOf(':');
        if (index == -1) {
          DartCore.logError("Expected ':' in " + shortName);
          continue;
        }
        explicitShortNames.add(shortName);
        String scheme = shortName.substring(0, index + 1);
        String host = shortName.substring(index + 1);
        addLib(scheme, host, host, file.getParentFile(), file.getName());
      }
    }
    return libraries.toArray(new SystemLibrary[libraries.size()]);
  }

  /**
   * Read the import.config content and return it as a collection of key/value pairs
   */
  protected Properties getImportConfig() {
    Properties importConfig = new Properties();
    InputStream stream = getImportConfigStream();
    try {
      importConfig.load(stream);
    } catch (IOException e) {
      DartCore.logError("Failed to load import.config", e);
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
        DartCore.logError("Failed to close import.config reader", e);
      }
    }
    return importConfig;
  }

  /**
   * Answer a reader on the "import.config" file, typically residing in the editor installation
   * directory. It is the responsibility of the caller to close the reader when finished.
   */
  protected abstract InputStream getImportConfigStream();

  /**
   * Answer the platform name (DartC = "compiler", VM = "runtime") used when locating platform
   * specific library files (e.g. "core_runtime.dart" vs "core.dart").
   */
  abstract String getPlatformName();

  private boolean addLib(String scheme, String host, String name, File dir, String libFileName)
      throws AssertionError {
    File libFile = new File(dir, libFileName);
    if (!libFile.isFile()) {
      return false;
    }
    if (DartCoreDebug.DARTLIB) {
      DartCore.logInformation("Found " + scheme + name + " in " + libFile);
    }
    SystemLibrary lib = new SystemLibrary(name, host, libFileName, dir);
    libraries.add(lib);
    String libSpec = scheme + name;
    URI libUri;
    URI expandedUri;
    try {
      libUri = new URI(libSpec);
      expandedUri = new URI("dart:" + "//" + host + "/" + libFileName);
    } catch (URISyntaxException e) {
      throw new AssertionError(e);
    }
    URI resolvedUri = lib.translateUri(expandedUri);
    longToShortUriMap.put(resolvedUri, libUri);
    longToShortUriMap.put(expandedUri, libUri);
    return true;
  }
}

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

package com.google.dart.tools.core.internal.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * This canonicalization manager class caches the results from calling the File.getCanonicalFile()
 * method. When symlinks are involved, that method can be extremely expensive.
 */
public class CanonicalizationManager {
  private static CanonicalizationManager manager = new CanonicalizationManager();

  /**
   * @return the singleton instance of the CanonicalizationManager
   */
  public static CanonicalizationManager getManager() {
    return manager;
  }

  private Map<File, URI> canonicalMap = new HashMap<File, URI>();

  private CanonicalizationManager() {

  }

  /**
   * Given a File, return the canonicalized path as a URI.
   * 
   * @param file the target file
   * @return the canonicalized path as a URI
   */
  public URI getCanonicalUri(File file) {
    if (!canonicalMap.containsKey(file)) {
      canonicalMap.put(file, getCanonicalUriImpl(file));
    }

    return canonicalMap.get(file);
  }

  /**
   * Given a URI, return the canonicalized file path as a URI.
   * 
   * @param uri the target URI
   * @return the canonicalized path as a URI
   */
  public URI getCanonicalUri(URI uri) {
    if (uri == null) {
      return null;
    } else {
      return getCanonicalUri(new File(uri));
    }
  }

  /**
   * Reset any cached information. This should be called when the system manipulates symlinks.
   */
  public void reset() {
    canonicalMap.clear();
  }

  private URI getCanonicalUriImpl(File file) {
    try {
      return file.getAbsoluteFile().getCanonicalFile().toURI();
    } catch (IOException exception) {
      return file.getAbsoluteFile().toURI();
    }
  }

}

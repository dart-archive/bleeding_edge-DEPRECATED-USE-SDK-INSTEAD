/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.tools.core.utilities.net;

import java.io.File;
import java.net.URI;

/**
 * The class <code>URIUtilities</code> defines utility methods for working with instances of the
 * class {@link URI}.
 */
public final class URIUtilities {
  /**
   * Return an absolute URI representing the same resource as the given URI, or the given URI if it
   * is already absolute or if an absolute version of the URI cannot be constructed.
   * 
   * @param uri the URI to be made absolute
   * @return an absolute URI representing the same resource as the given URI
   */
  public static URI makeAbsolute(URI uri) {
    if (uri == null) {
      return null;
    }
    String scheme = uri.getScheme();
    if (scheme == null || scheme.isEmpty() || scheme.equals("file")) {
      String path = uri.getSchemeSpecificPart();
      if (path != null) {
        return new File(path).getAbsoluteFile().toURI();
      }
    }
    return uri;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private URIUtilities() {
    super();
  }
}

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

import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

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
   * Attempt to resolve the given URI. Return the resolved URI, or the original URI if the original
   * URI could not be resolved.
   * 
   * @param uri the URI to be resolved
   * @return the resolved URI
   */
  public static URI safelyResolveDartUri(URI uri) {
    try {
      URI resolvedUri = SystemLibraryManagerProvider.getSystemLibraryManager().resolveDartUri(uri);
      if (resolvedUri != null) {
        return resolvedUri;
      }
      // TODO(devoncarew): we need to track down why the URI is invalid
      // ex.: dart://core/corelib_impl.dart/corelib_impl.dart
    } catch (RuntimeException exception) {
      // Fall through to returned the URI that was provided.
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

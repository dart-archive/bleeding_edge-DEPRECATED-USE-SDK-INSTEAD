/*
 * Copyright 2012, the Dart project authors.
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

import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

/**
 * The class <code>URIUtilities</code> defines utility methods for working with instances of the
 * class {@link URI}.
 */
public final class URIUtilities {
  /**
   * @return <code>true</code> if given {@link URI} has "file" scheme.
   */
  public static boolean isFileUri(URI uri) {
    if (uri == null) {
      return false;
    }
    return "file".equals(uri.getScheme());
  }

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
   * @return the relative {@link URI}, using ".." if needed.
   *         <p>
   *         http://stackoverflow.com/questions/10801283/get-relative-path-of-two-uris-in-java
   */
  public static URI relativize(URI base, URI child) {
    // Normalize paths to remove . and .. segments
    base = base.normalize();
    child = child.normalize();

    // Split paths into segments
    String[] bParts = base.getPath().split("\\/");
    String[] cParts = child.getPath().split("\\/");

    // Discard trailing segment of base path
    if (bParts.length > 0 && !base.getPath().endsWith("/")) {
      bParts = Arrays.copyOf(bParts, bParts.length - 1);
    }

    // Remove common prefix segments
    int i = 0;
    while (i < bParts.length && i < cParts.length && bParts[i].equals(cParts[i])) {
      i++;
    }

    // Construct the relative path
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < (bParts.length - i); j++) {
      sb.append("../");
    }
    for (int j = i; j < cParts.length; j++) {
      if (j != i) {
        sb.append("/");
      }
      sb.append(cParts[j]);
    }

    return URI.create(sb.toString());
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
      URI resolvedUri = PackageLibraryManagerProvider.getPackageLibraryManager().resolveDartUri(uri);
      if (resolvedUri != null) {
        return resolvedUri;
      }
    } catch (RuntimeException exception) {
      // Fall through to returned the URI that was provided.
    }
    return uri;
  }

  /**
   * Convert from a non-uri encoded string to a uri encoded one.
   * 
   * @param str
   * @return the uri encoded input string
   */
  public static String uriEncode(String str) {
    StringBuilder builder = new StringBuilder(str.length() * 2);

    for (char c : str.toCharArray()) {
      switch (c) {
        case '%':
        case '?':
        case ';':
        case '#':
        case '"':
        case '\'':
        case '<':
        case '>':
        case ' ':
          // ' ' ==> "%20"
          builder.append('%');
          builder.append(Integer.toHexString(c));
          break;
        default:
          builder.append(c);
          break;
      }
    }

    return builder.toString();
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private URIUtilities() {
    super();
  }
}

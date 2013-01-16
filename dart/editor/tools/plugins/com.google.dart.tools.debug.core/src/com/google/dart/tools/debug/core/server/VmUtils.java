/*
 * Copyright (c) 2012, the Dart project authors.
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

package com.google.dart.tools.debug.core.server;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A utility class for the VM debugger. This normalizes between the url format that the VM expects
 * and the format the the editor uses.
 */
public class VmUtils {
  private static final String VM_FORMAT = "file:///";
  private static final String ECLIPSE_FORMAT = "file:/";

  /**
   * Convert the given URL from Eclipse format (file:/) to VM format (file:///).
   */
  public static String eclipseUrlToVm(String url) {
    // file:/ --> file:///

    if (url == null) {
      return null;
    }

    if (url.startsWith(ECLIPSE_FORMAT)) {
      // Use the URI class to convert things like '%20' ==> ' '.
      // The VM also wants file urls to start with file:///, not file:/.
      URI uri = URI.create(url);
      url = uri.getScheme() + "://" + uri.getPath();
    }

    return url;
  }

  /**
   * Convert the given URL from VM format (file:///) to Eclipse format (file:/).
   */
  public static String vmUrlToEclipse(String url) {
    if (url == null) {
      return null;
    }

    if (url.startsWith(VM_FORMAT)) {
      try {
        // Convert things like ' ' to '%20'.
        // Also, file:/// --> file:/.
        URI uri = new URI("file", null, url.substring(VM_FORMAT.length() - 1), null);
        url = uri.toString();
      } catch (URISyntaxException e) {
        DartDebugCorePlugin.logError(e);
      }
    }

    return url;
  }

}

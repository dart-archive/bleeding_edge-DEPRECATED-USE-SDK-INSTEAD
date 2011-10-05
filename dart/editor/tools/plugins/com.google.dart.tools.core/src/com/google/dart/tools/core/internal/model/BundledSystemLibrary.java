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

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A system library bundled in an Eclipse plugin
 */
public class BundledSystemLibrary extends SystemLibrary {
  private final Bundle bundle;

  public BundledSystemLibrary(String shortName, String host, String pathToLib, Bundle bundle) {
    super(shortName, host, pathToLib, null);
    this.bundle = bundle;
  }

  @Override
  public URI translateUri(URI dartUri) {
    String path = dartUri.getPath();
    URL entry = bundle.getEntry(path);

    // If this is a runtime workbench, try the "bin" directory
    if (entry == null) {
      path = "/bin" + path;
      entry = bundle.getEntry(path);
    }

    if (entry == null) {
      return null;
    }

    try {
      URL resolvedUrl = FileLocator.resolve(entry);

      // We need to use the 3-arg constructor of URI in order to properly escape file system chars.
      URI resolvedUri = new URI(resolvedUrl.getProtocol(), resolvedUrl.getPath(), null);

      return resolvedUri;
    } catch (URISyntaxException e) {
      throw new AssertionError(e);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}

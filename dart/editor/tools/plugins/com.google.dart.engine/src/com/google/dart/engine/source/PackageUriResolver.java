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
package com.google.dart.engine.source;

import java.io.File;
import java.net.URI;

/**
 * Instances of the class <code>PackageUriResolver</code> resolve <code>package</code> URI's in the
 * context of an application.
 */
public class PackageUriResolver extends UriResolver {
  /**
   * The "packages" directory within the root directory (the directory that contains the
   * <code>.dart</code> file that defines the application).
   */
  private File packagesDirectory;

  /**
   * The name of the <code>package</code> scheme.
   */
  private static final String PACKAGE_SCHEME = "package";

  /**
   * The name of the "packages" directory.
   */
  private static final String PACKAGES_DIRECTORY_NAME = "packages";

  /**
   * Return <code>true</code> if the given URI is a <code>package</code> URI.
   * 
   * @param uri the URI being tested
   * @return <code>true</code> if the given URI is a <code>package</code> URI
   */
  public static boolean isPackageUri(URI uri) {
    return uri.getScheme().equals(PACKAGE_SCHEME);
  }

  /**
   * Initialize a newly created resolver to resolve <code>package</code> URI's relative to the given
   * root directory.
   * 
   * @param rootDirectory the directory that contains the root of the application
   */
  public PackageUriResolver(File rootDirectory) {
    this.packagesDirectory = new File(rootDirectory, PACKAGES_DIRECTORY_NAME);
  }

  @Override
  protected Source resolveAbsolute(SourceFactory factory, URI uri) {
    if (!isPackageUri(uri)) {
      return null;
    }
    String path = uri.getPath();
    if (path == null) {
      path = uri.getSchemeSpecificPart();
      if (path == null) {
        return null;
      }
    }
    return new SourceImpl(factory, new File(packagesDirectory, path));
  }
}

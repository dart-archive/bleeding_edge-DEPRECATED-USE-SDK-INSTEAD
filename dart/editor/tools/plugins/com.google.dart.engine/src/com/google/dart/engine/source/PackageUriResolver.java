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
 * Instances of the class {@code PackageUriResolver} resolve {@code package} URI's in the context of
 * an application.
 */
public class PackageUriResolver extends UriResolver {
  /**
   * The package directories that {@code package} URI's are assumed to be relative to.
   */
  private File[] packagesDirectories;

  /**
   * The name of the {@code package} scheme.
   */
  private static final String PACKAGE_SCHEME = "package";

  /**
   * Return {@code true} if the given URI is a {@code package} URI.
   * 
   * @param uri the URI being tested
   * @return {@code true} if the given URI is a {@code package} URI
   */
  public static boolean isPackageUri(URI uri) {
    return uri.getScheme().equals(PACKAGE_SCHEME);
  }

  /**
   * Initialize a newly created resolver to resolve {@code package} URI's relative to the given
   * package directories.
   * 
   * @param packagesDirectories the package directories that {@code package} URI's are assumed to be
   *          relative to
   */
  public PackageUriResolver(File... packagesDirectories) {
    if (packagesDirectories.length < 1) {
      throw new IllegalArgumentException("At least one package directory must be provided");
    }
    this.packagesDirectories = packagesDirectories;
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
    for (File packagesDirectory : packagesDirectories) {
      File resolvedFile = new File(packagesDirectory, path);
      if (resolvedFile.exists()) {
        return new FileBasedSource(factory, resolvedFile);
      }
    }
    return new FileBasedSource(factory, new File(packagesDirectories[0], path));
  }
}

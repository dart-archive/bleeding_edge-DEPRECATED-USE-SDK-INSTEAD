/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.source;

import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.NonExistingSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.UriKind;
import com.google.dart.engine.source.UriResolver;

import java.io.File;
import java.net.URI;
import java.util.Map;

/**
 * An {@link UriResolver} implementation for the {@code package:} scheme that uses a map of package
 * names to their directories.
 * 
 * @coverage dart.server.local
 */
public class PackageMapUriResolver extends UriResolver {
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
    return PACKAGE_SCHEME.equals(uri.getScheme());
  }

  /**
   * A table mapping package names to the path of the directory containing the package.
   */
  private final Map<String, File> packageMap;

  /**
   * Create a new {@link PackageMapUriResolver}.
   * 
   * @param packageMap a table mapping package names to the path of the directory containing the
   *          package
   */
  public PackageMapUriResolver(Map<String, File> packageMap) {
    this.packageMap = packageMap;
  }

  @Override
  public Source fromEncoding(UriKind kind, URI uri) {
    if (kind == UriKind.PACKAGE_URI) {
      return new FileBasedSource(new java.io.File(uri), kind);
    } else {
      return null;
    }
  }

  @Override
  public Source resolveAbsolute(URI uri) {
    if (!isPackageUri(uri)) {
      return null;
    }
    // Prepare path.
    String path = uri.getSchemeSpecificPart();
    // Prepare path components.
    String pkgName;
    String relPath;
    int index = path.indexOf('/');
    if (index == -1 || index == 0) {
      return null;
    } else {
      // <pkgName>/<relPath>
      pkgName = path.substring(0, index);
      relPath = path.substring(index + 1);
    }
    // Try to find an existing file.
    File packageDir = packageMap.get(pkgName);
    if (packageDir != null && packageDir.exists()) {
      File result = new File(packageDir, relPath.replace('/', File.separatorChar));
      if (result.exists()) {
        return new FileBasedSource(result, UriKind.PACKAGE_URI);
      }
    }
    // Return a NonExistingSource instance.
    // This helps provide more meaningful error messages to users
    // (a missing file error, as opposed to an invalid URI error).
    return new NonExistingSource(uri.toString(), UriKind.PACKAGE_URI);
  }
}

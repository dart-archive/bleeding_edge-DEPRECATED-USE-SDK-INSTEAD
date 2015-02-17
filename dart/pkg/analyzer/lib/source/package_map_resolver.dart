// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library source.package_map_resolver;

import 'package:analyzer/file_system/file_system.dart';
import 'package:analyzer/src/generated/source.dart';
import 'package:analyzer/src/util/asserts.dart' as asserts;


/**
 * A [UriResolver] implementation for the `package:` scheme that uses a map of
 * package names to their directories.
 */
class PackageMapUriResolver extends UriResolver {
  /**
   * The name of the `package` scheme.
   */
  static const String PACKAGE_SCHEME = "package";

  /**
   * A table mapping package names to the path of the directories containing
   * the package.
   */
  final Map<String, List<Folder>> packageMap;

  /**
   * The [ResourceProvider] for this resolver.
   */
  final ResourceProvider resourceProvider;

  /**
   * Create a new [PackageMapUriResolver].
   *
   * [packageMap] is a table mapping package names to the paths of the
   * directories containing the package
   */
  PackageMapUriResolver(this.resourceProvider, this.packageMap) {
    asserts.notNull(resourceProvider);
    asserts.notNull(packageMap);
  }

  @override
  Source resolveAbsolute(Uri uri) {
    if (!isPackageUri(uri)) {
      return null;
    }
    // Prepare path.
    String path = uri.path;
    // Prepare path components.
    int index = path.indexOf('/');
    if (index == -1 || index == 0) {
      return null;
    }
    // <pkgName>/<relPath>
    String pkgName = path.substring(0, index);
    String relPath = path.substring(index + 1);
    // Try to find an existing file.
    List<Folder> packageDirs = packageMap[pkgName];
    if (packageDirs != null) {
      for (Folder packageDir in packageDirs) {
        if (packageDir.exists) {
          Resource result = packageDir.getChild(relPath);
          if (result is File && result.exists) {
            return result.createSource(uri);
          }
        }
      }
    }
    // Return a NonExistingSource instance.
    // This helps provide more meaningful error messages to users
    // (a missing file error, as opposed to an invalid URI error).
    return new NonExistingSource(uri.toString(), UriKind.PACKAGE_URI);
  }

  @override
  Uri restoreAbsolute(Source source) {
    String sourcePath = source.fullName;
    for (String pkgName in packageMap.keys) {
      List<Folder> pkgFolders = packageMap[pkgName];
      for (Folder pkgFolder in pkgFolders) {
        String pkgFolderPath = pkgFolder.path;
        if (sourcePath.startsWith(pkgFolderPath)) {
          String relPath = sourcePath.substring(pkgFolderPath.length);
          return Uri.parse('$PACKAGE_SCHEME:$pkgName$relPath');
        }
      }
    }
    return null;
  }

  /**
   * Returns `true` if [uri] is a `package` URI.
   */
  static bool isPackageUri(Uri uri) {
    return uri.scheme == PACKAGE_SCHEME;
  }
}

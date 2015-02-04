// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub.package_graph;

import 'barback/transformer_cache.dart';
import 'entrypoint.dart';
import 'lock_file.dart';
import 'package.dart';
import 'source/cached.dart';
import 'utils.dart';

/// A holistic view of the entire transitive dependency graph for an entrypoint.
///
/// A package graph can be loaded using [Entrypoint.loadPackageGraph].
class PackageGraph {
  /// The entrypoint.
  final Entrypoint entrypoint;

  /// The entrypoint's lockfile.
  ///
  /// This describes the sources and resolved descriptions of everything in
  /// [packages].
  final LockFile lockFile;

  /// The transitive dependencies of the entrypoint (including itself).
  ///
  /// This may not include all transitive dependencies of the entrypoint if the
  /// creator of the package graph knows only a subset of the packages are
  /// relevant in the current context.
  final Map<String, Package> packages;

  /// A map of transitive dependencies for each package.
  Map<String, Set<Package>> _transitiveDependencies;

  /// The transformer cache, if it's been loaded.
  TransformerCache _transformerCache;

  PackageGraph(this.entrypoint, this.lockFile, this.packages);

  /// Loads the transformer cache for this graph.
  ///
  /// This may only be called if [entrypoint] represents a physical package.
  /// This may modify the cache.
  TransformerCache loadTransformerCache() {
    if (_transformerCache == null) {
      if (entrypoint.root.dir == null) {
        throw new StateError(
            "Can't load the transformer cache for virtual "
                "entrypoint ${entrypoint.root.name}.");
      }
      _transformerCache = new TransformerCache.load(this);
    }
    return _transformerCache;
  }

  /// Returns all transitive dependencies of [package].
  ///
  /// For the entrypoint this returns all packages in [packages], which includes
  /// dev and override. For any other package, it ignores dev and override
  /// dependencies.
  Set<Package> transitiveDependencies(String package) {
    if (package == entrypoint.root.name) return packages.values.toSet();

    if (_transitiveDependencies == null) {
      var closure = transitiveClosure(
          mapMap(
              packages,
              value: (_, package) => package.dependencies.map((dep) => dep.name)));
      _transitiveDependencies = mapMap(
          closure,
          value: (_, names) => names.map((name) => packages[name]).toSet());
    }

    return _transitiveDependencies[package];
  }

  /// Returns whether [package] is mutable.
  ///
  /// A package is considered to be mutable if it or any of its dependencies
  /// don't come from a cached source, since the user can change its contents
  /// without modifying the pub cache. Information generated from mutable
  /// packages is generally not safe to cache, since it may change frequently.
  bool isPackageMutable(String package) {
    var id = lockFile.packages[package];
    if (id == null) return true;

    var source = entrypoint.cache.sources[id.source];
    if (source is! CachedSource) return true;

    return transitiveDependencies(package).any((dep) {
      var depId = lockFile.packages[dep.name];

      // The entrypoint package doesn't have a lockfile entry. It's always
      // mutable.
      if (depId == null) return true;

      return entrypoint.cache.sources[depId.source] is! CachedSource;
    });
  }

  /// Returns whether [package] is static.
  ///
  /// A package is considered to be static if it's not transformed and it came
  /// from a cached source. Static packages don't need to be fully processed by
  /// barback.
  ///
  /// Note that a static package isn't the same as an immutable package (see
  /// [isPackageMutable]).
  bool isPackageStatic(String package) {
    var id = lockFile.packages[package];
    if (id == null) return false;

    var source = entrypoint.cache.sources[id.source];
    if (source is! CachedSource) return false;

    return packages[package].pubspec.transformers.isEmpty;
  }
}

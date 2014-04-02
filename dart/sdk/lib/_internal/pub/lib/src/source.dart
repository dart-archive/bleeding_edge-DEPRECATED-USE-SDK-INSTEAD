// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub.source;

import 'dart:async';

import 'package:path/path.dart' as path;
import 'package:stack_trace/stack_trace.dart';

import 'io.dart';
import 'package.dart';
import 'pubspec.dart';
import 'system_cache.dart';
import 'utils.dart';
import 'version.dart';

/// A source from which to get packages.
///
/// Each source has many packages that it looks up using [PackageId]s. The
/// source is responsible for getting these packages into the package cache.
abstract class Source {
  /// The name of the source. Should be lower-case, suitable for use in a
  /// filename, and unique accross all sources.
  String get name;

  /// Whether or not this source is the default source.
  bool get isDefault => systemCache.sources.defaultSource == this;

  /// Whether this source's packages should be cached in Pub's global cache
  /// directory.
  ///
  /// A source should be cached if it requires network access to retrieve
  /// packages. It doesn't need to be cached if all packages are available
  /// locally.
  bool get shouldCache;

  /// The system cache with which this source is registered.
  SystemCache get systemCache {
    assert(_systemCache != null);
    return _systemCache;
  }

  /// The system cache variable. Set by [_bind].
  SystemCache _systemCache;

  /// The root directory of this source's cache within the system cache.
  ///
  /// This shouldn't be overridden by subclasses.
  String get systemCacheRoot => path.join(systemCache.rootDir, name);

  /// Records the system cache to which this source belongs.
  ///
  /// This should only be called once for each source, by
  /// [SystemCache.register]. It should not be overridden by base classes.
  void bind(SystemCache systemCache) {
    assert(_systemCache == null);
    this._systemCache = systemCache;
  }

  /// Get the list of all versions that exist for the package described by
  /// [description]. [name] is the expected name of the package.
  ///
  /// Note that this does *not* require the package to be downloaded locally,
  /// which is the point. This is used during version resolution to determine
  /// which package versions are available to be downloaded (or already
  /// downloaded).
  ///
  /// By default, this assumes that each description has a single version and
  /// uses [describe] to get that version.
  Future<List<Version>> getVersions(String name, description) {
    var id = new PackageId(name, this.name, Version.none, description);
    return describeUncached(id).then((pubspec) => [pubspec.version]);
  }

  /// Loads the (possibly remote) pubspec for the package version identified by
  /// [id]. This may be called for packages that have not yet been downloaded
  /// during the version resolution process.
  ///
  /// If the package has been downloaded to the system cache, the cached pubspec
  /// will be used. Otherwise, it delegates to host-specific lookup behavior.
  ///
  /// For cached sources, by default this uses [downloadToSystemCache] to get
  /// the pubspec. There is no default implementation for non-cached sources;
  /// they must implement it manually.
  Future<Pubspec> describe(PackageId id) {
    if (id.isRoot) throw new ArgumentError("Cannot describe the root package.");
    if (id.source != name) {
      throw new ArgumentError("Package $id does not use source $name.");
    }

    // Try to get it from the system cache first.
    if (shouldCache) {
      return systemCacheDirectory(id).then((packageDir) {
        if (!fileExists(path.join(packageDir, "pubspec.yaml"))) {
          return describeUncached(id);
        }

        return new Pubspec.load(packageDir, _systemCache.sources,
            expectedName: id.name);
      });
    }

    // Not cached, so get it from the source.
    return describeUncached(id);
  }

  /// Loads the pubspec for the package version identified by [id] which is not
  /// already in the system cache.
  ///
  /// For cached sources, by default this uses [downloadToSystemCache] to get
  /// the pubspec. There is no default implementation for non-cached sources;
  /// they must implement it manually.
  ///
  /// This method is effectively protected. Derived classes may override it,
  /// but external code should not call it. Call [describe()] instead.
  Future<Pubspec> describeUncached(PackageId id) {
    if (!shouldCache) {
      throw new UnimplementedError(
          "Source $name must implement describeUncached(id).");
    }
    return downloadToSystemCache(id).then((package) => package.pubspec);
  }

  /// Gets the package identified by [id] and places it at [path].
  ///
  /// Returns a [Future] that completes when the operation finishes. The
  /// [Future] should resolve to true if the package was found in the source
  /// and false if it wasn't. For all other error conditions, it should complete
  /// with an exception.
  ///
  /// [path] is guaranteed not to exist, and its parent directory is guaranteed
  /// to exist.
  ///
  /// Note that [path] may be deleted. If re-getting a package that has already
  /// been gotten would be costly or impossible, [downloadToSystemCache]
  /// should be implemented instead of [get].
  ///
  /// This doesn't need to be implemented if [downloadToSystemCache] is
  /// implemented.
  Future<bool> get(PackageId id, String path) {
    throw new UnimplementedError("Either get() or downloadToSystemCache() must "
        "be implemented for source $name.");
  }

  /// Determines if the package with [id] is already downloaded to the system
  /// cache.
  ///
  /// This should only be called for sources with [shouldCache] set to true.
  /// Completes to true if the package is in the cache and appears to be
  /// uncorrupted.
  Future<bool> isInSystemCache(PackageId id) {
    return systemCacheDirectory(id).then((packageDir) {
      return dirExists(packageDir) && !_isCachedPackageCorrupted(packageDir);
    });
  }

  /// Downloads the package identified by [id] to the system cache.
  ///
  /// This is only called for sources with [shouldCache] set to true. By
  /// default, this uses [systemCacheDirectory] and [get].
  Future<Package> downloadToSystemCache(PackageId id) {
    var packageDir;
    return systemCacheDirectory(id).then((p) {
      packageDir = p;

      // See if it's already cached.
      if (dirExists(packageDir)) {
        if (!_isCachedPackageCorrupted(packageDir)) return true;
        // Busted, so wipe out the package and re-download.
        deleteEntry(packageDir);
      }

      ensureDir(path.dirname(packageDir));
      return get(id, packageDir);
    }).then((found) {
      if (!found) fail('Package $id not found.');
      return new Package.load(id.name, packageDir, systemCache.sources);
    });
  }

  /// Since pub generates symlinks that point into the system cache (in
  /// particular, targeting the "lib" directories of cached packages), it's
  /// possible to accidentally break cached packages if something traverses
  /// that symlink.
  ///
  /// This tries to determine if the cached package at [packageDir] has been
  /// corrupted. The heuristics are it is corrupted if any of the following are
  /// true:
  ///
  ///   * It has an empty "lib" directory.
  ///   * It has no pubspec.
  bool _isCachedPackageCorrupted(String packageDir) {
    if (!fileExists(path.join(packageDir, "pubspec.yaml"))) return true;

    var libDir = path.join(packageDir, "lib");
    if (dirExists(libDir)) return listDir(libDir).length == 0;

    // If we got here, it's OK.
    return false;
  }

  /// Returns the directory where this package can be found locally. If this is
  /// a cached source, it will be in the system cache. Otherwise, it will
  /// depend on the source.
  Future<String> getDirectory(PackageId id) {
    if (shouldCache) return systemCacheDirectory(id);
    throw new UnimplementedError("Source $name must implement this.");
  }

  /// Returns the directory in the system cache that the package identified by
  /// [id] should be downloaded to. This should return a path to a subdirectory
  /// of [systemCacheRoot].
  ///
  /// This doesn't need to be implemented if [shouldCache] is false.
  Future<String> systemCacheDirectory(PackageId id) {
    return new Future.error(
        "systemCacheDirectory() must be implemented if shouldCache is true.",
        new Chain.current());
  }

  /// When a [Pubspec] or [LockFile] is parsed, it reads in the description for
  /// each dependency. It is up to the dependency's [Source] to determine how
  /// that should be interpreted. This will be called during parsing to validate
  /// that the given [description] is well-formed according to this source, and
  /// to give the source a chance to canonicalize the description.
  ///
  /// [containingPath] is the path to the local file (pubspec or lockfile)
  /// where this description appears. It may be `null` if the description is
  /// coming from some in-memory source (such as pulling down a pubspec from
  /// pub.dartlang.org).
  ///
  /// It should return if a (possibly modified) valid description, or throw a
  /// [FormatException] if not valid.
  ///
  /// [fromLockFile] is true when the description comes from a [LockFile], to
  /// allow the source to use lockfile-specific descriptions via [resolveId].
  dynamic parseDescription(String containingPath, description,
                           {bool fromLockFile: false}) {
    return description;
  }

  /// When a [LockFile] is serialized, it uses this method to get the
  /// [description] in the right format.
  ///
  /// [containingPath] is the containing directory of the root package.
  dynamic serializeDescription(String containingPath, description) {
    return description;
  }

  /// When a package [description] is shown to the user, this is called to
  /// convert it into a human-friendly form.
  ///
  /// By default, it just converts the description to a string, but sources
  /// may customize this. [containingPath] is the containing directory of the
  /// root package.
  String formatDescription(String containingPath, description) {
    return description.toString();
  }

  /// Returns whether or not [description1] describes the same package as
  /// [description2] for this source. This method should be light-weight. It
  /// doesn't need to validate that either package exists.
  ///
  /// By default, just uses regular equality.
  bool descriptionsEqual(description1, description2) =>
    description1 == description2;

  /// For some sources, [PackageId]s can point to different chunks of code at
  /// different times. This takes such an [id] and returns a future that
  /// completes to a [PackageId] that will uniquely specify a single chunk of
  /// code forever.
  ///
  /// For example, [GitSource] might take an [id] with description
  /// `http://github.com/dart-lang/some-lib.git` and return an id with a
  /// description that includes the current commit of the Git repository.
  ///
  /// Pub calls this after getting a package, so the source can use the local
  /// package to determine information about the resolved id.
  ///
  /// The returned [PackageId] may have a description field that's invalid
  /// according to [parseDescription], although it must still be serializable
  /// to JSON and YAML. It must also be equal to [id] according to
  /// [descriptionsEqual].
  ///
  /// By default, this just returns [id].
  Future<PackageId> resolveId(PackageId id) => new Future.value(id);

  /// Returns the [Package]s that have been downloaded to the system cache.
  List<Package> getCachedPackages() {
    if (shouldCache) {
      throw new UnimplementedError("Source $name must implement this.");
    }
    throw new UnsupportedError("Cannot call getCachedPackages() on an "
        "uncached source.");
  }

  /// Returns the source's name.
  String toString() => name;
}

// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/// A back-tracking depth-first solver. Attempts to find the best solution for
/// a root package's transitive dependency graph, where a "solution" is a set
/// of concrete package versions. A valid solution will select concrete
/// versions for every package reached from the root package's dependency graph,
/// and each of those packages will fit the version constraints placed on it.
///
/// The solver builds up a solution incrementally by traversing the dependency
/// graph starting at the root package. When it reaches a new package, it gets
/// the set of versions that meet the current constraint placed on it. It
/// *speculatively* selects one version from that set and adds it to the
/// current solution and then proceeds. If it fully traverses the dependency
/// graph, the solution is valid and it stops.
///
/// If it reaches an error because:
///
/// - A new dependency is placed on a package that's already been selected in
///   the solution and the selected version doesn't match the new constraint.
///
/// - There are no versions available that meet the constraint placed on a
///   package.
///
/// - etc.
///
/// then the current solution is invalid. It will then backtrack to the most
/// recent speculative version choice and try the next one. That becomes the
/// new in-progress solution and it tries to proceed from there. It will keep
/// doing this, traversing and then backtracking when it meets a failure until
/// a valid solution has been found or until all possible options for all
/// speculative choices have been exhausted.
library pub.solver.backtracking_solver;

import 'dart:async';
import 'dart:collection' show Queue;

import '../barback.dart' as barback;
import '../lock_file.dart';
import '../log.dart' as log;
import '../package.dart';
import '../pubspec.dart';
import '../sdk.dart' as sdk;
import '../source_registry.dart';
import '../utils.dart';
import '../version.dart';
import 'dependency_queue.dart';
import 'version_queue.dart';
import 'version_solver.dart';

/// The top-level solver. Keeps track of the current potential solution, and
/// the other possible versions for speculative package selections. Backtracks
/// and advances to the next potential solution in the case of a failure.
class BacktrackingSolver {
  final SourceRegistry sources;
  final Package root;

  /// The lockfile that was present before solving.
  final LockFile lockFile;

  final PubspecCache cache;

  /// The set of packages that are being explicitly upgraded. The solver will
  /// only allow the very latest version for each of these packages.
  final _forceLatest = new Set<String>();

  /// If this is set, the contents of [lockFile] are ignored while solving.
  final bool _upgradeAll;

  /// The set of packages whose dependecy is being overridden by the root
  /// package, keyed by the name of the package.
  ///
  /// Any dependency on a package that appears in this map will be overriden
  /// to use the one here.
  final _overrides = new Map<String, PackageDep>();

  /// Every time a package is encountered when traversing the dependency graph,
  /// the solver must select a version for it, sometimes when multiple versions
  /// are valid. This keeps track of which versions have been selected so far
  /// and which remain to be tried.
  ///
  /// Each entry in the list is a [VersionQueue], which is an ordered queue of
  /// versions to try for a single package. It maintains the currently selected
  /// version for that package. When a new dependency is encountered, a queue
  /// of versions of that dependency is pushed onto the end of the list. A
  /// queue is removed from the list once it's empty, indicating that none of
  /// the versions provided a solution.
  ///
  /// The solver tries versions in depth-first order, so only the last queue in
  /// the list will have items removed from it. When a new constraint is placed
  /// on an already-selected package, and that constraint doesn't match the
  /// selected version, that will cause the current solution to fail and
  /// trigger backtracking.
  final _selected = <VersionQueue>[];

  /// The number of solutions the solver has tried so far.
  int get attemptedSolutions => _attemptedSolutions;
  var _attemptedSolutions = 1;

  BacktrackingSolver(SourceRegistry sources, this.root, this.lockFile,
                     List<String> useLatest, {bool upgradeAll: false})
      : sources = sources,
        cache = new PubspecCache(sources),
        _upgradeAll = upgradeAll {
    for (var package in useLatest) {
      _forceLatest.add(package);
    }

    for (var override in root.dependencyOverrides) {
      _overrides[override.name] = override;
    }
  }

  /// Run the solver. Completes with a list of specific package versions if
  /// successful or an error if it failed to find a solution.
  Future<SolveResult> solve() {
    var stopwatch = new Stopwatch();

    _logParameters();

    // Sort the overrides by package name to make sure they're deterministic.
    var overrides = _overrides.values.toList();
    overrides.sort((a, b) => a.name.compareTo(b.name));

    return newFuture(() {
      stopwatch.start();

      // Pre-cache the root package's known pubspec.
      cache.cache(new PackageId.root(root), root.pubspec);

      _validateSdkConstraint(root.pubspec);
      return _traverseSolution();
    }).then((packages) {
      return new SolveResult.success(sources, root, lockFile, packages,
          overrides, _getAvailableVersions(packages), attemptedSolutions);
    }).catchError((error) {
      if (error is! SolveFailure) throw error;

      // Wrap a failure in a result so we can attach some other data.
      return new SolveResult.failure(sources, root, lockFile, overrides,
          error, attemptedSolutions);
    }).whenComplete(() {
      // Gather some solving metrics.
      var buffer = new StringBuffer();
      buffer.writeln('${runtimeType} took ${stopwatch.elapsed} seconds.');
      buffer.writeln(
          '- Requested ${cache.versionCacheMisses} version lists');
      buffer.writeln(
          '- Looked up ${cache.versionCacheHits} cached version lists');
      buffer.writeln(
          '- Requested ${cache.pubspecCacheMisses} pubspecs');
      buffer.writeln(
          '- Looked up ${cache.pubspecCacheHits} cached pubspecs');
      log.solver(buffer);
    });
  }

  /// Generates a map containing all of the known available versions for each
  /// package in [packages].
  ///
  /// The version list may not always be complete. The the package is the root
  /// root package, or its a package that we didn't unlock while solving
  /// because we weren't trying to upgrade it, we will just know the current
  /// version.
  Map<String, List<Version>> _getAvailableVersions(List<PackageId> packages) {
    var availableVersions = new Map<String, List<Version>>();
    for (var package in packages) {
      var cached = cache.getCachedVersions(package.toRef());
      var versions;
      if (cached != null) {
        versions = cached.map((id) => id.version).toList();
      } else {
        // If the version list was never requested, just use the one known
        // version.
        versions = [package.version];
      }

      availableVersions[package.name] = versions;
    }

    return availableVersions;
  }

  /// Adds [versions], which is the list of all allowed versions of a given
  /// package, to the set of versions to consider for solutions. The first item
  /// in the list will be the currently selected version of that package.
  /// Subsequent items will be tried if it the current selection fails. Returns
  /// the first selected version.
  PackageId select(VersionQueue versions) {
    _selected.add(versions);
    logSolve();
    return versions.current;
  }

  /// Returns the the currently selected id for the package [name] or `null` if
  /// no concrete version has been selected for that package yet.
  PackageId getSelected(String name) {
    // Always prefer the root package.
    if (root.name == name) return new PackageId.root(root);

    // Look through the current selections.
    for (var i = _selected.length - 1; i >= 0; i--) {
      if (_selected[i].current.name == name) return _selected[i].current;
    }

    return null;
  }

  /// Gets the version of [package] currently locked in the lock file. Returns
  /// `null` if it isn't in the lockfile (or has been unlocked).
  PackageId getLocked(String package) {
    if (_upgradeAll) return null;
    if (_forceLatest.contains(package)) return null;

    return lockFile.packages[package];
  }

  /// Traverses the root package's dependency graph using the current potential
  /// solution. If successful, completes to the solution. If not, backtracks
  /// to the most recently selected version of a package and tries the next
  /// version of it. If there are no more versions, continues to backtrack to
  /// previous selections, and so on. If there is nothing left to backtrack to,
  /// completes to the last failure that occurred.
  Future<List<PackageId>> _traverseSolution() => resetStack(() {
    return new Traverser(this).traverse().catchError((error) {
      if (error is! SolveFailure) throw error;

      return _backtrack(error).then((canTry) {
        if (canTry) {
          _attemptedSolutions++;
          return _traverseSolution();
        }

        // All out of solutions, so fail.
        throw error;
      });
    });
  });

  /// Backtracks from the current failed solution and determines the next
  /// solution to try. If possible, it will backjump based on the cause of the
  /// [failure] to minize backtracking. Otherwise, it will simply backtrack to
  /// the next possible solution.
  ///
  /// Returns `true` if there is a new solution to try.
  Future<bool> _backtrack(SolveFailure failure) {
    // Bail if there is nothing to backtrack to.
    if (_selected.isEmpty) return new Future.value(false);

    // Get the set of packages that may have led to this failure.
    var dependers = _getTransitiveDependers(failure.package);

    // Advance past the current version of the leaf-most package.
    advanceVersion() {
      _backjump(failure, dependers);
      var previous = _selected.last.current;
      return _selected.last.advance().then((success) {
        if (success) {
          logSolve();
          return true;
        }

        logSolve('$previous is last version, backtracking');

        // That package has no more versions, so pop it and try the next one.
        _selected.removeLast();
        if (_selected.isEmpty) return false;

        // If we got here, the leafmost package was discarded so we need to
        // advance the next one.
        return advanceVersion();
      });
    }

    return advanceVersion();
  }

  /// Walks the selected packages from most to least recent to determine which
  /// ones can be ignored and jumped over by the backtracker. The only packages
  /// we need to backtrack to are ones that led (possibly indirectly) to the
  /// failure. Everything else can be skipped.
  void _backjump(SolveFailure failure, Set<String> dependers) {
    for (var i = _selected.length - 1; i >= 0; i--) {
      // Each queue will never be empty since it gets discarded by _backtrack()
      // when that happens.
      var selected = _selected[i].current;

      // If the failure is a disjoint version range, then no possible versions
      // for that package can match and there's no reason to try them. Instead,
      // just backjump past it.
      if (failure is DisjointConstraintException &&
          selected.name == failure.package) {
        logSolve("skipping past disjoint selected ${selected.name}");
        continue;
      }

      // If we get to the package that failed, backtrack to here.
      if (selected.name == failure.package) {
        logSolve('backjump to failed package ${selected.name}');
        _selected.removeRange(i + 1, _selected.length);
        return;
      }

      // If we get to a package that depends on the failing package, backtrack
      // to here.
      if (dependers.contains(selected.name)) {
        logSolve('backjump to ${selected.name} because it depends on '
                 '${failure.package}');
        _selected.removeRange(i + 1, _selected.length);
        return;
      }
    }

    // If we got here, we walked the entire list without finding a package that
    // could lead to another solution, so discard everything. This will happen
    // if every package that led to the failure has no other versions that it
    // can try to select.
    _selected.removeRange(1, _selected.length);
  }

  /// Gets the set of currently selected packages that depend on [dependency]
  /// either directly or indirectly.
  ///
  /// When backtracking, it's only useful to consider changing the version of
  /// packages who have a dependency on the failed package that triggered
  /// backtracking. This is used to determine those packages.
  ///
  /// We calculate the full set up front before backtracking because during
  /// backtracking, we will unselect packages and start to lose this
  /// information in the middle of the process.
  ///
  /// For example, consider dependencies A -> B -> C. We've selected A and B
  /// then encounter a problem with C. We start backtracking. B has no more
  /// versions so we discard it and keep backtracking to A. When we get there,
  /// since we've unselected B, we no longer realize that A had a transitive
  /// dependency on C. We would end up backjumping over A and failing.
  ///
  /// Calculating the dependency set up front before we start backtracking
  /// solves that.
  Set<String> _getTransitiveDependers(String dependency) {
    // Generate a reverse dependency graph. For each package, create edges to
    // each package that depends on it.
    var dependers = new Map<String, Set<String>>();

    addDependencies(name, deps) {
      dependers.putIfAbsent(name, () => new Set<String>());
      for (var dep in deps) {
        dependers.putIfAbsent(dep.name, () => new Set<String>()).add(name);
      }
    }

    for (var i = 0; i < _selected.length; i++) {
      var id = _selected[i].current;
      var pubspec = cache.getCachedPubspec(id);
      if (pubspec != null) addDependencies(id.name, pubspec.dependencies);
    }

    // Include the root package's dependencies.
    addDependencies(root.name, root.immediateDependencies);

    // Now walk the depending graph to see which packages transitively depend
    // on [dependency].
    var visited = new Set<String>();
    walk(String package) {
      // Don't get stuck in cycles.
      if (visited.contains(package)) return;
      visited.add(package);
      var depender = dependers[package].forEach(walk);
    }

    walk(dependency);
    return visited;
  }

  /// Logs the initial parameters to the solver.
  void _logParameters() {
    var buffer = new StringBuffer();
    buffer.writeln("Solving dependencies:");
    for (var package in root.dependencies) {
      buffer.write("- $package");
      var locked = getLocked(package.name);
      if (_forceLatest.contains(package.name)) {
        buffer.write(" (use latest)");
      } else if (locked != null) {
        var version = locked.version;
        buffer.write(" (locked to $version)");
      }
      buffer.writeln();
    }
    log.solver(buffer.toString().trim());
  }

  /// Logs [message] in the context of the current selected packages. If
  /// [message] is omitted, just logs a description of leaf-most selection.
  void logSolve([String message]) {
    if (message == null) {
      if (_selected.isEmpty) {
        message = "* start at root";
      } else {
        message = "* select ${_selected.last.current}";
      }
    } else {
      // Otherwise, indent it under the current selected package.
      message = prefixLines(message);
    }

    // Indent for the previous selections.
    var prefix = _selected.skip(1).map((_) => '| ').join();
    log.solver(prefixLines(message, prefix: prefix));
  }
}

/// Given the solver's current set of selected package versions, this tries to
/// traverse the dependency graph and see if a complete set of valid versions
/// has been chosen. If it reaches a conflict, it will fail and stop
/// traversing. If it reaches a package that isn't selected it will refine the
/// solution by adding that package's set of allowed versions to the solver and
/// then select the best one and continue.
class Traverser {
  final BacktrackingSolver _solver;

  /// The queue of packages left to traverse. We do a breadth-first traversal
  /// using an explicit queue just to avoid the code complexity of a recursive
  /// asynchronous traversal.
  final _packages = new Queue<PackageId>();

  /// The packages we have already traversed. Used to avoid traversing the same
  /// package multiple times, and to build the complete solution results.
  final _visited = new Set<PackageId>();

  /// The dependencies visited so far in the traversal. For each package name
  /// (the map key) we track the list of dependencies that other packages have
  /// placed on it so that we can calculate the complete constraint for shared
  /// dependencies.
  final _dependencies = <String, List<Dependency>>{};

  Traverser(this._solver);

  /// Walks the dependency graph starting at the root package and validates
  /// that each reached package has a valid version selected.
  Future<List<PackageId>> traverse() {
    // Start at the root.
    _packages.add(new PackageId.root(_solver.root));
    return _traversePackage();
  }

  /// Traverses the next package in the queue. Completes to a list of package
  /// IDs if the traversal completed successfully and found a solution.
  /// Completes to an error if the traversal failed. Otherwise, recurses to the
  /// next package in the queue, etc.
  Future<List<PackageId>> _traversePackage() {
    if (_packages.isEmpty) {
      // We traversed the whole graph. If we got here, we successfully found
      // a solution.
      return new Future<List<PackageId>>.value(_visited.toList());
    }

    var id = _packages.removeFirst();

    // Don't visit the same package twice.
    if (_visited.contains(id)) {
      return _traversePackage();
    }
    _visited.add(id);

    return _solver.cache.getPubspec(id).then((pubspec) {
      _validateSdkConstraint(pubspec);

      var deps = pubspec.dependencies.toSet();

      if (id.isRoot) {
        // Include dev dependencies of the root package.
        deps.addAll(pubspec.devDependencies);

        // Add all overrides. This ensures a dependency only present as an
        // override is still included.
        deps.addAll(_solver._overrides.values);
      }

      // Replace any overridden dependencies.
      deps = deps.map((dep) {
        var override = _solver._overrides[dep.name];
        if (override != null) return override;

        // Not overridden.
        return dep;
      });

      // Make sure the package doesn't have any bad dependencies.
      for (var dep in deps) {
        if (!dep.isRoot && !_solver.sources.contains(dep.source)) {
          throw new UnknownSourceException(id.name,
              [new Dependency(id.name, dep)]);
        }
      }

      return _traverseDeps(id.name, new DependencyQueue(_solver, deps));
    });
  }

  /// Traverses the references that [depender] depends on, stored in [deps].
  ///
  /// Desctructively modifies [deps]. Completes to a list of packages if the
  /// traversal is complete. Completes it to an error if a failure occurred.
  /// Otherwise, recurses.
  Future<List<PackageId>> _traverseDeps(String depender, DependencyQueue deps) {
    // Move onto the next package if we've traversed all of these references.
    if (deps.isEmpty) return _traversePackage();

    return resetStack(() {
      return deps.advance().then((dep) {
        _validateDependency(dep, depender);

        // Add the dependency.
        var dependencies = _getDependencies(dep.name);
        dependencies.add(new Dependency(depender, dep));

        // If the package is barback, pub has an implicit version constraint on
        // it since pub itself uses barback too. Note that we don't check for
        // the hosted source here because we still want to do this even when
        // people on the Dart team are on the bleeding edge and have a path
        // dependency on the tip version of barback in the Dart repo.
        //
        // The length check here is to ensure we only add the barback
        // dependency once.
        if (dep.name == "barback" && dependencies.length == 1) {
          _solver.logSolve('add implicit ${barback.supportedVersions} pub '
              'dependency on barback');

          // Use the same source and description as the explicit dependency.
          // That way, this doesn't fail with a source/desc conflict if users
          // (like Dart team members) use things like a path dependency to
          // find barback.
          var barbackDep = new PackageDep(dep.name, dep.source,
              barback.supportedVersions, dep.description);
          dependencies.add(new Dependency("pub itself", barbackDep));
        }

        var constraint = _getConstraint(dep.name);

        // See if it's possible for a package to match that constraint.
        if (constraint.isEmpty) {
          var constraints = _getDependencies(dep.name)
              .map((dep) => "  ${dep.dep.constraint} from ${dep.depender}")
              .join('\n');
          _solver.logSolve(
              'disjoint constraints on ${dep.name}:\n$constraints');
          throw new DisjointConstraintException(dep.name, dependencies);
        }

        var selected = _validateSelected(dep, constraint);
        if (selected != null) {
          // The selected package version is good, so enqueue it to traverse
          // into it.
          _packages.add(selected);
          return _traverseDeps(depender, deps);
        }

        // We haven't selected a version. Try all of the versions that match
        // the constraints we currently have for this package.
        var locked = _getValidLocked(dep.name);

        return VersionQueue.create(locked,
            () => _getAllowedVersions(dep)).then((versions) {
          _packages.add(_solver.select(versions));
        });
      }).then((_) => _traverseDeps(depender, deps));
    });
  }

  /// Gets all versions of [dep] that match the current constraints placed on
  /// it.
  Future<Iterable<PackageId>> _getAllowedVersions(PackageDep dep) {
    var constraint = _getConstraint(dep.name);
    return _solver.cache.getVersions(dep.toRef()).then((versions) {
      var allowed = versions.where((id) => constraint.allows(id.version));

      if (allowed.isEmpty) {
        _solver.logSolve('no versions for ${dep.name} match $constraint');
        throw new NoVersionException(dep.name, constraint,
            _getDependencies(dep.name));
      }

      // If we're doing an upgrade on this package, only allow the latest
      // version.
      if (_solver._forceLatest.contains(dep.name)) allowed = [allowed.first];

      // Remove the locked version, if any, since that was already handled.
      var locked = _getValidLocked(dep.name);
      if (locked != null) {
        allowed = allowed.where((dep) => dep.version != locked.version);
      }

      return allowed;
    }).catchError((error, stackTrace) {
      if (error is PackageNotFoundException) {
        // Show the user why the package was being requested.
        throw new DependencyNotFoundException(
            dep.name, error, _getDependencies(dep.name));
      }

      throw error;
    });
  }

  /// Ensures that dependency [dep] from [depender] is consistent with the
  /// other dependencies on the same package. Throws a [SolveFailure]
  /// exception if not. Only validates sources and descriptions, not the
  /// version.
  void _validateDependency(PackageDep dep, String depender) {
    // Make sure the dependencies agree on source and description.
    var required = _getRequired(dep.name);
    if (required == null) return;

    // Make sure all of the existing sources match the new reference.
    if (required.dep.source != dep.source) {
      _solver.logSolve('source mismatch on ${dep.name}: ${required.dep.source} '
                       '!= ${dep.source}');
      throw new SourceMismatchException(dep.name,
          [required, new Dependency(depender, dep)]);
    }

    // Make sure all of the existing descriptions match the new reference.
    var source = _solver.sources[dep.source];
    if (!source.descriptionsEqual(dep.description, required.dep.description)) {
      _solver.logSolve('description mismatch on ${dep.name}: '
                       '${required.dep.description} != ${dep.description}');
      throw new DescriptionMismatchException(dep.name,
          [required, new Dependency(depender, dep)]);
    }
  }

  /// Validates the currently selected package against the new dependency that
  /// [dep] and [constraint] place on it. Returns `null` if there is no
  /// currently selected package, throws a [SolveFailure] if the new reference
  /// it not does not allow the previously selected version, or returns the
  /// selected package if successful.
  PackageId _validateSelected(PackageDep dep, VersionConstraint constraint) {
    var selected = _solver.getSelected(dep.name);
    if (selected == null) return null;

    // Make sure it meets the constraint.
    if (!dep.constraint.allows(selected.version)) {
      _solver.logSolve('selection $selected does not match $constraint');
      throw new NoVersionException(dep.name, constraint,
                                   _getDependencies(dep.name));
    }

    return selected;
  }

  /// Gets the list of dependencies for package [name]. Will create an empty
  /// list if needed.
  List<Dependency> _getDependencies(String name) {
    return _dependencies.putIfAbsent(name, () => <Dependency>[]);
  }

  /// Gets a "required" reference to the package [name]. This is the first
  /// non-root dependency on that package. All dependencies on a package must
  /// agree on source and description, except for references to the root
  /// package. This will return a reference to that "canonical" source and
  /// description, or `null` if there is no required reference yet.
  ///
  /// This is required because you may have a circular dependency back onto the
  /// root package. That second dependency won't be a root dependency and it's
  /// *that* one that other dependencies need to agree on. In other words, you
  /// can have a bunch of dependencies back onto the root package as long as
  /// they all agree with each other.
  Dependency _getRequired(String name) {
    return _getDependencies(name)
        .firstWhere((dep) => !dep.dep.isRoot, orElse: () => null);
  }

  /// Gets the combined [VersionConstraint] currently being placed on package
  /// [name].
  VersionConstraint _getConstraint(String name) {
    var constraint = _getDependencies(name)
        .map((dep) => dep.dep.constraint)
        .fold(VersionConstraint.any, (a, b) => a.intersect(b));

    return constraint;
  }

  /// Gets the package [name] that's currently contained in the lockfile if it
  /// meets [constraint] and has the same source and description as other
  /// references to that package. Returns `null` otherwise.
  PackageId _getValidLocked(String name) {
    var package = _solver.getLocked(name);
    if (package == null) return null;

    var constraint = _getConstraint(name);
    if (!constraint.allows(package.version)) {
      _solver.logSolve('$package is locked but does not match $constraint');
      return null;
    } else {
      _solver.logSolve('$package is locked');
    }

    var required = _getRequired(name);
    if (required != null) {
      if (package.source != required.dep.source) return null;

      var source = _solver.sources[package.source];
      if (!source.descriptionsEqual(
          package.description, required.dep.description)) return null;
    }

    return package;
  }
}

/// Ensures that if [pubspec] has an SDK constraint, then it is compatible
/// with the current SDK. Throws a [SolveFailure] if not.
void _validateSdkConstraint(Pubspec pubspec) {
  if (pubspec.environment.sdkVersion.allows(sdk.version)) return;

  throw new BadSdkVersionException(pubspec.name,
      'Package ${pubspec.name} requires SDK version '
      '${pubspec.environment.sdkVersion} but the current SDK is '
      '${sdk.version}.');
}

// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library analyzer_impl;

import 'dart:async';
import 'dart:collection';
import 'dart:io';

import 'package:analyzer/file_system/file_system.dart' show Folder;
import 'package:analyzer/file_system/physical_file_system.dart';
import 'package:analyzer/source/package_map_provider.dart';
import 'package:analyzer/source/package_map_resolver.dart';
import 'package:analyzer/source/pub_package_map_provider.dart';
import 'package:analyzer/src/error_formatter.dart';
import 'package:analyzer/src/generated/java_core.dart' show JavaSystem;
import 'package:analyzer/src/generated/java_engine.dart';

import '../options.dart';
import 'generated/constant.dart';
import 'generated/element.dart';
import 'generated/engine.dart';
import 'generated/error.dart';
import 'generated/java_io.dart';
import 'generated/sdk_io.dart';
import 'generated/source_io.dart';
import 'package:analyzer/src/generated/utilities_general.dart';

DirectoryBasedDartSdk sdk;

/**
 * The maximum number of sources for which AST structures should be kept in the cache.
 */
const int _MAX_CACHE_SIZE = 512;

/// Analyzes single library [File].
class AnalyzerImpl {
  final String sourcePath;

  final CommandLineOptions options;
  final int startTime;
  ContentCache contentCache = new ContentCache();

  SourceFactory sourceFactory;
  AnalysisContext context;
  Source librarySource;
  /// All [Source]s references by the analyzed library.
  final Set<Source> sources = new Set<Source>();

  /// All [AnalysisErrorInfo]s in the analyzed library.
  final List<AnalysisErrorInfo> errorInfos = new List<AnalysisErrorInfo>();

  /// [HashMap] between sources and analysis error infos.
  final HashMap<Source, AnalysisErrorInfo> sourceErrorsMap =
      new HashMap<Source, AnalysisErrorInfo>();

  AnalyzerImpl(String sourcePath, this.options, this.startTime)
      : sourcePath = _normalizeSourcePath(sourcePath) {
    if (sdk == null) {
      sdk = new DirectoryBasedDartSdk(new JavaFile(options.dartSdkPath));
    }
  }

  /// Returns the maximal [ErrorSeverity] of the recorded errors.
  ErrorSeverity get maxErrorSeverity {
    var status = ErrorSeverity.NONE;
    for (AnalysisErrorInfo errorInfo in errorInfos) {
      for (AnalysisError error in errorInfo.errors) {
        if (!_isDesiredError(error)) {
          continue;
        }
        var severity = computeSeverity(error, options.enableTypeChecks);
        status = status.max(severity);
      }
    }
    return status;
  }

  void addCompilationUnitSource(CompilationUnitElement unit,
      Set<LibraryElement> libraries, Set<CompilationUnitElement> units) {
    if (unit == null || units.contains(unit)) {
      return;
    }
    units.add(unit);
    sources.add(unit.source);
  }

  void addLibrarySources(LibraryElement library, Set<LibraryElement> libraries,
      Set<CompilationUnitElement> units) {
    if (library == null || !libraries.add(library)) {
      return;
    }
    // may be skip library
    {
      UriKind uriKind = library.source.uriKind;
      // Optionally skip package: libraries.
      if (!options.showPackageWarnings && uriKind == UriKind.PACKAGE_URI) {
        return;
      }
      // Optionally skip SDK libraries.
      if (!options.showSdkWarnings && uriKind == UriKind.DART_URI) {
        return;
      }
    }
    // add compilation units
    addCompilationUnitSource(library.definingCompilationUnit, libraries, units);
    for (CompilationUnitElement child in library.parts) {
      addCompilationUnitSource(child, libraries, units);
    }
    // add referenced libraries
    for (LibraryElement child in library.importedLibraries) {
      addLibrarySources(child, libraries, units);
    }
    for (LibraryElement child in library.exportedLibraries) {
      addLibrarySources(child, libraries, units);
    }
  }

  /**
   * Treats the [sourcePath] as the top level library and analyzes it using a
   * asynchronous algorithm over the analysis engine.
   */
  void analyzeAsync() {
    setupForAnalysis();
    _analyzeAsync();
  }

  /**
   * Treats the [sourcePath] as the top level library and analyzes it using a
   * synchronous algorithm over the analysis engine. If [printMode] is `0`,
   * then no error or performance information is printed. If [printMode] is `1`,
   * then both will be printed. If [printMode] is `2`, then only performance
   * information will be printed, and it will be marked as being for a cold VM.
   */
  ErrorSeverity analyzeSync({int printMode: 1}) {
    setupForAnalysis();
    return _analyzeSync(printMode);
  }

  void prepareAnalysisContext(JavaFile sourceFile, Source source) {
    List<UriResolver> resolvers = [
        new CustomUriResolver(options.customUrlMappings),
        new DartUriResolver(sdk),
        new FileUriResolver()];
    // may be add package resolver
    {
      JavaFile packageDirectory;
      if (options.packageRootPath != null) {
        packageDirectory = new JavaFile(options.packageRootPath);
        resolvers.add(new PackageUriResolver([packageDirectory]));
      } else {
        PubPackageMapProvider pubPackageMapProvider =
            new PubPackageMapProvider(PhysicalResourceProvider.INSTANCE, sdk);
        PackageMapInfo packageMapInfo = pubPackageMapProvider.computePackageMap(
            PhysicalResourceProvider.INSTANCE.getResource('.'));
        Map<String, List<Folder>> packageMap = packageMapInfo.packageMap;
        if (packageMap != null) {
          resolvers.add(
              new PackageMapUriResolver(
                  PhysicalResourceProvider.INSTANCE,
                  packageMap));
        }
      }
    }
    sourceFactory = new SourceFactory(resolvers);
    context = AnalysisEngine.instance.createAnalysisContext();
    context.sourceFactory = sourceFactory;
    Map<String, String> definedVariables = options.definedVariables;
    if (!definedVariables.isEmpty) {
      DeclaredVariables declaredVariables = context.declaredVariables;
      definedVariables.forEach((String variableName, String value) {
        declaredVariables.define(variableName, value);
      });
    }
    // Uncomment the following to have errors reported on stdout and stderr
    AnalysisEngine.instance.logger = new StdLogger(options.log);

    // set options for context
    AnalysisOptionsImpl contextOptions = new AnalysisOptionsImpl();
    contextOptions.cacheSize = _MAX_CACHE_SIZE;
    contextOptions.hint = !options.disableHints;
    context.analysisOptions = contextOptions;

    // Create and add a ChangeSet
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    context.applyChanges(changeSet);
  }

  /// Fills [errorInfos] using [sources].
  void prepareErrors() {
    for (Source source in sources) {
      context.computeErrors(source);
      var sourceErrors = context.getErrors(source);
      errorInfos.add(sourceErrors);
    }
  }

  /// Fills [sources].
  void prepareSources(LibraryElement library) {
    var units = new Set<CompilationUnitElement>();
    var libraries = new Set<LibraryElement>();
    addLibrarySources(library, libraries, units);
  }

  /**
   * Setup local fields such as the analysis context for analysis.
   */
  void setupForAnalysis() {
    sources.clear();
    errorInfos.clear();
    if (sourcePath == null) {
      throw new ArgumentError("sourcePath cannot be null");
    }
    JavaFile sourceFile = new JavaFile(sourcePath);
    Uri uri = getUri(sourceFile);
    librarySource = new FileBasedSource.con2(uri, sourceFile);

    // prepare context
    prepareAnalysisContext(sourceFile, librarySource);
  }

  /// The async version of the analysis
  void _analyzeAsync() {
    new Future(context.performAnalysisTask).then((AnalysisResult result) {
      List<ChangeNotice> notices = result.changeNotices;
      if (result.hasMoreWork) {
        // There is more work, record the set of sources, and then call self
        // again to perform next task
        for (ChangeNotice notice in notices) {
          sources.add(notice.source);
          sourceErrorsMap[notice.source] = notice;
        }
        return _analyzeAsync();
      }
      //
      // There are not any more tasks, set error code and print performance
      // numbers.
      //
      // prepare errors
      sourceErrorsMap.forEach((k, v) {
        errorInfos.add(sourceErrorsMap[k]);
      });

      // print errors and performance numbers
      _printErrorsAndPerf();

      // compute max severity and set exitCode
      ErrorSeverity status = maxErrorSeverity;
      if (status == ErrorSeverity.WARNING && options.warningsAreFatal) {
        status = ErrorSeverity.ERROR;
      }
      exitCode = status.ordinal;
    }).catchError((ex, st) {
      AnalysisEngine.instance.logger.logError("$ex\n$st");
    });
  }

  /// The sync version of analysis.
  ErrorSeverity _analyzeSync(int printMode) {
    // don't try to analyze parts
    if (context.computeKindOf(librarySource) == SourceKind.PART) {
      print("Only libraries can be analyzed.");
      print("$sourcePath is a part and can not be analyzed.");
      return ErrorSeverity.ERROR;
    }
    // resolve library
    var libraryElement = context.computeLibraryElement(librarySource);
    // prepare source and errors
    prepareSources(libraryElement);
    prepareErrors();

    // print errors and performance numbers
    if (printMode == 1) {
      _printErrorsAndPerf();
    } else if (printMode == 2) {
      _printColdPerf();
    }

    // compute max severity and set exitCode
    ErrorSeverity status = maxErrorSeverity;
    if (status == ErrorSeverity.WARNING && options.warningsAreFatal) {
      status = ErrorSeverity.ERROR;
    }
    return status;
  }

  bool _isDesiredError(AnalysisError error) {
    if (error.errorCode.type == ErrorType.TODO) {
      return false;
    }
    if (computeSeverity(error, options.enableTypeChecks) ==
        ErrorSeverity.INFO &&
        options.disableHints) {
      return false;
    }
    return true;
  }

  _printColdPerf() {
    // print cold VM performance numbers
    int totalTime = JavaSystem.currentTimeMillis() - startTime;
    int otherTime = totalTime;
    for (PerformanceTag tag in PerformanceTag.all) {
      if (tag != PerformanceTag.UNKNOWN) {
        int tagTime = tag.elapsedMs;
        stdout.writeln('${tag.label}-cold:$tagTime');
        otherTime -= tagTime;
      }
    }
    stdout.writeln('other-cold:$otherTime');
    stdout.writeln("total-cold:$totalTime");
  }

  _printErrorsAndPerf() {
    // The following is a hack. We currently print out to stderr to ensure that
    // when in batch mode we print to stderr, this is because the prints from
    // batch are made to stderr. The reason that options.shouldBatch isn't used
    // is because when the argument flags are constructed in BatchRunner and
    // passed in from batch mode which removes the batch flag to prevent the
    // "cannot have the batch flag and source file" error message.
    IOSink sink = options.machineFormat ? stderr : stdout;

    // print errors
    ErrorFormatter formatter =
        new ErrorFormatter(sink, options, _isDesiredError);
    formatter.formatErrors(errorInfos);

    // print performance numbers
    if (options.perf || options.warmPerf) {
      int totalTime = JavaSystem.currentTimeMillis() - startTime;
      int otherTime = totalTime;
      for (PerformanceTag tag in PerformanceTag.all) {
        if (tag != PerformanceTag.UNKNOWN) {
          int tagTime = tag.elapsedMs;
          stdout.writeln('${tag.label}:$tagTime');
          otherTime -= tagTime;
        }
      }
      stdout.writeln('other:$otherTime');
      stdout.writeln("total:$totalTime");
    }
  }

  /**
   * Compute the severity of the error; however, if
   * [enableTypeChecks] is false, then de-escalate checked-mode compile time
   * errors to a severity of [ErrorSeverity.INFO].
   */
  static ErrorSeverity computeSeverity(AnalysisError error,
      bool enableTypeChecks) {
    if (!enableTypeChecks &&
        error.errorCode.type == ErrorType.CHECKED_MODE_COMPILE_TIME_ERROR) {
      return ErrorSeverity.INFO;
    }
    return error.errorCode.errorSeverity;
  }

  static JavaFile getPackageDirectoryFor(JavaFile sourceFile) {
    // we are going to ask parent file, so get absolute path
    sourceFile = sourceFile.getAbsoluteFile();
    // look in the containing directories
    JavaFile dir = sourceFile.getParentFile();
    while (dir != null) {
      JavaFile packagesDir = new JavaFile.relative(dir, "packages");
      if (packagesDir.exists()) {
        return packagesDir;
      }
      dir = dir.getParentFile();
    }
    // not found
    return null;
  }

  /**
   * Returns the [Uri] for the given input file.
   *
   * Usually it is a `file:` [Uri], but if [file] is located in the `lib`
   * directory of the [sdk], then returns a `dart:` [Uri].
   */
  static Uri getUri(JavaFile file) {
    // may be file in SDK
    {
      Source source = sdk.fromFileUri(file.toURI());
      if (source != null) {
        return source.uri;
      }
    }
    // some generic file
    return file.toURI();
  }

  /**
   * Convert [sourcePath] into an absolute path.
   */
  static String _normalizeSourcePath(String sourcePath) {
    return new File(sourcePath).absolute.path;
  }
}

/**
 * This [Logger] prints out information comments to [stdout] and error messages
 * to [stderr].
 */
class StdLogger extends Logger {
  final bool log;

  StdLogger(this.log);

  @override
  void logError(String message, [CaughtException exception]) {
    stderr.writeln(message);
    if (exception != null) {
      stderr.writeln(exception);
    }
  }

  @override
  void logError2(String message, Object exception) {
    stderr.writeln(message);
  }

  @override
  void logInformation(String message, [CaughtException exception]) {
    if (log) {
      stdout.writeln(message);
      if (exception != null) {
        stderr.writeln(exception);
      }
    }
  }

  @override
  void logInformation2(String message, Object exception) {
    if (log) {
      stdout.writeln(message);
    }
  }
}

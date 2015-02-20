// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library options;

import 'dart:io';

import 'package:args/args.dart';

const _BINARY_NAME = 'dartanalyzer';

/**
 * Analyzer commandline configuration options.
 */
class CommandLineOptions {
  /** The path to the dart SDK */
  final String dartSdkPath;

  /** A table mapping the names of defined variables to their values. */
  final Map<String, String> definedVariables;

  /** Whether to report hints */
  final bool disableHints;

  /** Whether to display version information */
  final bool displayVersion;

  /**
   * Whether to treat type mismatches found during constant evaluation as
   * errors.
   */
  final bool enableTypeChecks;

  /** Whether to ignore unrecognized flags */
  final bool ignoreUnrecognizedFlags;

  /** Whether to log additional analysis messages and exceptions */
  final bool log;

  /** Whether to use machine format for error display */
  final bool machineFormat;

  /** The path to the package root */
  final String packageRootPath;

  /** Whether to show performance statistics */
  final bool perf;

  /** Batch mode (for unit testing) */
  final bool shouldBatch;

  /** Whether to show package: warnings */
  final bool showPackageWarnings;

  /** Whether to show SDK warnings */
  final bool showSdkWarnings;

  /** The source files to analyze */
  final List<String> sourceFiles;

  /** Whether to show both cold and hot performance statistics */
  final bool warmPerf;

  /** Whether to treat warnings as fatal */
  final bool warningsAreFatal;

  /** A table mapping library URIs to the file system path where the library
   *  source is located.
   */
  final Map<String, String> customUrlMappings;

  /**
   * Initialize options from the given parsed [args].
   */
  CommandLineOptions._fromArgs(ArgResults args, Map<String,
      String> definedVariables, Map<String, String> customUrlMappings)
      : dartSdkPath = args['dart-sdk'],
        this.definedVariables = definedVariables,
        disableHints = args['no-hints'],
        displayVersion = args['version'],
        enableTypeChecks = args['enable_type_checks'],
        ignoreUnrecognizedFlags = args['ignore-unrecognized-flags'],
        log = args['log'],
        machineFormat = args['machine'] || args['format'] == 'machine',
        packageRootPath = args['package-root'],
        perf = args['perf'],
        shouldBatch = args['batch'],
        showPackageWarnings = args['show-package-warnings'] ||
          args['package-warnings'],
        showSdkWarnings = args['show-sdk-warnings'] || args['warnings'],
        sourceFiles = args.rest,
        warmPerf = args['warm-perf'],
        warningsAreFatal = args['fatal-warnings'],
        this.customUrlMappings = customUrlMappings;

  /**
   * Parse [args] into [CommandLineOptions] describing the specified
   * analyzer options. In case of a format error, prints error and exists.
   */
  static CommandLineOptions parse(List<String> args) {
    CommandLineOptions options = _parse(args);
    // check SDK
    {
      var sdkPath = options.dartSdkPath;
      // check that SDK is specified
      if (sdkPath == null) {
        print('Usage: $_BINARY_NAME: no Dart SDK found.');
        exit(15);
      }
      // check that SDK is existing directory
      if (!(new Directory(sdkPath)).existsSync()) {
        print('Usage: $_BINARY_NAME: invalid Dart SDK path: $sdkPath');
        exit(15);
      }
    }
    // OK
    return options;
  }

  static String _getVersion() {
    try {
      // This is relative to bin/snapshot, so ../..
      String versionPath =
          Platform.script.resolve('../../version').toFilePath();
      File versionFile = new File(versionPath);
      return versionFile.readAsStringSync().trim();
    } catch (_) {
      // This happens when the script is not running in the context of an SDK.
      return "<unknown>";
    }
  }

  static CommandLineOptions _parse(List<String> args) {
    args = args.expand((String arg) => arg.split('=')).toList();
    var parser = new CommandLineParser()
        ..addFlag(
            'batch',
            abbr: 'b',
            help: 'Run in batch mode',
            defaultsTo: false,
            negatable: false)
        ..addOption('dart-sdk', help: 'The path to the Dart SDK')
        ..addOption(
            'package-root',
            abbr: 'p',
            help:
                'The path to the package root. The flag package-root is deprecated. Remove to use package information computed by pub.')
        ..addOption(
            'format',
            help: 'Specifies the format in which errors are displayed')
        ..addFlag(
            'machine',
            help: 'Print errors in a format suitable for parsing (deprecated)',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'version',
            help: 'Print the analyzer version',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'no-hints',
            help: 'Do not show hint results',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'ignore-unrecognized-flags',
            help: 'Ignore unrecognized command line flags',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'fatal-warnings',
            help: 'Treat non-type warnings as fatal',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'package-warnings',
            help: 'Show warnings from package: imports',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'show-package-warnings',
            help: 'Show warnings from package: imports (deprecated)',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'perf',
            help: 'Show performance statistics',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'warnings',
            help: 'Show warnings from SDK imports',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'show-sdk-warnings',
            help: 'Show warnings from SDK imports (deprecated)',
            defaultsTo: false,
            negatable: false)
        ..addFlag(
            'help',
            abbr: 'h',
            help: 'Display this help message',
            defaultsTo: false,
            negatable: false)
        ..addOption(
            'url-mapping',
            help: '--url-mapping=libraryUri,/path/to/library.dart directs the '
                'analyzer to use "library.dart" as the source for an import ' 'of "libraryUri"',
            allowMultiple: true)
        //
        // Hidden flags.
        //
        ..addFlag(
            'enable-async',
            help: 'Enable support for the proposed async feature',
            defaultsTo: false,
            negatable: false,
            hide: true)
        ..addFlag(
            'enable-enum',
            help: 'Enable support for the proposed enum feature',
            defaultsTo: false,
            negatable: false,
            hide: true)
        ..addFlag(
            'log',
            help: 'Log additional messages and exceptions',
            defaultsTo: false,
            negatable: false,
            hide: true)
        ..addFlag(
            'warm-perf',
            help: 'Show both cold and warm performance statistics',
            defaultsTo: false,
            negatable: false,
            hide: true)
        ..addFlag(
            'enable_type_checks',
            help: 'Check types in constant evaluation',
            defaultsTo: false,
            negatable: false,
            hide: true);

    try {
      // TODO(scheglov) https://code.google.com/p/dart/issues/detail?id=11061
      args =
          args.map((String arg) => arg == '-batch' ? '--batch' : arg).toList();
      Map<String, String> definedVariables = <String, String>{};
      var results = parser.parse(args, definedVariables);
      // help requests
      if (results['help']) {
        _showUsage(parser);
        exit(0);
      }
      // batch mode and input files
      if (results['batch']) {
        if (results.rest.isNotEmpty) {
          print('No source files expected in the batch mode.');
          _showUsage(parser);
          exit(15);
        }
      } else if (results['version']) {
        print('$_BINARY_NAME version ${_getVersion()}');
        exit(0);
      } else {
        if (results.rest.isEmpty) {
          _showUsage(parser);
          exit(15);
        }
      }
      Map<String, String> customUrlMappings = <String, String>{};
      for (String mapping in results['url-mapping']) {
        List<String> splitMapping = mapping.split(',');
        if (splitMapping.length != 2) {
          _showUsage(parser);
          exit(15);
        }
        customUrlMappings[splitMapping[0]] = splitMapping[1];
      }
      return new CommandLineOptions._fromArgs(
          results,
          definedVariables,
          customUrlMappings);
    } on FormatException catch (e) {
      print(e.message);
      _showUsage(parser);
      exit(15);
    }

  }

  static _showUsage(parser) {
    print('Usage: $_BINARY_NAME [options...] <libraries to analyze...>');
    print(parser.getUsage());
    print('');
    print('For more information, see http://www.dartlang.org/tools/analyzer.');
  }
}

/**
 * Commandline argument parser.
 *
 * TODO(pquitslund): when the args package supports ignoring unrecognized
 * options/flags, this class can be replaced with a simple [ArgParser] instance.
 */
class CommandLineParser {

  final List<String> _knownFlags;
  final bool _alwaysIgnoreUnrecognized;
  final ArgParser _parser;

  /** Creates a new command line parser */
  CommandLineParser({bool alwaysIgnoreUnrecognized: false})
      : _knownFlags = <String>[],
        _alwaysIgnoreUnrecognized = alwaysIgnoreUnrecognized,
        _parser = new ArgParser(allowTrailingOptions: true);


  ArgParser get parser => _parser;

  /**
   * Defines a flag.
   *
   * See [ArgParser.addFlag()].
   */
  void addFlag(String name, {String abbr, String help, bool defaultsTo: false,
      bool negatable: true, void callback(bool value), bool hide: false}) {
    _knownFlags.add(name);
    _parser.addFlag(
        name,
        abbr: abbr,
        help: help,
        defaultsTo: defaultsTo,
        negatable: negatable,
        callback: callback,
        hide: hide);
  }

  /**
   * Defines a value-taking option.
   *
   * See [ArgParser.addOption()].
   */
  void addOption(String name, {String abbr, String help, List<String> allowed,
      Map<String, String> allowedHelp, String defaultsTo, void callback(value),
      bool allowMultiple: false}) {
    _knownFlags.add(name);
    _parser.addOption(
        name,
        abbr: abbr,
        help: help,
        allowed: allowed,
        allowedHelp: allowedHelp,
        defaultsTo: defaultsTo,
        callback: callback,
        allowMultiple: allowMultiple);
  }


  /**
   * Generates a string displaying usage information for the defined options.
   *
   * See [ArgParser.usage].
   */
  String getUsage() => _parser.usage;

  /**
   * Parses [args], a list of command-line arguments, matches them against the
   * flags and options defined by this parser, and returns the result. The
   * values of any defined variables are captured in the given map.
   *
   * See [ArgParser].
   */
  ArgResults parse(List<String> args, Map<String, String> definedVariables) =>
      _parser.parse(_filterUnknowns(parseDefinedVariables(args, definedVariables)));

  List<String> parseDefinedVariables(List<String> args, Map<String,
      String> definedVariables) {
    int count = args.length;
    List<String> remainingArgs = <String>[];
    for (int i = 0; i < count; i++) {
      String arg = args[i];
      if (arg == '--') {
        while (i < count) {
          remainingArgs.add(args[i++]);
        }
      } else if (arg.startsWith("-D")) {
        definedVariables[arg.substring(2)] = args[++i];
      } else {
        remainingArgs.add(arg);
      }
    }
    return remainingArgs;
  }

  List<String> _filterUnknowns(List<String> args) {

    // Only filter args if the ignore flag is specified, or if
    // _alwaysIgnoreUnrecognized was set to true
    if (_alwaysIgnoreUnrecognized ||
        args.contains('--ignore-unrecognized-flags')) {

      //TODO(pquitslund): replace w/ the following once library skew issues are
      // sorted out
      //return args.where((arg) => !arg.startsWith('--') ||
      //  _knownFlags.contains(arg.substring(2)));

      // Filter all unrecognized flags and options.
      List<String> filtered = <String>[];
      for (int i = 0; i < args.length; ++i) {
        String arg = args[i];
        if (arg.startsWith('--') && arg.length > 2) {
          String option = arg.substring(2);
          // strip the last '=value'
          int equalsOffset = option.lastIndexOf('=');
          if (equalsOffset != -1) {
            option = option.substring(0, equalsOffset);
          }
          // check the option
          if (!_knownFlags.contains(option)) {
            //print('remove: $arg');
            //"eat" params by advancing to the next flag/option
            i = _getNextFlagIndex(args, i);
          } else {
            filtered.add(arg);
          }
        } else {
          filtered.add(arg);
        }
      }

      return filtered;
    } else {
      return args;
    }
  }

  _getNextFlagIndex(args, i) {
    for ( ; i < args.length; ++i) {
      if (args[i].startsWith('--')) {
        return i;
      }
    }
    return i;
  }
}

// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library driver;

import 'dart:async';
import 'dart:io';
import 'dart:math';

import 'package:analysis_server/plugin/plugin.dart';
import 'package:analysis_server/src/analysis_server.dart';
import 'package:analysis_server/src/plugin/plugin_impl.dart';
import 'package:analysis_server/src/plugin/server_plugin.dart';
import 'package:analysis_server/src/server/http_server.dart';
import 'package:analysis_server/src/server/stdio_server.dart';
import 'package:analysis_server/src/socket_server.dart';
import 'package:analysis_server/starter.dart';
import 'package:analyzer/file_system/physical_file_system.dart';
import 'package:analyzer/instrumentation/file_instrumentation.dart';
import 'package:analyzer/instrumentation/instrumentation.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/incremental_logger.dart';
import 'package:analyzer/src/generated/java_io.dart';
import 'package:analyzer/src/generated/sdk.dart';
import 'package:analyzer/src/generated/sdk_io.dart';
import 'package:analyzer/options.dart';
import 'package:args/args.dart';

/**
 * Initializes incremental logger.
 *
 * Supports following formats of [spec]:
 *
 *     "console" - log to the console;
 *     "file:/some/file/name" - log to the file, overwritten on start.
 */
void _initIncrementalLogger(String spec) {
  logger = NULL_LOGGER;
  if (spec == null) {
    return;
  }
  // create logger
  if (spec == 'console') {
    logger = new StringSinkLogger(stdout);
  }
  if (spec.startsWith('file:')) {
    String fileName = spec.substring('file:'.length);
    File file = new File(fileName);
    IOSink sink = file.openWrite();
    logger = new StringSinkLogger(sink);
  }
}


/**
 * The [Driver] class represents a single running instance of the analysis
 * server application.  It is responsible for parsing command line options
 * and starting the HTTP and/or stdio servers.
 */
class Driver implements ServerStarter {
  /**
   * The name of the application that is used to start a server.
   */
  static const BINARY_NAME = "server";

  /**
   * The name of the option used to set the identifier for the client.
   */
  static const String CLIENT_ID = "client-id";

  /**
   * The name of the option used to set the version for the client.
   */
  static const String CLIENT_VERSION = "client-version";

  /**
   * The name of the option used to enable incremental resolution of API
   * changes.
   */
  static const String ENABLE_INCREMENTAL_RESOLUTION_API =
      "enable-incremental-resolution-api";

  /**
   * The name of the option used to describe the incremental resolution logger.
   */
  static const String INCREMENTAL_RESOLUTION_LOG = "incremental-resolution-log";

  /**
   * The name of the option used to enable validation of incremental resolution
   * results.
   */
  static const String INCREMENTAL_RESOLUTION_VALIDATION =
      "incremental-resolution-validation";

  /**
   * The name of the option used to cause instrumentation to also be written to
   * a local file.
   */
  static const String INSTRUMENTATION_LOG_FILE = "instrumentation-log-file";

  /**
   * The name of the option used to enable instrumentation.
   */
  static const String ENABLE_INSTRUMENTATION_OPTION = "enable-instrumentation";

  /**
   * The name of the option used to print usage information.
   */
  static const String HELP_OPTION = "help";

  /**
   * The name of the option used to specify if [print] should print to the
   * console instead of being intercepted.
   */
  static const String INTERNAL_PRINT_TO_CONSOLE = "internal-print-to-console";

  /**
   * The name of the option used to specify if [print] should print to the
   * console instead of being intercepted.
   */
  static const String INTERNAL_DELAY_FREQUENCY = 'internal-delay-frequency';

  /**
   * The name of the option used to specify the port to which the server will
   * connect.
   */
  static const String PORT_OPTION = "port";

  /**
   * The path to the SDK.
   * TODO(paulberry): get rid of this once the 'analysis.updateSdks' request is
   * operational.
   */
  static const String SDK_OPTION = "sdk";

  /**
   * The name of the flag used to disable error notifications.
   */
  static const String NO_ERROR_NOTIFICATION = "no-error-notification";

  /**
   * The name of the flag used to disable the index.
   */
  static const String NO_INDEX = "no-index";

  /**
   * The name of the option used to set the file read mode.
   */
  static const String FILE_READ_MODE = "file-read-mode";

  /**
   * The instrumentation server that is to be used by the analysis server.
   */
  InstrumentationServer instrumentationServer;

  /**
   * The plugins that are defined outside the analysis_server package.
   */
  List<Plugin> _userDefinedPlugins = <Plugin>[];

  SocketServer socketServer;

  HttpAnalysisServer httpServer;

  StdioAnalysisServer stdioServer;

  Driver();

  /**
   * Set the [plugins] that are defined outside the analysis_server package.
   */
  void set userDefinedPlugins(List<Plugin> plugins) {
    _userDefinedPlugins = plugins == null ? <Plugin>[] : plugins;
  }

  /**
   * Use the given command-line [arguments] to start this server.
   */
  void start(List<String> arguments) {
    CommandLineParser parser = _createArgParser();
    ArgResults results = parser.parse(arguments, <String, String>{});
    if (results[HELP_OPTION]) {
      _printUsage(parser.parser);
      return;
    }

    // TODO(brianwilkerson) Enable this after it is possible for an
    // instrumentation server to be provided.
//    if (results[ENABLE_INSTRUMENTATION_OPTION]) {
//      if (instrumentationServer == null) {
//        print('Exiting server: enabled instrumentation without providing an instrumentation server');
//        print('');
//        _printUsage(parser);
//        return;
//      }
//    } else {
//      if (instrumentationServer != null) {
//        print('Exiting server: providing an instrumentation server without enabling instrumentation');
//        print('');
//        _printUsage(parser);
//        return;
//      }
//    }

    // TODO (danrubel) Remove this workaround
    // once the underlying VM and dart:io issue has been fixed.
    if (results[INTERNAL_DELAY_FREQUENCY] != null) {
      AnalysisServer.performOperationDelayFreqency =
          int.parse(results[INTERNAL_DELAY_FREQUENCY], onError: (_) => 0);
    }

    int port;
    bool serve_http = false;
    if (results[PORT_OPTION] != null) {
      serve_http = true;
      try {
        port = int.parse(results[PORT_OPTION]);
      } on FormatException {
        print('Invalid port number: ${results[PORT_OPTION]}');
        print('');
        _printUsage(parser.parser);
        exitCode = 1;
        return;
      }
    }

    AnalysisServerOptions analysisServerOptions = new AnalysisServerOptions();
    analysisServerOptions.enableIncrementalResolutionApi =
        results[ENABLE_INCREMENTAL_RESOLUTION_API];
    analysisServerOptions.enableIncrementalResolutionValidation =
        results[INCREMENTAL_RESOLUTION_VALIDATION];
    analysisServerOptions.noErrorNotification = results[NO_ERROR_NOTIFICATION];
    analysisServerOptions.noIndex = results[NO_INDEX];
    analysisServerOptions.fileReadMode = results[FILE_READ_MODE];

    _initIncrementalLogger(results[INCREMENTAL_RESOLUTION_LOG]);

    DartSdk defaultSdk;
    if (results[SDK_OPTION] != null) {
      defaultSdk = new DirectoryBasedDartSdk(new JavaFile(results[SDK_OPTION]));
    } else {
      // No path to the SDK provided; use DirectoryBasedDartSdk.defaultSdk,
      // which will make a guess.
      defaultSdk = DirectoryBasedDartSdk.defaultSdk;
    }

    if (instrumentationServer != null) {
      String filePath = results[INSTRUMENTATION_LOG_FILE];
      if (filePath != null) {
        instrumentationServer = new MulticastInstrumentationServer(
            [instrumentationServer, new FileInstrumentationServer(filePath)]);
      }
    }
    InstrumentationService service =
        new InstrumentationService(instrumentationServer);
    service.logVersion(
        _readUuid(service),
        results[CLIENT_ID],
        results[CLIENT_VERSION],
        AnalysisServer.VERSION,
        defaultSdk.sdkVersion);
    AnalysisEngine.instance.instrumentationService = service;
    //
    // Process all of the plugins so that extensions are registered.
    //
    ServerPlugin serverPlugin = new ServerPlugin();
    List<Plugin> plugins = <Plugin>[];
    plugins.add(serverPlugin);
    plugins.addAll(_userDefinedPlugins);
    ExtensionManager manager = new ExtensionManager();
    manager.processPlugins(plugins);

    socketServer =
        new SocketServer(analysisServerOptions, defaultSdk, service, serverPlugin);
    httpServer = new HttpAnalysisServer(socketServer);
    stdioServer = new StdioAnalysisServer(socketServer);

    if (serve_http) {
      httpServer.serveHttp(port);
    }

    _captureExceptions(service, () {
      stdioServer.serveStdio().then((_) {
        if (serve_http) {
          httpServer.close();
        }
        service.shutdown();
        exit(0);
      });
    },
        print: results[INTERNAL_PRINT_TO_CONSOLE] ? null : httpServer.recordPrint);
  }

  /**
   * Execute the given [callback] within a zone that will capture any unhandled
   * exceptions and both report them to the client and send them to the given
   * instrumentation [service]. If a [print] function is provided, then also
   * capture any data printed by the callback and redirect it to the function.
   */
  dynamic _captureExceptions(InstrumentationService service, dynamic callback(),
      {void print(String line)}) {
    Function errorFunction =
        (Zone self, ZoneDelegate parent, Zone zone, dynamic exception,
            StackTrace stackTrace) {
      service.logPriorityException(exception, stackTrace);
      AnalysisServer analysisServer = socketServer.analysisServer;
      analysisServer.sendServerErrorNotification(exception, stackTrace);
      throw exception;
    };
    Function printFunction = print == null ?
        null :
        (Zone self, ZoneDelegate parent, Zone zone, String line) {
      // Note: we don't pass the line on to stdout, because that is reserved
      // for communication to the client.
      print(line);
    };
    ZoneSpecification zoneSpecification = new ZoneSpecification(
        handleUncaughtError: errorFunction,
        print: printFunction);
    return runZoned(callback, zoneSpecification: zoneSpecification);
  }

  /**
   * Create and return the parser used to parse the command-line arguments.
   */
  CommandLineParser _createArgParser() {
    CommandLineParser parser =
        new CommandLineParser(alwaysIgnoreUnrecognized: true);
    parser.addOption(
        CLIENT_ID,
        help: "an identifier used to identify the client");
    parser.addOption(CLIENT_VERSION, help: "the version of the client");
    parser.addFlag(
        ENABLE_INCREMENTAL_RESOLUTION_API,
        help: "enable using incremental resolution for API changes",
        defaultsTo: false,
        negatable: false);
    parser.addFlag(
        ENABLE_INSTRUMENTATION_OPTION,
        help: "enable sending instrumentation information to a server",
        defaultsTo: false,
        negatable: false);
    parser.addFlag(
        HELP_OPTION,
        help: "print this help message without starting a server",
        defaultsTo: false,
        negatable: false);
    parser.addOption(
        INCREMENTAL_RESOLUTION_LOG,
        help: "the description of the incremental resolution log");
    parser.addFlag(
        INCREMENTAL_RESOLUTION_VALIDATION,
        help: "enable validation of incremental resolution results (slow)",
        defaultsTo: false,
        negatable: false);
    parser.addOption(
        INSTRUMENTATION_LOG_FILE,
        help: "the path of the file to which instrumentation data will be written");
    parser.addFlag(
        INTERNAL_PRINT_TO_CONSOLE,
        help: "enable sending `print` output to the console",
        defaultsTo: false,
        negatable: false);
    parser.addOption(
        PORT_OPTION,
        help: "[port] the port on which the server will listen");
    parser.addOption(INTERNAL_DELAY_FREQUENCY);
    parser.addOption(SDK_OPTION, help: "[path] the path to the sdk");
    parser.addFlag(
        NO_ERROR_NOTIFICATION,
        help: "disable sending all analysis error notifications to the server",
        defaultsTo: false,
        negatable: false);
    parser.addFlag(
        NO_INDEX,
        help: "disable indexing sources",
        defaultsTo: false,
        negatable: false);
    parser.addOption(
        FILE_READ_MODE,
        help: "an option of the ways files can be read from disk, " +
            "some clients normalize end of line characters which would make " +
            "the file offset and range information incorrect.",
        allowed: ["as-is", "normalize-eol-always"],
        allowedHelp: {
      "as-is": "file contents are read as-is, no file changes occur",
      "normalize-eol-always":
          r'file contents normalize the end of line characters to the single character new line `\n`'
    }, defaultsTo: "as-is");

    return parser;
  }

  /**
   * Print information about how to use the server.
   */
  void _printUsage(ArgParser parser) {
    print('Usage: $BINARY_NAME [flags]');
    print('');
    print('Supported flags are:');
    print(parser.usage);
  }

  /**
   * Read the UUID from disk, generating and storing a new one if necessary.
   */
  String _readUuid(InstrumentationService service) {
    File uuidFile = new File(
        PhysicalResourceProvider.INSTANCE.getStateLocation(
            '.instrumentation').getChild('uuid.txt').path);
    try {
      if (uuidFile.existsSync()) {
        String uuid = uuidFile.readAsStringSync();
        if (uuid != null && uuid.length > 5) {
          return uuid;
        }
      }
    } catch (exception, stackTrace) {
      service.logPriorityException(exception, stackTrace);
    }
    int millisecondsSinceEpoch = new DateTime.now().millisecondsSinceEpoch;
    int random = new Random().nextInt(0x3fffffff);
    String uuid = '$millisecondsSinceEpoch$random';
    try {
      uuidFile.parent.createSync(recursive: true);
      uuidFile.writeAsStringSync(uuid);
    } catch (exception, stackTrace) {
      service.logPriorityException(exception, stackTrace);
      // Slightly alter the uuid to indicate it was not persisted
      uuid = 'temp-$uuid';
    }
    return uuid;
  }
}

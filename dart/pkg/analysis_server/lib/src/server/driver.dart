// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library driver;

import 'dart:async';
import 'dart:io';

import 'package:analysis_server/plugin/plugin.dart';
import 'package:analysis_server/src/analysis_server.dart';
import 'package:analysis_server/src/plugin/plugin_impl.dart';
import 'package:analysis_server/src/plugin/server_plugin.dart';
import 'package:analysis_server/src/server/http_server.dart';
import 'package:analysis_server/src/server/stdio_server.dart';
import 'package:analysis_server/src/socket_server.dart';
import 'package:analysis_server/starter.dart';
import 'package:analyzer/instrumentation/instrumentation.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/incremental_logger.dart';
import 'package:analyzer/src/generated/java_io.dart';
import 'package:analyzer/src/generated/sdk.dart';
import 'package:analyzer/src/generated/sdk_io.dart';
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
    logger = new StringSinkLogger(console.log);
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
   * The name of the option used to disable error notifications.
   */
  static const String NO_ERROR_NOTIFICATION = "no-error-notification";

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
    ArgParser parser = _createArgParser();
    ArgResults results = parser.parse(arguments);
    if (results[HELP_OPTION]) {
      _printUsage(parser);
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

    int port;
    bool serve_http = false;
    if (results[PORT_OPTION] != null) {
      serve_http = true;
      try {
        port = int.parse(results[PORT_OPTION]);
      } on FormatException {
        print('Invalid port number: ${results[PORT_OPTION]}');
        print('');
        _printUsage(parser);
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

    _initIncrementalLogger(results[INCREMENTAL_RESOLUTION_LOG]);

    DartSdk defaultSdk;
    if (results[SDK_OPTION] != null) {
      defaultSdk = new DirectoryBasedDartSdk(new JavaFile(results[SDK_OPTION]));
    } else {
      // No path to the SDK provided; use DirectoryBasedDartSdk.defaultSdk,
      // which will make a guess.
      defaultSdk = DirectoryBasedDartSdk.defaultSdk;
    }

    InstrumentationService service =
        new InstrumentationService(instrumentationServer);
//    service.logVersion(results[CLIENT_ID], defaultSdk.sdkVersion);
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
  ArgParser _createArgParser() {
    ArgParser parser = new ArgParser();
    parser.addOption(
        CLIENT_ID,
        help: "an identifier used to identify the client");
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
    parser.addFlag(
        INTERNAL_PRINT_TO_CONSOLE,
        help: "enable sending `print` output to the console",
        defaultsTo: false,
        negatable: false);
    parser.addOption(
        PORT_OPTION,
        help: "[port] the port on which the server will listen");
    parser.addOption(SDK_OPTION, help: "[path] the path to the sdk");
    parser.addFlag(
        NO_ERROR_NOTIFICATION,
        help:
            "disable sending all analysis error notifications to the server",
        defaultsTo: false,
        negatable: false);

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
}

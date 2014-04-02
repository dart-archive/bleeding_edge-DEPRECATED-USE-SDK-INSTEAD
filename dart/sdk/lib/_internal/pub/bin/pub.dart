// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:async';
import 'dart:io';

import 'package:args/args.dart';
import 'package:path/path.dart' as path;
import 'package:stack_trace/stack_trace.dart';

import '../lib/src/command.dart';
import '../lib/src/exit_codes.dart' as exit_codes;
import '../lib/src/http.dart';
import '../lib/src/io.dart';
import '../lib/src/log.dart' as log;
import '../lib/src/sdk.dart' as sdk;
import '../lib/src/utils.dart';

void main(List<String> arguments) {
  ArgResults options;

  try {
    options = PubCommand.pubArgParser.parse(arguments,
        allowTrailingOptions: true);
  } on FormatException catch (e) {
    log.error(e.message);
    log.error('Run "pub help" to see available options.');
    flushThenExit(exit_codes.USAGE);
    return;
  }

  if (options['version']) {
    log.message('Pub ${sdk.version}');
    return;
  }

  if (options['help']) {
    PubCommand.printGlobalUsage();
    return;
  }

  if (options['trace']) {
    log.recordTranscript();
  }

  switch (options['verbosity']) {
    case 'normal': log.showNormal(); break;
    case 'io':     log.showIO(); break;
    case 'solver': log.showSolver(); break;
    case 'all':    log.showAll(); break;
    default:
      // No specific verbosity given, so check for the shortcut.
      if (options['verbose']) {
        log.showAll();
      } else {
        log.showNormal();
      }
      break;
  }

  log.fine('Pub ${sdk.version}');

  var cacheDir;
  if (Platform.environment.containsKey('PUB_CACHE')) {
    cacheDir = Platform.environment['PUB_CACHE'];
  } else if (Platform.operatingSystem == 'windows') {
    var appData = Platform.environment['APPDATA'];
    cacheDir = path.join(appData, 'Pub', 'Cache');
  } else {
    cacheDir = '${Platform.environment['HOME']}/.pub-cache';
  }

  validatePlatform().then((_) => runPub(cacheDir, options, arguments));
}

/// Runs the appropriate pub command whose [arguments] have been parsed to
/// [options] using the system cache in [cacheDir].
///
/// Handles and correctly reports any errors that occur while running.
void runPub(String cacheDir, ArgResults options, List<String> arguments) {
  var captureStackChains =
      options['trace'] ||
      options['verbose'] ||
      options['verbosity'] == 'all';

  captureErrors(() => invokeCommand(cacheDir, options),
      captureStackChains: captureStackChains).catchError((error, Chain chain) {
    // This is basically the top-level exception handler so that we don't
    // spew a stack trace on our users.
    var message = getErrorMessage(error);
    log.error(message);
    log.fine("Exception type: ${error.runtimeType}");

    if (log.json.enabled) {
      if (error is UsageException) {
        // Don't print usage info in JSON output.
        log.json.error(error.message);
      } else {
        log.json.error(error);
      }
    }

    if (options['trace'] || !isUserFacingException(error)) {
      log.error(chain.terse);
    } else {
      log.fine(chain.terse);
    }

    if (error is ApplicationException && error.innerError != null) {
      var message = "Wrapped exception: ${error.innerError}";
      if (error.innerTrace != null) message = "$message\n${error.innerTrace}";
      log.fine(message);
    }

    if (options['trace']) {
      log.dumpTranscript();
    } else if (!isUserFacingException(error)) {
      log.error("""
This is an unexpected error. Please run

    pub --trace ${arguments.map((arg) => "'$arg'").join(' ')}

and include the results in a bug report on http://dartbug.com/new.
""");
    }

    return flushThenExit(chooseExitCode(error));
  }).then((_) {
    // Explicitly exit on success to ensure that any dangling dart:io handles
    // don't cause the process to never terminate.
    return flushThenExit(exit_codes.SUCCESS);
  });
}

/// Returns the appropriate exit code for [exception], falling back on 1 if no
/// appropriate exit code could be found.
int chooseExitCode(exception) {
  if (exception is HttpException || exception is HttpException ||
      exception is SocketException || exception is PubHttpException) {
    return exit_codes.UNAVAILABLE;
  } else if (exception is FormatException || exception is DataException) {
    return exit_codes.DATA;
  } else if (exception is UsageException) {
    return exit_codes.USAGE;
  } else {
    return 1;
  }
}

/// Walks the command tree and runs the selected pub command.
Future invokeCommand(String cacheDir, ArgResults mainOptions) {
  var commands = PubCommand.mainCommands;
  var command;
  var commandString = "pub";
  var options = mainOptions;

  while (commands.isNotEmpty) {
    if (options.command == null) {
      if (options.rest.isEmpty) {
        if (command == null) {
          // No top-level command was chosen.
          PubCommand.printGlobalUsage();
          return new Future.value();
        }

        command.usageError('Missing subcommand for "$commandString".');
      } else {
        if (command == null) {
          PubCommand.usageErrorWithCommands(commands,
              'Could not find a command named "${options.rest[0]}".');
        }

        command.usageError('Could not find a subcommand named '
            '"${options.rest[0]}" for "$commandString".');
      }
    }

    // Step into the command.
    options = options.command;
    command = commands[options.name];
    commands = command.subcommands;
    commandString += " ${options.name}";

    if (options['help']) {
      command.printUsage();
      return new Future.value();
    }
  }

  // Make sure there aren't unexpected arguments.
  if (!command.takesArguments && options.rest.isNotEmpty) {
    command.usageError(
        'Command "${options.name}" does not take any arguments.');
  }

  return syncFuture(() {
    return command.run(cacheDir, options);
  }).whenComplete(() {
    command.cache.deleteTempDir();
  });
}

/// Checks that pub is running on a supported platform. If it isn't, it prints
/// an error message and exits. Completes when the validation is done.
Future validatePlatform() {
  return syncFuture(() {
    if (Platform.operatingSystem != 'windows') return null;

    return runProcess('ver', []).then((result) {
      if (result.stdout.join('\n').contains('XP')) {
        log.error('Sorry, but pub is not supported on Windows XP.');
        return flushThenExit(exit_codes.USAGE);
      }
    });
  });
}

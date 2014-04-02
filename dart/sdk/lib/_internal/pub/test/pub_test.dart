// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub_tests;

import 'package:scheduled_test/scheduled_test.dart';

import '../lib/src/exit_codes.dart' as exit_codes;
import 'test_pub.dart';

final USAGE_STRING = """
    Pub is a package manager for Dart.

    Usage: pub <command> [arguments]

    Global options:
    -h, --help            Print this usage information.
        --version         Print pub version.
        --[no-]trace      Print debugging information when an error occurs.
        --verbosity       Control output verbosity.

              [all]       Show all output including internal tracing messages.
              [io]        Also show IO operations.
              [normal]    Show errors, warnings, and user messages.
              [solver]    Show steps during version resolution.

    -v, --verbose         Shortcut for "--verbosity=all".

    Available commands:
      build      Apply transformers to build a package.
      cache      Work with the system cache.
      deps       Print package dependencies.
      get        Get the current package's dependencies.
      help       Display help information for Pub.
      publish    Publish the current package to pub.dartlang.org.
      serve      Run a local web development server.
      upgrade    Upgrade the current package's dependencies to latest versions.
      uploader   Manage uploaders for a package on pub.dartlang.org.
      version    Print pub version.

    Use "pub help [command]" for more information about a command.
    """;

final VERSION_STRING = '''
    Pub 0.1.2+3
    ''';

main() {
  initConfig();

  integration('running pub with no command displays usage', () {
    schedulePub(args: [], output: USAGE_STRING);
  });

  integration('running pub with just --help displays usage', () {
    schedulePub(args: ['--help'], output: USAGE_STRING);
  });

  integration('running pub with just -h displays usage', () {
    schedulePub(args: ['-h'], output: USAGE_STRING);
  });

  integration('running pub with --help after command shows command usage', () {
    schedulePub(args: ['get', '--help'],
        output: '''
          Get the current package's dependencies.

          Usage: pub get
          -h, --help            Print usage information for this command.
              --[no-]offline    Use cached packages instead of accessing the network.
    ''');
  });

  integration('running pub with -h after command shows command usage', () {
    schedulePub(args: ['get', '-h'],
        output: '''
          Get the current package's dependencies.

          Usage: pub get
          -h, --help            Print usage information for this command.
              --[no-]offline    Use cached packages instead of accessing the network.
    ''');
  });

  integration('running pub with --help after a command with subcommands shows '
              'command usage', () {
    schedulePub(args: ['cache', '--help'],
        output: '''
          Work with the system cache.

          Usage: pub cache <subcommand>
          -h, --help    Print usage information for this command.

          Available subcommands:
            add   Install a package.
     ''');
  });


  integration('running pub with just --version displays version', () {
    schedulePub(args: ['--version'], output: VERSION_STRING);
  });

  integration('an unknown command displays an error message', () {
    schedulePub(args: ['quylthulg'],
        error: '''
        Could not find a command named "quylthulg".

        Available commands:
          build      Apply transformers to build a package.
          cache      Work with the system cache.
          deps       Print package dependencies.
          get        Get the current package's dependencies.
          help       Display help information for Pub.
          publish    Publish the current package to pub.dartlang.org.
          serve      Run a local web development server.
          upgrade    Upgrade the current package's dependencies to latest versions.
          uploader   Manage uploaders for a package on pub.dartlang.org.
          version    Print pub version.
        ''',
        exitCode: exit_codes.USAGE);
  });

  integration('an unknown subcommand displays an error message', () {
    schedulePub(args: ['cache', 'quylthulg'],
        error: '''
        Could not find a subcommand named "quylthulg" for "pub cache".

        Usage: pub cache <subcommand>
        -h, --help    Print usage information for this command.

        Available subcommands:
          add   Install a package.
        ''',
        exitCode: exit_codes.USAGE);
  });

  integration('an unknown option displays an error message', () {
    schedulePub(args: ['--blorf'],
        error: '''
        Could not find an option named "blorf".
        Run "pub help" to see available options.
        ''',
        exitCode: exit_codes.USAGE);
  });

  integration('an unknown command option displays an error message', () {
    // TODO(rnystrom): When pub has command-specific options, a more precise
    // error message would be good here.
    schedulePub(args: ['version', '--blorf'],
        error: '''
        Could not find an option named "blorf".
        Run "pub help" to see available options.
        ''',
        exitCode: exit_codes.USAGE);
  });

  integration('an unexpected argument displays an error message', () {
    schedulePub(args: ['version', 'unexpected'],
        error: '''
        Command "version" does not take any arguments.

        Usage: pub version
         -h, --help    Print usage information for this command.
        ''',
        exitCode: exit_codes.USAGE);
  });

  integration('a missing subcommand displays an error message', () {
    schedulePub(args: ['cache'],
        error: '''
        Missing subcommand for "pub cache".

        Usage: pub cache <subcommand>
        -h, --help    Print usage information for this command.

        Available subcommands:
          add   Install a package.
        ''',
        exitCode: exit_codes.USAGE);
  });

  group('help', () {
    integration('shows global help if no command is given', () {
      schedulePub(args: ['help'], output: USAGE_STRING);
    });

    integration('shows help for a command', () {
      schedulePub(args: ['help', 'get'],
          output: '''
            Get the current package's dependencies.

            Usage: pub get
            -h, --help            Print usage information for this command.
                --[no-]offline    Use cached packages instead of accessing the network.
            ''');
    });

    integration('shows help for a command', () {
      schedulePub(args: ['help', 'publish'],
          output: '''
            Publish the current package to pub.dartlang.org.

            Usage: pub publish [options]
            -h, --help       Print usage information for this command.
            -n, --dry-run    Validate but do not publish the package.
            -f, --force      Publish without confirmation if there are no errors.
                --server     The package server to which to upload this package.
                             (defaults to "https://pub.dartlang.org")
            ''');
    });

    integration('shows non-truncated help', () {
      schedulePub(args: ['help', 'serve'],
          output: '''
            Run a local web development server.

            By default, this serves "web/" and "test/", but an explicit list of
            directories to serve can be provided as well.

            Usage: pub serve [directories...]
            -h, --help               Print usage information for this command.
                --mode               Mode to run transformers in.
                                     (defaults to "debug")

                --all                Use all default source directories.
                --hostname           The hostname to listen on.
                                     (defaults to "localhost")

                --port               The base port to listen on.
                                     (defaults to "8080")

                --[no-]dart2js       Compile Dart to JavaScript.
                                     (defaults to on)

                --[no-]force-poll    Force the use of a polling filesystem watcher.
            ''');
    });

    integration('shows help for a subcommand', () {
      schedulePub(args: ['help', 'cache', 'list'],
          output: '''
            List packages in the system cache.

            Usage: pub cache list
            -h, --help    Print usage information for this command.
            ''');
    });

    integration('an unknown help command displays an error message', () {
      schedulePub(args: ['help', 'quylthulg'],
          error: '''
            Could not find a command named "quylthulg".

            Available commands:
              build      Apply transformers to build a package.
              cache      Work with the system cache.
              deps       Print package dependencies.
              get        Get the current package's dependencies.
              help       Display help information for Pub.
              publish    Publish the current package to pub.dartlang.org.
              serve      Run a local web development server.
              upgrade    Upgrade the current package's dependencies to latest versions.
              uploader   Manage uploaders for a package on pub.dartlang.org.
              version    Print pub version.
            ''',
            exitCode: exit_codes.USAGE);
    });

    integration('an unknown help subcommand displays an error message', () {
      schedulePub(args: ['help', 'cache', 'quylthulg'],
          error: '''
            Could not find a subcommand named "quylthulg" for "pub cache".

            Usage: pub cache <subcommand>
            -h, --help    Print usage information for this command.

            Available subcommands:
              add   Install a package.
            ''',
            exitCode: exit_codes.USAGE);
    });

    integration('an unexpected help subcommand displays an error message', () {
      schedulePub(args: ['help', 'version', 'badsubcommand'],
          error: '''
            Command "pub version" does not expect a subcommand.

            Usage: pub version
            -h, --help    Print usage information for this command.
            ''',
            exitCode: exit_codes.USAGE);
    });
  });

  group('version', () {
    integration('displays the current version', () {
      schedulePub(args: ['version'], output: VERSION_STRING);
    });
  });
}

// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub.command.run;

import 'dart:async';

import 'package:barback/barback.dart';
import 'package:path/path.dart' as p;

import '../command.dart';
import '../executable.dart';
import '../io.dart';
import '../utils.dart';

/// Handles the `run` pub command.
class RunCommand extends PubCommand {
  String get name => "run";
  String get description =>
      "Run an executable from a package.\n"
          "NOTE: We are currently optimizing this command's startup time.";
  String get invocation => "pub run <executable> [args...]";
  bool get allowTrailingOptions => false;

  RunCommand() {
    argParser.addOption(
        "mode",
        help: 'Mode to run transformers in.\n'
            '(defaults to "release" for dependencies, "debug" for ' 'entrypoint)');
  }

  Future run() {
    final completer0 = new Completer();
    scheduleMicrotask(() {
      try {
        join0() {
          var package = entrypoint.root.name;
          var executable = argResults.rest[0];
          var args = argResults.rest.skip(1).toList();
          join1() {
            var mode;
            join2() {
              new Future.value(
                  runExecutable(entrypoint, package, executable, args, mode: mode)).then((x0) {
                try {
                  var exitCode = x0;
                  new Future.value(flushThenExit(exitCode)).then((x1) {
                    try {
                      x1;
                      completer0.complete();
                    } catch (e0, s0) {
                      completer0.completeError(e0, s0);
                    }
                  }, onError: completer0.completeError);
                } catch (e1, s1) {
                  completer0.completeError(e1, s1);
                }
              }, onError: completer0.completeError);
            }
            if (argResults['mode'] != null) {
              mode = new BarbackMode(argResults['mode']);
              join2();
            } else {
              join3() {
                join2();
              }
              if (package == entrypoint.root.name) {
                mode = BarbackMode.DEBUG;
                join3();
              } else {
                mode = BarbackMode.RELEASE;
                join3();
              }
            }
          }
          if (executable.contains(":")) {
            var components = split1(executable, ":");
            package = components[0];
            executable = components[1];
            join4() {
              join1();
            }
            if (p.split(executable).length > 1) {
              usageException(
                  "Cannot run an executable in a subdirectory of a " + "dependency.");
              join4();
            } else {
              join4();
            }
          } else {
            join1();
          }
        }
        if (argResults.rest.isEmpty) {
          usageException("Must specify an executable to run.");
          join0();
        } else {
          join0();
        }
      } catch (e, s) {
        completer0.completeError(e, s);
      }
    });
    return completer0.future;
  }
}

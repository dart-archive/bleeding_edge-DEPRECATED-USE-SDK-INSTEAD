// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub.command.global_run;

import 'dart:async';

import 'package:barback/barback.dart';
import 'package:path/path.dart' as p;

import '../command.dart';
import '../io.dart';
import '../utils.dart';

/// Handles the `global run` pub command.
class GlobalRunCommand extends PubCommand {
  String get name => "run";
  String get description =>
      "Run an executable from a globally activated package.\n"
          "NOTE: We are currently optimizing this command's startup time.";
  String get invocation => "pub global run <package>:<executable> [args...]";
  bool get allowTrailingOptions => false;

  /// The mode for barback transformers.
  BarbackMode get mode => new BarbackMode(argResults["mode"]);

  GlobalRunCommand() {
    argParser.addOption(
        "mode",
        defaultsTo: "release",
        help: 'Mode to run transformers in.');
  }

  Future run() {
    final completer0 = new Completer();
    scheduleMicrotask(() {
      try {
        join0() {
          var package;
          var executable = argResults.rest[0];
          join1() {
            var args = argResults.rest.skip(1).toList();
            join2() {
              new Future.value(
                  globals.runExecutable(package, executable, args, mode: mode)).then((x0) {
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
            if (p.split(executable).length > 1) {
              usageException(
                  'Cannot run an executable in a subdirectory of a global ' + 'package.');
              join2();
            } else {
              join2();
            }
          }
          if (executable.contains(":")) {
            var parts = split1(executable, ":");
            package = parts[0];
            executable = parts[1];
            join1();
          } else {
            package = executable;
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

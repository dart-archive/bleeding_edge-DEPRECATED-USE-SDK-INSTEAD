// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dcat;

import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:args/args.dart';

const LINE_NUMBER = 'line-number';
var NEWLINE = '\n';

ArgResults argResults;

/**
 * Simple implementation of the *nix cat utility.
 *
 * Usage: dart dcat.dart [-n] patterns [files]
 *
 * `dcat` reads `files` sequentially, writing them to standard output. The
 * file operands are processed in command-line order.
 * If `files` is absent, `dcat` reads from the standard input until `EOF`.
 *
 * Unlike the *nix `cat`, `dcat` does not support single dash ('-') arguments.
 */
Future dcat(List<String> paths, bool showLineNumbers) {
  if (paths.isEmpty) {
    // No files provided as arguments. Read from stdin and print each line.
    return stdin.pipe(stdout);
  } else {
    // `Future.forEach` asynchronously runs the callback provided on
    // each `path`. `forEach` runs the callback for each element in order,
    // moving to the next element only when the Future returned by the callback
    // completes.
    return Future.forEach(paths, (path) {
      int lineNumber = 1;
      Stream<List<int>> stream = new File(path).openRead();

      // Transform the stream using a `StreamTransformer`. The transformers
      // used here convert the data to UTF8 and split string values into
      // individual lines.
      return stream
          .transform(UTF8.decoder)
          .transform(const LineSplitter())
          .listen((line) {
            if (showLineNumbers) {
              stdout.write('${lineNumber++} ');
            }
            stdout.writeln(line);
          }).asFuture().catchError((_) => _handleError(path));
    });
  }
}

_handleError(String path) {
  FileSystemEntity.isDirectory(path).then((isDir) {
    if (isDir) {
      print('error: $path is a directory');
    } else {
      print('error: $path not found');
    }
  });
}

void main(List<String> arguments) {
  final parser = new ArgParser()
      ..addFlag(LINE_NUMBER, negatable: false, abbr: 'n');

  argResults = parser.parse(arguments);
  List<String> paths = argResults.rest;

  dcat(paths, argResults[LINE_NUMBER]);
}

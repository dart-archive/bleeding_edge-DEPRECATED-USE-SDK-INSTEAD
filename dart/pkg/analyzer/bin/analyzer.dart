#!/usr/bin/env dart
// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** The entry point for the analyzer. */
library analyzer;

import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:analyzer/options.dart';
import 'package:analyzer/src/analyzer_impl.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/error.dart';
import 'package:analyzer/src/generated/interner.dart';
import 'package:analyzer/src/generated/java_core.dart' show JavaSystem;
import 'package:analyzer/src/generated/java_engine.dart';
import 'package:analyzer/src/generated/utilities_general.dart';

void main(List<String> args) {
  StringUtilities.INTERNER = new MappedInterner();
  CommandLineOptions options = CommandLineOptions.parse(args);
  if (options.shouldBatch) {
    BatchRunner.runAsBatch(args, (List<String> args) {
      CommandLineOptions options = CommandLineOptions.parse(args);
      return _analyzeAll(options);
    });
  } else {
    _analyzeAll(options);
  }
}

_analyzeAll(CommandLineOptions options) {
  if (!options.machineFormat) {
    stdout.writeln("Analyzing ${options.sourceFiles}...");
  }
  ErrorSeverity allResult = ErrorSeverity.NONE;
  for (String sourcePath in options.sourceFiles) {
    sourcePath = sourcePath.trim();
    // check that file exists
    if (!new File(sourcePath).existsSync()) {
      print('File not found: $sourcePath');
      exitCode = ErrorSeverity.ERROR.ordinal;
      // fail fast; don't analyze more files
      return ErrorSeverity.ERROR;
    }
    // check that file is Dart file
    if (!AnalysisEngine.isDartFileName(sourcePath)) {
      print('$sourcePath is not a Dart file');
      exitCode = ErrorSeverity.ERROR.ordinal;
      // fail fast; don't analyze more files
      return ErrorSeverity.ERROR;
    }
    ErrorSeverity status = _runAnalyzer(options, sourcePath);
    allResult = allResult.max(status);
  }
  return allResult;
}

_runAnalyzer(CommandLineOptions options, String sourcePath) {
  if (options.warmPerf) {
    int startTime = JavaSystem.currentTimeMillis();
    AnalyzerImpl analyzer = new AnalyzerImpl(sourcePath, options, startTime);
    analyzer.analyzeSync(printMode: 2);

    for (int i = 0; i < 8; i++) {
      startTime = JavaSystem.currentTimeMillis();
      analyzer = new AnalyzerImpl(sourcePath, options, startTime);
      analyzer.analyzeSync(printMode: 0);
    }

    PerformanceTag.reset();
    startTime = JavaSystem.currentTimeMillis();
    analyzer = new AnalyzerImpl(sourcePath, options, startTime);
    return analyzer.analyzeSync();
  }
  int startTime = JavaSystem.currentTimeMillis();
  AnalyzerImpl analyzer = new AnalyzerImpl(sourcePath, options, startTime);
  var errorSeverity = analyzer.analyzeSync();
  if (errorSeverity == ErrorSeverity.ERROR) {
    exitCode = errorSeverity.ordinal;
  }
  if (options.warningsAreFatal && errorSeverity == ErrorSeverity.WARNING) {
    exitCode = errorSeverity.ordinal;
  }
  return errorSeverity;
}

typedef ErrorSeverity BatchRunnerHandler(List<String> args);

/// Provides a framework to read command line options from stdin and feed them to a callback.
class BatchRunner {
  /**
   * Run the tool in 'batch' mode, receiving command lines through stdin and returning pass/fail
   * status through stdout. This feature is intended for use in unit testing.
   */
  static void runAsBatch(List<String> sharedArgs, BatchRunnerHandler handler) {
    stdout.writeln('>>> BATCH START');
    Stopwatch stopwatch = new Stopwatch();
    stopwatch.start();
    int testsFailed = 0;
    int totalTests = 0;
    ErrorSeverity batchResult = ErrorSeverity.NONE;
    // read line from stdin
    Stream cmdLine =
        stdin.transform(UTF8.decoder).transform(new LineSplitter());
    cmdLine.listen((String line) {
      // may be finish
      if (line.isEmpty) {
        var time = stopwatch.elapsedMilliseconds;
        stdout.writeln(
            '>>> BATCH END (${totalTests - testsFailed}/$totalTests) ${time}ms');
        exitCode = batchResult.ordinal;
      }
      // prepare aruments
      var args;
      {
        var lineArgs = line.split(new RegExp('\\s+'));
        args = new List<String>();
        args.addAll(sharedArgs);
        args.addAll(lineArgs);
        args.remove('-b');
        args.remove('--batch');
      }
      // analyze single set of arguments
      try {
        totalTests++;
        ErrorSeverity result = handler(args);
        bool resultPass = result != ErrorSeverity.ERROR;
        if (!resultPass) {
          testsFailed++;
        }
        batchResult = batchResult.max(result);
        // Write stderr end token and flush.
        stderr.writeln('>>> EOF STDERR');
        String resultPassString = resultPass ? 'PASS' : 'FAIL';
        stdout.writeln(
            '>>> TEST $resultPassString ${stopwatch.elapsedMilliseconds}ms');
      } catch (e, stackTrace) {
        stderr.writeln(e);
        stderr.writeln(stackTrace);
        stderr.writeln('>>> EOF STDERR');
        stdout.writeln('>>> TEST CRASH');
      }
    });
  }
}

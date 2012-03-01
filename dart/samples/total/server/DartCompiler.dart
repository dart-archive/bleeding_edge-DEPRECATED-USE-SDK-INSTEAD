// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("total:dartcompiler");

#import('dart:io');

/**
 * A simple wrapper around the Dart compiler (runs frog).
 * TODO: automatically determine the exec path 
 */
class DartCompiler {
  final FROG_EXECS = const [
    '../../../xcodebuild/Release_ia32/dart-sdk/bin/frogc',
    '../../../out/Release_ia32/dart-sdk/bin/frogc'];

  String scriptName;
  String out = null;
  bool warningsAsErrors = false;
  bool enableTypeChecks = false;
  bool checkOnly = false;

  String _frogExecPath;

  DartCompiler(String this.scriptName) {
    for (var path in FROG_EXECS) {
      var f = new File(path);
      if (f.existsSync()) {
        _frogExecPath = path;
        break;
      }
    }
    if (_frogExecPath == null) {
      throw new Exception("Can't find frog on path: " + FROG_EXECS);
    }
  }

  void compile(void callback(int exitCode, String errorOutput)) {
    List<String> args = new List<String>();
    
    args.add("--compile-only");
    
    if (out != null) {
      args.add('--out=' + out);
    }
    if (warningsAsErrors) {
      args.add('--warnings_as_errors');
    }
    if (enableTypeChecks) {
      args.add('--enable_type_checks');
    }
    if (checkOnly) {
      args.add('--check-only');
    }
   

    args.add(scriptName);

    Process compiler = new Process.start(_frogExecPath, args);
    StringBuffer messages = new StringBuffer();
    compiler.onExit = (int status) {
      compiler.close();
      callback(status, messages.toString());
    };

    compiler.stdout.onData = () => _readAll(compiler.stdout, messages);
    compiler.stderr.onData = () => _readAll(compiler.stderr, messages);
  }
}

void _readAll(InputStream input, StringBuffer output) {
  while (input.available() != 0) {
    output.add(new String.fromCharCodes(input.read()));
  }
}


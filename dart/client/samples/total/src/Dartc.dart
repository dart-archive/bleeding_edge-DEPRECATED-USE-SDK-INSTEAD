// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("total:dartc");

/**
 * A simple wrapper around dartc.
 * TODO: automatically determine the exec path to dartc
 * TODO: prevent ANSI colors from being exposed to the user
 */
class Dartc {
  final String DARTC_EXEC_PATH = '../../../../compiler/out/Release_ia32/dartc';

  String scriptName;
  String work = null;
  bool wError = false;
  bool fatalTypeErrors = false;
  bool optimize = false;
  bool checkOnly = false;

  Dartc(String this.scriptName);

  void compile(void callback(int exitCode, String errorOutput)) {
    List<String> args = new List<String>();
    if (work != null) {
      args.add('--work');
      args.add('${work}');
    }
    if (wError) {
      args.add('-Werror');
    }
    if (fatalTypeErrors) {
      args.add('-fatal-type-errors');
    }
    if (optimize) {
      args.add('-optimize');
    }
    if (checkOnly) {
      args.add('-check-only');
    }
    args.add(scriptName);

    Process compiler = new Process(DARTC_EXEC_PATH, args);

    compiler.setExitHandler((int status) {
        StringBuffer messages = new StringBuffer();
        if(status != 0) {
          // TODO(rchandia) increase read size when stream handling works better
          int BUFSIZE = 1;

          _readAll(false, compiler.stdoutStream, new List<int>(BUFSIZE), messages);
          _readAll(false, compiler.stderrStream, new List<int>(BUFSIZE), messages);
        }
        compiler.close();
        callback(status, messages.toString());
      });

    compiler.start();
  }
}

void _readAll(bool fromCallback, InputStream input, List<int> buffer, StringBuffer output) {
  if (fromCallback) {
    output.add(String.fromCharCodes(buffer));
  }
  while (input.read(buffer, 0, buffer.length, readAll(true, input, buffer, output))) {
    output.add(String.fromCharCodes(buffer));
  }
}


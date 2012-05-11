#!/usr/bin/env dart
// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("total:runner");

#import('dart:io');

typedef void ExitCallback(int status, String exitString);

void main() {
  String SERVER_EXEC_PATH = './TotalServer.dart';
  final int RESTART_STATUS = 42;
  final int EXIT_STATUS = 0;

  void keepServerRunning(int status, ServerRunner runner) {
    switch (status) {
      case RESTART_STATUS:
        runner.run(keepServerRunning);
        break;
      case EXIT_STATUS:
        break;
      default:
        print("ERROR: exiting due to unknown condition. Exit status: $status.");
        break;
    }
  }

  new ServerRunner(SERVER_EXEC_PATH).run(keepServerRunning);
}

class ServerRunner {
  String _serverMain;

  static final DART_EXECS = const <String>[
      '../../../xcodebuild/Release_ia32/dart',
      '../../../out/Release_ia32/dart'];
  static final CR = 0x0d;
  static final LF = 0x0a;

  ServerRunner(String this._serverMain);

  void run(ExitCallback exitCallback) {
    var foundExec;
    for (String dartExec in DART_EXECS) {
      var file = new File(dartExec);
      if (file.existsSync()) {
        foundExec = dartExec;
      }
    }
    if (foundExec == null) {
      print("No executable found on DART_EXECS:\n" + DART_EXECS);
      exitCallback(1, this);
      return;
    }
    Process dart = Process.start(foundExec, [_serverMain]);

    dart.onExit = (int status) {
      dart.close();
      exitCallback(status, this);
    };

    dart.stdout.onData = () => readMore(dart.stdout, new StringBuffer());
    dart.stderr.onData = () => readMore(dart.stderr, new StringBuffer());
  }

  void readMore(InputStream i, StringBuffer readSoFar) {
    while(i.available() != 0) {
      processBuffer(i.read(), readSoFar);
    }
  }

  void processBuffer(List<int> buf, StringBuffer readSoFar) {
    buf.forEach((int i) {
        if (i != CR && i != LF) {
          readSoFar.add(new String.fromCharCodes([i]));
        } else {
          String line = readSoFar.toString();
          readSoFar.clear();
          if (line.length != 0) {
            print(line);
          }
        }
      });
  }
}

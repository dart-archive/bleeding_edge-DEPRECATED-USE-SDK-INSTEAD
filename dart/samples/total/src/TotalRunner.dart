#!/usr/bin/env dart
// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("total:runner");

typedef void ExitCallback(int status, String exitString);

void main() {
  String SERVER_EXEC_PATH = './TotalServer.dart';
  String RESTART = "GRACEFUL RESTART!!";
  String EXIT = "GRACEFUL EXIT!!";

  void keepServerRunning(int status, ServerRunner runner) {
    switch (runner.lastExitString) {
      case RESTART:
        runner.run(keepServerRunning);
        break;
      case EXIT:
        break;
      default:
        print("ERROR: exiting due to unknown condition. Exit status: $status."
              + " Exit string: '${runner.lastExitString}'");
        break;
    }
  }

  new ServerRunner(SERVER_EXEC_PATH, [RESTART, EXIT]).run(keepServerRunning);
}

class ServerRunner {
  List<String> _exitStrings;
  String _serverMain;

  // TODO: fix this to use a bigger buffer when streams work better
  int _BUFSIZE = 1;

  String lastExitString;

  // TODO: Release or Debug? We should be able to find automatically
  static final DART_EXEC_PATH = '../../../runtime/out/Release_ia32/dart_bin';
  static final CR = 0x0d;
  static final LF = 0x0a;

  ServerRunner(String this._serverMain, List<String> this._exitStrings);

  void run(ExitCallback exitCallback) {
    Process dart = new Process(DART_EXEC_PATH, [_serverMain]);

    dart.setExitHandler((int status) {
        dart.close();
        exitCallback(status, this);
      });

    dart.start();

    readMore(false, dart.stdoutStream, new List<int>(_BUFSIZE), new StringBuffer());
    readMore(false, dart.stderrStream, new List<int>(_BUFSIZE), new StringBuffer());
  }

  void readMore(bool fromCallback, InputStream i, List<int> buf, StringBuffer readSoFar) {
    if (fromCallback) {
      processBuffer(buf, readSoFar);
    }
    while(i.read(buf, 0, buf.length, () => readMore(true, i, buf, readSoFar))) {
      processBuffer(buf, readSoFar);
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
            _updateLastExitString(line);
          }
        }
      });
  }

  void _updateLastExitString(String line) {
    _exitStrings.some((String e) {
        if (line.contains(e, 0)) {
          lastExitString = e;
          return true;
        } else {
          return false;
        }
      });
  }

}

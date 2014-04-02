// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

patch class _WindowsCodePageDecoder {
  /* patch */ static String _decodeBytes(List<int> bytes)
      native "SystemEncodingToString";
}


patch class _WindowsCodePageEncoder {
  /* patch */ static List<int> _encodeString(String string)
      native "StringToSystemEncoding";
}


patch class Process {
  /* patch */ static Future<Process> start(
      String executable,
      List<String> arguments,
      {String workingDirectory,
       Map<String, String> environment,
       bool includeParentEnvironment: true,
       bool runInShell: false}) {
    _ProcessImpl process = new _ProcessImpl(executable,
                                            arguments,
                                            workingDirectory,
                                            environment,
                                            includeParentEnvironment,
                                            runInShell);
    return process._start();
  }

  /* patch */ static Future<ProcessResult> run(
      String executable,
      List<String> arguments,
      {String workingDirectory,
       Map<String, String> environment,
       bool includeParentEnvironment: true,
       bool runInShell: false,
       Encoding stdoutEncoding: SYSTEM_ENCODING,
       Encoding stderrEncoding: SYSTEM_ENCODING}) {
    return _runNonInteractiveProcess(executable,
                                     arguments,
                                     workingDirectory,
                                     environment,
                                     includeParentEnvironment,
                                     runInShell,
                                     stdoutEncoding,
                                     stderrEncoding);
  }

  /* patch */ static ProcessResult runSync(
      String executable,
      List<String> arguments,
      {String workingDirectory,
       Map<String, String> environment,
       bool includeParentEnvironment: true,
       bool runInShell: false,
       Encoding stdoutEncoding: SYSTEM_ENCODING,
       Encoding stderrEncoding: SYSTEM_ENCODING}) {
    return _runNonInteractiveProcessSync(executable,
                                         arguments,
                                         workingDirectory,
                                         environment,
                                         includeParentEnvironment,
                                         runInShell,
                                         stdoutEncoding,
                                         stderrEncoding);
  }
}


List<_SignalController> _signalControllers = new List(32);


class _SignalController {
  final ProcessSignal signal;

  StreamController _controller;
  var _id;

  _SignalController(this.signal) {
    _controller = new StreamController.broadcast(
        onListen: _listen,
        onCancel: _cancel);
  }

  Stream<ProcessSignal> get stream => _controller.stream;

  void _listen() {
    var id = _setSignalHandler(signal._signalNumber);
    if (id is! int) {
      _controller.addError(
          new SignalException("Failed to listen for $signal", id));
      return;
    }
    _id = id;
    var socket = new _RawSocket(new _NativeSocket.watch(id));
    socket.listen((event) {
      if (event == RawSocketEvent.READ) {
        var bytes = socket.read();
        for (int i = 0; i < bytes.length; i++) {
          _controller.add(signal);
        }
      }
    });
  }

  void _cancel() {
    if (_id != null) {
      _clearSignalHandler(signal._signalNumber);
      _id = null;
    }
  }

  /* patch */ static _setSignalHandler(int signal)
      native "Process_SetSignalHandler";
  /* patch */ static int _clearSignalHandler(int signal)
      native "Process_ClearSignalHandler";
}


patch class _ProcessUtils {
  /* patch */ static void _exit(int status) native "Process_Exit";
  /* patch */ static void _setExitCode(int status)
      native "Process_SetExitCode";
  /* patch */ static int _getExitCode() native "Process_GetExitCode";
  /* patch */ static void _sleep(int millis) native "Process_Sleep";
  /* patch */ static int _pid(Process process) native "Process_Pid";
  /* patch */ static Stream<ProcessSignal> _watchSignal(ProcessSignal signal) {
    if (signal != ProcessSignal.SIGHUP &&
        signal != ProcessSignal.SIGINT &&
        signal != ProcessSignal.SIGTERM &&
        (Platform.isWindows ||
         (signal != ProcessSignal.SIGUSR1 &&
          signal != ProcessSignal.SIGUSR2 &&
          signal != ProcessSignal.SIGWINCH))) {
      throw new SignalException(
          "Listening for signal $signal is not supported");
    }
    if (_signalControllers[signal._signalNumber] == null) {
      _signalControllers[signal._signalNumber] = new _SignalController(signal);
    }
    return _signalControllers[signal._signalNumber].stream;
  }
}


class _ProcessStartStatus {
  int _errorCode;  // Set to OS error code if process start failed.
  String _errorMessage;  // Set to OS error message if process start failed.
}


class _ProcessImpl extends NativeFieldWrapperClass1 implements Process {
  _ProcessImpl(String path,
               List<String> arguments,
               String this._workingDirectory,
               Map<String, String> environment,
               bool includeParentEnvironment,
               bool runInShell) {
    runInShell = identical(runInShell, true);
    if (runInShell) {
      arguments = _getShellArguments(path, arguments);
      path = _getShellCommand();
    }

    if (path is !String) {
      throw new ArgumentError("Path is not a String: $path");
    }
    _path = path;

    if (arguments is !List) {
      throw new ArgumentError("Arguments is not a List: $arguments");
    }
    int len = arguments.length;
    _arguments = new List<String>(len);
    for (int i = 0; i < len; i++) {
      var arg = arguments[i];
      if (arg is !String) {
        throw new ArgumentError("Non-string argument: $arg");
      }
      _arguments[i] = arguments[i];
      if (Platform.operatingSystem == 'windows') {
        _arguments[i] = _windowsArgumentEscape(_arguments[i]);
      }
    }

    if (_workingDirectory != null && _workingDirectory is !String) {
      throw new ArgumentError(
          "WorkingDirectory is not a String: $_workingDirectory");
    }

    _environment = [];
    if (environment == null) {
      environment = {};
    }
    if (environment is !Map) {
      throw new ArgumentError("Environment is not a map: $environment");
    }
    if (identical(true, includeParentEnvironment)) {
      environment = new Map.from(Platform.environment)..addAll(environment);
    }
    environment.forEach((key, value) {
      if (key is !String || value is !String) {
        throw new ArgumentError(
            "Environment key or value is not a string: ($key, $value)");
      }
      _environment.add('$key=$value');
    });

    // stdin going to process.
    _stdin = new _StdSink(new _Socket._writePipe());
    // stdout coming from process.
    _stdout = new _StdStream(new _Socket._readPipe());
    // stderr coming from process.
    _stderr = new _StdStream(new _Socket._readPipe());
    _exitHandler = new _Socket._readPipe();
    _ended = false;
    _started = false;
  }

  static String _getShellCommand() {
    if (Platform.operatingSystem == 'windows') {
      return 'cmd.exe';
    }
    return '/bin/sh';
  }

  static List<String> _getShellArguments(String executable,
                                         List<String> arguments) {
    List<String> shellArguments = [];
    if (Platform.operatingSystem == 'windows') {
      shellArguments.add('/c');
      shellArguments.add(executable);
      for (var arg in arguments) {
        shellArguments.add(arg);
      }
    } else {
      var commandLine = new StringBuffer();
      executable = executable.replaceAll("'", "'\"'\"'");
      commandLine.write("'$executable'");
      shellArguments.add("-c");
      for (var arg in arguments) {
        arg = arg.replaceAll("'", "'\"'\"'");
        commandLine.write(" '$arg'");
      }
      shellArguments.add(commandLine.toString());
    }
    return shellArguments;
  }

  String _windowsArgumentEscape(String argument) {
    var result = argument;
    if (argument.contains('\t') ||
        argument.contains(' ') ||
        argument.contains('"')) {
      // Produce something that the C runtime on Windows will parse
      // back as this string.

      // Replace any number of '\' followed by '"' with
      // twice as many '\' followed by '\"'.
      var backslash = '\\'.codeUnitAt(0);
      var sb = new StringBuffer();
      var nextPos = 0;
      var quotePos = argument.indexOf('"', nextPos);
      while (quotePos != -1) {
        var numBackslash = 0;
        var pos = quotePos - 1;
        while (pos >= 0 && argument.codeUnitAt(pos) == backslash) {
          numBackslash++;
          pos--;
        }
        sb.write(argument.substring(nextPos, quotePos - numBackslash));
        for (var i = 0; i < numBackslash; i++) {
          sb.write(r'\\');
        }
        sb.write(r'\"');
        nextPos = quotePos + 1;
        quotePos = argument.indexOf('"', nextPos);
      }
      sb.write(argument.substring(nextPos, argument.length));
      result = sb.toString();

      // Add '"' at the beginning and end and replace all '\' at
      // the end with two '\'.
      sb = new StringBuffer('"');
      sb.write(result);
      nextPos = argument.length - 1;
      while (argument.codeUnitAt(nextPos) == backslash) {
        sb.write('\\');
        nextPos--;
      }
      sb.write('"');
      result = sb.toString();
    }

    return result;
  }

  int _intFromBytes(List<int> bytes, int offset) {
    return (bytes[offset] +
            (bytes[offset + 1] << 8) +
            (bytes[offset + 2] << 16) +
            (bytes[offset + 3] << 24));
  }

  Future<Process> _start() {
    var completer = new Completer();
    // TODO(ager): Make the actual process starting really async instead of
    // simulating it with a timer.
    Timer.run(() {
      var status = new _ProcessStartStatus();
      bool success = _startNative(_path,
                                  _arguments,
                                  _workingDirectory,
                                  _environment,
                                  _stdin._sink._nativeSocket,
                                  _stdout._stream._nativeSocket,
                                  _stderr._stream._nativeSocket,
                                  _exitHandler._nativeSocket,
                                  status);
      if (!success) {
        completer.completeError(
            new ProcessException(_path,
                                 _arguments,
                                 status._errorMessage,
                                 status._errorCode));
        return;
      }
      _started = true;

      // Setup an exit handler to handle internal cleanup and possible
      // callback when a process terminates.
      int exitDataRead = 0;
      final int EXIT_DATA_SIZE = 8;
      List<int> exitDataBuffer = new List<int>(EXIT_DATA_SIZE);
      _exitHandler.listen((data) {

        int exitCode(List<int> ints) {
          var code = _intFromBytes(ints, 0);
          var negative = _intFromBytes(ints, 4);
          assert(negative == 0 || negative == 1);
          return (negative == 0) ? code : -code;
        }

        void handleExit() {
          _ended = true;
          _exitCode.complete(exitCode(exitDataBuffer));
          // Kill stdin, helping hand if the user forgot to do it.
          _stdin._sink.destroy();
        }

        exitDataBuffer.setRange(exitDataRead, exitDataRead + data.length, data);
        exitDataRead += data.length;
        if (exitDataRead == EXIT_DATA_SIZE) {
          handleExit();
        }
      });

      completer.complete(this);
    });
    return completer.future;
  }

  ProcessResult _runAndWait(Encoding stdoutEncoding,
                            Encoding stderrEncoding) {
    var status = new _ProcessStartStatus();
    bool success = _startNative(_path,
                                _arguments,
                                _workingDirectory,
                                _environment,
                                _stdin._sink._nativeSocket,
                                _stdout._stream._nativeSocket,
                                _stderr._stream._nativeSocket,
                                _exitHandler._nativeSocket,
                                status);
    if (!success) {
      throw new ProcessException(_path,
                                 _arguments,
                                 status._errorMessage,
                                 status._errorCode);
    }

    var result = _wait(
        _stdin._sink._nativeSocket,
        _stdout._stream._nativeSocket,
        _stderr._stream._nativeSocket,
        _exitHandler._nativeSocket);

    getOutput(output, encoding) {
      if (encoding == null) return output;
      return encoding.decode(output);
    }

    return new _ProcessResult(
        result[0],
        result[1],
        getOutput(result[2], stdoutEncoding),
        getOutput(result[3], stderrEncoding));
  }

  bool _startNative(String path,
                    List<String> arguments,
                    String workingDirectory,
                    List<String> environment,
                    _NativeSocket stdin,
                    _NativeSocket stdout,
                    _NativeSocket stderr,
                    _NativeSocket exitHandler,
                    _ProcessStartStatus status) native "Process_Start";

  _wait(_NativeSocket stdin,
        _NativeSocket stdout,
        _NativeSocket stderr,
        _NativeSocket exitHandler) native "Process_Wait";

  Stream<List<int>> get stdout {
    return _stdout;
  }

  Stream<List<int>> get stderr {
    return _stderr;
  }

  IOSink get stdin {
    return _stdin;
  }

  Future<int> get exitCode => _exitCode.future;

  bool kill([ProcessSignal signal = ProcessSignal.SIGTERM]) {
    if (signal is! ProcessSignal) {
      throw new ArgumentError(
          "Argument 'signal' must be a ProcessSignal");
    }
    assert(_started);
    if (_ended) return false;
    return _kill(this, signal._signalNumber);
  }

  bool _kill(Process p, int signal) native "Process_Kill";

  int get pid => _ProcessUtils._pid(this);

  String _path;
  List<String> _arguments;
  String _workingDirectory;
  List<String> _environment;
  // Private methods of Socket are used by _in, _out, and _err.
  _StdSink _stdin;
  _StdStream _stdout;
  _StdStream _stderr;
  Socket _exitHandler;
  bool _ended;
  bool _started;
  final Completer<int> _exitCode = new Completer<int>();
}


// _NonInteractiveProcess is a wrapper around an interactive process
// that buffers output so it can be delivered when the process exits.
// _NonInteractiveProcess is used to implement the Process.run
// method.
Future<ProcessResult> _runNonInteractiveProcess(String path,
                                                List<String> arguments,
                                                String workingDirectory,
                                                Map<String, String> environment,
                                                bool includeParentEnvironment,
                                                bool runInShell,
                                                Encoding stdoutEncoding,
                                                Encoding stderrEncoding) {
  // Start the underlying process.
  return Process.start(path,
                       arguments,
                       workingDirectory: workingDirectory,
                       environment: environment,
                       includeParentEnvironment: includeParentEnvironment,
                       runInShell: runInShell).then((Process p) {
    int pid = p.pid;

    // Make sure the process stdin is closed.
    p.stdin.close();

    // Setup stdout and stderr handling.
    Future foldStream(Stream<List<int>> stream, Encoding encoding) {
      if (encoding == null) {
        return stream
            .fold(new BytesBuilder(), (builder, data) => builder..add(data))
            .then((builder) => builder.takeBytes());
      } else {
        return stream
            .transform(encoding.decoder)
            .fold(
                new StringBuffer(),
                (buf, data) {
                  buf.write(data);
                  return buf;
                })
            .then((sb) => sb.toString());
      }
    }

    Future stdout = foldStream(p.stdout, stdoutEncoding);
    Future stderr = foldStream(p.stderr, stderrEncoding);

    return Future.wait([p.exitCode, stdout, stderr]).then((result) {
      return new _ProcessResult(pid, result[0], result[1], result[2]);
    });
  });
}

ProcessResult _runNonInteractiveProcessSync(
      String executable,
      List<String> arguments,
      String workingDirectory,
      Map<String, String> environment,
      bool includeParentEnvironment,
      bool runInShell,
      Encoding stdoutEncoding,
      Encoding stderrEncoding) {
  var process = new _ProcessImpl(executable,
                                 arguments,
                                 workingDirectory,
                                 environment,
                                 includeParentEnvironment,
                                 runInShell);
  return process._runAndWait(stdoutEncoding, stderrEncoding);
}


class _ProcessResult implements ProcessResult {
  const _ProcessResult(int this.pid,
                       int this.exitCode,
                       this.stdout,
                       this.stderr);

  final int pid;
  final int exitCode;
  final stdout;
  final stderr;
}

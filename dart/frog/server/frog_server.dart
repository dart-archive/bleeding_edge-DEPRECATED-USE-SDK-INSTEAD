// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('frog_server');

#import('dart:io');
#import('../../lib/json/json.dart');
#import('../lang.dart');
#import('../file_system_vm.dart');
#import('../../lib/utf/utf.dart');

/// The server socket used to listen for incoming connections.
ServerSocket serverSocket;

initializeCompiler(String homedir) {
  final filesystem = new VMFileSystem();
  parseOptions(homedir, [null, null], filesystem);
  initializeWorld(filesystem);
}

/// Compiles the Dart script at the specified location and saves the file.
/// Request should look like:
///     {
///       "command": "compile",
///       "id": "anyStringOrNumber",
///       "input": "/Path/To/main_script.dart",
///       "output": "/Path/To/main_script.dart.js",
///     }
compileCommand(Map request, OutputStream output) {
  var id = request['id'];
  world.reset();
  options.dartScript = request['input'];
  options.outfile = request['output'];
  if (options.outfile == null) {
    options.checkOnly = true;
  }
  print('starting compile with id $id, ' +
      '${options.dartScript} -> ${options.outfile}');

  world.messageHandler = (String prefix, String message, SourceSpan span) {
    var jsonSpan;
    if (span == null) {
      // Any messages that are not associated with a file become associated with
      // the library file.
      jsonSpan = { 'file': request['input'], 'start': 0, 'end': 0,
          'line': 0, 'column': 0 };
    } else {
      jsonSpan = { 'file': span.file.filename, 'start': span.start, 'end': span.end,
          'line': span.line, 'column': span.column };
    }
    
    writeJson(output, {
      'kind': 'message',
      'id': id,
      'prefix': prefix,
      'message': message,
      'span': jsonSpan
    });
  };

  bool success = false;

  try {
    success = world.compileAndSave();
  } catch (var exc) {
    writeJson(output, {
      'kind': 'message',
      'id': id,
      'message': "compiler exception: ${exc}"
    });
  }

  writeJson(output, {
    'kind': 'done',
    'command': 'compile',
    'id': id,
    'result': success
  });
}

/// Writes the supplied JSON-serializable [obj] to the output stream.
writeJson(OutputStream output, obj) {
  var jsonBytes = encodeUtf8(JSON.stringify(obj));
  output.write(int32ToBigEndian(jsonBytes.length), copyBuffer:false);
  output.write(jsonBytes, copyBuffer:false);
}

List<int> int32ToBigEndian(int len) =>
  [(len >> 24) & 0x7F, (len >> 16) & 0xFF, (len >> 8) & 0xFF, len & 0xFF];

int bigEndianToInt32(List<int> bytes) =>
  (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];

/// Reads some JSON, and pops bytes that were read.
/// Returns `null` if we don't have enough bytes yet.
tryReadJson(List<int> bytes) {
  if (bytes.length < 4) return;
  int len = bigEndianToInt32(bytes);
  if (bytes.length < 4 + len) {
    print('info: wait for more data (got ${bytes.length}, need ${len + 4})');
    return null; // wait for more data
  }
  var jsonBytes = bytes.getRange(4, len);
  bytes.removeRange(0, len + 4);
  return JSON.parse(decodeUtf8(jsonBytes));
}

/// Tries to handle a request. This routine will remove any bytes it consumes.
/// If the data is incomplete it's expected to leave them in the list and
/// return.
handleRequest(List<int> bytes, Socket socket) {
  var request = tryReadJson(bytes);
  if (request == null) {
    return; // wait for more data
  }
  switch (request['command']) {
    // TODO(jmesserly): split "compile" and "newWorld" commands, add
    // a way to set options.
    case 'compile':
      compileCommand(request, socket.outputStream);
      break;
    case 'close':
      socket.close();
      break;
    default:
      print('info: unknown command "${request["command"]}"');
  }
}

/// Accepts an incoming socket connection
onConnect(Socket socket) {
  var bytes = new List<int>();
  socket.onData = () {
    var pos = bytes.length;
    var len = socket.available();
    bytes.insertRange(pos, len);
    socket.readList(bytes, pos, len);
    handleRequest(bytes, socket);
  };
  socket.onError = () => socket.close();
  socket.onClosed = () => socket.close();

  // Close the serverSocket - we only ever service one client.
  serverSocket.close();
}

/// This token is used by the editor to know when frogc has successfully come up.
final STARTUP_TOKEN = 'frog: accepting connections';

/// Initialize the server and start listening for requests. Needs hostname/ip
/// and port that it should listen on, and the Frog library directory.
ServerSocket startServer(String homedir, String host, int port) {
  // Initialize the compiler. Only need to happen once.
  initializeCompiler(homedir);
  serverSocket = new ServerSocket(host, port, 50);
  serverSocket.onConnection = onConnect;
  print('$STARTUP_TOKEN on $host:${serverSocket.port}');
  return serverSocket;
}

/// Main entry point for FrogServer.
main() {
  // TODO(jmesserly): until we have a way of getting the executable's path,
  // we'll run from current working directory.
  var homedir = new File('.').fullPathSync();

  var argv = new Options().arguments;
  var host = argv.length > 0 ? argv[0] : '127.0.0.1';
  var port = argv.length > 1 ? Math.parseInt(argv[1]) : 1236;
  startServer(homedir, host, port);
}

// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('net');
#import('node.dart');

class net native "require('net')" {
  static Server createServer([ServerConnectionListener connectionListener,
      Map options]) native;

  static Socket createConnection(int port, [String hostName,
      SocketConnectListener connectListener]) native;

  static Socket createUnixConnection(String path,
    [SocketConnectListener connectListener])
    native "return this.createConnection(path,connectListener)";

  static int isIP(String input) native;
  static bool isIPv4(String input) native;
  static bool isIPv6(String input) native;
}

typedef void ServerListeningListener();
typedef void ServerConnectionListener(Socket socket);
typedef void ServerCloseListener();
typedef void ServerErrorListener(Error e);

// TODO(jackpal): resolve name conflict with http.Server. Maybe both should go
// into their own libraries.

class Server implements EventEmitter native "require('net').Server" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  var _listeners(String key)
    native "return this.listeners(key);";
  
  // event 'listening'
  void emitListening()
    native "this.emit('listening');";
  void addListenerListening(ServerListeningListener listener)
    native "this.addListener('listening', listener);";
  void onListening(ServerListeningListener listener)
    native "this.on('listening', listener);";
  void onceListening(ServerListeningListener listener)
    native "this.once('listening', listener);";
  void removeListenerListening(ServerListeningListener listener)
    native "this.removeListener('listening', listener);";
  List<ServerListeningListener> listenersListening()
    => new _NativeListPrimitiveElement<ServerListeningListener>(
      _listeners('listening'));

  // event 'connection'
  void emitConnection()
    native "this.emit('connection');";
  void addListenerConnection(ServerConnectionListener listener)
    native "this.addListener('connection', listener);";
  void onConnection(ServerConnectionListener listener)
    native "this.on('connection', listener);";
  void onceConnection(ServerConnectionListener listener)
    native "this.once('connection', listener);";
  void removeListenerConnection(ServerConnectionListener listener)
    native "this.removeListener('connection', listener);";
  List<ServerConnectionListener> listenersConnection()
    => new _NativeListPrimitiveElement<ServerConnectionListener>(
      _listeners('connection'));
    
  // event 'close'
  void emitClose()
    native "this.emit('close');";
  void addListenerClose(ServerCloseListener listener)
    native "this.addListener('close', listener);";
  void onClose(ServerCloseListener listener)
    native "this.on('close', listener);";
  void onceClose(ServerCloseListener listener)
    native "this.once('close', listener);";
  void removeListenerClose(ServerCloseListener listener)
    native "this.removeListener('close', listener);";
  List<ServerCloseListener> listenersClose()
    => new _NativeListPrimitiveElement<ServerCloseListener>(
      _listeners('close'));
    
  // event 'error'
  void emitError()
    native "this.emit('error');";
  void addListenerError(ServerErrorListener listener)
    native "this.addListener('error', listener);";
  void onError(ServerErrorListener listener)
    native "this.on('error', listener);";
  void onceError(ServerErrorListener listener)
    native "this.once('error', listener);";
  void removeListenerError(ServerErrorListener listener)
    native "this.removeListener('error', listener);";
  List<ServerErrorListener> listenersError()
    => new _NativeListPrimitiveElement<ServerErrorListener>(
      _listeners('error'));
 
  void listen(int port, [String host,
      ServerListeningListener listeningListener]) native;
  void listenUnix(String path,
      [ServerListeningListener listeningListener])
    native "this.listen(path, listeningListener);";
  void pause([int msecs]) native;
  void close() native;
  String address() native;
  int maxConnections;
  int connections;
}

typedef void SocketConnectListener();
typedef void SocketTimeoutListener();
typedef void SocketCloseListener(bool had_error);

class Socket implements ReadWriteStream native "require('net').Socket" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  var _listeners(String key)
    native "return this.listeners(key);";

  // CommonStream
  
  // Error event
  void emitError(Error error)
    native "this.emit('error', error);";
  void addListenerError(StreamErrorListener listener)
    native "this.addListener('error', listener);";
  void onError(StreamErrorListener listener)
    native "this.on('error', listener);";
  void onceError(StreamErrorListener listener)
    native "this.once('error', listener);";
  void removeListenerError(StreamErrorListener listener)
    native "this.removeListener('error', listener);";
  List<StreamErrorListener> listenersError()
    => new _NativeListPrimitiveElement<StreamErrorListener>(
      _listeners('error'));

  // Close event
  void emitClose()
    native "this.emit('close');";
  void addListenerClose(StreamCloseListener listener)
    native "this.addListener('close', listener);";
  void onClose(StreamCloseListener listener)
    native "this.on('close', listener);";
  void onceClose(StreamCloseListener listener)
    native "this.once('close', listener);";
  void removeListenerClose(StreamCloseListener listener)
    native "this.removeListener('close', listener);";
  List<StreamCloseListener> listenersClose()
    => new _NativeListPrimitiveElement<StreamCloseListener>(
      _listeners('close'));

  // ReadableStream
  
  // Data event
  void emitData(var data)
    native "this.emit('data', data);";
  void addListenerData(ReadableStreamDataListener listener)
    native "this.addListener('data', listener);";
  void onData(ReadableStreamDataListener listener)
    native "this.on('data', listener);";
  void onceData(ReadableStreamDataListener listener)
    native "this.once('data', listener);";
  void removeListenerData(ReadableStreamDataListener listener)
    native "this.removeListener('data', listener);";
  List<ReadableStreamDataListener> listenersData()
    => new _NativeListPrimitiveElement<ReadableStreamDataListener>(
      _listeners('data'));

  // End event
  void emitEnd()
    native "this.emit('end');";
  void addListenerEnd(ReadableStreamEndListener listener)
    native "this.addListener('end', listener);";
  void onEnd(ReadableStreamEndListener listener)
    native "this.on('end', listener);";
  void onceEnd(ReadableStreamEndListener listener)
    native "this.once('end', listener);";
  void removeListenerEnd(ReadableStreamEndListener listener)
    native "this.removeListener('end', listener);";
  List<ReadableStreamEndListener> listenersEnd()
    => new _NativeListPrimitiveElement<ReadableStreamEndListener>(
      _listeners('end'));
  
  bool readable;
  void setEncoding(String encoding) native;
  void pause() native;
  void resume() native;
  void destroy() native;
  void destroySoon() native;
  WritableStream pipe(WritableStream destination, [Map options]) native;

  // WritableStream

  // Drain event
  void emitDrain()
    native "this.emit('drain');";
  void addListenerDrain(WritableStreamDrainListener listener)
    native "this.addListener('drain', listener);";
  void onDrain(WritableStreamDrainListener listener)
    native "this.on('drain', listener);";
  void onceDrain(WritableStreamDrainListener listener)
    native "this.once('drain', listener);";
  void removeListenerDrain(WritableStreamDrainListener listener)
    native "this.removeListener('drain', listener);";
  List<WritableStreamDrainListener> listenersDrain()
    => new _NativeListPrimitiveElement<WritableStreamDrainListener>(
      _listeners('drain'));

  // Pipe event
  void emitPipe(ReadableStream src)
    native "this.emit('pipe', src);";
  void addListenerPipe(WritableStreamPipeListener listener)
    native "this.addListener('pipe', listener);";
  void onPipe(WritableStreamPipeListener listener)
    native "this.on('pipe', listener);";
  void oncePipe(WritableStreamPipeListener listener)
    native "this.once('pipe', listener);";
  void removeListenerPipe(WritableStreamPipeListener listener)
    native "this.removeListener('pipe', listener);";
  List<WritableStreamPipeListener> listenersPipe()
    => new _NativeListPrimitiveElement<WritableStreamPipeListener>(
      _listeners('pipe'));

  bool writable;
  bool write(String string, [String encoding, int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
}

// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('http');
#import('node.dart');
#import('net.dart', prefix:'net');
#import('url.dart');

// module http

class http native "require('http')" {
  static Server createServer([ServerRequestListener listener]) native;
  static ClientRequest request(UrlObject options, void response(ServerResponse
      res))
      native;
  static ClientRequest get(UrlObject options, void response(ServerResponse res))
      native;
  static Agent globalAgent;
}

typedef void ServerRequestListener(
    ServerRequest request, ServerResponse response);
typedef void ServerConnectionListener(net.Socket socket);
typedef void ServerCloseListener();
typedef void ServerCheckContinueListener(
    ServerRequest request, ServerResponse response);
typedef void ServerUpgradeListener(
    ServerRequest request, net.Socket socket, Buffer head);
typedef void ServerClientErrorListener(Error exception);

class Server native "require('http').Server" {
  // Implement EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  _listeners(String key)
    native "return this.listeners(key);";
    
  // request event
  void emitRequest(ServerRequest request, ServerResponse response)
    native "this.emit('request');";
  void addListenerRequest(ServerRequestListener listener)
    native "this.addListener('request', listener);";
  void onRequest(ServerRequestListener listener)
    native "this.on('request', listener);";
  void onceRequest(ServerRequestListener listener)
    native "this.once('request', listener);";
  void removeListenerRequest(ServerRequestListener listener)
    native "this.removeListener('request', listener);";
  List<ServerRequestListener> listenersRequest()
    => _listeners('request');
  
  // connection event
  void emitConnection(net.Socket socket)
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
    => _listeners('connection');
  
  // close event
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
    => _listeners('close');
  
  // checkContinue event
  void emitCheckContinue(ServerRequest request, ServerResponse response)
    native "this.emit('checkContinue');";
  void addListenerCheckContinue(ServerCheckContinueListener listener)
    native "this.addListener('checkContinue', listener);";
  void onCheckContinue(ServerCheckContinueListener listener)
    native "this.on('checkContinue', listener);";
  void onceCheckContinue(ServerCheckContinueListener listener)
    native "this.once('checkContinue', listener);";
  void removeListenerCheckContinue(ServerCheckContinueListener listener)
    native "this.removeListener('checkContinue', listener);";
  List<ServerCheckContinueListener> listenersCheckContinue()
    => _listeners('checkContinue');
  
  // upgrade event
  void emitUpgrade(ServerRequest request, net.Socket socket, Buffer head)
    native "this.emit('upgrade');";
  void addListenerUpgrade(ServerUpgradeListener listener)
    native "this.addListener('upgrade', listener);";
  void onUpgrade(ServerUpgradeListener listener)
    native "this.on('upgrade', listener);";
  void onceUpgrade(ServerUpgradeListener listener)
    native "this.once('upgrade', listener);";
  void removeListenerUpgrade(ServerUpgradeListener listener)
    native "this.removeListener('upgrade', listener);";
  List<ServerUpgradeListener> listenersUpgrade()
    => _listeners('upgrade');
  
  // clientError event
  void emitClientError(Error exception)
    native "this.emit('clientError');";
  void addListenerClientError(ServerClientErrorListener listener)
    native "this.addListener('clientError', listener);";
  void onClientError(ServerClientErrorListener listener)
    native "this.on('clientError', listener);";
  void onceClientError(ServerClientErrorListener listener)
    native "this.once('clientError', listener);";
  void removeListenerClientError(ServerClientErrorListener listener)
    native "this.removeListener('clientError', listener);";
  List<ServerClientErrorListener> listenersClientError()
    => _listeners('clientError');

  void listen(int port, [String hostname, void callback()]) native;
  void listenUnix(String path, [void callback()]) native;
  void close() native;
}

typedef void ServerRequestDataListener(var chunk);
typedef void ServerRequestEndListener();
typedef void ServerRequestCloseListener();

class ServerRequest implements EventEmitter native
    "request('http').IncomingMessage" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  _listeners(String key)
    native "return this.listeners(key);";
    
  // data event
  void emitData(var chunk)
    native "this.emit('data');";
  void addListenerData(ServerRequestDataListener listener)
    native "this.addListener('data', listener);";
  void onData(ServerRequestDataListener listener)
    native "this.on('data', listener);";
  void onceData(ServerRequestDataListener listener)
    native "this.once('data', listener);";
  void removeListenerData(ServerRequestDataListener listener)
    native "this.removeListener('data', listener);";
  List<ServerRequestDataListener> listenersData()
    => _listeners('data');
  
  // end event
  void emitEnd()
    native "this.emit('end');";
  void addListenerEnd(ServerRequestEndListener listener)
    native "this.addListener('end', listener);";
  void onEnd(ServerRequestEndListener listener)
    native "this.on('end', listener);";
  void onceEnd(ServerRequestEndListener listener)
    native "this.once('end', listener);";
  void removeListenerEnd(ServerRequestEndListener listener)
    native "this.removeListener('end', listener);";
  List<ServerRequestEndListener> listenersEnd()
    => _listeners('end');
  
  // close event
  void emitClose()
    native "this.emit('close');";
  void addListenerClose(ServerRequestCloseListener listener)
    native "this.addListener('close', listener);";
  void onClose(ServerRequestCloseListener listener)
    native "this.on('close', listener);";
  void onceClose(ServerRequestCloseListener listener)
    native "this.once('close', listener);";
  void removeListenerClose(ServerRequestCloseListener listener)
    native "this.removeListener('close', listener);";
  List<ServerRequestCloseListener> listenersClose()
    => _listeners('close');
  
  final String method;
  final String url;
  Map<String, String> get headers()
    => new NativeMapPrimitiveValue<String>(_headers());
  var _headers() native "return this.headers;";
  final String httpVersion;

  void setEncoding([String encoding]) native;
  void pause() native;
  void resume() native;
  net.Socket connection;
}

class ServerResponse implements WritableStream native "http.ServerResponse" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  _listeners(String key)
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
    => _listeners('error');

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
    => _listeners('close');

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
    => _listeners('drain');
    
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
    => _listeners('pipe');
  

  void writeContinue() native;
  void writeHead(int statusCode, [String reasonPhrase, Map<String,String>
      headers])
    native;
  int statusCode;
  void setHeader(String name, String value) native;
  String getHeader(String name) native;
  void removeHeader(String name) native;
  

  void write(String data, [String encoding = 'utf8']) native;
  void writeBuffer(Buffer data)
    native "this.write(data);";
  
  void addTrailers(Map<String,String> headers) native;

  void end([String data, String encoding = 'utf8']) native;
}

class Agent native "require('http').Agent" {
  int maxSockets;
}

typedef void ClientRequestResponseListener(ClientResponse response);
typedef void ClientRequestSocketListener(net.Socket socket);
typedef void ClientRequestUpgradeListener(
    ClientResponse response, net.Socket socket, Buffer head);
typedef void ClientRequestContinueListener();

class ClientRequest implements WritableStream native
    "require('http').ClientRequest" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  _listeners(String key)
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
    => _listeners('error');

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
    => _listeners('close');

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
    => _listeners('drain');
    
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
    => _listeners('pipe');


  // ClientRequest specific events

  // response event
  void emitResponse(ClientResponse response)
    native "this.emit('response');";
  void addListenerResponse(ClientRequestResponseListener listener)
    native "this.addListener('response', listener);";
  void onResponse(ClientRequestResponseListener listener)
    native "this.on('response', listener);";
  void onceResponse(ClientRequestResponseListener listener)
    native "this.once('response', listener);";
  void removeListenerResponse(ClientRequestResponseListener listener)
    native "this.removeListener('response', listener);";
  List<ClientRequestResponseListener> listenersResponse()
    => _listeners('response');

  // socket event
  void emitSocket(net.Socket socket)
    native "this.emit('socket');";
  void addListenerSocket(ClientRequestSocketListener listener)
    native "this.addListener('socket', listener);";
  void onSocket(ClientRequestSocketListener listener)
    native "this.on('socket', listener);";
  void onceSocket(ClientRequestSocketListener listener)
    native "this.once('socket', listener);";
  void removeListenerSocket(ClientRequestSocketListener listener)
    native "this.removeListener('socket', listener);";
  List<ClientRequestSocketListener> listenersSocket()
    => _listeners('socket');

  // upgrade event
  void emitUpgrade(ClientResponse response, net.Socket socket, Buffer head)
    native "this.emit('upgrade');";
  void addListenerUpgrade(ClientRequestUpgradeListener listener)
    native "this.addListener('upgrade', listener);";
  void onUpgrade(ClientRequestUpgradeListener listener)
    native "this.on('upgrade', listener);";
  void onceUpgrade(ClientRequestUpgradeListener listener)
    native "this.once('upgrade', listener);";
  void removeListenerUpgrade(ClientRequestUpgradeListener listener)
    native "this.removeListener('upgrade', listener);";
  List<ClientRequestUpgradeListener> listenersUpgrade()
    => _listeners('upgrade');

  // continue event
  void emitContinue()
    native "this.emit('continue');";
  void addListenerContinue(ClientRequestContinueListener listener)
    native "this.addListener('continue', listener);";
  void onContinue(ClientRequestContinueListener listener)
    native "this.on('continue', listener);";
  void onceContinue(ClientRequestContinueListener listener)
    native "this.once('continue', listener);";
  void removeListenerContinue(ClientRequestContinueListener listener)
    native "this.removeListener('continue', listener);";
  List<ClientRequestContinueListener> listenersContinue()
    => _listeners('continue');

  // WritableStream methods and instance variables
  
  bool writable;
  bool write(String string, [String encoding='utf8', int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
  void destroy() native;
  void destroySoon() native;
  
  // ClientRequest methods
  void abort() native;
  void setTimeout(int timeout, void callback()) native;
  void setNoDelay([bool noDelay]) native;
  void setSocketKeepAlive([bool enable, int initialDelay]) native;
}

class ClientResponse implements ReadableStream native
    "require('http').ClientResponse" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  _listeners(String key)
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
    => _listeners('error');

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
    => _listeners('close');

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
  List<ReadableStreamDataListener> listenerData()
    => _listeners('data');
    
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
    => _listeners('end');
  
  // ReadableStream methods and instance variables
  
  bool readable;
  void setEncoding(String encoding) native;
  void pause() native;
  void resume() native;
  void destroy() native;
  void destroySoon() native;
  WritableStream pipe(WritableStream destination, [Map options]) native;
  
  // class specific methods and instance variables
  int statusCode;
  String httpVersion;
  int httpVersionMajor;
  int httpVersionMinor;
  Map<String, String> get headers()
    => new NativeMapPrimitiveValue<String>(_headers());
  var _headers() native "return this.headers;";
  Map<String, String> trailers()
      => new NativeMapPrimitiveValue<String>(_trailers());
  var _trailers() native "return this.trailers;";
}

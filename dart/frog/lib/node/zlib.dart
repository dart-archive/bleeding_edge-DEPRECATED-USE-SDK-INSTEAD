// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('zlib');
#import('node.dart');

// module zlib

class Zlib {
  
  int get Z_NO_FLUSH() => _zlib.Z_NO_FLUSH;
  
  int get Z_PARTIAL_FLUSH() => _zlib.Z_PARTIAL_FLUSH;
  int get Z_SYNC_FLUSH() => _zlib.Z_SYNC_FLUSH;
  int get Z_FULL_FLUSH() => _zlib.Z_FULL_FLUSH;
  int get Z_FINISH() => _zlib.Z_FINISH;
  int get Z_BLOCK() => _zlib.Z_BLOCK;
  
  int get Z_OK() => _zlib.Z_OK;
  int get Z_STREAM_END() => _zlib.Z_STREAM_END;
  int get Z_NEED_DICT() => _zlib.Z_NEED_DICT;
  
  int get Z_ERRNO() => _zlib.Z_ERRNO;
  int get Z_STREAM_ERROR() => _zlib.Z_STREAM_ERROR;
  int get Z_DATA_ERROR() => _zlib.Z_DATA_ERROR;
  int get Z_MEM_ERROR() => _zlib.Z_MEM_ERROR;
  int get Z_BUF_ERROR() => _zlib.Z_BUF_ERROR;
  int get Z_VERSION_ERROR() => _zlib.Z_VERSION_ERROR;

  int get Z_NO_COMPRESSION() => _zlib.Z_NO_COMPRESSION;
  int get Z_BEST_SPEED() => _zlib.Z_BEST_SPEED;
  int get Z_BEST_COMPRESSION() => _zlib.Z_BEST_COMPRESSION;
  int get Z_DEFAULT_COMPRESSION() => _zlib.Z_DEFAULT_COMPRESSION;
  
  int get Z_FILTERED() => _zlib.Z_FILTERED;
  int get Z_HUFFMAN_ONLY() => _zlib.Z_HUFFMAN_ONLY;
  int get Z_RLE() => _zlib.Z_RLE;
  int get Z_FIXED() => _zlib.Z_FIXED;
  int get Z_DEFAULT_STRATEGY() => _zlib.Z_DEFAULT_STRATEGY;
  
  int get ZLIB_VERNUM() => _zlib.ZLIB_VERNUM;
  String get ZLIB_VERSION() => _zlib.ZLIB_VERSION;

  int get Z_MIN_WINDOWBITS() => _zlib.Z_MIN_WINDOWBITS;
  int get Z_MAX_WINDOWBITS() => _zlib.Z_MAX_WINDOWBITS;
  int get Z_DEFAULT_WINDOWBITS() => _zlib.Z_DEFAULT_WINDOWBITS;

  int get Z_MIN_CHUNK() => _zlib.Z_MIN_CHUNK;
  num get Z_MAX_CHUNK() => _zlib.Z_MAX_CHUNK;
  int get Z_DEFAULT_CHUNK() => _zlib.Z_DEFAULT_CHUNK;

  int get Z_MIN_MEMLEVEL() => _zlib.Z_MIN_MEMLEVEL;
  int get Z_MAX_MEMLEVEL() => _zlib.Z_MAX_MEMLEVEL;
  int get Z_DEFAULT_MEMLEVEL() => _zlib.Z_DEFAULT_MEMLEVEL;

  int get Z_MIN_LEVEL() => _zlib.Z_MIN_LEVEL;
  int get Z_MAX_LEVEL() => _zlib.Z_MAX_LEVEL;
  int get Z_DEFAULT_LEVEL() => _zlib.Z_DEFAULT_LEVEL;
  
  Gzip createGzip([Map<String,Object> options])
      => _zlib.createGzip(options);
  Gunzip createGunzip([Map<String,Object> options])
      => _zlib.createGunzip(options);
  Deflate createDeflate([Map<String,Object> options])
      => _zlib.createDeflate(options);
  Inflate createInflate([Map<String,Object> options])
      => _zlib.createInflate(options);
  DeflateRaw createDeflateRaw([Map<String,Object> options])
      => _zlib.createDeflateRaw(options);
  InflateRaw createInflateRaw([Map<String,Object> options])
      => _zlib.createInflateRaw(options);
  Unzip createUnzip([Map<String,Object> options])
      => _zlib.createUnzip(options);
  
  void deflate(String buf,
      void callback(Error err, String result))
      => _zlib.deflate(buf, callback);
  void deflateBuffer(Buffer buf,
      void callback(Error err, Buffer result))
      => _zlib.deflate(buf, callback);

  void deflateRaw(String buf,
      void callback(Error err, String result))
      => _zlib.deflateRaw(buf, callback);
  void deflateRawBuffer(Buffer buf,
      void callback(Error err, Buffer result))
      => _zlib.deflateRaw(buf, callback);

  void gzip(String buf,
      void callback(Error err, String result))
      => _zlib.gzip(buf, callback);
  void gzipBuffer(Buffer buf,
      void callback(Error err, Buffer result))
      => _zlib.gzip(buf, callback);

  void gunzip(String buf,
      void callback(Error err, String result))
      => _zlib.gunzip(buf, callback);
  void gunzipBuffer(Buffer buf,
      void callback(Error err, Buffer result))
      => _zlib.gunzip(buf, callback);

  void inflate(String buf,
      void callback(Error err, String result))
      => _zlib.inflate(buf, callback);
  void inflateBuffer(Buffer buf,
      void callback(Error err, Buffer result))
      => _zlib.inflate(buf, callback);

  void inflateRaw(String buf,
      void callback(Error err, String result))
      => _zlib.inflateRaw(buf, callback);
  void inflateRawBuffer(Buffer buf,
      void callback(Error err, Buffer result))
      => _zlib.inflateRaw(buf, callback);

  void unzip(String buf,
      void callback(Error err, String result))
      => _zlib.unzip(buf, callback);
  void unzipBuffer(Buffer buf,
      void callback(Error err, Buffer result))
      => _zlib.unzip(buf, callback);
}

class _zlib native "require('zlib')" {
  static int Z_NO_FLUSH;
  static int Z_PARTIAL_FLUSH;
  static int Z_SYNC_FLUSH;
  static int Z_FULL_FLUSH;
  static int Z_FINISH;
  static int Z_BLOCK;
  
  static int Z_OK;
  static int Z_STREAM_END;
  static int Z_NEED_DICT;
  
  static int Z_ERRNO;
  static int Z_STREAM_ERROR;
  static int Z_DATA_ERROR;
  static int Z_MEM_ERROR;
  static int Z_BUF_ERROR;
  static int Z_VERSION_ERROR;

  static int Z_NO_COMPRESSION;
  static int Z_BEST_SPEED;
  static int Z_BEST_COMPRESSION;
  static int Z_DEFAULT_COMPRESSION;
  
  static int Z_FILTERED;
  static int Z_HUFFMAN_ONLY;
  static int Z_RLE;
  static int Z_FIXED;
  static int Z_DEFAULT_STRATEGY;
  
  static int ZLIB_VERNUM;
  static String get ZLIB_VERSION;

  static int Z_MIN_WINDOWBITS;
  static int Z_MAX_WINDOWBITS;
  static int Z_DEFAULT_WINDOWBITS;

  static int Z_MIN_CHUNK;
  static num get Z_MAX_CHUNK;
  static int Z_DEFAULT_CHUNK;

  static int Z_MIN_MEMLEVEL;
  static int Z_MAX_MEMLEVEL;
  static int Z_DEFAULT_MEMLEVEL;

  static int Z_MIN_LEVEL;
  static int Z_MAX_LEVEL;
  static int Z_DEFAULT_LEVEL;
  
  static Gzip createGzip([Map options]) native;
  static Gunzip createGunzip([Map options]) native;
  static Deflate createDeflate([Map options]) native;
  static Inflate createInflate([Map options]) native;
  static DeflateRaw createDeflateRaw([Map options]) native;
  static InflateRaw createInflateRaw([Map options]) native;
  static Unzip createUnzip([Map options]) native;
  
  static void deflate(var buf,
      void callback(Error err, var result)) native;
  static void deflateRaw(var buf,
      void callback(Error err, var result)) native;
  static void gzip(var buf,
      void callback(Error err, var result)) native;
  static void gunzip(var buf,
      void callback(Error err, var result)) native;
  static void inflate(var buf,
      void callback(Error err, var result)) native;
  static void inflateRaw(var buf,
      void callback(Error err, var result)) native;
  static void unzip(var buf,
      void callback(Error err, var result)) native;
}

Zlib get zlib() => new Zlib();

class Gzip implements ReadWriteStream native "require('zlib').Gzip"{
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
  List<ReadableStreamDataListener> listenersData()
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

  bool writable;
  bool write(String string, [String encoding, int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
}

class Gunzip implements ReadWriteStream native "require('zlib').Gunzip"{
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
  List<ReadableStreamDataListener> listenersData()
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

  bool writable;
  bool write(String string, [String encoding, int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
}

class Deflate implements ReadWriteStream native "require('zlib').Deflate"{
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
  List<ReadableStreamDataListener> listenersData()
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

  bool writable;
  bool write(String string, [String encoding, int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
}

class Inflate implements ReadWriteStream native "require('zlib').Inflate"{
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
  List<ReadableStreamDataListener> listenersData()
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

  bool writable;
  bool write(String string, [String encoding, int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
}

class DeflateRaw implements ReadWriteStream
    native "require('zlib').DeflateRaw"{
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
  List<ReadableStreamDataListener> listenersData()
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

  bool writable;
  bool write(String string, [String encoding, int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
}

class InflateRaw implements ReadWriteStream
    native "require('zlib').InflateRaw"{
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
  List<ReadableStreamDataListener> listenersData()
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

  bool writable;
  bool write(String string, [String encoding, int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
}

class Unzip implements ReadWriteStream native "require('zlib').Unzip"{
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
  List<ReadableStreamDataListener> listenersData()
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

  bool writable;
  bool write(String string, [String encoding, int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding]) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
}

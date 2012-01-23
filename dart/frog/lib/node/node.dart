// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A collection of helper io functions implemented using node.js.
 *
 * Idea is to clone the node.js API as closely as possible while adding types.
 * Dart libraries on top of this will experiment with different APIs.
 */

#library('node');

#import('dart:coreimpl');
#import('nodeimpl.dart');
#import('fs.dart');

// The sandbox needs to import the constructor functions for all the
// non-hidden native types we use.

var createSandbox() native
  """return {
      'require': require,
      'process': process,
      'console': console,
      'Buffer' : Buffer,
      'setTimeout': setTimeout,
      'clearTimeout': clearTimeout,
      'setInterval': setInterval,
      'clearInterval': clearInterval,
      'module' : module,
      'ArrayBuffer': ArrayBuffer,
      'Int8Array': Int8Array,
      'Uint8Array': Uint8Array,
      'Int16Array': Int16Array,
      'Uint16Array': Uint16Array,
      'Int32Array': Int32Array,
      'Uint32Array': Uint32Array,
      'Float32Array': Float32Array,
      'Float64Array': Float64Array,
      'DataView': DataView
      };""";

// global console

class Console native "Console" {
  // TODO(jimhug): Map node.js's ability to take multiple args to what?
  void log(String text) native;
  void info(String text) native;
  void warn(String text) native;
  void error(String text) native;
  void dir(Object obj) native;
  void time(String label) native;
  void timeEnd(String label) native;
  void trace() native;
  void assert(bool value, [String message]) native;
}

Console get console()
  native "return console;";

/**
 Implement as much of EventEmitter polymorphically as is possible,
 given that frogsh function objects do not interoperate perfectly with
 JavaScript function objects
 */

// typedef EventListener([arg1, arg2, arg3]);

interface EventEmitter {
//  void addListener(String event, EventListener listener);
//  void on(String event, EventListener listener);
//  void once(String event, EventListener listener);
//  void removeListener(String event, EventListener listener);
//  List<EventListener> listeners(String event);
  void removeAllListeners(String event);
  void setMaxListeners(num n);
}

typedef void StreamErrorListener(Error exception);
typedef void StreamCloseListener();

interface CommonStream extends EventEmitter {
  // Error event
  void emitError(Error error);
  void addListenerError(StreamErrorListener listener);
  void onError(StreamErrorListener listener);
  void onceError(StreamErrorListener listener);
  void removeListenerError(StreamErrorListener listener);
  List<StreamErrorListener> listenersError();

  // Close event
  void emitClose();
  void addListenerClose(StreamCloseListener listener);
  void onClose(StreamCloseListener listener);
  void onceClose(StreamCloseListener listener);
  void removeListenerClose(StreamCloseListener listener);
  List<StreamCloseListener> listenersClose();

  void destroy();
  void destroySoon();
}

// data is either a Buffer or a String, depending upon whether setEncoding
// has been called.
typedef void ReadableStreamDataListener(var data);
typedef void ReadableStreamEndListener();

interface ReadableStream extends CommonStream {
  // Data event
  void emitData(var data);
  void addListenerData(ReadableStreamDataListener listener);
  void onData(ReadableStreamDataListener listener);
  void onceData(ReadableStreamDataListener listener);
  void removeListenerData(ReadableStreamDataListener listener);
  List<ReadableStreamDataListener> listenersData();

  // End event
  void emitEnd();
  void addListenerEnd(ReadableStreamEndListener listener);
  void onEnd(ReadableStreamEndListener listener);
  void onceEnd(ReadableStreamEndListener listener);
  void removeListenerEnd(ReadableStreamEndListener listener);
  List<ReadableStreamEndListener> listenersEnd();

  bool readable;
  void setEncoding(String encoding);
  void pause();
  void resume();
  WritableStream pipe(WritableStream destination, [Map options]);
}

typedef void WritableStreamDrainListener();
typedef void WritableStreamPipeListener(ReadableStream src);

interface WritableStream extends CommonStream {
  // Drain event
  void emitDrain();
  void addListenerDrain(WritableStreamDrainListener listener);
  void onDrain(WritableStreamDrainListener listener);
  void onceDrain(WritableStreamDrainListener listener);
  void removeListenerDrain(WritableStreamDrainListener listener);
  List<WritableStreamDrainListener> listenersDrain();

  // Pipe event
  void emitPipe(ReadableStream src);
  void addListenerPipe(WritableStreamPipeListener listener);
  void onPipe(WritableStreamPipeListener listener);
  void oncePipe(WritableStreamPipeListener listener);
  void removeListenerPipe(WritableStreamPipeListener listener);
  List<WritableStreamPipeListener> listenersPipe();

  bool writable;
  bool write(String string, [String encoding, int fd]);
  bool writeBuffer(Buffer buffer);
  void end([String string, String encoding]);
  void endBuffer(Buffer buffer);
}

interface ReadWriteStream extends ReadableStream, WritableStream {
  // No additional methods.
}

typedef void FsStreamOpenListener(int fd);

interface FsStream {
  // Open event
  void emitOpen(int fd);
  void addListenerOpen(FsStreamOpenListener listener);
  void onOpen(FsStreamOpenListener listener);
  void onceOpen(FsStreamOpenListener listener);
  void removeListenerOpen(FsStreamOpenListener listener);
  List<FsStreamOpenListener> listenersOpen();
}

typedef void ProcessExitListener();
typedef void ProcessUncaughtExceptionListener(Exception err);
typedef void ProcessSignalListener();

class Process implements EventEmitter native "Process" {
  var _process;

  // Note: This is not an exhaustive list of signals. Check with your
  // OS documentation for sigaction to see which signals are
  // available in your OS.

  final SIGHUP='SIGHUP';
  final SIGINT='SIGINT';
  final SIGQUIT='SIGQUIT';
  final SIGILL='SIGILL';
  final SIGTRAP='SIGTRAP';
  final SIGABRT='SIGABRT';
  final SIGEMT='SIGEMT';
  final SIGFPE='SIGFPE';
  final SIGKILL='SIGKILL';
  final SIGBUS='SIGBUS';
  final SIGSEGV='SIGSEGV';
  final SIGSYS='SIGSYS';
  final SIGPIPE='SIGPIPE';
  final SIGALRM='SIGALRM';
  final SIGTERM='SIGTERM';
  final SIGURG='SIGURG';
  final SIGSTOP='SIGSTOP';
  final SIGTSTP='SIGTSTP';
  final SIGCONT='SIGCONT';
  final SIGCHLD='SIGCHLD';
  final SIGTTIN='SIGTTIN';
  final SIGTTOU='SIGTTOU';
  final SIGIO='SIGIO';
  final SIGXCPU='SIGXCPU';
  final SIGXFSZ='SIGXFSZ';
  final SIGVTALRM='SIGVTALRM';
  final SIGPROF='SIGPROF';
  final SIGWINCH='SIGWINCH';
  final SIGINFO='SIGINFO';
  final SIGUSR1='SIGUSR1';
  final SIGUSR2='SIGUSR2';

  Process(var this._process);

  // Implement EventEmitter
  void removeAllListeners(String event)
    native "this._process.removeAllListeners(event);";
  void setMaxListeners(num n)
    native "this._process.setMaxListeners(n);";

  _listeners(String key)
      native "return this._process.listeners(key);";

  // Exit event
  void emitExit()
    native "this._process.emit('exit');";
  void addListenerExit(ProcessExitListener listener)
    native "this._process.addListener('exit', listener);";
  void onExit(ProcessExitListener listener)
    native "this._process.on('exit', listener);";
  void onceExit(ProcessExitListener listener)
    native "this._process.once('exit', listener);";
  void removeListenerExit(ProcessExitListener listener)
    native "this._process.removeListener('exit', listener);";
  List<ProcessExitListener> listenersExit()
    => _listeners('exit');

  // UncaughtException event
  void emitUncaughtException(Exception err)
    native "this._process.emit('uncaughtException', err);";
  void addListenerUncaughtException(ProcessUncaughtExceptionListener listener)
    native "this._process.addListener('uncaughtException', listener);";
  void onUncaughtException(ProcessUncaughtExceptionListener listener)
    native "this._process.on('uncaughtException', listener);";
  void onceUncaughtException(ProcessUncaughtExceptionListener listener)
    native "this._process.once('uncaughtException', listener);";
  void removeListenerUncaughtException(
      ProcessUncaughtExceptionListener listener)
    native "this._process.removeListener('uncaughtException', listener);";
  List<ProcessUncaughtExceptionListener> listenersUncaughtException()
      => _listeners('uncaughtException');

  // Signal events
  void emitSignal(String signal)
    native "this._process.emit(signal);";
  void addListenerSignal(String signal, ProcessSignalListener listener)
    native "this._process.addListener(signal, listener);";
  void onSignal(String signal, ProcessSignalListener listener)
    native "this._process.on(signal, listener);";
  void onceSignal(String signal, ProcessSignalListener listener)
    native "this._process.once(signal, listener);";
  void removeListenerSignal(String signal, ProcessSignalListener listener)
    native "this._process.removeListener(signal, listener);";
  List<ProcessSignalListener> listenersSignal(String signal)
    => _listeners(signal);

  WriteStream get stdout()
    native "return this._process.stdout;";
  WriteStream get stderr()
    native "return this._process.stderr;";
  ReadStream get stdin()
    native "return this._process.stdin;";

  List<String> get argv()
    native "return this._process.argv;";
  void set argv(List<String> value)
      native "this._process.argv = value;";
  String get execPath()
    native "return this._process.execPath;";
  String chdir(String directory)
    native "this._process.chdir(directory);";

  String cwd()
    native "return this._process.cwd();";

  Map<String,String> get env()
      => new _EnvMap(_env());
  var _env() native "return this._process.env;";

  void exit([int code = 0])
    native "this._process.exit(code);";
  int getgid()
    native "return this._process.getgid();";
  void setgid(var gid_or_groupname)
    native "this._process.setgid(uid_or_groupname);";
  int getuid()
    native "return this._process.getuid();";
  void setuid(var uid_or_username)
    native "this._process.setuid(uid_or_groupname);";
  String get version()
    native "return this._process.version;";
  Map<String,String> get versions()
    => new NativeMapPrimitiveValue<String>(_versions());
  var _versions() native "return this._process._versions;";
  String get installPrefix()
    native "return this._process.installPrefix;";
  void kill(int pid, [String signal=SIGTERM])
    native "this._process.kill(pid, signal);";
  int get pid()
    native "return this._process.pid;";
  String get title()
    native "return this._process.title;";
  String get arch()
    native "return this._process.arch;";
  String get platform()
    native "return this._process.platform;";
  MemoryUsage memoryUsage() => new MemoryUsage._from(_memoryUsage());
  var _memoryUsage() native "return this._process.memoryUsage()";
  void nextTick(Function callback)
    native "return this._process.nextTick(callback);";
  int umask([int mask])
    native "return this._process.umask(mask);";
  int uptime()
    native "return this._process.uptime();";
}

var get _process()
  native "return process;";

Process get process() {
  return new Process(_process);
}

// TODO(jmesserly) Frog should marshal Maps to JS automatically.

class _EnvMap extends NativeMapPrimitiveValue<String>{
  _EnvMap(var env) : super(env);
  // process.env doesn't implement hasOwnProperty
  void _forEachKey(var map, void f(String key))
   native """
       for (var i in map) {
         f(i);
       }
     """;
}

class MemoryUsage {
  MemoryUsage._from(var mu) {
    rss = nativeGetIntProperty(mu, 'rss');
    heapTotal = nativeGetIntProperty(mu, 'heapTotal');
    heapUsed = nativeGetIntProperty(mu, 'heapUsed');
  }
  int rss;
  int heapTotal;
  int heapUsed;
}

interface TimeoutId {}

TimeoutId setTimeout(callback(), int delay, [arg /* ... */]) native;
clearTimeout(TimeoutId timeoutId) native;

interface IntervalId {}

IntervalId setInterval(callback(), int delay, [arg  /* ... */]) native;
clearInterval(IntervalId intervalId) native;

// buffer

interface Buffer extends List<int> default _BufferImplementation {
  Buffer(int size);
  Buffer.fromSize(int size);
  Buffer.fromList(List<int> list);
  Buffer.fromString(String string, [String encoding]);

  int write(String string, int offset, int length, [String encoding]);
  String toString(String encoding, int start, int end);

  void copy(Buffer targetBuffer, int targetStart, int sourceStart, int sourceEnd);
  Buffer slice(int start, int end);

  int readUInt8(int offset, [bool noAssert]);
  int readUInt16LE(int offset, [bool noAssert]);
  int readUInt16BE(int offset, [bool noAssert]);
  int readUInt32LE(int offset, [bool noAssert]);
  int readUInt32BE(int offset, [bool noAssert]);

  int readInt8(int offset, [bool noAssert]);
  int readInt16LE(int offset, [bool noAssert]);
  int readInt16BE(int offset, [bool noAssert]);
  int readInt32LE(int offset, [bool noAssert]);
  int readInt32BE(int offset, [bool noAssert]);

  double readFloatLE(int offset, [bool noAssert]);
  double readFloatBE(int offset, [bool noAssert]);
  double readDoubleLE(int offset, [bool noAssert]);
  double readDoubleBE(int offset, [bool noAssert]);

  void writeUInt8(int value, int offset, [bool noAssert]);
  void writeUInt16LE(int value, int offset, [bool noAssert]);
  void writeUInt16BE(int value, int offset, [bool noAssert]);
  void writeUInt32LE(int value, int offset, [bool noAssert]);
  void writeUInt32BE(int value, int offset, [bool noAssert]);

  void writeInt8(int value, int offset, [bool noAssert]);
  void writeInt16LE(int value, int offset, [bool noAssert]);
  void writeInt16BE(int value, int offset, [bool noAssert]);
  void writeInt32LE(int value, int offset, [bool noAssert]);
  void writeInt32BE(int value, int offset, [bool noAssert]);

  void writeFloatLE(double value, int offset, [bool noAssert]);
  void writeFloatBE(double value, int offset, [bool noAssert]);
  void writeDoubleLE(double value, int offset, [bool noAssert]);
  void writeDoubleBE(double value, int offset, [bool noAssert]);

  // end defaults to buffer.length
  void fill(int value, int offset, int end);
}

/** Static methods that apply to all buffers */

class Buffers {
  static int get charsWritten() native "return Buffer._charsWritten;";
  static bool isBuffer(obj) native "return Buffer.isBuffer(obj);";
  static int byteLength(String string, [String encoding])
    native "return Buffer.byteLength(string, encoding);";
  static int get INSPECT_MAX_BYTES() native "return Buffer.INSPECT_MAX_BYTES;";
  static void set INSPECT_MAX_BYTES(int v) native "Buffer.INSPECT_MAX_BYTES = v;";
}

class _BufferImplementation implements Buffer native "Buffer" {
  _BufferImplementation(int size) native;
  _BufferImplementation.fromSize(int size)
    native "return new Buffer(size);";
  _BufferImplementation.fromList(List<int> list)
    native "return new Buffer(list);";
  _BufferImplementation.fromString(String string, [String encoding='utf8'])
    native "return new Buffer(string, encoding);";

  int write(String string, int offset, int length, [String encoding='utf8'])
    native;
  static int get charsWritten()
    native "return Buffer._charsWritten;";
  String toString(String encoding, int start, int end) native;

  // List<int> protocol
  int operator[](int index) native;
  int operator[]=(int index, int value) native;

  void _throwUnsupported() {
    throw new UnsupportedOperationException('not extendable');
  }
  void add(int value) => _throwUnsupported();
  void addAll(Collection<int> collection) => _throwUnsupported();
  void addLast(int value) => _throwUnsupported();
  void clear() => _throwUnsupported();
  List<int> getRange(int start, int length) {
    FixedLists.getRangeCheck(this.length, start, length);
    Buffer b = new Buffer(length);
    this.copy(b, 0, start, start + length);
    return b;
  }
  int indexOf(int element, [int start])
      => FixedLists.indexOf(this, element, start);
  void insertRange(int start, int length, [int initialValue])
      => _throwUnsupported();
  int last()
      => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
      => FixedLists.lastIndexOf(this, element, start);
  void set length(int newLength) => _throwUnsupported();
  int removeLast() {_throwUnsupported(); return 0; }
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
      => _throwUnsupported();
  void sort(int compare(int a, int b))
      => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Buffer filter(bool f(int element))
      => FixedLists.filter(this, f, (length) => new Buffer(length));
  Buffer map(f(int element))
      => FixedLists.map(this, f, new Buffer(this.length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);

  static bool isBuffer(obj) native;
  static int byteLength(String string, [String encoding='utf8']) native;
  int get length() native "return this.length;";
  void copy(Buffer targetBuffer, int targetStart, int sourceStart, int sourceEnd
      ) native;
  Buffer slice(int start, int end) native;

  int readUInt8(int offset, [bool noAssert=false]) native;
  int readUInt16LE(int offset, [bool noAssert=false]) native;
  int readUInt16BE(int offset, [bool noAssert=false]) native;
  int readUInt32LE(int offset, [bool noAssert=false]) native;
  int readUInt32BE(int offset, [bool noAssert=false]) native;

  int readInt8(int offset, [bool noAssert=false]) native;
  int readInt16LE(int offset, [bool noAssert=false]) native;
  int readInt16BE(int offset, [bool noAssert=false]) native;
  int readInt32LE(int offset, [bool noAssert=false]) native;
  int readInt32BE(int offset, [bool noAssert=false]) native;

  double readFloatLE(int offset, [bool noAssert=false]) native;
  double readFloatBE(int offset, [bool noAssert=false]) native;
  double readDoubleLE(int offset, [bool noAssert=false]) native;
  double readDoubleBE(int offset, [bool noAssert=false]) native;

  void writeUInt8(int value, int offset, [bool noAssert=false]) native;
  void writeUInt16LE(int value, int offset, [bool noAssert=false]) native;
  void writeUInt16BE(int value, int offset, [bool noAssert=false]) native;
  void writeUInt32LE(int value, int offset, [bool noAssert=false]) native;
  void writeUInt32BE(int value, int offset, [bool noAssert=false]) native;

  void writeInt8(int value, int offset, [bool noAssert=false]) native;
  void writeInt16LE(int value, int offset, [bool noAssert=false]) native;
  void writeInt16BE(int value, int offset, [bool noAssert=false]) native;
  void writeInt32LE(int value, int offset, [bool noAssert=false]) native;
  void writeInt32BE(int value, int offset, [bool noAssert=false]) native;

  void writeFloatLE(double value, int offset, [bool noAssert=false]) native;
  void writeFloatBE(double value, int offset, [bool noAssert=false]) native;
  void writeDoubleLE(double value, int offset, [bool noAssert=false]) native;
  void writeDoubleBE(double value, int offset, [bool noAssert=false]) native;

  // end defaults to buffer.length
  void fill(int value, int offset, int end) native;

  static int INSPECT_MAX_BYTES;
}

class Error native "Error" {
  var stack;
  var arguments;
  var type;
  String message;
  bool killed;
  int code;
  String signal;
}

// TODO: module dgram

// TODO: module https

// module assert TODO: (or maybe not, isn't there a Dart assert?)

// Typed arrays

class ArrayBuffer native "ArrayBuffer" {
  ArrayBuffer(int length) native;
  int byteLength;
}

interface ArrayBufferView {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;
}

interface TypedArrayBufferView<E> extends ArrayBufferView, List<E> {
  final int BYTES_PER_ELEMENT;

  void set(TypedArrayBufferView<E> array, [int offset]);
  TypedArrayBufferView<E> subarray(int begin, [int end]);
}

// TODO: Factor out common code. See how DOM handles the equivalent DOM types for ideas.

class Int8Array implements TypedArrayBufferView<int> native "Int8Array" {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;

  final int BYTES_PER_ELEMENT;
  final int length;

  Int8Array(int length) native;
  factory Int8Array.fromArray(Int8Array array) native "return new Int8Array(array);";
  factory Int8Array.fromList(List<int> list) native "return new Int8Array(list);";
  factory Int8Array.fromArrayBuffer(ArrayBuffer buffer, [int byteOffset, int length])
      native """if (byteOffset === undefined) return new Int8Array(buffer);
          if (length === undefined) return new Int8Array(buffer, byteOffset);
          return new Int8Array(buffer, byteOffset, length);""";

  // List protocol
  int operator[](int index) native;
  void operator[]=(int index, int value) native;
  void _throwUnsupported() {
     throw new UnsupportedOperationException('not extendable');
   }
   void add(int value) => _throwUnsupported();
   void addAll(Collection<int> collection) => _throwUnsupported();
   void addLast(int value) => _throwUnsupported();
   void clear() => _throwUnsupported();
   int indexOf(int element, [int start])
       => FixedLists.indexOf(this, element, start);
   void insertRange(int start, int length, [int initialValue])
       => _throwUnsupported();
  Int8Array getRange(int start, int length) {
    FixedLists.getRangeCheck(this.length, start, length);
    return new Int8Array.fromArray(subarray(start, start+length));
  }
  int last()
      => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
      => FixedLists.lastIndexOf(this, element, start);
  int removeLast() => _throwUnsupported();
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
      => _throwUnsupported();
  void sort(int compare(int a, int b))
      => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Int8Array filter(bool f(int element))
      => FixedLists.filter(this, f, (length) => new Int8Array(length));
  Int8Array map(f(int))
      => FixedLists.map(this, f, new Int8Array(this.length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);

  void set(Int8Array array, [int offset]) native;
  Int8Array subarray(int begin, [int end])
      native "return end == null ? this.subarray(begin) : this.subarray(begin, end);";
}

class Uint8Array implements TypedArrayBufferView<int> native "Uint8Array" {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;

  final int BYTES_PER_ELEMENT;
  final int length;

  Uint8Array(int length) native;
  factory Uint8Array.fromArray(Uint8Array array) native "return new Uint8Array(array);";
  factory Uint8Array.fromList(List<int> list) native "return new Uint8Array(list);";
  factory Uint8Array.fromArrayBuffer(ArrayBuffer buffer, [int byteOffset, int length])
      native """if (byteOffset === undefined) return new Uint8Array(buffer);
          if (length === undefined) return new Uint8Array(buffer, byteOffset);
          return new Uint8Array(buffer, byteOffset, length);""";

  // List protocol
  int operator[](int index) native;
  void operator[]=(int index, int value) native;
  void _throwUnsupported() {
     throw new UnsupportedOperationException('not extendable');
  }
  void add(int value) => _throwUnsupported();
  void addAll(Collection<int> collection) => _throwUnsupported();
  void addLast(int value) => _throwUnsupported();
  void clear() => _throwUnsupported();
  int indexOf(int element, [int start])
      => FixedLists.indexOf(this, element, start);
  void insertRange(int start, int length, [int initialValue])
      => _throwUnsupported();
  Uint8Array getRange(int start, int length) {
    FixedLists.getRangeCheck(this.length, start, length);
    return new Uint8Array.fromArray(subarray(start, start+length));
  }
  int last()
      => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
      => FixedLists.lastIndexOf(this, element, start);
  int removeLast() => _throwUnsupported();
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
      => _throwUnsupported();
  void sort(int compare(int a, int b))
      => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Uint8Array filter(bool f(int element))
      => FixedLists.filter(this, f, (length) => new Uint8Array(length));
  Uint8Array map(f(int))
      => FixedLists.map(this, f, new Uint8Array(this.length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);

  void set(Uint8Array array, [int offset]) native;
  Uint8Array subarray(int begin, [int end])
      native "return end == null ? this.subarray(begin) : this.subarray(begin, end);";
}

class Int16Array implements TypedArrayBufferView<int> native "Int16Array" {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;

  final int BYTES_PER_ELEMENT;
  final int length;

  Int16Array(int length) native;
  factory Int16Array.fromArray(Int16Array array) native "return new Int16Array(array);";
  factory Int16Array.fromList(List<int> list) native "return new Int16Array(list);";
  factory Int16Array.fromArrayBuffer(ArrayBuffer buffer, [int byteOffset, int length])
      native """if (byteOffset === undefined) return new Int16Array(buffer);
          if (length === undefined) return new Int16Array(buffer, byteOffset);
          return new Int16Array(buffer, byteOffset, length);""";

  // List protocol
  int operator[](int index) native;
  void operator[]=(int index, int value) native;
  void _throwUnsupported() {
     throw new UnsupportedOperationException('not extendable');
   }
   void add(int value) => _throwUnsupported();
   void addAll(Collection<int> collection) => _throwUnsupported();
   void addLast(int value) => _throwUnsupported();
   void clear() => _throwUnsupported();
   int indexOf(int element, [int start])
       => FixedLists.indexOf(this, element, start);
   void insertRange(int start, int length, [int initialValue])
       => _throwUnsupported();
  Int16Array getRange(int start, int length) {
    FixedLists.getRangeCheck(this.length, start, length);
    return new Int16Array.fromArray(subarray(start, start+length));
  }
  int last()
      => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
      => FixedLists.lastIndexOf(this, element, start);
  int removeLast() => _throwUnsupported();
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
      => _throwUnsupported();
  void sort(int compare(int a, int b))
      => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Int16Array filter(bool f(int element))
      => FixedLists.filter(this, f, (length) => new Int16Array(length));
  Int16Array map(f(int))
      => FixedLists.map(this, f, new Int16Array(this.length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);

  void set(Int16Array array, [int offset]) native;
  Int16Array subarray(int begin, [int end])
      native "return end == null ? this.subarray(begin) : this.subarray(begin, end);";
}

class Uint16Array implements TypedArrayBufferView<int> native "Uint16Array" {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;

  final int BYTES_PER_ELEMENT;
  final int length;

  Uint16Array(int length) native;
  factory Uint16Array.fromArray(Uint16Array array) native "return new Uint16Array(array);";
  factory Uint16Array.fromList(List<int> list) native "return new Uint16Array(list);";
  factory Uint16Array.fromArrayBuffer(ArrayBuffer buffer, [int byteOffset, int length])
      native """if (byteOffset === undefined) return new Uint16Array(buffer);
          if (length === undefined) return new Uint16Array(buffer, byteOffset);
          return new Uint16Array(buffer, byteOffset, length);""";

  // List protocol
  int operator[](int index) native;
  void operator[]=(int index, int value) native;
  void _throwUnsupported() {
     throw new UnsupportedOperationException('not extendable');
   }
   void add(int value) => _throwUnsupported();
   void addAll(Collection<int> collection) => _throwUnsupported();
   void addLast(int value) => _throwUnsupported();
   void clear() => _throwUnsupported();
   int indexOf(int element, [int start])
       => FixedLists.indexOf(this, element, start);
   void insertRange(int start, int length, [int initialValue])
       => _throwUnsupported();
  Uint16Array getRange(int start, int length) {
    FixedLists.getRangeCheck(this.length, start, length);
    return new Uint16Array.fromArray(subarray(start, start+length));
  }
  int last()
      => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
      => FixedLists.lastIndexOf(this, element, start);
  int removeLast() => _throwUnsupported();
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
      => _throwUnsupported();
  void sort(int compare(int a, int b))
      => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Uint16Array filter(bool f(int element))
      => FixedLists.filter(this, f, (length) => new Uint16Array(length));
  Uint16Array map(f(int))
      => FixedLists.map(this, f, new Uint16Array(this.length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);

  void set(Uint16Array array, [int offset]) native;
  Uint16Array subarray(int begin, [int end])
      native "return end == null ? this.subarray(begin) : this.subarray(begin, end);";
}

class Int32Array implements TypedArrayBufferView<int> native "Int32Array" {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;

  final int BYTES_PER_ELEMENT;
  final int length;

  Int32Array(int length) native;
  factory Int32Array.fromArray(Int32Array array) native "return new Int32Array(array);";
  factory Int32Array.fromList(List<int> list) native "return new Int32Array(list);";
  factory Int32Array.fromArrayBuffer(ArrayBuffer buffer, [int byteOffset, int length])
      native """if (byteOffset === undefined) return new Int32Array(buffer);
          if (length === undefined) return new Int32Array(buffer, byteOffset);
          return new Int32Array(buffer, byteOffset, length);""";

  // List protocol
  int operator[](int index) native;
  void operator[]=(int index, int value) native;
  void _throwUnsupported() {
    throw new UnsupportedOperationException('not extendable');
  }
  void add(int value) => _throwUnsupported();
  void addAll(Collection<int> collection) => _throwUnsupported();
  void addLast(int value) => _throwUnsupported();
  void clear() => _throwUnsupported();
  int indexOf(int element, [int start])
      => FixedLists.indexOf(this, element, start);
  void insertRange(int start, int length, [int initialValue])
      => _throwUnsupported();
  Int32Array getRange(int start, int length) {
   FixedLists.getRangeCheck(this.length, start, length);
   return new Int32Array.fromArray(subarray(start, start+length));
  }
  int last()
     => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
     => FixedLists.lastIndexOf(this, element, start);
  int removeLast() => _throwUnsupported();
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
     => _throwUnsupported();
  void sort(int compare(int a, int b))
     => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Int32Array filter(bool f(int element))
      => FixedLists.filter(this, f, (length) => new Int32Array(length));
  Int32Array map(f(int))
      => FixedLists.map(this, f, new Int32Array(this.length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);

  void set(Int32Array array, [int offset]) native;
  Int32Array subarray(int begin, [int end])
      native "return end == null ? this.subarray(begin) : this.subarray(begin, end);";
}

class Uint32Array implements TypedArrayBufferView<int> native "Uint32Array" {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;

  final int BYTES_PER_ELEMENT;
  final int length;

  Uint32Array(int length) native;
  factory Uint32Array.fromArray(Uint32Array array) native "return new Uint32Array(array);";
  factory Uint32Array.fromList(List<int> list) native "return new Uint32Array(list);";
  factory Uint32Array.fromArrayBuffer(ArrayBuffer buffer, [int byteOffset, int length])
      native """if (byteOffset === undefined) return new Uint32Array(buffer);
          if (length === undefined) return new Uint32Array(buffer, byteOffset);
          return new Uint32Array(buffer, byteOffset, length);""";

  // List protocol
  int operator[](int index) native;
  void operator[]=(int index, int value) native;
  void _throwUnsupported() {
    throw new UnsupportedOperationException('not extendable');
  }
  void add(int value) => _throwUnsupported();
  void addAll(Collection<int> collection) => _throwUnsupported();
  void addLast(int value) => _throwUnsupported();
  void clear() => _throwUnsupported();
  int indexOf(int element, [int start])
      => FixedLists.indexOf(this, element, start);
  void insertRange(int start, int length, [int initialValue])
      => _throwUnsupported();
  Uint32Array getRange(int start, int length) {
   FixedLists.getRangeCheck(this.length, start, length);
   return new Uint32Array.fromArray(subarray(start, start+length));
  }
  int last()
     => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
     => FixedLists.lastIndexOf(this, element, start);
  int removeLast() => _throwUnsupported();
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
     => _throwUnsupported();
  void sort(int compare(int a, int b))
     => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Uint32Array filter(bool f(int element))
      => FixedLists.filter(this, f, (length) => new Uint32Array(length));
  Uint32Array map(f(int))
      => FixedLists.map(this, f, new Uint32Array(this.length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);

  void set(Uint32Array array, [int offset]) native;
  Uint32Array subarray(int begin, [int end])
      native "return end == null ? this.subarray(begin) : this.subarray(begin, end);";
}

class Float32Array implements TypedArrayBufferView<num> native "Float32Array" {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;

  final int BYTES_PER_ELEMENT;
  final int length;

  Float32Array(int length) native;
  factory Float32Array.fromArray(Float32Array array) native "return new Float32Array(array);";
  factory Float32Array.fromList(List<double> list) native "return new Float32Array(list);";
  factory Float32Array.fromArrayBuffer(ArrayBuffer buffer, [int byteOffset, int length])
      native """if (byteOffset === undefined) return new Float32Array(buffer);
          if (length === undefined) return new Float32Array(buffer, byteOffset);
          return new Float32Array(buffer, byteOffset, length);""";

  // List protocol
  int operator[](int index) native;
  void operator[]=(int index, int value) native;
  void _throwUnsupported() {
    throw new UnsupportedOperationException('not extendable');
  }
  void add(int value) => _throwUnsupported();
  void addAll(Collection<int> collection) => _throwUnsupported();
  void addLast(int value) => _throwUnsupported();
  void clear() => _throwUnsupported();
  int indexOf(int element, [int start])
      => FixedLists.indexOf(this, element, start);
  void insertRange(int start, int length, [int initialValue])
      => _throwUnsupported();
  Float32Array getRange(int start, int length) {
   FixedLists.getRangeCheck(this.length, start, length);
   return new Float32Array.fromArray(subarray(start, start+length));
  }
  int last()
     => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
     => FixedLists.lastIndexOf(this, element, start);
  int removeLast() => _throwUnsupported();
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
     => _throwUnsupported();
  void sort(int compare(int a, int b))
     => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Float32Array filter(bool f(int element))
      => FixedLists.filter(this, f, (length) => new Float32Array(length));
  Float32Array map(f(int))
      => FixedLists.map(this, f, new Float32Array(this.length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);

  void set(Float32Array array, [int offset]) native;
  Float32Array subarray(int begin, [int end])
      native "return end == null ? this.subarray(begin) : this.subarray(begin, end);";
}

class Float64Array implements TypedArrayBufferView<num> native "Float64Array" {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;

  final int BYTES_PER_ELEMENT;
  final int length;

  Float64Array(int length) native;
  factory Float64Array.fromArray(Float64Array array) native "return new Float64Array(array);";
  factory Float64Array.fromList(List<double> list) native "return new Float64Array(list);";
  factory Float64Array.fromArrayBuffer(ArrayBuffer buffer, [int byteOffset, int length])
      native """if (byteOffset === undefined) return new Float64Array(buffer);
          if (length === undefined) return new Float64Array(buffer, byteOffset);
          return new Float64Array(buffer, byteOffset, length);""";

  // List protocol
  int operator[](int index) native;
  void operator[]=(int index, int value) native;
  void _throwUnsupported() {
    throw new UnsupportedOperationException('not extendable');
  }
  void add(int value) => _throwUnsupported();
  void addAll(Collection<int> collection) => _throwUnsupported();
  void addLast(int value) => _throwUnsupported();
  void clear() => _throwUnsupported();
  int indexOf(int element, [int start])
      => FixedLists.indexOf(this, element, start);
  void insertRange(int start, int length, [int initialValue])
      => _throwUnsupported();
  Float64Array getRange(int start, int length) {
    FixedLists.getRangeCheck(this.length, start, length);
    return new Float64Array.fromArray(subarray(start, start+length));
  }
  int last()
     => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
     => FixedLists.lastIndexOf(this, element, start);
  int removeLast() => _throwUnsupported();
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
     => _throwUnsupported();
  void sort(int compare(int a, int b))
     => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Float64Array filter(bool f(int element))
      => FixedLists.filter(this, f, (length) => new Float64Array(length));
  Float64Array map(f(int))
      => FixedLists.map(this, f, new Float64Array(this.length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);
  void set(Float64Array array, [int offset]) native;
  Float64Array subarray(int begin, [int end])
      native "return end == null ? this.subarray(begin) : this.subarray(begin, end);";
}

class DataView implements ArrayBufferView native "DataView" {
  final ArrayBuffer buffer;
  final int byteOffset;
  final int byteLength;

  DataView.fromArray(ArrayBuffer buffer, [int byteOffset, int byteLength])
      native """if (byteOffset === undefined) return new DataView(buffer);
          if (length === undefined) return new DataView(buffer, byteOffset);
          return new DataView(buffer, byteOffset, length);""";

  int getInt8(int byteOffset) native;
  int getUint8(int byteOffset) native;
  int getInt16(int byteOffset, [bool littleEndian=false]) native;
  int getUint16(int byteOffset, [bool littleEndian=false]) native;
  int getInt32(int byteOffset, [bool littleEndian=false]) native;
  int getUint32(int byteOffset, [bool littleEndian=false]) native;
  num getFloat32(int byteOffset, [bool littleEndian=false]) native;
  num getFloat64(int byteOffset, [bool littleEndian=false]) native;

  void setInt8(int byteOffset, int value) native;
  void setUint8(int byteOffset, int value) native;
  void setInt16(int byteOffset, int value, [bool littleEndian=false]) native;
  void setUint16(int byteOffset, int value, [bool littleEndian=false]) native;
  void setInt32(int byteOffset, int value, [bool littleEndian=false]) native;
  void setUint32(int byteOffset, int value, [bool littleEndian=false]) native;
  void setFloat32(int byteOffset, num value, [bool littleEndian=false]) native;
  void setFloat64(int byteOffset, num value, [bool littleEndian=false]) native;
}

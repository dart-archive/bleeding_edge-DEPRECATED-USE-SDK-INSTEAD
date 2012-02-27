// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A collection of helper io functions implemented using node.js.
 *
 * Idea is to clone the node.js API as closely as possible while adding types.
 * Dart libraries on top of this will experiment with different APIs.
 */
#library('node');

// The sandbox needs to import the constructor functions for all the non-hidden native types we use.

var createSandbox() native
  """return {'require': require, 'process': process, 'console': console,
      'Buffer' : Buffer,
      'setTimeout\$': this.setTimeout, 'clearTimeout': clearTimeout};""";

typedef void RequestListener(ServerRequest request, ServerResponse response);

// TODO(nweiz): properly title-case these class names

class http native "require('http')" {
  static Server createServer(RequestListener listener) native;
}

class Server native "http.Server" {
  void listen(int port, [String hostname, Function callback]) native;
}

class ServerRequest native "http.IncomingMessage" {
  final String method;
  final String url;
  final Map<String, String> headers;
  final String httpVersion;

  void setEncoding([String encoding]) {}
}

class ServerResponse native "http.ServerResponse" {
  int statusCode;

  void setHeader(String name, String value) native;

  String getHeader(String name) native;

  void removeHeader(String name) native;

  void write(String data, [String encoding = 'utf8']) native;

  void end([String data, String encoding = 'utf8']) native;
}

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
  // TODO(jackpal): use rest arguments
  void emit(String event, [var arg1, var arg2, var arg3]);
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
  void emit(String event, [var arg1, var arg2, var arg3])
    native "this._process.emit(event, arg1, arg2, arg3)";
  
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
    native "return this._process.listeners('exit');";

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
    native "return this._process.listeners('uncaughtException');";
  
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
    native "return this._process.listeners(signal);";

  WritableStream get stdout()
    native "return this._process.stdout;";
  WritableStream get stderr()
    native "return this._process.stderr;";
  ReadableStream get stdin()
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

  EnvMap get env() => new EnvMap(_process);

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
  String get installPrefix()
    native "return this._process.installPrefix;";
  void kill(int pid, [String signal=SIGTERM])
    native "this._process.kill(pid, signal);";
  int get pid()
    native "return this._process.pid;";
  String get title()
    native "return this._process.title;";
  String get platform()
    native "return this._process.platform;";

  // TODO(jackpal) implement Map memoryUsage() native;
  void nextTick(Function callback)
    native "return this._process.nextTick(callback);";
  int umask([int mask])
    native "return this._process.umask(mask);";
}

var get _process()
  native "return process;";

Process get process() {
  return new Process(_process);
}

class EnvMap {
  var _process;
  const EnvMap(this._process);
  operator [](key) native "return this._process.env[key];";
  
}

typedef void UtilPumpCallback(var error);

class util native "require('util')" {
  static void debug(String string) native;
  static void log(String string) native;
  static void inspect(var object, [bool showHidden=false, num depth=2]) native;
  static pump(ReadableStream readableStream, WritableStream writeableStream,
    [UtilPumpCallback callback]) native;
  // the method inherits(a,b) doesn't make sense for Dart
}

// Object is either a Buffer or a String, depending upon whether setEncoding has been called.
typedef void ReadableStreamDataListener(var object);
typedef void ReadableStreamEndListener();
typedef void ReadableStreamErrorListener(Object exception);
typedef void ReadableStreamCloseListener();

class ReadableStream implements EventEmitter native "*ReadStream" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  void emit(String event, [var arg1, var arg2, var arg3]) native;
  
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
    native "return this._process.listeners('data');";

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
    native "return this._process.listeners('end');";
  
  // Error event
  void emitError(Object exception)
    native "this.emit('error', exception);";
  void addListenerError(ReadableStreamErrorListener listener)
    native "this.addListener('error', listener);";
  void onError(ReadableStreamErrorListener listener)
    native "this.on('error', listener);";
  void onceError(ReadableStreamErrorListener listener)
    native "this.once('error', listener);";
  void removeListenerError(ReadableStreamErrorListener listener)
    native "this.removeListener('error', listener);";
  List<ReadableStreamErrorListener> listenersError()
    native "return this._process.listeners('error');";

  // Close event
  void emitClose()
    native "this.emit('close');";
  void addListenerClose(ReadableStreamCloseListener listener)
    native "this.addListener('close', listener);";
  void onClose(ReadableStreamCloseListener listener)
    native "this.on('close', listener);";
  void onceClose(ReadableStreamCloseListener listener)
    native "this.once('close', listener);";
  void removeListenerClose(ReadableStreamCloseListener listener)
    native "this.removeListener('close', listener);";
  List<ReadableStreamCloseListener> listenersClose()
    native "return this._process.listeners('close');";
    
  bool readable;
  void setEncoding(String encoding) native;
  void pause() native;
  void resume() native;
  void destroy() native;
  void destroySoon() native;
  void pipe(WritableStream destination, [bool end=true])
    native "this.pipe(destination, {'end': end});";
}

typedef void WritableStreamDrainListener();
typedef void WritableStreamErrorListener(Object exception);
typedef void WritableStreamCloseListener();
typedef void WritableStreamPipeListener(ReadableStream src);

class WritableStream implements EventEmitter native "*WriteStream" {
  // EventEmitter
  void removeAllListeners(String event) native "this._writeStream.removeAllListeners(event);";
  void setMaxListeners(num n) native;
  void emit(String event, [var arg1, var arg2, var arg3]) native;
  
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
    native "return this._process.listeners('drain');";
    
  // Error event
  void emitError(Object exception)
    native "this.emit('error', exception);";
  void addListenerError(WritableStreamErrorListener listener)
    native "this.addListener('error', listener);";
  void onError(WritableStreamErrorListener listener)
    native "this.on('error', listener);";
  void onceError(WritableStreamErrorListener listener)
    native "this.once('error', listener);";
  void removeListenerError(WritableStreamErrorListener listener)
    native "this.removeListener('error', listener);";
  List<WritableStreamErrorListener> listenersError()
    native "return this._process.listeners('error');";

  // Close event
  void emitClose()
    native "this.emit('close');";
  void addListenerClose(WritableStreamCloseListener listener)
    native "this.addListener('close', listener);";
  void onClose(WritableStreamCloseListener listener)
    native "this.on('close', listener);";
  void onceClose(WritableStreamCloseListener listener)
    native "this.once('close', listener);";
  void removeListenerClose(WritableStreamCloseListener listener)
    native "this.removeListener('close', listener);";
  List<WritableStreamCloseListener> listenersClose()
    native "return this._process.listeners('close');";

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
    native "return this._process.listeners('pipe');";

  bool writable;
  bool write(String string, [String encoding='utf8', int fd]) native;
  bool writeBuffer(Buffer buffer) native;
  void end([String string, String encoding='utf8']) native;
  void endBuffer(Buffer buffer) native "this.end(buffer);";
  void destroy() native;
  void destroySoon() native;
}

class vm native "require('vm')" {
  static void runInThisContext(String code, [String filename]) native;
  static void runInNewContext(String code, [var sandbox, String filename])
    native;
  static Script createScript(String code, [String filename]) native;
  static Context createContext([sandbox]) native;
  static runInContext(String code, Context context, [String filename]) native;
}

interface Context {}

class Script native "vm.Script" {
  void runInThisContext() native;
  void runInNewContext([Map sandbox]) native;
}

class fs native "require('fs')" {
  static void writeFileSync(String outfile, String text) native;

  static String readFileSync(String filename, [String encoding = 'utf8'])
    native;

  static String realpathSync(String path) native;

  static void mkdirSync(String path, [num mode = 511 /* 0777 octal */]) native;
  static List<String> readdirSync(String path) native;
  static void rmdirSync(String path) native;
  static Stats statSync(String path) native;
  static void unlinkSync(String path) native;

  static void writeSync(int fd, String text) native;  
  static int openSync(String path, String flags,
    [num mode = 438] /* 0666 octal */) native;
  static void closeSync(int fd) native;
}

class Stats native "fs.Stats" {
  bool isFile() native;
  bool isDirectory() native;
  bool isBlockDevice() native;
  bool isCharacterDevice() native;
  bool isSymbolicLink() native;
  bool isFIFO() native;
  bool isSocket() native;

  // TODO(rnystrom): There are also the other fields we can add here if needed.
  // See: http://nodejs.org/docs/v0.6.1/api/fs.html#fs.Stats.
}

class path native "require('path')" {
  static bool existsSync(String filename) native;
  static String dirname(String path) native;
  static String basename(String path) native;
  static String extname(String path) native;
  static String normalize(String path) native;
  // TODO(jimhug): Get the right signatures for normalizeArray and join
}

class Readline native "require('readline')" {
  static ReadlineInterface createInterface(input, output) native;
}

class ReadlineInterface native "Readline.Interface" {
  void setPrompt(String prompt, [int length]) native;
  void prompt() native;
  void on(String event, Function callback) native;
}

interface TimeoutId {}

TimeoutId setTimeout(Function callback, num delay, [arg]) native;
clearTimeout(TimeoutId id) native;

typedef void ChildProcessExitListener(int code, String signal);

class ChildProcess implements EventEmitter native "ChildProcess" {
  var _childprocess;
  
  ChildProcess(this._childprocess);
  
  // EventEmitter
  void removeAllListeners(String event)
    native "this._childprocess.removeAllListeners(event);";
  void setMaxListeners(num n)
    native "this._childprocess.setMaxListeners(n);";
  void emit(String event, [var arg1, var arg2, var arg3])
      native "this._childprocess.emit(event, arg1, arg2, arg3);";
  
  // 'exit' event
  void addListenerExit(ChildProcessExitListener listener)
    native "this._childprocess.addListener('exit', listener);";
  void onExit(ChildProcessExitListener listener)
    native "this._childprocess.on('exit', listener);";
  void onceExit(ChildProcessExitListener listener)
    native "this._childprocess.once('exit', listener);";
  void removeListenerExit(ChildProcessExitListener listener)
    native "this._childprocess.removeListener('exit', listener);";
  List<ChildProcessExitListener> listenersExit()
    native "return this._childprocess.listeners('exit');";
  
  WritableStream get stdin()
    native "return this._childprocess.stdin;";

  ReadableStream get stdout()
    native "return this._childprocess.stdout;";
  ReadableStream get stderr()
    native "return this._childprocess.stderr;";
  int get pid()
    native "return this._childprocess.pid;";
}

typedef void Child_processCallback(Error error, String stdout, String stderr);

class Child_process native {
  var _cp;
  
  Child_process() {
    _cp = _get_child_process();
  }
  
  // TODOO(jackpal): translate options into a Javascript dictionary
  ChildProcess spawn(String command, [List<String> args,
    Map<String, Object> options]){
    return new ChildProcess(_spawn(_cp, command, args));
  }
  
  // TODOO(jackpal): translate options into a Javascript dictionary
  ChildProcess exec(String command, Child_processCallback callback,
      [Map<String, Object> options]) {
    // Note the argument order to exec is different than to _exec,
    // because Dart can't have optional arguments in the middle of
    // an argument list.
    return new ChildProcess(_exec(_cp, command, options, callback));
  }
  
  static var _spawn(var cp, String command, List<String> args)
    native "return cp.spawn(command, args);";
  static var _exec(var cp, String command, Map<String, Object> options,
    Child_processCallback callback)
    native "return cp.exec(command, options, callback);";

  static var _get_child_process()
    native "return require('child_process');";
}

var get child_process() {
  return new Child_process();
}

class Buffer native "Buffer" {
  Buffer(int size) native;
  Buffer.fromSize(int size)
    native "return new Buffer(size);";
  Buffer.fromList(List<int> list)
    native "return new Buffer(list);";
  Buffer.fromString(String string, [String encoding='utf8'])
    native "return new Buffer(string, encoding);";
  // the default length is buffer.length-offset
  int write(String string, int offset, int length, [String encoding='utf8'])
    native;
  static int get charsWritten()
    native "return Buffer._charsWritten;";
  String toString(String encoding, int start, int end) native;
  int operator[](int index)
    native "return this[index];";
  int operator[]=(int index, int value)
    native "this[index] = value; return value;";
  static bool isBuffer(obj) native;
  static int byteLength(String string, [String encoding='utf8']) native;
  int length;
  void copy(Buffer targetBuffer, int targetStart, int sourceStart, int sourceEnd) native;
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

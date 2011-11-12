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

var createSandbox() native
  """return {'require': require, 'process': process, 'console': console,
      'setTimeout': setTimeout, 'clearTimeout': clearTimeout};""";

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

class console native "console" {
  // TODO(jimhug): Map node.js's ability to take multiple args to what?
  static void log(String text) native;
  static void info(String text) native;
  static void warn(String text) native;
  static void error(String text) native;
}

class process native "process" {
  static List<String> argv;
  // TODO(nweiz): add Stream type information
  static stdin;
  static stdout;

  static void exit([int code = 0]) native;
  static String cwd() native;
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

class ReadlineInterface native "readline.Interface" {
  void setPrompt(String prompt, [int length]) native;
  void prompt() native;
  void on(String event, Function callback) native;
}

interface TimeoutId {}

TimeoutId setTimeout(Function callback, num delay, [arg]) native;
clearTimeout(TimeoutId id) native;

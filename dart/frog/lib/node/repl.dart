// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('repl');
#import('node.dart');

typedef void ReplEvalCallback(var ignore, var result);
typedef void ReplEvalFunction(String cmd, ReplEvalCallback callback);

class repl native "require('repl')" {
  static REPLServer start([String prompt, ReadWriteStream stream,
    ReplEvalFunction evalFn, bool useGlobal, bool ignoreUndefined]) native;
  static REPLServer startStdio([String prompt, ReplEvalFunction evalFn,
    bool useGlobal, bool ignoreUndefined])
    native "return this.start(prompt, evalFn, useGlobal, ignoreUndefined);";
  static bool disableColors;
}

class REPLServer native 'repl.REPLServer'{
  REPLServerMap get context() => new REPLServerMap(this);
}

class REPLServerMap {
  REPLServer _server;
  const REPLServerMap(this._server);
  void operator[]=(String key, var value) native
      "this._server.context[key] = value;";
  var operator[](String key) native "return this._server.context[key];";
}

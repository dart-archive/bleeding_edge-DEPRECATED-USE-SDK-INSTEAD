// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('readline');
#import('node.dart');

// module readline

// Actual shape of result is [List<String>, String]
typedef List ReadlineCompleter(String linePartial);

// Actual shape of results is [List<String>, String]
typedef void ReadlineCompleterCallback(var unused_pass_null, List results);

typedef void ReadlineCompleterAsync(String linePartial,
  ReadlineCompleterCallback callback);

class Readline native "require('readline')" {
  static ReadlineInterface createInterface(ReadStream input, WriteStream output,
    [ReadlineCompleter completer]) native;
  static ReadlineInterface createInterfaceAsyncCompleter(ReadStream input,
    WriteStream output, ReadlineCompleterAsync completer)
    native "this.createInterface(input, output, completer);";
  static int columns;
}

typedef void ReadlineInterfaceLineListener(String line);
typedef void ReadlineInterfaceCloseListener();
typedef void ReadlineInterfaceQuestionCallback(String answer);

class ReadlineInterface implements EventEmitter native "Readline.Interface" {
  // EventEmitter
  void removeAllListeners(String event) native;
  void setMaxListeners(num n) native;
  _listeners(String key)
    native "return this.listeners(key);";
  
  // event 'line'
  void emitLine(String line)
    native "this.emit('line');";
  void addListenerLine(ReadlineInterfaceLineListener listener)
    native "this.addListener('line', listener);";
  void onLine(ReadlineInterfaceLineListener listener)
    native "this.on('line', listener);";
  void onceLine(ReadlineInterfaceLineListener listener)
    native "this.once('line', listener);";
  void removeListenerLine(ReadlineInterfaceLineListener listener)
    native "this.removeListener('line', listener);";
  List<ReadlineInterfaceLineListener> listenersLine()
    => new NativeListPrimitiveElement<ReadlineInterfaceLineListener>(
      _listeners('line'));

  // event 'close'
  void emitClose()
    native "this.emit('close');";
  void addListenerClose(ReadlineInterfaceCloseListener listener)
    native "this.addListener('close', listener);";
  void onClose(ReadlineInterfaceCloseListener listener)
    native "this.on('close', listener);";
  void onceClose(ReadlineInterfaceCloseListener listener)
    native "this.once('close', listener);";
  void removeListenerClose(ReadlineInterfaceCloseListener listener)
    native "this.removeListener('close', listener);";
  List<ReadlineInterfaceCloseListener> listenersClose()
    => new NativeListPrimitiveElement<ReadlineInterfaceCloseListener>(
      _listeners('close'));

  void setPrompt(String prompt, [int length]) native;
  void prompt() native;
  void question(String question, ReadlineInterfaceQuestionCallback callback)
      native;
  void close() native;
  void pause() native;
  void resume() native;
  void write(String s) native;
}

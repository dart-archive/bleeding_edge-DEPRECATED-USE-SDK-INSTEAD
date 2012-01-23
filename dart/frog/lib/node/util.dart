// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('util.dart');
#import('node.dart');

typedef void UtilPumpCallback(Error error);

class util native "require('util')" {
  static void debug(String string) native;
  static void log(String string) native;
  static void inspect(var object, [bool showHidden=false, num depth=2,
    bool colors=false]) native;
  static bool isArray(var object) native;
  static bool isRegExp(var object) native;
  static bool isDate(var object) native;
  static bool isError(var object) native;
  static pump(ReadableStream readableStream, WritableStream writeableStream,
    [UtilPumpCallback callback]) native;
  // the method inherits(a,b) doesn't make sense for Dart
}

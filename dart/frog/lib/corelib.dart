// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("dart:core");
#import("dart:coreimpl");

// TODO(jimhug): Better way to map in standard corelib
#source("../../corelib/src/bool.dart");
#source("../../corelib/src/collection.dart");
#source("../../corelib/src/comparable.dart");
#source("../../corelib/src/date.dart");
#source("../../corelib/src/double.dart");
#source("../../corelib/src/duration.dart");
#source("../../corelib/src/exceptions.dart");
#source("../../corelib/src/expect.dart");
#source("../../corelib/src/function.dart");
#source("../../corelib/src/future.dart");
#source("../../corelib/src/hashable.dart");
#source("../../corelib/src/int.dart");
#source("../../corelib/src/isolate.dart");
#source("../../corelib/src/iterable.dart");
#source("../../corelib/src/iterator.dart");
#source("../../corelib/src/list.dart");
#source("../../corelib/src/map.dart");
#source("math.dart"); // overriden to be more directly native
#source("num.dart"); // overriden to include int members on num - weird typing
#source("../../corelib/src/pattern.dart");
#source("../../corelib/src/promise.dart");
#source("../../corelib/src/queue.dart");
#source("../../corelib/src/regexp.dart");
#source("../../corelib/src/set.dart");
#source("../../corelib/src/stopwatch.dart");
#source("../../corelib/src/string.dart");
#source("../../corelib/src/strings.dart");
#source("../../corelib/src/string_buffer.dart");
#source("../../corelib/src/time_zone.dart");

// TODO(jimhug): Ad-hoc cut-paste-and-edit from compiler/lib below:
// Conceptual change is moving to more true natives.

/**
 * The class [Clock] provides access to a monotonically incrementing clock
 * device.
 */
class Clock {
  /** Returns the current clock tick. */
  static int now() native 'return new Date().getTime();';

  /** Returns the frequency of clock ticks in Hz. */
  // TODO(jimhug): Why isn't this a property?
  static int frequency() native 'return 1000;';
}

void print(obj) native '''if (typeof console == 'object') {
  if (obj) obj = obj.toString();
  console.log(obj);
} else {
  write(obj);
  write('\n');
}''';

// Exceptions thrown by the generated JS code.

class AssertError {
  final String failedAssertion;
  final String url;
  final int line;
  final int column;

  AssertError(this.failedAssertion, this.url, this.line, this.column);

  String toString() {
    return "Failed assertion: '$failedAssertion' is not true " +
        "in $url at line $line, column $column.";
  }
}

class TypeError extends AssertError {
  final String srcType;
  final String dstType;

  TypeError(Object src, String dstType) native @'''
if (src === null || src === undefined) {
  this.srcType = 'null';
} else {
  this.srcType = src.get$typeName();
}
this.dstType = dstType;
''';

  String toString() {
    return "Failed type check: type $srcType is not assignable to type " +
        "$dstType";
  }
}

class FallThroughError {

  const FallThroughError();

  String toString() {
    return "Switch case fall-through.";
  }
}

// Dart core library.

class Object native "Object" {

  const Object() native;

  bool operator ==(Object other) native;
  String toString() native;

  // TODO(jmesserly): optimize this. No need to call it.
  get dynamic() => this;

  // TODO(jmesserly): add named args. For now stay compatible with the VM.
  void noSuchMethod(String name, List args) {
    throw new NoSuchMethodException(this, name, args);
  }
}

#!/bin/bash
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

set -e

make -q || make

function listDartFiles() {
  find "$@" \
    \( -name xcodebuild -o -name out -o -name eclipse.workspace \) -prune -o \
    -name \*.dart -print
}

# BUG(5275207): Update grammar instead of filtering out the
# interpolation test.
listDartFiles ../../runtime | \
  grep -v NegativeTest | \
  grep -v PrivateTest.dart | \
  xargs java -jar out/dart.jar

listDartFiles ../../corelib | \
  grep -v NegativeTest | \
  xargs java -jar out/dart.jar

listDartFiles ../../compiler | \
  grep -v Negative | \
  xargs java -jar out/dart.jar

# BUG(5275207): Update grammar instead of filtering out the
# code that relies on fancy interpolation (Value.dart).
listDartFiles ../../client | \
  grep -v NegativeTest | \
  grep -v Value.dart | \
  xargs java -jar out/dart.jar

listDartFiles ../../tests | \
  grep -v NegativeTest | \
  grep -v StaticTopLevelTest.dart | \
  grep -v StringInterpolate2Test.dart | \
  xargs java -jar out/dart.jar

listDartFiles ../../samples | \
  xargs java -jar out/dart.jar

listDartFiles .. | \
  grep -v NegativeTest | \
  xargs java -jar out/dart.jar

// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test constant folding on numbers.

#import("compiler_helper.dart");

final String STRING_DOUBLE_FOLDING = """
void main() {
  print("titi" + 1.0);
}
""";

main() {
  String generated = compile(STRING_DOUBLE_FOLDING);
  Expect.isTrue(const RegExp("'titi1.0'").hasMatch(generated));
}

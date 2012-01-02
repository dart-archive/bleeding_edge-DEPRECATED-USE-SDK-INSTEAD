// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("compiler_helper.dart");

final String TEST_ONE = @"""
foo(a) {
  int c = foo(1);
  if (a) c = foo(2);
  return c;
}
""";


final String TEST_TWO = @"""
bar(a) {}
foo() {
  int a = 1;
  int c = foo(1);
  if (true) {}
  return a + c;
}
""";

final String TEST_THREE = @"""
foo(int param1, int param2) {
  return param1 + param2;
}
""";

bool checkNumberOfMatches(Iterator it, int nb) {
  for (int i = 0; i < nb; i++) {
    Expect.isTrue(it.hasNext());
    it.next();
  }
  Expect.isFalse(it.hasNext());
}

String anyIdentifier = "[a-zA-Z][a-zA-Z0-9]*";

main() {
  String generated = compile(TEST_ONE, 'foo');
  // Check that there is no assignment from a guard.
  RegExp regexp = const RegExp("= currentIsolate\\.guard\\\$num");
  Expect.isFalse(regexp.hasMatch(generated));

  regexp = new RegExp(
      "currentIsolate\\.guard\\\$num\\($anyIdentifier\\);");
  Iterator<Match> matches = regexp.allMatches(generated).iterator();
  checkNumberOfMatches(matches, 2);

  regexp = const RegExp("return c;");
  Expect.isTrue(regexp.hasMatch(generated));

  generated = compile(TEST_TWO, 'foo');
  regexp = const RegExp("foo\\(1\\)");
  matches = regexp.allMatches(generated).iterator();
  checkNumberOfMatches(matches, 1);

  generated = compile(TEST_THREE, 'foo');
  regexp = const RegExp("guard\\\$num\\(param1\\)");
  Expect.isTrue(regexp.hasMatch(generated));
  regexp = const RegExp("guard\\\$num\\(param2\\)");
  Expect.isTrue(regexp.hasMatch(generated));
}

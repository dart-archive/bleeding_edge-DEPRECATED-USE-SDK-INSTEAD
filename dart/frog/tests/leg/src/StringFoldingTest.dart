// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("compiler_helper.dart");

test(expression, avoid) {
  String generated = compile("foo() => $expression;", "foo");
  Expect.isFalse(avoid.allMatches(generated).iterator().hasNext());
}

main() {
  RegExp noEmptyString = new RegExp('""');
  // Adding an empty string to a string is folded.
  test("'a' + ''", noEmptyString);
  // Adding a string to an empty string is folded.
  test("''+ 'a'", noEmptyString);

  // Ditto for non-literal strings.
  test("('a' + 'b') + ''", noEmptyString);
  test("''+ ('a' + 'b')", noEmptyString);

  // Adding a non-string literal to a string is folded.
  test("'' + 42", noEmptyString);
  test("'' + true", noEmptyString);
  test("'' + null", noEmptyString);
}

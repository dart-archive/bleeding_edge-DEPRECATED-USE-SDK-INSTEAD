// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test that parameters keep their names in the output.

#import("compiler_helper.dart");
#import("parser_helper.dart");

final String TEST_INVOCATION0 = @"""
main() {
  var o = null;
  o();
}
""";

final String TEST_INVOCATION1 = @"""
main() {
  var o = null;
  o(1);
}
""";

final String TEST_INVOCATION2 = @"""
main() {
  var o = null;
  o(1, 2);
}
""";

closureInvocation() {
  String generated = compile(TEST_INVOCATION0);
  Expect.isTrue(generated.contains(".\$call\$0()"));
  generated = compile(TEST_INVOCATION1);
  Expect.isTrue(generated.contains(".\$call\$1(1)"));
  generated = compile(TEST_INVOCATION2);
  Expect.isTrue(generated.contains(".\$call\$2(1, 2)"));
}

main() {
  closureInvocation();
}

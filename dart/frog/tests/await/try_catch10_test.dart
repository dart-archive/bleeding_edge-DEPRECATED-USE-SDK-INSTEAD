// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a try-catch block, exception should not be caught.

#import("await_test_helper.dart");

class E1 implements Exception {
  const E1([msg=null]);
}

class E2 implements Exception {
  const E2([msg=null]);
}

awaitAndCatch() {
  try {
    final t = await futureOf(0);
    return t;
  } catch (E1 e) {
    throw const E2(); // this shouldn't execute, but currently does because
                      // t.then() is called synchronously in the future library.
  }
}

await_main() {
  int t = await awaitAndCatch();
  throw const E1();
}

main() {
  try {
    int t = await await_main();
  } catch (e) {
    Expect.isTrue(e is E1);
  }
}

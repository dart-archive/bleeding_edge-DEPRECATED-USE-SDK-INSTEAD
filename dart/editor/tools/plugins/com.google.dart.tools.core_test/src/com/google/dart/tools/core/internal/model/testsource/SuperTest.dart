// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Tests super calls and constructors.
class SuperTest {
  static main() {
    Sub sub = new Sub(1, 2);
    assert(sub.x == 1);
    assert(sub.y == 2);
    assert(sub.z == 3);
    assert(sub.v == 1);
    assert(sub.w == 2);
    assert(sub.u == 3);

    // TODO(ngeoffrya): Re-enable this test once we have proper
    // initializers.
    /*
    sub = new Sub.static(0);
    assert(sub.x == 0);
    assert(sub.y == 1);
    assert(sub.v == 2);
    assert(sub.w == 3);
    assert(sub.z == 4);
    assert(sub.u == 5);*/
  }
}

class Sup {
  // static int i = 0;
  var x, y, z;

  Sup(a, b) : x(a), y(b) {
    z = a + b;
  }
  /*
  Sup.static() : x(i++), y(i++) {
    z = i++;
  }
  */
}

class Sub extends Sup {
  var u, v, w;

  Sub(a, b) : super(a, b), v(a), w(b) {
    u = a + b;
  }
  /*
  Sub.static() : super.static(), v(i++), w(i++) {
    u = i++;
  }
  */
}

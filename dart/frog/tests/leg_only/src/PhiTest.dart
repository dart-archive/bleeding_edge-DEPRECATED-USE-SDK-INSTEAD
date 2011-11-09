// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

void phi1() {
  var x = 42;
  if (true) {
    print(x);
  }
  print(x);
}

void phi2() {
  var x = 499;
  if (true) {
    x = 42;
  }
  print(x);
}

void phi3() {
  var x = 499;
  if (true) {
    x = 42;
  } else {
    print(x);
  }
  print(x);
}

void phi4() {
  var x = 499;
  if (true) {
    print(x);
  } else {
    x = 42;
  }
  print(x);
}

void phi5() {
  var x = 499;
  if (true) {
    if (true) {
      x = 42;
    }
  }
  print(x);
}

void phi6() {
  var x = 499;
  if (true) {
    if (true) {
      print(x);
    } else {
      x = 42;
    }
  }
  print(x);
}

void phi7() {
  var x = 499;
  if (true) {
    x = 42;
    if (true) {
      x = 99;
    } else {
      x = 111;
    }
  } else {
    if (false) {
      x = 341;
    } else {
      x = 1024;
    }
  }
  print(x);
}

void phi8() {
  var x = 499;
  if (true) {
    x = 42;
    if (true) {
      x = 99;
    } else {
      x = 111;
    }
  } else {
    if (false) {
      x = 341;
    } else {
      x = 1024;
    }
  }
  if (true) {
    x = 12342;
    if (true) {
      x = 12399;
    } else {
      x = 123111;
    }
  } else {
    if (false) {
      x = 123341;
    } else {
      x = 1231024;
    }
  }
  print(x);
}

void phi9() {
  var x = 499;
  if (true) {
    var y = 42;
    if (true) {
      y = 99;
    } else {
      x = 111;
    }
    print(y);
  }
  print(x);
}

void main() {
  phi1();
  phi2();
  phi3();
  phi4();
  phi5();
  phi6();
  phi7();
  phi8();
  phi9();
}

// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

break1(int x, int y, int ew, int ez) {
  int w = 1;
  int z = 0;
  bk1: if (x == 2) {
    z = 1;
    if (y == 3) {
      w = 2;
      break bk1;
    } else {
      w = 3;
    }
  } else {
    z = 2;
    if (y == 3) {
      w = 4;
    } else {
      w = 5;
      break bk1;
    }
    break bk1;
  }
  Expect.equals(ew, w);
  Expect.equals(ez, z);
}

break2(int x, int y, int ew, int ez) {
  int w = 1;
  int z = 0;
  bk1: do {
    if (x == 2) {
      z = 1;
      if (y == 3) {
        w = 2;
        break bk1;
      } else {
        w = 3;
      }
    } else {
      z = 2;
      if (y == 3) {
        w = 4;
      } else {
        w = 5;
        break bk1;
      }
      break bk1;
    }
  } while (false);
  Expect.equals(ew, w);
  Expect.equals(ez, z);
}

break3(int x, int y, int ew, int ez) {
  int w = 1;
  int z = 0;
  do {
    if (x == 2) {
      z = 1;
      if (y == 3) {
        w = 2;
        break;
      } else {
        w = 3;
      }
    } else {
      z = 2;
      if (y == 3) {
        w = 4;
      } else {
        w = 5;
        break;
      }
      break;
    }
  } while (false);
  Expect.equals(ew, w);
  Expect.equals(ez, z);
}

main() {
  break1(2, 3, 2, 1);
  break1(2, 4, 3, 1);
  break1(3, 3, 4, 2);
  break1(3, 4, 5, 2);
  // Reenable when if-with-aborting-branch-inside-loop works.
  // break2(2, 3, 2, 1);
  // break2(2, 4, 3, 1);
  // break2(3, 3, 4, 2);
  // break2(3, 4, 5, 2);
  // break3(2, 3, 2, 1);
  // break3(2, 4, 3, 1);
  // break3(3, 3, 4, 2);
  // break3(3, 4, 5, 2);
}
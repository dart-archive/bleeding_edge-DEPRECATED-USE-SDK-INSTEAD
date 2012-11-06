// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test_sample_extension;

import "sample_asynchronous_extension.dart";

// TODO(3008): Run this test automatically on buildbot (dart:3008).
void main() {
  RandomArray r = new RandomArray();
  r.randomArray(100, 17, (list_100) {
    r.randomArray(200, 17, (list_200) {
      for (var i = 0; i < 100; ++i) {
        Expect.equals(list_100[i], list_200[i]);
      }
    });
  });

  // Gets a list of 256000 random uint8 values, using seed 19, and
  // runs checkNormal on that list.
  r.randomArray(256000, 19, checkNormal);
}

void checkNormal(List l) {
  // Count how many times each byte value occurs.  Assert that the counts
  // are all withing a reasonable (six-sigma) range.
  List counts = new List<int>();
  counts.insertRange(0, 256, 0);
  for (var e in l) { counts[e]++; }
  new RandomArray().randomArray(256000, 18, checkCorrelation(counts));
}

void checkCorrelation(List counts) {
  return (List l) {
    List counts_2 = new List<int>();
    counts_2.insertRange(0, 256, 0);
    for (var e in l) { counts_2[e]++; }
    var product = 0;
    for (var i = 0; i < 256; ++i) {
      Expect.isTrue(counts[i] < 1200);
      Expect.isTrue(counts_2[i] < 1200);
      Expect.isTrue(counts[i] > 800);
      Expect.isTrue(counts[i] > 800);

      product += counts[i] * counts_2[i];
    }
    Expect.isTrue(product < 256000000 * 1.001);
    Expect.isTrue(product > 256000000 * 0.999);
  };
}

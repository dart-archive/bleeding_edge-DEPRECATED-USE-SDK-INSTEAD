// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
library pop_pop_win.test_util;

import 'package:pop_pop_win/src/game.dart';

// This grid
// XXXXX2
// X7X8X3
// X5XXX2
// X32321
// 110000

const SAMPLE_FIELD = const
    [null, null, null, null, null, 2,
     null,    7, null,    8, null, 3,
     null,    5, null, null, null, 2,
     null,    3,    2,    3,    2, 1,
        1,    1,    0,    0,    0, 0];


Field getSampleField() {
  var bools = new List<bool>.from(SAMPLE_FIELD.map((x) => x == null));

  return new Field.fromSquares(6, 5, bools);
}

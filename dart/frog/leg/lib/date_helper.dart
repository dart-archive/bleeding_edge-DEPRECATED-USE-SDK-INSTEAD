// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

String fourDigits(int n) {
  int absN = n.abs();
  String sign = n < 0 ? "-" : "";
  if (absN >= 1000) return "$n";
  if (absN >= 100) return "${sign}0$absN";
  if (absN >= 10) return "${sign}00$absN";
  if (absN >= 1) return "${sign}000$absN";
  throw new IllegalArgumentException(n);
}

String threeDigits(int n) {
  if (n >= 100) return "${n}";
  if (n > 10) return "0${n}";
  return "00${n}";
}

String twoDigits(int n) {
  if (n >= 10) return "${n}";
  return "0${n}";
}

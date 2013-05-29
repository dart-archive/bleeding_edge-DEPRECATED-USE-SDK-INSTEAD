// Copyright (c) 2011, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

public class DartDoubleLiteral extends DartLiteral {

  public static DartDoubleLiteral get(double x) {
    return new DartDoubleLiteral(x);
  }

  private final double value;

  private DartDoubleLiteral(double value) {
    this.value = value;
  }

  public double getValue() {
    return value;
  }

}

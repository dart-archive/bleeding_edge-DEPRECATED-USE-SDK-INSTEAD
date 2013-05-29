// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

public class DartForStatement extends DartStatement {

  public DartForStatement(DartStatement init, DartExpression condition, DartExpression increment,
      int closeParenOffset, DartStatement body) {
  }

  public DartStatement getBody() {
    return null;
  }

  public int getCloseParenOffset() {
    return -1;
  }

  public DartExpression getCondition() {
    return null;
  }

  public DartExpression getIncrement() {
    return null;
  }

  public DartStatement getInit() {
    return null;
  }

}

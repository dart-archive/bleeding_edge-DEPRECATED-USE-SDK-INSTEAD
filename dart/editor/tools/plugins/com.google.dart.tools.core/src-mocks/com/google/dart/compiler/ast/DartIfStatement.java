// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

public class DartIfStatement extends DartStatement {

  public DartIfStatement(DartExpression condition, int closeParenOffset, DartStatement thenStmt,
      int elseTokenOffset, DartStatement elseStmt) {
  }

  public int getCloseParenOffset() {
    return -1;
  }

  public DartExpression getCondition() {
    return null;
  }

  public DartStatement getElseStatement() {
    return null;
  }

  public int getElseTokenOffset() {
    return -1;
  }

  public DartStatement getThenStatement() {
    return null;
  }
}

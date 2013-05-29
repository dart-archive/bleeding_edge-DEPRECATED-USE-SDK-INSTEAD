// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

public class DartForInStatement extends DartStatement {

  public DartForInStatement(DartStatement setup, DartExpression iterable, int closeParenOffset,
      DartStatement body) {
  }

  public DartStatement getBody() {
    return null;
  }

  public int getCloseParenOffset() {
    return -1;
  }

  public DartIdentifier getIdentifier() {
    return null;
  }

  public DartExpression getIterable() {
    return null;
  }

  public DartVariableStatement getVariableStatement() {
    return null;
  }

  public boolean introducesVariable() {
    return false;
  }
}

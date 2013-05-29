// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

/**
 * Represents a Dart 'do/while' statement.
 */
public class DartDoWhileStatement extends DartStatement {

  public DartDoWhileStatement(DartExpression condition, DartStatement body) {
  }

  public DartStatement getBody() {
    return null;
  }

  public DartExpression getCondition() {
    return null;
  }

}

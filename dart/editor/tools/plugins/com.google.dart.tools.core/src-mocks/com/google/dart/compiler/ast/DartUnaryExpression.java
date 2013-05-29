// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import com.google.dart.compiler.parser.Token;

public class DartUnaryExpression extends DartExpression {

  public DartUnaryExpression(Token operator, int operatorOffset, DartExpression arg,
      boolean isPrefix) {
  }

  public DartExpression getArg() {
    return null;
  }

  public Token getOperator() {
    return null;
  }

  public int getOperatorOffset() {
    return -1;
  }

  public boolean isPrefix() {
    return false;
  }

}

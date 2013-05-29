// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.List;

public class DartAnnotation extends DartNode {

  public DartAnnotation(DartExpression name, List<DartExpression> arguments) {
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return null;
  }

  public NodeList<DartExpression> getArguments() {
    return null;
  }

  public DartExpression getName() {
    return null;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
  }
}

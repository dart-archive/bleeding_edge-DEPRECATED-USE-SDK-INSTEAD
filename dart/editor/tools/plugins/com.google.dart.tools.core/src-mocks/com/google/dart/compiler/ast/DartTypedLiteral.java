// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.List;

public abstract class DartTypedLiteral extends DartExpression {

  DartTypedLiteral(boolean isConst, List<DartTypeNode> typeArguments) {
  }

  public List<DartTypeNode> getTypeArguments() {
    return null;
  }

  public boolean isConst() {
    return false;
  }

}

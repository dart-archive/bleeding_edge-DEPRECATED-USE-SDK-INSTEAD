// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.List;

public class DartNewExpression extends DartInvocation {

  public DartNewExpression(DartNode constructor, List<DartExpression> args, boolean isConst) {
    super(args);

  }

  public DartNode getConstructor() {
    return null;
  }

  public boolean isConst() {
    return false;
  }

  public void setConstructor(DartExpression newConstructor) {
  }

}

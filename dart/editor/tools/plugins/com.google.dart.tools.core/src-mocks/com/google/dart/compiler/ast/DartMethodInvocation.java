// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.List;

public class DartMethodInvocation extends DartInvocation {

  public DartMethodInvocation(DartExpression target, boolean isCascade,
      DartIdentifier functionName, List<DartExpression> args) {
    super(args);
  }

  public DartIdentifier getFunctionName() {
    return null;
  }

  public String getFunctionNameString() {
    return null;
  }

  public DartExpression getRealTarget() {
    return null;
  }

  @Override
  public DartExpression getTarget() {
    return null;
  }

  public boolean isCascade() {
    return false;
  }

  public void setFunctionName(DartIdentifier newName) {
  }

}

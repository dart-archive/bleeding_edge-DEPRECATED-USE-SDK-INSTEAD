// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.List;

public class DartSuperConstructorInvocation extends DartInvocation {

  public DartSuperConstructorInvocation(DartIdentifier name, List<DartExpression> args) {
    super(args);
  }

  public String getConstructorName() {
    return null;
  }

  public DartIdentifier getName() {
    return null;
  }

  public void setName(DartIdentifier newName) {
  }

}

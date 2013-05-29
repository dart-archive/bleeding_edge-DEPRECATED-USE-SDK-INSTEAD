// Copyright (c) 2011, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.List;

public abstract class DartInvocation extends DartExpression {

  public DartInvocation(List<DartExpression> arguments) {
  }

  public List<DartExpression> getArguments() {
    return null;
  }

  public DartExpression getTarget() {
    return null;
  }

}

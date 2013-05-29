// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.List;

public class DartFunctionObjectInvocation extends DartInvocation {

  public DartFunctionObjectInvocation(DartExpression target, List<DartExpression> args) {
    super(args);
  }

}

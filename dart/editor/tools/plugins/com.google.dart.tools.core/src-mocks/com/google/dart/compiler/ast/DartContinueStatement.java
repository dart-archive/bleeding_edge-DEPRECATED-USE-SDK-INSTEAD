// Copyright (c) 2011, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

public class DartContinueStatement extends DartGotoStatement {

  public DartContinueStatement(DartIdentifier label) {
    super(label);
  }

  @Override
  public boolean isAbruptCompletingStatement() {
    return false;
  }

}

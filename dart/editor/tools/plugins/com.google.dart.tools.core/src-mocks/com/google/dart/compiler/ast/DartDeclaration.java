// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

public abstract class DartDeclaration<N extends DartExpression> extends DartNode {

  protected DartDeclaration(N name) {
  }

  public final N getName() {
    return null;
  }

  public final void setName(N newName) {
  }
}

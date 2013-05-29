// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

public class DartInitializer extends DartNode {

  public DartInitializer(DartIdentifier name, DartExpression value) {
  }

  public String getInitializerName() {
    return null;
  }

  public DartIdentifier getName() {
    return null;
  }

  public DartExpression getValue() {
    return null;
  }

  public boolean isInvocation() {
    return false;
  }

  public void setName(DartIdentifier newName) {
  }

}

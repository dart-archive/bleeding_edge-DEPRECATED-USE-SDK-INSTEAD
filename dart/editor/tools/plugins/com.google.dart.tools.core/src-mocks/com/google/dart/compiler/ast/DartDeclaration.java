// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

public abstract class DartDeclaration<N extends DartExpression> extends DartNodeWithMetadata
    implements HasObsoleteMetadata {

  protected DartDeclaration(N name) {
  }

  public DartComment getDartDoc() {
    return null;
  }

  public final N getName() {
    return null;
  }

  @Override
  public DartObsoleteMetadata getObsoleteMetadata() {
    return null;
  }

  public void setDartDoc(DartComment dartDoc) {
  }

  public final void setName(N newName) {
  }

  @Override
  public void setObsoleteMetadata(DartObsoleteMetadata metadata) {

  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
  }
}

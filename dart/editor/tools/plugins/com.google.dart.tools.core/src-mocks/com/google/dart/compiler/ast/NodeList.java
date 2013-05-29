// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.AbstractList;
import java.util.Collection;

public class NodeList<E extends DartNode> extends AbstractList<E> {

  public static <E extends DartNode> NodeList<E> create(DartNode owner) {
    return null;
  }

  public NodeList(DartNode owner) {
  }

  public void accept(ASTVisitor<?> visitor) {
  }

  @Override
  public void add(int index, E element) {
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    return false;
  }

  @Override
  public E get(int index) {
    return null;
  }

  @Override
  public E set(int index, E element) {
    return null;
  }

  @Override
  public int size() {
    return -1;
  }
}

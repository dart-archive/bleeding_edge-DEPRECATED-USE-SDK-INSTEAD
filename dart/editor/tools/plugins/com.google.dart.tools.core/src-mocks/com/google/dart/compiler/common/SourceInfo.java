package com.google.dart.compiler.common;

// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import com.google.dart.compiler.Source;

import java.io.Serializable;

public final class SourceInfo implements Serializable {

  public static final SourceInfo UNKNOWN = new SourceInfo(null, 0, 0);

  public SourceInfo(Source source, int offset, int length) {
  }

  public int getColumn() {
    return -1;
  }

  public int getEnd() {
    return -1;
  }

  public int getLength() {
    return -1;
  }

  public int getLine() {
    return -1;
  }

  public int getOffset() {
    return -1;
  }

  public Source getSource() {
    return null;
  }

}

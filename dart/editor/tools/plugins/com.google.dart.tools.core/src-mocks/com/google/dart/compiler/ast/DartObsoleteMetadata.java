// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

public class DartObsoleteMetadata {

  public static final DartObsoleteMetadata EMPTY = null;

  public boolean isDeprecated() {
    return false;
  }

  public boolean isOverride() {
    return false;
  }

  public DartObsoleteMetadata makeDeprecated() {
    return null;
  }

  public DartObsoleteMetadata makeOverride() {
    return null;
  }
}

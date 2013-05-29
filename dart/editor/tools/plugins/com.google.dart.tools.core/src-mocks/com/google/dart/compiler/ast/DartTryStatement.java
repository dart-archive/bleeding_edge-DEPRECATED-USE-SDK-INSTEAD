// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.List;

public class DartTryStatement extends DartStatement {

  public DartTryStatement(DartBlock tryBlock, List<DartCatchBlock> catchBlocks,
      DartBlock finallyBlock) {
  }

  public List<DartCatchBlock> getCatchBlocks() {
    return null;
  }

  public DartBlock getFinallyBlock() {
    return null;
  }

  public DartBlock getTryBlock() {
    return null;
  }

}

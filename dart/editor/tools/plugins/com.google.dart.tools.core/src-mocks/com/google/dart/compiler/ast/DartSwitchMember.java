// Copyright (c) 2012, the Dart project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import java.util.List;

public abstract class DartSwitchMember extends DartNode {

  public DartSwitchMember(List<DartLabel> labels, List<DartStatement> statements) {
  }

  public List<DartLabel> getLabels() {
    return null;
  }

  public List<DartStatement> getStatements() {
    return null;
  }

}

/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.internal.dom.rewrite;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.dom.rewrite.TrackedNodePosition;

import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

/**
 * Instances of the class <code>TrackedNodePositionImpl</code> implement a tracked node position.
 */
public class TrackedNodePositionImpl implements TrackedNodePosition {
  private final TextEditGroup group;
  private final DartNode node;

  public TrackedNodePositionImpl(TextEditGroup group, DartNode node) {
    this.group = group;
    this.node = node;
  }

  @Override
  public int getLength() {
    if (group.isEmpty()) {
      return node.getSourceLength();
    }
    IRegion coverage = TextEdit.getCoverage(group.getTextEdits());
    if (coverage == null) {
      return node.getSourceLength();
    }
    return coverage.getLength();
  }

  @Override
  public int getStartPosition() {
    if (group.isEmpty()) {
      return node.getSourceStart();
    }
    IRegion coverage = TextEdit.getCoverage(group.getTextEdits());
    if (coverage == null) {
      return node.getSourceStart();
    }
    return coverage.getOffset();
  }
}

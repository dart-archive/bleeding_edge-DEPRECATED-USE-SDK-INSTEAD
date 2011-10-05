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
package com.google.dart.tools.core.internal.model.delta;

import com.google.dart.tools.core.model.DartElementDelta;

/**
 * Instances of the class <code>SimpleDelta</code> implement a simple delta that remembers the kind
 * of changes only.
 */
public class SimpleDelta {
  protected int kind = 0;

  protected int changeFlags = 0;

  /*
   * Mark this delta as added.
   */
  public void added() {
    this.kind = DartElementDelta.ADDED;
  }

  /*
   * Mark this delta as changed with the given change flag.
   */
  public void changed(int flags) {
    this.kind = DartElementDelta.CHANGED;
    this.changeFlags |= flags;
  }

  public int getFlags() {
    return this.changeFlags;
  }

  public int getKind() {
    return this.kind;
  }

  /**
   * Mark this delta has a having a modifiers change.
   */
  public void modifiers() {
    changed(DartElementDelta.F_MODIFIERS);
  }

  /**
   * Mark this delta as removed.
   */
  public void removed() {
    this.kind = DartElementDelta.REMOVED;
    this.changeFlags = 0;
  }

  /**
   * Mark this delta has a having a super type change.
   */
  public void superTypes() {
    changed(DartElementDelta.F_SUPER_TYPES);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    toDebugString(builder);
    return builder.toString();
  }

  protected void toDebugString(StringBuilder builder) {
    builder.append("["); //$NON-NLS-1$
    switch (getKind()) {
      case DartElementDelta.ADDED:
        builder.append('+');
        break;
      case DartElementDelta.REMOVED:
        builder.append('-');
        break;
      case DartElementDelta.CHANGED:
        builder.append('*');
        break;
      default:
        builder.append('?');
        break;
    }
    builder.append("]: {"); //$NON-NLS-1$
    toDebugString(builder, getFlags());
    builder.append("}"); //$NON-NLS-1$
  }

  protected boolean toDebugString(StringBuilder builder, int flags) {
    boolean prev = false;
    // if ((flags & DartElementDelta.F_MODIFIERS) != 0) {
    // if (prev)
    //        buffer.append(" | "); //$NON-NLS-1$
    //      buffer.append("MODIFIERS CHANGED"); //$NON-NLS-1$
    // prev = true;
    // }
    if ((flags & DartElementDelta.F_SUPER_TYPES) != 0) {
      if (prev) {
        builder.append(" | "); //$NON-NLS-1$
      }
      builder.append("SUPER TYPES CHANGED"); //$NON-NLS-1$
      prev = true;
    }
    return prev;
  }
}

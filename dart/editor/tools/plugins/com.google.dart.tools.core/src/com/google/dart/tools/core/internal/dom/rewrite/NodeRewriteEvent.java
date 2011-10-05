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

/**
 * Instances of the class <code>NodeRewriteEvent</code>
 */
public class NodeRewriteEvent extends RewriteEvent {
  private Object originalValue;
  private Object newValue;

  public NodeRewriteEvent(Object originalValue, Object newValue) {
    this.originalValue = originalValue;
    this.newValue = newValue;
  }

  @Override
  public int getChangeKind() {
    if (originalValue == newValue) {
      return UNCHANGED;
    }
    if (originalValue == null) {
      return INSERTED;
    }
    if (newValue == null) {
      return REMOVED;
    }
    if (originalValue.equals(newValue)) {
      return UNCHANGED;
    }
    return REPLACED;
  }

  @Override
  public RewriteEvent[] getChildren() {
    return null;
  }

  @Override
  public Object getNewValue() {
    return newValue;
  }

  @Override
  public Object getOriginalValue() {
    return originalValue;
  }

  @Override
  public boolean isListRewrite() {
    return false;
  }

  /*
   * Sets a new value for the new node. Internal access only.
   * 
   * @param newValue The new value to set.
   */
  public void setNewValue(Object newValue) {
    this.newValue = newValue;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    switch (getChangeKind()) {
      case INSERTED:
        buf.append(" [inserted: "); //$NON-NLS-1$
        buf.append(getNewValue());
        buf.append(']');
        break;
      case REPLACED:
        buf.append(" [replaced: "); //$NON-NLS-1$
        buf.append(getOriginalValue());
        buf.append(" -> "); //$NON-NLS-1$
        buf.append(getNewValue());
        buf.append(']');
        break;
      case REMOVED:
        buf.append(" [removed: "); //$NON-NLS-1$
        buf.append(getOriginalValue());
        buf.append(']');
        break;
      default:
        buf.append(" [unchanged]"); //$NON-NLS-1$
    }
    return buf.toString();
  }
}

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
 * Instances of the class <code>RewriteEvent</code>
 */
public abstract class RewriteEvent {
  /**
   * Change kind to describe that the event is an insert event. Does not apply for list events.
   */
  public static final int INSERTED = 1;

  /**
   * Change kind to describe that the event is an remove event. Does not apply for list events.
   */
  public static final int REMOVED = 2;

  /**
   * Change kind to describe that the event is an replace event. Does not apply for list events.
   */
  public static final int REPLACED = 4;

  /**
   * Change kind to signal that children changed. Does only apply for list events.
   */
  public static final int CHILDREN_CHANGED = 8;

  /**
   * Change kind to signal that the property did not change
   */
  public static final int UNCHANGED = 0;

  /**
   * @return the event's change kind.
   */
  public abstract int getChangeKind();

  /**
   * @return the events describing the changes in a list, or <code>null</code> if the event is not a
   *         list event.
   */
  public abstract RewriteEvent[] getChildren();

  /**
   * @return the new value. For lists this is a <code>List<code> of ASTNode's, for non-list
   * events this can be an ASTNode (for node properties), Integer (for an integer property),
   * Boolean (for boolean node properties) or properties like Operator.
   * <code>null</code> is returned if the event is a remove event.
   */
  public abstract Object getNewValue();

  /**
   * @return the original value. For lists this is a <code>List<code> of ASTNode's, for non-list
   * events this can be an ASTNode (for node properties), Integer (for an integer property),
   * Boolean (for boolean node properties) or properties like Operator.
   * <code>null</code> is returned if the event is a insert event.
   */
  public abstract Object getOriginalValue();

  /**
   * @return <code>true</code> if the given event is a list event.
   */
  public abstract boolean isListRewrite();
}

/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - Initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.dnd;

import org.eclipse.swt.dnd.DND;

import java.util.ArrayList;
import java.util.Collection;

abstract public class DefaultDragAndDropCommand implements DragAndDropCommand {
  /**
   * This keeps track of the owner that is the target of the drag and drop.
   */
  protected Object target;

  /**
   * This keeps track of the location of the drag and drop.
   */
  protected float location;

  /**
   * This keeps track of the lower range of locations in which the effect of this command remains
   * unchanged.
   */
  protected float lowerLocationBound;

  /**
   * This keeps track of the upper range of locations in which the effect of this command remains
   * unchanged.
   */
  protected float upperLocationBound;

  /**
   * This keeps track of the permitted operations.
   */
  protected int operations;

  /**
   * This keeps track of the current operation that will be returned by {@link #getOperation}.
   */
  protected int operation;

  /**
   * This keeps track of the feedback that will be returned by {@link #getFeedback}.
   */
  protected int feedback;

  /**
   * This keeps track of the collection of dragged sources.
   */
  protected Collection sources;

  public DefaultDragAndDropCommand(Object target, float location, int operations, int operation,
      Collection sources) {
    this.target = target;
    this.location = location;
    this.operations = operations;
    this.operation = operation;
    this.sources = new ArrayList(sources);
    if (!canExecute()) {
      this.operation = DND.DROP_NONE;
    }

  }

  public int getFeedback() {
    if (isAfter()) {
      return DND.FEEDBACK_INSERT_AFTER;
    } else {
      return DND.FEEDBACK_INSERT_BEFORE;
    }
  }

  public boolean isAfter() {
    return location > 0.5;
  }

  public int getOperation() {
    return operation;
  }

  public void reinitialize(Object target, float location, int operations, int operation,
      Collection sources) {
    this.target = target;
    this.location = location;
    this.operations = operations;
    this.operation = operation;
    this.sources = new ArrayList(sources);
    if (!canExecute()) {
      this.operation = DND.DROP_NONE;
    }
  }
}

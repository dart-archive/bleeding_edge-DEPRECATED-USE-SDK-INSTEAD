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
package com.google.dart.tools.core.internal.operation;

import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.DartElement;

/**
 * Instances of the class <code>MoveElementsOperation</code> implement an operation that moves
 * elements from their current container to a specified destination container, optionally renaming
 * the elements. A move operation is equivalent to a copy operation, where the source elements are
 * deleted after the copy.
 * <p>
 * This operation can be used for reorganizing elements within the same container.
 */
public class MoveElementsOperation extends CopyElementsOperation {
  /**
   * When executed, this operation will move the given elements to the given containers.
   */
  public MoveElementsOperation(DartElement[] elementsToMove, DartElement[] destContainers,
      boolean force) {
    super(elementsToMove, destContainers, force);
  }

  /**
   * Return the <code>String</code> to use as the main task name for progress monitoring.
   */
  @Override
  protected String getMainTaskName() {
    return Messages.operation_moveElementProgress;
  }

  @Override
  protected boolean isMove() {
    return true;
  }
}

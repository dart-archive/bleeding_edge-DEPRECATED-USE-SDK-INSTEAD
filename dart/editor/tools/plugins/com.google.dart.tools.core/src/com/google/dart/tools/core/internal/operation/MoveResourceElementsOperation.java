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
 * Instances of the class <code>MoveResourceElementsOperation</code> implement an operation the
 * moves resources (libraries and compilation units) from their current container to a specified
 * destination container, optionally renaming the elements. A move resource operation is equivalent
 * to a copy resource operation, where the source resources are deleted after the copy.
 * <p>
 * This operation can be used for reorganizing resources within the same container.
 */
public class MoveResourceElementsOperation extends CopyResourceElementsOperation {
  /**
   * When executed, this operation will move the given elements to the given containers.
   */
  public MoveResourceElementsOperation(DartElement[] elementsToMove, DartElement[] destContainers,
      boolean force) {
    super(elementsToMove, destContainers, force);
  }

  @Override
  protected String getMainTaskName() {
    return Messages.operation_moveResourceProgress;
  }

  @Override
  protected boolean isMove() {
    return true;
  }
}

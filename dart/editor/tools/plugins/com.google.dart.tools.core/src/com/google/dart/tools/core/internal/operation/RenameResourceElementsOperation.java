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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;

/**
 * Instances of the class <code>RenameResourceElementsOperation</code> implement an operation the
 * renames resources (libraries and compilation units).
 * <p>
 * Notes:
 * <ul>
 * <li>When a compilation unit is renamed, its main type and the constructors of the main type are
 * renamed.
 * </ul>
 */
public class RenameResourceElementsOperation extends MoveResourceElementsOperation {
  /**
   * When executed, this operation will rename the specified elements with the given names in the
   * corresponding destinations.
   */
  public RenameResourceElementsOperation(DartElement[] elements, DartElement[] destinations,
      String[] newNames, boolean force) {
    // a rename is a move to the same parent with a new name specified
    // these elements are from different parents
    super(elements, destinations, force);
    setRenamings(newNames);
  }

  @Override
  protected String getMainTaskName() {
    return Messages.operation_renameResourceProgress;
  }

  @Override
  protected boolean isRename() {
    return true;
  }

  @Override
  protected void verify(DartElement element) throws DartModelException {
    super.verify(element);

    int elementType = element.getElementType();

    DartCore.notYetImplemented();
    if (!(elementType == DartElement.COMPILATION_UNIT /*
                                                       * || elementType == DartElement
                                                       * .PACKAGE_FRAGMENT
                                                       */)) {
      error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
    }
    if (elementType == DartElement.COMPILATION_UNIT) {
      CompilationUnitImpl cu = (CompilationUnitImpl) element;
      if (cu.isWorkingCopy() && !cu.isPrimary()) {
        error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
      }
    }
    verifyRenaming(element);
  }
}

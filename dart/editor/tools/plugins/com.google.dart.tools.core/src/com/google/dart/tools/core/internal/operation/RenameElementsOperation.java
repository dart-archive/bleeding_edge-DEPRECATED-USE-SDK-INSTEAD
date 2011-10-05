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
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.SourceReference;

/**
 * Instances of the class <code>RenameElementsOperation</code> implement an operation that renames
 * elements.
 * <p>
 * Notes:
 * <ul>
 * <li>Resource rename is not supported - this operation only renames elements contained in
 * compilation units.
 * <li>When a main type is renamed, its compilation unit and constructors are renamed.
 * <li>Constructors cannot be renamed.
 * </ul>
 */
public class RenameElementsOperation extends MoveElementsOperation {
  /**
   * When executed, this operation will rename the specified elements with the given names in the
   * corresponding destinations.
   */
  public RenameElementsOperation(DartElement[] elements, DartElement[] destinations,
      String[] newNames, boolean force) {
    // a rename is a move to the same parent with a new name specified
    // these elements are from different parents
    super(elements, destinations, force);
    setRenamings(newNames);
  }

  @Override
  protected String getMainTaskName() {
    return Messages.operation_renameElementProgress;
  }

  @Override
  protected boolean isRename() {
    return true;
  }

  @Override
  protected DartModelStatus verify() {
    DartModelStatus status = super.verify();
    if (!status.isOK()) {
      return status;
    }
    if (this.renamingsList == null || this.renamingsList.length == 0) {
      return new DartModelStatusImpl(DartModelStatusConstants.NULL_NAME);
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  @Override
  protected void verify(DartElement element) throws DartModelException {
    if (element == null || !element.exists()) {
      error(DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST, element);
    }
    if (element.isReadOnly()) {
      error(DartModelStatusConstants.READ_ONLY, element);
    }
    if (!(element instanceof SourceReference)) {
      error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
    }
    DartCore.notYetImplemented();
    // int elementType = element.getElementType();
    // if (elementType < DartElement.TYPE || elementType ==
    // DartElement.INITIALIZER) {
    // error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
    // }
    verifyRenaming(element);
  }
}

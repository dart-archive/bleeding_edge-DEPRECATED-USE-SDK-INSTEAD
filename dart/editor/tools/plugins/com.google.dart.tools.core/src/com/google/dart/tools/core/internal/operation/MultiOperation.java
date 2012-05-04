/*
 * Copyright (c) 2012, the Dart project authors.
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
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.Method;

import org.eclipse.core.runtime.IStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * The abstract class <code>MultiOperation</code> defines the behavior common to operation that are
 * used to perform operations on multiple {@link DartElement Dart elements}. It is responsible for
 * running each operation in turn, collecting the errors, and merging the corresponding
 * {@link DartElementDelta deltas}.
 * <p>
 * If several errors occurred, they are collected in a multi-status {@link DartModelStatus}.
 * Otherwise, a simple {@link DartModelStatus} is thrown.
 */
public abstract class MultiOperation extends DartModelOperation {
  /**
   * Table specifying insertion positions for elements being copied/moved/renamed. Keyed by elements
   * being processed, and values are the corresponding insertion point.
   * 
   * @see #processElements()
   */
  protected Map<DartElement, DartElement> insertBeforeElements = new HashMap<DartElement, DartElement>(
      1);

  /**
   * Table specifying the new parent for elements being copied/moved/renamed. Keyed by elements
   * being processed, and values are the corresponding destination parent.
   */
  protected Map<DartElement, DartElement> newParents;

  /**
   * This table presents the data in <code>renamingList</code> in a more convenient way.
   */
  protected Map<DartElement, String> renamings;

  /**
   * The list of renamings supplied to the operation
   */
  protected String[] renamingsList = null;

  /**
   * Creates a new <code>MultiOperation</code> on <code>elementsToProcess</code> .
   */
  protected MultiOperation(DartElement[] elementsToProcess, boolean force) {
    super(elementsToProcess, force);
  }

  /**
   * Creates a new <code>MultiOperation</code>.
   */
  protected MultiOperation(DartElement[] elementsToProcess, DartElement[] parentElements,
      boolean force) {
    super(elementsToProcess, parentElements, force);
    newParents = new HashMap<DartElement, DartElement>(elementsToProcess.length);
    if (elementsToProcess.length == parentElements.length) {
      for (int i = 0; i < elementsToProcess.length; i++) {
        newParents.put(elementsToProcess[i], parentElements[i]);
      }
    } else { // same destination for all elements to be moved/copied/renamed
      for (int i = 0; i < elementsToProcess.length; i++) {
        newParents.put(elementsToProcess[i], parentElements[0]);
      }
    }

  }

  /**
   * Sets the insertion position in the new container for the modified element. The element being
   * modified will be inserted before the specified new sibling. The given sibling must be a child
   * of the destination container specified for the modified element. The default is
   * <code>null</code>, which indicates that the element is to be inserted at the end of the
   * container.
   */
  public void setInsertBefore(DartElement modifiedElement, DartElement newSibling) {
    insertBeforeElements.put(modifiedElement, newSibling);
  }

  /**
   * Sets the new names to use for each element being copied. The renamings correspond to the
   * elements being processed, and the number of renamings must match the number of elements being
   * processed. A <code>null</code> entry in the list indicates that an element is not to be
   * renamed.
   * <p>
   * Note that some renamings may not be used. If both a parent and a child have been selected for
   * copy/move, only the parent is changed. Therefore, if a new name is specified for the child, the
   * child's name will not be changed.
   */
  public void setRenamings(String[] renamings) {
    renamingsList = renamings;
    initializeRenamings();
  }

  /**
   * Convenience method to create a <code>DartModelException</code> embedding a
   * <code>DartModelStatusImpl</code>.
   */
  protected void error(int code, DartElement element) throws DartModelException {
    throw new DartModelException(new DartModelStatusImpl(code, element));
  }

  /**
   * Executes the operation.
   * 
   * @exception DartModelException if one or several errors occurred during the operation. If
   *              multiple errors occurred, the corresponding <code>DartModelStatusImpl</code> is a
   *              multi-status. Otherwise, it is a simple one.
   */
  @Override
  protected void executeOperation() throws DartModelException {
    processElements();
  }

  /**
   * Returns the parent of the element being copied/moved/renamed.
   */
  protected DartElement getDestinationParent(DartElement child) {
    return newParents.get(child);
  }

  /**
   * Returns the name to be used by the progress monitor.
   */
  protected abstract String getMainTaskName();

  /**
   * Returns the new name for <code>element</code>, or <code>null</code> if there are no renamings
   * specified.
   * 
   * @throws DartModelException if no name can be found for the element
   */
  protected String getNewNameFor(DartElement element) {
    String newName = null;
    if (renamings != null) {
      newName = renamings.get(element);
    }
    if (newName == null && element instanceof Method && ((Method) element).isConstructor()) {
      newName = getDestinationParent(element).getElementName();
    }
    return newName;
  }

  /**
   * Returns <code>true</code> if this operation represents a move or rename, <code>false</code> if
   * this operation represents a copy.<br>
   * Note: a rename is just a move within the same parent with a name change.
   */
  protected boolean isMove() {
    return false;
  }

  /**
   * Returns <code>true</code> if this operation represents a rename, <code>false</code> if this
   * operation represents a copy or move.
   */
  protected boolean isRename() {
    return false;
  }

  /**
   * Subclasses must implement this method to process a given <code>DartElement</code>.
   */
  protected abstract void processElement(DartElement element) throws DartModelException;

  /**
   * Processes all the <code>DartElement</code>s in turn, collecting errors and updating the
   * progress monitor.
   * 
   * @exception DartModelException if one or several operation(s) was unable to be completed.
   */
  protected void processElements() throws DartModelException {
    try {
      beginTask(getMainTaskName(), elementsToProcess.length);
      DartModelStatus[] errors = new DartModelStatus[3];
      int errorsCounter = 0;
      for (int i = 0; i < elementsToProcess.length; i++) {
        try {
          verify(elementsToProcess[i]);
          processElement(elementsToProcess[i]);
        } catch (DartModelException jme) {
          if (errorsCounter == errors.length) {
            // resize
            System.arraycopy(
                errors,
                0,
                (errors = new DartModelStatus[errorsCounter * 2]),
                0,
                errorsCounter);
          }
          errors[errorsCounter++] = jme.getDartModelStatus();
        } finally {
          worked(1);
        }
      }
      if (errorsCounter == 1) {
        throw new DartModelException(errors[0]);
      } else if (errorsCounter > 1) {
        if (errorsCounter != errors.length) {
          // resize
          System.arraycopy(
              errors,
              0,
              (errors = new DartModelStatus[errorsCounter]),
              0,
              errorsCounter);
        }
        throw new DartModelException(DartModelStatusImpl.newMultiStatus(errors));
      }
    } finally {
      done();
    }
  }

  /**
   * This method is called for each <code>DartElement</code> before <code>processElement</code>. It
   * should check that this <code>element</code> can be processed.
   */
  protected abstract void verify(DartElement element) throws DartModelException;

  /**
   * Verifies that the <code>destination</code> specified for the <code>element</code> is valid for
   * the types of the <code>element</code> and <code>destination</code>.
   */
  protected void verifyDestination(DartElement element, DartElement destination)
      throws DartModelException {
    if (destination == null || !destination.exists()) {
      error(DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST, destination);
    }

    int destType = destination.getElementType();
    DartCore.notYetImplemented();
    switch (element.getElementType()) {
    // case DartElement.PACKAGE_DECLARATION :
    // case DartElement.IMPORT_DECLARATION :
    // if (destType != DartElement.COMPILATION_UNIT)
    // error(DartModelStatusConstants.INVALID_DESTINATION, element);
    // break;
      case DartElement.TYPE:
        if (destType != DartElement.COMPILATION_UNIT && destType != DartElement.TYPE) {
          error(DartModelStatusConstants.INVALID_DESTINATION, element);
        }
        break;
      case DartElement.METHOD:
      case DartElement.FIELD:
        // case DartElement.INITIALIZER :
        if (destType != DartElement.TYPE /* || destination instanceof BinaryType */) {
          error(DartModelStatusConstants.INVALID_DESTINATION, element);
        }
        break;
      case DartElement.COMPILATION_UNIT:
        if (destType != DartElement.LIBRARY) {
          error(DartModelStatusConstants.INVALID_DESTINATION, element);
        } else {
          CompilationUnitImpl cu = (CompilationUnitImpl) element;
          if (isMove() && cu.isWorkingCopy() && !cu.isPrimary()) {
            error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
          }
        }
        break;
      // case DartElement.PACKAGE_FRAGMENT :
      // IPackageFragment fragment = (IPackageFragment) element;
      // DartElement parent = fragment.getParent();
      // if (parent.isReadOnly())
      // error(DartModelStatusConstants.READ_ONLY, element);
      // else if (destType != DartElement.PACKAGE_FRAGMENT_ROOT)
      // error(DartModelStatusConstants.INVALID_DESTINATION, element);
      // break;
      default:
        error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
    }
  }

  /**
   * Verify that the new name specified for <code>element</code> is valid for that type of Dart
   * element.
   */
  protected void verifyRenaming(DartElement element) throws DartModelException {
    String newName = getNewNameFor(element);
    boolean isValid = true;
    // DartProject project = element.getDartProject();
    DartCore.notYetImplemented();
    switch (element.getElementType()) {
    // case DartElement.PACKAGE_FRAGMENT :
    // if (((IPackageFragment) element).isDefaultPackage()) {
    // // don't allow renaming of default package (see PR #1G47GUM)
    // throw new DartModelException(new
    // DartModelStatusImpl(DartModelStatusConstants.NAME_COLLISION, element));
    // }
    // isValid = DartConventions.validatePackageName(newName).getSeverity() !=
    // IStatus.ERROR;
    // break;
      case DartElement.COMPILATION_UNIT:
        isValid = DartConventions.validateCompilationUnitName(newName).getSeverity() != IStatus.ERROR;
        break;
      // case DartElement.INITIALIZER :
      // isValid = false; //cannot rename initializers
      // break;
      default:
        isValid = false; // DartConventions.validateIdentifier(newName).getSeverity()
                         // != IStatus.ERROR;
        break;
    }

    if (!isValid) {
      throw new DartModelException(new DartModelStatusImpl(
          DartModelStatusConstants.INVALID_NAME,
          element,
          newName));
    }
  }

  /**
   * Verifies that the positioning sibling specified for the <code>element</code> is exists and its
   * parent is the destination container of this <code>element</code>.
   */
  protected void verifySibling(DartElement element, DartElement destination)
      throws DartModelException {
    DartElement insertBeforeElement = insertBeforeElements.get(element);
    if (insertBeforeElement != null) {
      if (!insertBeforeElement.exists() || !insertBeforeElement.getParent().equals(destination)) {
        error(DartModelStatusConstants.INVALID_SIBLING, insertBeforeElement);
      }
    }
  }

  /**
   * Sets up the renamings hashtable - keys are the elements and values are the new name.
   */
  private void initializeRenamings() {
    if (renamingsList != null && renamingsList.length == elementsToProcess.length) {
      renamings = new HashMap<DartElement, String>(renamingsList.length);
      for (int i = 0; i < renamingsList.length; i++) {
        if (renamingsList[i] != null) {
          renamings.put(elementsToProcess[i], renamingsList[i]);
        }
      }
    }
  }
}

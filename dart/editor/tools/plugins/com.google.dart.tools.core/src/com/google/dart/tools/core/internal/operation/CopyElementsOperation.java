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
import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.ParentElement;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;

import org.eclipse.core.runtime.IPath;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class <code>CopyElementsOperation</code> implement an operation that copies or
 * moves a collection of elements from their current container to a new container, optionally
 * renaming the elements.
 * <p>
 * Notes:
 * <ul>
 * <li>If there is already an element with the same name in the new container, the operation either
 * overwrites or aborts, depending on the collision policy setting. The default setting is abort.
 * <li>When constructors are copied to a type, the constructors are automatically renamed to the
 * name of the destination type.
 * <li>When main types are renamed (move within the same parent), the compilation unit and
 * constructors are automatically renamed.
 * <li>The collection of elements being copied must all share the same type of container (for
 * example, must all be type members).
 * <li>The elements are inserted in the new container in the order given.
 * <li>The elements can be positioned in the new container - see #setInsertBefore. By default, the
 * elements are inserted based on the default positions as specified in the creation operation for
 * that element type.
 * <li>This operation can be used to copy and rename elements within the same container.
 * <li>This operation only copies elements contained within compilation units.
 * </ul>
 */
public class CopyElementsOperation extends MultiOperation {
  private Map<DartElement, String> sources = new HashMap<DartElement, String>();

  /**
   * When executed, this operation will copy the given elements to the given container.
   */
  public CopyElementsOperation(DartElement[] elementsToCopy, DartElement destContainer,
      boolean force) {
    this(elementsToCopy, new DartElement[] {destContainer}, force);
  }

  /**
   * When executed, this operation will copy the given elements to the given containers. The
   * elements and destination containers must be in the correct order. If there is > 1 destination,
   * the number of destinations must be the same as the number of elements being
   * copied/moved/renamed.
   */
  public CopyElementsOperation(DartElement[] elementsToCopy, DartElement[] destContainers,
      boolean force) {
    super(elementsToCopy, destContainers, force);
  }

  /**
   * Returns the <code>String</code> to use as the main task name for progress monitoring.
   */
  @Override
  protected String getMainTaskName() {
    return Messages.operation_copyElementProgress;
  }

  /**
   * Returns the nested operation to use for processing this element
   */
  protected DartModelOperation getNestedOperation(DartElement element) {
    try {
      DartElement dest = getDestinationParent(element);
      DartCore.notYetImplemented();
      switch (element.getElementType()) {
      // case DartElement.PACKAGE_DECLARATION :
      // return new
      // CreatePackageDeclarationOperation(element.getElementName(),
      // (ICompilationUnit) dest);
      // case DartElement.IMPORT_DECLARATION :
      // IImportDeclaration importDeclaration = (IImportDeclaration) element;
      // return new CreateImportOperation(element.getElementName(),
      // (ICompilationUnit) dest, importDeclaration.getFlags());
        case DartElement.TYPE:
          if (isRenamingMainType(element, dest)) {
            IPath path = element.getPath();
            String extension = path.getFileExtension();
            return new RenameResourceElementsOperation(
                new DartElement[] {dest},
                new DartElement[] {dest.getParent()},
                new String[] {getNewNameFor(element) + '.' + extension},
                force);
          } else {
            String source = getSourceFor(element);
            String lineSeparator = Util.getLineSeparator(source, element.getDartProject());
            return new CreateTypeOperation(dest, source + lineSeparator, force);
          }
        case DartElement.METHOD:
          String source = getSourceFor(element);
          String lineSeparator = Util.getLineSeparator(source, element.getDartProject());
          return new CreateMethodOperation((Type) dest, source + lineSeparator, force);
        case DartElement.FIELD:
          source = getSourceFor(element);
          lineSeparator = Util.getLineSeparator(source, element.getDartProject());
          return new CreateFieldOperation((Type) dest, source + lineSeparator, force);
          // case DartElement.INITIALIZER :
          // source = getSourceFor(element);
          // lineSeparator =
          // Util.getLineSeparator(source,
          // element.getDartProject());
          // return new CreateInitializerOperation((IType) dest, source +
          // lineSeparator);
        default:
          return null;
      }
    } catch (DartModelException exception) {
      return null;
    }
  }

  /**
   * Return <code>true</code> if this element is the main type of its compilation unit.
   * 
   * @param element the element being renamed
   * @param dest the location to which it is being copied
   * @return <code>true</code> if this element is the main type of its compilation unit
   * @throws DartModelException if we cannot access the elements in question
   */
  protected boolean isRenamingMainType(DartElement element, DartElement dest) {
    if ((isRename() || getNewNameFor(element) != null)
        && dest.getElementType() == DartElement.COMPILATION_UNIT) {
      String typeName = dest.getElementName();
      typeName = Util.getNameWithoutDartLikeExtension(typeName);
      return element.getElementName().equals(typeName) && element.getParent().equals(dest);
    }
    return false;
  }

  /**
   * Copy/move the element from the source to destination, renaming the elements as specified,
   * honoring the collision policy.
   * 
   * @throws DartModelException if the operation is unable to be completed
   */
  @Override
  protected void processElement(DartElement element) throws DartModelException {
    DartModelOperation op = getNestedOperation(element);
    boolean createElementInCUOperation = op instanceof CreateElementInCUOperation;
    if (op == null) {
      return;
    }
    if (createElementInCUOperation) {
      DartElement sibling = insertBeforeElements.get(element);
      if (sibling != null) {
        ((CreateElementInCUOperation) op).setRelativePosition(
            sibling,
            CreateElementInCUOperation.INSERT_BEFORE);
      } else if (isRename()) {
        DartElement anchor = resolveRenameAnchor(element);
        if (anchor != null) {
          // insert after so that the anchor is found before when deleted below
          ((CreateElementInCUOperation) op).setRelativePosition(
              anchor,
              CreateElementInCUOperation.INSERT_AFTER);
        }
      }
      String newName = getNewNameFor(element);
      if (newName != null) {
        ((CreateElementInCUOperation) op).setAlteredName(newName);
      }
    }
    executeNestedOperation(op, 1);

    DartElementImpl destination = (DartElementImpl) getDestinationParent(element);
    CompilationUnitImpl unit = (CompilationUnitImpl) destination.getCompilationUnit();
    if (!unit.isWorkingCopy()) {
      unit.close();
    }

    if (createElementInCUOperation && isMove() && !isRenamingMainType(element, destination)) {
      DartModelOperation deleteOp = new DeleteElementsOperation(
          new DartElement[] {element},
          this.force);
      executeNestedOperation(deleteOp, 1);
    }
  }

  /**
   * Possible failures:
   * <ul>
   * <li>NO_ELEMENTS_TO_PROCESS - no elements supplied to the operation
   * <li>INDEX_OUT_OF_BOUNDS - the number of renamings supplied to the operation does not match the
   * number of elements that were supplied.
   * </ul>
   */
  @Override
  protected DartModelStatus verify() {
    DartModelStatus status = super.verify();
    if (!status.isOK()) {
      return status;
    }
    if (this.renamingsList != null && this.renamingsList.length != this.elementsToProcess.length) {
      return new DartModelStatusImpl(DartModelStatusConstants.INDEX_OUT_OF_BOUNDS);
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * @see MultiOperation Possible failure codes:
   *      <ul>
   *      <li>ELEMENT_DOES_NOT_EXIST - <code>element</code> or its specified destination is is
   *      <code>null</code> or does not exist. If a <code>null</code> element is supplied, no
   *      element is provided in the status, otherwise, the non-existent element is supplied in the
   *      status.
   *      <li>INVALID_ELEMENT_TYPES - <code>element</code> is not contained within a compilation
   *      unit. This operation only operates on elements contained within compilation units.
   *      <li>READ_ONLY - <code>element</code> is read only.
   *      <li>INVALID_DESTINATION - The destination parent specified for <code>element</code> is of
   *      an incompatible type. The destination for a package declaration or import declaration must
   *      be a compilation unit; the destination for a type must be a type or compilation unit; the
   *      destination for any type member (other than a type) must be a type. When this error
   *      occurs, the element provided in the operation status is the <code>element</code>.
   *      <li>INVALID_NAME - the new name for <code>element</code> does not have valid syntax. In
   *      this case the element and name are provided in the status.
   *      </ul>
   */
  @Override
  protected void verify(DartElement element) throws DartModelException {
    if (element == null || !element.exists()) {
      error(DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST, element);
    }
    if (element.getElementType() < DartElement.TYPE) {
      error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
    }
    if (element.isReadOnly()) {
      error(DartModelStatusConstants.READ_ONLY, element);
    }
    DartElement dest = getDestinationParent(element);
    verifyDestination(element, dest);
    verifySibling(element, dest);
    if (renamingsList != null) {
      verifyRenaming(element);
    }
  }

  /**
   * Return the cached source for this element or compute it if not already cached.
   * 
   * @return the cached source for this element
   */
  private String getSourceFor(DartElement element) throws DartModelException {
    String source = sources.get(element);
    if (source == null && element instanceof TypeMember) {
      source = ((TypeMember) element).getSource();
      sources.put(element, source);
    }
    return source;
  }

  /**
   * Return the anchor used for positioning in the destination for the element being renamed. For
   * renaming, if no anchor has explicitly been provided, the element is anchored in the same
   * position.
   */
  private DartElement resolveRenameAnchor(DartElement element) throws DartModelException {
    ParentElement parent = (ParentElement) element.getParent();
    DartElement[] children = parent.getChildren();
    for (int i = 0; i < children.length; i++) {
      DartElement child = children[i];
      if (child.equals(element)) {
        return child;
      }
    }
    return null;
  }
}

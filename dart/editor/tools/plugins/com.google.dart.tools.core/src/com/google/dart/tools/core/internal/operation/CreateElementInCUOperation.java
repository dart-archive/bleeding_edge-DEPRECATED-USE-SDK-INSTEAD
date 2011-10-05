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
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * The abstract class <code>CreateElementInCUOperation</code> implements behavior common to
 * operations that create elements within a compilation unit. To create a compilation unit, or an
 * element contained in a compilation unit, the source code for the entire compilation unit is
 * updated and saved.
 * <p>
 * The element being created can be positioned relative to an existing element in the compilation
 * unit via the methods {@link #createAfter(DartElement)} and {@link #createBefore(DartElement)}. By
 * default, the new element is positioned as the last child of its parent element.
 */
public abstract class CreateElementInCUOperation extends DartModelOperation {
  /**
   * The compilation unit AST used for this operation.
   */
  // protected DartUnit cuAST;

  /**
   * A constant meaning to position the new element as the last child of its parent element.
   */
  protected static final int INSERT_LAST = 1;

  /**
   * A constant meaning to position the new element after the element defined by
   * {@link #anchorElement}.
   */
  protected static final int INSERT_AFTER = 2;

  /**
   * A constant meaning to position the new element before the element defined by
   * {@link #anchorElement}.
   */
  protected static final int INSERT_BEFORE = 3;

  /**
   * One of the position constants, describing where to position the newly created element.
   */
  protected int insertionPolicy = INSERT_LAST;

  /**
   * The element that the newly created element is positioned relative to, as described by
   * {@link #insertionPolicy}, or <code>null</code> if the newly created element will be positioned
   * last.
   */
  protected DartElement anchorElement = null;

  /**
   * A flag indicating whether creation of a new element occurred. A request for creating a
   * duplicate element would result in this flag being set to <code>false</code>. Ensures that no
   * deltas are generated when creation does not occur.
   */
  protected boolean creationOccurred = true;

  /**
   * Initialize a newly created operation to create a Dart Element with the specified parent,
   * contained within a compilation unit.
   */
  public CreateElementInCUOperation(DartElement parentElement) {
    super(null, new DartElement[] {parentElement});
    initializeDefaultPosition();
  }

  /**
   * Instructs this operation to position the new element after the given sibling, or to add the new
   * element as the last child of its parent if <code>null</code>.
   */
  public void createAfter(DartElement sibling) {
    setRelativePosition(sibling, INSERT_AFTER);
  }

  /**
   * Instructs this operation to position the new element before the given sibling, or to add the
   * new element as the last child of its parent if <code>null</code>.
   */
  public void createBefore(DartElement sibling) {
    setRelativePosition(sibling, INSERT_BEFORE);
  }

  /**
   * Return the name of the main task of this operation for progress reporting.
   * 
   * @return the name of the main task of this operation
   */
  public abstract String getMainTaskName();

  /**
   * Possible failures:
   * <ul>
   * <li>NO_ELEMENTS_TO_PROCESS - the compilation unit supplied to the operation is
   * <code>null</code>.
   * <li>INVALID_NAME - no name, a name was null or not a valid name.
   * <li>INVALID_SIBLING - the sibling provided for positioning is not valid.
   * </ul>
   */
  @Override
  public DartModelStatus verify() {
    if (getParentElement() == null) {
      return new DartModelStatusImpl(DartModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
    }
    if (anchorElement != null) {
      DartElement domPresentParent = anchorElement.getParent();
      // if (domPresentParent.getElementType() == DartElement.IMPORT_CONTAINER)
      // {
      // domPresentParent = domPresentParent.getParent();
      // }
      if (!domPresentParent.equals(getParentElement())) {
        return new DartModelStatusImpl(DartModelStatusConstants.INVALID_SIBLING, anchorElement);
      }
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * Only allow canceling if this operation is not nested.
   */
  @Override
  protected void checkCanceled() {
    if (!isNested) {
      super.checkCanceled();
    }
  }

  /**
   * Execute the operation - generate new source for the compilation unit and save the results.
   * 
   * @throws DartModelException if the operation is unable to complete
   */
  @Override
  protected void executeOperation() throws DartModelException {
    try {
      beginTask(getMainTaskName(), getMainAmountOfWork());
      DartElementDeltaImpl delta = newDartElementDelta();
      CompilationUnitImpl unit = (CompilationUnitImpl) getCompilationUnit();
      generateNewCompilationUnitAST(unit);
      if (creationOccurred) {
        // a change has really occurred
        unit.save(null, false);
        boolean isWorkingCopy = unit.isWorkingCopy();
        if (!isWorkingCopy) {
          setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
        }
        worked(1);
        resultElements = generateResultHandles();
        // if unit is working copy, then save will have already fired the delta
        if (!isWorkingCopy
        /* && !Util.isExcluded(unit) */
        && unit.getParent().exists()) {
          for (int i = 0; i < resultElements.length; i++) {
            delta.added(resultElements[i]);
          }
          addDelta(delta);
        }
        // else unit is created outside classpath
        // non-java resource delta will be notified by delta processor
      }
    } finally {
      done();
    }
  }

  /**
   * Return the property descriptor for the element being created.
   * 
   * @return the property descriptor for the element being created
   */
  // protected abstract StructuralPropertyDescriptor
  // getChildPropertyDescriptor(DartNode parent);

  /**
   * Return an AST node for the element being created.
   * 
   * @return an AST node for the element being created
   */
  // protected abstract DartNode generateElementAST(ASTRewrite rewriter,
  // CompilationUnit cu) throws DartModelException;

  /**
   * Generate a new AST for this operation and applies it to the given cu
   */
  protected void generateNewCompilationUnitAST(CompilationUnit cu) throws DartModelException {
    DartCore.notYetImplemented();
    // cuAST = parse(cu);
    //
    // AST ast = cuAST.getAST();
    // ASTRewrite rewriter = ASTRewrite.create(ast);
    // ASTNode child = generateElementAST(rewriter, cu);
    // if (child != null) {
    // ASTNode parent = ((DartElementImpl) getParentElement()).findNode(cuAST);
    // if (parent == null)
    // parent = cuAST;
    // insertASTNode(rewriter, parent, child);
    // TextEdit edits = rewriter.rewriteAST();
    // applyTextEdit(cu, edits);
    // }
    worked(1);
  }

  /**
   * Create and return the handle for the element this operation created.
   * 
   * @return the handle for the element this operation created
   */
  protected abstract DartElement generateResultHandle();

  /**
   * Create and return the handles for the elements this operation created.
   * 
   * @return the handles for the elements this operation created
   */
  protected DartElement[] generateResultHandles() {
    return new DartElement[] {generateResultHandle()};
  }

  /**
   * Return the compilation unit in which the new element is being created.
   * 
   * @return the compilation unit in which the new element is being created
   */
  protected CompilationUnit getCompilationUnit() {
    return getCompilationUnitFor(getParentElement());
  }

  /**
   * Return the amount of work for the main task of this operation for progress reporting.
   * 
   * @return the amount of work for the main task of this operation
   */
  protected int getMainAmountOfWork() {
    return 2;
  }

  @Override
  protected ISchedulingRule getSchedulingRule() {
    IResource resource = getCompilationUnit().getResource();
    IWorkspace workspace = resource.getWorkspace();
    return workspace.getRuleFactory().modifyRule(resource);
  }

  /**
   * Set the default position in which to create the new type member. Operations that require a
   * different default position must override this method.
   */
  protected void initializeDefaultPosition() {
    // By default, the new element is positioned as the
    // last child of the parent element in which it is created.
  }

  /**
   * Insert the given child into the given AST, based on the position settings of this operation.
   * 
   * @see #createAfter(DartElement)
   * @see #createBefore(DartElement)
   */
  // protected void insertASTNode(ASTRewrite rewriter, DartNode parent, DartNode
  // child) throws DartModelException {
  // StructuralPropertyDescriptor propertyDescriptor =
  // getChildPropertyDescriptor(parent);
  // if (propertyDescriptor instanceof ChildListPropertyDescriptor) {
  // ChildListPropertyDescriptor childListPropertyDescriptor =
  // (ChildListPropertyDescriptor) propertyDescriptor;
  // ListRewrite rewrite = rewriter.getListRewrite(parent,
  // childListPropertyDescriptor);
  // switch (insertionPolicy) {
  // case INSERT_BEFORE:
  // ASTNode element = ((DartElementImpl) anchorElement).findNode(cuAST);
  // if
  // (childListPropertyDescriptor.getElementType().isAssignableFrom(element.getClass()))
  // rewrite.insertBefore(child, element, null);
  // else
  // // case of an empty import list: the anchor element is the top level type
  // and cannot be used in insertBefore as it is not the same type
  // rewrite.insertLast(child, null);
  // break;
  // case INSERT_AFTER:
  // element = ((DartElementImpl) anchorElement).findNode(cuAST);
  // if
  // (childListPropertyDescriptor.getElementType().isAssignableFrom(element.getClass()))
  // rewrite.insertAfter(child, element, null);
  // else
  // // case of an empty import list: the anchor element is the top level type
  // and cannot be used in insertAfter as it is not the same type
  // rewrite.insertLast(child, null);
  // break;
  // case INSERT_LAST:
  // rewrite.insertLast(child, null);
  // break;
  // }
  // } else {
  // rewriter.set(parent, propertyDescriptor, child, null);
  // }
  // }

  // protected DartUnit parse(CompilationUnit cu) throws DartModelException {
  // // ensure cu is consistent (noop if already consistent)
  // cu.makeConsistent(progressMonitor);
  // // create an AST for the compilation unit
  // ASTParser parser = ASTParser.newParser(AST.JLS3);
  // parser.setSource(cu);
  // return (DartUnit) parser.createAST(progressMonitor);
  // }

  /**
   * Set the name of the <code>DOMNode</code> that will be used to create this new element. Used by
   * the <code>CopyElementsOperation</code> for renaming. Only used for
   * <code>CreateTypeMemberOperation</code>.
   * 
   * @param newName the name that will be used to create the new element
   */
  protected void setAlteredName(String newName) {
    // implementation in CreateTypeMemberOperation
  }

  /**
   * Instructs this operation to position the new element relative to the given sibling, or to add
   * the new element as the last child of its parent if <code>null</code>. The <code>position</code>
   * must be one of the position constants.
   */
  protected void setRelativePosition(DartElement sibling, int policy)
      throws IllegalArgumentException {
    if (sibling == null) {
      anchorElement = null;
      insertionPolicy = INSERT_LAST;
    } else {
      anchorElement = sibling;
      insertionPolicy = policy;
    }
  }
}

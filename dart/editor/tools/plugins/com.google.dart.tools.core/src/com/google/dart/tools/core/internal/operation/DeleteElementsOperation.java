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

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.AST;
import com.google.dart.tools.core.dom.rewrite.ASTRewrite;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.model.RegionImpl;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.Region;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.text.edits.TextEdit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Instances of the class <code>DeleteElementsOperation</code> implement an operation that deletes a
 * collection of elements (and all of their children). If an element does not exist, it is ignored.
 * <p>
 * NOTE: This operation only deletes elements contained within leaf resources - that is, elements
 * within compilation units. To delete a compilation unit or a package, etc (which have an actual
 * resource), a DeleteResourcesOperation should be used.
 */
public class DeleteElementsOperation extends MultiOperation {
  /**
   * The elements this operation processes grouped by compilation unit
   * 
   * @see #processElements() Keys are compilation units, values are <code>IRegion</code>s of
   *      elements to be processed in each compilation unit.
   */
  protected Map<CompilationUnit, Region> childrenToRemove;

  /**
   * The <code>ASTParser</code> used to manipulate the source code of <code>ICompilationUnit</code>.
   */
  // protected ASTParser parser;

  /**
   * When executed, this operation will delete the given elements. The elements to delete cannot be
   * <code>null</code> or empty, and must be contained within a compilation unit.
   */
  public DeleteElementsOperation(DartElement[] elementsToDelete, boolean force) {
    super(elementsToDelete, force);
    initASTParser();
  }

  @Override
  protected String getMainTaskName() {
    return Messages.operation_deleteElementProgress;
  }

  @Override
  protected ISchedulingRule getSchedulingRule() {
    if (this.elementsToProcess != null && this.elementsToProcess.length == 1) {
      IResource resource = this.elementsToProcess[0].getResource();
      if (resource != null) {
        return ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(resource);
      }
    }
    return super.getSchedulingRule();
  }

  /**
   * Groups the elements to be processed by their compilation unit. If parent/child combinations are
   * present, children are discarded (only the parents are processed). Removes any duplicates
   * specified in elements to be processed.
   */
  protected void groupElements() throws DartModelException {
    childrenToRemove = new HashMap<CompilationUnit, Region>(1);
    int uniqueCUs = 0;
    for (int i = 0, length = this.elementsToProcess.length; i < length; i++) {
      DartElement e = this.elementsToProcess[i];
      CompilationUnit cu = getCompilationUnitFor(e);
      if (cu == null) {
        throw new DartModelException(new DartModelStatusImpl(DartModelStatusConstants.READ_ONLY, e));
      } else {
        Region region = childrenToRemove.get(cu);
        if (region == null) {
          region = new RegionImpl();
          childrenToRemove.put(cu, region);
          uniqueCUs += 1;
        }
        region.add(e);
      }
    }
    this.elementsToProcess = new DartElement[uniqueCUs];
    Iterator<CompilationUnit> iter = childrenToRemove.keySet().iterator();
    int i = 0;
    while (iter.hasNext()) {
      this.elementsToProcess[i++] = iter.next();
    }
  }

  /**
   * Deletes this element from its compilation unit.
   */
  @Override
  protected void processElement(DartElement element) throws DartModelException {
    CompilationUnitImpl cu = (CompilationUnitImpl) element;

    // keep track of the import statements - if all are removed, delete
    // the import container (and report it in the delta)
    DartCore.notYetImplemented();
    // int numberOfImports = cu.getImports().length;

    DartElementDeltaImpl delta = new DartElementDeltaImpl(cu);
    DartElement[] cuElements = childrenToRemove.get(cu).getElements();
    for (int i = 0, length = cuElements.length; i < length; i++) {
      DartElement e = cuElements[i];
      if (e.exists()) {
        deleteElement(e, cu);
        delta.removed(e);
        // if (e.getElementType() == DartElement.IMPORT_DECLARATION) {
        // numberOfImports--;
        // if (numberOfImports == 0) {
        // delta.removed(cu.getImportContainer());
        // }
        // }
      }
    }
    if (delta.getAffectedChildren().length > 0) {
      cu.save(getSubProgressMonitor(1), this.force);
      if (!cu.isWorkingCopy()) {
        // if unit is working copy, then save will have already fired the delta
        addDelta(delta);
        setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
      }
    }
  }

  /**
   * @see MultiOperation This method first group the elements by <code>ICompilationUnit</code>, and
   *      then processes the <code>ICompilationUnit</code>.
   */
  @Override
  protected void processElements() throws DartModelException {
    groupElements();
    super.processElements();
  }

  @Override
  protected void verify(DartElement element) throws DartModelException {
    DartElement[] children = childrenToRemove.get(element).getElements();
    for (int i = 0; i < children.length; i++) {
      DartElement child = children[i];
      if (child.getCorrespondingResource() != null) {
        error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, child);
      }
      if (child.isReadOnly()) {
        error(DartModelStatusConstants.READ_ONLY, child);
      }
    }
  }

  private void deleteElement(DartElement elementToRemove, CompilationUnit cu)
      throws DartModelException {
    // ensure cu is consistent (noop if already consistent)
    cu.makeConsistent(this.progressMonitor);
    DartUnit astCU = DartCompilerUtilities.parseUnit(cu, null);
    DartNode node = ((DartElementImpl) elementToRemove).findNode(astCU);
    if (node == null) {
      Assert.isTrue(
          false,
          "Failed to locate " + elementToRemove.getElementName() + " in " + cu.getElementName()); //$NON-NLS-1$//$NON-NLS-2$
    }
    ASTRewrite rewriter = ASTRewrite.create(new AST());
    rewriter.remove(node, null);
    TextEdit edits = rewriter.rewriteAST();
    applyTextEdit(cu, edits);
  }

  private void initASTParser() {
    // this.parser = ASTParser.newParser(AST.JLS3);
  }
}

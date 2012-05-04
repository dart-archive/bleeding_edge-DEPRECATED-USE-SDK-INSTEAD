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

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.dom.rewrite.ASTRewrite;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.Type;

/**
 * This operation creates a class or interface.
 * <p>
 * Required Attributes:
 * <ul>
 * <li>Parent element - must be a compilation unit.
 * <li>The source code for the type. No verification of the source is performed.
 * </ul>
 */
public class CreateTypeOperation extends CreateTypeMemberOperation {
  /**
   * When executed, this operation will create a type unit in the given parent element (a
   * compilation unit, type)
   */
  public CreateTypeOperation(DartElement parentElement, String source, boolean force) {
    super(parentElement, source, force);
  }

  @Override
  public String getMainTaskName() {
    return Messages.operation_createTypeProgress;
  }

  @Override
  public DartModelStatus verify() {
    DartModelStatus status = super.verify();
    if (!status.isOK()) {
      return status;
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  @Override
  protected DartNode generateElementAST(ASTRewrite rewriter, CompilationUnit cu)
      throws DartModelException {
    DartNode node = super.generateElementAST(rewriter, cu);
    if (!(node instanceof DartClass)) {
      throw new DartModelException(new DartModelStatusImpl(
          DartModelStatusConstants.INVALID_CONTENTS));
    }
    return node;
  }

  @Override
  protected DartElement generateResultHandle() {
    DartElement parent = getParentElement();
    if (parent.getElementType() == DartElement.COMPILATION_UNIT) {
      return ((CompilationUnit) parent).getType(getASTNodeName());
    }
    return null;
  }

  /**
   * Returns the <code>Type</code> the member is to be created in.
   */
  @Override
  protected Type getType() {
    return null;
  }

  @Override
  protected DartIdentifier rename(DartNode node, DartIdentifier newName) {
    DartClass type = (DartClass) node;
    DartIdentifier oldName = type.getName();
    type.setName(newName);
    return oldName;
  }

  @Override
  protected DartModelStatus verifyNameCollision() {
    DartElement parent = getParentElement();
    if (parent.getElementType() == DartElement.COMPILATION_UNIT) {
      String typeName = getASTNodeName();
      if (((CompilationUnit) parent).getType(typeName).exists()) {
        return new DartModelStatusImpl(DartModelStatusConstants.NAME_COLLISION, Messages.bind(
            Messages.status_nameCollision,
            typeName));
      }
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  private String getASTNodeName() {
    return ((DartClass) this.createdNode).getName().getName();
  }
}

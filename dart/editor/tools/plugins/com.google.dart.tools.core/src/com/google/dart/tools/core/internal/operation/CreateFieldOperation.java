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

import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.rewrite.ASTRewrite;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Type;

/**
 * Instances of the class <code>CreateFieldOperation</code> implement an operation that creates a
 * field declaration in a type.
 * <p>
 * Required Attributes:
 * <ul>
 * <li>Containing Type
 * <li>The source code for the declaration. No verification of the source is performed.
 * </ul>
 */
public class CreateFieldOperation extends CreateTypeMemberOperation {
  /**
   * When executed, this operation will create a field with the given name in the given type with
   * the specified source.
   * <p>
   * By default the new field is positioned after the last existing field declaration, or as the
   * first member in the type if there are no field declarations.
   */
  public CreateFieldOperation(Type parentElement, String source, boolean force) {
    super(parentElement, source, force);
  }

  @Override
  public String getMainTaskName() {
    return Messages.operation_createFieldProgress;
  }

  @Override
  protected DartNode generateElementAST(ASTRewrite rewriter, CompilationUnit cu)
      throws DartModelException {
    DartNode node = super.generateElementAST(rewriter, cu);
    if (!(node instanceof DartFieldDefinition)) {
      throw new DartModelException(new DartModelStatusImpl(
          DartModelStatusConstants.INVALID_CONTENTS));
    }
    return node;
  }

  @Override
  protected DartElement generateResultHandle() {
    return getType().getField(getASTNodeName());
  }

  // private VariableDeclarationFragment getFragment(DartNode node) {
  // Iterator fragments = ((FieldDeclaration) node).fragments().iterator();
  // if (anchorElement != null) {
  // VariableDeclarationFragment fragment = null;
  // String fragmentName = anchorElement.getElementName();
  // while (fragments.hasNext()) {
  // fragment = (VariableDeclarationFragment) fragments.next();
  // if (fragment.getName().getIdentifier().equals(fragmentName)) {
  // return fragment;
  // }
  // }
  // return fragment;
  // } else {
  // return (VariableDeclarationFragment) fragments.next();
  // }
  // }

  /**
   * By default the new field is positioned after the last existing field declaration, or as the
   * first member in the type if there are no field declarations.
   */
  @Override
  protected void initializeDefaultPosition() {
    Type parentElement = getType();
    try {
      Field[] fields = parentElement.getFields();
      if (fields != null && fields.length > 0) {
        final Field lastField = fields[fields.length - 1];
        // if (parentElement.isEnum()) {
        // Field field = lastField;
        // if (!field.isEnumConstant()) {
        // createAfter(lastField);
        // }
        // } else {
        createAfter(lastField);
        // }
      } else {
        DartElement[] elements = parentElement.getChildren();
        if (elements != null && elements.length > 0) {
          createBefore(elements[0]);
        }
      }
    } catch (DartModelException e) {
      // type doesn't exist: ignore
    }
  }

  @Override
  protected DartIdentifier rename(DartNode node, DartIdentifier newName) {
    DartCore.notYetImplemented();
    return null;
    // VariableDeclarationFragment fragment = getFragment(node);
    // DartIdentifier oldName = fragment.getName();
    // fragment.setName(newName);
    // return oldName;
  }

  @Override
  protected DartModelStatus verifyNameCollision() {
    if (createdNode != null) {
      Type type = getType();
      String fieldName = getASTNodeName();
      if (type.getField(fieldName).exists()) {
        return new DartModelStatusImpl(DartModelStatusConstants.NAME_COLLISION, Messages.bind(
            Messages.status_nameCollision,
            fieldName));
      }
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  private String getASTNodeName() {
    if (alteredName != null) {
      return alteredName;
    }
    DartCore.notYetImplemented();
    return null;
    // return getFragment(createdNode).getName().getIdentifier();
  }
}

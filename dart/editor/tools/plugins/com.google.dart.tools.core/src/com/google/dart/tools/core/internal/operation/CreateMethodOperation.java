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
import com.google.dart.tools.core.model.Type;

/**
 * Instances of the class <code>CreateMethodOperation</code> implement an operation that creates an
 * instance method.
 * <p>
 * Required Attributes:
 * <ul>
 * <li>Containing type
 * <li>The source code for the method. No verification of the source is performed.
 * </ul>
 */
public class CreateMethodOperation extends CreateTypeMemberOperation {
  protected String[] parameterTypes;

  /**
   * When executed, this operation will create a method in the given type with the specified source.
   */
  public CreateMethodOperation(Type parentElement, String source, boolean force) {
    super(parentElement, source, force);
  }

  @Override
  public String getMainTaskName() {
    return Messages.operation_createMethodProgress;
  }

  /**
   * Return the type signatures of the parameter types of the current <code>MethodDeclaration</code>
   */
  protected String[] convertASTMethodTypesToSignatures() {
    if (parameterTypes == null) {
      DartCore.notYetImplemented();
      // if (createdNode != null) {
      // DartMethodDefinition methodDeclaration = (DartMethodDefinition)
      // createdNode;
      // List parameters = methodDeclaration.parameters();
      // int size = parameters.size();
      // parameterTypes = new String[size];
      // Iterator iterator = parameters.iterator();
      // // convert the AST types to signatures
      // for (int i = 0; i < size; i++) {
      // SingleVariableDeclaration parameter = (SingleVariableDeclaration)
      // iterator.next();
      // String typeSig = Util.getSignature(parameter.getType());
      // int extraDimensions = parameter.getExtraDimensions();
      // if (methodDeclaration.isVarargs() && i == size-1)
      // extraDimensions++;
      // parameterTypes[i] = Signature.createArraySignature(typeSig,
      // extraDimensions);
      // }
      // }
    }
    return parameterTypes;
  }

  @Override
  protected DartNode generateElementAST(ASTRewrite rewriter, CompilationUnit cu)
      throws DartModelException {
    DartNode node = super.generateElementAST(rewriter, cu);
    DartCore.notYetImplemented();
    // if (node.getNodeType() != DartNode.METHOD_DECLARATION) {
    // throw new DartModelException(new
    // DartModelStatusImpl(DartModelStatusConstants.INVALID_CONTENTS));
    // }
    return node;
  }

  @Override
  protected DartElement generateResultHandle() {
    String[] types = convertASTMethodTypesToSignatures();
    String name = getASTNodeName();
    return getType().getMethod(name, types);
  }

  @Override
  protected DartIdentifier rename(DartNode node, DartIdentifier newName) {
    DartCore.notYetImplemented();
    return null;
    // DartMethodDefinition method = (DartMethodDefinition) node;
    // DartSimpleName oldName = method.getName();
    // method.setName(newName);
    // return oldName;
  }

  @Override
  protected DartModelStatus verifyNameCollision() {
    if (createdNode != null) {
      Type type = getType();
      String name;
      DartCore.notYetImplemented();
      // if (((DartMethodDefinition) createdNode).isConstructor()) {
      // name = type.getElementName();
      // } else {
      name = getASTNodeName();
      // }
      String[] types = convertASTMethodTypesToSignatures();
      if (type.getMethod(name, types).exists()) {
        return new DartModelStatusImpl(DartModelStatusConstants.NAME_COLLISION, Messages.bind(
            Messages.status_nameCollision,
            name));
      }
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  private String getASTNodeName() {
    DartCore.notYetImplemented();
    return null;
    // return ((DartMethodDefinition) createdNode).getName().getIdentifier();
  }
}

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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.FieldNodeElement;
import com.google.dart.compiler.resolver.FunctionAliasElement;
import com.google.dart.compiler.resolver.MethodNodeElement;
import com.google.dart.compiler.resolver.VariableElement;

/**
 * Visit elements of top-level members along with field and method definitions.
 */
public class ElementVisitor extends ASTVisitor<Void> {

  public void element(ClassElement element) {
  }

  public void element(FieldNodeElement element) {
  }

  public void element(FunctionAliasElement element) {
  }

  public void element(MethodNodeElement element) {
  }

  public void element(VariableElement element) {
  }

  @Override
  public Void visitClass(DartClass node) {
    element(node.getElement());
    return super.visitClass(node);
  }

  @Override
  public Void visitField(DartField node) {
    element(node.getElement());
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    element(node.getElement());
    return null;
  }

  @Override
  public Void visitMethodDefinition(DartMethodDefinition node) {
    if (node.getName() instanceof DartIdentifier) {
      element(node.getElement());
    }
    return null;
  }

  @Override
  public Void visitVariable(DartVariable node) {
    element(node.getElement());
    return null;
  }
}

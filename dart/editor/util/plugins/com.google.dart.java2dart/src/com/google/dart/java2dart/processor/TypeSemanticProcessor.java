/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.java2dart.processor;

import com.google.common.base.Objects;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.java2dart.Context;

import static com.google.dart.java2dart.util.AstFactory.asExpression;
import static com.google.dart.java2dart.util.AstFactory.parenthesizedExpression;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * {@link SemanticProcessor} for Dart vs. Java type differences.
 */
public class TypeSemanticProcessor extends SemanticProcessor {
  public TypeSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitVariableDeclaration(VariableDeclaration node) {
        Expression initializer = node.getInitializer();
        if (initializer instanceof ConditionalExpression) {
          ITypeBinding reqType = context.getNodeTypeBinding(node);
          ITypeBinding initType = context.getNodeTypeBinding(initializer);
          if (!Objects.equal(reqType, initType)) {
            TypeName reqTypeName = ((VariableDeclarationList) node.getParent()).getType();
            node.setInitializer(asExpression(parenthesizedExpression(initializer), reqTypeName));
          }
        }
        return super.visitVariableDeclaration(node);
      }
    });
  }
}

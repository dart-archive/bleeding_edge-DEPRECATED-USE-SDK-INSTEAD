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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;

import static com.google.dart.java2dart.util.ASTFactory.assignmentExpression;
import static com.google.dart.java2dart.util.ASTFactory.propertyAccess;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * {@link SemanticProcessor} for converting <code>getX()</code> or <code>setX(x)</code> into getter
 * or setter.
 */
public class PropertySemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new PropertySemanticProcessor();

  private static boolean isValidSetterType(TypeName type) {
    return type.getName().getName().equals("void");
  }

  @Override
  public void process(final Context context, final CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitFieldDeclaration(FieldDeclaration node) {
        if (context.getPrivateClassMembers().contains(node)) {
          for (VariableDeclaration field : node.getFields().getVariables()) {
            SimpleIdentifier name = field.getName();
            context.renameIdentifier(name, "_" + name.getName());
          }
        }
        return super.visitFieldDeclaration(node);
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        if (node.getName() instanceof SimpleIdentifier && node.getParameters() != null) {
          SimpleIdentifier nameNode = node.getName();
          String name = context.getIdentifierOriginalName(nameNode);
          List<FormalParameter> parameters = node.getParameters().getParameters();
          // getter
          if (name.startsWith("get") && parameters.isEmpty()) {
            String propertyName = StringUtils.uncapitalize(name.substring("get".length()));
            // rename references
            context.renameIdentifier(nameNode, propertyName);
            // replace MethodInvocation with PropertyAccess
            for (SimpleIdentifier reference : context.getReferences(nameNode)) {
              if (reference.getParent() instanceof MethodInvocation) {
                MethodInvocation invocation = (MethodInvocation) reference.getParent();
                if (invocation.getMethodName() == reference) {
                  Expression invocationTarget = invocation.getTarget();
                  // prepare replacement
                  Expression replacement;
                  if (invocationTarget != null) {
                    replacement = propertyAccess(invocationTarget, reference);
                  } else {
                    replacement = reference;
                  }
                  // do replace
                  replaceNode(invocation, replacement);
                }
              }
            }
            // convert method to getter
            node.setPropertyKeyword(token(Keyword.GET));
            node.setParameters(null);
          }
          // setter
          if (name.startsWith("set") && parameters.size() == 1
              && isValidSetterType(node.getReturnType())) {
            String propertyName = StringUtils.uncapitalize(name.substring("set".length()));
            // rename references
            context.renameIdentifier(nameNode, propertyName);
            // replace MethodInvocation with AssignmentExpression
            for (SimpleIdentifier reference : context.getReferences(nameNode)) {
              if (reference.getParent() instanceof MethodInvocation) {
                MethodInvocation invocation = (MethodInvocation) reference.getParent();
                if (invocation.getMethodName() == reference) {
                  Expression invocationTarget = invocation.getTarget();
                  List<Expression> arguments = invocation.getArgumentList().getArguments();
                  // prepare assignment target
                  Expression assignmentTarget;
                  if (invocationTarget != null) {
                    assignmentTarget = propertyAccess(invocationTarget, reference);
                  } else {
                    assignmentTarget = reference;
                  }
                  // do replace
                  replaceNode(
                      invocation,
                      assignmentExpression(assignmentTarget, TokenType.EQ, arguments.get(0)));
                }
              }
            }
            // convert method to setter
            node.setPropertyKeyword(token(Keyword.SET));
          }
        }
        return super.visitMethodDeclaration(node);
      }
    });
  }
}

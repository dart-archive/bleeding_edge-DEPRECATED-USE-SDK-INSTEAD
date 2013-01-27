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
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.JavaUtils;
import com.google.dart.java2dart.util.TokenFactory;

import static com.google.dart.java2dart.util.ASTFactory.assignmentExpression;
import static com.google.dart.java2dart.util.ASTFactory.functionExpression;
import static com.google.dart.java2dart.util.ASTFactory.indexExpression;
import static com.google.dart.java2dart.util.ASTFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.ASTFactory.methodInvocation;
import static com.google.dart.java2dart.util.ASTFactory.propertyAccess;
import static com.google.dart.java2dart.util.ASTFactory.simpleIdentifier;
import static com.google.dart.java2dart.util.ASTFactory.typeName;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

/**
 * {@link SemanticProcessor} for Java <code>java.util</code> collections.
 */
public class CollectionSemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new CollectionSemanticProcessor();

  @Override
  public void process(final Context context, final CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitCompilationUnit(CompilationUnit node) {
        List<CompilationUnitMember> declarations = Lists.newArrayList(unit.getDeclarations());
        for (CompilationUnitMember member : declarations) {
          member.accept(this);
        }
        return null;
      }

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        Object binding = context.getNodeBinding(node);
        // translate java.util.Comparator to function expression
        if (binding instanceof IMethodBinding) {
          IMethodBinding methodBinding = (IMethodBinding) binding;
          ITypeBinding declaringClass = methodBinding.getDeclaringClass();
          if (methodBinding.isConstructor() && declaringClass.isAnonymous()) {
            ITypeBinding[] intfs = declaringClass.getInterfaces();
            if (intfs.length == 1
                && JavaUtils.getQualifiedName(intfs[0]).equals("java.util.Comparator")) {
              ClassDeclaration innerClass = context.getAnonymousDeclaration(node);
              if (innerClass != null) {
                unit.getDeclarations().remove(innerClass);
                List<ClassMember> innerMembers = innerClass.getMembers();
                MethodDeclaration compareMethod = (MethodDeclaration) innerMembers.get(0);
                FunctionExpression functionExpression = functionExpression(
                    compareMethod.getParameters(),
                    compareMethod.getBody());
                // don't add ";" at the end of ExpressionFunctionBody of the last first field
                if (node.getParent() instanceof VariableDeclaration
                    && node.getParent().getParent() instanceof VariableDeclarationList) {
                  VariableDeclarationList variableDeclarationList = (VariableDeclarationList) node.getParent().getParent();
                  int index = variableDeclarationList.getVariables().indexOf(node.getParent());
                  if (index == variableDeclarationList.getVariables().size() - 1) {
                    if (compareMethod.getBody() instanceof ExpressionFunctionBody) {
                      ExpressionFunctionBody expressionFunctionBody = (ExpressionFunctionBody) compareMethod.getBody();
                      expressionFunctionBody.setSemicolon(null);
                    }
                  }
                }
                // do replace
                replaceNode(node, functionExpression);
              }
            }
          }
        }
        // done
        return super.visitInstanceCreationExpression(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        List<Expression> arguments = node.getArgumentList().getArguments();
        SimpleIdentifier nameNode = node.getMethodName();
        if (isMethodInClass(node, "size", "java.util.Collection")) {
          replaceNode(node, propertyAccess(node.getTarget(), nameNode));
          nameNode.setToken(token("length"));
          return null;
        }
        if (isMethodInClass(node, "isEmpty", "java.util.Collection")) {
          replaceNode(node, propertyAccess(node.getTarget(), nameNode));
          return null;
        }
        if (isMethodInClass(node, "get", "java.util.List")
            || isMethodInClass(node, "get", "java.util.Map")) {
          replaceNode(node, indexExpression(node.getTarget(), arguments.get(0)));
          return null;
        }
        if (isMethodInClass(node, "toArray", "java.util.List")) {
          replaceNode(
              node,
              instanceCreationExpression(Keyword.NEW, typeName("List"), "from", node.getTarget()));
          return null;
        }
        if (isMethodInClass(node, "put", "java.util.Map")) {
          Assert.isTrue(node.getParent() instanceof ExpressionStatement);
          IndexExpression indexExpression = indexExpression(node.getTarget(), arguments.get(0));
          AssignmentExpression assignment = assignmentExpression(
              indexExpression,
              TokenType.EQ,
              arguments.get(1));
          replaceNode(node, assignment);
          return null;
        }
        if (isMethodInClass(node, "remove", "java.util.List")) {
          nameNode.setToken(TokenFactory.token("removeAt"));
          return null;
        }
        if (isMethodInClass(node, "sort", "java.util.Arrays")) {
          replaceNode(node, methodInvocation(arguments.get(0), "sort"));
          return null;
        }
        return null;
      }

      @Override
      public Void visitTypeName(TypeName node) {
        if (node.getName() instanceof SimpleIdentifier) {
          SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
          String name = nameNode.getName();
          if ("ArrayList".equals(name)) {
            replaceNode(nameNode, simpleIdentifier("List"));
            return null;
          }
        }
        return super.visitTypeName(node);
      }

      private boolean isMethodInClass(MethodInvocation node, String reqName, String reqClassName) {
        String name = node.getMethodName().getName();
        return Objects.equal(name, reqName)
            && JavaUtils.isMethodInClass(context.getNodeBinding(node), reqClassName);
      }
    });
  }
}

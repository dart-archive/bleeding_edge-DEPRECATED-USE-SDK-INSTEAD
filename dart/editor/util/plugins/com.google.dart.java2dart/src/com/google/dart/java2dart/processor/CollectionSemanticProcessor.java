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
import com.google.common.collect.ImmutableList;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.ASTFactory;
import com.google.dart.java2dart.util.JavaUtils;

import org.eclipse.core.runtime.Assert;

import java.util.List;

/**
 * {@link SemanticProcessor} for Java <code>java.util</code> collections.
 */
public class CollectionSemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new CollectionSemanticProcessor();

  @Override
  public void process(final Context context, CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        List<Expression> arguments = node.getArgumentList().getArguments();
        SimpleIdentifier nameNode = node.getMethodName();
        if (isMethodInClass(node, "size", "java.util.Collection")) {
          replaceNode(node, new PropertyAccess(
              node.getTarget(),
              new Token(TokenType.PERIOD, 0),
              nameNode));
          nameNode.setToken(ASTFactory.identifierToken("length"));
          return null;
        }
        if (isMethodInClass(node, "isEmpty", "java.util.Collection")) {
          replaceNode(node, new PropertyAccess(
              node.getTarget(),
              new Token(TokenType.PERIOD, 0),
              nameNode));
          return null;
        }
        if (isMethodInClass(node, "get", "java.util.List")
            || isMethodInClass(node, "get", "java.util.Map")) {
          replaceNode(node, new IndexExpression(node.getTarget(), null, arguments.get(0), null));
          return null;
        }
        if (isMethodInClass(node, "toArray", "java.util.List")) {
          replaceNode(
              node,
              new InstanceCreationExpression(ASTFactory.TOKEN_NEW, new ConstructorName(
                  ASTFactory.typeName("List"),
                  null,
                  ASTFactory.simpleIdentifier("from")), new ArgumentList(
                  null,
                  ImmutableList.of(node.getTarget()),
                  null)));
          return null;
        }
        if (isMethodInClass(node, "put", "java.util.Map")) {
          Assert.isTrue(node.getParent() instanceof ExpressionStatement);
          IndexExpression indexExpression = new IndexExpression(
              node.getTarget(),
              null,
              arguments.get(0),
              null);
          AssignmentExpression assignment = new AssignmentExpression(indexExpression, new Token(
              TokenType.EQ,
              0), arguments.get(1));
          replaceNode(node, assignment);
          return null;
        }
        if (isMethodInClass(node, "remove", "java.util.List")) {
          nameNode.setToken(ASTFactory.identifierToken("removeAt"));
          return null;
        }
        if (isMethodInClass(node, "sort", "java.util.Arrays")) {
          replaceNode(
              node,
              new MethodInvocation(
                  arguments.get(0),
                  null,
                  ASTFactory.simpleIdentifier("sort"),
                  new ArgumentList(null, null, null)));
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
            replaceNode(nameNode, ASTFactory.simpleIdentifier("List"));
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

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
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.ASTFactory;
import com.google.dart.java2dart.util.JavaUtils;

/**
 * {@link SemanticProcessor} for Java <code>Object</code>.
 */
public class ObjectSemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new ObjectSemanticProcessor();

  @Override
  public void process(final Context context, CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        if (node.getName() instanceof SimpleIdentifier) {
          String name = ((SimpleIdentifier) node.getName()).getName();
          if (name.equals("hashCode")) {
            node.setOperatorKeyword(new KeywordToken(Keyword.GET, 0));
            node.setParameters(null);
          }
        }
        return super.visitMethodDeclaration(node);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        SimpleIdentifier nameNode = node.getMethodName();
        String name = nameNode.getName();
        if ("hashCode".equals(name) || isMethodInClass(node, "length", "java.lang.String")
            || isMethodInClass(node, "values", "java.lang.Enum")) {
          Token period = node.getTarget() != null ? new Token(TokenType.PERIOD, 0) : null;
          replaceNode(node, new PropertyAccess(node.getTarget(), period, nameNode));
          return null;
        }
        if (isMethodInClass(node, "charAt", "java.lang.String")) {
          nameNode.setToken(ASTFactory.identifierToken("charCodeAt"));
          return null;
        }
        if (isMethodInClass(node, "format", "java.lang.String")) {
          replaceNode(node.getTarget(), ASTFactory.simpleIdentifier("JavaString"));
          return null;
        }
        if (isMethodInClass(node, "valueOf", "java.lang.Integer")) {
          replaceNode(node, node.getArgumentList().getArguments().get(0));
          return null;
        }
        if (isMethodInClass(node, "append", "java.lang.StringBuilder")) {
          replaceNode(nameNode, ASTFactory.simpleIdentifier("add"));
          return null;
        }
        return super.visitMethodInvocation(node);
      }

      @Override
      public Void visitTypeName(TypeName node) {
        if (node.getName() instanceof SimpleIdentifier) {
          SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
          String name = nameNode.getName();
          if (name.equals("StringBuilder")) {
            replaceNode(nameNode, ASTFactory.simpleIdentifier("StringBuffer"));
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

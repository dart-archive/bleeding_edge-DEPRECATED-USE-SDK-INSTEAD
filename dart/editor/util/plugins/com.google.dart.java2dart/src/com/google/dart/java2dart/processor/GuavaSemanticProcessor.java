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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;

import static com.google.dart.java2dart.util.AstFactory.binaryExpression;
import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.AstFactory.listLiteral;
import static com.google.dart.java2dart.util.AstFactory.mapLiteral;
import static com.google.dart.java2dart.util.AstFactory.typeName;
import static com.google.dart.java2dart.util.TokenFactory.token;

import java.util.List;

/**
 * {@link SemanticProcessor} for Google Guava.
 */
public class GuavaSemanticProcessor extends SemanticProcessor {
  private static boolean isNegationParent(AstNode node) {
    if (node.getParent() instanceof PrefixExpression) {
      PrefixExpression prefixExpression = (PrefixExpression) node.getParent();
      return prefixExpression.getOperator().getType() == TokenType.BANG;
    }
    return false;
  }

  public GuavaSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(final CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        List<Expression> args = node.getArgumentList().getArguments();
        if (isMethodInClass(node, "equal", "com.google.common.base.Objects")) {
          AstNode toReplace = node;
          TokenType operator = TokenType.EQ_EQ;
          if (isNegationParent(node)) {
            operator = TokenType.BANG_EQ;
            toReplace = node.getParent();
          }
          replaceNode(toReplace, binaryExpression(args.get(0), operator, args.get(1)));
          return null;
        }
        if (isMethodInClass(node, "hashCode", "com.google.common.base.Objects")) {
          replaceNode(node.getTarget(), identifier("JavaArrays"));
          node.getMethodName().setToken(token("makeHashCode"));
          return null;
        }
        if (isMethodInClass(node, "of", "com.google.common.collect.ImmutableMap")) {
          replaceNode(node, instanceCreationExpression(Keyword.NEW, typeName("Map")));
          return null;
        }
        if (isMethodInClass(node, "newArrayList", "com.google.common.collect.Lists")) {
          replaceNode(node, listLiteral());
          return null;
        }
        if (isMethodInClass(node, "newLinkedList", "com.google.common.collect.Lists")) {
          replaceNode(node, instanceCreationExpression(Keyword.NEW, typeName("Queue")));
          return null;
        }
        if (isMethodInClass(node, "newHashMap", "com.google.common.collect.Maps")) {
          replaceNode(node, mapLiteral());
          return null;
        }
        if (isMethodInClass(node, "difference", "com.google.common.collect.Sets")) {
          node.setTarget(args.get(0));
          args.remove(0);
          return null;
        }
        if (isMethodInClass(node, "newHashSet", "com.google.common.collect.Sets")) {
          replaceNode(node, instanceCreationExpression(Keyword.NEW, typeName("Set")));
          return null;
        }
        return null;
      }
    });
  }
}

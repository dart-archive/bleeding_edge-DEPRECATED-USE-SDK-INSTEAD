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
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.JavaUtils;

import static com.google.dart.java2dart.util.ASTFactory.binaryExpression;
import static com.google.dart.java2dart.util.ASTFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.ASTFactory.typeName;

import java.util.List;

/**
 * {@link SemanticProcessor} for Google Guava.
 */
public class GuavaSemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new GuavaSemanticProcessor();

  @Override
  public void process(final Context context, final CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        List<Expression> args = node.getArgumentList().getArguments();
        if (isMethodInClass(node, "equal", "com.google.common.base.Objects")) {
          replaceNode(node, binaryExpression(args.get(0), TokenType.EQ_EQ, args.get(1)));
          return null;
        }
        if (isMethodInClass(node, "of", "com.google.common.collect.ImmutableMap")) {
          replaceNode(node, instanceCreationExpression(Keyword.NEW, typeName("Map")));
          return null;
        }
        if (isMethodInClass(node, "newHashSet", "com.google.common.collect.Sets")) {
          replaceNode(node, instanceCreationExpression(Keyword.NEW, typeName("Set")));
          return null;
        }
        return null;
      }

      private boolean isMethodInClass(MethodInvocation node, String reqName, String reqClassName) {
        String name = node.getMethodName().getName();
        return Objects.equal(name, reqName)
            && JavaUtils.isMethodInClass(context.getNodeBinding(node), reqClassName);
      }
    });
  }
}

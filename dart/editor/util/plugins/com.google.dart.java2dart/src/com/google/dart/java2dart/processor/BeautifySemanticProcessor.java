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
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;

import static com.google.dart.java2dart.util.TokenFactory.token;

/**
 * {@link SemanticProcessor} for making Dart AST cleaner and nicer.
 */
public class BeautifySemanticProcessor extends SemanticProcessor {
  public static final SemanticProcessor INSTANCE = new BeautifySemanticProcessor();

  @Override
  public void process(final Context context, CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitPrefixExpression(PrefixExpression node) {
        super.visitPrefixExpression(node);
        if (node.getOperator().getType() == TokenType.BANG) {
          if (node.getOperand() instanceof ParenthesizedExpression) {
            ParenthesizedExpression parExpression = (ParenthesizedExpression) node.getOperand();
            if (parExpression.getExpression() instanceof IsExpression) {
              IsExpression isExpression = (IsExpression) parExpression.getExpression();
              if (isExpression.getNotOperator() == null) {
                isExpression.setNotOperator(token(TokenType.BANG));
                replaceNode(node, isExpression);
              }
            }
          }
        }
        return null;
      }
    });
  }
}

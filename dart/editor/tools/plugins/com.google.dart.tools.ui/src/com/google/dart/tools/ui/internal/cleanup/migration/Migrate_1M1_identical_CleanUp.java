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
package com.google.dart.tools.ui.internal.cleanup.migration;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartDoubleLiteral;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartNullLiteral;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

/**
 * Replace === with == or identical(x,y). Replace !== with != or !identical(x,y).
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M1_identical_CleanUp extends AbstractMigrateCleanUp {
  private static boolean isEqualsLiteral(DartExpression arg) {
    return arg instanceof DartNullLiteral || arg instanceof DartIntegerLiteral
        || arg instanceof DartDoubleLiteral;
  }

  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitBinaryExpression(DartBinaryExpression node) {
        SourceRange operatorRange = SourceRangeFactory.forStartLength(
            node.getOperatorOffset(),
            node.getOperator().getSyntax().length());
        if (node.getOperator() == Token.EQ_STRICT) {
          if (isEqualsLiteral(node.getArg1()) || isEqualsLiteral(node.getArg2())) {
            addReplaceEdit(operatorRange, "==");
          } else {
            addReplaceEdit(SourceRangeFactory.forStartLength(node, 0), "identical(");
            addReplaceEdit(SourceRangeFactory.forEndLength(node, 0), ")");
            addReplaceEdit(SourceRangeFactory.forEndStart(node.getArg1(), node.getArg2()), ", ");
          }
        }
        if (node.getOperator() == Token.NE_STRICT) {
          if (isEqualsLiteral(node.getArg1()) || isEqualsLiteral(node.getArg2())) {
            addReplaceEdit(operatorRange, "!=");
          } else {
            addReplaceEdit(SourceRangeFactory.forStartLength(node, 0), "!identical(");
            addReplaceEdit(SourceRangeFactory.forEndLength(node, 0), ")");
            addReplaceEdit(SourceRangeFactory.forEndStart(node.getArg1(), node.getArg2()), ", ");
          }
        }
        return super.visitBinaryExpression(node);
      }
    });
  }
}

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
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.engine.scanner.BeginToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.code.TokenUtils;

import java.util.List;

/**
 * In specification 1.0 M1 getter should not have parameters.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M1_get_CleanUp extends AbstractMigrateCleanUp {
  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitMethodDefinition(DartMethodDefinition node) {
        if (node.getModifiers().isGetter()) {
          SourceRange rangeAfterName = SourceRangeFactory.forEndEnd(node.getName(), node);
          String source = utils.getText(rangeAfterName);
          List<Token> tokens = TokenUtils.getTokens(source);
          if (tokens.size() > 2) {
            Token t0 = tokens.get(0);
            if (t0 instanceof BeginToken && t0.getType() == TokenType.OPEN_PAREN) {
              Token t1 = ((BeginToken) t0).getEndToken();
              if (t1.getType() == TokenType.CLOSE_PAREN) {
                SourceRange parensRange = SourceRangeFactory.forStartEnd(t0, t1);
                parensRange = SourceRangeFactory.withBase(rangeAfterName, parensRange);
                addReplaceEdit(parensRange, "");
              }
            }
          }
        }
        return super.visitMethodDefinition(node);
      }
    });
  }

}

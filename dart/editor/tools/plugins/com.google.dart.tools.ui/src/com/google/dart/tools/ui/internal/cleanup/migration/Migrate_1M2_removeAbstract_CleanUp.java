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
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.code.TokenUtils;

import java.util.List;

/**
 * In 1.0 M2 "abstract" modifier is not required for methods without body.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M2_removeAbstract_CleanUp extends AbstractMigrateCleanUp {

  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitMethodDefinition(DartMethodDefinition node) {
        if (node.getModifiers().isAbstract() && node.getParent() instanceof DartClass) {
          String source = utils.getText(node);
          List<Token> tokens = TokenUtils.getTokens(source);
          KeywordToken abstractToken = TokenUtils.findKeywordToken(tokens, Keyword.ABSTRACT);
          if (abstractToken != null) {
            int offset = node.getSourceInfo().getOffset() + abstractToken.getOffset();
            int length = abstractToken.getLength() + 1;
            SourceRange range = SourceRangeFactory.forStartLength(offset, length);
            addReplaceEdit(range, "");
          }
        }
        return super.visitMethodDefinition(node);
      }
    });
  }
}

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
package com.google.dart.tools.ui.internal.cleanup.style;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.TypeKind;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.internal.corext.refactoring.code.TokenUtils;
import com.google.dart.tools.ui.internal.cleanup.migration.AbstractMigrateCleanUp;

import java.util.List;

/**
 * Use/not-use type annotations for local variables.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Style_useTypeAnnotations_CleanUp extends AbstractMigrateCleanUp {
  private static final String BASE = "useTypeAnnotations-";
  public static final String ALWAYS = BASE + "always";
  public static final String NEVER = BASE + "never";

  private String config;

  public void setConfig(String config) {
    this.config = config;
  }

  @Override
  protected void createFix() throws Exception {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitVariableStatement(DartVariableStatement node) {
        if (ALWAYS.equals(config)) {
          processAlways(node);
        }
        if (NEVER.equals(config)) {
          processNever(node);
        }
        return super.visitVariableStatement(node);
      }

      private void processAlways(DartVariableStatement node) {
        // no type yet
        if (node.getTypeNode() != null) {
          return;
        }
        // prepare variable
        if (node.getVariables().size() != 1) {
          return;
        }
        DartVariable variable = node.getVariables().get(0);
        // prepare type
        Type type = variable.getElement().getType();
        if (type == null || TypeKind.of(type) == TypeKind.DYNAMIC) {
          return;
        }
        // add edit
        {
          DartIdentifier nameNode = variable.getName();
          String typeSource = ExtractUtils.getTypeSource(type);
          // find "var" token
          KeywordToken varToken;
          {
            SourceRange modifiersRange = SourceRangeFactory.forStartEnd(node, nameNode);
            String modifiersSource = utils.getText(modifiersRange);
            List<com.google.dart.engine.scanner.Token> tokens = TokenUtils.getTokens(modifiersSource);
            varToken = TokenUtils.findKeywordToken(tokens, Keyword.VAR);
          }
          // replace "var", or insert type before name
          if (varToken != null) {
            SourceRange range = SourceRangeFactory.forToken(varToken);
            range = SourceRangeFactory.withBase(node, range);
            addReplaceEdit(range, typeSource);
          } else {
            SourceRange range = SourceRangeFactory.forStartLength(nameNode, 0);
            addReplaceEdit(range, typeSource + " ");
          }
        }
      }

      private void processNever(DartVariableStatement node) {
        // has type
        DartTypeNode typeNode = node.getTypeNode();
        if (typeNode == null) {
          return;
        }
        // add edit
        if (!node.getVariables().isEmpty()) {
          DartIdentifier nameNode = node.getVariables().get(0).getName();
          SourceRange typeRange = SourceRangeFactory.forStartStart(typeNode, nameNode);
          // add "var" if there are no "const" or "final" modifiers
          if (typeRange.getOffset() == node.getSourceInfo().getOffset()) {
            addReplaceEdit(typeRange, "var ");
          } else {
            addReplaceEdit(typeRange, "");
          }
        }
      }
    });
  }
}

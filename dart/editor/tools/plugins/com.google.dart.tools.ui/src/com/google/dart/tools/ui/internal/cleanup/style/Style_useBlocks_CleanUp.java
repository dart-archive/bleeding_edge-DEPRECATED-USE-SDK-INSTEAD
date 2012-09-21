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

import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_IF_STATEMENT_ELSE;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.getLocationInParent;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.ui.internal.cleanup.migration.AbstractMigrateCleanUp;

/**
 * Use/not-use block is control statements.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Style_useBlocks_CleanUp extends AbstractMigrateCleanUp {

  @Override
  protected void createFix() throws Exception {
    final String eol = utils.getEndOfLine();

    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitForInStatement(DartForInStatement node) {
        if (!utils.getText(node).contains(eol)) {
          return null;
        }
        DartStatement body = node.getBody();
        ensureBlock(node, node.getCloseParenOffset() + 1, body);
        return super.visitForInStatement(node);
      }

      @Override
      public Void visitForStatement(DartForStatement node) {
        if (!utils.getText(node).contains(eol)) {
          return null;
        }
        DartStatement body = node.getBody();
        ensureBlock(node, node.getCloseParenOffset() + 1, body);
        return super.visitForStatement(node);
      }

      @Override
      public Void visitIfStatement(DartIfStatement node) {
        DartStatement thenStatement = node.getThenStatement();
        DartStatement elseStatement = node.getElseStatement();
        // single line statement
        if (!utils.getText(node).contains(eol)) {
          return null;
        }
        // ensure block
        if (!(thenStatement instanceof DartBlock)) {
          String prefix = getNodePrefix(node);
          addReplaceEdit(SourceRangeFactory.forStartLength(node.getCloseParenOffset() + 1, 0), " {");
          if (elseStatement != null) {
            int elseOffset = node.getElseTokenOffset();
            addReplaceEdit(SourceRangeFactory.forStartLength(elseOffset, 0), "} ");
          } else {
            addReplaceEdit(SourceRangeFactory.forEndLength(thenStatement, 0), eol + prefix + "}");
          }
        }
        if (elseStatement != null) {
          ensureBlock(node, node.getElseTokenOffset() + "else".length(), elseStatement);
        }
        return super.visitIfStatement(node);
      }

      @Override
      public Void visitWhileStatement(DartWhileStatement node) {
        if (!utils.getText(node).contains(eol)) {
          return null;
        }
        DartStatement body = node.getBody();
        ensureBlock(node, node.getCloseParenOffset() + 1, body);
        return super.visitWhileStatement(node);
      }

      private void ensureBlock(DartNode node, int closeParenOffset, DartStatement statement) {
        if (statement instanceof DartBlock) {
          return;
        }
        if (statement instanceof DartIfStatement) {
          return;
        }
        String prefix = getNodePrefix(node);
        addReplaceEdit(SourceRangeFactory.forStartLength(closeParenOffset, 0), " {");
        addReplaceEdit(SourceRangeFactory.forEndLength(statement, 0), eol + prefix + "}");
      }
    });
  }

  private String getNodePrefix(DartNode node) {
    if (node instanceof DartIfStatement) {
      if (getLocationInParent(node) == DART_IF_STATEMENT_ELSE) {
        return getNodePrefix(node.getParent());
      }
    }
    return utils.getNodePrefix(node);
  }
}

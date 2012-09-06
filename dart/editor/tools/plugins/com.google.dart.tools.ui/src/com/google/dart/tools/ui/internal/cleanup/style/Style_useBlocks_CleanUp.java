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

import com.google.common.base.Objects;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.ui.internal.cleanup.migration.AbstractMigrateCleanUp;

import java.util.List;

/**
 * Use/not-use block is control statements.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Style_useBlocks_CleanUp extends AbstractMigrateCleanUp {
  public static final String ALWAYS = "ALWAYS";
  public static final String WHEN_NECESSARY = "WHEN_NECESSARY";

  private String flag;

  public void setFlag(String flag) {
    this.flag = flag;
  }

  @Override
  protected void createFix() throws Exception {
    final String eol = utils.getEndOfLine();
    if (Objects.equal(flag, ALWAYS)) {
      unitNode.accept(new ASTVisitor<Void>() {
        @Override
        public Void visitForInStatement(DartForInStatement node) {
          DartStatement body = node.getBody();
          ensureBlock(node, node.getCloseParenOffset() + 1, body);
          return super.visitForInStatement(node);
        }

        @Override
        public Void visitForStatement(DartForStatement node) {
          DartStatement body = node.getBody();
          ensureBlock(node, node.getCloseParenOffset() + 1, body);
          return super.visitForStatement(node);
        }

        @Override
        public Void visitIfStatement(DartIfStatement node) {
          DartStatement thenStatement = node.getThenStatement();
          DartStatement elseStatement = node.getElseStatement();
          if (!(thenStatement instanceof DartBlock)) {
            String prefix = utils.getNodePrefix(node);
            addReplaceEdit(
                SourceRangeFactory.forStartLength(node.getCloseParenOffset() + 1, 0),
                " {");
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
          DartStatement body = node.getBody();
          ensureBlock(node, node.getCloseParenOffset() + 1, body);
          return super.visitWhileStatement(node);
        }

        private void ensureBlock(DartNode node, int closeParenOffset, DartStatement statement) {
          if (statement instanceof DartBlock) {
            return;
          }
          String prefix = utils.getNodePrefix(node);
          addReplaceEdit(SourceRangeFactory.forStartLength(closeParenOffset, 0), " {");
          addReplaceEdit(SourceRangeFactory.forEndLength(statement, 0), eol + prefix + "}");
        }
      });
    }
    if (Objects.equal(flag, WHEN_NECESSARY)) {
      unitNode.accept(new ASTVisitor<Void>() {
        @Override
        public Void visitForInStatement(DartForInStatement node) {
          ensureSingleStatement(node.getBody(), node.getCloseParenOffset() + ")".length());
          return super.visitForInStatement(node);
        }

        @Override
        public Void visitForStatement(DartForStatement node) {
          ensureSingleStatement(node.getBody(), node.getCloseParenOffset() + ")".length());
          return super.visitForStatement(node);
        }

        @Override
        public Void visitIfStatement(DartIfStatement node) {
          DartStatement thenStatement = node.getThenStatement();
          DartStatement elseStatement = node.getElseStatement();
          // "then" block
          if (thenStatement instanceof DartBlock) {
            DartBlock block = (DartBlock) thenStatement;
            List<DartStatement> thenStatements = block.getStatements();
            if (thenStatements.size() == 1) {
              thenStatement = thenStatements.get(0);
              // remove then block "{"
              addReplaceEdit(SourceRangeFactory.forStartEnd(
                  node.getCloseParenOffset() + 1,
                  block.getSourceInfo().getOffset() + 1), "");
              // remove then block "}" - to the "else" or to the end of "if"
              SourceRange closeRange;
              if (elseStatement != null) {
                closeRange = SourceRangeFactory.forStartEnd(
                    block.getSourceInfo().getEnd() - 1,
                    node.getElseTokenOffset());
              } else {
                closeRange = SourceRangeFactory.forEndEnd(thenStatement, block);
              }
              addReplaceEdit(closeRange, "");
            }
          }
          // "else" block
          if (elseStatement instanceof DartBlock) {
            ensureSingleStatement(elseStatement, node.getElseTokenOffset() + "else".length());
          }
          return super.visitIfStatement(node);
        }

        @Override
        public Void visitWhileStatement(DartWhileStatement node) {
          ensureSingleStatement(node.getBody(), node.getCloseParenOffset() + ")".length());
          return super.visitWhileStatement(node);
        }

        private void ensureSingleStatement(DartStatement statement, int removeStart) {
          if (statement instanceof DartBlock) {
            DartBlock block = (DartBlock) statement;
            List<DartStatement> statements = block.getStatements();
            if (statements.size() == 1) {
              statement = statements.get(0);
              // remove "{"
              int removeEnd = block.getSourceInfo().getOffset() + 1;
              addReplaceEdit(SourceRangeFactory.forStartEnd(removeStart, removeEnd), "");
              // remove "}"
              addReplaceEdit(SourceRangeFactory.forEndEnd(statement, block), "");
            }
          }
        }
      });
    }
  }
}

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
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

/**
 * In specification 1.0 M1 "on Type catch (e, stack)" syntax should be used.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M1_catch_CleanUp extends AbstractMigrateCleanUp {
  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitCatchBlock(DartCatchBlock node) {
        processCatch(node);
        return super.visitCatchBlock(node);
      }

      private void processCatch(DartCatchBlock node) {
        // may be already new syntax
        if (utils.getText(node).startsWith("on")) {
          return;
        }
        // move exception type to "on Type"
        {
          DartParameter exception = node.getException();
          DartTypeNode exceptionType = exception.getTypeNode();
          if (exceptionType != null) {
            DartExpression exceptionName = exception.getName();
            addReplaceEdit(
                SourceRangeFactory.forStartLength(node, 0),
                "on " + utils.getText(exceptionType) + " ");
            addReplaceEdit(SourceRangeFactory.forStartStart(exception, exceptionName), "");
          }
        }
        // remove "StackTrace" type
        {
          DartParameter stackTrace = node.getStackTrace();
          if (stackTrace != null) {
            DartTypeNode stackType = stackTrace.getTypeNode();
            if (stackType != null) {
              addReplaceEdit(SourceRangeFactory.forStartStart(stackType, stackTrace.getName()), "");
            }
          }
        }
      }
    });
  }
}

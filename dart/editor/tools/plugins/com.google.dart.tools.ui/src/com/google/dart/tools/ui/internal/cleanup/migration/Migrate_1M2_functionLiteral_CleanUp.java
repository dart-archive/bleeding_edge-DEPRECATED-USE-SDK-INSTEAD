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
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

/**
 * In 1.0 M2 function literal should not have a name or a return type.
 * <p>
 * http://code.google.com/p/dart/issues/detail?id=6710
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M2_functionLiteral_CleanUp extends AbstractMigrateCleanUp {
  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitFunctionExpression(DartFunctionExpression node) {
        if (!node.isStatement()) {
          processFunctionExpression(node);
        }
        return super.visitFunctionExpression(node);
      }

      private void processFunctionExpression(DartFunctionExpression node) {
        DartIdentifier name = node.getName();
        // remove: type + name
        {
          DartFunction function = node.getFunction();
          DartTypeNode returnType = function.getReturnTypeNode();
          if (returnType != null) {
            SourceRange range = SourceRangeFactory.forStartEnd(returnType, name);
            addReplaceEdit(range, "");
            return;
          }
        }
        // remove: name
        if (name != null) {
          SourceRange range = SourceRangeFactory.create(name);
          addReplaceEdit(range, "");
        }
      }
    });
  }
}

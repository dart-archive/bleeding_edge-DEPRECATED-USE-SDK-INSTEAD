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
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

import java.util.List;

/**
 * In specification 1.0 M1 raw string should start with 'r'.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M1_rawString_CleanUp extends AbstractMigrateCleanUp {
  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitStringLiteral(DartStringLiteral node) {
        List<DartStringLiteral> parts = node.getParts();
        for (DartStringLiteral part : parts) {
          String source = utils.getText(part);
          if (source.startsWith("@'") || source.startsWith("@\"")) {
            addReplaceEdit(SourceRangeFactory.forStartLength(part, 1), "r");
          }
        }
        return null;
      }
    });
  }
}

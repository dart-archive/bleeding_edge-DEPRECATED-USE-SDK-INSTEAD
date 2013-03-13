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
package com.google.dart.tools.ui.internal.cleanup.migration;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

import static com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_methods_CleanUp.isSubType;

import org.apache.commons.lang3.StringUtils;

/**
 * Migrations for 1.0 M4 library changes.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M4_CleanUp extends AbstractMigrateCleanUp {

  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {

      @Override
      public Void visitMethodInvocation(DartMethodInvocation node) {
        super.visitMethodInvocation(node);
        DartExpression realTarget = node.getRealTarget();
        DartIdentifier nameNode = node.getFunctionName();
        String name = nameNode.getName();
        // with target
        if (realTarget != null && realTarget.getType() instanceof InterfaceType) {
          InterfaceType targetType = (InterfaceType) realTarget.getType();
          // xMatching renamed to xWhere
          if ("firstMatching".equals(name) || "lastMatching".equals(name)
              || "singleMatching".equals(name)) {
            if (isSubType(targetType, "Iterable", "dart://core/core.dart")
                || isSubType(targetType, "Stream", "dart://async/async.dart")) {
              name = StringUtils.removeEnd(name, "Matching") + "Where";
              addReplaceEdit(SourceRangeFactory.create(nameNode), name);
              return null;
            }
          }
          if ("removeMatching".equals(name) || "retainMatching".equals(name)) {
            if (isSubType(targetType, "Collection", "dart://core/core.dart")) {
              name = StringUtils.removeEnd(name, "Matching") + "Where";
              addReplaceEdit(SourceRangeFactory.create(nameNode), name);
              return null;
            }
          }
        }
        return null;
      }
    });
  }
}

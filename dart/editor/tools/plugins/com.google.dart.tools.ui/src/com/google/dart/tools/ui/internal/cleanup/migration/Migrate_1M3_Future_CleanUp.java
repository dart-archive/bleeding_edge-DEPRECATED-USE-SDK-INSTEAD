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

import com.google.common.base.Objects;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.resolver.Elements;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.TypeKind;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

import static com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_methods_CleanUp.isSubType;

/**
 * Separate add-on for {@link Migrate_1M3_corelib_CleanUp} because these changes are not always
 * valid and require user review.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M3_Future_CleanUp extends AbstractMigrateCleanUp {
  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(DartMethodInvocation node) {
        DartIdentifier nameNode = node.getFunctionName();
        // chain/transform -> then
        if (Elements.isIdentifierName(nameNode, "chain")
            || Elements.isIdentifierName(nameNode, "transform")) {
          Type targetType = node.getRealTarget().getType();
          if (targetType instanceof InterfaceType) {
            if (isSubType((InterfaceType) targetType, "Future", "dart://core/core.dart")
                || isSubType((InterfaceType) targetType, "Future", "dart://async/async.dart")
                || TypeKind.of(targetType) == TypeKind.DYNAMIC) {
              addReplaceEdit(SourceRangeFactory.create(nameNode), "then");
            }
          }
        }
        // Futures.forEach/wait -> Future.*
        if (Elements.isIdentifierName(nameNode, "forEach")
            || Elements.isIdentifierName(nameNode, "wait")) {
          if (node.getTarget() instanceof DartIdentifier) {
            DartIdentifier targetIdentifier = (DartIdentifier) node.getTarget();
            if (Objects.equal(targetIdentifier.getName(), "Futures")) {
              addReplaceEdit(SourceRangeFactory.create(targetIdentifier), "Future");
            }
          }
        }
        return super.visitMethodInvocation(node);
      }
    });
  }
}

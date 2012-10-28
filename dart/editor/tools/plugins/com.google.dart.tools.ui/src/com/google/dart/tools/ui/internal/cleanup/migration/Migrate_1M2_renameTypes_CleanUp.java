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
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.resolver.Elements;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

/**
 * In 1.0 M2 many exceptions were renamed, e.g. InvalidArgumentException to ArgumentError.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M2_renameTypes_CleanUp extends AbstractMigrateCleanUp {
  private static class TypeRenameSpec {
    private final String oldName;
    private final String newName;

    public TypeRenameSpec(String oldName, String newName) {
      this.oldName = oldName;
      this.newName = newName;
    }
  }

  private static final TypeRenameSpec[] SPECS = new TypeRenameSpec[] {
      new TypeRenameSpec("EmptyQueueException", "StateError"),
      new TypeRenameSpec("InvalidAccessException", "UnsupportedError"),
      new TypeRenameSpec("InvalidArgumentException", "ArgumentError"),
      new TypeRenameSpec("NoMoreElementsException", "StateError"),
      new TypeRenameSpec("OutOfMemoryException", "OutOfMemoryError"),
      new TypeRenameSpec("StackOverflowException", "StackOverflowError"),
      new TypeRenameSpec("UnsupportedOperationException", "UnsupportedError"),
      new TypeRenameSpec("Dynamic", "dynamic"),};

  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitTypeNode(DartTypeNode node) {
        // prepare name
        DartIdentifier nameIdentifier = null;
        DartNode name = node.getIdentifier();
        if (name instanceof DartIdentifier) {
          nameIdentifier = (DartIdentifier) name;
        } else if (name instanceof DartPropertyAccess) {
          nameIdentifier = ((DartPropertyAccess) name).getName();
        }
        // analyze name
        if (nameIdentifier != null) {
          for (TypeRenameSpec spec : SPECS) {
            if (Elements.isIdentifierName(nameIdentifier, spec.oldName)) {
              addReplaceEdit(SourceRangeFactory.create(nameIdentifier), spec.newName);
              break;
            }
          }
        }
        // done
        return super.visitTypeNode(node);
      }
    });
  }
}

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
package com.google.dart.tools.core.internal.indexer.contributor;

import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.ElementKind;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.tools.core.internal.indexer.location.FieldLocation;
import com.google.dart.tools.core.internal.indexer.location.VariableLocation;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;

/**
 * Instances of the class <code>FieldAccessContributor</code> implement a contributor that adds a
 * reference every time it finds a reference (assignment or access) to a field.
 */
public class FieldAccessContributor extends ScopedDartContributor {
  @Override
  public Void visitIdentifier(DartIdentifier node) {
    Element element = node.getElement();
    if (element == null) {
      element = node.getElement();
    }
    if (element instanceof FieldElement) {
      processVariable(node, (FieldElement) element);
    }
    return super.visitIdentifier(node);
  }

  private void processVariable(DartIdentifier node, FieldElement binding) {
    if (binding.getKind() == ElementKind.FIELD) {
      CompilationUnitElement fieldOrVariable = getDartElement(binding);
      if (fieldOrVariable instanceof Field) {
        Field field = (Field) fieldOrVariable;
        try {
          recordRelationship(
              peekTarget(new SourceRangeImpl(node)),
              new FieldLocation(field, field.getNameRange()));
        } catch (DartModelException exception) {
          // Ignored
        }
      } else if (fieldOrVariable instanceof DartVariableDeclaration) {
        DartVariableDeclaration variable = (DartVariableDeclaration) fieldOrVariable;
        try {
          recordRelationship(peekTarget(new SourceRangeImpl(node)), new VariableLocation(
              variable,
              variable.getNameRange()));
        } catch (DartModelException exception) {
          // Ignored
        }
      }
    }
  }
}

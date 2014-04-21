/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularDecoratorElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.internal.index.IndexConstants;

/**
 * Visits resolved {@link CompilationUnit} and adds Angular specific relationships into
 * {@link IndexStore}.
 * 
 * @coverage dart.engine.index
 */
public class AngularDartIndexContributor extends GeneralizingAstVisitor<Void> {
  private final IndexStore store;

  public AngularDartIndexContributor(IndexStore store) {
    this.store = store;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement classElement = node.getElement();
    if (classElement != null) {
      ToolkitObjectElement[] toolkitObjects = classElement.getToolkitObjects();
      for (ToolkitObjectElement object : toolkitObjects) {
        if (object instanceof AngularComponentElement) {
          indexComponent((AngularComponentElement) object);
        }
        if (object instanceof AngularDecoratorElement) {
          AngularDecoratorElement directive = (AngularDecoratorElement) object;
          indexDirective(directive);
        }
      }
    }
    // stop visiting
    return null;
  }

  @Override
  public Void visitCompilationUnitMember(CompilationUnitMember node) {
    // stop visiting
    return null;
  }

  private void indexComponent(AngularComponentElement component) {
    indexProperties(component.getProperties());
  }

  private void indexDirective(AngularDecoratorElement directive) {
    indexProperties(directive.getProperties());
  }

  /**
   * Index {@link FieldElement} references from {@link AngularPropertyElement}s.
   */
  private void indexProperties(AngularPropertyElement[] properties) {
    for (AngularPropertyElement property : properties) {
      FieldElement field = property.getField();
      if (field != null) {
        int offset = property.getFieldNameOffset();
        if (offset == -1) {
          continue;
        }
        int length = field.getName().length();
        Location location = new Location(property, offset, length);
        // getter reference
        if (property.getPropertyKind().callsGetter()) {
          PropertyAccessorElement getter = field.getGetter();
          if (getter != null) {
            store.recordRelationship(getter, IndexConstants.IS_REFERENCED_BY_QUALIFIED, location);
          }
        }
        // setter reference
        if (property.getPropertyKind().callsSetter()) {
          PropertyAccessorElement setter = field.getSetter();
          if (setter != null) {
            store.recordRelationship(setter, IndexConstants.IS_REFERENCED_BY_QUALIFIED, location);
          }
        }
      }
    }
  }
}

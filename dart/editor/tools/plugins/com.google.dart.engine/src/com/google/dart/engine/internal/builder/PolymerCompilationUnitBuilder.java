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

package com.google.dart.engine.internal.builder;

import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.polymer.PolymerElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.polymer.PolymerTagDartElementImpl;

import java.util.List;

/**
 * Instances of the class {@code PolymerCompilationUnitBuilder} build a Polymer specific element
 * model for a single compilation unit.
 * 
 * @coverage dart.engine.resolver
 */
public class PolymerCompilationUnitBuilder {
  private static final String CUSTOM_TAG = "CustomTag";

  public static Element getElement(AstNode node, int offset) {
    // maybe node is not SimpleStringLiteral
    if (!(node instanceof SimpleStringLiteral)) {
      return null;
    }
    SimpleStringLiteral literal = (SimpleStringLiteral) node;
    // maybe has PolymerElement
    {
      Element element = literal.getToolkitElement();
      if (element instanceof PolymerElement) {
        return element;
      }
    }
    // no Element
    return null;
  }

  /**
   * The compilation unit with built Dart element models.
   */
  private CompilationUnit unit;

  /**
   * The {@link ClassDeclaration} that is currently being analyzed.
   */
  private ClassDeclaration classDeclaration;

  /**
   * The {@link ClassElementImpl} that is currently being analyzed.
   */
  private ClassElementImpl classElement;

  /**
   * The {@link Annotation} that is currently being analyzed.
   */
  private Annotation annotation;

  /**
   * Initialize a newly created compilation unit element builder.
   * 
   * @param unit the compilation unit with built Dart element models
   */
  public PolymerCompilationUnitBuilder(CompilationUnit unit) {
    this.unit = unit;
  }

  /**
   * Builds Polymer specific element models and adds them to the existing Dart elements.
   */
  public void build() {
    // process classes
    for (CompilationUnitMember unitMember : unit.getDeclarations()) {
      if (unitMember instanceof ClassDeclaration) {
        this.classDeclaration = (ClassDeclaration) unitMember;
        this.classElement = (ClassElementImpl) classDeclaration.getElement();
        // process annotations
        NodeList<Annotation> annotations = classDeclaration.getMetadata();
        for (Annotation annotation : annotations) {
          // verify annotation
          if (annotation.getArguments() == null) {
            continue;
          }
          this.annotation = annotation;
          // @CustomTag
          if (isAnnotation(annotation, CUSTOM_TAG)) {
            parseCustomTag();
            continue;
          }
        }
      }
    }
  }

  /**
   * Checks if given {@link Annotation} is an annotation with required name.
   */
  private boolean isAnnotation(Annotation annotation, String name) {
    Element element = annotation.getElement();
    if (element instanceof ConstructorElement) {
      ConstructorElement constructorElement = (ConstructorElement) element;
      return constructorElement.getReturnType().getDisplayName().equals(name);
    }
    return false;
  }

  private void parseCustomTag() {
    List<Expression> arguments = annotation.getArguments().getArguments();
    if (arguments.size() == 1) {
      Expression nameExpression = arguments.get(0);
      if (nameExpression instanceof SimpleStringLiteral) {
        SimpleStringLiteral nameLiteral = (SimpleStringLiteral) nameExpression;
        String name = nameLiteral.getValue();
        int nameOffset = nameLiteral.getValueOffset();
        PolymerTagDartElementImpl element = new PolymerTagDartElementImpl(
            name,
            nameOffset,
            classElement);
        classElement.addToolkitObjects(element);
        nameLiteral.setToolkitElement(element);
      }
    }
  }
}

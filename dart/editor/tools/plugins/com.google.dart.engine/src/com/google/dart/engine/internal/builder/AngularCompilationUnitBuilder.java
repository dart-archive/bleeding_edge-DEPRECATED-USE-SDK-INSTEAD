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
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.angular.AngularFilterElementImpl;
import com.google.dart.engine.source.Source;

import java.util.List;

/**
 * Instances of the class {@code AngularCompilationUnitBuilder} build an Angular specific element
 * model for a single compilation unit.
 * 
 * @coverage dart.engine.resolver
 */
public class AngularCompilationUnitBuilder {
  /**
   * The source containing the unit that will be analyzed.
   */
  private final Source source;

  /**
   * The listener to which errors will be reported.
   */
  private final AnalysisErrorListener errorListener;

  /**
   * The annotation that is currently being analyzed.
   */
  private Annotation annotation;

  /**
   * Initialize a newly created compilation unit element builder.
   * 
   * @param errorListener the listener to which errors will be reported.
   * @param source the source containing the unit that will be analyzed
   */
  public AngularCompilationUnitBuilder(AnalysisErrorListener errorListener, Source source) {
    this.errorListener = errorListener;
    this.source = source;
  }

  /**
   * Builds Angular specific element models and adds them to the existing Dart elements.
   * 
   * @param unit the compilation unit with built Dart element models
   */
  public void build(CompilationUnit unit) {
    for (CompilationUnitMember unitMember : unit.getDeclarations()) {
      if (unitMember instanceof ClassDeclaration) {
        ClassDeclaration classDeclaration = (ClassDeclaration) unitMember;
        NodeList<Annotation> annotations = classDeclaration.getMetadata();
        for (Annotation annotation : annotations) {
          this.annotation = annotation;
          // @NgFilter
          if (isNgFilter(annotation)) {
            if (hasStringArgument("name")) {
              String name = getStringArgument("name");
              int nameOffset = getStringArgumentOffset("name");
              setToolkitElement(classDeclaration, new AngularFilterElementImpl(name, nameOffset));
            }
            continue;
          }
        }
      }
    }
  }

  /**
   * @return the argument {@link Expression} with given name form {@link #annotation}, may be
   *         {@code null} if not found.
   */
  private Expression getArgument(String name) {
    List<Expression> arguments = annotation.getArguments().getArguments();
    for (Expression argument : arguments) {
      if (argument instanceof NamedExpression) {
        NamedExpression namedExpression = (NamedExpression) argument;
        String argumentName = namedExpression.getName().getLabel().getName();
        if (name.equals(argumentName)) {
          return namedExpression.getExpression();
        }
      }
    }
    return null;
  }

  /**
   * @return the {@link String} value of the named argument.
   */
  private String getStringArgument(String name) {
    Expression argument = getArgument(name);
    return ((SimpleStringLiteral) argument).getValue();
  }

  /**
   * @return the offset of the value of the named argument.
   */
  private int getStringArgumentOffset(String name) {
    Expression argument = getArgument(name);
    return ((SimpleStringLiteral) argument).getValueOffset();
  }

  /**
   * Checks if {@link #namedArguments} has string value for the argument with the given name.
   */
  private boolean hasStringArgument(String name) {
    Expression argument = getArgument(name);
    return argument instanceof SimpleStringLiteral;
  }

  /**
   * Checks if given {@link Annotation} is resolved as <code>NgFilter</code>.
   */
  private boolean isNgFilter(Annotation annotation) {
    Element element = annotation.getElement();
    if (element instanceof ConstructorElement) {
      ConstructorElement constructorElement = (ConstructorElement) element;
      return constructorElement.getReturnType().getDisplayName().equals("NgFilter");
    }
    return false;
  }

  /**
   * Set the given {@link ToolkitObjectElement} for {@link ClassElement} of the
   * {@link ClassDeclaration}.
   */
  private void setToolkitElement(ClassDeclaration classDeclaration, ToolkitObjectElement element) {
    ClassElementImpl classElement = (ClassElementImpl) classDeclaration.getElement();
    classElement.setToolkitObjects(new ToolkitObjectElement[] {element});
  }
}

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
package com.google.dart.engine.internal.element.handle;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.angular.AngularViewElement;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code CompilationUnitElementHandle} implements a handle to a
 * {@link CompilationUnitElement}.
 * 
 * @coverage dart.engine.element
 */
public class CompilationUnitElementHandle extends ElementHandle implements CompilationUnitElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public CompilationUnitElementHandle(CompilationUnitElement element) {
    super(element);
  }

  @Override
  public PropertyAccessorElement[] getAccessors() {
    return getActualElement().getAccessors();
  }

  @Override
  public AngularViewElement[] getAngularViews() {
    return getActualElement().getAngularViews();
  }

  @Override
  public LibraryElement getEnclosingElement() {
    return (LibraryElement) super.getEnclosingElement();
  }

  @Override
  public ClassElement getEnum(String enumName) {
    return getActualElement().getEnum(enumName);
  }

  @Override
  public ClassElement[] getEnums() {
    return getActualElement().getEnums();
  }

  @Override
  public FunctionElement[] getFunctions() {
    return getActualElement().getFunctions();
  }

  @Override
  public FunctionTypeAliasElement[] getFunctionTypeAliases() {
    return getActualElement().getFunctionTypeAliases();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.COMPILATION_UNIT;
  }

  @Override
  public CompilationUnit getNode() throws AnalysisException {
    return getActualElement().getNode();
  }

  @Override
  public Source getSource() {
    return getActualElement().getSource();
  }

  @Override
  public TopLevelVariableElement[] getTopLevelVariables() {
    return getActualElement().getTopLevelVariables();
  }

  @Override
  public ClassElement getType(String className) {
    return getActualElement().getType(className);
  }

  @Override
  public ClassElement[] getTypes() {
    return getActualElement().getTypes();
  }

  @Override
  public String getUri() {
    return getActualElement().getUri();
  }

  @Override
  public int getUriEnd() {
    return getActualElement().getUriEnd();
  }

  @Override
  public int getUriOffset() {
    return getActualElement().getUriOffset();
  }

  @Override
  public boolean hasLoadLibraryFunction() {
    return getActualElement().hasLoadLibraryFunction();
  }

  @Override
  protected CompilationUnitElement getActualElement() {
    return (CompilationUnitElement) super.getActualElement();
  }
}

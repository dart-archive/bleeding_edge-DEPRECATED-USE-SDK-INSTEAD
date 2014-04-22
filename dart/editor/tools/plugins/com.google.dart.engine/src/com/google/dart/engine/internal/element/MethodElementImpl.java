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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * Instances of the class {@code MethodElementImpl} implement a {@code MethodElement}.
 * 
 * @coverage dart.engine.element
 */
public class MethodElementImpl extends ExecutableElementImpl implements MethodElement {
  /**
   * An empty array of method elements.
   */
  public static final MethodElement[] EMPTY_ARRAY = new MethodElement[0];

  /**
   * Initialize a newly created method element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public MethodElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created method element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public MethodElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitMethodElement(this);
  }

  @Override
  public String getDisplayName() {
    String displayName = super.getDisplayName();
    if ("unary-".equals(displayName)) {
      return "-";
    }
    return displayName;
  }

  @Override
  public ClassElement getEnclosingElement() {
    return (ClassElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.METHOD;
  }

  @Override
  public String getName() {
    String name = super.getName();
    if (isOperator() && name.equals("-")) {
      if (getParameters().length == 0) {
        return "unary-";
      }
    }
    return super.getName();
  }

  @Override
  public MethodDeclaration getNode() throws AnalysisException {
    return getNodeMatching(MethodDeclaration.class);
  }

  @Override
  public boolean isAbstract() {
    return hasModifier(Modifier.ABSTRACT);
  }

  @Override
  public boolean isOperator() {
    String name = getDisplayName();
    if (name.isEmpty()) {
      return false;
    }
    char first = name.charAt(0);
    return !(('a' <= first && first <= 'z') || ('A' <= first && first <= 'Z') || first == '_' || first == '$');
  }

  @Override
  public boolean isStatic() {
    return hasModifier(Modifier.STATIC);
  }

  /**
   * Set whether this method is abstract to correspond to the given value.
   * 
   * @param isAbstract {@code true} if the method is abstract
   */
  public void setAbstract(boolean isAbstract) {
    setModifier(Modifier.ABSTRACT, isAbstract);
  }

  /**
   * Set whether this method is static to correspond to the given value.
   * 
   * @param isStatic {@code true} if the method is static
   */
  public void setStatic(boolean isStatic) {
    setModifier(Modifier.STATIC, isStatic);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append(getEnclosingElement().getDisplayName());
    builder.append(".");
    builder.append(getDisplayName());
    super.appendTo(builder);
  }
}

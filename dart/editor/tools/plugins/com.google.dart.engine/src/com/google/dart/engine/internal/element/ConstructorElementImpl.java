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

import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * Instances of the class {@code ConstructorElementImpl} implement a {@code ConstructorElement}.
 * 
 * @coverage dart.engine.element
 */
public class ConstructorElementImpl extends ExecutableElementImpl implements ConstructorElement {
  /**
   * An empty array of constructor elements.
   */
  public static final ConstructorElement[] EMPTY_ARRAY = new ConstructorElement[0];

  /**
   * The constructor to which this constructor is redirecting.
   */
  private ConstructorElement redirectedConstructor;

  /**
   * Initialize a newly created constructor element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public ConstructorElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created constructor element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public ConstructorElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitConstructorElement(this);
  }

  @Override
  public ClassElement getEnclosingElement() {
    return (ClassElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.CONSTRUCTOR;
  }

  @Override
  public ConstructorDeclaration getNode() throws AnalysisException {
    return getNodeMatching(ConstructorDeclaration.class);
  }

  @Override
  public ConstructorElement getRedirectedConstructor() {
    return redirectedConstructor;
  }

  @Override
  public boolean isConst() {
    return hasModifier(Modifier.CONST);
  }

  @Override
  public boolean isDefaultConstructor() {
    // unnamed
    String name = getName();
    if (name != null && name.length() != 0) {
      return false;
    }
    // no required parameters
    for (ParameterElement parameter : getParameters()) {
      if (parameter.getParameterKind() == ParameterKind.REQUIRED) {
        return false;
      }
    }
    // OK, can be used as default constructor
    return true;
  }

  @Override
  public boolean isFactory() {
    return hasModifier(Modifier.FACTORY);
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  /**
   * Set whether this constructor represents a 'const' constructor to the given value.
   * 
   * @param isConst {@code true} if this constructor represents a 'const' constructor
   */
  public void setConst(boolean isConst) {
    setModifier(Modifier.CONST, isConst);
  }

  /**
   * Set whether this constructor represents a factory method to the given value.
   * 
   * @param isFactory {@code true} if this constructor represents a factory method
   */
  public void setFactory(boolean isFactory) {
    setModifier(Modifier.FACTORY, isFactory);
  }

  /**
   * Sets the constructor to which this constructor is redirecting.
   * 
   * @param redirectedConstructor the constructor to which this constructor is redirecting
   */
  public void setRedirectedConstructor(ConstructorElement redirectedConstructor) {
    this.redirectedConstructor = redirectedConstructor;
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append(getEnclosingElement().getDisplayName());
    String name = getDisplayName();
    if (name != null && !name.isEmpty()) {
      builder.append(".");
      builder.append(name);
    }
    super.appendTo(builder);
  }
}

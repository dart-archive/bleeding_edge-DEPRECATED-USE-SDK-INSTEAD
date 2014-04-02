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
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.source.SourceRange;

/**
 * Instances of the class {@code ParameterElementImpl} implement a {@code ParameterElement}.
 * 
 * @coverage dart.engine.element
 */
public class ParameterElementImpl extends VariableElementImpl implements ParameterElement {
  /**
   * Is {@code true} if this variable is potentially mutated somewhere in its scope.
   */
  private boolean potentiallyMutatedInScope;

  /**
   * Is {@code true} if this variable is potentially mutated somewhere in closure.
   */
  private boolean potentiallyMutatedInClosure;

  /**
   * An array containing all of the parameters defined by this parameter element. There will only be
   * parameters if this parameter is a function typed parameter.
   */
  private ParameterElement[] parameters = ParameterElementImpl.EMPTY_ARRAY;

  /**
   * The kind of this parameter.
   */
  private ParameterKind parameterKind;

  /**
   * The offset to the beginning of the default value range for this element.
   */
  private int defaultValueRangeOffset;

  /**
   * The length of the default value range for this element, or {@code -1} if this element does not
   * have a default value.
   */
  private int defaultValueRangeLength = -1;

  /**
   * The offset to the beginning of the visible range for this element.
   */
  private int visibleRangeOffset;

  /**
   * The length of the visible range for this element, or {@code -1} if this element does not have a
   * visible range.
   */
  private int visibleRangeLength = -1;

  /**
   * An empty array of field elements.
   */
  public static final ParameterElement[] EMPTY_ARRAY = new ParameterElement[0];

  /**
   * Initialize a newly created parameter element to have the given name.
   * 
   * @param name the name of this element
   */
  public ParameterElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created parameter element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public ParameterElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitParameterElement(this);
  }

  @Override
  public SourceRange getDefaultValueRange() {
    if (defaultValueRangeLength < 0) {
      return null;
    }
    return new SourceRange(defaultValueRangeOffset, defaultValueRangeLength);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.PARAMETER;
  }

  @Override
  public ParameterKind getParameterKind() {
    return parameterKind;
  }

  @Override
  public ParameterElement[] getParameters() {
    return parameters;
  }

  @Override
  public SourceRange getVisibleRange() {
    if (visibleRangeLength < 0) {
      return null;
    }
    return new SourceRange(visibleRangeOffset, visibleRangeLength);
  }

  @Override
  public boolean isInitializingFormal() {
    return false;
  }

  @Override
  public boolean isPotentiallyMutatedInClosure() {
    return potentiallyMutatedInClosure;
  }

  @Override
  public boolean isPotentiallyMutatedInScope() {
    return potentiallyMutatedInScope;
  }

  /**
   * Specifies that this variable is potentially mutated somewhere in closure.
   */
  public void markPotentiallyMutatedInClosure() {
    potentiallyMutatedInClosure = true;
  }

  /**
   * Specifies that this variable is potentially mutated somewhere in its scope.
   */
  public void markPotentiallyMutatedInScope() {
    potentiallyMutatedInScope = true;
  }

  /**
   * Set the range of the default value for this parameter to the range starting at the given offset
   * with the given length.
   * 
   * @param offset the offset to the beginning of the default value range for this element
   * @param length the length of the default value range for this element, or {@code -1} if this
   *          element does not have a default value
   */
  public void setDefaultValueRange(int offset, int length) {
    defaultValueRangeOffset = offset;
    defaultValueRangeLength = length;
  }

  /**
   * Set the kind of this parameter to the given kind.
   * 
   * @param parameterKind the new kind of this parameter
   */
  public void setParameterKind(ParameterKind parameterKind) {
    this.parameterKind = parameterKind;
  }

  /**
   * Set the parameters defined by this executable element to the given parameters.
   * 
   * @param parameters the parameters defined by this executable element
   */
  public void setParameters(ParameterElement[] parameters) {
    for (ParameterElement parameter : parameters) {
      ((ParameterElementImpl) parameter).setEnclosingElement(this);
    }
    this.parameters = parameters;
  }

  /**
   * Set the visible range for this element to the range starting at the given offset with the given
   * length.
   * 
   * @param offset the offset to the beginning of the visible range for this element
   * @param length the length of the visible range for this element, or {@code -1} if this element
   *          does not have a visible range
   */
  public void setVisibleRange(int offset, int length) {
    visibleRangeOffset = offset;
    visibleRangeLength = length;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(parameters, visitor);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    String left = "";
    String right = "";
    switch (getParameterKind()) {
      case NAMED:
        left = "{";
        right = "}";
        break;
      case POSITIONAL:
        left = "[";
        right = "]";
        break;
      case REQUIRED:
        // No need to change the default.
        break;
    }
    builder.append(left);
    appendToWithoutDelimiters(builder);
    builder.append(right);
  }

  /**
   * Append the type and name of this parameter to the given builder.
   * 
   * @param builder the builder to which the type and name are to be appended
   */
  protected void appendToWithoutDelimiters(StringBuilder builder) {
    builder.append(getType());
    builder.append(" ");
    builder.append(getDisplayName());
  }
}

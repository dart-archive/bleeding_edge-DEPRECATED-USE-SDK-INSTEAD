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
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.utilities.source.SourceRange;

/**
 * Instances of the class {@code FunctionElementImpl} implement a {@code FunctionElement}.
 * 
 * @coverage dart.engine.element
 */
public class FunctionElementImpl extends ExecutableElementImpl implements FunctionElement {
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
   * An empty array of function elements.
   */
  public static final FunctionElement[] EMPTY_ARRAY = new FunctionElement[0];

  /**
   * Initialize a newly created synthetic function element.
   */
  public FunctionElementImpl() {
    super("", -1);
    setSynthetic(true);
  }

  /**
   * Initialize a newly created function element to have the given name.
   * 
   * @param name the name of this element
   */
  public FunctionElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created function element to have no name and the given offset. This is used
   * for function expressions, which have no name.
   * 
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public FunctionElementImpl(int nameOffset) {
    super("", nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitFunctionElement(this);
  }

  /**
   * Treating the set of arrays defined in {@link ExecutableElementImpl} as one long array, this
   * returns the index position of the passed child in this {@link FunctionElement}. This gives a
   * unique integer for each element, this is provided primarily for function elements that do not
   * have a name (closures), which cannot use {@link Element#getNameOffset()}. If there is no such
   * element, {@code -1} is returned.
   * 
   * @param element the element to find and return an integer for, if there is no such element,
   *          {@code -1} is returned
   */
  public int getIndexPosition(Element element) {
    FunctionElement[] functions = getFunctions();
    LabelElement[] labels = getLabels();
    LocalVariableElement[] localVariables = getLocalVariables();
    ParameterElement[] parameters = getParameters();

    int count = 0;
    for (int i = 0; i < functions.length; i++, count++) {
      if (element == functions[i]) {
        return count;
      }
    }
    for (int i = 0; i < labels.length; i++, count++) {
      if (element == labels[i]) {
        return count;
      }
    }
    for (int i = 0; i < localVariables.length; i++, count++) {
      if (element == localVariables[i]) {
        return count;
      }
    }
    for (int i = 0; i < parameters.length; i++, count++) {
      if (element == parameters[i]) {
        return count;
      }
    }
    return -1;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.FUNCTION;
  }

  @Override
  public SourceRange getVisibleRange() {
    if (visibleRangeLength < 0) {
      return null;
    }
    return new SourceRange(visibleRangeOffset, visibleRangeLength);
  }

  @Override
  public boolean isStatic() {
    return getEnclosingElement() instanceof CompilationUnitElement;
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
  protected void appendTo(StringBuilder builder) {
    String name = getDisplayName();
    if (name != null) {
      builder.append(name);
    }
    super.appendTo(builder);
  }

  @Override
  protected String getIdentifier() {
    return getName() + "@" + getNameOffset();
  }
}

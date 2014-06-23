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

import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.translation.DartName;

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
   * Initialize a newly created function element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
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
  @DartName("forOffset")
  public FunctionElementImpl(int nameOffset) {
    super("", nameOffset);
  }

  /**
   * Initialize a newly created function element to have the given name and offset.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public FunctionElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitFunctionElement(this);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.FUNCTION;
  }

  @Override
  public FunctionDeclaration getNode() throws AnalysisException {
    return getNodeMatching(FunctionDeclaration.class);
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
    String identifier = super.getIdentifier();
    if (!isStatic()) {
      identifier += "@" + getNameOffset();
    }
    return identifier;
  }
}

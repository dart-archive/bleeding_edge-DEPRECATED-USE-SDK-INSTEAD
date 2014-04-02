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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.utilities.source.SourceRange;

/**
 * Instances of the class {@code LocalVariableElementImpl} implement a {@code LocalVariableElement}.
 * 
 * @coverage dart.engine.element
 */
public class LocalVariableElementImpl extends VariableElementImpl implements LocalVariableElement {
  /**
   * Is {@code true} if this variable is potentially mutated somewhere in its scope.
   */
  private boolean potentiallyMutatedInScope;

  /**
   * Is {@code true} if this variable is potentially mutated somewhere in closure.
   */
  private boolean potentiallyMutatedInClosure;

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
  public static final LocalVariableElement[] EMPTY_ARRAY = new LocalVariableElement[0];

  /**
   * Initialize a newly created local variable element to have the given name.
   * 
   * @param name the name of this element
   */
  public LocalVariableElementImpl(Identifier name) {
    super(name);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitLocalVariableElement(this);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.LOCAL_VARIABLE;
  }

  @Override
  public ToolkitObjectElement[] getToolkitObjects() {
    CompilationUnitElementImpl unit = getAncestor(CompilationUnitElementImpl.class);
    if (unit == null) {
      return ToolkitObjectElement.EMPTY_ARRAY;
    }
    return unit.getToolkitObjects(this);
  }

  @Override
  public SourceRange getVisibleRange() {
    if (visibleRangeLength < 0) {
      return null;
    }
    return new SourceRange(visibleRangeOffset, visibleRangeLength);
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
   * Set the toolkit specific information objects attached to this variable.
   * 
   * @param toolkitObjects the toolkit objects attached to this variable
   */
  public void setToolkitObjects(ToolkitObjectElement[] toolkitObjects) {
    CompilationUnitElementImpl unit = getAncestor(CompilationUnitElementImpl.class);
    if (unit == null) {
      return;
    }
    unit.setToolkitObjects(this, toolkitObjects);
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
    builder.append(getType());
    builder.append(" ");
    builder.append(getDisplayName());
  }

  @Override
  protected String getIdentifier() {
    return super.getIdentifier() + "@" + getNameOffset();
  }
}

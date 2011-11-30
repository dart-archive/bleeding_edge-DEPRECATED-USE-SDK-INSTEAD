/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>DeclarationElementInfo</code> define the behavior of elements that
 * declare a program element.
 */
public class DeclarationElementInfo extends SourceElementWithChildrenInfo {
  /**
   * The range in the source of the DartDoc associated with this info's element, or
   * <code>null</code> if the element does not have any DartDoc associated with it.
   */
  private SourceRange dartDocRange;

  /**
   * The modifiers associated with the declared element.
   */
  private Modifiers modifiers = Modifiers.NONE;

  /**
   * Return the range in the source of the DartDoc associated with this info's element, or
   * <code>null</code> if the element does not have any DartDoc associated with it.
   * 
   * @return the range in the source of the DartDoc associated with this info's element
   */
  public SourceRange getDartDocRange() {
    return dartDocRange;
  }

  /**
   * Return the modifiers associated with the declared element.
   * 
   * @return the modifiers associated with the declared element
   */
  public Modifiers getModifiers() {
    return modifiers;
  }

  /**
   * Set the range in the source of the DartDoc associated with this info's element to the given
   * range.
   * 
   * @param range the range in the source of the DartDoc associated with this info's element
   */
  public void setDartDocRange(SourceRange range) {
    dartDocRange = range;
  }

  /**
   * Set the modifiers associated with the declared element to the given modifiers.
   * 
   * @param newModifiers the modifiers to be associated with the declared element
   */
  public void setModifiers(Modifiers newModifiers) {
    modifiers = newModifiers;
  }
}

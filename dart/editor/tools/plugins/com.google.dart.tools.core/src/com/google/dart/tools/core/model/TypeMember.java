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
package com.google.dart.tools.core.model;

/**
 * The interface <code>TypeMember</code> defines the behavior common to objects representing members
 * defined in types.
 */
public interface TypeMember extends CompilationUnitElement, ParentElement, SourceManipulation,
    SourceReference, DartDocumentable {

  /**
   * Return the type in which this member is declared, or <code>null</code> if this member is not
   * declared in a type.
   * 
   * @return the type in which this member is declared
   */
  public Type getDeclaringType();

  /**
   * Return the modifiers that were explicitly declared for this element.
   * 
   * @return the modifiers that were explicitly declared for this element
   */
  public DartModifiers getModifiers();

  /**
   * Return <code>true</code> if the element is private to the library in which it is defined.
   * 
   * @return <code>true</code> if the element is private to the library in which it is defined
   */
  @Override
  public boolean isPrivate();

  /**
   * Return <code>true</code> if the element is defined with the static modifier.
   * 
   * @return <code>true</code> if the element is defined with the static modifier
   */
  public boolean isStatic();
}

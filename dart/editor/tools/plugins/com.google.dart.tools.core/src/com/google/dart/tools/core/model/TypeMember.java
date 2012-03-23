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
package com.google.dart.tools.core.model;

/**
 * The interface <code>TypeMember</code> defines the behavior common to objects representing members
 * defined in types.
 */
public interface TypeMember extends CompilationUnitElement, ParentElement, SourceManipulation,
    SourceReference {

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
   * Search the supertypes of this member's declaring type for any types that define a member that
   * is overridden by this member. Return an array containing all of the overridden members, or an
   * empty array if there are no overridden members. The members in the array are not guaranteed to
   * be in any particular order.
   * <p>
   * The result will contain only immediately overridden members. For example, given a class
   * <code>A</code>, a class <code>B</code> that extends <code>A</code>, and a class <code>C</code>
   * that extends <code>B</code>, all three of which define a method <code>m</code>, asking the
   * method defined in class <code>C</code> for it's overridden methods will return an array
   * containing only the method defined in <code>B</code>.
   * 
   * @return an array containing all of the members declared in supertypes of this member's
   *         declaring type that are overridden by this member
   * @throws DartModelException if the list of overridden members could not be computed
   */
  public TypeMember[] getOverriddenMembers() throws DartModelException;

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

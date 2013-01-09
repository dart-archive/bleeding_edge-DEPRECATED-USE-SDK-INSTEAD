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
package com.google.dart.tools.core.model;

/**
 * The interface <code>DartClassTypeAlias</code> defines the behavior of objects representing class
 * type aliases defined in compilation units.
 */
public interface DartClassTypeAlias extends CompilationUnitElement, SourceManipulation,
    SourceReference {
  /**
   * Return the name of this class' superclass, or <code>null</code> for source types that do not
   * specify a superclass.
   * <p>
   * For interfaces, the superclass name is always <code>null</code>.
   * 
   * @return the name of this class' superclass, or <code>null</code> for source types that do not
   *         specify a superclass
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public String getSuperclassName() throws DartModelException;

  /**
   * Return the names of interfaces that this type implements or extends, in the order in which they
   * are listed in the source.
   * <p>
   * For classes, this gives the interfaces that this class implements. For interfaces, this gives
   * the interfaces that this interface extends. An empty collection is returned if this type does
   * not implement or extend any interfaces.
   * 
   * @return the names of interfaces that this type implements or extends, in the order in which
   *         they are listed in the source, or an empty collection if none
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public String[] getSuperInterfaceNames() throws DartModelException;

  /**
   * Return the names of the supertypes of this type, including both the superclass name (if it
   * exists) and the names of the superinterfaces.
   * 
   * @return the names of the supertypes of this type
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public String[] getSupertypeNames() throws DartModelException;

  /**
   * @return the {@link DartTypeParameter}s declared by this type
   */
  public DartTypeParameter[] getTypeParameters() throws DartModelException;
}

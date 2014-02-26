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

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The interface <code>Type</code> defines the behavior of objects representing types defined in
 * compilation units.
 * 
 * @coverage dart.tools.core.model
 */
public interface Type extends CompilationUnitElement, ParentElement, SourceReference {
  /**
   * Finds the methods in this type that correspond to the given method. A method m1 corresponds to
   * another method m2 if:
   * <ul>
   * <li>m1 has the same element name as m2.
   * <li>m1 has the same number of arguments as m2 and the simple names of the argument types must
   * be equals.
   * <li>m1 exists.
   * </ul>
   * 
   * @param method the given method
   * @return the found method or <code>null</code> if no such methods can be found
   */
  public Method[] findMethods(Method method);

  /**
   * Return the fields declared by this type in the order in which they appear in the source file.
   * 
   * @return the fields declared by this type
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public Field[] getFields() throws DartModelException;

  /**
   * Return the library containing this type, or <code>null</code> if this type is not defined in a
   * library.
   * 
   * @return the library containing this type
   */
  public DartLibrary getLibrary();

  /**
   * Return the method with the specified name and parameter types in this type (for example,
   * <code>"foo", {"I", "QString;"}</code>). To get the handle for a constructor, the name specified
   * must be the simple name of the enclosing type. This is a handle-only method. The method may or
   * may not be present.
   * <p>
   * 
   * @param name the given name
   * @param parameterTypeSignatures the given parameter types
   * @return the method with the specified name and parameter types in this type
   */
  public Method getMethod(String name, String[] parameterTypeSignatures);

  /**
   * Return the methods and constructors declared by this type.
   * <p>
   * The results are listed in the order in which they appear in the source or class file.
   * 
   * @return the methods and constructors declared by this type
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public Method[] getMethods() throws DartModelException;

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
   * Return the qualified type name, using the given char as a separator.
   * 
   * @param separatorChar the separator char
   * @return the qualified type name
   * @deprecated Dart does not have a notion of qualified type names - this method should be removed
   */
  @Deprecated
  public String getTypeQualifiedName(char separatorChar);

  /**
   * Return <code>true</code> if this type represents either an interface or an abstract class.
   * 
   * @return <code>true</code> if this type represents an abstract class
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public boolean isAbstract() throws DartModelException;

  /**
   * Return <code>true</code> if this type represents a class.
   * 
   * @return <code>true</code> if this type represents a class
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public boolean isClass() throws DartModelException;

  /**
   * Return <code>true</code> if this type represents an interface.
   * 
   * @return <code>true</code> if this type represents an interface
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public boolean isInterface() throws DartModelException;

  /**
   * Creates and returns a type hierarchy for this type containing this type and all of its
   * supertypes.
   * 
   * @param monitor the given progress monitor
   * @exception DartModelException if this element does not exist or if an exception occurs while
   *              accessing its corresponding resource.
   * @return a type hierarchy for this type containing this type and all of its supertypes
   */
  public TypeHierarchy newSupertypeHierarchy(IProgressMonitor progressMonitor)
      throws DartModelException;

}

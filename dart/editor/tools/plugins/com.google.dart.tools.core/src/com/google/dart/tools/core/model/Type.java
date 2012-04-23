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

import com.google.dart.tools.core.completion.CompletionRequestor;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The interface <code>Type</code> defines the behavior of objects representing types defined in
 * compilation units.
 */
public interface Type extends CompilationUnitElement, ParentElement, SourceManipulation,
    SourceReference {
  /**
   * Do code completion inside a code snippet in the context of the current type.
   * <p>
   * If the type has access to its source code and the insertion position is valid, then completion
   * is performed against the source. Otherwise the completion is performed against the type
   * structure and the given locals variables.
   * 
   * @param snippet the code snippet
   * @param insertion the position with in source where the snippet is inserted. This position must
   *          not be in comments. A possible value is -1, if the position is not known.
   * @param position the position within snippet where the user is performing code assist.
   * @param localVariableTypeNames an array (possibly empty) of fully qualified type names of local
   *          variables visible at the current scope
   * @param localVariableNames an array (possibly empty) of local variable names that are visible at
   *          the current scope
   * @param localVariableModifiers an array (possible empty) of modifiers for local variables
   * @param isStatic whether the current scope is in a static context
   * @param requestor the completion requestor
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public void codeComplete(char[] snippet, int insertion, int position,
      char[][] localVariableTypeNames, char[][] localVariableNames, int[] localVariableModifiers,
      boolean isStatic, CompletionRequestor requestor) throws DartModelException;

  /**
   * Do code completion inside a code snippet in the context of the current type.
   * <p>
   * If the type has access to its source code and the insertion position is valid, then completion
   * is performed against the source. Otherwise the completion is performed against the type
   * structure and the given locals variables.
   * <p>
   * If {@link IProgressMonitor} is not <code>null</code> then some proposals which can be very long
   * to compute are proposed. To avoid that the code assist operation take too much time a
   * {@link IProgressMonitor} which automatically cancel the code assist operation when a specified
   * amount of time is reached could be used.
   * 
   * <pre>
   * new IProgressMonitor() {
   *     private final static int TIMEOUT = 500; //ms
   *     private long endTime;
   *     public void beginTask(String name, int totalWork) {
   *         fEndTime= System.currentTimeMillis() + TIMEOUT;
   *     }
   *     public boolean isCanceled() {
   *         return endTime <= System.currentTimeMillis();
   *     }
   *     ...
   * };
   * </pre>
   * 
   * @param snippet the code snippet
   * @param insertion the position with in source where the snippet is inserted. This position must
   *          not be in comments. A possible value is -1, if the position is not known.
   * @param position the position within snippet where the user is performing code assist.
   * @param localVariableTypeNames an array (possibly empty) of fully qualified type names of local
   *          variables visible at the current scope
   * @param localVariableNames an array (possibly empty) of local variable names that are visible at
   *          the current scope
   * @param localVariableModifiers an array (possible empty) of modifiers for local variables
   * @param isStatic whether the current scope is in a static context
   * @param requestor the completion requestor
   * @param monitor the progress monitor used to report progress
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public void codeComplete(char[] snippet, int insertion, int position,
      char[][] localVariableTypeNames, char[][] localVariableNames, int[] localVariableModifiers,
      boolean isStatic, CompletionRequestor requestor, IProgressMonitor monitor)
      throws DartModelException;

  /**
   * Do code completion inside a code snippet in the context of the current type. It considers types
   * in the working copies with the given owner first. In other words, the owner's working copies
   * will take precedence over their original compilation units in the workspace.
   * <p>
   * Note that if a working copy is empty, it will be as if the original compilation unit had been
   * deleted.
   * <p>
   * If the type has access to its source code and the insertion position is valid, then completion
   * is performed against the source. Otherwise the completion is performed against the type
   * structure and the given locals variables.
   * 
   * @param snippet the code snippet
   * @param insertion the position with in source where the snippet is inserted. This position must
   *          not be in comments. A possible value is -1, if the position is not known.
   * @param position the position with in snippet where the user is performing code assist.
   * @param localVariableTypeNames an array (possibly empty) of fully qualified type names of local
   *          variables visible at the current scope
   * @param localVariableNames an array (possibly empty) of local variable names that are visible at
   *          the current scope
   * @param localVariableModifiers an array (possible empty) of modifiers for local variables
   * @param isStatic whether the current scope is in a static context
   * @param requestor the completion requestor
   * @param owner the owner of working copies that take precedence over their original compilation
   *          units
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public void codeComplete(char[] snippet, int insertion, int position,
      char[][] localVariableTypeNames, char[][] localVariableNames, int[] localVariableModifiers,
      boolean isStatic, CompletionRequestor requestor, WorkingCopyOwner owner)
      throws DartModelException;

  /**
   * Do code completion inside a code snippet in the context of the current type. It considers types
   * in the working copies with the given owner first. In other words, the owner's working copies
   * will take precedence over their original compilation units in the workspace.
   * <p>
   * Note that if a working copy is empty, it will be as if the original compilation unit had been
   * deleted.
   * </p>
   * <p>
   * If the type has access to its source code and the insertion position is valid, then completion
   * is performed against the source. Otherwise the completion is performed against the type
   * structure and the given locals variables.
   * </p>
   * <p>
   * If {@link IProgressMonitor} is not <code>null</code> then some proposals which can be very long
   * to compute are proposed. To avoid that the code assist operation take too much time a
   * {@link IProgressMonitor} which automatically cancel the code assist operation when a specified
   * amount of time is reached could be used.
   * 
   * <pre>
   * new IProgressMonitor() {
   *     private final static int TIMEOUT = 500; //ms
   *     private long endTime;
   *     public void beginTask(String name, int totalWork) {
   *         endTime= System.currentTimeMillis() + TIMEOUT;
   *     }
   *     public boolean isCanceled() {
   *         return endTime <= System.currentTimeMillis();
   *     }
   *     ...
   * };
   * </pre>
   * 
   * @param snippet the code snippet
   * @param insertion the position with in source where the snippet is inserted. This position must
   *          not be in comments. A possible value is -1, if the position is not known.
   * @param position the position with in snippet where the user is performing code assist.
   * @param localVariableTypeNames an array (possibly empty) of fully qualified type names of local
   *          variables visible at the current scope
   * @param localVariableNames an array (possibly empty) of local variable names that are visible at
   *          the current scope
   * @param localVariableModifiers an array (possible empty) of modifiers for local variables
   * @param isStatic whether the current scope is in a static context
   * @param requestor the completion requestor
   * @param owner the owner of working copies that take precedence over their original compilation
   *          units
   * @param monitor the progress monitor used to report progress
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public void codeComplete(char[] snippet, int insertion, int position,
      char[][] localVariableTypeNames, char[][] localVariableNames, int[] localVariableModifiers,
      boolean isStatic, CompletionRequestor requestor, WorkingCopyOwner owner,
      IProgressMonitor monitor) throws DartModelException;

  /**
   * Create and return a field in this type with the given contents.
   * <p>
   * Optionally, the new element can be positioned before the specified sibling. If no sibling is
   * specified, the element will be inserted as the last field declaration in this type.
   * <p>
   * It is possible that a field with the same name already exists in this type. The value of the
   * <code>force</code> parameter affects the resolution of such a conflict:
   * <ul>
   * <li> <code>true</code> - in this case the field is created with the new contents</li>
   * <li> <code>false</code> - in this case a <code>DartModelException</code> is thrown</li>
   * </ul>
   * 
   * @param contents the given contents
   * @param sibling the given sibling
   * @param force a flag in case the same name already exists in this type
   * @param monitor the given progress monitor
   * @return a field in this type with the given contents
   * @throws DartModelException if the element could not be created. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li>A <code>
   *           CoreException</code> occurred while updating an underlying resource <li>The specified
   *           sibling is not a child of this type (INVALID_SIBLING) <li>The contents could not be
   *           recognized as a field declaration (INVALID_CONTENTS) <li>This type is read-only
   *           (binary) (READ_ONLY) <li>There was a naming collision with an existing field
   *           (NAME_COLLISION)
   *           </ul>
   */
  public Field createField(String contents, TypeMember sibling, boolean force,
      IProgressMonitor monitor) throws DartModelException;

  /**
   * Create and return a method or constructor in this type with the given contents.
   * <p>
   * Optionally, the new element can be positioned before the specified sibling. If no sibling is
   * specified, the element will be appended to this type.
   * <p>
   * It is possible that a method with the same signature already exists in this type. The value of
   * the <code>force</code> parameter affects the resolution of such a conflict:
   * <ul>
   * <li> <code>true</code> - in this case the method is created with the new contents</li>
   * <li> <code>false</code> - in this case a <code>DartModelException</code> is thrown</li>
   * </ul>
   * 
   * @param contents the given contents
   * @param sibling the given sibling
   * @param force a flag in case the same name already exists in this type
   * @param monitor the given progress monitor
   * @return a method or constructor in this type with the given contents
   * @throws DartModelException if the element could not be created. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li>A <code>
   *           CoreException</code> occurred while updating an underlying resource <li>The specified
   *           sibling is not a child of this type (INVALID_SIBLING) <li>The contents could not be
   *           recognized as a method or constructor declaration (INVALID_CONTENTS) <li>This type is
   *           read-only (READ_ONLY) <li> There was a naming collision with an existing method
   *           (NAME_COLLISION)
   *           </ul>
   */
  public Method createMethod(String contents, TypeMember sibling, boolean force,
      IProgressMonitor monitor) throws DartModelException;

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
   * Return all of the existing members in this type that have the given name.
   * 
   * @param memberName the name of the members to be returned
   * @return all of the members in this type that have the given name
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public TypeMember[] getExistingMembers(String memberName) throws DartModelException;

  /**
   * Return the field with the specified name in this type (for example, <code>"bar"</code>). This
   * is a handle-only method. The field may or may not exist.
   * 
   * @param fieldName the given name
   * @return the field with the specified name in this type
   */
  public Field getField(String fieldName);

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
   * @return the {@link DartTypeParameter}s declared by this type
   */
  public DartTypeParameter[] getTypeParameters() throws DartModelException;

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

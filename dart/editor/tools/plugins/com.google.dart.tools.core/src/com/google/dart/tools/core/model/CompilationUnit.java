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

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.problem.ProblemRequestor;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The interface <code>CompilationUnit</code> defines the behavior of objects representing files
 * containing Dart source code that needs to be compiled.
 */
public interface CompilationUnit extends CodeAssistElement, SourceFileElement<CompilationUnit>,
    OpenableElement, ParentElement, SourceManipulation, SourceReference {
  /**
   * Return the Dart elements corresponding to the given selected text in this compilation unit. The
   * <code>offset</code> is the 0-based index of the first selected character. The
   * <code>length</code> is the number of selected characters. It considers types in the working
   * copies with the given owner first. In other words, the owner's working copies will take
   * precedence over their original compilation units in the workspace.
   * <p>
   * Note that if the <code>length</code> is 0 and the <code>offset</code> is inside an identifier
   * or the index just after an identifier then this identifier is considered as the selection.
   * <p>
   * Note that if a working copy is empty, it will be as if the original compilation unit had been
   * deleted.
   * 
   * @param ast the AST structure representing the current state of this compilation unit
   * @param offset the given offset position
   * @param length the number of selected characters
   * @param owner the owner of working copies that take precedence over their original compilation
   *          units
   * @return the Dart elements corresponding to the given selected text
   * @throws DartModelException if code resolve could not be performed. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li> The range
   *           specified is not within this element's source range (INDEX_OUT_OF_BOUNDS)
   *           </ul>
   */
  public DartElement[] codeSelect(DartUnit ast, int offset, int length, WorkingCopyOwner owner)
      throws DartModelException;

  /**
   * Create and return a type in this compilation unit with the given contents. If this compilation
   * unit does not exist, one will be created.
   * <p>
   * Optionally, the new type can be positioned before the specified sibling. If
   * <code>sibling</code> is <code>null</code>, the type will be appended to the end of this
   * compilation unit.
   * <p>
   * It is possible that a type with the same name already exists in this compilation unit. The
   * value of the <code>force</code> parameter affects the resolution of such a conflict:
   * <ul>
   * <li> <code>true</code> - in this case the type is created with the new contents</li>
   * <li> <code>false</code> - in this case a {@link DartModelException} is thrown</li>
   * </ul>
   * 
   * @param contents the source contents of the type declaration to add
   * @param sibling the existing element which the type will be inserted immediately before (if
   *          <code>null</code>, then this type will be inserted as the last type declaration
   * @param force a <code>boolean</code> flag indicating how to deal with duplicates
   * @param monitor the progress monitor to notify
   * @return the newly inserted type
   * @throws DartModelException if the element could not be created. Reasons include:
   *           <ul>
   *           <li>The specified sibling element does not exist (ELEMENT_DOES_NOT_EXIST)</li> <li> A
   *           {@link org.eclipse.core.runtime.CoreException} occurred while updating an underlying
   *           resource <li> The specified sibling is not a child of this compilation unit
   *           (INVALID_SIBLING) <li> The contents could not be recognized as a type declaration
   *           (INVALID_CONTENTS) <li> There was a naming collision with an existing type
   *           (NAME_COLLISION)
   *           </ul>
   */
  public Type createType(String contents, DartElement sibling, boolean force,
      IProgressMonitor monitor) throws DartModelException;

  /**
   * Return <code>true</code> if this compilation unit defines a library (by including a library
   * directive).
   * 
   * @return <code>true</code> if this compilation unit defines a library
   */
  public boolean definesLibrary();

  /**
   * Finds the elements in this Dart file that correspond to the given element. An element A
   * corresponds to an element B if:
   * <ul>
   * <li>A has the same element name as B.
   * <li>If A is a method, A must have the same number of arguments as B and the simple names of the
   * argument types must be equal, if known.
   * <li>The parent of A corresponds to the parent of B recursively up to their respective Dart
   * files.
   * <li>A exists.
   * </ul>
   * Returns <code>null</code> if no such Dart elements can be found or if the given element is not
   * included in a Dart file.
   * 
   * @param element the given element
   * @return the found elements in this Dart file that correspond to the given element
   */
  public DartElement[] findElements(DartElement element);

  /**
   * Returns the most narrow element including the given offset.
   * 
   * @param offset the offset included by the retrieved element
   * @return the most narrow element which includes the given offset
   * @throws DartModelException if the contents of the compilation unit cannot be accessed
   */
  public DartElement getElementAt(int offset) throws DartModelException;

  /**
   * Return an array containing the top-level function type aliases defined in this compilation
   * unit.
   * 
   * @return the top-level function type aliases defined in this compilation unit
   * @throws DartModelException if the function type aliases defined in this compilation unit cannot
   *           be determined
   */
  public DartFunctionTypeAlias[] getFunctionTypeAliases() throws DartModelException;

  /**
   * Return an array containing the global (top-level) variables defined in this compilation unit.
   * 
   * @return the global variables defined in this compilation unit
   * @throws DartModelException if the global variables defined in this compilation unit cannot be
   *           determined
   */
  public DartVariableDeclaration[] getGlobalVariables() throws DartModelException;

  /**
   * Return the library containing this compilation unit, or <code>null</code> if this compilation
   * unit is not defined in a library.
   * 
   * @return the library containing this compilation unit
   */
  public DartLibrary getLibrary();

  /**
   * Return the top-level type declared in this compilation unit with the given simple type name.
   * The type name must be valid. This is a handle-only method. The type may or may not exist.
   * 
   * @param name the simple name of the requested type in the compilation unit
   * @return a handle onto the corresponding type. The type may or may not exist.
   * @see DartConventions#validateTypeName(String name)
   */
  public Type getType(String name);

  /**
   * Return an array containing the top-level types defined in this compilation unit.
   * 
   * @return the top-level types defined in this compilation unit
   * @throws DartModelException if the types defined in this compilation unit cannot be determined
   */
  public Type[] getTypes() throws DartModelException;

  /**
   * Return true if this compilation unit contains a top level function main().
   * 
   * @return if this compilation unit contains a top level function main()
   * @throws DartModelException
   */
  public boolean hasMain() throws DartModelException;

  /**
   * Changes this compilation unit handle into a working copy. A new {@link IBuffer} is created
   * using this compilation unit handle's owner. Uses the primary owner if none was specified when
   * this compilation unit handle was created.
   * <p>
   * When switching to working copy mode, problems are reported to given {@link ProblemRequestor}.
   * Note that once in working copy mode, the given {@link ProblemRequestor} is ignored. Only the
   * original {@link ProblemRequestor} is used to report subsequent problems.
   * </p>
   * <p>
   * Once in working copy mode, changes to this compilation unit or its children are done in memory.
   * Only the new buffer is affected. Using {@link #commitWorkingCopy(boolean, IProgressMonitor)}
   * will bring the underlying resource in sync with this compilation unit.
   * </p>
   * <p>
   * If this compilation unit was already in working copy mode, an internal counter is incremented
   * and no other action is taken on this compilation unit. To bring this compilation unit back into
   * the original mode (where it reflects the underlying resource), {@link #discardWorkingCopy} must
   * be call as many times as {@link #becomeWorkingCopy(ProblemRequestor, IProgressMonitor)}.
   * </p>
   * 
   * @param problemRequestor a requestor which will get notified of problems detected during
   *          reconciling as they are discovered. The requestor can be set to <code>null</code>
   *          indicating that the client is not interested in problems.
   * @param monitor a progress monitor used to report progress while opening this compilation unit
   *          or <code>null</code> if no progress should be reported
   * @throws JavaModelException if this compilation unit could not become a working copy.
   * @see #discardWorkingCopy()
   * @since 3.0
   * @deprecated Use {@link #becomeWorkingCopy(IProgressMonitor)} instead. Note that if this
   *             deprecated method is used, problems will be reported to the given problem requestor
   *             as well as the problem requestor returned by the working copy owner (if not null).
   */
  @Deprecated
  void becomeWorkingCopy(ProblemRequestor problemRequestor, IProgressMonitor monitor)
      throws DartModelException;

}

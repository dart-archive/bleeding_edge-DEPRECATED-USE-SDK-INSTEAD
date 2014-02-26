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
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

/**
 * The interface <code>CompilationUnit</code> defines the behavior of objects representing files
 * containing Dart source code that needs to be compiled.
 * 
 * @coverage dart.tools.core.model
 */
public interface CompilationUnit extends CodeAssistElement, SourceFileElement<CompilationUnit>,
    OpenableElement, ParentElement, SourceReference {

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
   * Return the library containing this compilation unit, or <code>null</code> if this compilation
   * unit is not defined in a library.
   * 
   * @return the library containing this compilation unit
   */
  public DartLibrary getLibrary();

  /**
   * Return an array containing the top-level types defined in this compilation unit.
   * 
   * @return the top-level types defined in this compilation unit
   * @throws DartModelException if the types defined in this compilation unit cannot be determined
   */
  public Type[] getTypes() throws DartModelException;
}

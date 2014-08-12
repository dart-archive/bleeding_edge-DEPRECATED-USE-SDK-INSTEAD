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
 * The interface <code>CompilationUnit</code> defines the behavior of objects representing files
 * containing Dart source code that needs to be compiled.
 * 
 * @coverage dart.tools.core.model
 */
public interface CompilationUnit extends CodeAssistElement, SourceFileElement<CompilationUnit>,
    OpenableElement, ParentElement, SourceReference {

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
}

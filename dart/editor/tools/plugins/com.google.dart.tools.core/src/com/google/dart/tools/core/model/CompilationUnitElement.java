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
 * The interface <code>CompilationUnitElement</code> defines the behavior common to
 * {@link DartElement Dart elements} that are contained, either directly or indirectly, in a
 * {@link CompilationUnit compilation unit}.
 */
public interface CompilationUnitElement extends DartElement, DartDocumentable {
  /**
   * Return the compilation unit in which this element is declared, or <code>null</code> if this
   * element is not declared in a compilation unit.
   * 
   * @return the compilation unit in which this element is declared
   */
  public CompilationUnit getCompilationUnit();

  /**
   * Return <code>true</code> if the element is private to the library in which it is defined.
   * 
   * @return <code>true</code> if the element is private to the library in which it is defined
   */
  public boolean isPrivate();
}

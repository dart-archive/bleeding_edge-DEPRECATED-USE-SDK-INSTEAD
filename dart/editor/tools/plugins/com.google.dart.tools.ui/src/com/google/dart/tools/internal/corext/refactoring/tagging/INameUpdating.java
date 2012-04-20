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
package com.google.dart.tools.internal.corext.refactoring.tagging;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Represents processors in the JDT space that rename elements.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public interface INameUpdating {

  /**
   * Checks if the new name is valid for the entity that this refactoring renames.
   * 
   * @param newName the new name
   * @return returns the resulting status
   * @throws CoreException Core exception is thrown when the validation could not be performed
   */
  RefactoringStatus checkNewElementName(String newName) throws CoreException;

  /**
   * Gets the current name of the entity that this refactoring is working on.
   * 
   * @return returns the current name
   */
  String getCurrentElementName();

  /**
   * Gets the original elements. Since an <code>INameUpdating</code> only renames one element, this
   * method must return an array containing exactly one element.
   * 
   * @return an array containing exactly one element
   * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
   */
  Object[] getElements();

  /**
   * Gets the element after renaming, or <code>null</code> if not available.
   * 
   * @return returns the new element or <code>null</code>
   * 
   * @throws CoreException thrown when the new element could not be evaluated
   */
  Object getNewElement() throws CoreException;

  /**
   * Get the name for the entity that this refactoring is working on.
   * 
   * @return returns the new name
   */
  String getNewElementName();

  /**
   * Sets new name for the entity that this refactoring is working on.
   * 
   * @param newName the new name
   */
  void setNewElementName(String newName);
}

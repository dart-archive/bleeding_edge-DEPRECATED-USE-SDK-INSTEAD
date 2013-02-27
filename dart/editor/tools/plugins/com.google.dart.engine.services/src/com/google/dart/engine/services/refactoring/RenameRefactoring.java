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

package com.google.dart.engine.services.refactoring;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.services.status.RefactoringStatus;

/**
 * Abstract {@link Refactoring} to rename some {@link Element}.
 */
public interface RenameRefactoring extends Refactoring {
  /**
   * Validates new {@link Element} name.
   */
  RefactoringStatus checkNewName(String newName);

  /**
   * @return the current name for the {@link Element}.
   */
  String getCurrentName();

  /**
   * @return the new name for the {@link Element}.
   */
  String getNewName();

  /**
   * Sets the new name for the {@link Element}.
   */
  void setNewName(String newName);
}

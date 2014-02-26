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
package com.google.dart.tools.internal.corext.refactoring;

import org.eclipse.core.resources.IResource;

/**
 * Helper class to detect whether a certain refactoring can be enabled on a selection.
 * <p>
 * This class has been introduced to decouple actions from the refactoring code, in order not to
 * eagerly load refactoring classes during action initialization.
 * </p>
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RefactoringAvailabilityTester {
  public static boolean isRenameAvailable(IResource resource) {
    if (resource == null) {
      return false;
    }
    if (!resource.exists()) {
      return false;
    }
    if (!resource.isAccessible()) {
      return false;
    }
    return true;
  }
}

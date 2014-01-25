/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.status.RefactoringStatus;

/**
 * {@link Refactoring} for renaming an {@link AngularComponentElement} in its template.
 */
public class RenameAngularComponentRefactoringImpl extends RenameAngularElementRefactoringImpl {
  public RenameAngularComponentRefactoringImpl(SearchEngine searchEngine,
      AngularComponentElement element) {
    super(searchEngine, element);
  }

  @Override
  public String getRefactoringName() {
    return "Rename Angular Component in its template";
  }

  @Override
  protected RefactoringStatus checkNameConflicts(String newName) {
    // It is OK to have several component with the same name.
    // What we should check is that selector is unique.
    return new RefactoringStatus();
  }

  @Override
  protected RefactoringStatus checkNameSyntax(String newName) {
    return NamingConventions.validateAngularComponentName(newName);
  }
}

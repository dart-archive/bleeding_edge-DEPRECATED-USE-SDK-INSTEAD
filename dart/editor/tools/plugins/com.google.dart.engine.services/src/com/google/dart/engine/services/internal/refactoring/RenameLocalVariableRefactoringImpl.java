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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.status.RefactoringStatus;

import java.util.List;

/**
 * {@link Refactoring} for renaming {@link VariableElement}.
 */
public class RenameLocalVariableRefactoringImpl extends RenameRefactoringImpl {
  private final VariableElement element;

  public RenameLocalVariableRefactoringImpl(SearchEngine searchEngine, VariableElement element) {
    super(searchEngine, element);
    this.element = element;
  }

  @Override
  public RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception {
    RefactoringStatus result = new RefactoringStatus();
    return result;
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    return NamingConventions.validateVariableName(newName);
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    SourceChange change = new SourceChange(getRefactoringName(), elementSource);
    // update declaration
    change.addEdit(createDeclarationRenameEdit());
    // update references
    List<SearchMatch> references = searchEngine.searchReferences(element, null, null);
    for (SearchMatch reference : references) {
      change.addEdit(createReferenceRenameEdit(reference));
    }
    return change;
  }

  @Override
  public String getRefactoringName() {
    return "Rename Local Variable";
  }
}

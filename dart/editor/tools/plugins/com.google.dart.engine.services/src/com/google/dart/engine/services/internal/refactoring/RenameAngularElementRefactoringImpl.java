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

import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.change.SourceChangeManager;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.Source;

import java.util.List;

/**
 * {@link Refactoring} for renaming some {@link AngularElement}.
 */
abstract public class RenameAngularElementRefactoringImpl extends RenameRefactoringImpl {
  protected final AngularElement element;
  protected SourceChangeManager changeManager;

  public RenameAngularElementRefactoringImpl(SearchEngine searchEngine, AngularElement element) {
    super(searchEngine, element);
    this.element = element;
  }

  @Override
  public RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking final conditions", 1);
    try {
      return new RefactoringStatus();
    } finally {
      pm.done();
    }
  }

  @Override
  public RefactoringStatus checkNewName(String newName) {
    RefactoringStatus result = new RefactoringStatus();
    result.merge(super.checkNewName(newName));
    result.merge(checkNameSyntax(newName));
    result.merge(checkNameConflicts(newName));
    return result;
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    try {
      changeManager = new SourceChangeManager();
      // update declaration
      {
        Source elementSource = element.getSource();
        SourceChange elementChange = changeManager.get(elementSource);
        addDeclarationEdit(elementChange, element);
      }
      // update references
      List<SearchMatch> matches = searchEngine.searchReferences(element, null, null);
      List<SourceReference> references = getSourceReferences(matches);
      for (SourceReference reference : references) {
        SourceChange refChange = changeManager.get(reference.source);
        addReferenceEdit(refChange, reference);
      }
      // additional changes
      createAdditionalChanges();
      // return CompositeChange
      CompositeChange compositeChange = new CompositeChange(getRefactoringName());
      compositeChange.add(changeManager.getChanges());
      return compositeChange;
    } finally {
      pm.done();
    }
  }

  /**
   * Checks if {@link AngularElement} with the given name will conflict with any existing element.
   */
  protected abstract RefactoringStatus checkNameConflicts(String newName);

  /**
   * Check if the given name is valid for the {@link AngularElement} being renamed.
   */
  protected abstract RefactoringStatus checkNameSyntax(String newName);

  /**
   * Subclasses may override this method to contribute additional changes to {@link #changeManager}.
   */
  protected void createAdditionalChanges() throws Exception {
  }
}

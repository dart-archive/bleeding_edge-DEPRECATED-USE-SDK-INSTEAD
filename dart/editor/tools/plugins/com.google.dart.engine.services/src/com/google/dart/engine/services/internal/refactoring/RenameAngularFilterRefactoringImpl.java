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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularFilterElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.change.SourceChangeManager;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.Source;

import java.text.MessageFormat;
import java.util.List;

/**
 * {@link Refactoring} for renaming {@link AngularFilterElement}.
 */
public class RenameAngularFilterRefactoringImpl extends RenameRefactoringImpl {
  private final AngularFilterElement element;

  public RenameAngularFilterRefactoringImpl(SearchEngine searchEngine, AngularFilterElement element) {
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
    result.merge(NamingConventions.validateAngularFilterName(newName));
    result.merge(checkIfAlreadyDefines(newName));
    return result;
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    try {
      SourceChangeManager changeManager = new SourceChangeManager();
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
      // return CompositeChange
      CompositeChange compositeChange = new CompositeChange(getRefactoringName());
      compositeChange.add(changeManager.getChanges());
      return compositeChange;
    } finally {
      pm.done();
    }
  }

  @Override
  public String getRefactoringName() {
    return "Rename Angular Filter";
  }

  private RefactoringStatus checkIfAlreadyDefines(String newName) {
    LibraryElement library = element.getLibrary();
    AnalysisContext context = library.getContext();
    Source librarySource = library.getSource();
    AngularElement[] angularElements = context.getLibraryAngularElements(librarySource);
    for (AngularElement angularElement : angularElements) {
      if (angularElement instanceof AngularFilterElement) {
        if (angularElement.getName().equals(newName)) {
          String message = MessageFormat.format(
              "Library already defines filter with name ''{0}''.",
              newName);
          return RefactoringStatus.createErrorStatus(message);
        }
      }
    }
    return new RefactoringStatus();
  }
}

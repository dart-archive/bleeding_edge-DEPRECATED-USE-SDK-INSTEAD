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

import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.change.SourceChangeManager;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.refactoring.SubProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;

import java.util.List;

/**
 * {@link Refactoring} for renaming {@link ImportElement}.
 */
public class RenameImportRefactoringImpl extends RenameRefactoringImpl {

  private final ImportElement element;

  public RenameImportRefactoringImpl(SearchEngine searchEngine, ImportElement element) {
    super(searchEngine, element);
    this.element = element;
  }

  @Override
  public RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking final conditions", 1);
    try {
      RefactoringStatus result = new RefactoringStatus();
      result.merge(analyzePossibleConflicts(new SubProgressMonitor(pm, 1)));
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public RefactoringStatus checkNewName(String newName) {
    RefactoringStatus result = new RefactoringStatus();
    result.merge(super.checkNewName(newName));
    result.merge(NamingConventions.validateImportPrefixName(newName));
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
        PrefixElement prefix = element.getPrefix();
        Edit edit = null;
        if (newName.isEmpty()) {
          int uriEnd = element.getUriEnd();
          int prefixEnd = element.getPrefixOffset() + prefix.getDisplayName().length();
          SourceRange range = rangeStartEnd(uriEnd, prefixEnd);
          edit = new Edit(range, "");
        } else {
          if (prefix == null) {
            SourceRange range = rangeStartLength(element.getUriEnd(), 0);
            edit = new Edit(range, " as " + newName);
          } else {
            SourceRange range = rangeStartLength(
                element.getPrefixOffset(),
                prefix.getDisplayName().length());
            edit = new Edit(range, newName);
          }
        }
        if (edit != null) {
          elementChange.addEdit("Update import directive", edit);
        }
      }
      // update references
      List<SearchMatch> references = searchEngine.searchReferences(element, null, null);
      references = getUniqueMatches(references);
      for (SearchMatch reference : references) {
        Source refSource = reference.getElement().getSource();
        SourceChange refChange = changeManager.get(refSource);
        Edit edit;
        if (newName.isEmpty()) {
          edit = createReferenceEdit(reference, newName);
        } else {
          edit = createReferenceEdit(reference, newName + ".");
        }
        refChange.addEdit("Update reference", edit);
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
  public String getCurrentName() {
    PrefixElement prefix = element.getPrefix();
    if (prefix != null) {
      return prefix.getDisplayName();
    }
    return "";
  }

  @Override
  public String getRefactoringName() {
    return "Rename Import Prefix";
  }

  private RefactoringStatus analyzePossibleConflicts(ProgressMonitor pm) {
    // TODO(scheglov)
    return new RefactoringStatus();
//    RenameUnitMemberValidator validator = new RenameUnitMemberValidator(
//        searchEngine,
//        element,
//        element.getKind(),
//        newName);
//    return validator.validate(pm, true);
  }
}

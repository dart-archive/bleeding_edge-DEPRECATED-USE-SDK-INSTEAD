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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.TypeVariableElement;
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

/**
 * {@link Refactoring} for renaming {@link FieldElement} and {@link MethodElement}.
 */
public class RenameClassMemberRefactoringImpl extends RenameRefactoringImpl {
  private final Element element;
  private RenameClassMemberValidator validator;

  public RenameClassMemberRefactoringImpl(SearchEngine searchEngine, Element element) {
    super(searchEngine, element);
    this.element = element;
  }

  @Override
  public RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking final conditions", 1);
    try {
      RefactoringStatus result = new RefactoringStatus();
      validator = new RenameClassMemberValidator(
          searchEngine,
          element.getKind(),
          (ClassElement) element.getEnclosingElement(),
          element.getName(),
          newName);
      result.merge(validator.validate(new SubProgressMonitor(pm, 1), true));
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public RefactoringStatus checkNewName(String newName) {
    RefactoringStatus result = new RefactoringStatus();
    result.merge(super.checkNewName(newName));
    if (element instanceof FieldElement) {
      FieldElement fieldElement = (FieldElement) element;
      if (fieldElement.isStatic() && fieldElement.isConst()) {
        result.merge(NamingConventions.validateConstantName(newName));
      } else {
        result.merge(NamingConventions.validateFieldName(newName));
      }
    }
    if (element instanceof MethodElement) {
      result.merge(NamingConventions.validateMethodName(newName));
    }
    return result;
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    try {
      SourceChangeManager changeManager = new SourceChangeManager();
      // update declaration
      for (Element renameElement : validator.renameElements) {
        if (!renameElement.isSynthetic()) {
          Source elementSource = renameElement.getSource();
          SourceChange elementChange = changeManager.get(elementSource);
          elementChange.addEdit("Update declaration", createDeclarationRenameEdit(renameElement));
        }
      }
      // update references
      for (SearchMatch reference : validator.renameElementsReferences) {
        Source refSource = reference.getElement().getSource();
        SourceChange refChange = changeManager.get(refSource);
        refChange.addEdit("Update reference", createReferenceRenameEdit(reference));
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
    if (element instanceof TypeVariableElement) {
      return "Rename Type Variable";
    }
    if (element instanceof FieldElement) {
      return "Rename Field";
    }
    return "Rename Method";
  }
}

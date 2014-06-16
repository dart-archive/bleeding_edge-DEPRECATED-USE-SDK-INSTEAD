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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
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
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getElementKindName;
import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getElementQualifiedName;

import java.text.MessageFormat;
import java.util.List;

/**
 * {@link Refactoring} for renaming {@link LocalElement}.
 */
public class RenameLocalRefactoringImpl extends RenameRefactoringImpl {
  private final LocalElement element;

  public RenameLocalRefactoringImpl(SearchEngine searchEngine, LocalElement element) {
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
    if (element instanceof LocalVariableElement) {
      LocalVariableElement variableElement = (LocalVariableElement) element;
      if (variableElement.isConst()) {
        result.merge(NamingConventions.validateConstantName(newName));
      } else {
        result.merge(NamingConventions.validateVariableName(newName));
      }
    } else if (element instanceof ParameterElement) {
      result.merge(NamingConventions.validateParameterName(newName));
    } else if (element instanceof FunctionElement) {
      result.merge(NamingConventions.validateFunctionName(newName));
    }
    return result;
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    SourceChangeManager changeManager = new SourceChangeManager();
    // update declaration
    {
      Source source = element.getSource();
      SourceChange change = changeManager.get(source);
      addDeclarationEdit(change, element);
    }
    // update references
    List<SearchMatch> refMatches = searchEngine.searchReferences(element, null, null);
    List<SourceReference> references = getSourceReferences(refMatches);
    for (SourceReference reference : references) {
      SourceChange refChange = changeManager.get(reference.source);
      addReferenceEdit(refChange, reference);
    }
    // prepare change
    return new CompositeChange(getRefactoringName(), changeManager.getChanges());
  }

  @Override
  public String getRefactoringName() {
    if (element instanceof ParameterElement) {
      return "Rename Parameter";
    }
    if (element instanceof FunctionElement) {
      return "Rename Local Function";
    }
    return "Rename Local Variable";
  }

  @Override
  public boolean shouldReportUnsafeRefactoringSource(AnalysisContext context, Source source) {
    return element.getSource().equals(source);
  }

  private RefactoringStatus analyzePossibleConflicts(ProgressMonitor pm) {
    pm.beginTask("Analyze possible conflicts", 1);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // find all references to the "newName"
      List<SearchMatch> nameDeclarations = searchEngine.searchDeclarations(newName, null, null);
      for (SearchMatch nameDeclaration : nameDeclarations) {
        Element nameElement = nameDeclaration.getElement();
        nameElement = HierarchyUtils.getSyntheticAccessorVariable(nameElement);
        // duplicate declaration
        if (haveIntersectingRanges(element, nameElement)) {
          String message = MessageFormat.format(
              "Duplicate local {0} ''{1}''.",
              getElementKindName(nameElement),
              newName);
          result.addError(message, new RefactoringStatusContext(nameElement));
          return result;
        }
        // shadowing referenced element
        List<SearchMatch> nameReferences = searchEngine.searchReferences(nameElement, null, null);
        for (SearchMatch nameReference : nameReferences) {
          if (isReferenceInLocalRange(element, nameReference)) {
            String nameElementSourceName = nameElement.getSource().getShortName();
            String message = MessageFormat.format(
                "Usage of {0} ''{1}'' declared in ''{2}'' will be shadowed by renamed {3}.",
                getElementKindName(nameElement),
                getElementQualifiedName(nameElement),
                nameElementSourceName,
                getElementKindName(element));
            result.addError(message, RefactoringStatusContext.create(nameReference));
          }
        }
      }
      pm.worked(1);
      // done
      return result;
    } finally {
      pm.done();
    }
  }
}

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

import com.google.common.collect.Lists;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.TopLevelVariableElement;
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

import java.util.List;

/**
 * {@link Refactoring} for renaming {@link ClassElement}, {@link TopLevelVariableElement} and
 * top-level {@link FunctionElement}.
 */
public class RenameUnitMemberRefactoringImpl extends RenameRefactoringImpl {

  private final Element element;

  public RenameUnitMemberRefactoringImpl(SearchEngine searchEngine, Element element) {
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
    if (element instanceof TopLevelVariableElement) {
      result.merge(NamingConventions.validateVariableName(newName));
    }
    if (element instanceof FunctionElement) {
      result.merge(NamingConventions.validateFunctionName(newName));
    }
    if (element instanceof FunctionTypeAliasElement) {
      result.merge(NamingConventions.validateFunctionTypeAliasName(newName));
    }
    if (element instanceof ClassElement) {
      result.merge(NamingConventions.validateClassName(newName));
    }
    return result;
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    try {
      SourceChangeManager changeManager = new SourceChangeManager();
      // prepare elements (for synthetic property)
      List<Element> elements = Lists.newArrayList();
      if (element instanceof PropertyInducingElement && element.isSynthetic()) {
        PropertyInducingElement property = (PropertyInducingElement) element;
        PropertyAccessorElement getter = property.getGetter();
        PropertyAccessorElement setter = property.getSetter();
        if (getter != null) {
          elements.add(getter);
        }
        if (setter != null) {
          elements.add(setter);
        }
      } else {
        elements.add(element);
      }
      // update each element
      for (Element element : elements) {
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
    if (element instanceof TopLevelVariableElement) {
      return "Rename Top-Level Variable";
    }
    if (element instanceof FunctionElement) {
      return "Rename Top-Level Function";
    }
    if (element instanceof FunctionTypeAliasElement) {
      return "Rename Function Type Alias";
    }
    return "Rename Class";
  }

  private RefactoringStatus analyzePossibleConflicts(ProgressMonitor pm) {
    RenameUnitMemberValidator validator = new RenameUnitMemberValidator(
        searchEngine,
        element,
        element.getKind(),
        newName);
    return validator.validate(pm, true);
  }
}

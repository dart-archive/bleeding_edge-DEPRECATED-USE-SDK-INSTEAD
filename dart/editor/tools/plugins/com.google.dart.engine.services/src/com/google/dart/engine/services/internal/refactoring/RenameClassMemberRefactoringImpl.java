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
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchFilter;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.MergeCompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.change.SourceChangeManager;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.refactoring.SubProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import java.util.Iterator;
import java.util.List;

/**
 * {@link Refactoring} for renaming {@link FieldElement} and {@link MethodElement}.
 */
public class RenameClassMemberRefactoringImpl extends RenameRefactoringImpl {
  private final Element element;
  private RenameClassMemberValidator validator;
  private List<SearchMatch> nameReferences;

  private boolean hasIgnoredElements = false;

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
          element.getDisplayName(),
          newName);
      result.merge(validator.validate(new SubProgressMonitor(pm, 1), true));
      // add warning if some elements/reference are ignored
      hasIgnoredElements = validator.hasIgnoredElements;
      removeReferencesIfCannotUpdateSource(nameReferences);
      removeReferencesIfCannotUpdateSource(validator.renameElementsReferences);
      if (hasIgnoredElements) {
        result.addWarning("Elements and references in SDK and external packages will not be renamed.");
      }
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    if (element instanceof MethodElement && ((MethodElement) element).isOperator()) {
      return RefactoringStatus.createFatalErrorStatus("Cannot rename operator.");
    }
    preparePotentialMatchers();
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkNewName(String newName) {
    RefactoringStatus result = new RefactoringStatus();
    result.merge(super.checkNewName(newName));
    if (element instanceof FieldElement) {
      result.merge(NamingConventions.validateFieldName(newName));
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
      SourceChangeManager exactManager = new SourceChangeManager();
      // update declaration
      for (Element renameElement : validator.renameElements) {
        if (!renameElement.isSynthetic()) {
          Source elementSource = renameElement.getSource();
          SourceChange elementChange = exactManager.get(elementSource);
          addDeclarationEdit(elementChange, renameElement);
        }
      }
      // update references
      List<SourceReference> elementRefs = getSourceReferences(validator.renameElementsReferences);
      for (SourceReference reference : elementRefs) {
        SourceChange refChange = exactManager.get(reference.source);
        addReferenceEdit(refChange, reference);
      }
      // potential matches
      SourceChangeManager previewManager = new SourceChangeManager();
      List<SourceReference> nameSourceReferences = getSourceReferences(nameReferences);
      for (SourceReference reference : nameSourceReferences) {
        // check that element is accessible
        boolean accessible = false;
        for (Element refElement : reference.elements) {
          LibraryElement whereLibrary = refElement.getLibrary();
          accessible |= element.isAccessibleIn(whereLibrary);
        }
        if (!accessible) {
          continue;
        }
        // add edit
        SourceChange refChange = previewManager.get(reference.source);
        Edit edit = createReferenceEdit(reference, newName);
        addEdit(refChange, "Update reference @" + reference.range.getOffset(), edit);
      }
      // return CompositeChange
      SourceChange[] exactChanges = exactManager.getChanges();
      SourceChange[] previewChanges = previewManager.getChanges();
      if (previewChanges.length == 0) {
        CompositeChange compositeChange = new CompositeChange(getRefactoringName());
        compositeChange.add(exactChanges);
        return compositeChange;
      } else {
        CompositeChange exactChange = new CompositeChange("Exact changes");
        CompositeChange previewChange = new CompositeChange("Potential matches");
        exactChange.add(exactChanges);
        previewChange.add(previewChanges);
        return new MergeCompositeChange(getRefactoringName(), previewChange, exactChange);
      }
    } finally {
      pm.done();
    }
  }

  @Override
  public String getRefactoringName() {
    if (element instanceof TypeParameterElement) {
      return "Rename Type Parameter";
    }
    if (element instanceof FieldElement) {
      return "Rename Field";
    }
    return "Rename Method";
  }

  @Override
  public boolean requiresPreview() {
    return nameReferences != null && !nameReferences.isEmpty();
  }

  private void preparePotentialMatchers() {
    if (element instanceof FieldElement || element instanceof MethodElement
        || element instanceof PropertyAccessorElement) {
      nameReferences = searchEngine.searchQualifiedMemberReferences(
          element.getDisplayName(),
          null,
          new SearchFilter() {
            @Override
            public boolean passes(SearchMatch match) {
              return match.getKind() == MatchKind.NAME_REFERENCE_UNRESOLVED;
            }
          });
    } else {
      nameReferences = Lists.newArrayList();
    }
    nameReferences = HierarchyUtils.getAccessibleMatches(element, nameReferences);
  }

  private void removeReferencesIfCannotUpdateSource(List<SearchMatch> references) {
    SourceFactory sourceFactory = element.getContext().getSourceFactory();
    for (Iterator<SearchMatch> iter = references.iterator(); iter.hasNext();) {
      SearchMatch reference = iter.next();
      Source refSource = reference.getElement().getSource();
      if (!sourceFactory.isLocalSource(refSource)) {
        iter.remove();
        hasIgnoredElements = true;
      }
    }
  }
}

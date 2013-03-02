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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.refactoring.SubProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusContext;

import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getChildren;
import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getElementKindName;
import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getElementQualifiedName;
import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getSuperClassElements;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

/**
 * {@link Refactoring} for renaming {@link FieldElement} and {@link MethodElement}.
 */
public class RenameClassMemberRefactoringImpl extends RenameRefactoringImpl {
  private final Element element;
  private ClassElement elementClass;
  private Set<ClassElement> superClasses;
  private Set<ClassElement> subClasses;
  private Set<ClassElement> hierarchyClasses;
  private Set<Element> renameElements = Sets.newHashSet();

  private List<SearchMatch> renameElementsReferences = Lists.newArrayList();

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
      prepareHierarchyClasses();
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
    SourceChange change = new SourceChange(getRefactoringName(), elementSource);
    // update declaration
    for (Element renameElement : renameElements) {
      if (!renameElement.isSynthetic()) {
        change.addEdit("Update declaration", createDeclarationRenameEdit(renameElement));
      }
    }
    // update references
    for (SearchMatch reference : renameElementsReferences) {
      change.addEdit("Update reference", createReferenceRenameEdit(reference));
    }
    return change;
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

  private RefactoringStatus analyzePossibleConflicts(ProgressMonitor pm) {
    pm.beginTask("Analyze possible conflicts", 4);
    try {
      final RefactoringStatus result = new RefactoringStatus();
      // check if there are members with "newName" in the same ClassElement
      {
        ClassElement parentClass = (ClassElement) element.getEnclosingElement();
        for (Element newNameMember : getChildren(parentClass, newName)) {
          String message = MessageFormat.format(
              "Class ''{0}'' already declares {1} with name ''{2}''.",
              parentClass.getName(),
              getElementKindName(newNameMember),
              newName);
          result.addError(message, RefactoringStatusContext.create(newNameMember));
        }
      }
      pm.worked(1);
      // check shadowing in hierarchy
      List<SearchMatch> nameDeclarations = searchEngine.searchDeclarations(newName, null, null);
      {
        for (SearchMatch nameDeclaration : nameDeclarations) {
          Element member = nameDeclaration.getElement();
          Element memberDeclClass = member.getEnclosingElement();
          // renamed Element shadows member of super-class
          if (superClasses.contains(memberDeclClass)) {
            String message = MessageFormat.format(
                "Renamed {0} will shadow {1} ''{2}''.",
                getElementKindName(element),
                getElementKindName(member),
                getElementQualifiedName(member));
            result.addError(message, RefactoringStatusContext.create(member));
          }
          // renamed Element is shadowed by member of sub-class
          if (subClasses.contains(memberDeclClass)) {
            String message = MessageFormat.format(
                "Renamed {0} will be shadowed by {1} ''{2}''.",
                getElementKindName(element),
                getElementKindName(member),
                getElementQualifiedName(member));
            result.addError(message, RefactoringStatusContext.create(member));
          }
        }
        pm.worked(1);
      }
      // check if shadowed by local
      {
        for (SearchMatch nameDeclaration : nameDeclarations) {
          Element nameElement = nameDeclaration.getElement();
          if (nameElement instanceof LocalElement) {
            LocalElement localElement = (LocalElement) nameElement;
            ClassElement enclosingClass = nameElement.getAncestor(ClassElement.class);
            if (Objects.equal(enclosingClass, elementClass) || subClasses.contains(enclosingClass)) {
              for (SearchMatch reference : renameElementsReferences) {
                if (isReferenceInLocalRange(localElement, reference)) {
                  String message = MessageFormat.format(
                      "Usage of renamed {0} will be shadowed by {1} ''{2}''.",
                      getElementKindName(element),
                      getElementKindName(localElement),
                      localElement.getName());
                  result.addError(message, RefactoringStatusContext.create(reference));
                }
              }
            }
          }
        }
        pm.worked(1);
      }
      // TODO(scheglov) may be top-level shadows reference
      {
        pm.worked(1);
      }
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  /**
   * Fills {@link #hierarchyClasses} with super- and sub- {@link ClassElement}s; and
   * {@link #renameElements} with all {@link Element}s which should be renamed, i.e. overridden in
   * super- and overrides in sub-classes.
   */
  private void prepareHierarchyClasses() {
    Element seed = element;
    // getter or setter, rename FieldElement
    if (seed instanceof PropertyAccessorElement) {
      seed = ((PropertyAccessorElement) seed).getVariable();
    }
    // prepare super/sub-classes
    elementClass = (ClassElement) seed.getEnclosingElement();
    superClasses = getSuperClassElements(elementClass);
    subClasses = getSubClassElements(elementClass);
    // full hierarchy
    hierarchyClasses = Sets.newHashSet();
    hierarchyClasses.add(elementClass);
    hierarchyClasses.addAll(superClasses);
    hierarchyClasses.addAll(subClasses);
    // prepare elements to rename
    renameElements.clear();
    String oldName = seed.getName();
    for (ClassElement superClass : hierarchyClasses) {
      renameElements.addAll(getChildren(superClass, oldName));
    }
    // prepare references
    for (Element renameElement : renameElements) {
      List<SearchMatch> references = searchEngine.searchReferences(renameElement, null, null);
      renameElementsReferences.addAll(references);
    }
  }
}

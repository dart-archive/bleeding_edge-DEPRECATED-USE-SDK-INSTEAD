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
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.services.util.HierarchyUtils;

import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getChildren;
import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getElementKindName;
import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getElementQualifiedName;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

/**
 * Helper to check if renaming or creating {@link Element} with given name will cause any problems.
 */
class RenameClassMemberValidator {
  private final SearchEngine searchEngine;
  private final ElementKind elementKind;
  private final ClassElement elementClass;
  private final String oldName;
  private final String newName;
  private List<ClassElement> superClasses;
  private Set<ClassElement> subClasses;
  private Set<ClassElement> hierarchyClasses;
  Set<Element> renameElements = Sets.newHashSet();
  List<SearchMatch> renameElementsReferences = Lists.newArrayList();

  public RenameClassMemberValidator(SearchEngine searchEngine, ElementKind elementKind,
      ClassElement elementClass, String oldName, String newName) {
    this.searchEngine = searchEngine;
    this.elementKind = elementKind;
    this.elementClass = elementClass;
    this.oldName = oldName;
    this.newName = newName;
  }

  RefactoringStatus validate(ProgressMonitor pm, boolean isRename) {
    pm.beginTask("Analyze possible conflicts", 4);
    try {
      final RefactoringStatus result = new RefactoringStatus();
      // prepare
      prepareHierarchyClasses();
      // check if there are members with "newName" in the same ClassElement
      for (Element newNameMember : getChildren(elementClass, newName)) {
        String message = MessageFormat.format(
            "Class ''{0}'' already declares {1} with name ''{2}''.",
            elementClass.getName(),
            getElementKindName(newNameMember),
            newName);
        result.addError(message, RefactoringStatusContext.create(newNameMember));
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
                isRename ? "Renamed {0} will shadow {1} ''{2}''."
                    : "Created {0} will shadow {1} ''{2}''.",
                getElementKindName(elementKind),
                getElementKindName(member),
                getElementQualifiedName(member));
            result.addError(message, RefactoringStatusContext.create(member));
          }
          // renamed Element is shadowed by member of sub-class
          if (isRename && subClasses.contains(memberDeclClass)) {
            String message = MessageFormat.format(
                "Renamed {0} will be shadowed by {1} ''{2}''.",
                getElementKindName(elementKind),
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
                if (RenameRefactoringImpl.isReferenceInLocalRange(localElement, reference)) {
                  String message = MessageFormat.format(
                      "Usage of renamed {0} will be shadowed by {1} ''{2}''.",
                      getElementKindName(elementKind),
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
    // prepare super/sub-classes
    superClasses = HierarchyUtils.getSuperClasses(elementClass);
    subClasses = HierarchyUtils.getSubClasses(searchEngine, elementClass);
    // full hierarchy
    hierarchyClasses = Sets.newHashSet();
    hierarchyClasses.add(elementClass);
    hierarchyClasses.addAll(superClasses);
    hierarchyClasses.addAll(subClasses);
    // prepare elements to rename
    renameElements.clear();
    for (ClassElement superClass : hierarchyClasses) {
      for (Element child : getChildren(superClass, oldName)) {
        if (!child.isSynthetic()) {
          renameElements.add(child);
        }
      }
    }
    // prepare references
    for (Element renameElement : renameElements) {
      List<SearchMatch> references = searchEngine.searchReferences(renameElement, null, null);
      renameElementsReferences.addAll(references);
    }
  }
}

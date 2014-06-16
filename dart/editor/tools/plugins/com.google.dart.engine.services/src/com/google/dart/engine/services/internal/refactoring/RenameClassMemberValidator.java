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
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getChildren;
import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getElementKindName;
import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getElementQualifiedName;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Helper to check if renaming or creating {@link Element} with given name will cause any problems.
 */
class RenameClassMemberValidator {
  private final SearchEngine searchEngine;
  private final AnalysisContext activeContext;
  private final ElementKind elementKind;
  private final ClassElement elementClass;
  private final String oldName;
  private final String newName;
  private Set<ClassElement> superClasses;
  private Set<ClassElement> subClasses;
  boolean hasIgnoredElements = false;
  Set<Element> renameElements = Sets.newHashSet();
  List<SearchMatch> renameElementsReferences = Lists.newArrayList();

  public RenameClassMemberValidator(SearchEngine searchEngine, ElementKind elementKind,
      ClassElement elementClass, String oldName, String newName) {
    this.searchEngine = searchEngine;
    this.activeContext = elementClass.getContext();
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
      prepareHierarchyClasses(result);
      // check if there are members with "newName" in the same ClassElement
      for (Element newNameMember : getChildren(elementClass, newName)) {
        String message = MessageFormat.format(
            "Class ''{0}'' already declares {1} with name ''{2}''.",
            elementClass.getDisplayName(),
            getElementKindName(newNameMember),
            newName);
        result.addError(message, new RefactoringStatusContext(newNameMember));
      }
      pm.worked(1);
      // check shadowing in hierarchy
      List<SearchMatch> nameDeclarations = searchEngine.searchDeclarations(newName, null, null);
      {
        for (SearchMatch nameDeclaration : nameDeclarations) {
          Element member = nameDeclaration.getElement();
          member = HierarchyUtils.getSyntheticAccessorVariable(member);
          Element memberDeclClass = member.getEnclosingElement();
          // renamed Element shadows member of super-class
          if (superClasses.contains(memberDeclClass)) {
            String message = MessageFormat.format(
                isRename ? "Renamed {0} will shadow {1} ''{2}''."
                    : "Created {0} will shadow {1} ''{2}''.",
                getElementKindName(elementKind),
                getElementKindName(member),
                getElementQualifiedName(member));
            result.addError(message, new RefactoringStatusContext(member));
          }
          // renamed Element is shadowed by member of sub-class
          if (isRename && subClasses.contains(memberDeclClass)) {
            String message = MessageFormat.format(
                "Renamed {0} will be shadowed by {1} ''{2}''.",
                getElementKindName(elementKind),
                getElementKindName(member),
                getElementQualifiedName(member));
            result.addError(message, new RefactoringStatusContext(member));
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
                      localElement.getDisplayName());
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
  private void prepareHierarchyClasses(RefactoringStatus status) {
    // prepare elements to rename
    superClasses = HierarchyUtils.getSuperClasses(elementClass);
    subClasses = Sets.newHashSet();
    renameElements.clear();
    // process super-classes with "oldName" and their sub-classes
    Set<ClassElement> processed = Sets.newHashSet();
    LinkedList<ClassElement> toProcess = Lists.newLinkedList();
    toProcess.addAll(superClasses);
    toProcess.add(elementClass);
    while (!toProcess.isEmpty()) {
      ClassElement classElement = toProcess.removeFirst();
      // maybe already processed
      if (!processed.add(classElement)) {
        continue;
      }
      // add "oldName" children
      List<Element> children = CorrectionUtils.getChildren(classElement, oldName);
      for (Element child : children) {
        // ignore if Source cannot be updated
        Source source = child.getSource();
        SourceFactory activeSourceFactory = activeContext.getSourceFactory();
        if (!activeSourceFactory.isLocalSource(source)) {
          hasIgnoredElements = true;
          continue;
        }
        // add element to rename
        renameElements.add(child);
        // process sub-classes if this class has an "oldName" child
        toProcess.addAll(HierarchyUtils.getSubClasses(searchEngine, classElement));
      }
      // add sub-class
      if (!superClasses.contains(classElement)) {
        subClasses.add(classElement);
      }
    }
    // prepare references
    renameElementsReferences.clear();
    for (Element renameElement : renameElements) {
      List<SearchMatch> references = searchEngine.searchReferences(renameElement, null, null);
      renameElementsReferences.addAll(references);
    }
  }
}

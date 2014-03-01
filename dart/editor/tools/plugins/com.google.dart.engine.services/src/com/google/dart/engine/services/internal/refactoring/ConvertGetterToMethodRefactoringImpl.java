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

import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.change.SourceChangeManager;
import com.google.dart.engine.services.refactoring.ConvertGetterToMethodRefactoring;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getChildren;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeElementName;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;

import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link ConvertGetterToMethodRefactoring}.
 */
public class ConvertGetterToMethodRefactoringImpl extends RefactoringImpl implements
    ConvertGetterToMethodRefactoring {
  private final SearchEngine searchEngine;
  private final PropertyAccessorElement element;
  private SourceChangeManager changeManager;

  public ConvertGetterToMethodRefactoringImpl(SearchEngine searchEngine,
      PropertyAccessorElement element) {
    this.searchEngine = searchEngine;
    this.element = element;
  }

  @Override
  public RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception {
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking initial conditions", 1);
    try {
      RefactoringStatus result = new RefactoringStatus();
      if (element.isSynthetic()) {
        return RefactoringStatus.createFatalErrorStatus("Only explicit getter can be converted to method.");
      }
      pm.worked(1);
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm.beginTask("Processing...", 2);
    try {
      changeManager = new SourceChangeManager();
      // FunctionDeclaration
      if (element.getEnclosingElement() instanceof CompilationUnitElement) {
        // update declaration
        updateElementDeclaration(element);
        pm.worked(1);
        // update references
        updateElementReferences(element);
        pm.worked(1);
      }
      // MethodDeclaration
      if (element.getEnclosingElement() instanceof ClassElement) {
        Set<PropertyAccessorElement> updateElements = getHierarchyMethods();
        pm.worked(1);
        // update elements
        for (PropertyAccessorElement element : updateElements) {
          updateElementDeclaration(element);
          updateElementReferences(element);
        }
        pm.worked(1);
      }
      // done
      return new CompositeChange(getRefactoringName(), changeManager.getChanges());
    } finally {
      pm.done();
      changeManager = null;
    }
  }

  @Override
  public String getRefactoringName() {
    return "Convert Getter to Method";
  }

  /**
   * When {@link #element} is {@link MethodElement}, finds all overrides in super- and sub- classes.
   */
  private Set<PropertyAccessorElement> getHierarchyMethods() {
    ClassElement enclosingClass = (ClassElement) element.getEnclosingElement();
    // prepare super/sub-classes
    Set<ClassElement> superClasses = HierarchyUtils.getSuperClasses(enclosingClass);
    Set<ClassElement> subClasses = HierarchyUtils.getSubClasses(searchEngine, enclosingClass);
    // full hierarchy
    Set<ClassElement> hierarchyClasses = Sets.newHashSet();
    hierarchyClasses.add(enclosingClass);
    hierarchyClasses.addAll(superClasses);
    hierarchyClasses.addAll(subClasses);
    // prepare elements to update
    Set<PropertyAccessorElement> updateElements = Sets.newHashSet();
    for (ClassElement superClass : hierarchyClasses) {
      for (Element child : getChildren(superClass, element.getDisplayName())) {
        if (child instanceof PropertyAccessorElement && !child.isSynthetic()) {
          PropertyAccessorElement accessor = (PropertyAccessorElement) child;
          if (accessor.isGetter()) {
            updateElements.add(accessor);
          }
        }
      }
    }
    return updateElements;
  }

  private void updateElementDeclaration(PropertyAccessorElement element) throws AnalysisException {
    String description = "Convert getter declaration into method";
    SourceChange change = changeManager.get(element.getSource());
    // prepare "get" keyword
    Token getKeyword = null;
    {
      AstNode node = element.getNode();
      if (node instanceof MethodDeclaration) {
        getKeyword = ((MethodDeclaration) node).getPropertyKeyword();
      } else if (node instanceof FunctionDeclaration) {
        getKeyword = ((FunctionDeclaration) node).getPropertyKeyword();
      }
    }
    // remove "get "
    if (getKeyword != null) {
      Edit edit = new Edit(rangeStartEnd(getKeyword, element.getNameOffset()), "");
      change.addEdit(description, edit);
    }
    // add parameters "()"
    {
      Edit edit = new Edit(rangeElementName(element).getEnd(), 0, "()");
      change.addEdit(description, edit);
    }
  }

  private void updateElementReferences(Element element) throws Exception {
    List<SearchMatch> references = searchEngine.searchReferences(element, null, null);
    for (SearchMatch reference : references) {
      Element refElement = reference.getElement();
      SourceRange refRange = reference.getSourceRange();
      SourceChange refChange = changeManager.get(refElement.getSource());
      // insert "()"
      refChange.addEdit(
          "Replace field access with invocation",
          new Edit(refRange.getEnd(), 0, "()"));
    }
  }
}

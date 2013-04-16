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
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.change.SourceChangeManager;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.refactoring.ConvertMethodToGetterRefactoring;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.services.internal.correction.CorrectionUtils.getChildren;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;

import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link ConvertMethodToGetterRefactoring}.
 */
public class ConvertMethodToGetterRefactoringImpl extends RefactoringImpl implements
    ConvertMethodToGetterRefactoring {
  private final SearchEngine searchEngine;
  private final ExecutableElement element;
  private SourceChangeManager changeManager;

  public ConvertMethodToGetterRefactoringImpl(SearchEngine searchEngine, ExecutableElement element) {
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
      // check Element kind
      if (element instanceof MethodElement) {
      } else if (element instanceof FunctionElement) {
        if (element.getEnclosingElement() instanceof CompilationUnitElement) {
        } else {
          return RefactoringStatus.createFatalErrorStatus("Function should be top-level to converted it to getter.");
        }
      } else {
        return RefactoringStatus.createFatalErrorStatus("Only class method or top-level function can be converted to getter.");
      }
      // no parameters
      if (element.getParameters().length != 0) {
        return RefactoringStatus.createFatalErrorStatus("Only method without parameters can be converted to getter.");
      }
      // done
      pm.worked(1);
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm.beginTask("Processing...", 3);
    try {
      changeManager = new SourceChangeManager();
      // FunctionElement
      if (element instanceof FunctionElement) {
        // update declaration
        updateElementDeclaration(element);
        pm.worked(1);
        // update references
        updateElementReferences(element);
        pm.worked(1);
      }
      // MethodElement
      if (element instanceof MethodElement) {
        Set<Element> updateElements = getHierarchyMethods();
        pm.worked(1);
        // update elements
        for (Element element : updateElements) {
          updateElementDeclaration(element);
          updateElementReferences(element);
        }
        pm.worked(1);
      }
      pm.worked(1);
      // done
      return new CompositeChange(getRefactoringName(), changeManager.getChanges());
    } finally {
      pm.done();
      changeManager = null;
    }
  }

  @Override
  public String getRefactoringName() {
    return "Convert Method to Getter";
  }

  /**
   * When {@link #element} is {@link MethodElement}, finds all overrides in super- and sub- classes.
   */
  private Set<Element> getHierarchyMethods() {
    ClassElement enclosingClass = (ClassElement) element.getEnclosingElement();
    // prepare super/sub-classes
    List<ClassElement> superClasses = HierarchyUtils.getSuperClasses(enclosingClass);
    Set<ClassElement> subClasses = HierarchyUtils.getSubClasses(searchEngine, enclosingClass);
    // full hierarchy
    Set<ClassElement> hierarchyClasses = Sets.newHashSet();
    hierarchyClasses.add(enclosingClass);
    hierarchyClasses.addAll(superClasses);
    hierarchyClasses.addAll(subClasses);
    // prepare elements to update
    Set<Element> updateElements = Sets.newHashSet();
    for (ClassElement superClass : hierarchyClasses) {
      for (Element child : getChildren(superClass, element.getName())) {
        if (!child.isSynthetic()) {
          updateElements.add(child);
        }
      }
    }
    return updateElements;
  }

  private void updateElementDeclaration(Element element) throws AnalysisException {
    String description = "Convert method declaration into getter";
    SourceChange change = changeManager.get(element.getSource());
    // prepare MethodDeclaration
    FormalParameterList parameters;
    {
      CompilationUnit unit = CorrectionUtils.getResolvedUnit(element);
      ASTNode node = new NodeLocator(element.getNameOffset()).searchWithin(unit);
      if (element instanceof MethodElement) {
        MethodDeclaration methodDeclaration = node.getAncestor(MethodDeclaration.class);
        parameters = methodDeclaration.getParameters();
      } else {
        FunctionDeclaration methodDeclaration = node.getAncestor(FunctionDeclaration.class);
        parameters = methodDeclaration.getFunctionExpression().getParameters();
      }
    }
    // insert "get "
    {
      Edit edit = new Edit(element.getNameOffset(), 0, "get ");
      change.addEdit(description, edit);
    }
    // remove parameters
    {
      Edit edit = new Edit(rangeNode(parameters), "");
      change.addEdit(description, edit);
    }
  }

  private void updateElementReferences(Element element) throws Exception {
    List<SearchMatch> references = searchEngine.searchReferences(element, null, null);
    for (SearchMatch reference : references) {
      Element refElement = reference.getElement();
      SourceRange refRange = reference.getSourceRange();
      SourceChange refChange = changeManager.get(refElement.getSource());
      // prepare invocation
      MethodInvocation invocation;
      {
        CompilationUnit refUnit = CorrectionUtils.getResolvedUnit(refElement);
        int refOffset = refRange.getOffset();
        ASTNode coveringNode = new NodeLocator(refOffset).searchWithin(refUnit);
        invocation = coveringNode.getAncestor(MethodInvocation.class);
      }
      // we need invocation
      if (invocation != null) {
        SourceRange range = rangeEndEnd(refRange, invocation);
        refChange.addEdit("Replace invocation with field access", new Edit(range, ""));
      }
    }
  }
}

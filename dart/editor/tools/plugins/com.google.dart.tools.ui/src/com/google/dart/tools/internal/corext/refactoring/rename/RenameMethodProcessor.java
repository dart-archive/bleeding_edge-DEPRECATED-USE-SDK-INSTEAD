/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.MatchQuality;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.viewsupport.BasicElementLabels;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.List;

/**
 * {@link DartRenameProcessor} for {@link Method}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameMethodProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameMethodProcessor"; //$NON-NLS-1$

  private static void addTextEdit(TextChange change, String groupName, TextEdit textEdit) {
    TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
  }

  private final Method method;
  private final String oldName;
  private final TextChangeManager changeManager = new TextChangeManager(true);

  private List<SearchMatch> references;

  /**
   * @param method the {@link Method} to rename, not <code>null</code>.
   */
  public RenameMethodProcessor(Method method) {
    this.method = method;
    oldName = method.getElementName();
    setNewElementName(oldName);
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    return Checks.checkIfCuBroken(method);
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkMethodName(newName);

    if (Checks.isAlreadyNamed(method, newName)) {
      result.addError(
          RefactoringCoreMessages.RenameRefactoring_another_name,
          DartStatusContext.create(method));
      return result;
    }

    // type can not have two members with same name
    {
      Type enclosingType = method.getDeclaringType();
      TypeMember[] existingMembers = enclosingType.getExistingMembers(newName);
      if (existingMembers.length != 0) {
        IPath resourcePath = enclosingType.getResource().getFullPath();
        result.addError(Messages.format(
            RefactoringCoreMessages.RenameRefactoring_enclosing_type_member_already_defined,
            new Object[] {
                enclosingType.getElementName(),
                BasicElementLabels.getPathLabel(resourcePath, false),
                newName}), DartStatusContext.create(existingMembers[0]));
        return result;
      }
    }

    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameRefactoring_checking, 1);
    try {
      return new CompositeChange(getProcessorName(), changeManager.getAllChanges());
    } finally {
      monitor.done();
    }
  }

  @Override
  public final String getCurrentElementName() {
    return method.getElementName();
  }

  @Override
  public Object[] getElements() {
    return new Object[] {method};
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return method.getDeclaringType().getMethod(getNewElementName(), null);
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameMethodRefactoring_name;
  }

  @Override
  public int getSaveMode() {
    return RefactoringSaveHelper.SAVE_ALL;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(method);
  }

  @Override
  protected RefactoringStatus doCheckFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException {
    try {
      pm.beginTask("", 19); //$NON-NLS-1$
      pm.setTaskName(RefactoringCoreMessages.RenameRefactoring_checking);
      RefactoringStatus result = new RefactoringStatus();
      // check new name
      result.merge(checkNewElementName(getNewElementName()));
      pm.worked(1);
      // prepare references
      pm.setTaskName(RefactoringCoreMessages.RenameRefactoring_searching);
      prepareReferences(new SubProgressMonitor(pm, 3));
      pm.setTaskName(RefactoringCoreMessages.RenameRefactoring_checking);
      // analyze affected units (such as warn about existing compilation errors)
      result.merge(analyzeAffectedCompilationUnits());
      // check for possible conflicts
      result.merge(analyzePossibleConflicts(new SubProgressMonitor(pm, 10)));
      if (result.hasFatalError()) {
        return result;
      }
      // OK, create changes
      createChanges(new SubProgressMonitor(pm, 5));
      return result;
    } finally {
      pm.done();
    }
  }

  private void addDeclarationUpdate() throws CoreException {
    SourceRange nameRange = method.getNameRange();
    CompilationUnit cu = method.getCompilationUnit();
    String editName = RefactoringCoreMessages.RenameRefactoring_update_declaration;
    addTextEdit(changeManager.get(cu), editName, createTextChange(nameRange));
  }

  private void addReferenceUpdates(IProgressMonitor pm) throws DartModelException {
    pm.beginTask("", references.size()); //$NON-NLS-1$
    String editName = RefactoringCoreMessages.RenameRefactoring_update_reference;
    for (SearchMatch match : references) {
      CompilationUnit cu = match.getElement().getAncestor(CompilationUnit.class);
      SourceRange matchRange = match.getSourceRange();
      addTextEdit(changeManager.get(cu), editName, createTextChange(matchRange));
      pm.worked(1);
    }
  }

  private RefactoringStatus analyzeAffectedCompilationUnits() throws CoreException {
    RefactoringStatus result = new RefactoringStatus();
    result.merge(Checks.checkCompileErrorsInAffectedFiles(references));
    return result;
  }

  private RefactoringStatus analyzePossibleConflicts(IProgressMonitor pm) throws CoreException {
    pm.beginTask("Analyze possible conflicts", 3);
    try {
      RefactoringStatus result = new RefactoringStatus();
      String newName = getNewElementName();
      // analyze top-level elements
      pm.subTask("Analyze top-level elements");
      RenameAnalyzeUtil.checkShadow_topLevel(
          result,
          method,
          references,
          newName,
          RefactoringCoreMessages.RenameRefactoring_shadow_topLevel);
      pm.worked(1);
      if (result.hasFatalError()) {
        return result;
      }
      // analyze supertypes
      pm.subTask("Analyze supertypes");
      RenameAnalyzeUtil.checkShadow_superType_member(
          result,
          method,
          newName,
          RefactoringCoreMessages.RenameRefactoring_shadow_superType_member);
      pm.worked(1);
      if (result.hasFatalError()) {
        return result;
      }
      // analyze subtypes
      pm.subTask("Analyze subtypes");
      RenameAnalyzeUtil.checkShadow_subType(
          result,
          method,
          newName,
          RefactoringCoreMessages.RenameRefactoring_shadow_subType_member,
          RefactoringCoreMessages.RenameRefactoring_shadow_subType_parameter,
          RefactoringCoreMessages.RenameRefactoring_shadow_subType_variable);
      pm.worked(1);

      // OK
      return result;
    } finally {
      pm.done();
    }
  }

  private void createChanges(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.RenameRefactoring_checking, 10);
    changeManager.clear();
    // update declaration
    addDeclarationUpdate();
    pm.worked(1);
    // update references
    addReferenceUpdates(new SubProgressMonitor(pm, 9));
    pm.done();
  }

  private TextEdit createTextChange(SourceRange sourceRange) {
    return new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), getNewElementName());
  }

  private void prepareReferences(final IProgressMonitor pm) throws CoreException {
    references = ExecutionUtils.runObjectCore(new RunnableObjectEx<List<SearchMatch>>() {
      @Override
      public List<SearchMatch> runObject() throws Exception {
        SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
        return searchEngine.searchReferences(method, null, null, pm);
      }
    });
    // FIXME(scheglov) SearchEngine does not return declaration
    references.add(new SearchMatch(MatchQuality.EXACT, method, method.getNameRange()));
  }
}

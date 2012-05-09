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
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
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
 * {@link DartRenameProcessor} for local {@link DartFunction}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameLocalFunctionProcessor extends DartRenameProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameLocalFunctionProcessor"; //$NON-NLS-1$

  private final DartFunction function;
  private final String oldName;

  private final TextChangeManager changeManager = new TextChangeManager(true);
  private List<SearchMatch> references;

  /**
   * @param function the {@link DartFunction} to rename, not <code>null</code>.
   */
  public RenameLocalFunctionProcessor(DartFunction function) {
    Assert.isLegal(function.isLocal());
    this.function = function;
    this.oldName = function.getElementName();
    newName = oldName;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    return Checks.checkIfCuBroken(function);
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkFunctionName(newName);
    // may be already such name 
    if (Checks.isAlreadyNamed(function, newName)) {
      result.addFatalError(
          RefactoringCoreMessages.RenameProcessor_another_name,
          DartStatusContext.create(function));
      return result;
    }
    // done
    return result;
  }

  @Override
  public Change createChange(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask(RefactoringCoreMessages.RenameProcessor_checking, 1);
    try {
      return new CompositeChange(getProcessorName(), changeManager.getAllChanges());
    } finally {
      monitor.done();
    }
  }

  @Override
  public final String getCurrentElementName() {
    return oldName;
  }

  @Override
  public Object[] getElements() {
    return new Object[] {function};
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return null;
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameFunctionProcessor_name;
  }

  @Override
  public int getSaveMode() {
    return RefactoringSaveHelper.SAVE_NOTHING;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(function);
  }

  @Override
  public void setNewElementName(String newName) {
    Assert.isNotNull(newName);
    this.newName = newName;
  }

  protected final TextEdit createTextChange(SourceRange sourceRange) {
    return new ReplaceEdit(sourceRange.getOffset(), sourceRange.getLength(), newName);
  }

  @Override
  protected RefactoringStatus doCheckFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException, OperationCanceledException {
    try {
      pm.beginTask("", 19); //$NON-NLS-1$
      pm.setTaskName(RefactoringCoreMessages.RenameProcessor_checking);
      RefactoringStatus result = new RefactoringStatus();
      // check new name
      result.merge(checkNewElementName(newName));
      pm.worked(1);
      if (result.hasFatalError()) {
        return result;
      }
      // prepare references
      pm.setTaskName(RefactoringCoreMessages.RenameProcessor_searching);
      references = RenameAnalyzeUtil.getReferences(function, new SubProgressMonitor(pm, 3));
      pm.setTaskName(RefactoringCoreMessages.RenameProcessor_checking);
      // check for possible conflicts
      result.merge(analyzePossibleConflicts(new SubProgressMonitor(pm, 10)));
      // OK, create changes
      createChanges(new SubProgressMonitor(pm, 5));
      return result;
    } finally {
      pm.done();
    }
  }

  private void addDeclarationUpdate() throws CoreException {
    SourceRange nameRange = function.getNameRange();
    CompilationUnit cu = function.getCompilationUnit();
    String editName = RefactoringCoreMessages.RenameProcessor_update_declaration;
    addTextEdit(cu, editName, createTextChange(nameRange));
  }

  private void addReferenceUpdate(SearchMatch match) {
    String editName = RefactoringCoreMessages.RenameProcessor_update_reference;
    CompilationUnit cu = match.getElement().getAncestor(CompilationUnit.class);
    SourceRange matchRange = match.getSourceRange();
    addTextEdit(cu, editName, createTextChange(matchRange));
  }

  private void addReferenceUpdates(IProgressMonitor pm) throws DartModelException {
    pm.beginTask("", references.size()); //$NON-NLS-1$
    for (SearchMatch match : references) {
      addReferenceUpdate(match);
      pm.worked(1);
    }
  }

  private void addTextEdit(CompilationUnit unit, String groupName, TextEdit textEdit) {
    if (unit.getResource() != null) {
      TextChange change = changeManager.get(unit);
      TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
    }
  }

  private RefactoringStatus analyzePossibleConflicts(IProgressMonitor pm) throws CoreException {
    return RenameLocalVariableProcessor.analyzePossibleConflicts(
        new FunctionLocalElement(function),
        newName,
        pm);
  }

  private void createChanges(IProgressMonitor pm) throws CoreException {
    pm.beginTask(RefactoringCoreMessages.RenameProcessor_checking, 10);
    changeManager.clear();
    // update declaration
    addDeclarationUpdate();
    pm.worked(1);
    // update references
    addReferenceUpdates(new SubProgressMonitor(pm, 9));
    pm.done();
  }
}

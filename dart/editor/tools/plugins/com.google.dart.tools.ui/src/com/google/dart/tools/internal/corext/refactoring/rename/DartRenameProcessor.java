package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.tools.internal.corext.refactoring.tagging.INameUpdating;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public abstract class DartRenameProcessor extends RenameProcessor implements INameUpdating {

  private String fNewElementName;

  @Override
  public final RefactoringStatus checkFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException, OperationCanceledException {
    return doCheckFinalConditions(pm, context);
  }

  @Override
  public String getNewElementName() {
    return fNewElementName;
  }

  /**
   * @return a save mode from {@link RefactoringSaveHelper}
   * @see RefactoringSaveHelper
   */
  public abstract int getSaveMode();

  @Override
  public final RefactoringParticipant[] loadParticipants(
      RefactoringStatus status,
      SharableParticipants shared) throws CoreException {
    // TODO(scheglov) no rename participants
    return new RefactoringParticipant[0];
  }

  @Override
  public void setNewElementName(String newName) {
    Assert.isNotNull(newName);
    fNewElementName = newName;
  }

  protected abstract RefactoringStatus doCheckFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException, OperationCanceledException;

}

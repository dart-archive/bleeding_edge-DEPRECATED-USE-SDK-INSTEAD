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

  protected String newName;

  @Override
  public final RefactoringStatus checkFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException, OperationCanceledException {
    return doCheckFinalConditions(pm, context);
  }

  @Override
  public String getNewElementName() {
    return newName;
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
    this.newName = newName;
  }

  protected abstract RefactoringStatus doCheckFinalConditions(
      IProgressMonitor pm,
      CheckConditionsContext context) throws CoreException, OperationCanceledException;

}

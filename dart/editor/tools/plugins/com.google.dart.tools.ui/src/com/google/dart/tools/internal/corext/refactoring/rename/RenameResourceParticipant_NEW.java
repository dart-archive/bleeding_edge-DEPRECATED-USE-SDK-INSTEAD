/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.core.utilities.io.FilenameUtils;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.ServerMoveFileRefactoring;
import com.google.dart.tools.ui.internal.refactoring.ServiceUtils_NEW;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

/**
 * {@link RenameParticipant} for updating resource references in Dart libraries.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameResourceParticipant_NEW extends RenameParticipant {
  private IFile file;

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
      throws OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return null;
  }

  @Override
  public Change createPreChange(IProgressMonitor pm) {
    RenameArguments arguments = getArguments();
    IPath fileLocation = file.getLocation();
    String oldFile = fileLocation.toOSString();
    String oldDir = fileLocation.removeLastSegments(1).toOSString();
    String newName = arguments.getNewName();
    String newFile = FilenameUtils.concat(oldDir, newName);
    try {
      ServerMoveFileRefactoring refactoring = new ServerMoveFileRefactoring(oldFile);
      refactoring.setNewFile(newFile);
      if (refactoring.checkAllConditions(pm).hasError()) {
        return null;
      }
      // prepare change
      Change change = refactoring.createChange(pm);
      change = ServiceUtils_NEW.expandSingleChildCompositeChanges(change);
      return change;
    } catch (Throwable e) {
      return null;
    }
  }

  @Override
  public String getName() {
    return RefactoringMessages.RenameResourceParticipant_name;
  }

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof IFile) {
      file = (IFile) element;
      return true;
    }
    return false;
  }
}

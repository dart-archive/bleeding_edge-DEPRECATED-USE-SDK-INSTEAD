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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.MoveResourcesDescriptor;

/**
 * Central access point to execute move refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class MoveSupport {
  public static Refactoring createMoveRefactoring(RefactoringStatus status, IResource[] resources,
      IContainer destination) throws CoreException {
    RefactoringContribution contribution = RefactoringCore.getRefactoringContribution(MoveResourcesDescriptor.ID);
    MoveResourcesDescriptor descriptor = (MoveResourcesDescriptor) contribution.createDescriptor();
    descriptor.setResourcesToMove(resources);
    descriptor.setDestination(destination);
    Refactoring refactoring = descriptor.createRefactoring(status);
    return refactoring;
  }

  /**
   * Performs "MoveResources" refactoring.
   */
  public static void performMove(RefactoringStatus status, IResource[] resources,
      IContainer destination) throws Exception {
    Refactoring refactoring = createMoveRefactoring(status, resources, destination);
    RefactoringExecutionHelper helper = new RefactoringExecutionHelper(
        refactoring,
        RefactoringCore.getConditionCheckingFailedSeverity(),
        RefactoringSaveHelper.SAVE_ALL,
        DartToolsPlugin.getActiveWorkbenchShell(),
        DartToolsPlugin.getActiveWorkbenchWindow());
    helper.perform(true, true);
  }
}

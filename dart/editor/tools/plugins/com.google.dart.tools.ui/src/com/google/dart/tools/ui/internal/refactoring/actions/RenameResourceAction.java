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
package com.google.dart.tools.ui.internal.refactoring.actions;

import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.ui.actions.SelectionDispatchAction;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameResourceAction extends SelectionDispatchAction {

  public RenameResourceAction(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    IResource element = getResource(selection);
    if (element == null)
      setEnabled(false);
    else
      setEnabled(RefactoringAvailabilityTester.isRenameAvailable(element));
  }

  @Override
  public void run(IStructuredSelection selection) {
    IResource resource = getResource(selection);
    if (!RefactoringAvailabilityTester.isRenameAvailable(resource))
      return;
    RefactoringExecutionStarter.startRenameResourceRefactoring(resource, getShell());
  }

  private static IResource getResource(IStructuredSelection selection) {
    if (selection.size() != 1)
      return null;
    Object first = selection.getFirstElement();
    if (!(first instanceof IResource))
      return null;
    return (IResource) first;
  }
}

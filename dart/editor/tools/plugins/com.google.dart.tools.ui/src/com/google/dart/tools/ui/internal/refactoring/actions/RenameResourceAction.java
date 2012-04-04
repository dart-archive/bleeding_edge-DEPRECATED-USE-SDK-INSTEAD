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

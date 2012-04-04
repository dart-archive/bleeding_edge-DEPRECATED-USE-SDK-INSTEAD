package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.widgets.Shell;

/**
 * Opens the user interface for a given refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class UserInterfaceStarter {

  private RefactoringWizard fWizard;

  /**
   * Actually activates the user interface. This default implementation assumes that the
   * configuration element passed to <code>initialize
   * </code> has an attribute wizard denoting the wizard class to be used for the given refactoring.
   * <p>
   * Subclasses may override to open a different user interface
   * 
   * @param refactoring the refactoring for which the user interface should be opened
   * @param parent the parent shell to be used
   * @param saveMode a save mode from {@link RefactoringSaveHelper}
   * @return <code>true</code> iff the refactoring was executed, <code>false</code> otherwise
   * @exception CoreException if the user interface can't be activated
   */
  public boolean activate(Refactoring refactoring, Shell parent, int saveMode) throws CoreException {
    String title = fWizard.getDefaultPageTitle();
    if (title == null) {
      title = ""; //$NON-NLS-1$
    }
    return new RefactoringStarter().activate(fWizard, parent, title, saveMode);
  }

  /**
   * Initializes this user interface starter with the given wizard.
   * 
   * @param wizard the refactoring wizard to use
   */
  public void initialize(RefactoringWizard wizard) {
    fWizard = wizard;
  }
}

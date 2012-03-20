package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameDartElementAction;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameResourceAction;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Renames a Dart element or workbench resource.
 * <p>
 * Action is applicable to selections containing elements of type <code>IJavaElement</code> or
 * <code>IResource</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class RenameAction extends SelectionDispatchAction {

  private RenameDartElementAction fRenameDartElement;
  private RenameResourceAction fRenameResource;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Java editor
   * 
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public RenameAction(DartEditor editor) {
    this(editor.getEditorSite());
    fRenameDartElement = new RenameDartElementAction(editor);
  }

  /**
   * Creates a new <code>RenameAction</code>. The action requires that the selection provided by the
   * site's selection provider is of type <code>
   * org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param site the site providing context information for this action
   */
  public RenameAction(IWorkbenchSite site) {
    super(site);
    setText(RefactoringMessages.RenameAction_text);
    fRenameDartElement = new RenameDartElementAction(site);
    fRenameDartElement.setText(getText());
    fRenameResource = new RenameResourceAction(site);
    fRenameResource.setText(getText());
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.RENAME_ACTION);
  }

  @Override
  public void run(IStructuredSelection selection) {
    if (fRenameDartElement.isEnabled()) {
      fRenameDartElement.run(selection);
    }
    if (fRenameResource != null && fRenameResource.isEnabled()) {
      fRenameResource.run(selection);
    }
  }

  @Override
  public void run(ITextSelection selection) {
    if (fRenameDartElement.isEnabled()) {
      fRenameDartElement.run(selection);
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    fRenameDartElement.selectionChanged(event);
    if (fRenameResource != null) {
      fRenameResource.selectionChanged(event);
    }
    setEnabled(computeEnabledState());
  }

  @Override
  public void update(ISelection selection) {
    fRenameDartElement.update(selection);

    if (fRenameResource != null) {
      fRenameResource.update(selection);
    }

    setEnabled(computeEnabledState());
  }

  private boolean computeEnabledState() {
    if (fRenameResource != null) {
      return fRenameDartElement.isEnabled() || fRenameResource.isEnabled();
    } else {
      return fRenameDartElement.isEnabled();
    }
  }
}

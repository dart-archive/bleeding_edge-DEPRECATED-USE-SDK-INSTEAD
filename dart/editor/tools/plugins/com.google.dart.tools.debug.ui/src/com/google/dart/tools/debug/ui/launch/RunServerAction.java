package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.ui.internal.preferences.DebugPreferencePage;
import com.google.dart.tools.debug.ui.internal.server.DartServerLaunchShortcut;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.dialogs.FilteredPreferenceDialog;
import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;

/**
 * Launch a Dart server application.
 */

@SuppressWarnings("restriction")
public class RunServerAction extends Action implements ISelectionListener, IPartListener {
  private IWorkbenchWindow window;
  ISelection selection;

  public RunServerAction(IWorkbenchWindow window) {
    this.window = window;

    setText("Launch in Dart Server");
    setId(DartToolsPlugin.PLUGIN_ID + ".runServerAction");
    setDescription("Launch in Dart Server");
    setToolTipText("Launch in Dart Server");
    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/run_server.png"));
    window.getSelectionService().addSelectionListener(this);
    window.getPartService().addPartListener(this);
    setEnabled(true);
  }

  @Override
  public void partActivated(IWorkbenchPart part) {
    if (part instanceof IEditorPart) {
      handleEditorActivated((IEditorPart) part);
    }
  }

  @Override
  public void partBroughtToTop(IWorkbenchPart part) {

  }

  @Override
  public void partClosed(IWorkbenchPart part) {

  }

  @Override
  public void partDeactivated(IWorkbenchPart part) {

  }

  @Override
  public void partOpened(IWorkbenchPart part) {

  }

  @Override
  public void run() {

    String vmExecPath = DartDebugCorePlugin.getPlugin().getDartVmExecutablePath();
    if (vmExecPath.length() == 0) {
      if (showPreferenceDialog() != Window.OK) {
        return;
      }
    }

    DartServerLaunchShortcut launchShortcut = new DartServerLaunchShortcut();
    launchShortcut.launch(selection, ILaunchManager.RUN_MODE);

  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      handleSelectionChanged((IStructuredSelection) selection);
    }
  }

  private void handleEditorActivated(IEditorPart editorPart) {
    if (editorPart.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) editorPart.getEditorInput();

      handleSelectionChanged(new StructuredSelection(input.getFile()));
    }
  }

  private void handleSelectionChanged(IStructuredSelection selection) {
    if (selection != null && !selection.isEmpty()) {
      this.selection = selection;
    } else {
      selection = null;
    }
    setEnabled(selection != null);

  }

  private int showPreferenceDialog() {

    FilteredPreferenceDialog dialog = WorkbenchPreferenceDialog.createDialogOn(window.getShell(),
        DebugPreferencePage.PAGE_ID);

    dialog.showOnly(new String[] {DebugPreferencePage.PAGE_ID});
    return dialog.open();
  }

}

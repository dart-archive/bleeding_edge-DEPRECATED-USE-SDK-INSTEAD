package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Launch a Dart server application.
 */
public class RunServerAction extends Action {
  private IWorkbenchWindow window;

  public RunServerAction(IWorkbenchWindow window) {
    this.window = window;

    setText("Launch in Dart Server");
    setId(DartToolsPlugin.PLUGIN_ID + ".runServerAction");
    setDescription("Launch in Dart Server");
    setToolTipText("Launch in Dart Server");
    setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/run_server.png"));
    setEnabled(false);
  }

  @Override
  public void run() {
    // TODO(devoncarew):
    MessageDialog.openInformation(window.getShell(), "Run Server", "Under construction.");
  }
}

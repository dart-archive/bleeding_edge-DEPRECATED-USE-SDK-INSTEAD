/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.pub.PubMessages;
import com.google.dart.tools.core.pub.RunPubJob;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action that runs the pub lish command for the selected package
 */
public class RunPublishAction extends RunPubAction {

  private class PubMessageDialog extends MessageDialog {

    public PubMessageDialog(Shell parentShell, String dialogTitle, String dialogMessage) {
      super(parentShell, dialogTitle, null, dialogMessage, MessageDialog.CONFIRM, new String[] {
          IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
    }

    @Override
    protected Control createCustomArea(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
      layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
      layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
      composite.setLayout(layout);
      composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      Text textArea = new Text(composite, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
      textArea.setEditable(false);
      textArea.setText(stringBuilder.toString());
      return composite;
    }

    @Override
    protected boolean customShouldTakeFocus() {
      return false;
    }

  }

  private static final String WARNINGS_REGEX = "Package has [0-9]+ warning(s)?."; //$NON-NLS-1$

  private static final String ERRORS_STRING = "Missing requirements:"; //$NON-NLS-1$

  private static final String ZERO_WARNINGS = "Package has 0 warnings."; //$NON-NLS-1$

  public static RunPublishAction createPubPublishAction(IWorkbenchWindow window) {
    RunPublishAction action = new RunPublishAction(window);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Publish"));
    action.setDescription(NLS.bind(
        ActionMessages.RunPubAction_commandDesc,
        RunPubJob.PUBLISH_COMMAND));
    return action;
  }

  private RunPublishAction(IWorkbenchWindow window) {
    super(window, RunPubJob.PUBLISH_COMMAND);
  }

  @Override
  protected void runPubJob(IContainer container) {
    if (container.findMember(DartCore.PUBSPEC_FILE_NAME) != null) {

      MessageConsole console = DartCore.getConsole();
      console.printSeparator(NLS.bind(PubMessages.RunPubJob_running, RunPubJob.PUBLISH_COMMAND));

      // use publish --dry-run to do just validation on the package and do not upload.
      List<String> args = buildPublishCommand("--dry-run"); //$NON-NLS-1$
      runPub(container, console, args, true);
      // check if we can publish - no errors and user confirms
      if (!confirmPackageUpload(stringBuilder.toString())) {
        console.println(ActionMessages.RunPubAction_publish_upload_cancel);
        return;
      }
      // use publish --force upload package without asking for user confirmation - the editor has done so.  
      args = buildPublishCommand("--force"); //$NON-NLS-1$
      runPub(container, console, args, false);

    } else {
      MessageDialog.openError(
          getShell(),
          ActionMessages.RunPubAction_fail,
          ActionMessages.RunPubAction_fileNotFound);
    }
  }

  private List<String> buildPublishCommand(String arg) {
    DartSdk sdk = DartSdkManager.getManager().getSdk();
    File pubFile = sdk.getPubExecutable();
    List<String> args = new ArrayList<String>();
    args.add(pubFile.getAbsolutePath());
    args.add(RunPubJob.PUBLISH_COMMAND);
    args.add(arg);
    return args;
  }

  private boolean confirmPackageUpload(String output) {
    if (output.indexOf(ERRORS_STRING) != -1) { // missing requirements, cannot publish
      MessageDialog.openInformation(
          getShell(),
          "Publish package",
          ActionMessages.RunPubAction_publish_missing_requirements);
      return false;
    }
    Pattern warningsPattern = Pattern.compile(WARNINGS_REGEX);
    Matcher m = warningsPattern.matcher(output);
    String message = "";
    if (m.find()) {
      if (!m.group(0).equals(ZERO_WARNINGS)) {
        message = m.group(0);
      }
    }
    int returnCode = new PubMessageDialog(
        getShell(),
        ActionMessages.RunPubAction_publish_dialog_title,
        ActionMessages.RunPubAction_publish_upload_message + "\n" + message).open();

    return (returnCode == 0);
  }

}

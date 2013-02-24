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
package com.google.dart.tools.ui.actions;

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.pub.PubMessages;
import com.google.dart.tools.core.pub.RunPubJob;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action that runs pub commands on the selected project
 */
public class RunPubAction extends InstrumentedSelectionDispatchAction {

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

  public static RunPubAction createPubInstallAction(IWorkbenchWindow window) {
    RunPubAction action = new RunPubAction(window, RunPubJob.INSTALL_COMMAND);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Install"));
    action.setDescription(NLS.bind(
        ActionMessages.RunPubAction_commandDesc,
        RunPubJob.INSTALL_COMMAND));
    return action;
  }

  public static RunPubAction createPubPublishAction(IWorkbenchWindow window) {
    RunPubAction action = new RunPubAction(window, RunPubJob.PUBLISH_COMMAND);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Publish"));
    action.setDescription(NLS.bind(
        ActionMessages.RunPubAction_commandDesc,
        RunPubJob.PUBLISH_COMMAND));
    return action;
  }

  public static RunPubAction createPubUpdateAction(IWorkbenchWindow window) {
    RunPubAction action = new RunPubAction(window, RunPubJob.UPDATE_COMMAND);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Update"));
    action.setDescription(NLS.bind(
        ActionMessages.RunPubAction_commandDesc,
        RunPubJob.UPDATE_COMMAND));
    return action;
  }

  private String command;

  private StringBuilder stringBuilder;

  private RunPubAction(IWorkbenchWindow window, String command) {
    super(window);
    this.command = command;
  }

  @Override
  public void doRun(ISelection selection, Event event, InstrumentationBuilder instrumentation) {
    instrumentation.metric("command", command);

    if (!(selection instanceof ITextSelection)) {
      instrumentation.metric("Problem", "Selection was not a TextSelection");
    }

    IWorkbenchPage page = DartToolsPlugin.getActivePage();
    if (page == null) {
      instrumentation.metric("Problem", "Page was null");
      return;
    }

    IEditorPart part = page.getActiveEditor();
    if (part == null) {
      instrumentation.metric("Problem", "Part was null");
      return;
    }

    IEditorInput editorInput = part.getEditorInput();
    DartProject dartProject = EditorUtility.getDartProject(editorInput);
    if (dartProject == null) {
      instrumentation.metric("Problem", "dartProject was null");
      return;
    }

    IProject project = dartProject.getProject();
    instrumentation.data("Project", project.getName());
    runPubJob(project);

  }

  @Override
  public void doRun(IStructuredSelection selection, Event event,
      InstrumentationBuilder instrumentation) {

    instrumentation.metric("command", command);

    if (!selection.isEmpty() && selection.getFirstElement() instanceof IResource) {
      Object object = selection.getFirstElement();
      if (object instanceof IFile) {
        object = ((IFile) object).getParent();
      }
      while (object != null && ((IContainer) object).findMember(DartCore.PUBSPEC_FILE_NAME) == null) {
        object = ((IContainer) object).getParent();
      }
      if (object != null) {

        instrumentation.data("name", ((IContainer) object).getName());
        runPubJob((IContainer) object);

        return;
      } else {
        instrumentation.metric("Problem", "Object was null").log();
      }
    }

    instrumentation.metric("Problem", "pubspec.yaml file not selected, showing dialog");

    MessageDialog.openError(
        getShell(),
        ActionMessages.RunPubAction_fail,
        ActionMessages.RunPubAction_fileNotFound);

    instrumentation.log();
  }

  private List<String> buildPublishCommand(String arg) {
    DartSdk sdk = DartSdkManager.getManager().getSdk();
    File pubFile = new File(sdk.getDirectory().getAbsolutePath(), RunPubJob.PUB_PATH);
    List<String> args = new ArrayList<String>();
    args.add(sdk.getVmExecutable().getPath());
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

  private void copy(InputStream in, MessageConsole console) throws IOException {
    while (true) {
      int c = in.read();
      if (c == -1) {
        break;
      }
      String string = Character.toString((char) c);
      stringBuilder.append(string); // store output to check for errors and warnings
      console.print(string);
    }
  }

  private void runPub(IContainer container, final MessageConsole console, List<String> args,
      boolean wait) {

    ProcessBuilder builder = new ProcessBuilder();
    builder.directory(container.getLocation().toFile());
    builder.redirectErrorStream(true);
    builder.command(args);
    Map<String, String> env = builder.environment();
    env.put("DART_SDK", DartSdkManager.getManager().getSdk().getDirectory().getAbsolutePath()); //$NON-NLS-1$

    final Process process;
    try {
      process = builder.start();
      final Thread stdoutThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            copy(process.getInputStream(), console);
          } catch (IOException e) {
            // do nothing
          }
        }
      });
      stdoutThread.start();
      if (wait) {
        process.waitFor();
      }
    } catch (IOException e) {
      String message = NLS.bind(PubMessages.RunPubJob_failed, command, e.toString());
      console.println(message);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void runPubJob(IContainer container) {
    if (container.findMember(DartCore.PUBSPEC_FILE_NAME) != null) {
      if (command.equals(RunPubJob.PUBLISH_COMMAND)) {
        runPubPublish(container);
      } else {
        RunPubJob runPubJob = new RunPubJob(container, command);
        runPubJob.schedule();
      }
    } else {
      MessageDialog.openError(
          getShell(),
          ActionMessages.RunPubAction_fail,
          ActionMessages.RunPubAction_fileNotFound);
    }
  }

  private void runPubPublish(IContainer container) {
    stringBuilder = new StringBuilder();
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
  }

}

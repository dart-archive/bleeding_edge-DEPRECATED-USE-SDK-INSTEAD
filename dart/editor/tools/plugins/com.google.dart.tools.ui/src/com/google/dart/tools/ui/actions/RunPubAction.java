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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.pub.PubMessages;
import com.google.dart.tools.core.pub.RunPubJob;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Action that runs pub commands on the selected project
 */
public class RunPubAction extends InstrumentedSelectionDispatchAction {

  public static RunPubAction createPubDeployAction(IWorkbenchWindow window) {
    RunPubAction action = new RunPubAction(window, RunPubJob.BUILD_COMMAND);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Build (generates JS)"));
    action.setDescription(NLS.bind(ActionMessages.RunPubAction_commandDesc, RunPubJob.BUILD_COMMAND));
    return action;
  }

  public static RunPubAction createPubInstallAction(IWorkbenchWindow window) {
    RunPubAction action = new RunPubAction(window, RunPubJob.INSTALL_COMMAND);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Get"));
    action.setDescription(NLS.bind(
        ActionMessages.RunPubAction_commandDesc,
        RunPubJob.INSTALL_COMMAND));
    return action;
  }

  public static RunPubAction createPubInstallOfflineAction(IWorkbenchWindow window) {
    RunPubAction action = new RunPubAction(window, RunPubJob.INSTALL_OFFLINE_COMMAND);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Get Offline"));
    action.setDescription(NLS.bind(
        ActionMessages.RunPubAction_commandDesc,
        RunPubJob.INSTALL_COMMAND));
    return action;
  }

  public static RunPubAction createPubUpdateAction(IWorkbenchWindow window) {
    RunPubAction action = new RunPubAction(window, RunPubJob.UPDATE_COMMAND);
    action.setText(NLS.bind(ActionMessages.RunPubAction_commandText, "Upgrade"));
    action.setDescription(NLS.bind(
        ActionMessages.RunPubAction_commandDesc,
        RunPubJob.UPDATE_COMMAND));
    return action;
  }

  private String command;

  protected StringBuilder stringBuilder;

  RunPubAction(IWorkbenchWindow window, String command) {
    super(window);
    this.command = command;
  }

  @Override
  public void doRun(ISelection selection, Event event, UIInstrumentationBuilder instrumentation) {
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

    IContainer container = null;
    IEditorInput editorInput = part.getEditorInput();
    if (editorInput instanceof IFileEditorInput) {
      container = DartCore.getApplicationDirectory(((IFileEditorInput) editorInput).getFile());
    }
    if (container != null) {
      instrumentation.data("Container", container.getName());
      savePubspecFile(container);
      runPubJob(container);
    } else {
      instrumentation.metric("Problem", "Object was null").log();
      MessageDialog.openError(
          getShell(),
          ActionMessages.RunPubAction_fail,
          ActionMessages.RunPubAction_fileNotFound);

      instrumentation.log();
    }
  }

  @Override
  public void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {

    instrumentation.metric("command", command);

    if (!selection.isEmpty() && selection.getFirstElement() instanceof IResource) {
      Object object = selection.getFirstElement();
      if (object instanceof IFile) {
        object = ((IFile) object).getParent();
      }
      IContainer sourceFolder = (IContainer) object;
      while (object != null && ((IContainer) object).findMember(DartCore.PUBSPEC_FILE_NAME) == null) {
        object = ((IContainer) object).getParent();
      }
      if (object instanceof IContainer) {
        IContainer container = (IContainer) object;
        instrumentation.data("name", container.getName());
        savePubspecFile(container);
        runPubJob(container, sourceFolder);
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

  protected void runPub(IContainer container, final MessageConsole console, List<String> args,
      boolean wait) {

    stringBuilder = new StringBuilder();
    ProcessBuilder builder = new ProcessBuilder();
    builder.directory(container.getLocation().toFile());
    builder.redirectErrorStream(true);
    builder.command(args);

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

  protected void runPubJob(IContainer container) {
    runPubJob(container, null);
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

  private void runPubJob(IContainer container, IContainer sourceFolder) {
    if (container.findMember(DartCore.PUBSPEC_FILE_NAME) != null) {
      RunPubJob runPubJob;
      if (command.equals(RunPubJob.BUILD_COMMAND) && container != sourceFolder) {
        runPubJob = new RunPubJob(container, command, false, sourceFolder);
      } else {
        runPubJob = new RunPubJob(container, command, false);
      }
      runPubJob.schedule();
    } else {
      MessageDialog.openError(
          getShell(),
          ActionMessages.RunPubAction_fail,
          ActionMessages.RunPubAction_fileNotFound);
    }
  }

  private void savePubspecFile(IContainer container) {
    IResource resource = container.findMember(DartCore.PUBSPEC_FILE_NAME);
    if (resource != null) {
      IEditorPart editor = EditorUtility.isOpenInEditor(resource);
      if (editor != null && editor.isDirty()) {
        if (MessageDialog.openQuestion(
            getShell(),
            NLS.bind(ActionMessages.RunPubAction_commandText, command),
            ActionMessages.RunPubAction_savePubspecMessage)) {

          editor.doSave(new NullProgressMonitor());
        }
      }
    }
  }

}

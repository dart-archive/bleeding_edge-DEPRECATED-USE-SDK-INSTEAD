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
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Action that runs pub commands on the selected project
 */
public class RunPubAction extends SelectionDispatchAction {

  class RunPubJob extends Job {

    IProject project;

    public RunPubJob(IProject project) {
      super(NLS.bind(ActionMessages.RunPubAction_jobText, command));
      setRule(ResourcesPlugin.getWorkspace().getRoot());
      setUser(true);
      this.project = project;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

      try {
        ProcessBuilder builder = new ProcessBuilder();

        List<String> args = new ArrayList<String>();

        args.add(DartSdk.getInstance().getVmExecutable().getPath());
        args.add("--new_gen_heap_size=256"); //$NON-NLS-1$
        args.addAll(getPubCommand());

        builder.command(args);
        builder.directory(project.getLocation().toFile());
        Map<String, String> env = builder.environment();
        env.put("DART_SDK", DartSdk.getInstance().getDirectory().getAbsolutePath()); //$NON-NLS-1$
        builder.redirectErrorStream(true);

        ProcessRunner runner = new ProcessRunner(builder);
        runner.runSync(monitor);

        StringBuilder stringBuilder = new StringBuilder();
        if (!runner.getStdOut().isEmpty()) {
          stringBuilder.append(runner.getStdOut().trim() + "\n"); //$NON-NLS-1$
        }
        if (!runner.getStdErr().isEmpty()) {
          stringBuilder.append(runner.getStdErr().trim() + "\n"); //$NON-NLS-1$
        }

        if (runner.getExitCode() != 0) {
          DartCore.getConsole().println(
              NLS.bind(ActionMessages.RunPubAction_jobFail, command, stringBuilder.toString()));
          return new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, stringBuilder.toString());
        }

        DartCore.getConsole().println(stringBuilder.toString());
        return Status.OK_STATUS;

      } catch (OperationCanceledException exception) {
        DartCore.getConsole().println(ActionMessages.RunPubAction_cancel);
        return Status.CANCEL_STATUS;

      } catch (IOException ioe) {
        DartCore.getConsole().println(
            NLS.bind(ActionMessages.RunPubAction_jobFail, command, ioe.toString()));
        return Status.CANCEL_STATUS;

      } finally {
        monitor.done();
      }
    }
  }

  public static final String INSTALL_COMMAND = "install"; //$NON-NLS-1$
  public static final String UPDATE_COMMAND = "update"; //$NON-NLS-1$

  private static final String PUB_PATH = "util/pub/pub.dart"; //$NON-NLS-1$

  private String command;

  public RunPubAction(IWorkbenchSite site, String command) {
    super(site);
    setText(NLS.bind(ActionMessages.RunPubAction_commandText, command));
    setDescription(NLS.bind(ActionMessages.RunPubAction_commandDesc, command));
    this.command = command;
  }

  public RunPubAction(IWorkbenchWindow window, String command) {
    super(window);
    setText(NLS.bind(ActionMessages.RunPubAction_commandText, command));
    setDescription(NLS.bind(ActionMessages.RunPubAction_commandDesc, command));
    this.command = command;
  }

  @Override
  public void run(IStructuredSelection selection) {
    if (!selection.isEmpty() && selection.getFirstElement() instanceof IResource) {
      Object object = selection.getFirstElement();
      IProject project = ((IResource) object).getProject();
      RunPubJob runPubJob = new RunPubJob(project);
      runPubJob.schedule();
    } else {
      MessageDialog.openError(
          getShell(),
          ActionMessages.RunPubAction_fail,
          ActionMessages.RunPubAction_fileNotFound);
    }

  }

  private List<String> getPubCommand() {
    List<String> args = new ArrayList<String>();
    File pubFile = new File(DartSdk.getInstance().getDirectory().getAbsolutePath(), PUB_PATH);
    args.add(pubFile.getAbsolutePath());
    args.add(command);
    return args;
  }

}

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
package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Run the build.dart with deploy option which compiles the Polymer app to JavaScript.
 */
public class DeployPolymerAppHandler extends AbstractHandler {

  public DeployPolymerAppHandler() {

  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    ISelection selection = HandlerUtil.getActivePart(event).getSite().getSelectionProvider().getSelection();
    if (!selection.isEmpty()) {
      if (selection instanceof IStructuredSelection) {
        Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
        if (selectedObject instanceof IResource) {
          IContainer parent = ((IResource) selectedObject).getParent();
          while (parent != null) {
            IResource buildFile = parent.findMember("build.dart");
            if (buildFile != null) {
              runBuildFile(buildFile);
              return null;
            }
            parent = parent.getParent();
          }
          DartCore.getConsole().println(
              "Error: Could not find build.dart file for " + ((IResource) selectedObject).getName());
        }
      }
    }
    return null;
  }

  private void runBuildFile(IResource buildFile) {

    MessageConsole console = DartCore.getConsole();

    ProcessBuilder builder = new ProcessBuilder();
    builder.redirectErrorStream(true);
    List<String> args = new ArrayList<String>();

    String vmExecPath = "";
    if (DartSdkManager.getManager().hasSdk()) {
      File vmExec = DartSdkManager.getManager().getSdk().getVmExecutable();

      if (vmExec != null) {
        vmExecPath = vmExec.getAbsolutePath().toString();
      }
    }

    if (vmExecPath.length() == 0) {
      String message = "Could not find the Dart VM executable";
      console.print(message);
      DartCore.logError("Deploy Polymer app - " + message);
    }

    args.add(vmExecPath);
    args.add(buildFile.getLocation().toFile().getAbsolutePath());
    args.add("--deploy");

    builder.command(args);
    builder.directory(buildFile.getParent().getLocation().toFile());

    ProcessRunner runner = new ProcessRunner(builder);

    try {
      runner.runSync(new NullProgressMonitor());
    } catch (IOException e) {
      String message = "Failed to run " + buildFile.getLocation().toString() + e.toString();
      console.print(message);
      DartCore.logError(message, e);
    }

    StringBuilder stringBuilder = new StringBuilder();

    if (!runner.getStdOut().isEmpty()) {
      stringBuilder.append(runner.getStdOut().trim() + "\n"); //$NON-NLS-1$
    }

    console.printSeparator("build.dart --deploy");
    console.print(stringBuilder.toString());

  }

}

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
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Runs the appropriate script. Scripts name and hot keys are stored in a scripts.properties files
 * that is in the root of the Dart Editor installation directory. The properties file is a key value
 * pair of hot keys to script file name. The current selection is passed in as an argument to the
 * script. The keys bound are Ctrl+Shift+F1 through Ctrl+Shift+F5.
 * <p>
 * scripts.properties
 * <ol>
 * <li>Ctrl+Shift+F1=/testDir/script.sh</li>
 * <li>Ctrl+Shift+F2=/testDir/script2.sh</li>
 * <li>Ctrl+Shift+F3=/testDir/script3.sh</li>
 * <li>Ctrl+Shift+F4=/testDir/script4.sh</li>
 * <li>Ctrl+Shift+F5=/testDir/script5.sh</li>
 * </ol>
 * </p>
 */

public class RunScriptAction extends InstrumentedSelectionDispatchAction {

  private class RunScriptJob extends Job {

    private String fileLocation;
    private String scriptLocation;

    public RunScriptJob(IResource resource, String script) {
      super("Run Script Job");
      if (resource != null) {
        fileLocation = resource.getLocation().toString();
      }
      scriptLocation = script;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

      ProcessBuilder builder = new ProcessBuilder();
      builder.redirectErrorStream(true);
      List<String> args = new ArrayList<String>();
      args.add(scriptLocation);
      if (fileLocation != null) {
        args.add(fileLocation);
      }
      builder.command(args);

      ProcessRunner runner = new ProcessRunner(builder);

      try {
        runner.runSync(monitor);
      } catch (IOException e) {
        String message = "Failed to run script " + scriptLocation + e.toString();
        return new Status(IStatus.CANCEL, DartToolsPlugin.PLUGIN_ID, message, e);
      }

      StringBuilder stringBuilder = new StringBuilder();

      if (!runner.getStdOut().isEmpty()) {
        stringBuilder.append(runner.getStdOut().trim() + "\n"); //$NON-NLS-1$
      }

      int exitCode = runner.getExitCode();

      if (exitCode != 0) {
        String output = "[" + exitCode + "] " + stringBuilder.toString();
        String message = "Failed to run script " + scriptLocation + output;
        return new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, message);
      }

      return new Status(IStatus.OK, DartToolsPlugin.PLUGIN_ID, stringBuilder.toString());
    }

  }

  private static String F1_KEY = "Ctrl+Shift+F1";
  private static String F2_KEY = "Ctrl+Shift+F2";
  private static String F3_KEY = "Ctrl+Shift+F3";
  private static String F4_KEY = "Ctrl+Shift+F4";
  private static String F5_KEY = "Ctrl+Shift+F5";

  private Properties properties;

  public RunScriptAction() {
    this(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
  }

  public RunScriptAction(IWorkbenchSite site) {
    super(site);

  }

  public RunScriptAction(IWorkbenchWindow window) {
    super(window);
  }

  @Override
  public void doRun(ISelection selection, Event event, UIInstrumentationBuilder instrumentation) {

    String scriptName = getScript(event.keyCode);
    instrumentation.metric("Running script ", scriptName);

    if (scriptName != null && !scriptName.isEmpty()) {
      IFile file = null;
      if (!selection.isEmpty()) {
        IWorkbenchPage page = DartToolsPlugin.getActivePage();
        if (page == null) {
          instrumentation.metric("Problem", "Page was null");
        } else {
          IEditorPart part = page.getActiveEditor();
          if (part == null) {
            instrumentation.metric("Problem", "Part was null");
          } else {
            IEditorInput editorInput = part.getEditorInput();
            if (editorInput instanceof IFileEditorInput) {
              file = ((IFileEditorInput) editorInput).getFile();
              new RunScriptJob(file, scriptName).schedule();
              return;
            }
          }
        }
      }
      new RunScriptJob(file, scriptName).schedule();
    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {

    String scriptName = getScript(event.keyCode);
    instrumentation.metric("Running script ", scriptName);

    if (!selection.isEmpty() && selection.getFirstElement() instanceof IResource) {
      IResource res = (IResource) selection.getFirstElement();
      new RunScriptJob(res, scriptName).schedule();
    } else {
      new RunScriptJob(null, scriptName).schedule();
    }
  }

  private String getScript(int keyCode) {

    Properties properties = getScriptProperties();
    String key = null;
    switch (keyCode) {
      case SWT.F1:
        key = F1_KEY;
        break;
      case SWT.F2:
        key = F2_KEY;
        break;
      case SWT.F3:
        key = F3_KEY;
        break;
      case SWT.F4:
        key = F4_KEY;
        break;
      case SWT.F5:
        key = F5_KEY;
        break;
    }
    return properties.getProperty(key);
  }

  private Properties getScriptProperties() {
    properties = new Properties();
    File installDirectory = DartCore.getEclipseInstallationDirectory();
    File file = new File(installDirectory, "scripts.properties");

    if (file.exists()) {
      try {
        properties.load(new FileReader(file));
      } catch (FileNotFoundException e) {
        DartCore.logError(e);
      } catch (IOException e) {
        DartCore.logError(e);
      }
    }
    return properties;
  }

}

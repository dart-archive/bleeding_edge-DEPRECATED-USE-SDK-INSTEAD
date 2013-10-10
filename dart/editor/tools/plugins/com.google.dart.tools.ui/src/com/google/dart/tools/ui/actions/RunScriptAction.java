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
import com.google.dart.tools.core.utilities.general.ScriptUtils;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

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

      IStatus status = ScriptUtils.runScript(scriptLocation, fileLocation, monitor);
      if (status.isOK()) {
        refreshEditor();
      }
      return status;
    }

  }

  private static String F1_KEY = "Ctrl+Shift+F1";
  private static String F2_KEY = "Ctrl+Shift+F2";
  private static String F3_KEY = "Ctrl+Shift+F3";
  private static String F4_KEY = "Ctrl+Shift+F4";
  private static String F5_KEY = "Ctrl+Shift+F5";

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

    MessageConsole console = DartCore.getConsole();
    console.clear();

    if (scriptName == null || scriptName.isEmpty()) {

      console.print("Unable to run script.  No script specified in '"
          + ScriptUtils.getPropertiesFile().getAbsolutePath() + "'");

    } else {

      console.print("Running script '" + scriptName + "'...\n");

      IFile file = getSelectedFile(instrumentation);

      new RunScriptJob(file, scriptName).schedule();
    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {

    String scriptName = getScript(event.keyCode);
    if (scriptName != null && !scriptName.isEmpty()) {
      MessageConsole console = DartCore.getConsole();
      console.clear();
      console.print("Running script '" + scriptName + "'...\n");

      instrumentation.metric("Running script ", scriptName);

      if (!selection.isEmpty() && selection.getFirstElement() instanceof IResource) {
        IResource res = (IResource) selection.getFirstElement();
        new RunScriptJob(res, scriptName).schedule();
        return;
      } else {
        IFile file = getSelectedFile(instrumentation);
        new RunScriptJob(file, scriptName).schedule();
      }
    }
  }

  private String getScript(int keyCode) {
    Properties properties = ScriptUtils.getScriptProperties();
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

  private IFile getSelectedFile(UIInstrumentationBuilder instrumentation) {
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
          return ((IFileEditorInput) editorInput).getFile();
        }
      }
    }
    return null;
  }

  /**
   * Refresh the active editor - this is to catch changes to read/write access of resource.
   */
  private void refreshEditor() {

    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (editor instanceof DartEditor) {
          ((DartEditor) editor).validateEditorInputState();
        }
      }
    });
  }

}

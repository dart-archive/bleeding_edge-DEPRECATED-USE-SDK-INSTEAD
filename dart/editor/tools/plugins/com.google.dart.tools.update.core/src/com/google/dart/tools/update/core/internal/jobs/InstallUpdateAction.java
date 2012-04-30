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
package com.google.dart.tools.update.core.internal.jobs;

import com.google.dart.tools.update.core.UpdateManager;
import com.google.dart.tools.update.core.internal.UpdateUtils;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * An action that installs an available Dart Editor update.
 */
public class InstallUpdateAction extends Action {

  private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$
  private static final String PROP_EXIT_DATA = "eclipse.exitdata"; //$NON-NLS-1$  
  private static final String PROP_VM = "eclipse.vm"; //$NON-NLS-1$
  private static final String PROP_COMMANDS = "eclipse.commands"; //$NON-NLS-1$
  private static final String PROP_VMARGS = "eclipse.vmargs"; //$NON-NLS-1$
  private static final String CMD_VMARGS = "-vmargs"; //$NON-NLS-1$

  private static final String NEW_LINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

  private final UpdateManager updateManager;

  private static final FileFilter UPDATE_OVERRIDE_FILTER = new FileFilter() {
    /**
     * Returns <code>true</code> if this file should be overwritten, <code>false</code> otherwise
     */
    @Override
    public boolean accept(File file) {

      //org.eclipse.equinox.simpleconfigurator/bundles.info
      if (file.getName().equals("bundles.info")) { //$NON-NLS-1$
        return true;
      }

      return false;
    }
  };

  /**
   * Create an instance.
   */
  public InstallUpdateAction(UpdateManager updateManager) {
    this.updateManager = updateManager;
  }

  @Override
  public void run() {
    try {
      applyUpdate();
      restart();
    } catch (Throwable th) {
      MessageDialog.openError(getShell(), UpdateJobMessages.InstallUpdateAction_errorTitle,
          NLS.bind(UpdateJobMessages.InstallUpdateAction_errorMessage, th.getMessage()));
    }
  }

  private void applyUpdate() throws InvocationTargetException, InterruptedException {
    new ProgressMonitorDialog(getShell()).run(true, false, new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException,
          InterruptedException {
        try {
          doApplyUpdate(monitor);
        } catch (IOException e) {
          throw new InvocationTargetException(e);
        }
      }
    });
  }

  //TODO (pquitslund): this step may be unnecessary if writing bundles.info suffices
  private String buildCommandLine() {
    String property = System.getProperty(PROP_VM);
    if (property == null) {
      throw new AssertionFailedException("System property \"" + PROP_VM + "\" not set");
    }

    StringBuffer result = new StringBuffer(512);
    result.append(property);
    result.append(NEW_LINE);

    // append the vmargs and commands. Assume that these already end in \n
    String vmargs = System.getProperty(PROP_VMARGS);
    if (vmargs != null) {
      result.append(vmargs);
    }

    //TODO (pquitslund): where does this really belong?
    result.append("-Declipse.refreshBundles=true"); //$NON-NLS-1$
    result.append(NEW_LINE);

    property = System.getProperty(PROP_COMMANDS);
    if (property != null) {
      result.append(property);
    }

    // put the vmargs back at the very end (the eclipse.commands property
    // already contains the -vm arg)
    if (vmargs != null) {
      result.append(CMD_VMARGS);
      result.append(NEW_LINE);
      result.append(vmargs);
    }

    return result.toString();
  }

  private void cleanupTempDir(File tmpDir, IProgressMonitor monitor) {
    File[] files = tmpDir.listFiles();
    monitor.beginTask(UpdateJobMessages.InstallUpdateAction_cleanup_task, files.length);
    for (File file : files) {
      UpdateUtils.delete(file);
      monitor.worked(1);
    }
    monitor.done();
  }

  private void doApplyUpdate(IProgressMonitor monitor) throws IOException {

    File tmpDir = UpdateUtils.getUpdateTempDir();

    SubMonitor mon = SubMonitor.convert(monitor,
        UpdateJobMessages.InstallUpdateAction_install_task, 100);

    cleanupTempDir(tmpDir, mon.newChild(3));

    IPath updatePath = updateManager.getLatestStagedUpdate().getLocalPath();
    UpdateUtils.unzip(updatePath.toFile(), tmpDir,
        UpdateJobMessages.InstallUpdateAction_extract_task, mon.newChild(70));

    File installTarget = UpdateUtils.getUpdateInstallDir();
    //TODO (pquitslund): only necessary for testing
    if (!installTarget.exists()) {
      installTarget.mkdir();
    }

    //TODO (pquitslund): add progress?
    UpdateUtils.deleteDirectory(new File(installTarget, "dart-sdk")); //$NON-NLS-1$
    UpdateUtils.deleteDirectory(new File(installTarget, "samples")); //$NON-NLS-1$

    UpdateUtils.copyDirectory(new File(tmpDir, "dart"), installTarget, UPDATE_OVERRIDE_FILTER, //$NON-NLS-1$
        mon.newChild(27));
  }

  private Shell getShell() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

  private void restart() {

    String commandLine = buildCommandLine();

    System.setProperty(PROP_EXIT_CODE, Integer.toString(24));
    System.setProperty(PROP_EXIT_DATA, commandLine);

    PlatformUI.getWorkbench().restart();
  }
}

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

import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.update.core.Revision;
import com.google.dart.tools.update.core.UpdateCore;
import com.google.dart.tools.update.core.UpdateManager;
import com.google.dart.tools.update.core.internal.UpdateUtils;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * An action that installs an available Dart Editor update.
 */
public class InstallUpdateAction extends Action {

  /**
   * Internal representation of an executable file that needs to be renamed before update and
   * cleaned up after update.
   */
  private static class Executable {
    static void add(List<Executable> list, String name, File executable) {
      if (executable != null) {
        list.add(new Executable(name, executable));
      }
    }

    private final String name;
    private final File executable;
    private final File oldExecutable;

    Executable(String name, File executable) {
      this.name = name;
      this.executable = executable;
      this.oldExecutable = new File(executable.getAbsolutePath() + ".old");
    }

    boolean deleteOld() {
      return !oldExecutable.exists() || oldExecutable.delete();
    }

    String getExistingProcessMessage() {
      return "Update complete, but existing " + name + " process still running.\n\n"
          + oldExecutable.getAbsolutePath();
    }

    String getRenameFailedMessage() {
      return "Could not update " + name + ". Please terminate any running\n" + name
          + " processes, check the file permissions, and try again.\n\n"
          + executable.getAbsolutePath() + "\n" + oldExecutable.getAbsolutePath();
    }

    boolean rename() {
      return !executable.exists() || (deleteOld() && executable.renameTo(oldExecutable));
    }

    void restore() {
      oldExecutable.renameTo(executable);
    }
  }

  private static class RetryUpdateDialog extends MessageDialog {

    public RetryUpdateDialog(Shell parentShell) {
      super(
          parentShell,
          UpdateJobMessages.InstallUpdateAction_bad_zip_dialog_title,
          null,
          UpdateJobMessages.InstallUpdateAction_bad_zip_dialog_msg,
          MessageDialog.QUESTION,
          new String[] {
              UpdateJobMessages.InstallUpdateAction_bad_zip_retry_confirm,
              UpdateJobMessages.InstallUpdateAction_bad_zip_dialog_cancel},
          0);
    }

  }

  private static final String SLASH = System.getProperty("file.separator");

  private static final String INSTALL_SCRIPT = "install.py";

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
      String name = file.getName();

      //org.eclipse.equinox.simpleconfigurator/bundles.info
      if (name.equals("bundles.info")) { //$NON-NLS-1$
        return true;
      }

      // DartEditor.app/Contents/MacOS/DartEditor.ini
      if (name.equals("DartEditor.ini")) { //$NON-NLS-1$
        //mac INI files need to be overwritten since they're signed
        if (DartCore.isMac()) {
          return true;
        }
        //on linux and windows, we handle a merge post-copy
        return false;
      }

      if (name.equals("editor.properties")) { //$NON-NLS-1$
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

    if (resourcesNeedSaving()) {

      //prompt to save dirty editors
      if (!MessageDialog.openConfirm(
          getShell(),
          UpdateJobMessages.InstallUpdateAction_confirm_save_title,
          UpdateJobMessages.InstallUpdateAction_confirm_save_msg)) {
        return;
      }

      //attempt to close dirty editors
      if (!PlatformUI.getWorkbench().saveAllEditors(false)) {
        MessageDialog.openError(
            getShell(),
            UpdateJobMessages.InstallUpdateAction_errorTitle,
            UpdateJobMessages.InstallUpdateAction_error_in_save);
        return;
      }
    }

    DirectoryBasedDartSdk sdk = DartSdkManager.getManager().getSdk();
    List<Executable> executables = new ArrayList<Executable>();
    Executable.add(executables, "Dart VM", sdk.getVmExecutable());
    Executable.add(executables, "Dartium", sdk.getDartiumExecutable());
    int index = 0;
    while (index < executables.size()) {
      if (!executables.get(index).rename()) {
        Executable failedRename = executables.get(index);
        --index;
        while (index >= 0) {
          executables.get(index).restore();
          --index;
        }
        MessageDialog.openError(
            getShell(),
            UpdateJobMessages.InstallUpdateAction_errorTitle,
            failedRename.getRenameFailedMessage());
        return;
      }
      ++index;
    }

    try {
      if (applyUpdate()) {
        for (Executable executable : executables) {
          if (!executable.deleteOld()) {
            MessageDialog.openError(
                getShell(),
                UpdateJobMessages.InstallUpdateAction_errorTitle,
                executable.getExistingProcessMessage());
          }
        }
        restart();
      }
    } catch (Throwable th) {
      UpdateCore.logError(th);
      MessageDialog.openError(
          getShell(),
          UpdateJobMessages.InstallUpdateAction_errorTitle,
          UpdateJobMessages.InstallUpdateAction_errorMessage);
    }
  }

  private boolean applyUpdate() throws InvocationTargetException, InterruptedException {

    final boolean result[] = new boolean[1];

    new ProgressMonitorDialog(getShell()) {
      @Override
      protected void configureShell(Shell shell) {
        shell.setText(UpdateJobMessages.InstallUpdateAction_progress_mon_title);
      }
    }.run(true, false, new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException,
          InterruptedException {
        try {
          result[0] = doApplyUpdate(monitor);
        } catch (IOException e) {
          throw new InvocationTargetException(e);
        }
      }

    });

    return result[0];
  }

  //TODO (pquitslund): this step may be unnecessary if writing bundles.info suffices
  private String buildCommandLine() {
    String property = System.getProperty(PROP_VM);
    if (property == null) {
      throw new AssertionFailedException("System property \"" + PROP_VM + "\" not set"); //$NON-NLS-1$ //$NON-NLS-2$
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
      UpdateUtils.delete(file, monitor);
    }
    monitor.done();
  }

  private void deleteUpdateDirectory(IProgressMonitor monitor) {
    IPath updateDir = UpdateCore.getUpdateDirPath();
    try {
      UpdateUtils.deleteDirectory(updateDir.toFile(), monitor);
    } catch (Throwable th) {
      // Don't let exceptions in cleanup block update
      UpdateCore.logError(th);
    }
  }

  private boolean doApplyUpdate(IProgressMonitor monitor) throws IOException {

    File installer = getInstaller();
    if (installer != null && installer.exists()) {
      return runInstaller(installer);
    }

    File installTarget = UpdateUtils.getUpdateInstallDir();
    //TODO (pquitslund): only necessary for testing
    if (!installTarget.exists()) {
      installTarget.mkdir();
    }
    File installScript = new File(installTarget, INSTALL_SCRIPT);
    File tmpDir = UpdateUtils.getUpdateTempDir();

    SubMonitor mon = SubMonitor.convert(monitor, null, installScript.exists() ? 120 : 100);

    cleanupTempDir(tmpDir, mon.newChild(3));

    Revision latestStagedUpdate = updateManager.getLatestStagedUpdate();
    File updateZip = latestStagedUpdate.getLocalPath().toFile();

    if (latestStagedUpdate == Revision.UNKNOWN || !UpdateUtils.isZipValid(updateZip)) {

      final boolean[] retry = new boolean[1];

      Display.getDefault().syncExec(new Runnable() {
        @Override
        public void run() {
          retry[0] = new RetryUpdateDialog(getShell()).open() == 0;
        }
      });

      if (retry[0]) {
        Revision latest = UpdateManager.getInstance().getLatestRevision();
        UpdateManager.getInstance().scheduleDownload(latest);
      }

      return false;
    }

    monitor.setTaskName(UpdateJobMessages.InstallUpdateAction_extract_task);
    UpdateUtils.unzip(
        updateZip,
        tmpDir,
        UpdateJobMessages.InstallUpdateAction_extract_task,
        mon.newChild(20));

    monitor.setTaskName(UpdateJobMessages.InstallUpdateAction_preparing_task);
    File sdkDir = new File(installTarget, "dart-sdk"); //$NON-NLS-1$
    UpdateUtils.deleteDirectory(sdkDir, mon.newChild(4));
    UpdateUtils.deleteDirectory(new File(installTarget, "samples"), mon.newChild(4)); //$NON-NLS-1$
    // TODO(keertip): check can be removed after all installs are on version with android dir
    File androidDir = new File(installTarget, "android");
    if (androidDir.exists()) {
      UpdateUtils.deleteDirectory(androidDir, mon.newChild(4));
    }
    terminateRunningDartLaunches();

    File dartium = DartSdkManager.getManager().getSdk().getDartiumWorkingDirectory(installTarget);
    try {
      UpdateUtils.delete(dartium, mon.newChild(2));
    } catch (Throwable th) {
      //TODO(pquitslund): handle delete errors
      UpdateCore.logError(th);
    }

    monitor.setTaskName(UpdateJobMessages.InstallUpdateAction_install_task);
    File installDir = new File(tmpDir, "dart"); //$NON-NLS-1$
    int fileCount = UpdateUtils.countFiles(installDir);
    UpdateUtils.copyDirectory(
        installDir,
        installTarget,
        UPDATE_OVERRIDE_FILTER,
        mon.newChild(67).setWorkRemaining(fileCount));

    //update/merge DartEditor.ini
    if (!DartCore.isMac()) {
      //mac INI files are not merged since that would throw off signing
      mergeIniFile(installDir, installTarget);
    }

    //ensure executables (such as the analyzer, pub and VM) have the exec bit set 
    UpdateUtils.ensureExecutable(new File(sdkDir, "bin").listFiles()); //$NON-NLS-1$
    UpdateUtils.ensureExecutable(DartSdkManager.getManager().getSdk().getDartiumExecutable());

    //run install.py if present
    if (installScript.exists()) {
      monitor.setTaskName("Running " + installScript.getName() + " script");
      runInstallScript(installScript, mon.newChild(20));
    }

    // Cleanup
    deleteUpdateDirectory(monitor);

    return true;
  }

  private File getIni(File dir) {
    //NOTE: only used for Windows and Linux
    return new File(dir, "DartEditor.ini"); //$NON-NLS-1$
  }

  /**
   * @return
   */
  private File getInstaller() {
    IPath zipPath = updateManager.getLatestStagedUpdate().getLocalPath();
    IPath msiFile = zipPath.removeFileExtension().addFileExtension("msi");
    File installer = msiFile.toFile();
    return installer;
  }

  private Shell getShell() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

  private boolean isDartLaunch(ILaunch launch) {
    try {
      return launch.getLaunchConfiguration().getType().getIdentifier().startsWith("com.google"); //$NON-NLS-1$
    } catch (CoreException e) {
      UpdateCore.logError(e);
    }
    return false;
  }

  private void mergeIniFile(File installDir, File installTarget) {

    File latestIni = getIni(installDir);
    File currentIni = getIni(installTarget);

    try {

      INIRewriter.mergeAndWrite(currentIni, latestIni);

    } catch (IOException e) {
      UpdateCore.logError(e);
    }

  }

  private boolean resourcesNeedSaving() {
    for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
      for (IWorkbenchPage page : window.getPages()) {
        if (page.getDirtyEditors().length > 0) {
          return true;
        }
      }
    }
    return false;
  }

  private void restart() {

    String commandLine = buildCommandLine();

    System.setProperty(PROP_EXIT_CODE, Integer.toString(24));
    System.setProperty(PROP_EXIT_DATA, commandLine);

    PlatformUI.getWorkbench().restart();
  }

  private boolean runInstaller(File msiFile) {

    //msiexec.exe /i PATH_TO_NEW_INSTALLER /quiet
    List<String> args = new ArrayList<String>();
    args.add("msiexec.exe");
    args.add("/i");
    args.add(msiFile.getName());
    //TODO (pquitslund): investigate how reliable the quiet flag is
    //args.add("/quiet");

    ProcessBuilder builder = new ProcessBuilder(args);
    builder.directory(msiFile.getParentFile());
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder);
    try {
      runner.runAsync();
    } catch (IOException e) {
      DartCore.logError(msiFile.getName() + " IOException" + SLASH + runner.getStdOut(), e);
    }

    // Return false to indicate NO restart
    return false;
  }

  private void runInstallScript(File installScript, SubMonitor mon) {

    mon.beginTask("Running " + installScript.getName(), IProgressMonitor.UNKNOWN);

    ProcessBuilder builder = new ProcessBuilder("python", installScript.getName());
    builder.directory(installScript.getParentFile());
    builder.redirectErrorStream(true);

    ProcessRunner runner = new ProcessRunner(builder);
    int result;
    try {
      result = runner.runSync(mon);
    } catch (IOException e) {
      DartCore.logError(installScript.getName() + " IOException" + SLASH + runner.getStdOut(), e);
      return;
    }
    if (result != 0) {
      DartCore.logError(installScript.getName() + " terminated abnormally: " + result + SLASH
          + runner.getStdOut());
    }
  }

  private void terminate(ILaunch launch) {
    try {
      launch.terminate();
    } catch (DebugException e) {
      UpdateCore.logError(e);
    }
  }

  private void terminateRunningDartLaunches() {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    for (ILaunch launch : launchManager.getLaunches()) {
      if (!launch.isTerminated() && isDartLaunch(launch) && launch.canTerminate()) {
        terminate(launch);
      }
    }
  }

}

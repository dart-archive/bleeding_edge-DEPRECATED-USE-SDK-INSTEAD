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
package com.google.dart.tools.ui.internal.handlers;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.actions.WorkbenchRunnableAdapter;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Prompt the user for a file to be opened, and open that file in an editor. If a Dart application
 * or library file is selected, then open that library or application otherwise find and open the
 * application or library containing the selected file.
 */
@Deprecated
public class OpenFileHandler extends AbstractHandler {

  public static final String COMMAND_ID = DartUI.class.getPackage().getName() + ".file.open"; //$NON-NLS-1$

  private static final String FILTER_PATH_KEY = "openFileFilterPath"; //$NON-NLS-1$

  /**
   * Opens {@link DartLibrary} using {@link DartCore#openLibrary(File, IProgressMonitor)} and then
   * opens the given file from this library in an editor.
   */
  public static void openFile(Shell shell, final File file) throws ExecutionException {
    final DartLibrary[] library = new DartLibrary[1];
    final IFile[] resource = new IFile[1];
    try {
      PlatformUI.getWorkbench().getProgressService().run(
          true,
          true,
          new WorkbenchRunnableAdapter(new IWorkspaceRunnable() {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException {

              monitor.beginTask(HandlerMessages.OpenFile_taskName, 2);
              library[0] = DartCore.openLibrary(file, new NullProgressMonitor());
              if (library[0] != null) {
                library[0].setTopLevel(true);
              }
              monitor.worked(1);
              IResource[] resources = ResourceUtil.getResources(file);
              if (resources.length == 0) {
                resource[0] = null;
              } else if (resources.length == 1 && resources[0] instanceof IFile) {
                resource[0] = (IFile) resources[0];
              } else if (library[0] != null) {
                for (IResource r : resources) {
                  if (r instanceof IFile
                      && r.getProject().equals(library[0].getDartProject().getProject())) {
                    resource[0] = (IFile) r;
                  }
                }
              }
              monitor.worked(1);
              monitor.done();
            }
          })); // workspace lock

    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(
          e,
          shell,
          HandlerMessages.OpenFile_label,
          HandlerMessages.OpenFile_errorMessage);
    } catch (InterruptedException e) {
      // canceled by user
    }

    try {
      if (resource[0] != null) {
        EditorUtility.openInEditor(resource[0], true);
      } else if (library[0] == null) {
        MessageDialog.openError(
            shell,
            HandlerMessages.OpenFile_label,
            Messages.format(HandlerMessages.OpenFile_errorFileNotInLibrary, file.getName()));
      }
    } catch (PartInitException e) {
      throwFailedToOpen(file, e);
    } catch (DartModelException e) {
      throwFailedToOpen(file, e);
    }
  }

  private static void throwFailedToOpen(File file, Exception e) throws ExecutionException {
    throw new ExecutionException("Failed to open " + file, e);
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = HandlerUtil.getActiveShell(event);
    return execute(shell);
  }

  public Object execute(Shell shell) throws ExecutionException {
    String selectedFilePath = promptForFile(shell);
    if (selectedFilePath == null) {
      return null;
    }
    final File selectedFile = new File(selectedFilePath);
    if (!selectedFile.exists()) {
      return null;
    }
    openFile(shell, selectedFile);
    return null;
  }

  /**
   * Answer the path to the Dart samples directory or <code>null</code> if it cannot be found
   */
  private String getSamplesPath() {
    String userHome = System.getProperty("user.home");
    if (userHome == null) {
      return null;
    }
    IPath dartPath = new Path(userHome).append("Documents").append(Extensions.DART);
    if (!dartPath.toFile().exists()) {
      return null;
    }
    File dir = dartPath.append("sdk/samples").toFile();
    if (dir.exists()) {
      return dir.getPath();
    }
    dir = dartPath.append("samples").toFile();
    if (dir.exists()) {
      return dir.getPath();
    }
    return dartPath.toOSString();
  }

  private boolean isWindowsPlatform() {
    String platform = SWT.getPlatform();
    return platform.equals("win32") || platform.equals("wpf");
  }

  /**
   * Prompt the user to select a file to be opened.
   * 
   * @return The absolute path of the selected file or <code>null</code> if the user canceled the
   *         operation.
   */
  private String promptForFile(Shell shell) {
    FileDialog dialog = new FileDialog(shell, SWT.OPEN);
    String allFilesFilter = isWindowsPlatform() ? "*.*" : "*";
    dialog.setFilterNames(new String[] {"Dart Files", "All Files (" + allFilesFilter + ")"});
    dialog.setFilterExtensions(new String[] {"*.dart", allFilesFilter});
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettings();
    String filterPath = settings.get(FILTER_PATH_KEY);
    if (filterPath == null) {
      filterPath = getSamplesPath();
    }
    dialog.setFilterPath(filterPath);
    String result = dialog.open();
    if (result != null) {
      settings.put(FILTER_PATH_KEY, dialog.getFilterPath());
    }
    return result;
  }
}

/*
 * Copyright (c) 2011, the Dart project authors.
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
public class OpenFileHandler extends AbstractHandler {

  public static final String COMMAND_ID = DartUI.class.getPackage().getName() + ".file.open"; //$NON-NLS-1$

  private static final String FILTER_PATH_KEY = "openFileFilterPath"; //$NON-NLS-1$

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = HandlerUtil.getActiveShell(event);
    String selectedFilePath = promptForFile(shell);
    if (selectedFilePath == null) {
      return null;
    }
    final File selectedFile = new File(selectedFilePath);
    if (!selectedFile.exists()) {
      return null;
    }
    final DartLibrary[] libFile = new DartLibrary[1];
    final IFile[] files = new IFile[1];
    try {
      PlatformUI.getWorkbench().getProgressService().run(true, true,
          new WorkbenchRunnableAdapter(new IWorkspaceRunnable() {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException {

              monitor.beginTask(HandlerMessages.OpenFile_taskName, 30);
              libFile[0] = DartCore.openLibrary(selectedFile, new NullProgressMonitor());
              if (libFile[0] != null) {
                libFile[0].setTopLevel(true);
              }
              monitor.worked(1);
              IFile[] resources = ResourceUtil.getResources(selectedFile);
              if (resources.length == 0) {
                files[0] = null;
              } else if (resources.length == 1) {
                files[0] = resources[0];
              } else if (libFile[0] != null) {
                for (IFile file : resources) {
                  if (file.getProject().equals(libFile[0].getDartProject().getProject())) {
                    files[0] = file;
                  }
                }
              }
              monitor.done();
            }
          })); // workspace lock

    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(e, shell, HandlerMessages.OpenFile_label,
          HandlerMessages.OpenFile_errorMessage);
    } catch (InterruptedException e) {
      // canceled by user
    }

    try {
      if (files[0] != null) {
        EditorUtility.openInEditor(files[0], true);
      } else if (libFile[0] == null) {
        MessageDialog.openError(shell, HandlerMessages.OpenFile_label,
            Messages.format(HandlerMessages.OpenFile_errorFileNotInLibrary, selectedFile.getName()));
      }
    } catch (PartInitException e) {
      throwFailedToOpen(selectedFile, e);
    } catch (DartModelException e) {
      throwFailedToOpen(selectedFile, e);
    }
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

  private void throwFailedToOpen(File file, Exception e) throws ExecutionException {
    throw new ExecutionException("Failed to open " + file, e);
  }
}

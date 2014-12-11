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
package com.google.dart.tools.ui.internal.intro;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.download.DownloadUtilities;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;
import com.google.dart.tools.ui.internal.projects.ProjectUtils;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A helper for opening samples.
 */
public class SampleHelper {
  /**
   * Open the given sample.
   * 
   * @param description the sample description
   * @param window the current active workbench window
   */
  public static void openSample(final SampleDescription description, final IWorkbenchWindow window) {
    try {
      IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
      progressService.run(true, true, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          monitor.beginTask("Downloading " + description.name + "...", IProgressMonitor.UNKNOWN);
          createSample(description, monitor, window);
        }
      });
    } catch (InvocationTargetException e) {
      showError("Error Opening Sample", e.getCause());
      return;
    } catch (InterruptedException e) {
      DartToolsPlugin.log(e);
    }
  }

  static void showError(String title, Throwable exception) {
    showErrorStatus(title, new Status(
        IStatus.ERROR,
        DartUI.ID_PLUGIN,
        exception.toString(),
        exception));
  }

  static void showErrorStatus(final String title, final IStatus status) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        Shell shell = Display.getDefault().getActiveShell();
        ErrorDialog.openError(shell, title, status.getMessage(), status);
      }
    });
  }

  /**
   * Open the given sample.
   * 
   * @param sampleFile the sample file
   * @param monitor a progress monitor
   * @param window the current active workbench window
   */
  private static void createSample(final SampleDescription description,
      final IProgressMonitor monitor, final IWorkbenchWindow window)
      throws InvocationTargetException {
    final String sampleName = description.name;
    // user.home/dart/clock
    File potentialDir = new File(DartCore.getUserDefaultDartFolder(), sampleName);
    final File newProjectDir = ProjectUtils.generateUniqueSampleDirFrom(sampleName, potentialDir);

    final String newProjectName = newProjectDir.getName();
    final IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(newProjectName);
    final URI location = newProjectDir.toURI();

    // Copy sample to new directory before creating project so that builder will have the
    // resources to analyze first time through.
    try {
      downloadAndUnzip(description.url, newProjectDir, monitor);
    } catch (IOException e) {
      showError("Error Opening Sample", e);
      return;
    } catch (URISyntaxException e) {
      showError("Error Opening Sample", e);
      return;
    }

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        try {
          ProjectUtils.createNewProject(
              newProjectName,
              newProject,
              ProjectType.NONE,
              location,
              window,
              window.getShell());

          IResource mainFile = newProject.findMember(description.file);

          if (mainFile instanceof IFile) {
            EditorUtility.openInTextEditor((IFile) mainFile, true);
          }
        } catch (CoreException e) {
          showErrorStatus("Error Opening Sample", e.getStatus());
          return;
        }
      }
    });
  }

  private static void downloadAndUnzip(String url, File projectDir, IProgressMonitor monitor)
      throws IOException, URISyntaxException {
    File zipFile = DownloadUtilities.downloadZipFile(
        new URI(url),
        "sample",
        "Downloading sample",
        monitor);

    unzip(zipFile, projectDir);
  }

  /**
   * Uzips the given zip into the specified destination
   * 
   * @param zipFile the file to unzip
   * @param destination the destination directory
   * @param monitor
   * @throws IOException
   */
  private static void unzip(File zipFile, File destination) throws IOException {
    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
    ZipEntry entry;

    while ((entry = zis.getNextEntry()) != null) {
      int count;
      byte data[] = new byte[4096];

      // Strip off the first directory from the path name.
      String pathName = entry.getName();
      if (pathName.indexOf('/') != -1) {
        pathName = pathName.substring(pathName.indexOf('/') + 1);
      }

      if (pathName.isEmpty()) {
        continue;
      }

      File outFile = new File(destination, pathName);

      if (entry.isDirectory()) {
        if (!outFile.exists()) {
          outFile.mkdirs();
        }
      } else {
        if (!outFile.getParentFile().exists()) {
          outFile.getParentFile().mkdirs();
        }

        OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));

        while ((count = zis.read(data, 0, data.length)) != -1) {
          out.write(data, 0, count);
        }

        out.flush();
        out.close();
      }
    }

    zis.close();
  }
}

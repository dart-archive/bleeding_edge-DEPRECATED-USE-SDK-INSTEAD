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
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.RunPubAction;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;
import com.google.dart.tools.ui.internal.projects.ProjectUtils;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.MessageFormat;

/**
 * A helper for opening samples.
 */
public class SampleHelper {

  /**
   * Open the given sample.
   * 
   * @param sampleFile the sample file
   * @param monitor a progress monitor
   * @param window the current active workbench window
   */
  public static void openSample(final File sampleFile, final IProgressMonitor monitor,
      final IWorkbenchWindow window) {

    String sampleName = getDirectory(sampleFile).getName();
    // user.home/dart/clock
    File newProjectDir = new File(DartCore.getUserDefaultDartFolder(), sampleName);
    newProjectDir = generateUniqueSampleDirFrom(sampleName, newProjectDir);

    final String newProjectName = newProjectDir.getName();
    final IProject newProjectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(
        newProjectName);
    final URI location = newProjectDir.toURI();
    final File fileToOpen = new File(newProjectDir, getFilePath(sampleFile));

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {

        try {
          IProject newProject = ProjectUtils.createNewProject(
              newProjectName,
              newProjectHandle,
              ProjectType.NONE,
              location,
              window,
              window.getShell());

          FileUtilities.copyDirectoryContents(
              getDirectory(sampleFile),
              newProject.getLocation().toFile());
          newProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);

          if (newProject.findMember(DartCore.PUBSPEC_FILE_NAME) != null) {
            RunPubAction runPubAction = RunPubAction.createPubInstallAction(window);
            runPubAction.run(new StructuredSelection(newProject));
          }

          EditorUtility.openInTextEditor(ResourceUtil.getFile(fileToOpen));

        } catch (CoreException e) {
          DartToolsPlugin.log(e);
        } catch (IOException e) {
          DartToolsPlugin.log(e);
        }
      }
    });

  }

  /**
   * Open the given sample.
   * 
   * @param description the sample description
   * @param window the current active workbench site
   */
  public static void openSample(SampleDescription description, IWorkbenchPartSite site) {
    openSample(description, site.getWorkbenchWindow());
  }

  /**
   * Open the given sample.
   * 
   * @param description the sample description
   * @param window the current active workbench window
   */
  public static void openSample(final SampleDescription description, final IWorkbenchWindow window) {
    try {
      window.run(true, false, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          openSample(new File(description.directory, description.file), monitor, window);
        }
      });
    } catch (InvocationTargetException e) {
      DartToolsPlugin.log(e);
    } catch (InterruptedException e) {
      DartToolsPlugin.log(e);
    }
  }

  private static File generateUniqueSampleDirFrom(String baseName, File dir) {
    int index = 1;
    int copyIndex = baseName.lastIndexOf("-"); //$NON-NLS-1$
    if (copyIndex > -1) {
      String trailer = baseName.substring(copyIndex + 1);
      if (isNumber(trailer)) {
        try {
          index = Integer.parseInt(trailer);
          baseName = baseName.substring(0, copyIndex);
        } catch (NumberFormatException nfe) {
        }
      }
    }
    String newName = baseName;
    File newDir = new File(dir.getParent(), newName);
    while (newDir.exists()) {
      newName = MessageFormat.format(IntroMessages.IntroEditor_projectName, new Object[] {
          baseName, Integer.toString(index)});
      index++;
      newDir = new File(dir.getParent(), newName);
    }

    return newDir;
  }

  private static File getDirectory(File file) {
    IPath path = new Path(file.getAbsolutePath());
    int i = getPathIndexForSamplesDir(path);
    // get directory to depth samples + 1
    int index = i;
    Path p = (Path) path.removeLastSegments((path.segmentCount() - index) - 2);
    return new File(p.toString());
  }

  // get path in samples/sampleName to samples file that should be opened in editor
  private static String getFilePath(File file) {
    IPath path = new Path(file.getAbsolutePath());
    int i = getPathIndexForSamplesDir(path);
    int index = i;
    Path p = (Path) path.removeFirstSegments(index + 2);
    return p.toPortableString();
  }

  private static int getPathIndexForSamplesDir(IPath path) {
    String[] segments = path.segments();
    int i;
    for (i = 0; i < segments.length; i++) {
      if (segments[i].equals("samples")) {
        break;
      }
    }
    return i;
  }

  private static boolean isNumber(String string) {
    int numChars = string.length();
    if (numChars == 0) {
      return false;
    }
    for (int i = 0; i < numChars; i++) {
      if (!Character.isDigit(string.charAt(i))) {
        return false;
      }
    }
    return true;
  }

}

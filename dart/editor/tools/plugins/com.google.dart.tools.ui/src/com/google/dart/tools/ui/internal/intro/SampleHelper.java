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

import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.pub.PubspecConstants;
import com.google.dart.tools.core.pub.RunPubCacheListJob;
import com.google.dart.tools.core.pub.RunPubJob;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;
import com.google.dart.tools.ui.internal.projects.ProjectUtils;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper for opening samples.
 */
public class SampleHelper {

  private static String TODOMVC_PUBSPEC = "name: todomvc\ndescription: TodoMVC built with the Dart Web UI package\n"
      + "dependencies:\n  browser: any\n  web_ui: any\n";

  /**
   * Open the given sample.
   * 
   * @param sampleFile the sample file
   * @param monitor a progress monitor
   * @param window the current active workbench window
   */
  public static void openSample(SampleDescription description, final File sampleFile,
      final IProgressMonitor monitor, final IWorkbenchWindow window) {
    final String sampleName = getDirectory(sampleFile).getName();
    // user.home/dart/clock
    File potentialDir = new File(DartCore.getUserDefaultDartFolder(), sampleName);
    final File newProjectDir = ProjectUtils.generateUniqueSampleDirFrom(sampleName, potentialDir);

    final String newProjectName = newProjectDir.getName();
    final IProject newProjectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(
        newProjectName);
    final URI location = newProjectDir.toURI();
    final File fileToOpen = new File(newProjectDir, description.file);

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {

        // Copy sample to new directory before creating project
        // so that builder will have the resources to analyze first time through
        try {
          FileUtilities.copyDirectoryContents(getDirectory(sampleFile), newProjectDir);
        } catch (IOException e) {
          DartToolsPlugin.log(e);
        }

        try {
          ProjectUtils.createNewProject(
              newProjectName,
              newProjectHandle,
              ProjectType.NONE,
              location,
              window,
              window.getShell());

          Project project = DartCore.getProjectManager().getProject(newProjectHandle);
          IFile resource = (IFile) project.getResource(new FileBasedSource(fileToOpen));
          EditorUtility.openInTextEditor(resource, true);
        } catch (CoreException e) {
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
          monitor.beginTask("Opening sample...", IProgressMonitor.UNKNOWN);

          openSample(
              description,
              new File(description.directory, description.file),
              monitor,
              window);
        }
      });
    } catch (InvocationTargetException e) {
      DartToolsPlugin.log(e);
    } catch (InterruptedException e) {
      DartToolsPlugin.log(e);
    }
  }

  @SuppressWarnings("unused")
  private static boolean copySampleContents(final File sampleFile, final File newProjectDir,
      final IProject project) {

    Control focusControl = Display.getCurrent().getFocusControl();
    try {
      IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
      progressService.busyCursorWhile(new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          copyTodoMVCContents(sampleFile, newProjectDir, project, monitor);
        }
      });

      return true;
    } catch (Throwable ie) {
      return false;
    } finally {
      if (focusControl != null) {
        focusControl.setFocus();
      }
    }
  }

  // create a pubspec file and run pub install/update to get the latest web_ui package
  // copy the contents of example/todomvc 
  private static void copyTodoMVCContents(File sampleFile, File newProjectDir, IProject project,
      IProgressMonitor monitor) {

    monitor.subTask("Creating pubspec.yaml");
    File pubspecFile = new File(newProjectDir, DartCore.PUBSPEC_FILE_NAME);
    try {
      FileUtilities.create(pubspecFile);
    } catch (IOException e) {
      DartCore.logError("TodoMVC Sample - Error while creating pubspec.yaml", e);
    }
    monitor.worked(1);
    if (pubspecFile.exists()) {
      try {
        FileUtilities.setContents(pubspecFile, TODOMVC_PUBSPEC);
      } catch (IOException e) {
        DartCore.logError("TodoMVC Sample - Error while setting pubspec.yaml contents", e);
      }
      if (runPubInstall(newProjectDir, monitor)) {
        monitor.subTask("Get web_ui directory information");
        String webUiPackageLoc = getWebUiDir(monitor);
        monitor.worked(2);
        if (webUiPackageLoc != null) {
          File webuiDir = new File(webUiPackageLoc, "example/todomvc");
          try {
            monitor.subTask("Copy todomvc sample code");
            FileUtilities.copyDirectoryContents(webuiDir, newProjectDir);
            // delete lock file so pub install runs again to create packages folder in web directory
            FileUtilities.delete(new File(newProjectDir, DartCore.PUBSPEC_LOCK_FILE_NAME));
          } catch (IOException e) {
            DartCore.logError("TodoMVC Sample - Error while copying contents", e);
          }
          monitor.worked(1);
        } else {
          // error pub cache list did not run, cannot get location for web_ui package,
          // for now fall back to copying from samples
          try {
            monitor.subTask("Copy todomvc sample code");
            FileUtilities.copyDirectoryContents(getDirectory(sampleFile), newProjectDir);
          } catch (IOException e) {
            DartCore.logError("TodoMVC Sample - Error while copying contents", e);
          }
          monitor.worked(1);
        }
      }
    }

  }

  private static File getDirectory(File file) {
    IPath path = new Path(file.getAbsolutePath());
    int i = getPathIndexForSamplesDir(path);
    // get directory to depth samples + 1
    int index = i;
    Path p = (Path) path.removeLastSegments((path.segmentCount() - index) - 2);
    return new File(p.toString());
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

  // run pub cache list and get location of web ui package on disk
  @SuppressWarnings("unchecked")
  private static String getWebUiDir(IProgressMonitor monitor) {

    // TODO(keertip): refactor and move common code to RunPubCacheListJob()
    String message = new RunPubCacheListJob().run(monitor).getMessage();
    if (message.startsWith("{\"packages")) {
      Map<String, Object> object = null;
      try {
        object = PubYamlUtils.parsePubspecYamlToMap(message);
      } catch (Exception e) {
        DartCore.logError("Error while running pub cache list", e);
      }
      if (object != null) {
        HashMap<String, Object> pubCachePackages = (HashMap<String, Object>) object.get(PubspecConstants.PACKAGES);
        if (pubCachePackages != null) {
          Map<String, Object> webui = (Map<String, Object>) pubCachePackages.get("web_ui");
          if (webui != null) {
            String[] list = webui.keySet().toArray(new String[webui.keySet().size()]);
            list = PubYamlUtils.sortVersionArray(list);
            String latestVersion = list[list.length - 1].toString();
            return ((Map<String, String>) webui.get(latestVersion)).get(PubspecConstants.LOCATION);
          }
        }
      }
    }
    return null;
  }

  private static boolean runPubInstall(File newProjectDir, IProgressMonitor monitor) {

    // TODO(keertip): move to RunPubJob
    monitor.subTask("Running pub install");
    ProcessBuilder builder = new ProcessBuilder();
    builder.directory(newProjectDir);
    builder.redirectErrorStream(true);
    File pubFile = DartSdkManager.getManager().getSdk().getPubExecutable();

    List<String> args = new ArrayList<String>();
    if (DartCore.isMac()) {
      args.add("/bin/bash");
      args.add("--login");
      args.add("-c");
      args.add("\"" + pubFile.getAbsolutePath() + "\"" + " " + RunPubJob.INSTALL_COMMAND);
    } else {
      args.add(pubFile.getAbsolutePath());
      args.add(RunPubJob.INSTALL_COMMAND);
    }
    builder.command(args);
    ProcessRunner runner = new ProcessRunner(builder);
    try {
      runner.runSync(monitor);
    } catch (IOException e) {
      DartCore.logError("TodoMVC Sample - Running pub install", e);
    }
    monitor.worked(2);
    if (runner.getExitCode() == 0) {
      return true;
    }
    return false;
  }
}

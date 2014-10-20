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
package com.google.dart.tools.ui.internal.pub;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.pub.PubCacheManager_OLD;
import com.google.dart.tools.core.pub.PubspecConstants;
import com.google.dart.tools.core.pub.RunPubJob;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for installing and copying pub packages
 */
public class PubPackageUtils {

  /**
   * Copy the contents of the directory and delete lock file
   * 
   * @param newProjectDir
   * @param packageLoc
   * @param monitor
   */
  public static void copyPackageContents(File newProjectDir, String packageLoc,
      IProgressMonitor monitor) {
    try {
      monitor.subTask("Copy package contents");
      FileUtilities.copyDirectoryContents(new File(packageLoc), newProjectDir);
      // delete lock file so pub install runs again to create packages folder in web directory
      FileUtilities.delete(new File(newProjectDir, DartCore.PUBSPEC_LOCK_FILE_NAME));
    } catch (IOException e) {
      DartCore.logError("New Application from package - Error while copying contents", e);
    }
    monitor.worked(1);
  }

  /**
   * Create a pubspec file and set its contents
   * 
   * @param newProjectDir
   * @param pubspec
   * @param monitor
   * @return
   */
  public static boolean createPubspec(File newProjectDir, String pubspec, IProgressMonitor monitor) {
    monitor.subTask("Creating pubspec.yaml");
    File pubspecFile = new File(newProjectDir, DartCore.PUBSPEC_FILE_NAME);
    try {
      FileUtilities.create(pubspecFile);
    } catch (IOException e) {
      DartCore.logError("New Application from package - Error while creating pubspec.yaml", e);
      return false;
    }
    monitor.worked(1);
    if (pubspecFile.exists()) {
      try {
        FileUtilities.setContents(pubspecFile, pubspec);
      } catch (IOException e) {
        DartCore.logError(
            "New Application from package - Error while setting pubspec.yaml contents",
            e);
        return false;
      }
    }
    return true;
  }

  /**
   * Recursively deletes all "packages" folders.
   */
  public static void deletePackageDirectories(IProject project) throws CoreException {
    deletePackageDirectories(project.members());
  }

  /**
   * Run pub cache list command and get the location of the package-version in pub cache. If version
   * is not specified, return the latest version in pub cache.
   * 
   * @param monitor
   * @param packageName
   * @param version
   * @return
   */
  @SuppressWarnings("unchecked")
  public static String getPackageCacheDir(IProgressMonitor monitor, String packageName,
      String version) {

    HashMap<String, Object> pubCachePackages = PubCacheManager_OLD.getInstance().updateAndGetAllCachePackages();

    if (pubCachePackages != null) {
      Map<String, Object> packageMap = (Map<String, Object>) pubCachePackages.get(packageName);
      if (packageMap != null) {
        if (version != null) {
          return ((Map<String, String>) packageMap.get(version)).get(PubspecConstants.LOCATION);
        } else {
          String[] list = packageMap.keySet().toArray(new String[packageMap.keySet().size()]);
          list = PubYamlUtils.sortVersionArray(list);
          String latestVersion = list[list.length - 1].toString();
          return ((Map<String, String>) packageMap.get(latestVersion)).get(PubspecConstants.LOCATION);
        }
      }
    }

    return null;
  }

  /**
   * Run pub install on the given directory
   * 
   * @param newProjectDir
   * @param monitor
   * @return
   */
  public static boolean runPubInstall(File newProjectDir, IProgressMonitor monitor) {

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
      DartCore.logError("New Application from package - Running pub install", e);
    }
    monitor.worked(2);
    if (runner.getExitCode() == 0) {
      return true;
    }
    return false;
  }

  /**
   * Recursively visits all {@link IContainer} and runs "pub install" in ones that have a "pubspec"
   * file.
   */
  public static void runPubInstall(IContainer container) throws CoreException {
    for (IResource resource : container.members()) {
      if (resource instanceof IFile && resource.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
        RunPubJob job = new RunPubJob(container, RunPubJob.INSTALL_COMMAND, true);
        job.schedule();
        // Do we support Pub folder in another Pub folder?
        return;
      }
      if (resource instanceof IFolder) {
        IFolder folder = (IFolder) resource;
        runPubInstall(folder);
      }
    }
  }

  /**
   * Recursively deletes all "packages" folders.
   */
  private static void deletePackageDirectories(IResource[] resources) throws CoreException {
    for (IResource resource : resources) {
      if (resource instanceof IFolder) {
        IFolder folder = (IFolder) resource;
        if (DartCore.isPackagesDirectory(folder)) {
          folder.delete(true, null);
        } else {
          deletePackageDirectories(folder.members());
        }
      }
    }
  }

}

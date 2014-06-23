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
package com.google.dart.tools.core.pub;

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.internal.builder.DartBuilder;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

/**
 * This build participant has a higher priority and should be called by {@link DartBuilder} before
 * the dart project is analyzed or build.dart is run. It will run pub install on any pubspec file
 * that has been added or changed.
 * 
 * @coverage dart.tools.core.pub
 */
public class PubBuildParticipant implements BuildParticipant, BuildVisitor {

  /**
   * Flag indicating whether the sources are being reanalyzed and pub does NOT need to be run.
   */
  private boolean reanalyze = false;

  /**
   * The set of containers on which pub is currently running. Synchronize against this collection
   * before accessing it.
   */
  private static final HashSet<IContainer> currentContainers = new HashSet<IContainer>();

  @VisibleForTesting
  public static boolean isPubContainersEmpty() {
    return currentContainers.isEmpty();
  }

  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {
    if (reanalyze) {
      reanalyze = false;
    } else {
      event.traverse(this, false);
    }
  }

  @Override
  public void clean(CleanEvent event, IProgressMonitor monitor) {
    reanalyze = true;
  }

  /**
   * Find the pubspec associated with the specified resource, and if necessary run pub install
   */
  public void runPubFor(IResource res, IProgressMonitor monitor) {
    if (res == null) {
      return;
    }
    IWorkspaceRoot root = res.getWorkspace().getRoot();
    IContainer container = res.getType() == IResource.FILE ? res.getParent() : (IContainer) res;
    while (container != root) {
      IFile pubFile = container.getFile(new Path(DartCore.PUBSPEC_FILE_NAME));
      if (pubFile.exists()) {
        runPub(container, monitor);
        return;
      }
      container = container.getParent();
    }
  }

  @Override
  public boolean visit(IResourceDelta delta, IProgressMonitor monitor) {
    final IResource resource = delta.getResource();

    if (resource.getType() == IResource.FILE) {
      if (delta.getKind() == IResourceDelta.CHANGED) {
        if (resource.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
          runPub(resource.getParent(), monitor);
          processPubspecContents(resource, resource.getProject(), monitor);
          IResource lockFile = resource.getParent().findMember(DartCore.PUBSPEC_LOCK_FILE_NAME);
          if (lockFile != null) {
            processLockFileContents(lockFile, resource.getProject(), monitor);
          }
        }
        if (resource.getName().equals(DartCore.PUBSPEC_LOCK_FILE_NAME)) {
          processLockFileContents(resource, resource.getProject(), monitor);
        }
      }
    }

    return true;
  }

  @Override
  public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) {
    if (proxy.getType() == IResource.FILE) {
      if (proxy.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
        runPub(proxy.requestResource().getParent(), monitor);
        processPubspecContents(
            proxy.requestResource(),
            proxy.requestResource().getProject(),
            monitor);
        IResource lockFile = proxy.requestResource().getParent().findMember(
            DartCore.PUBSPEC_LOCK_FILE_NAME);
        if (lockFile != null) {
          processLockFileContents(lockFile, proxy.requestResource().getProject(), monitor);
        }
      }
      if (proxy.getName().equals(DartCore.PUBSPEC_LOCK_FILE_NAME)) {
        processLockFileContents(
            proxy.requestResource(),
            proxy.requestResource().getProject(),
            monitor);
      }
    }

    return true;
  }

  /**
   * Process the lockfile to extract the version information, and save the information in the
   * resource property DartCore.PUB_PACKAGE_VERSION
   * 
   * @param lockFile the pubspec.lock file
   * @param project containing the pubspec.lock file
   * @param monitor the progress monitor
   */
  protected void processLockFileContents(IResource lockFile, IProject project,
      IProgressMonitor monitor) {

    Map<String, String> versionMap = PubYamlUtils.getPackageVersionMap(lockFile);
    if (versionMap != null && !versionMap.isEmpty()) {
      for (String key : versionMap.keySet()) {
        IResource folder = lockFile.getParent().findMember(
            DartCore.PACKAGES_DIRECTORY_NAME + "/" + key);
        if (folder != null) {
          try {
            folder.setPersistentProperty(DartCore.PUB_PACKAGE_VERSION, versionMap.get(key));
          } catch (CoreException e) {
            DartCore.logError(e);
          }
        }
      }
      PubCacheManager.getInstance().updatePackagesList(0, versionMap);
      PubManager.getInstance().notifyListeners(lockFile.getParent());
    }
  }

  /**
   * Process the pubspec file to extract name and dependencies and save in model (DartProjectImpl)
   * 
   * @param pubspec the pubspec.yaml file
   * @param project IProject project for the pubspec file
   * @param monitor the progress monitor
   */
  protected void processPubspecContents(IResource pubspec, IProject project,
      IProgressMonitor monitor) {

    try {
      PubFolder pubFolder = DartCore.getProjectManager().getPubFolder(pubspec);
      if (pubFolder != null) {
        pubFolder.invalidatePubspec();
      }
    } catch (CoreException e) {
      DartCore.logError(e);
    } catch (IOException e) {
      DartCore.logError(e);
    }

  }

  /**
   * Execute the pub operation. This is overridden when testing this class to record the intent to
   * run pub but prevent actually running pub.
   * 
   * @param container the working directory in which pub should be run (not <code>null</code>)
   * @param monitor the progress monitor (not <code>null</code>)
   */
  protected void runPub(IContainer container, final IProgressMonitor monitor) {

    // If pub is already running for this container, then wait until it finishes before returning
    synchronized (currentContainers) {
      if (!currentContainers.add(container)) {
        while (currentContainers.contains(container)) {
          try {
            currentContainers.wait(1000);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
        return;
      }
    }

    try {
      // Only run pub automatically if it is not already up to date
      File dir = container.getLocation().toFile();
      File pubFile = new File(dir, DartCore.PUBSPEC_FILE_NAME);
      File lockFile = new File(dir, DartCore.PUBSPEC_LOCK_FILE_NAME);
      File packagesDir = new File(dir, DartCore.PACKAGES_DIRECTORY_NAME);
      if (packagesDir.exists() && lockFile.exists()
          && lockFile.lastModified() >= pubFile.lastModified()) {
        return;
      }

      // Run pub or notify the user that it needs to be run
      if (DartCore.getPlugin().isAutoRunPubEnabled()) {
        new RunPubJob(container, RunPubJob.INSTALL_COMMAND, true).run(monitor);
      } else {
        MessageConsole console = DartCore.getConsole();
        console.printSeparator("");
        console.println("Run Tools > Pub Get to install packages");
      }

    } finally {
      // Ensure that currentContainers is updated
      synchronized (currentContainers) {
        currentContainers.remove(container);
        currentContainers.notifyAll();
      }
    }
  }
}

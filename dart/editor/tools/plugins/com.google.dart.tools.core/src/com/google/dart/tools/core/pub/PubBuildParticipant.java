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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.internal.builder.DartBuilder;
import com.google.dart.tools.core.internal.model.DartProjectImpl;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.IOException;
import java.util.Map;

/**
 * This build participant has a higher priority and should be called by {@link DartBuilder} before
 * the dart project is analyzed or build.dart is run. It will run pub install on any pubspec file
 * that has been added or changed.
 */
public class PubBuildParticipant implements BuildParticipant, BuildVisitor {

  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {
    if (DartCoreDebug.ENABLE_PUB && DartCore.getPlugin().isAutoRunPubEnabled()) {
      event.traverse(this, false);
    }
  }

  @Override
  public void clean(CleanEvent event, IProgressMonitor monitor) {
    // nothing to do
  }

  @Override
  public boolean visit(IResourceDelta delta, IProgressMonitor monitor) {
    final IResource resource = delta.getResource();

    if (resource.getType() == IResource.FILE) {
      if (delta.getKind() == IResourceDelta.CHANGED) {
        if (resource.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
          runPub(resource.getParent(), monitor);
          processPubspecContents(resource, resource.getProject(), monitor);
        }
        if (resource.getName().equals(DartCore.PUBSPEC_LOCK_FILE_NAME)) {
          processLockFileContents(resource, resource.getProject(), monitor);
        }
      }
      if (delta.getKind() == IResourceDelta.REMOVED) {
        if (resource.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
          processPubspecContents(null, resource.getProject(), monitor);
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
      PubManager.getInstance().notifyListeners(lockFile.getParent());
    }
  }

  /**
   * Process the pubspec file to extract name and dependencies and save in model (DartProjectImpl)
   * 
   * @param pubspec the pubspec.yaml file
   * @param project IProject project for the pubspec file
   * @param monitor the progress monitor
   * @throws IOException
   * @throws CoreException
   */
  protected void processPubspecContents(IResource pubspec, IProject project,
      IProgressMonitor monitor) {

    DartProjectImpl dartProject = (DartProjectImpl) DartCore.create(project);
    dartProject.recomputePackageInfo(pubspec);

  }

  /**
   * Execute the pub operation. This is overridden when testing this class to record the intent to
   * run pub but prevent actually running pub.
   * 
   * @param container the working directory in which pub should be run (not <code>null</code>)
   * @param monitor the progress monitor (not <code>null</code>)
   */
  protected void runPub(IContainer container, final IProgressMonitor monitor) {
    new RunPubJob(container, RunPubJob.INSTALL_COMMAND).run(monitor);
  }

}

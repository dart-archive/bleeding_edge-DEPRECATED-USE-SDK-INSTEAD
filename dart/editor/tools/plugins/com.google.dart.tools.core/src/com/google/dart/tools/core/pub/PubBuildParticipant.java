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
import com.google.dart.tools.core.builder.CleanEvent;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This build participant is called from the DartBuilder before the dart project is built. It will
 * run pub install if the pubspec file has changed.
 */
public class PubBuildParticipant implements BuildParticipant {

  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {
    if (DartCoreDebug.ENABLE_PUB) {
      event.traverse(this);
    }
  }

  @Override
  public void clean(CleanEvent event, IProgressMonitor monitor) {
    // nothing to do
  }

  @Override
  public boolean visit(IResourceDelta delta, IProgressMonitor monitor) {
    final IResource resource = delta.getResource();

    // Don't traverse "packages" directories
    if (resource.getType() != IResource.FILE) {
      return !resource.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME);
    }

    if (delta.getKind() == IResourceDelta.CHANGED) {
      // TODO(keertip): optimize for just changes in dependencies
      if (resource.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
        runPub(resource.getParent(), monitor);
      }
    }
    return false;
  }

  @Override
  public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) {

    // Don't traverse "packages" directories
    if (proxy.getType() != IResource.FILE) {
      return !proxy.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME);
    }

    if (proxy.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
      runPub(proxy.requestResource().getParent(), monitor);
    }
    return false;
  }

  /**
   * Execute the pub operation. This is overridden when testing this class to record the intent to
   * run pub but prevent actually running pub.
   * 
   * @param container the workng directory in which pub should be run (not <code>null</code>)
   * @param monitor the progress monitor (not <code>null</code>)
   */
  protected void runPub(IContainer container, final IProgressMonitor monitor) {
    new RunPubJob(container, RunPubJob.INSTALL_COMMAND).run(monitor);
  }
}

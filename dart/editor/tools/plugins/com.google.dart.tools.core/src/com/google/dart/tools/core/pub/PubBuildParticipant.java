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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This build participant has a higher priority and should be called by {@link DartBuilder} before
 * the dart project is analyzed or build.dart is run. It will run pub install on any pubspec file
 * that has been added or changed.
 */
public class PubBuildParticipant implements BuildParticipant, BuildVisitor {

  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {
    if (DartCoreDebug.ENABLE_PUB) {
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
        // TODO(keertip): optimize for just changes in dependencies
        if (resource.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
          runPub(resource.getParent(), monitor);
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
      }
    }
    return true;
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

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
import com.google.dart.tools.core.builder.DartBuildParticipant;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.Map;

/**
 * This build participant is called from the DartBuilder before the dart project is built. It will
 * run pub install if the pubspec file has changed.
 */
public class PubBuildParticipant implements DartBuildParticipant {

  @Override
  public void build(int kind, Map<String, String> args, IResourceDelta delta,
      final IProgressMonitor monitor) throws CoreException {

    // Pub not supported on Windows XP
    if (!DartCoreDebug.ENABLE_PUB) {
      return;
    }

    // check if pubspec has changed, if so invoke pub install
    if (delta != null && delta.getKind() == IResourceDelta.CHANGED) {
      delta.accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) {
          final IResource resource = delta.getResource();
          if (resource.getType() != IResource.FILE) {
            return true;
          }
          // TODO(keertip): optimize for just changes in dependencies
          if (resource.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
            IContainer container = resource.getParent();
            new RunPubJob(container, RunPubJob.INSTALL_COMMAND).run(monitor);
          }
          return false;
        }
      });
    }
  }

  @Override
  public void clean(IProject project, IProgressMonitor monitor) throws CoreException {
    // do nothing

  }
}

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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.ScanCallback;
import com.google.dart.tools.core.builder.DartBuildParticipant;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.Extensions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubMonitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 * Instances of the class <code>DartBuilder</code> implement the incremental builder for Dart
 * projects.
 */
public class DartBuilder extends IncrementalProjectBuilder {

  private static final String PARTICIPANT_EXTENSION_POINT = "buildParticipant"; //$NON-NLS-1$
  private static final String PARTICIPANT_CONTRIBUTION = "buildParticipant"; //$NON-NLS-1$
  private static final String PARTICIPANT_CLASS_ATTR = "class"; //$NON-NLS-1$

  private final AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();

  private static DartBuildParticipant[] PARTICIPANTS;

  private static DartBuildParticipant[] getBuildParticipants() {
    if (PARTICIPANTS == null) {
      loadParticipantExtensions();
    }

    return PARTICIPANTS;
  }

  private static void loadParticipantExtensions() {
    IExtensionRegistry registry = RegistryFactory.getRegistry();
    ArrayList<DartBuildParticipant> participants = new ArrayList<DartBuildParticipant>();

    IExtensionPoint point = registry.getExtensionPoint(
        DartCore.PLUGIN_ID,
        PARTICIPANT_EXTENSION_POINT);

    for (IExtension extension : point.getExtensions()) {
      for (IConfigurationElement element : extension.getConfigurationElements()) {
        try {
          if (element.getName().equals(PARTICIPANT_CONTRIBUTION)) {
            DartBuildParticipant participant = (DartBuildParticipant) element.createExecutableExtension(PARTICIPANT_CLASS_ATTR);
            participants.add(participant);
          }
        } catch (CoreException e) {
          DartCore.logError(e);
        }
      }
    }

    PARTICIPANTS = participants.toArray(new DartBuildParticipant[participants.size()]);
  }

  @Override
  protected IProject[] build(final int kind, final Map<String, String> args,
      final IProgressMonitor _monitor) throws CoreException {

    int totalProgress = (getBuildParticipants().length + 1) * 10;
    final SubMonitor subMon = SubMonitor.convert(_monitor, totalProgress);

    final IResourceDelta delta = getDelta(getProject());

    // notify participants
    for (final DartBuildParticipant participant : getBuildParticipants()) {
      if (_monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      SafeRunner.run(new ISafeRunnable() {
        @Override
        public void handleException(Throwable exception) {
          DartCore.logError("Error notifying build participant", exception);
        }

        @Override
        public void run() throws Exception {
          participant.build(kind, args, delta, subMon.newChild(10));
        }
      });
    }

    if (_monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    DartBasedBuilder.getBuilder().build(getProject(), kind, delta, subMon.newChild(10));

    // If delta is null, then building a new project

    if (delta == null) {
      IPath location = getProject().getLocation();
      if (location != null) {
        ScanCallbackProvider provider = ScanCallbackProvider.getProvider();
        ScanCallback callback = provider != null ? provider.newCallback() : null;
        server.scan(location.toFile(), callback);
      }
    } else {
      // Recursively process the resource delta
      delta.accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) {
          IResource resource = delta.getResource();
          IPath location = resource.getLocation();
          if (location == null) {
            return false;
          }
          File file = location.toFile();

          // Process folder
          if (resource.getType() != IResource.FILE) {
            switch (delta.getKind()) {
              case IResourceDelta.ADDED:
                server.scan(file, null);
                return false;
              case IResourceDelta.REMOVED:
                server.discard(file);
                return false;
              case IResourceDelta.CHANGED:
                // recurse child deltas
                return true;
            }
            return false;
          }

          // Process file
          if (resource.getName().endsWith(Extensions.DOT_DART)) {
            switch (delta.getKind()) {
              case IResourceDelta.ADDED:
                server.scan(file, null);
                return false;
              case IResourceDelta.REMOVED:
                server.discard(file);
                return false;
              case IResourceDelta.CHANGED:
                server.changed(file);
                return false;
            }
            return false;
          }

          return false;
        }
      });
    }

    return null;
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    //notify participants
    for (final DartBuildParticipant participant : getBuildParticipants()) {
      SafeRunner.run(new ISafeRunnable() {
        @Override
        public void handleException(Throwable exception) {
          DartCore.logError("Error notifying build participant", exception);
        }

        @Override
        public void run() throws Exception {
          participant.clean(getProject(), new NullProgressMonitor());
        }
      });
    }

    DartBasedBuilder.getBuilder().handleClean(getProject(), new NullProgressMonitor());

    AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();
    server.reanalyze();
  }

}

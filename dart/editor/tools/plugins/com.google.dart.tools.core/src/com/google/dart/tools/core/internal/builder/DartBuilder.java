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
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.Extensions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubMonitor;

import java.io.File;
import java.util.Map;

/**
 * Instances of the class <code>DartBuilder</code> implement the incremental builder for Dart
 * projects and contained pub packages.
 */
public class DartBuilder extends IncrementalProjectBuilder {

  private final AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();

  /**
   * The participants associated with this builder or {@code null} if not initialized. This field is
   * lazily initialized by calling {@link #getParticipants()}.
   */
  private BuildParticipant[] participants;

  /**
   * Flag indicating whether {@link #clean(IProgressMonitor)} was called
   */
  private boolean cleaned;

  @Override
  protected IProject[] build(final int kind, final Map<String, String> args,
      final IProgressMonitor monitor) throws CoreException {

    int totalProgress = (getParticipants().length + 1) * 10;
    final SubMonitor subMonitor = SubMonitor.convert(monitor, totalProgress);

    final IResourceDelta delta = cleaned ? null : getDelta(getProject());
    final BuildEvent event = new BuildEvent(getProject(), delta, subMonitor);
    cleaned = false;

    // notify participants
    for (final BuildParticipant participant : getParticipants()) {
      if (subMonitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      SafeRunner.run(new ISafeRunnable() {
        @Override
        public void handleException(Throwable exception) {
          if (!(exception instanceof OperationCanceledException)) {
            DartCore.logError("Error notifying build participant", exception);
          }
        }

        @Override
        public void run() throws Exception {
          participant.build(event, subMonitor.newChild(10));
        }
      });
    }

    if (subMonitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    // If delta is null, then building a new project

    if (delta == null) {
      IPath location = getProject().getLocation();
      if (location != null) {
        ScanCallbackProvider provider = ScanCallbackProvider.getProvider(getProject().getName());
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
          } else {
            try {
              resource.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_ZERO);
            } catch (CoreException e) {
              // do nothing
            }
          }

          return false;
        }
      });
    }

    return null;
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    cleaned = true;
    final SubMonitor subMonitor = SubMonitor.convert(monitor, getParticipants().length + 3);
    final CleanEvent event = new CleanEvent(getProject(), subMonitor);
    try {

      //notify participants
      subMonitor.beginTask(getProject().getName(), getParticipants().length + 3);
      for (final BuildParticipant participant : getParticipants()) {
        SafeRunner.run(new ISafeRunnable() {
          @Override
          public void handleException(Throwable exception) {
            if (!(exception instanceof OperationCanceledException)) {
              DartCore.logError("Error notifying build participant", exception);
            }
          }

          @Override
          public void run() throws Exception {
            participant.clean(event, subMonitor.newChild(1));
          }
        });
      }

      // Clear the index before triggering reanalyze so that updates from re-analysis
      // will be included in the rebuilt index
      InMemoryIndex.getInstance().clear();
      subMonitor.worked(1);

      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot root = workspace.getRoot();

      root.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);

      AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();
      server.reanalyze();

    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  /**
   * Lazily initialize and answer the build participants for the this project.
   */
  private BuildParticipant[] getParticipants() {
    if (participants == null) {
      participants = BuildParticipantDeclaration.participantsFor(getProject());
    }
    return participants;
  }
}

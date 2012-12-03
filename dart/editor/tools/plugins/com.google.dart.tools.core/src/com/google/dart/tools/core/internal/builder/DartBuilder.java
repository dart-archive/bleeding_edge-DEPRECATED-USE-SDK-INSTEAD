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
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.CleanEvent;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubMonitor;

import java.util.Map;

/**
 * Instances of the class <code>DartBuilder</code> implement the incremental builder for Dart
 * projects and contained pub packages.
 */
public class DartBuilder extends IncrementalProjectBuilder {

  /**
   * The participants associated with this builder or {@code null} if not initialized. This field is
   * lazily initialized by calling {@link #getParticipants()}.
   */
  private BuildParticipant[] participants;

  /**
   * Flag indicating whether {@link #clean(IProgressMonitor)} was called
   */
  private boolean cleaned;

  public DartBuilder() {
  }

  public DartBuilder(BuildParticipant participant) {
    participants = new BuildParticipant[] {participant};
  }

  @Override
  public IProject[] build(final int kind, final Map<String, String> args,
      final IProgressMonitor monitor) throws CoreException {
    return build(getProject(), kind, args, monitor);
  }

  public IProject[] build(IProject project, int kind, Map<String, String> args,
      final IProgressMonitor monitor) {

    final SubMonitor subMonitor = SubMonitor.convert(
        monitor,
        project.getName(),
        getParticipants().length);

    final IResourceDelta delta = cleaned ? null : getDelta(project);
    final BuildEvent event = new BuildEvent(project, delta, subMonitor);
    cleaned = false;

    try {
      for (final BuildParticipant participant : getParticipants()) {

        // Check if the operation has been canceled
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
            SubMonitor childMonitor = subMonitor.newChild(1);
            participant.build(event, childMonitor);
            childMonitor.done();
          }
        });
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
    return null;
  }

  @Override
  public void clean(IProgressMonitor monitor) throws CoreException {
    clean(getProject(), monitor);
  }

  public void clean(IProject project, IProgressMonitor monitor) {
    final SubMonitor subMonitor = SubMonitor.convert(
        monitor,
        project.getName(),
        getParticipants().length);

    final CleanEvent event = new CleanEvent(project, subMonitor);
    cleaned = true;

    try {
      for (final BuildParticipant participant : getParticipants()) {

        // Check if the operation has been canceled
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
            SubMonitor childMonitor = subMonitor.newChild(1);
            participant.clean(event, childMonitor);
            childMonitor.done();
          }
        });
      }
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

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
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.CleanEvent;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import java.util.Map;

/**
 * Instances of the class <code>DartBuilder</code> implement the incremental builder for Dart
 * projects and contained pub packages.
 * 
 * @coverage dart.tools.core.builder
 */
public class DartBuilder extends IncrementalProjectBuilder {

  private static abstract class ParticipantRunner {
    public abstract void run(IProgressMonitor monitor) throws CoreException;
  }

  /**
   * The participants associated with this builder or {@code null} if not initialized. This field is
   * lazily initialized by calling {@link #getParticipants()}.
   */
  private BuildParticipant[] participants;

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

    final IResourceDelta delta = getDelta(project);
    final BuildEvent event = new BuildEvent(project, delta, subMonitor);

    try {
      for (final BuildParticipant participant : getParticipants()) {
        safeRun(new ParticipantRunner() {
          @Override
          public void run(IProgressMonitor monitor) throws CoreException {
            // TODO (danrubel) Remove the BuildParticipant entry in the plugin.xml
            // once analysis server is the norm.
            if (DartCoreDebug.ENABLE_ANALYSIS_SERVER
                && participant instanceof AnalysisEngineParticipant) {
              return;
            }
            participant.build(event, monitor);
          }
        }, subMonitor);
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

    try {
      for (final BuildParticipant participant : getParticipants()) {
        safeRun(new ParticipantRunner() {
          @Override
          public void run(IProgressMonitor monitor) throws CoreException {
            participant.clean(event, monitor);
          }
        }, subMonitor);
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

  /**
   * Safely execute the specified runner, logging any exceptions that occur
   * 
   * @param runner the runner (not {@code null})
   * @param subMonitor the monitor (not {@code null})
   */
  private void safeRun(ParticipantRunner runner, SubMonitor subMonitor) {
    // Check if the operation has been canceled
    if (subMonitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    try {
      SubMonitor childMonitor = subMonitor.newChild(1);
      runner.run(childMonitor);
      childMonitor.done();
    } catch (OperationCanceledException e) {
      throw e;
    } catch (Exception e) {
      DartCore.logError("Error notifying build participant", e);
    } catch (LinkageError e) {
      DartCore.logError("Error notifying build participant", e);
    } catch (AssertionError e) {
      DartCore.logError("Error notifying build participant", e);
    }
  }
}

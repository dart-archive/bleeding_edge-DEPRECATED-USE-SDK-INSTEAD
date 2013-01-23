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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl;

import static com.google.dart.tools.core.DartCore.DART_PROBLEM_MARKER_TYPE;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import static org.eclipse.core.resources.IResource.DEPTH_INFINITE;

/**
 * Performs source analysis using instances of {@link AnalysisContext}.
 * {@link AnalysisServerParticipant} should be disabled when this participant is enabled.
 * 
 * @see DartCoreDebug#ENABLE_NEW_ANALYSIS
 */
public class AnalysisEngineParticipant implements BuildParticipant {

  private final boolean enabled;

  /**
   * The project being analyzed by this build participant or {@code null} if it has not been
   * initialized yet.
   */
  private Project project;

  public AnalysisEngineParticipant() {
    this(DartCoreDebug.ENABLE_NEW_ANALYSIS);
  }

  public AnalysisEngineParticipant(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Traverse and analyze resources
   */
  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {

    // This participant and AnalysisServerParticipant are mutually exclusive
    if (!enabled || monitor.isCanceled()) {
      return;
    }

    // Traverse resources specified by the build event
    event.traverse(new BuildVisitor() {
      @Override
      public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
        IProject resource = (IProject) delta.getResource();

        // Do not mark any resources changed when initializing
        init(resource, false);

        if (monitor.isCanceled()) {
          return false;
        }

        // Update the project
        DeltaProcessor processor = createProcessor(project);
        processor.addDeltaListener(new ProjectUpdater(true));
        processor.traverse(delta);

        // Parse changed files
        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        processor = createProcessor(project);
        processor.addDeltaListener(analyzer);
        processor.traverse(delta);
        analyzer.updateMarkers();

        return false;
      }

      @Override
      public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
        IProject resource = (IProject) proxy.requestResource();
        boolean traverse = project != null;

        // Always mark all resources changed when initializing
        init(resource, true);

        if (monitor.isCanceled()) {
          return false;
        }

        // Only update the project if not already traversed during initialization
        if (traverse) {
          DeltaProcessor processor = createProcessor(project);
          processor.addDeltaListener(new ProjectUpdater(true));
          processor.traverse(resource);
        }

        // Parse changed files
        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        DeltaProcessor processor = createProcessor(project);
        processor.addDeltaListener(analyzer);
        processor.traverse(resource);
        analyzer.updateMarkers();

        return false;
      }
    }, false);

    if (monitor.isCanceled()) {
      return;
    }

    // TODO (danrubel): resolve all available sources
  }

  /**
   * Discard all markers and cached analysis
   */
  @Override
  public void clean(CleanEvent event, IProgressMonitor monitor) throws CoreException {

    // This participant and AnalysisServerParticipant are mutually exclusive
    if (!enabled || monitor.isCanceled()) {
      return;
    }

    if (project != null) {
      project.discardContextsIn(event.getProject());
      project = null;
    }
    event.getProject().deleteMarkers(DART_PROBLEM_MARKER_TYPE, true, DEPTH_INFINITE);
  }

  /**
   * Initialize the delta processor associated with this builder. Overridden when testing this
   * class.
   * 
   * @param project the project for which the processor is created (not {@code null})
   * @return the delta processor (not {@code null})
   */
  protected DeltaProcessor createProcessor(Project project) {
    DeltaProcessor processor = new DeltaProcessor(project);
    return processor;
  }

  /**
   * Initialize the Dart {@link Project} for the given {@link IProject} resource. Overridden when
   * testing this class.
   * 
   * @param resource the project resource (not {@code null})
   * @return the dart project
   */
  protected Project createProject(IProject resource) {
    return new ProjectImpl(resource);
  }

  /**
   * Create a project and processor if one does not already exist then traverse the project to build
   * the collection of available sources
   * 
   * @param resource the project resource (not {@code null})
   * @param notifyChanged {@code true} if the context(s) should be notified via
   *          {@link AnalysisContext#sourceChanged(Source)} that every source in the project has
   *          been modified and thus should be analyzed.
   */
  private void init(IProject resource, boolean notifyChanged) throws CoreException {
    if (project == null) {
      project = createProject(resource);
      DeltaProcessor processor = createProcessor(project);
      processor.addDeltaListener(new ProjectUpdater(notifyChanged));
      processor.traverse(resource);
    }
  }
}

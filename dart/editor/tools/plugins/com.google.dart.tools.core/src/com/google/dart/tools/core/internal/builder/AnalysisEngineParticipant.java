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
import com.google.dart.engine.index.Index;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

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

  /**
   * The object (not {@code null}) used to manage which resources should be not be analyzed.
   */
  private final DartIgnoreManager ignoreManager;

  /**
   * The index (not {@code null}) to be updated.
   */
  private final Index index;

  public AnalysisEngineParticipant() {
    this(
        DartCoreDebug.ENABLE_NEW_ANALYSIS,
        DartIgnoreManager.getInstance(),
        DartCore.getProjectManager().getIndex());
  }

  public AnalysisEngineParticipant(boolean enabled, DartIgnoreManager ignoreManager, Index index) {
    if (ignoreManager == null | index == null) {
      throw new IllegalArgumentException();
    }
    this.enabled = enabled;
    this.ignoreManager = ignoreManager;
    this.index = index;
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

        if (project == null) {
          project = createProject(resource);
        }

        if (monitor.isCanceled()) {
          return false;
        }

        // Update the project
        DeltaProcessor processor = createProcessor(project);
        processor.addDeltaListener(new ProjectUpdater());
        processor.traverse(delta);

        if (monitor.isCanceled()) {
          return false;
        }

        // Parse changed files
        ProjectAnalyzer analyzer = new ProjectAnalyzer(ignoreManager, index);
        processor = createProcessor(project);
        processor.addDeltaListener(analyzer);
        processor.traverse(delta);
        analyzer.analyze(monitor);

        return false;
      }

      @Override
      public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
        IProject resource = (IProject) proxy.requestResource();

        if (project == null) {
          project = createProject(resource);
        }

        if (monitor.isCanceled()) {
          return false;
        }

        // Update the project
        DeltaProcessor processor = createProcessor(project);
        processor.addDeltaListener(new ProjectUpdater());
        processor.traverse(resource);

        if (monitor.isCanceled()) {
          return false;
        }

        // Parse changed files
        ProjectAnalyzer analyzer = new ProjectAnalyzer(ignoreManager, index);
        processor = createProcessor(project);
        processor.addDeltaListener(analyzer);
        processor.traverse(resource);
        analyzer.analyze(monitor);

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
}

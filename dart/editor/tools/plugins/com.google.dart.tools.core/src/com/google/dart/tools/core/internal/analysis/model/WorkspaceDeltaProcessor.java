package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.core.internal.builder.DeltaProcessor;
import com.google.dart.tools.core.internal.builder.IndexUpdater;
import com.google.dart.tools.core.internal.builder.ProjectUpdater;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * The {@code WorkspaceDeltaProcessor} receives resource changes from Eclipse and updates the
 * associated {@link ProjectManager} based on those changes. In addition, it traverses changes in
 * the "packages" directory hierarchy because the builder does not receive resources changes for
 * symlinked folders (e.g. packages).
 * 
 * @coverage dart.tools.core.model
 */
public class WorkspaceDeltaProcessor implements IResourceChangeListener {
  /**
   * The associated project manager updated by the receiver (not {@code null})
   */
  private final ProjectManager manager;

  public WorkspaceDeltaProcessor(ProjectManager manager) {
    this.manager = manager;
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    IResourceDelta delta = event.getDelta();
    if (delta != null) {
      try {
        delta.accept(new IResourceDeltaVisitor() {
          @Override
          public boolean visit(IResourceDelta delta) throws CoreException {

            IResource res = delta.getResource();
            if (res == null) {
              return false;

            } else if (res.getType() == IResource.ROOT) {
              return true;

            } else if (res.getType() == IResource.PROJECT) {
              if (delta.getKind() == IResourceDelta.REMOVED) {
                manager.projectRemoved((IProject) res);
                return false;
              }
              return true;

            } else if (res.getType() == IResource.FOLDER) {
              if (res.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME)) {

                // The builder is not notified about changes in symlinked folders (e.g. packages)
                // thus we traverse those changes here using the same mechanism as the builder
                Project project = manager.getProject(res.getProject());
                ProjectUpdater updater = new ProjectUpdater();
                IndexUpdater indexUpdater = new IndexUpdater(manager.getIndex());
                DeltaProcessor processor = new DeltaProcessor(project);
                processor.addDeltaListener(updater);
                processor.addDeltaListener(indexUpdater);
                processor.traverse(delta);
                updater.applyChanges();
                AnalysisContext context = manager.getContext(res);
                startBackgroundAnalysis(project, context);
                return false;
              }
              return true;

            } else {
              return false;
            }
          }
        });
      } catch (CoreException e) {
        DartCore.logError(e);
      }
    }
  }

  /**
   * Kick off a background analysis worker for the given context.
   * 
   * @param project the project (not {@code null}) containing the context
   * @param context the context to be analyzed (not {@code null})
   */
  protected void startBackgroundAnalysis(Project project, AnalysisContext context) {
    new AnalysisWorker(project, context).performAnalysisInBackground();
  }
}

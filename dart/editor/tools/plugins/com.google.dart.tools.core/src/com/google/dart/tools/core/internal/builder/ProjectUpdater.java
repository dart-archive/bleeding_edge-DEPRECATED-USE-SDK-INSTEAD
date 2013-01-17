/*
 * Copyright 2013 Dart project authors.
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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.IPath;

import static org.eclipse.core.resources.IResource.PROJECT;

/**
 * {@link DeltaProcessor} listener for updating a {@link Project} and its contained
 * {@link AnalysisContext}s.
 */
public class ProjectUpdater {

  private final Project project;
  private boolean notifyChanged;
  private AnalysisContext context;

  /**
   * Construct a new instance for updating the specified project
   * 
   * @param project the project to be updated (not {@code null})
   * @param notifyChanged {@code true} if the context(s) being updated should be modified of changed
   *          sources via {@link AnalysisContext#sourceChanged(Source)}, or {@code false} if not.
   */
  public ProjectUpdater(Project project, boolean notifyChanged) {
    this.project = project;
    this.notifyChanged = notifyChanged;
  }

  public void containerRemoved(IContainer container) {
    if (container.getType() != PROJECT) {
      context = project.getContext(container);
      AnalysisContext parentContext = project.getContext(container.getParent());
      // If the container is part of a larger context (context == parentContext)
      // then remove the contained sources from the larger context
      if (context == parentContext) {
        sourcesDeleted(container);
      }
    }
    project.discardContextsIn(container);
  }

  public void packageSource(IResourceProxy proxy) {
    if (!notifyChanged) {
      return;
    }
    IFile resource = (IFile) proxy.requestResource();
    IPath location = resource.getLocation();
    if (location == null) {
      logNoLocation(resource);
      return;
    }
    Source source = context.getSourceFactory().forFile(location.toFile());
    context.sourceChanged(source);
  }

  public void packageSourceAdded(IFile file) {
    packageSourceChanged(file);
  }

  public void packageSourceChanged(IFile file) {
    IPath location = file.getLocation();
    if (!notifyChanged) {
      return;
    }
    if (location == null) {
      logNoLocation(file);
      return;
    }
    Source source = context.getSourceFactory().forFile(location.toFile());
    context.sourceChanged(source);
  }

  public void packageSourceRemoved(IFile resource) {
    IPath location = resource.getLocation();
    Source source = null;
    if (location != null) {
      source = context.getSourceFactory().forFile(location.toFile());
    } else {
      logNoLocation(resource);
    }
    sourceRemoved(resource, source);
  }

  public void packageSourcesRemoved(IContainer container) {
    sourcesDeleted(container);
  }

  public void pubspec(IResourceProxy proxy) {
    // Notify project that pubspec exists.
    // Pubspec changes will be processed by pubspec build participant
    // and result in a "packages" resource delta.
    project.pubspecAdded(proxy.requestResource().getParent());
  }

  public void pubspecAdded(IFile resource) {
    // Notify project when pubspec is added.
    // Pubspec changes will be processed by pubspec build participant
    // and result in a "packages" resource delta.
    project.pubspecAdded(resource.getParent());
  }

  public void pubspecChanged(IFile resource) {
    // ignored
  }

  public void pubspecRemoved(IFile resource) {
    project.pubspecRemoved(resource.getParent());
  }

  public void setNotifyChanged(boolean notifyChanged) {
    this.notifyChanged = notifyChanged;
  }

  /**
   * Called when a source file has been added
   * 
   * @param resource the file that was added (not {@code null})
   * @param source the source that was added (not {@code null})
   */
  public void sourceAdded(IFile resource, Source source) {
    context.sourceAvailable(source);
    sourceChanged(resource, source);
  }

  /**
   * Called when a source file has changed
   * 
   * @param resource the file that changed (not {@code null})
   * @param source the source that changed (not {@code null})
   */
  public void sourceChanged(IFile resource, Source source) {
    if (notifyChanged) {
      context.sourceChanged(source);
    }
  }

  /**
   * Called when a source file has been removed
   * 
   * @param resource the file that was removed (not {@code null})
   * @param source the source that was removed (may be {@code null} if the location of the source
   *          cannot be determined)
   */
  public void sourceRemoved(IFile resource, Source source) {
    if (source != null) {
      context.sourceDeleted(source);
    }
  }

  public void visitContext(IContainer container, AnalysisContext context) {
    this.context = context;
  }

  private void logNoLocation(IResource resource) {
    DartCore.logInformation("No location for " + resource);
  }

  private void sourcesDeleted(IContainer resource) {
    IPath location = resource.getLocation();
    if (location == null) {
      logNoLocation(resource);
      return;
    }
    SourceContainer container = context.getSourceFactory().forDirectory(location.toFile());
    context.sourcesDeleted(container);
  }
}

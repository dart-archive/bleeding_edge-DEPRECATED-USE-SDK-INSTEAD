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
import com.google.dart.tools.core.analysis.model.Project;

import org.eclipse.core.resources.IContainer;

/**
 * {@link DeltaProcessor} listener for updating a {@link Project} and its contained
 * {@link AnalysisContext}s.
 */
public class ProjectUpdater implements DeltaListener {

  private boolean notifyChanged;

  /**
   * Construct a new instance for updating the specified project
   * 
   * @param notifyChanged {@code true} if the context(s) being updated should be modified of changed
   *          sources via {@link AnalysisContext#sourceChanged(Source)}, or {@code false} if not.
   */
  public ProjectUpdater(boolean notifyChanged) {
    this.notifyChanged = notifyChanged;
  }

  @Override
  public void packageSourceAdded(SourceDeltaEvent event) {
    packageSourceChanged(event);
  }

  @Override
  public void packageSourceChanged(SourceDeltaEvent event) {
    if (notifyChanged) {
      Source source = event.getSource();
      if (source != null) {
        event.getContext().sourceChanged(source);
      }
    }
  }

  @Override
  public void packageSourceContainerRemoved(SourceContainerDeltaEvent event) {
    sourcesDeleted(event);
  }

  @Override
  public void packageSourceRemoved(SourceDeltaEvent event) {
    sourceRemoved(event);
  }

  @Override
  public void pubspecAdded(ResourceDeltaEvent event) {
    // Notify project when pubspec is added.
    // Pubspec changes will be processed by pubspec build participant
    // and result in a "packages" resource delta.
    event.getProject().pubspecAdded(event.getResource().getParent());
  }

  @Override
  public void pubspecChanged(ResourceDeltaEvent event) {
    // ignored
  }

  @Override
  public void pubspecRemoved(ResourceDeltaEvent event) {
    event.getProject().pubspecRemoved(event.getResource().getParent());
  }

  @Override
  public void sourceAdded(SourceDeltaEvent event) {
    Source source = event.getSource();
    if (source != null) {
      sourceChanged(event);
    }
  }

  @Override
  public void sourceChanged(SourceDeltaEvent event) {
    if (notifyChanged) {
      Source source = event.getSource();
      if (source != null) {
        event.getContext().sourceChanged(source);
      }
    }
  }

  @Override
  public void sourceContainerRemoved(SourceContainerDeltaEvent event) {
    // If the container is part of a larger context (context == parentContext)
    // then remove the contained sources from the larger context
    if (!event.isTopContainerInContext()) {
      sourcesDeleted(event);
    }
    event.getProject().discardContextsIn((IContainer) event.getResource());
  }

  @Override
  public void sourceRemoved(SourceDeltaEvent event) {
    Source source = event.getSource();
    if (source != null) {
      event.getContext().sourceDeleted(source);
    }
  }

  private void sourcesDeleted(SourceContainerDeltaEvent event) {
    SourceContainer container = event.getSourceContainer();
    if (container != null) {
      event.getContext().sourcesDeleted(container);
    }
  }
}

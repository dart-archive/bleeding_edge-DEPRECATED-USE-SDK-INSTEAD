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
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;

import org.eclipse.core.resources.IContainer;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Instances of {@code ProjectUpdater} are used to update instances of {@link Project}. To update a
 * project, add an instance of {@code ProjectUpdater} as a listener via
 * {@link DeltaProcessor#addDeltaListener(DeltaListener)}, use
 * {@link DeltaProcessor#traverse(IContainer)} or
 * {@link DeltaProcessor#traverse(org.eclipse.core.resources.IResourceDelta)} to traverse the
 * changes, then call {@link #applyChanges()}.
 */
public class ProjectUpdater implements DeltaListener {
  private HashMap<AnalysisContext, ChangeSet> contextChangeMap = new HashMap<AnalysisContext, ChangeSet>();
  private ChangeSet currentChanges;

  /**
   * Apply change sets to the associated contexts.
   */
  public void applyChanges() {
    for (Entry<AnalysisContext, ChangeSet> entry : contextChangeMap.entrySet()) {
      AnalysisContext context = entry.getKey();
      ChangeSet changeSet = entry.getValue();
      if (!changeSet.isEmpty()) {
        context.applyChanges(changeSet);
      }
    }
  }

  @Override
  public void packageSourceAdded(SourceDeltaEvent event) {
    currentChanges.added(event.getSource());
  }

  @Override
  public void packageSourceChanged(SourceDeltaEvent event) {
    currentChanges.changed(event.getSource());
  }

  @Override
  public void packageSourceContainerRemoved(SourceContainerDeltaEvent event) {
    currentChanges.removedContainer(event.getSourceContainer());
  }

  @Override
  public void packageSourceRemoved(SourceDeltaEvent event) {
    currentChanges.removed(event.getSource());
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
    if (DartCore.isAnalyzed(event.getResource())) {
      currentChanges.added(event.getSource());
    }
  }

  @Override
  public void sourceChanged(SourceDeltaEvent event) {
    if (DartCore.isAnalyzed(event.getResource())) {
      currentChanges.changed(event.getSource());
    }
  }

  @Override
  public void sourceContainerRemoved(SourceContainerDeltaEvent event) {
    // If the container is part of a larger context (context == parentContext)
    // then remove the contained sources from the larger context
    if (!event.isTopContainerInContext()) {
      currentChanges.removedContainer(event.getSourceContainer());
    }
    event.getProject().discardContextsIn((IContainer) event.getResource());
  }

  @Override
  public void sourceRemoved(SourceDeltaEvent event) {
    currentChanges.removed(event.getSource());
  }

  @Override
  public void visitContext(ResourceDeltaEvent event) {
    AnalysisContext context = event.getContext();
    currentChanges = contextChangeMap.get(context);
    if (currentChanges == null) {
      currentChanges = new ChangeSet();
      contextChangeMap.put(context, currentChanges);
    }
  }
}

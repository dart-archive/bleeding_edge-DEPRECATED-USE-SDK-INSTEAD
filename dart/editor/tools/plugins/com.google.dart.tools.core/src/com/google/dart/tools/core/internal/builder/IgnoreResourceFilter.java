/*
 * Copyright (c) 2014, the Dart project authors.
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
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.resources.IResource;

/**
 * A delta listener that filters out sources specified by {@link DartIgnoreManager}, clears markers
 * from those filtered sources, and broadcasts any remaining changes to its own listeners. For
 * performance, this filter caches information about what is ignored and as such should be used once
 * and discarded.
 */
public class IgnoreResourceFilter extends DeltaBroadcaster implements DeltaListener {

  //TODO (danrubel): Optimizations:
  // Don't traverse containers that are ignored
  // Optimize case where nothing in container is ignored

  private final DartIgnoreManager ignoreManager;
  private AnalysisMarkerManager markerManager;
  private boolean hasIgnores;

  public IgnoreResourceFilter() {
    this(DartCore.getIgnoreManager(), AnalysisMarkerManager.getInstance());
  }

  public IgnoreResourceFilter(DartIgnoreManager ignoreManager, AnalysisMarkerManager markerManager) {
    this.ignoreManager = ignoreManager;
    this.markerManager = markerManager;
    this.hasIgnores = ignoreManager.getExclusionPatterns().size() > 0;
  }

  @Override
  public void packageSourceAdded(SourceDeltaEvent event) {
    if (shouldForward(event)) {
      listener.packageSourceAdded(event);
    } else {
      IResource resource = event.getResource();
      if (resource != null) {
        markerManager.clearMarkers(resource);
      }
    }
  }

  @Override
  public void packageSourceChanged(SourceDeltaEvent event) {
    if (shouldForward(event)) {
      listener.packageSourceChanged(event);
    }
  }

  @Override
  public void packageSourceContainerRemoved(SourceContainerDeltaEvent event) {
    if (shouldForward(event)) {
      listener.packageSourceContainerRemoved(event);
    }
  }

  @Override
  public void packageSourceRemoved(SourceDeltaEvent event) {
    if (shouldForward(event)) {
      listener.packageSourceRemoved(event);
    }
  }

  @Override
  public void pubspecAdded(ResourceDeltaEvent event) {
    if (shouldForward(event)) {
      listener.pubspecAdded(event);
    }
  }

  @Override
  public void pubspecChanged(ResourceDeltaEvent event) {
    if (shouldForward(event)) {
      listener.pubspecChanged(event);
    }
  }

  @Override
  public void pubspecRemoved(ResourceDeltaEvent event) {
    if (shouldForward(event)) {
      listener.pubspecRemoved(event);
    }
  }

  @Override
  public void sourceAdded(SourceDeltaEvent event) {
    if (shouldForward(event)) {
      listener.sourceAdded(event);
    } else {
      IResource resource = event.getResource();
      if (resource != null) {
        markerManager.clearMarkers(resource);
      }
    }
  }

  @Override
  public void sourceChanged(SourceDeltaEvent event) {
    if (shouldForward(event)) {
      listener.sourceChanged(event);
    }
  }

  @Override
  public void sourceContainerRemoved(SourceContainerDeltaEvent event) {
    if (shouldForward(event)) {
      listener.sourceContainerRemoved(event);
    }
  }

  @Override
  public void sourceRemoved(SourceDeltaEvent event) {
    if (shouldForward(event)) {
      listener.sourceRemoved(event);
    }
  }

  @Override
  public void visitContext(ResourceDeltaEvent event) {
    listener.visitContext(event);
  }

  /**
   * Determine if the specified event should be forwarded to listeners.
   * 
   * @param event the event, not {@code null}
   * @return {@code true} if the event should be forwarded
   */
  private boolean shouldForward(ResourceDeltaEvent event) {
    if (hasIgnores) {
      IResource res = event.getResource();
      if (res == null || ignoreManager.isIgnored(res.getLocation())) {
        return false;
      }
    }
    return true;
  }
}

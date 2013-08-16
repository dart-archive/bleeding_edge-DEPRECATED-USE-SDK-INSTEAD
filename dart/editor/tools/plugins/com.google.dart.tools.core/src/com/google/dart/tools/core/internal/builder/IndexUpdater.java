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

import com.google.dart.engine.index.Index;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResourceDelta;

/**
 * Instances of {@code IndexUpdater} are used to remove {@link Source}s from {@link Index}. To
 * update an index, add an instance of {@code IndexUpdater} as a listener via
 * {@link DeltaProcessor#addDeltaListener(DeltaListener)}, use
 * {@link DeltaProcessor#traverse(IContainer)} or {@link DeltaProcessor#traverse(IResourceDelta)} to
 * traverse the changes.
 * 
 * @coverage dart.tools.core.builder
 */
public class IndexUpdater extends DeltaAdapter {
  private final Index index;

  public IndexUpdater(Index index) {
    this.index = index;
  }

  @Override
  public void sourceContainerRemoved(SourceContainerDeltaEvent event) {
    SourceContainer sourceContainer = event.getSourceContainer();
    if (sourceContainer == null) {
      return;
    }
    index.removeSources(event.getContext(), sourceContainer);
  }

  @Override
  public void sourceRemoved(SourceDeltaEvent event) {
    Source source = event.getSource();
    if (source == null) {
      return;
    }
    index.removeSource(event.getContext(), source);
  }
}

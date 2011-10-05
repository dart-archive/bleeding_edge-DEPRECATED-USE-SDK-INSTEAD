/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.workspace.driver;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

final class SavedDeltasProcessor implements IResourceChangeListener {

  private boolean called = false;

  private final DeltaProcessor deltaProcessor;

  public SavedDeltasProcessor(IndexConfigurationInstance configuration) {
    deltaProcessor = new DeltaProcessor(configuration);
  }

  public IFile[] getModifiedFiles() {
    return deltaProcessor.getModifiedFiles();
  }

  public boolean hasBeenCalled() {
    return called;
  }

  public boolean isResyncRequired() {
    return deltaProcessor.isResyncRequired();
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    called = true;
    IResourceDelta delta = event.getDelta();
    try {
      delta.accept(deltaProcessor);
    } catch (CoreException e) {
      // cannot happen (only propagates from the visitor)
      IndexerPlugin.getLogger().logError(e);
    }
  }

}

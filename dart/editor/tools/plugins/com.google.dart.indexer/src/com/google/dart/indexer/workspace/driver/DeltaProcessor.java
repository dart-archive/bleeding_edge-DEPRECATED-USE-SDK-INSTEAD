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
import com.google.dart.indexer.utils.ResourceDeltaSwitch;
import com.google.dart.indexer.workspace.index.WorkspaceFilesCollector;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.Collection;

final class DeltaProcessor extends ResourceDeltaSwitch {

  private boolean resyncRequired = false;

  private final Collection<IFile> modifiedFiles = new ArrayList<IFile>();

  private final IndexConfigurationInstance configuration;

  public DeltaProcessor(IndexConfigurationInstance configuration) {
    this.configuration = configuration;
  }

  public IFile[] getModifiedFiles() {
    return modifiedFiles.toArray(new IFile[modifiedFiles.size()]);
  }

  public boolean isResyncRequired() {
    return resyncRequired;
  }

  @Override
  protected boolean visitAddedContainer(IContainer container) {
    try {
      // closed projects cannot be visited
      if (!container.isAccessible()) {
        return false;
      }
      container.accept(new WorkspaceFilesCollector(configuration, modifiedFiles));
    } catch (CoreException e) {
      IndexerPlugin.getLogger().logError(e);
    }
    return true;
  }

  @Override
  protected void visitAddedFile(IFile file) {
    modifiedFiles.add(file);
  }

  @Override
  protected void visitChangedFile(IFile file, IResourceDelta delta) {
    if ((delta.getFlags() & (IResourceDelta.CONTENT | IResourceDelta.ENCODING)) != 0) {
      modifiedFiles.add(file);
    }
  }

  @Override
  protected boolean visitRemovedContainer(IContainer container) {
    resyncRequired = true;
    return false;
  }

  @Override
  protected void visitRemovedFile(IFile file) {
    modifiedFiles.add(file);
  }
}

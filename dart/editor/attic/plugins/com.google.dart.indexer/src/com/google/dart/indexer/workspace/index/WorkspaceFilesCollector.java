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
package com.google.dart.indexer.workspace.index;

import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.utils.ResourceSwitch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;

import java.util.Collection;

public final class WorkspaceFilesCollector extends ResourceSwitch {
  private final Collection<IFile> existingFiles;
  private final IndexConfigurationInstance configuration;

  public WorkspaceFilesCollector(IndexConfigurationInstance configuration,
      Collection<IFile> destination) {
    this.configuration = configuration;
    this.existingFiles = destination;
  }

  @Override
  protected boolean visitFile(IFile file) {
    if (configuration.isIndexedFile(file)) {
      existingFiles.add(file);
    }
    return false;
  }

  @Override
  protected boolean visitFolder(IFolder resource) {
    return true;
  }

  @Override
  protected boolean visitProject(IProject resource) {
    return true;
  }

  @Override
  protected boolean visitRoot(IWorkspaceRoot resource) {
    return true;
  }
}

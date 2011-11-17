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

import com.google.dart.indexer.utils.ResourceDeltaSwitch;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;

import java.util.ArrayList;
import java.util.Collection;

final class DeltaProcessor extends ResourceDeltaSwitch {
  private final Collection<IFile> removedFiles = new ArrayList<IFile>();

  public DeltaProcessor() {
    super();
  }

  public IFile[] getRemovedFiles() {
    return removedFiles.toArray(new IFile[removedFiles.size()]);
  }

  @Override
  protected boolean visitAddedContainer(IContainer container) {
    return true;
  }

  @Override
  protected void visitAddedFile(IFile file) {
  }

  @Override
  protected void visitChangedFile(IFile file, IResourceDelta delta) {
  }

  @Override
  protected boolean visitRemovedContainer(IContainer container) {
    return false;
  }

  @Override
  protected void visitRemovedFile(IFile file) {
    removedFiles.add(file);
  }
}

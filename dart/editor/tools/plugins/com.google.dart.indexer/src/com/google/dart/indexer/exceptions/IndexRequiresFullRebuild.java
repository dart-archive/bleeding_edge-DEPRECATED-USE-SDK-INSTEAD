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
package com.google.dart.indexer.exceptions;

import com.google.dart.indexer.source.IndexableSource;

import org.eclipse.core.resources.IFile;

public class IndexRequiresFullRebuild extends IndexRequestFailed {
  private static final long serialVersionUID = 1L;
  @Deprecated
  private IFile[] filesToIndex;
  private IndexableSource[] sourcesToIndex;
  private boolean reportAsError = true;

  public IndexRequiresFullRebuild() {
    super();
  }

  public IndexRequiresFullRebuild(String message) {
    super(message);
  }

  public IndexRequiresFullRebuild(String message, boolean reportAsError) {
    super(message);
    this.reportAsError = reportAsError;
  }

  public IndexRequiresFullRebuild(String message, Throwable cause) {
    super(message, cause);
  }

  public IndexRequiresFullRebuild(Throwable cause) {
    super(cause);
  }

  @Deprecated
  public IndexRequiresFullRebuild(Throwable cause, IFile[] filesToIndex) {
    super(cause);
    this.filesToIndex = filesToIndex;
  }

  public IndexRequiresFullRebuild(Throwable cause, IndexableSource[] sourcesToIndex) {
    super(cause);
    this.sourcesToIndex = sourcesToIndex;
  }

  @Deprecated
  public IFile[] getFilesToIndex() {
    return filesToIndex;
  }

  public IndexableSource[] getSourcesToIndex() {
    return sourcesToIndex;
  }

  public boolean shouldReportAsError() {
    return reportAsError;
  }
}

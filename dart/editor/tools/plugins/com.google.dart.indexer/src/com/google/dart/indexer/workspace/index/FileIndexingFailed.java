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

import org.eclipse.core.resources.IFile;

public class FileIndexingFailed extends Exception {

  private static final long serialVersionUID = 1L;

  // private final IFile file;
  // private final boolean isRetry;

  private static String formatMessage(IFile file, boolean isRetry) {
    return "Failed to index file (" + (isRetry ? "retry" : "first attempt") + "): " + file;
  }

  public FileIndexingFailed(IFile file, Error cause, boolean isRetry) {
    super(formatMessage(file, isRetry), cause);
    // this.file = file;
    // this.isRetry = isRetry;
  }

  public FileIndexingFailed(IFile file, RuntimeException cause, boolean isRetry) {
    super(formatMessage(file, isRetry), cause);
    // this.file = file;
    // this.isRetry = isRetry;
  }

}

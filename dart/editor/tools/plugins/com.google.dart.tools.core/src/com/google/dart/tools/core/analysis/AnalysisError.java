/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.DartCompilationError;

import java.io.File;

/**
 * A {@link File} {@link DartCompilationError} pair
 */
public class AnalysisError {

  private final File file;
  private final DartCompilationError error;

  public AnalysisError(File file, DartCompilationError error) {
    this.file = file;
    this.error = error;
  }

  public DartCompilationError getCompilationError() {
    return error;
  }

  public File getFile() {
    return file;
  }
}

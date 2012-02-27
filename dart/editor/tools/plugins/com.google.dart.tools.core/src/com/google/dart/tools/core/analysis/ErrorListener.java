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
import com.google.dart.compiler.DartCompilerListener;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.ast.DartUnit;

import java.io.File;
import java.net.URI;
import java.util.HashSet;

class ErrorListener implements DartCompilerListener {
  private final AnalysisEvent event;
  private HashSet<URI> fileUris = null;

  ErrorListener(AnalysisEvent event) {
    this.event = event;
  }

  @Override
  public void onError(DartCompilationError err) {
    URI uri = err.getSource().getUri();
    if (fileUris == null) {
      fileUris = new HashSet<URI>();
      for (File file : event.getFiles()) {
        fileUris.add(file.toURI());
      }
    }
    if (fileUris.contains(uri)) {
      event.addError(err);
    }
  }

  @Override
  public void unitAboutToCompile(DartSource source, boolean diet) {
  }

  @Override
  public void unitCompiled(DartUnit unit) {
  }
}

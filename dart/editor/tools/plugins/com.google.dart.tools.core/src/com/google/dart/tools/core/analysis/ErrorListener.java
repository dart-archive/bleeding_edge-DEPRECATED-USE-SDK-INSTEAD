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
import com.google.dart.compiler.Source;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.core.analysis.AnalysisUtility.toFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

class ErrorListener implements DartCompilerListener {
  private final AnalysisServer server;
  private Collection<AnalysisError> errors = AnalysisError.NONE;

  ErrorListener(AnalysisServer server) {
    this.server = server;
  }

  public Collection<AnalysisError> getErrors() {
    return errors;
  }

  @Override
  public void onError(DartCompilationError compilationError) {

    // TODO (danrubel) where to report errors with no source?
    Source source = compilationError.getSource();
    if (source == null) {
      return;
    }

    // TODO (danrubel): Where to report errors that do not map to a file
    File dartFile = toFile(server, source.getUri());

    if (dartFile == null) {
      return;
    }

    // if file is in the "packages" directory, do not report errors
    if (DartCore.isContainedInPackages(dartFile)) {
      System.out.println(dartFile.getAbsolutePath());
      return;
    }

    File libraryFile;
    if (source instanceof DartSource) {
      // TODO (danrubel): Where to report errors that do not map to a library
      libraryFile = toFile(server, ((DartSource) source).getLibrary().getUri());
      if (libraryFile == null) {
        return;
      }
    } else {
      libraryFile = dartFile;
    }

    if (errors == AnalysisError.NONE) {
      errors = new ArrayList<AnalysisError>();
    }
    errors.add(new AnalysisError(libraryFile, dartFile, compilationError));
  }

  @Override
  public void unitAboutToCompile(DartSource source, boolean diet) {
  }

  @Override
  public void unitCompiled(DartUnit unit) {
  }
}

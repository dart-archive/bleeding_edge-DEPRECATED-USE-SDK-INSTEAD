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
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.core.analysis.AnalysisUtility.toFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

class ErrorListener implements DartCompilerListener {
  private final AnalysisServer server;
  private final ArrayList<DartCompilationError> errors = new ArrayList<DartCompilationError>();

  ErrorListener(AnalysisServer server) {
    this.server = server;
  }

  @Override
  public void onError(DartCompilationError err) {
    errors.add(err);
  }

  @Override
  public void unitAboutToCompile(DartSource source, boolean diet) {
  }

  @Override
  public void unitCompiled(DartUnit unit) {
  }

  void notifyParsed(AnalysisEvent event) {
    for (AnalysisListener listener : server.getAnalysisListeners()) {
      try {
        listener.parsed(event);
      } catch (Throwable e) {
        DartCore.logError("Exception during parsed notification", e);
      }
    }
  }

  void notifyParsed(File libraryFile, File sourceFile, DartUnit dartUnit) {
    AnalysisEvent event = new AnalysisEvent(libraryFile);
    event.addFileAndDartUnit(sourceFile, dartUnit);
    event.addErrors(server, errors);
    notifyParsed(event);
  }

  void notifyResolved(File libFile, LibraryUnit libUnit) {
    AnalysisEvent event = new AnalysisEvent(libFile);
    Iterator<DartUnit> iter = libUnit.getUnits().iterator();
    while (iter.hasNext()) {
      DartUnit dartUnit = iter.next();
      File dartFile = toFile(server, dartUnit.getSourceInfo().getSource().getUri());
      if (dartFile != null) {
        event.addFileAndDartUnit(dartFile, dartUnit);
      }
    }
    event.addErrors(server, errors);

    for (AnalysisListener listener : server.getAnalysisListeners()) {
      try {
        listener.resolved(event);
      } catch (Throwable e) {
        DartCore.logError("Exception during resolved notification", e);
      }
    }
  }
}

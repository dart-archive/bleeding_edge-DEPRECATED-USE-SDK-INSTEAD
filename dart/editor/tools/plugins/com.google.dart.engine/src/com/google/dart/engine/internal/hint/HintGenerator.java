/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.hint;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.internal.context.PerformanceStatistics;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;

/**
 * Instances of the class {@code HintGenerator} traverse a library's worth of dart code at a time to
 * generate hints over the set of sources.
 * 
 * @see HintCode
 * @coverage dart.engine.resolver
 */
public class HintGenerator {
  private final CompilationUnit[] compilationUnits;

  @SuppressWarnings("unused")
  private final AnalysisContext context;

  AnalysisErrorListener errorListener;

  private ImportsVerifier importsVerifier;

  private final boolean enableDart2JSHints;

  /**
   * The inheritance manager used to find overridden methods.
   */
  private InheritanceManager manager;

  public HintGenerator(CompilationUnit[] compilationUnits, AnalysisContext context,
      AnalysisErrorListener errorListener) {
    this.compilationUnits = compilationUnits;
    this.context = context;
    this.errorListener = errorListener;
    LibraryElement library = compilationUnits[0].getElement().getLibrary();
    importsVerifier = new ImportsVerifier(library);
    enableDart2JSHints = context.getAnalysisOptions().getDart2jsHint();
    manager = new InheritanceManager(compilationUnits[0].getElement().getLibrary());
  }

  public void generateForLibrary() throws AnalysisException {
    TimeCounterHandle timeCounter = PerformanceStatistics.hints.start();
    try {
      for (int i = 0; i < compilationUnits.length; i++) {
        CompilationUnitElement element = compilationUnits[i].getElement();
        if (element != null) {
          if (i == 0) {
            importsVerifier.setInDefiningCompilationUnit(true);
            generateForCompilationUnit(compilationUnits[i], element.getSource());
            importsVerifier.setInDefiningCompilationUnit(false);
          } else {
            generateForCompilationUnit(compilationUnits[i], element.getSource());
          }
        }
      }
      ErrorReporter definingCompilationUnitErrorReporter = new ErrorReporter(
          errorListener,
          compilationUnits[0].getElement().getSource());
      importsVerifier.generateDuplicateImportHints(definingCompilationUnitErrorReporter);
      importsVerifier.generateUnusedImportHints(definingCompilationUnitErrorReporter);
    } finally {
      timeCounter.stop();
    }
  }

  private void generateForCompilationUnit(CompilationUnit unit, Source source) {
    ErrorReporter errorReporter = new ErrorReporter(errorListener, source);

    unit.accept(importsVerifier);

    // dead code analysis
    unit.accept(new DeadCodeVerifier(errorReporter));

    // dart2js analysis
    if (enableDart2JSHints) {
      unit.accept(new Dart2JSVerifier(errorReporter));
    }

    // Dart best practices
    unit.accept(new BestPracticesVerifier(errorReporter));

    unit.accept(new OverrideVerifier(manager, errorReporter));

    // Find to-do comments
    new ToDoFinder(errorReporter).findIn(unit);

    // pub analysis
    // TODO(danrubel/jwren) Commented out until bugs in the pub verifier are fixed
//    unit.accept(new PubVerifier(context, errorReporter));
  }
}

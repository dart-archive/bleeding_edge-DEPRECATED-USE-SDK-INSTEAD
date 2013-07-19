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
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.Library;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code HintGenerator} traverse a library's worth of dart code at a time to
 * generate hints over the set of sources.
 * 
 * @see HintCode
 * @coverage dart.engine.resolver
 */
public class HintGenerator {
  private final Library library;

  @SuppressWarnings("unused")
  private final AnalysisContext context;

  AnalysisErrorListener errorListener;

//  private PubVerifier pubVerifier;

  private DeadCodeVerifier deadCodeVerifier;

  public HintGenerator(Library library, AnalysisContext context, AnalysisErrorListener errorListener) {
    this.library = library;
    this.context = context;
    this.errorListener = errorListener;
  }

  public void generateForLibrary() throws AnalysisException {
    CompilationUnit definingUnit = library.getDefiningCompilationUnit();
    Source definingSource = library.getLibrarySource();

    generateForCompilationUnit(definingUnit, definingSource);

    for (Source source : library.getCompilationUnitSources()) {
      // visit all of the sources, except the defining source
      if (!source.equals(definingSource)) {
        CompilationUnit unit = library.getAST(source);
        generateForCompilationUnit(unit, source);
      }
    }
  }

  private void generateForCompilationUnit(CompilationUnit unit, Source source) {
    ErrorReporter errorReporter = new ErrorReporter(errorListener, source);

    deadCodeVerifier = new DeadCodeVerifier(errorReporter);
    deadCodeVerifier.visitCompilationUnit(unit);

//    pubVerifier = new PubVerifier(context, errorReporter);
//    pubVerifier.visitCompilationUnit(unit);
  }
}

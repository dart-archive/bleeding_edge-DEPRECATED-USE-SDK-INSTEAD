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
package com.google.dart.engine.internal.verifier;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.internal.error.ErrorReporter;

/**
 * Instances of the class {@code HintVerifier} traverse an AST structure looking for additional
 * additional suggestions not mentioned in the Dart Language Specification.
 * 
 * @coverage dart.engine.resolver
 */
public class HintVerifier {

//  private final PubVerifier pubVerifier;

  private final DeadCodeVerifier deadCodeVerifier;

  public HintVerifier(AnalysisContext context, ErrorReporter errorReporter) {
//    pubVerifier = new PubVerifier(context, errorReporter);
    deadCodeVerifier = new DeadCodeVerifier(errorReporter);
  }

  public void visitCompilationUnit(CompilationUnit node) {
//    node.accept(pubVerifier);
    node.accept(deadCodeVerifier);
  }

}

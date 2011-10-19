/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.completion;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.tools.core.internal.completion.CompletionEngine;
import com.google.dart.tools.core.model.CompilationUnit;

import java.util.Collection;

/**
 * Returned by {@link CompletionRequestor#getMetrics()} and used by {@link CompletionEngine} to
 * report metrics.
 */
public abstract class CompletionMetrics {

  /**
   * Called by {@link CompletionEngine} when code completion begins. By default, this method does
   * nothing, but subclasses may override as desired.
   * 
   * @param sourceUnit the source unit in which the code completion is occurring
   * @param completionPosition the source position at which the code completion is occurring
   */
  public void completionBegin(CompilationUnit sourceUnit, int completionPosition) {
  }

  /**
   * Called by {@link CompletionEngine} when code completion ends. By default, this method does
   * nothing, but subclasses may override as desired.
   */
  public void completionEnd() {
  }

  /**
   * Called by the {@link CompletionEngine} to report an exception that occurred during completion.
   * By default, this method does nothing, but subclasses may override as desired.
   * 
   * @param e the exception (not <code>null</code>)
   */
  public void completionException(Exception e) {
  }

  /**
   * Called when the library could not be resolved, causing the completion to fail. By default, this
   * method does nothing, but subclasses may override as desired.
   * 
   * @param parseErrors the parse errors reported by the compiler (not <code>null</code>)
   */
  public void resolveLibraryFailed(Collection<DartCompilationError> parseErrors) {
  }

  /**
   * Called by the {@link CompletionEngine} to report the elapse time calling the compiler to parse
   * and resolve the library elements. By default, this method does nothing, but subclasses may
   * override as desired.
   * 
   * @param ms the time in milliseconds
   */
  public void resolveLibraryTime(long ms) {
  }

  /**
   * Called by the {@link CompletionEngine} when an internal code completion visitor does not have a
   * particular visitor method implemented.By default, this method does nothing, but subclasses may
   * override as desired.
   */
  public void visitorNotImplementedYet(DartNode node, DartNode sourceNode,
      Class<? extends DartNodeTraverser<Void>> astClass) {
  }
}

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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.engine.ast.CompilationUnit;

/**
 * Interface of an object listening to Dart reconciling.
 */
public interface IDartReconcilingListener {
  /**
   * Called after reconciling has been finished and resolved {@link CompilationUnit} is ready.
   * 
   * @param unit the resolved {@link CompilationUnit}, not null.
   */
  void reconciled(CompilationUnit unit);
}

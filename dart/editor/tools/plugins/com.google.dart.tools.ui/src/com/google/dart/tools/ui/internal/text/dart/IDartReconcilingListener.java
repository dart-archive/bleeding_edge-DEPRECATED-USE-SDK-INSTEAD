/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.compiler.ast.DartUnit;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface of an object listening to Java reconciling.
 */
public interface IDartReconcilingListener {

  /**
   * Called before reconciling is started.
   */
  void aboutToBeReconciled();

  /**
   * Called after reconciling has been finished.
   * 
   * @param ast the compilation unit AST or <code>null</code> if the working copy was consistent or
   *          reconciliation has been cancelled
   * @param forced <code>true</code> iff this reconciliation was forced
   * @param progressMonitor the progress monitor
   */
  void reconciled(DartUnit ast, boolean forced, IProgressMonitor progressMonitor);
}

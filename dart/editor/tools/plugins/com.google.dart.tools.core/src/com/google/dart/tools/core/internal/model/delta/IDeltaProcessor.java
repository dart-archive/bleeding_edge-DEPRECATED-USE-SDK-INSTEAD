/*
 * Copyright (c) 2012, the Dart project authors.
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

package com.google.dart.tools.core.internal.model.delta;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElementDelta;

import java.util.HashMap;
import java.util.List;

/**
 * The public interface to the delta processor.
 */
public interface IDeltaProcessor {

  /**
   * Fire Dart Model delta, flushing them after the fact after post_change notification. If the
   * firing mode has been turned off, this has no effect.
   */
  public void fire(DartElementDelta delta, int postChange);

  /**
   * Queue of deltas created explicitly by the Dart Model that have yet to be fired.
   */
  public List<DartElementDelta> getDartModelDeltas();

  /**
   * Queue of reconcile deltas on working copies that have yet to be fired. This is a mapping from
   * IWorkingCopy to DartElementDelta
   */
  public HashMap<CompilationUnit, DartElementDelta> getReconcileDeltas();

  /**
   * Registers the given delta with this delta processor.
   */
  public void registerDartModelDelta(DartElementDelta delta);

  /**
   * Traverse the set of projects which have changed namespace, and reset their caches and their
   * dependents
   */
  public void resetProjectCaches();

  /**
   * Update Dart Model given some delta
   */
  public void updateDartModel(DartElementDelta dartElementDelta);

}

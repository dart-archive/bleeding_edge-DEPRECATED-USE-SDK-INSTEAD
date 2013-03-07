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

package com.google.dart.tools.core.internal.model.delta;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElementDelta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This mock delta processor is used for the transition to the new alanysis engine.
 */
public class MockDeltaProcessor implements IDeltaProcessor {
  private static IDeltaProcessor instance = new MockDeltaProcessor();

  public static IDeltaProcessor getInstance() {
    return instance;
  }

  public MockDeltaProcessor() {

  }

  @Override
  public void fire(DartElementDelta delta, int postChange) {

  }

  @Override
  public List<DartElementDelta> getDartModelDeltas() {
    return new ArrayList<DartElementDelta>();
  }

  @Override
  public Map<CompilationUnit, DartElementDelta> getReconcileDeltas() {
    return new HashMap<CompilationUnit, DartElementDelta>();
  }

  @Override
  public void registerDartModelDelta(DartElementDelta delta) {

  }

  @Override
  public void resetProjectCaches() {

  }

  @Override
  public void updateDartModel(DartElementDelta dartElementDelta) {

  }

}

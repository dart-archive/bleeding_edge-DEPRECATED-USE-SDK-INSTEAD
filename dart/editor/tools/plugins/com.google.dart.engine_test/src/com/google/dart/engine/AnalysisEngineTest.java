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
package com.google.dart.engine;

import com.google.dart.engine.utilities.logging.Logger;
import com.google.dart.engine.utilities.logging.TestLogger;

public class AnalysisEngineTest extends EngineTestCase {
  public void test_createAnalysisContext() {
    AnalysisEngine engine = AnalysisEngine.getInstance();
    assertNotNull(engine.createAnalysisContext());
  }

  public void test_getInstance() {
    assertNotNull(AnalysisEngine.getInstance());
  }

  public void test_getLogger() {
    AnalysisEngine engine = AnalysisEngine.getInstance();
    Logger defaultLogger = engine.getLogger();
    assertNotNull(defaultLogger);
    Logger logger = new TestLogger();
    engine.setLogger(logger);
    assertEquals(logger, engine.getLogger());
    engine.setLogger(null);
    assertEquals(defaultLogger, engine.getLogger());
  }
}

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

package com.google.dart.tools.ui.instrumentation;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationLogger;

import junit.framework.TestCase;

/**
 * Base class for Instrumentation test cases handle setting up and removing the mock instrumentation
 * framework
 */
public class InstrumentationTestCase extends TestCase {

  protected InstrumentationLogger oldLogger;

  protected MockInstrumentationLogger mockedLogger;

  @Override
  public void setUp() {

    oldLogger = Instrumentation.getLogger();
    mockedLogger = new MockInstrumentationLogger();
    Instrumentation.setLogger(mockedLogger);
  }

  @Override
  public void tearDown() {
    Instrumentation.setLogger(oldLogger);
  }

}

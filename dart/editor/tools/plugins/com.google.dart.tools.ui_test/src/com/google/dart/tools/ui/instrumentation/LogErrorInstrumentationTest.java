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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.Activator;

public class LogErrorInstrumentationTest extends InstrumentationTestCase {

  public void testLogError() {
    assertTrue(Activator.waitForEarlyStartup(10000));

    DartCore.logError(new Exception("TestException"));

    assertNotNull(mockedLogger.getBuilder("DartCore.LogError"));
    assertNotNull(mockedLogger.getBuilder("DartCore.LogError").getData("StackTrace"));

  }
}

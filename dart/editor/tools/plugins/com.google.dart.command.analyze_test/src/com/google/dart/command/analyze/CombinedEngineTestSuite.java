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

package com.google.dart.command.analyze;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * This suite runs the engine tests as well as the command.analyze tests.
 */
public class CombinedEngineTestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite("Tests in "
        + CombinedEngineTestSuite.class.getPackage().getName());
    suite.addTest(com.google.dart.engine.TestAll.suite());
    suite.addTest(com.google.dart.command.analyze.TestAll.suite());
    return suite;
  }

}
